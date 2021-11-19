package org.fhi360.lamis.modules.database.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fhi360.lamis.modules.database.service.vm.InstalledModule;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateStatusService {
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0/30 * * * ?")
    public void update() {
        List<String> nodeId = jdbcTemplate.queryForList("select node_id from sym_node_identity", String.class);
        if (!nodeId.isEmpty() && !nodeId.get(0).equals("000")) {
            List<InstalledModule> modules = jdbcTemplate.query("" +
                    "select name, version from module where install_on_boot = false" +
                    "   except " +
                    "select name, version from update_status",
                    new BeanPropertyRowMapper<>(InstalledModule.class));

            modules.forEach(module ->
                    facilities().forEach(facility -> {
                        try {
                            jdbcTemplate.queryForObject("select version from update_status where name = ? and node_id = ?",
                                    String.class, module.getName(), facility);
                            jdbcTemplate.update("update update_status set version = ? where name = ?", module.getVersion(), module.getName());
                        } catch (Exception e) {
                            jdbcTemplate.update("insert into update_status (name, version, node_id) values(?, ?, ?)",
                                    module.getName(), module.getVersion(), facility);
                        }
                    })
            );
            String sync = "insert into SYM_TABLE_RELOAD_REQUEST (target_node_id, source_node_id, trigger_id, router_id, create_time, create_table, last_update_time) " +
                    "values ('000', '@', 'public.update_status', 'facility_2_server', current_timestamp, 0, current_timestamp);";
            jdbcTemplate.update(sync.replaceAll("@", nodeId.get(0)));
        }
    }

    private List<String> facilities() {
        return jdbcTemplate.queryForList("select distinct facility_id::text from pharmacy where current_date - " +
                "interval '2 months' > last_modified", String.class);
    }

    @PostConstruct
    public void init() {

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Runnable task = () -> {
            try {
                List<String> nodeId = jdbcTemplate.queryForList("select node_id from sym_node_identity", String.class);
                if (!nodeId.isEmpty() && !nodeId.get(0).equals("000")) {
                    jdbcTemplate.update("delete from update_status where node_id != ?", nodeId.get(0));
                }
            } catch (Exception ignored) {
            }
        };
        int delay = 5;
        scheduler.schedule(task, delay, TimeUnit.MINUTES);
        scheduler.shutdown();
    }
}
