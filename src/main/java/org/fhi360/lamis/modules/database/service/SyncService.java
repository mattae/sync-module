package org.fhi360.lamis.modules.database.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fhi360.lamis.modules.database.domain.entities.SyncTrigger;
import org.fhi360.lamis.modules.database.domain.repositories.SyncTriggerRepository;
import org.lamisplus.modules.lamis.legacy.domain.entities.Encounter;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final SyncTriggerRepository syncTriggerRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final static RestTemplate restTemplate = new RestTemplate();
    public final static String PROXY_URL = "http://lamis3.sidhas.org:8080";
    //public final static String PROXY_URL = "http://lamis-sharpto2.fhi360.org:8080";
    //public final static String PROXY_URL = "http://lamis.ahnigeria.org:8080";
    private static final AtomicBoolean syncing = new AtomicBoolean(false);
    private static Long LAST_TRIGGER_ID;
    private boolean synced;

    public void sync() {
        messagingTemplate.convertAndSend("/topic/sync/upload-status/completed", "false");
        List<String> nodeId = jdbcTemplate.queryForList("select node_id from sym_node_identity", String.class);
        if (!nodeId.isEmpty()) {
            if (LAST_TRIGGER_ID == null) {
                syncTriggerRepository.findAll().stream()
                        .max(Comparator.comparing(SyncTrigger::getPriority))
                        .ifPresent(t -> LAST_TRIGGER_ID = t.getId());
                syncing.set(true);
                synced = true;
                jdbcTemplate.execute("truncate sym_outgoing_batch;truncate sym_table_reload_status;");
                String sync = "insert into SYM_TABLE_RELOAD_REQUEST (target_node_id, source_node_id, trigger_id, router_id, create_time, create_table, last_update_time) " +
                        "values ('000', '@', '#', 'facility_2_server', current_timestamp, 0, current_timestamp);";

                syncTriggerRepository.findAll().stream()
                        .sorted(Comparator.comparing(SyncTrigger::getPriority))
                        .forEach(syncTrigger -> {
                            jdbcTemplate.update(sync.replaceAll("@", nodeId.get(0)).replaceAll("#", syncTrigger.getTriggerId()));
                            try {
                                Thread.sleep(10_000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
            }
        }
    }

    private void updateTriggerStart() {
        if (syncing.get() && LAST_TRIGGER_ID != null) {
            syncTriggerRepository.findAll()
                    .forEach(syncTrigger -> {
                        if (triggerCompleted(syncTrigger.getTriggerId())) {
                            jdbcTemplate.update("update sync_trigger set start = ? where id = ?",
                                    LocalDateTime.now().minusDays(3), syncTrigger.getId());
                            //runningTriggers.remove(syncTrigger);
                            if (Objects.equals(syncTrigger.getId(), LAST_TRIGGER_ID)) {
                                LAST_TRIGGER_ID = null;
                            }
                        }
                    });
        }
    }

    private void checkSyncing() {
        String status = syncing.get() ? "false" : "true";
        messagingTemplate.convertAndSend("/topic/sync/upload-status/completed", status);
        if (!syncOngoing()) {
            updateTriggerStart();
            messagingTemplate.convertAndSend("/topic/sync/upload-status/completed", "true");
            if (syncing.get()) {
                syncing.set(false);
                try {
                    LocalDateTime dateTime = LocalDateTime.now();
                    List<Long> activeId = jdbcTemplate.queryForList("select distinct facility_id from pharmacy w where current_date - " +
                            "interval '2 months' > last_modified and archived = false ", Long.class);
                    activeId.forEach(id -> {
                        String node = StringUtils.leftPad(id.toString(), 4, "0");
                        jdbcTemplate.update("" +
                                        "insert into upload_status(node_id, last_sync) values (?, ?) " +
                                        "   on conflict(node_id)" +
                                        "   do" +
                                        "   update set last_sync = ? where upload_status.node_id = ?",
                                node, dateTime, dateTime, node);
                    });
                    synced = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<String> syncedTables() {
        return jdbcTemplate.queryForList("select distinct summary from sym_outgoing_batch where channel_id = 'reload' " +
                "and status = 'OK' and summary != 'update_status' order by 1", String.class);
    }

    public Boolean syncOngoing() {
        return synced && (
                jdbcTemplate.queryForObject("select count(*) from sym_table_reload_status where end_time is not null", Long.class)
                        < syncTriggerRepository.count() ||
                        jdbcTemplate.queryForObject("select count(*) > 0 from sym_outgoing_batch where status != 'OK'", Boolean.class));
    }

    public List<LocalDateTime> lastHeartbeat() {
        return jdbcTemplate.queryForList("select last_update_time from sym_incoming_batch where channel_id = 'heartbeat' " +
                "and status = 'OK' and node_id = '000' order by last_update_time desc limit 1", LocalDateTime.class);
    }

    public List<LocalDateTime> lastSuccessfulSync() {
        List<LocalDateTime> dateTimes = new ArrayList<>();
        List<String> nodeId = jdbcTemplate.queryForList("select node_id from sym_node_identity", String.class);
        if (!nodeId.isEmpty()) {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(PROXY_URL + "/sync/last-successful-sync")
                    .queryParam("nodeId", nodeId.get(0));
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Encounter> requestEntity = new HttpEntity<>(requestHeaders);
            try {
                ResponseEntity<LocalDateTime> response = restTemplate.exchange(
                        uriBuilder.toUriString(),
                        HttpMethod.GET,
                        requestEntity,
                        LocalDateTime.class
                );
                LocalDateTime dateTime = response.getBody();
                if (dateTime != null) {
                    dateTimes.add(dateTime);
                }
            } catch (Exception ignored) {
            }
        }
        return dateTimes;
    }

    @SneakyThrows
    public void cleanupDatabase(List<Long> ids) {
        String params = ids.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        String query = "select cleanup_database('{@}')";
        query = query.replace("@", params);
        try {
            jdbcTemplate.execute(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<String> tablesForTrigger(String triggerId) {
        return jdbcTemplate.queryForList("select regexp_split_to_table(source_table_name, ',') from sym_trigger where trigger_id = ?",
                String.class, triggerId);
    }

    private Boolean triggerCompleted(String triggerId) {
        Map<String, Object> params = new HashMap<>();
        params.put("tables", tablesForTrigger(triggerId));
        return namedParameterJdbcTemplate.queryForObject("select count(*) < 1 from sym_outgoing_batch where status " +
                "!= 'OK' and summary in (:tables)", params, Boolean.class);
    }

    public void init() {
        checkSyncing();
    }

    @SneakyThrows
    private void updateStatus() {
        String tables = String.join("\n", syncedTables());
        messagingTemplate.convertAndSend("/topic/sync/table-status", tables);
        messagingTemplate.convertAndSend("/topic/sync/server-status", !lastHeartbeat().isEmpty() ?
                lastHeartbeat().get(0).format(DateTimeFormatter.ISO_DATE_TIME) : "");
        messagingTemplate.convertAndSend("/topic/sync/sync-status", !lastSuccessfulSync().isEmpty() ?
                lastSuccessfulSync().get(0).format(DateTimeFormatter.ISO_DATE_TIME) : "");

        URL url = new URL(PROXY_URL);
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        huc.setRequestMethod("HEAD");
        int responseCode = huc.getResponseCode();
        boolean serverAlive = HttpURLConnection.HTTP_OK == responseCode;
        messagingTemplate.convertAndSend("/topic/sync/server", serverAlive);
    }

    public void destroy() {
    }

    @SneakyThrows
    @PostConstruct
    public void cleanup() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Runnable task = () -> {
            try {
                List<String> nodeId = jdbcTemplate.queryForList("select node_id from sym_node_identity", String.class);
                if (!nodeId.isEmpty() && !nodeId.get(0).equals("000")) {
                    List<Long> ids = jdbcTemplate.queryForList("select distinct facility_id from patient", Long.class);
                    List<Long> activeId = jdbcTemplate.queryForList("select distinct facility_id from pharmacy where last_modified " +
                            "> '2020-06-30' and archived = false ", Long.class);
                    try {
                        jdbcTemplate.execute("select remove_orphaned_records()");
                    } catch (Exception ignored) {
                    }
                    if (ids.size() != activeId.size()) {
                        cleanupDatabase(activeId);
                    }
                }
            } catch (Exception ignored) {

            }
        };
        int delay = 5;
        scheduler.schedule(task, delay, TimeUnit.MINUTES);
        scheduler.shutdown();
    }

    public void downloadBiometrics() {
        List<String> nodeId = jdbcTemplate.queryForList("select node_id from sym_node_identity", String.class);
        if (!nodeId.isEmpty()) {
            String sync = "insert into SYM_TABLE_RELOAD_REQUEST (target_node_id, source_node_id, trigger_id, router_id, create_time, create_table, last_update_time) " +
                    "values ('000', '@', 'public.patient_dead', 'facility_2_server', current_timestamp, 0, current_timestamp);";
            jdbcTemplate.update(sync.replaceAll("@", nodeId.get(0)));
            sleep(10_000);
            sync = "insert into SYM_TABLE_RELOAD_REQUEST (target_node_id, source_node_id, trigger_id, router_id, create_time, create_table, last_update_time) " +
                    "values ('@', '000', 'public.pull_patient', 'server_2_facility', current_timestamp, 0, current_timestamp);";
            jdbcTemplate.update(sync.replaceAll("@", nodeId.get(0)));
            sleep(10_000);
            sync = "insert into SYM_TABLE_RELOAD_REQUEST (target_node_id, source_node_id, trigger_id, router_id, create_time, create_table, last_update_time) " +
                    "values ('@', '000', 'public.pull_biometric', 'server_2_facility', current_timestamp, 0, current_timestamp);";
            jdbcTemplate.update(sync.replaceAll("@", nodeId.get(0)));
        }
    }

    private void sleep(long millisecs) {
        try {
            Thread.sleep(millisecs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void uploadBiometrics() {
        List<String> nodeId = jdbcTemplate.queryForList("select node_id from sym_node_identity", String.class);
        if (!nodeId.isEmpty()) {
            String sync = "insert into SYM_TABLE_RELOAD_REQUEST (target_node_id, source_node_id, trigger_id, router_id, create_time, create_table, last_update_time) " +
                    "values ('000', '@', 'public.patient_dead', 'facility_2_server', current_timestamp, 0, current_timestamp);";
            jdbcTemplate.update(sync.replaceAll("@", nodeId.get(0)));
            sleep(10_000);
            sync = "insert into SYM_TABLE_RELOAD_REQUEST (target_node_id, source_node_id, trigger_id, router_id, create_time, create_table, last_update_time) " +
                    "values ('000', '@', 'public.biometric_dead', 'facility_2_server', current_timestamp, 0, current_timestamp);";
            jdbcTemplate.update(sync.replaceAll("@", nodeId.get(0)));
        }
    }

    public boolean biometricUploadCompleted() {
        boolean syncStarted = jdbcTemplate.queryForObject("select count(*) > 0 from sym_outgoing_batch where summary = 'biometric'", Boolean.class);
        boolean syncCompleted = jdbcTemplate.queryForObject("select count(*) = 0 from sym_outgoing_batch where summary = 'biometric' and status != 'OK'", Boolean.class);

        return syncStarted && syncCompleted;
    }

    public boolean biometricDownloadCompleted() {
        boolean syncStarted = jdbcTemplate.queryForObject("select count(*) > 0 from sym_incoming_batch where summary = 'biometric'", Boolean.class);
        boolean syncCompleted = jdbcTemplate.queryForObject("select count(*) = 0 from sym_incoming_batch where summary = 'biometric' and status != 'OK'", Boolean.class);

        return syncStarted && syncCompleted;
    }

    @Scheduled(cron = "*/3 * * * * *")
    public void updateStatuses() {
        updateStatus();
    }

    @Scheduled(cron = "*/3 * * * * *")
    public void checkSync() {
        checkSyncing();
    }

    @Scheduled(cron = "*/30 * * * * *")
    public void cleanupDatabase() {
        cleanup();
    }
}
