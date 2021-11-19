package org.fhi360.lamis.modules.database.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jumpmind.db.model.Table;
import org.jumpmind.symmetric.io.data.CsvData;
import org.jumpmind.symmetric.io.data.DataContext;
import org.jumpmind.symmetric.io.data.writer.DatabaseWriterFilterAdapter;
import org.lamisplus.modules.base.config.ContextProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrphanedRecordFilter extends DatabaseWriterFilterAdapter {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean beforeWrite(DataContext context, Table table, CsvData data) {
        try {
            String[] parsedData = data.getParsedData(CsvData.ROW_DATA);
            int idx = table.getColumnIndex("patient_id");
            if (idx == -1) {
                return true;
            }
            idx = table.getColumnIndex("facility_id");
            if (idx == -1) {
                return true;
            }
            String patientId = parsedData[table.getColumnIndex("patient_id")];
            String facilityId = parsedData[table.getColumnIndex("facility_id")];
            if (StringUtils.equalsIgnoreCase(table.getName(), "biometric")) {
                return jdbcTemplate.queryForObject("select count(*) > 0 from patient where facility_id = ? and uuid = ?",
                    Boolean.class, Long.parseLong(facilityId), patientId);
            }
            boolean write = jdbcTemplate.queryForObject("select count(*) > 0 from patient where facility_id = ? and id = ?",
                Boolean.class, Long.parseLong(facilityId), Long.parseLong(patientId));

            idx = table.getColumnIndex("mother_id");
            if (idx == -1) {
                return write;
            }
            String motherId = parsedData[table.getColumnIndex("mother_id")];
            write = jdbcTemplate.queryForObject("select count(*) > 0 from mother_information where facility_id = ? and id = ?",
                Boolean.class, Long.parseLong(facilityId), Long.parseLong(motherId));
            idx = table.getColumnIndex("child_id");
            if (idx == -1) {
                return write;
            }
            String childId = parsedData[table.getColumnIndex("child_id")];
            return jdbcTemplate.queryForObject("select count(*) > 0 from child where facility_id = ? and id = ?",
                Boolean.class, Long.parseLong(facilityId), Long.parseLong(childId));
        } catch (Exception e) {
            return false;
        }
    }
}
