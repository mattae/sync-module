package org.fhi360.lamis.modules.database.web.rest;

import lombok.RequiredArgsConstructor;
import org.fhi360.lamis.modules.database.service.RecordDownloadService;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/database-sync")
@Profile("!server")
@RequiredArgsConstructor
public class EncounterUpdateResource {
    private final RecordDownloadService downloadService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/cparp/update/{facilityId}")
    public void update(@PathVariable Long facilityId) {
        downloadService.syncEncounter(facilityId);
    }

    @GetMapping("/download-records")
    public void downloadRecords() {
        List<Long> ids = jdbcTemplate.queryForList("select distinct facility_id from patient where archived = false", Long.class);
        ids.forEach(downloadService::updateRecords);
    }
}
