package org.fhi360.lamis.modules.database.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.fhi360.lamis.modules.database.domain.repositories.ModuleUpdateRepository;
import org.lamisplus.modules.base.config.ApplicationProperties;
import org.lamisplus.modules.base.domain.entities.Module;
import org.lamisplus.modules.base.domain.repositories.ModuleRepository;
import org.lamisplus.modules.base.module.ModuleFileStorageService;
import org.lamisplus.modules.base.module.ModuleManager;
import org.lamisplus.modules.base.module.ModuleService;
import org.lamisplus.modules.base.module.ModuleUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModuleUpdateManager {
    private final ModuleService moduleService;
    private final ModuleFileStorageService storageService;
    private final ModuleUpdateRepository moduleUpdateRepository;
    private final ModuleRepository moduleRepository;
    private final ApplicationProperties applicationProperties;
    private final JdbcTemplate jdbcTemplate;
    private final SimpMessageSendingOperations messagingTemplate;
    private Path rootLocation;

    public void installUpdates() {
        moduleUpdateRepository.findByInstallIsTrue().forEach(moduleUpdate -> {
            byte[] data = moduleUpdate.getData();
            InputStream inputStream = new ByteArrayInputStream(data);
            Module module = moduleService.uploadModuleData(moduleUpdate.getFileName(), inputStream);
            store(module.getName(), moduleUpdate.getFileName(), new ByteArrayInputStream(data));
            jdbcTemplate.query("select data from module_update where id = ?", rs -> {
                try {
                    IOUtils.copy(new ByteArrayInputStream(rs.getBytes(1)),
                            new FileOutputStream(rootLocation.resolve(Paths.get(module.getArtifact())).toFile()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, moduleUpdate.getId());
            moduleService.update(module, false);
            //moduleUpdate.setInstall(false);
            //moduleUpdateRepository.save(moduleUpdate);
            //jdbcTemplate.update("update module_update set install = false where id = ?", moduleUpdate.getId());
            jdbcTemplate.update("delete from module_update where id = ?", moduleUpdate.getId());
        });

        moduleUpdateRepository.findByUninstallIsTrue().forEach(moduleUpdate -> {
            moduleRepository.findByName(moduleUpdate.getName()).ifPresent(module ->
                    moduleService.uninstall(module, true));
            //moduleUpdate.setUninstall(false);
            //moduleUpdateRepository.save(moduleUpdate);
            //jdbcTemplate.update("update module_update set uninstall = false where id = ?", moduleUpdate.getId());
            jdbcTemplate.update("delete from module_update where id = ?", moduleUpdate.getId());
        });
    }

    public List<Module> updatesAvailable() {
        return moduleUpdateRepository.findByInstallIsTrue().stream()
                .map(moduleUpdate -> {
                    byte[] data = moduleUpdate.getData();
                    InputStream inputStream = new ByteArrayInputStream(data);
                    ModuleManager.VersionInfo versionInfo = readVersionInfo(storageService.getURL(moduleUpdate.getFileName()));

                    Module module = moduleService.uploadModuleData(moduleUpdate.getFileName(), inputStream);
                    module.setVersion(versionInfo.version);
                    module.setDescription(versionInfo.projectName);
                    module.setBuildTime(toZonedDateTime(versionInfo.buildTime));
                    return module;
                }).collect(Collectors.toList());
    }

    @SneakyThrows
    public ModuleManager.VersionInfo readVersionInfo(URL url) {
        Path target = Files.createTempDirectory(null);
        ModuleUtils.copyPathFromJar(url, "/", target);
        URL c = new File(target + "/META-INF/MANIFEST.MF").toPath().toUri().toURL();
        ModuleManager.VersionInfo versionInfo = ModuleManager.VersionInfo.UNKNOWN;
        Manifest manifest = new Manifest(c.openStream());
        try {
            FileUtils.deleteDirectory(target.toFile());
        } catch (Exception ignored) {

        }

        Attributes attr = manifest.getMainAttributes();
        versionInfo = new ModuleManager.VersionInfo();
        versionInfo.manifest = manifest;
        versionInfo.available = true;
        versionInfo.projectName = StringUtils.defaultString(attr.getValue("Implementation-Title"), "unknown");
        versionInfo.version = StringUtils.defaultString(attr.getValue("Implementation-Version"), "unknown");
        String buildTime = attr.getValue("Build-Time");
        if (buildTime != null) {
            try {
                versionInfo.buildTime = DateUtils.parseDate(buildTime, "yyyyMMdd-HHmm", "yyyy-MM-dd'T'HH:mm:ss'Z'");
            } catch (ParseException ignored) {
            }
        }

        return versionInfo;
    }

    public Module uploadModuleData(MultipartFile file) {
        return moduleService.uploadModuleData(file);
    }

    public void checkForUpdates() {
        messagingTemplate.convertAndSend("/topic/update/download/completed", "false");
        jdbcTemplate.execute("delete from sym_incoming_batch where channel_id = 'reload'");
        List<String> nodeId = jdbcTemplate.queryForList("select node_id from sym_node_identity", String.class);
        if (!nodeId.isEmpty()) {
            String sync = "insert into SYM_TABLE_RELOAD_REQUEST (target_node_id, source_node_id, trigger_id, router_id, create_time, create_table, last_update_time) " +
                    " values ('@', '000', 'public.modules_update', 'server_2_facility', current_timestamp, 0, current_timestamp)";
            jdbcTemplate.update(sync.replaceAll("@", nodeId.get(0)));

            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
            Runnable runnable = () -> {
                String query = "select status = 'OK' from sym_incoming_batch where channel_id = 'reload' and " +
                        "node_id = '000' and summary = 'module_update' order by batch_id desc limit 1";
                List<Boolean> statuses = jdbcTemplate.queryForList(query, Boolean.class);
                if (!statuses.isEmpty() && statuses.get(0)) {
                    moduleUpdateRepository.findByInstallIsTrue()
                            .forEach(moduleUpdate -> moduleRepository.findByName(moduleUpdate.getName()).ifPresent(module -> {
                                ZoneId zoneId = module.getBuildTime().getZone();
                                if (module.getBuildTime() != null &&
                                        toLocalDateTime(moduleUpdate.getBuildTime()).atZone(zoneId).minusSeconds(1)
                                                .isBefore(module.getBuildTime())) {
                                    //moduleUpdateRepository.delete(moduleUpdate);
                                    jdbcTemplate.update("delete from module_update where id = ?", moduleUpdate.getId());
                                }
                            }));
                    service.shutdown();
                    messagingTemplate.convertAndSend("/topic/update/download/completed", "true");
                } else {
                    messagingTemplate.convertAndSend("/topic/update/download/completed", "false");
                }
            };

            service.scheduleAtFixedRate(runnable, 0, 5, TimeUnit.SECONDS);
        }
    }

    private ZonedDateTime toZonedDateTime(Date date) {
        return date == null ? null : ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public String store(String module, String name, InputStream inputStream) {
        module = module.toLowerCase();
        String filename = module + File.separator + org.springframework.util.StringUtils.cleanPath(name);
        if (filename.endsWith(File.separator)) {
            filename = filename.substring(0, filename.length() - 1) + ".jar";
        }

        if (!filename.endsWith(".jar")) {
            filename = filename + ".jar";
        }

        if (!Files.exists(this.rootLocation.resolve(module))) {
            try {
                Files.createDirectories(this.rootLocation.resolve(module));
            } catch (IOException var7) {
                var7.printStackTrace();
            }
        }

        try {
            if (filename.contains("..")) {
                throw new RuntimeException("Cannot store file with relative path outside current directory " + filename);
            } else {
                FileUtils.copyInputStreamToFile(inputStream, this.rootLocation.resolve(filename).toFile());
                return filename;
            }
        } catch (IOException var6) {
            throw new RuntimeException("Failed to store file " + filename, var6);
        }
    }

    public LocalDateTime lastHeartbeat() {
        List<LocalDateTime> heartbeats = jdbcTemplate.queryForList("select last_update_time from sym_incoming_batch where channel_id = 'heartbeat' " +
                "and status = 'OK' and node_id = '000' order by last_update_time desc limit 1", LocalDateTime.class);
        return !heartbeats.isEmpty() ? heartbeats.get(0) : null;
    }

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(applicationProperties.getModulePath());
    }

    public LocalDateTime toLocalDateTime(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
