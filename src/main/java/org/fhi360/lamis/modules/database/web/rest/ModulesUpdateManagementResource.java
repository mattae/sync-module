package org.fhi360.lamis.modules.database.web.rest;

import io.github.jhipster.web.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.fhi360.lamis.modules.database.domain.entities.ModuleUpdate;
import org.fhi360.lamis.modules.database.domain.repositories.ModuleUpdateRepository;
import org.fhi360.lamis.modules.database.service.ModuleUpdateManager;
import org.lamisplus.modules.base.domain.entities.Module;
import org.lamisplus.modules.base.module.ModuleFileStorageService;
import org.lamisplus.modules.base.module.ModuleManager;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/modules-update")
@RequiredArgsConstructor
public class ModulesUpdateManagementResource {
    private final ModuleUpdateManager moduleUpdateManager;
    private final ModuleUpdateRepository moduleUpdateRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ModuleFileStorageService moduleFileStorageService;

    @GetMapping("/install-updates")
    public List<ModuleUpdate> update() {
        moduleUpdateManager.installUpdates();
        return moduleUpdateRepository.findAll();
    }

    @GetMapping("/module/{id}")
    public ResponseEntity<ModuleUpdate> findById(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(moduleUpdateRepository.findById(id));
    }

    @GetMapping("/available-updates")
    public List<Module> availableUpdates() {
        return moduleUpdateManager.updatesAvailable();
    }

    @GetMapping("/available-modules")
    public List<ModuleUpdate> availableModules() {
        return moduleUpdateRepository.findAll();
    }

    @GetMapping("/check-for-updates")
    public void checkForUpdates() {
        moduleUpdateManager.checkForUpdates();
    }

    @GetMapping("/last-heartbeat")
    public LocalDateTime lastHeartbeat() {
        return moduleUpdateManager.lastHeartbeat();
    }

    @PostMapping("/upload")
    public ModuleUpdate upload(@RequestParam("file") MultipartFile file) {
        Module module = moduleUpdateManager.uploadModuleData(file);

        ModuleUpdate update = new ModuleUpdate();
        update.setName(module.getName());
        update.setFileName(module.getArtifact());
        update.setVersion(module.getVersion());

        ModuleManager.VersionInfo versionInfo = moduleUpdateManager.readVersionInfo(moduleFileStorageService.getURL(module.getArtifact()));
        //InputStream inputStream = moduleFileStorageService.readFile(update.getFileName());
        //byte[] data = IOUtils.toByteArray(inputStream);

        //module = moduleUpdateManager.updateBuildTime(new ByteArrayInputStream(data));
        //update.setBuildTime(module.getBuildTime());
        update.setBuildTime(versionInfo.buildTime);
        return update;
    }

    @PostMapping("/save-update")
    public ResponseEntity<ModuleUpdate> saveUpdate(@RequestBody ModuleUpdate update) throws IOException {
        moduleUpdateRepository.findByName(update.getName()).ifPresent(mu ->
                jdbcTemplate.update("delete from module_update where id = ?", mu.getId()));
        InputStream inputStream = moduleFileStorageService.readFile(update.getFileName());
        byte[] data = IOUtils.toByteArray(inputStream);
        update.setData(data);
        /*update = moduleUpdateRepository.save(update);
        return ResponseEntity.ok(update);*/
        int index = update.getFileName().lastIndexOf('/');
        if (index == -1) {
            index = update.getFileName().lastIndexOf('\\');
        }
        String fileName = update.getFileName().substring(index + 1);
        jdbcTemplate.update("insert into module_update(name, version, build_time, file_name, data) values(?, ?, ?, ?, ?)",
                update.getName(), update.getVersion(), update.getBuildTime(), fileName, data);
        return ResponseUtil.wrapOrNotFound(moduleUpdateRepository.findByName(update.getName()));
    }

    @PostMapping("/uninstall")
    public ResponseEntity<ModuleUpdate> uninstall(@RequestBody ModuleUpdate update) {
        update.setInstall(false);
        update.setUninstall(true);
        //update = moduleUpdateRepository.save(update);
        jdbcTemplate.update("update module_update set install = false, uninstall = true where id = ?", update.getId());
        return ResponseEntity.ok(update);
    }
}
