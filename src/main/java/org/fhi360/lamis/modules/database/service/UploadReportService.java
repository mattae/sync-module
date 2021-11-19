package org.fhi360.lamis.modules.database.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.lamisplus.modules.base.service.PrinceXMLService;
import org.lamisplus.modules.security.config.security.SecurityUtils;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UploadReportService {
    private final JdbcTemplate jdbcTemplate;
    private final PrinceXMLService princeXMLService;
    private final ITemplateEngine templateEngine;
    private final Environment environment;

    @SneakyThrows
    public ByteArrayOutputStream getUploadReport(Long stateId, int format) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String port = environment.getProperty("local.server.port");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("title", String.format("%s Database Upload/ Biometric Report", getState(stateId)));
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyyy");

        final long[] totalActives = {0};
        final long[] totalBiometrics = {0};
        final long[] totalInterruptions = {0};
        final long[] totalDeaths = {0};
        final long[] totalTransferred = {0};
        final long[] totalStopped = {0};
        final long[] totalEnrolled = {0};
        final long[] totalActiveEnrolled = {0};
        final long[] totalStarted = {0};
        List<Map<String, Object>> biometrics = getBiomericsEnrollement(stateId);
        List<Map<String, Object>> actives = getActiveCount(stateId);
        List<Map<String, Object>> interruptions = getInterruptions(stateId);
        List<Map<String, Object>> stopped = getStoppedTreatment(stateId);
        List<Map<String, Object>> deaths = getDeaths(stateId);
        List<Map<String, Object>> transferred = getTransferOut(stateId);
        List<Map<String, Object>> enrolled = getEverEnrolled(stateId);
        List<Map<String, Object>> activesEnrolled = getActivePatients(stateId);
        List<Map<String, Object>> backstops = getBackstops(stateId);
        List<Map<String, Object>> started = getStartedOnArt(stateId);
        List<Map<String, Object>> validEnrollment = getValidEnrollment(stateId);
        //Map<String, List<Map<String, Object>>> dataSource = getSyncReport(stateId).stream()
        List<Map<String, Object>> dataSource = getSyncReport(stateId).stream()
            .map(r -> {
                Long facilityId = (Long) r.get("id");
                boolean complete = moduleCountOk(facilityId) && versionsOk(facilityId);
                r.put("complete", complete);

                r.put("backstop", "");
                backstops.forEach(b -> {
                    long fac = (long) b.get("facility_id");
                    if (Objects.equals(facilityId, fac)) {
                        String name = (String) b.get("name");
                        r.put("backstop", name);
                    }
                });

                r.put("actives", 0L);
                actives.forEach(b -> {
                    long fac2 = (long) b.get("facility_id");
                    if (Objects.equals(facilityId, fac2)) {
                        long status = (long) b.get("actives");
                        totalActives[0] += status;
                        r.put("actives", status);
                    }
                });

                r.put("interruptions", 0);
                interruptions.forEach(b -> {
                    long fac2 = (long) b.get("facility_id");
                    if (Objects.equals(facilityId, fac2)) {
                        long status = (long) b.get("interruptions");
                        totalInterruptions[0] += status;
                        r.put("interruptions", status);
                    }
                });

                r.put("stopped", 0);
                stopped.forEach(b -> {
                    long fac2 = (long) b.get("facility_id");
                    if (Objects.equals(facilityId, fac2)) {
                        long status = (long) b.get("stopped");
                        totalStopped[0] += status;
                        r.put("stopped", status);
                    }
                });

                r.put("enrolled", 0);
                enrolled.forEach(b -> {
                    long fac2 = (long) b.get("facility_id");
                    if (Objects.equals(facilityId, fac2)) {
                        long status = (long) b.get("enrolled");
                        totalEnrolled[0] += status;
                        r.put("enrolled", status);
                    }
                });

                r.put("valid", 0);
                validEnrollment.forEach(b -> {
                    long fac2 = (long) b.get("facility_id");
                    if (Objects.equals(facilityId, fac2)) {
                        long status = (long) b.get("correct");
                        totalEnrolled[0] += status;
                        r.put("valid", status);
                    }
                });


                r.put("deaths", 0);
                deaths.forEach(b -> {
                    long fac2 = (long) b.get("facility_id");
                    if (Objects.equals(facilityId, fac2)) {
                        long status = (long) b.get("deaths");
                        totalDeaths[0] += status;
                        r.put("deaths", status);
                    }
                });

                r.put("transferred", 0);
                transferred.forEach(b -> {
                    long fac2 = (long) b.get("facility_id");
                    if (Objects.equals(facilityId, fac2)) {
                        long status = (long) b.get("transferred");
                        totalTransferred[0] += status;
                        r.put("transferred", status);
                    }
                });

                r.put("activeEnrolled", 0);
                activesEnrolled.forEach(b -> {
                    long fac2 = (long) b.get("facility_id");
                    if (Objects.equals(facilityId, fac2)) {
                        long status = (long) b.get("active_enrolled");
                        totalActiveEnrolled[0] += status;
                        r.put("activeEnrolled", status);
                    }
                });

                r.put("startedOnArt", 0);
                started.forEach(b -> {
                    long fac2 = (long) b.get("facility_id");
                    if (Objects.equals(facilityId, fac2)) {
                        long status = (long) b.get("started");
                        totalActiveEnrolled[0] += status;
                        r.put("startedOnArt", status);
                    }
                });

                r.put("biometrics", 0);
                r.put("coverage", 0);
                biometrics.forEach(b -> {
                    long fac2 = (long) b.get("facility_id");
                    if (Objects.equals(facilityId, fac2)) {
                        long biometric = (long) b.get("biometrics");
                        totalBiometrics[0] += biometric;
                        r.put("biometrics", biometric);
                        double coverage = 0;
                        try {
                            coverage = biometric / ((((long) r.get("actives")) + ((long) r.get("interruptions"))) * 1.0);
                        } catch (Exception ignored) {
                        }
                        r.put("coverage", coverage);
                    }
                });
                return r;
            })
            .collect(Collectors.toList());
                /*.collect(groupingBy(e -> {
                    Date date = (Date) e.get("last_sync");
                    return dateFormat.format(date);
                }))
                .entrySet()
                .stream()
                .sorted((e1, e2) -> {
                    try {
                        return dateFormat.parse(e2.getKey()).compareTo(dateFormat.parse(e1.getKey()));
                    } catch (ParseException e) {
                        throw new RuntimeException();
                    }
                })
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));*/
        double totalCoverage = (totalBiometrics[0] * 1.0) / totalEnrolled[0];
        Context context = new Context();
        context.setVariables(parameters);
        context.setVariable("css", "http://localhost:" + port + "/across/resources/static/database/css/style.css");
        context.setVariable("cancel", "http://localhost:" + port + "/across/resources/static/database/img/cancel.png");
        context.setVariable("correct", "http://localhost:" + port + "/across/resources/static/database/img/ok.png");
        context.setVariable("datasource", dataSource);
        context.setVariable("today", new Date());
        context.setVariable("biometrics", totalBiometrics[0]);
        context.setVariable("actives", totalActives[0]);
        context.setVariable("enrolled", totalEnrolled[0]);
        context.setVariable("transferred", totalTransferred[0]);
        context.setVariable("deaths", totalDeaths[0]);
        context.setVariable("stopped", totalStopped[0]);
        context.setVariable("interruptions", totalInterruptions[0]);
        context.setVariable("activeEnrolled", totalActiveEnrolled[0]);
        context.setVariable("coverage", totalCoverage);
        if (format == 1) {
            return excelReport(dataSource, getState(stateId));
        }
        String output = templateEngine.process("templates/upload_report", context);
        princeXMLService.convert(IOUtils.toInputStream(output), baos);
        return baos;
    }

    private String getState(Long id) {
        return jdbcTemplate.queryForObject("select name from state where id = ?", String.class, id);
    }

    private List<Map<String, Object>> getSyncReport(Long stateId) {
        return jdbcTemplate.queryForList("select id, name, last_sync from upload_status join facility on " +
            "id = node_id::int where id in (select id from facility where state_id = ?) order by 2", stateId);
    }

    public List<Map<String, Object>> getStates() {
        List<Map<String, Object>> states = jdbcTemplate.queryForList("select id, name from state where id in (select distinct state_id from " +
            "pharmacy p join facility f on f.id = facility_id)");

        Long stateId = null;
        String username = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (username == null) {
            return new ArrayList<>();
        }
        try {
            stateId = jdbcTemplate.queryForObject("select (extra->>'state')::int from users where lower(login) = ?", Long.class,
                StringUtils.lowerCase(username));
        } catch (Exception ignored) {
        }

        if (stateId != null) {
            Long finalStateId = stateId;
            states = states.stream()
                .filter(s -> {
                    Long id = (Long) s.get("id");
                    return Objects.equals(id, finalStateId);
                })
                .collect(Collectors.toList());
        }
        return states;
    }

    private List<Map<String, Object>> getActiveCount(Long stateId) {
        String query = "" +
            "select facility_id, count(*) actives from (" +
            "   select * from (" +
            "       select p.facility_id, patient_id, row_number() over(partition by patient_id order by date_visit desc ) pos, " +
            "           first_value(date_visit + cast(jsonb_extract_path_text(l,'duration') as integer) + INTERVAL '28 DAYS' < current_date) " +
            "           over(partition by patient_id order by date_visit desc ) ltfu" +
            "       from pharmacy p join patient o on patient_id = o.id, jsonb_array_elements(lines) with ordinality a(l) " +
            "           where cast(jsonb_extract_path_text(l,'regimen_type_id') as integer)" +
            "           in (1,2,3,4,14) and date_visit between current_date - interval '8 months' and current_date and " +
            "           p.archived = false and o.archived = false and cast(o.extra->>'art' as boolean) = true and" +
            "           o.facility_id in (select id from facility where state_id = ?)" +
            "   ) act where pos = 1 and patient_id not in (" +
            "       select patient_id from (" +
            "           select * from (" +
            "               select s.facility_id, patient_id, status, row_number() over(partition by patient_id order by date_status desc ) pos, " +
            "                   first_value(date_status) over(partition by patient_id order by date_status desc ) date_status" +
            "               from status_history s join patient o on patient_id = o.id where date_status between " +
            "                   current_date - interval '8 months' and current_date and s.archived = false and o.archived = false" +
            "                   and o.facility_id in (select id from facility where state_id = ?)" +
            "               ) act where pos = 1" +
            "           ) a where status in ('ART_TRANSFER_OUT', 'KNOWN_DEATH', 'STOPPED_TREATMENT') " +
            "       )" +
            ") a where ltfu = false group by 1 order by 2";
        return jdbcTemplate.queryForList(query, stateId, stateId);
    }

    private List<Map<String, Object>> getActivePatients(Long stateId) {
        String query = "" +
            "select facility_id, count(*) active_enrolled from (" +
            "    select facility_id, patient_id actives from (" +
            "        select * from (" +
            "            select p.facility_id, patient_id, row_number() over(partition by patient_id order by date_visit desc ) pos, " +
            "               first_value(date_visit + cast(jsonb_extract_path_text(l,'duration') as integer) + INTERVAL '28 DAYS' < current_date) " +
            "               over(partition by patient_id order by date_visit desc ) ltfu" +
            "            from pharmacy p join patient o on patient_id = o.id, jsonb_array_elements(lines) with ordinality a(l) " +
            "               where cast(jsonb_extract_path_text(l,'regimen_type_id') as integer)" +
            "               in (1,2,3,4,14) and date_visit between current_date - interval '8 months' and current_date and " +
            "               p.archived = false and o.archived = false and cast(o.extra->>'art' as boolean) = true and " +
            "               o.facility_id in (select id from facility where state_id = ?)" +
            "        ) act where pos = 1 and patient_id not in (" +
            "            select patient_id from (" +
            "                select * from (" +
            "                    select s.facility_id, patient_id, status, row_number() over(partition by patient_id order by date_status desc ) pos, " +
            "                        first_value(date_status) over(partition by patient_id order by date_status desc ) date_status" +
            "                    from status_history s join patient o on patient_id = o.id where date_status between " +
            "                       current_date - interval '8 months' and current_date and s.archived = false and o.archived = false" +
            "                       and o.facility_id in (select id from facility where state_id = ?)" +
            "                   ) act where pos = 1" +
            "              ) a where status in ('ART_TRANSFER_OUT', 'KNOWN_DEATH', 'STOPPED_TREATMENT')" +
            "          )" +
            "    ) a where ltfu = false " +
            "" +
            "    intersect" +
            "" +
            "    select distinct b.facility_id, p.id biometrics from biometric b join patient p on p.uuid = patient_id where p.archived = false and " +
            "        b.archived = false and b.facility_id in (select id from facility where state_id = ?)" +
            ") ae group by 1 ";
        return jdbcTemplate.queryForList(query, stateId, stateId, stateId);
    }

    private List<Map<String, Object>> getStartedOnArt(Long stateId) {
        String query = "" +
            "select facility_id, count(*) started from patient join facility f on f.id = facility_id where " +
            "   date_started is not null and (extra->>'art')::bool = true and state_id = ? group by 1";
        return jdbcTemplate.queryForList(query, stateId);
    }

    private List<Map<String, Object>> getInterruptions(Long stateId) {
        String query = "" +
            "select facility_id, count(*) interruptions from (" +
            "   select * from (" +
            "       select p.facility_id, patient_id, row_number() over(partition by patient_id order by date_visit desc ) pos, " +
            "           first_value(date_visit + cast(jsonb_extract_path_text(l,'duration') as integer) + INTERVAL '28 DAYS' < current_date) " +
            "           over(partition by patient_id order by date_visit desc ) ltfu" +
            "       from pharmacy p join patient o on patient_id = o.id, jsonb_array_elements(lines) with ordinality a(l) where " +
            "           cast(jsonb_extract_path_text(l,'regimen_type_id') as integer) in (1,2,3,4,14) and p.archived = false and " +
            "           o.archived = false and cast(o.extra->>'art' as boolean) = true and o.facility_id in (select id from " +
            "           facility where state_id = ?)" +
            "   ) act where pos = 1 and patient_id not in (" +
            "       select patient_id from (" +
            "           select * from (" +
            "               select s.facility_id, patient_id, status, row_number() over(partition by patient_id order by date_status desc ) pos, " +
            "                   first_value(date_status) over(partition by patient_id order by date_status desc ) date_status" +
            "               from status_history s join patient o on patient_id = o.id where s.archived = false and o.archived = false" +
            "                   and o.facility_id in (select id from facility where state_id = ?)" +
            "               ) act where pos = 1" +
            "           ) a where status in ('ART_TRANSFER_OUT', 'KNOWN_DEATH', 'STOPPED_TREATMENT')" +
            "       )" +
            ") a where ltfu = true group by 1 order by 2";
        return jdbcTemplate.queryForList(query, stateId, stateId);
    }

    private List<Map<String, Object>> getStoppedTreatment(Long stateId) {
        String query = "" +
            "select facility_id, count(*) stopped from (" +
            "       select patient_id, facility_id from (" +
            "           select * from (" +
            "               select s.facility_id, patient_id, status, row_number() over(partition by patient_id order by date_status desc ) pos, " +
            "                   first_value(date_status) over(partition by patient_id order by date_status desc ) date_status" +
            "               from status_history s join patient o on patient_id = o.id where s.archived = false " +
            "                   and o.archived = false and cast(o.extra->>'art' as boolean) = true and " +
            "                   o.facility_id in (select id from facility where state_id = ?)" +
            "           ) act where pos = 1" +
            "       ) a where status = 'STOPPED_TREATMENT' " +
            ") a  group by 1";
        return jdbcTemplate.queryForList(query, stateId);
    }

    private List<Map<String, Object>> getEverEnrolled(Long stateId) {
        String query = "" +
            "select facility_id, count(*) enrolled from patient where facility_id in " +
            "   (select id from facility where state_id = ?) and cast(extra->>'art' as boolean) = true group by 1";
        return jdbcTemplate.queryForList(query, stateId);
    }

    private List<Map<String, Object>> getTransferOut(Long stateId) {
        String query = "" +
            "select facility_id, count(*) transferred from (" +
            "       select patient_id, facility_id from (" +
            "           select * from (" +
            "               select s.facility_id, patient_id, status, row_number() over(partition by patient_id order by date_status desc ) pos, " +
            "                   first_value(date_status) over(partition by patient_id order by date_status desc ) date_status" +
            "               from status_history s join patient o on o.id = patient_id where s.archived = false and " +
            "                   o.archived = false and cast(o.extra->>'art' as boolean) = true and " +
            "                   o.facility_id in (select id from facility where state_id = ?)" +
            "           ) act where pos = 1" +
            "       ) a where status = 'ART_TRANSFER_OUT' " +
            ") a group by 1";
        return jdbcTemplate.queryForList(query, stateId);
    }

    private List<Map<String, Object>> getBiomericsEnrollement(Long stateId) {
        String query = "" +
            "select p.facility_id, count(distinct patient_id) biometrics from biometric b join patient p on p.uuid = patient_id" +
            "   where b.archived = false and p.archived = false and p.facility_id in (select id from facility where state_id = ?) group by 1";
        return jdbcTemplate.queryForList(query, stateId);
    }

    private List<Map<String, Object>> getValidEnrollment(Long stateId) {
        String query = "" +
            "select facility_id, count(*) correct from (" +
            "   with iso(patient_id, template_type, facility_id) as (" +
            "       select patient_id, template_type, p.facility_id from biometric b join facility f on f.id = facility_id " +
            "           join patient p on p.uuid = patient_id where state_id = ? and iso = true and b.archived = false and p.archived = false" +
            "   )" +
            "   select patient_id, facility_id, count(distinct upper(substring(template_type, 0, 4))) from iso group by 1, 2 " +
            "       having count(distinct upper(substring(template_type, 0, 4))) = 2" +
            ") a group by 1";

        return jdbcTemplate.queryForList(query, stateId);
    }

    private List<Map<String, Object>> getBackstops(Long stateId) {
        String query = "" +
            "select facility_id, name from facility_backstop where facility_id in (select id from facility where " +
            "   state_id = ?)";
        return jdbcTemplate.queryForList(query, stateId);
    }

    private List<Map<String, Object>> getDeaths(Long stateId) {
        String query = "" +
            "select facility_id, count(*) deaths from (" +
            "       select patient_id, facility_id from (" +
            "           select * from (" +
            "               select s.facility_id, patient_id, status, row_number() over(partition by patient_id order by date_status desc ) pos, " +
            "                   first_value(date_status) over(partition by patient_id order by date_status desc ) date_status" +
            "                   from status_history s join patient o on patient_id = o.id where s.archived = false " +
            "                       and o.archived = false and cast(o.extra->>'art' as boolean) = true and " +
            "                       o.facility_id in (select id from facility where state_id = ?)" +
            "           ) act where pos = 1" +
            "       ) a where status = 'KNOWN_DEATH' " +
            ") a group by 1";
        return jdbcTemplate.queryForList(query, stateId);
    }

    private boolean moduleCountOk(Long facilityId) {
        return jdbcTemplate.queryForObject("" +
            "with node_count(nc) as (" +
            "   select count(distinct name) from update_status where node_id::int = ?" +
            ")," +
            "server_count(sc) as (" +
            "   select count(*) from module_update" +
            ") " +
            "" +
            "select nc >= sc from node_count, server_count", Boolean.class, facilityId);
    }

    private boolean versionsOk(Long facilityId) {
        return jdbcTemplate.queryForObject("" +
                "with node_status(name, version) as ( " +
                "   select name, max(version) from update_status where node_id::int = ? group by 1" +
                ") " +
                "select count(*) = 0 from node_status s, module_update m where s.name = m.name and s.version != m.version",
            Boolean.class, facilityId);
    }

    private ByteArrayOutputStream excelReport(List<Map<String, Object>> dataSource, String state) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Workbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = (XSSFSheet) workbook.createSheet();

            //Create a new font
            Font font = workbook.createFont();
            font.setFontHeightInPoints((short) 12);
            font.setBold(true);
            font.setColor(HSSFColor.HSSFColorPredefined.BLACK.getIndex());

            //Create a style and set the font into it
            CellStyle style = getCellStyle(workbook);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
            style.setBorderRight(BorderStyle.THIN);
            style.setRightBorderColor(IndexedColors.BLACK.getIndex());
            style.setBorderLeft(BorderStyle.THIN);
            style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
            style.setBorderTop(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.BLACK.getIndex());

            CellStyle numericStyle = workbook.createCellStyle();
            numericStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat("#,##0"));
            sheet.setDefaultColumnStyle(0, numericStyle);
            sheet.setDefaultColumnStyle(1, numericStyle);
            sheet.setDefaultColumnStyle(5, numericStyle);
            sheet.setDefaultColumnStyle(6, numericStyle);
            sheet.setDefaultColumnStyle(7, numericStyle);
            sheet.setDefaultColumnStyle(8, numericStyle);
            sheet.setDefaultColumnStyle(9, numericStyle);
            sheet.setDefaultColumnStyle(10, numericStyle);
            sheet.setDefaultColumnStyle(11, numericStyle);
            sheet.setDefaultColumnStyle(12, numericStyle);
            sheet.setDefaultColumnStyle(13, numericStyle);
            CellStyle percentageStyle = workbook.createCellStyle();
            percentageStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat("0.00%"));
            sheet.setDefaultColumnStyle(15, percentageStyle);

            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM, yyyy HH:mm");

            int rowNum = 0;
            int cellNum = 0;
            Row row = sheet.createRow(rowNum);
            Cell cell = row.createCell(14);
            cell.setCellValue("Run at:");
            CellUtil.setAlignment(cell, HorizontalAlignment.RIGHT);
            cell = row.createCell(15);
            cell.setCellValue(dateFormat.format(new Date()));
            CellUtil.setAlignment(cell, HorizontalAlignment.LEFT);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 15, 16));
            row = sheet.createRow(++rowNum);
            cell = row.createCell(cellNum);
            cell.setCellValue(String.format("%s Database Upload /Biometric Report", "Cross River"));
            cell.setCellStyle(style);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 16));
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            row = sheet.createRow(++rowNum);
            cellNum = 0;
            cell = row.createCell(cellNum);
            cell.setCellValue("SN");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("ID");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("Name");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("Last Update");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("LAMIS Version");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("PLHIV enrolled into care");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("Started on ART");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("TX_CURR");
            cell.setCellStyle(style);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 7, 11));
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cellNum = 12;
            cell = row.createCell((cellNum));
            cell.setCellValue("Biometric Status");
            cell.setCellStyle(style);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 12, 15));
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cellNum = 16;
            cell = row.createCell((cellNum));
            cell.setCellValue("Backstop");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            row = sheet.createRow(++rowNum);
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 1, 1));
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 2, 2));
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 3, 3));
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 4, 4));
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 5, 5));
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 6, 6));
            cellNum = 7;
            cell = row.createCell(cellNum);
            cell.setCellValue("Active");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("Interruption In Treatment");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("Transferred Out");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("Stopped");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("Dead");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("Active clients enrolled");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("Total Enrolled");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("Valid Enrollment");
            cell.setCellStyle(style);
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);
            cell = row.createCell(++cellNum);
            cell.setCellValue("Coverage");
            cell.setCellStyle(style);
            sheet.addMergedRegion(new CellRangeAddress(2, 3, 16, 16));
            CellUtil.setAlignment(cell, HorizontalAlignment.CENTER);
            CellUtil.setVerticalAlignment(cell, VerticalAlignment.CENTER);

            AtomicInteger sn = new AtomicInteger(1);
            AtomicInteger rn = new AtomicInteger(++rowNum);
            AtomicInteger cn = new AtomicInteger(0);
            dataSource.forEach(r -> {
                Row rw = sheet.createRow(rn.getAndIncrement());

                Cell c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(sn.getAndIncrement());
                CellStyle cellStyle = workbook.createCellStyle();
                cellStyle.cloneStyleFrom(style);
                cellStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat("#,##0"));
                c.setCellStyle(cellStyle);

                long facilityId = Long.parseLong(r.get("id").toString());
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(facilityId);
                cellStyle = workbook.createCellStyle();
                cellStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat("###0"));
                c.setCellStyle(cellStyle);

                String name = (String) r.get("name");
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(name);

                Date lastSync = (Date) r.get("last_sync");
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(dateFormat.format(lastSync));

                Boolean complete = (Boolean) r.get("complete");
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue((complete ? "Yes" : "No"));
                CellStyle cellStyle1 = workbook.createCellStyle();
                cellStyle1.setFillForegroundColor(IndexedColors.GREEN.getIndex());
                cellStyle1.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                if (!complete) {
                    cellStyle1.setFillForegroundColor(IndexedColors.RED.getIndex());
                }
                c.setCellStyle(cellStyle1);

                long enrolled = Long.parseLong(r.get("enrolled").toString());
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(enrolled);

                long started = Long.parseLong(r.get("startedOnArt").toString());
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(started);

                long actives = Long.parseLong(r.get("actives").toString());
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(actives);

                long interruptions = Long.parseLong(r.get("interruptions").toString());
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(interruptions);

                long transferred = Long.parseLong(r.get("transferred").toString());
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(transferred);

                long stopped = Long.parseLong(r.get("stopped").toString());
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(stopped);

                long deaths = Long.parseLong(r.get("deaths").toString());
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(deaths);

                long activeEnrolled = Long.parseLong(r.get("activeEnrolled").toString());
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(activeEnrolled);

                long biometrics = Long.parseLong(r.get("biometrics").toString());
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(biometrics);

                long valid = Long.parseLong(r.get("valid").toString());
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(valid);

                double coverage = Double.parseDouble(r.get("coverage").toString());
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(coverage);
                int ri = rn.get();
                c.setCellFormula(String.format("N%s/(I%s+G%s)", ri, ri, ri));
                evaluateCellFormula(workbook, c);

                String backstop = (String) r.get("backstop");
                c = rw.createCell(cn.getAndIncrement());
                c.setCellValue(backstop);

                cn.set(0);
            });
            row = sheet.createRow(rn.get());
            cellNum = 0;
            cell = row.createCell(cellNum);
            cell.setCellValue("Total:");
            cell.setCellStyle(style);
            sheet.addMergedRegion(new CellRangeAddress(rn.get(), rn.get(), 0, cellNum + 4));
            CellUtil.setAlignment(cell, HorizontalAlignment.RIGHT);

            style.setDataFormat((short) BuiltinFormats.getBuiltinFormat("#,##0"));

            int ri = rn.get() + 1;
            cellNum = 4;
            cell = row.createCell(++cellNum);
            cell.setCellFormula(String.format("SUM(F5:F%s)", rn));
            evaluateCellFormula(workbook, cell);
            cell.setCellStyle(style);

            cell = row.createCell(++cellNum);
            cell.setCellFormula(String.format("SUM(G5:G%s)", rn));
            evaluateCellFormula(workbook, cell);
            cell.setCellStyle(style);

            cell = row.createCell(++cellNum);
            cell.setCellFormula(String.format("SUM(H5:H%s)", rn));
            evaluateCellFormula(workbook, cell);
            cell.setCellStyle(style);

            cell = row.createCell(++cellNum);
            cell.setCellFormula(String.format("SUM(I5:I%s)", rn));
            evaluateCellFormula(workbook, cell);
            cell.setCellStyle(style);

            cell = row.createCell(++cellNum);
            cell.setCellFormula(String.format("SUM(J5:J%s)", rn));
            evaluateCellFormula(workbook, cell);
            cell.setCellStyle(style);

            cell = row.createCell(++cellNum);
            cell.setCellFormula(String.format("SUM(K5:K%s)", rn));
            evaluateCellFormula(workbook, cell);
            cell.setCellStyle(style);

            cell = row.createCell(++cellNum);
            cell.setCellFormula(String.format("SUM(L5:L%s)", rn));
            evaluateCellFormula(workbook, cell);
            cell.setCellStyle(style);

            cell = row.createCell(++cellNum);
            cell.setCellFormula(String.format("SUM(M5:M%s)", rn));
            evaluateCellFormula(workbook, cell);
            cell.setCellStyle(style);

            cell = row.createCell(++cellNum);
            cell.setCellFormula(String.format("SUM(N5:N%s)", rn));
            evaluateCellFormula(workbook, cell);
            cell.setCellStyle(style);

            cell = row.createCell(++cellNum);
            cell.setCellFormula(String.format("SUM(O5:O%s)", rn));
            evaluateCellFormula(workbook, cell);
            cell.setCellStyle(style);

            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.cloneStyleFrom(style);
            cellStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat("0.00%"));
            cell = row.createCell(++cellNum);
            cell.setCellFormula(String.format("N%s/(H%s+I%s)", ri, ri, ri));
            cell.setCellStyle(cellStyle);
            evaluateCellFormula(workbook, cell);

            try {
                workbook.write(baos);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos;
    }

    private CellStyle getCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        style.setFillForegroundColor(HSSFColor.HSSFColorPredefined.WHITE.getIndex());
        style.setFillBackgroundColor(HSSFColor.HSSFColorPredefined.BLUE.getIndex());
        style.setFillPattern(FillPatternType.FINE_DOTS);
        style.setFont(font);
        return style;
    }

    private void evaluateCellFormula(Workbook workbook, Cell cell) {
        FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
        formulaEvaluator.evaluateFormulaCell(cell);
    }
}
