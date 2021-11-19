package org.fhi360.lamis.modules.database.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Profile("server")
@Slf4j
public class UploadStatusService {
    private final JdbcTemplate jdbcTemplate;


    //@Scheduled(cron = "0 0/3 * * * ?")
    public void updateUploadStatus() {
        jdbcTemplate.query("select node_id, max(last_update_time) last_sync from sym_incoming_batch where channel_id = " +
                "'reload' and summary in ('patient', 'pharmacy', 'clinic', 'laboratory', 'biometric') and status = 'OK' group by 1", rs-> {
            String nodeId = rs.getString("node_id");
            LocalDateTime date = rs.getObject("last_sync", LocalDateTime.class);

            jdbcTemplate.update("" +
                    "insert into upload_status(node_id, last_sync) values (?, ?) " +
                    "   on conflict(node_id)" +
                    "   do" +
                    "   update set last_sync = ? where upload_status.node_id = ?", nodeId, date, date, nodeId);

            jdbcTemplate.query("select s_facility from roving_facility where p_facility = ?", rs1 -> {
                String facility = rs1.getString(1);
                jdbcTemplate.update("" +
                        "insert into upload_status(node_id, last_sync) values (?, ?) " +
                        "   on conflict(node_id)" +
                        "   do" +
                        "   update set last_sync = ? where upload_status.node_id = ?", facility, date, date, facility);
            }, nodeId);
        });
    }
}
