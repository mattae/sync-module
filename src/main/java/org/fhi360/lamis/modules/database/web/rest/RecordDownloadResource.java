package org.fhi360.lamis.modules.database.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fhi360.lamis.modules.database.web.rest.mapper.BiometricMapper;
import org.fhi360.lamis.modules.database.web.rest.mapper.ClinicMapper;
import org.fhi360.lamis.modules.database.web.rest.vm.BiometricVM;
import org.fhi360.lamis.modules.database.web.rest.vm.ClinicVM;
import org.lamisplus.modules.lamis.legacy.domain.entities.*;
import org.lamisplus.modules.lamis.legacy.domain.repositories.*;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sync")
@Profile("server")
@Slf4j
public class RecordDownloadResource {
    private final EncounterRepository encounterRepository;
    private final DevolveRepository devolveRepository;
    private final PatientRepository patientRepository;
    private final AssessmentRepository assessmentRepository;
    private final BiometricRepository biometricRepository;
    private final HtsRepository htsRepository;
    private final ClinicRepository clinicRepository;
    private final IndexContactRepository indexContactRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ClinicMapper clinicMapper;
    private final BiometricMapper biometricMapper;

    @GetMapping("/encounters")
    public List<Encounter> getEncounters(@RequestParam Long facilityId, @RequestParam LocalDateTime date) {
        Facility facility = new Facility();
        facility.setId(facilityId);
        return encounterRepository.findByFacilityAndLastModifiedAfter(facility, date).stream()
            .map(encounter -> {
                try {
                    Patient p = encounter.getPatient();
                    if (p != null) {
                        patientRepository.findById(p.getId()).ifPresent(patient -> encounter.setPuuid(patient.getUuid()));
                    }
                } catch (Exception ignored) {
                }
                return encounter;
            })
            .filter(encounter -> encounter.getPuuid() != null && encounter.getPatient() != null
                && Objects.equals(encounter.getPatient().getFacility(), encounter.getFacility()))
            .collect(toList());
    }

    @GetMapping("/patients")
    public List<Patient> getPatients(@RequestParam Long facilityId, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam LocalDateTime date) {
        return patientRepository.getMobilePatients(facilityId, date.toLocalDate().minusWeeks(2));
    }

    @GetMapping("/biometrics")
    public List<BiometricVM> getBiometrics(@RequestParam Long facilityId, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam LocalDateTime date) {
        return biometricMapper.listToVm(
            biometricRepository.getMobileBiometric(facilityId, date.toLocalDate().minusWeeks(2)).stream()
                .filter(biometric -> biometric.getPatient() != null)
                .collect(toList())
        );
    }

    @GetMapping("/clinics")
    public List<ClinicVM> getClinics(@RequestParam Long facilityId, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam LocalDateTime date) {
        return clinicMapper.listToVms(
            clinicRepository.getMobileClinics(facilityId, date.toLocalDate().minusWeeks(2)).stream()
                .filter(biometric -> biometric.getPatient() != null)
                .collect(toList())
        );
    }

    @GetMapping("/hts")
    public List<Hts> getHts(@RequestParam Long facilityId, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam LocalDateTime date) {
        return htsRepository.getMobileHts(facilityId, date.toLocalDate().minusWeeks(2)).stream()
            .map(hts -> {
                Assessment assessment = hts.getAssessment();
                if (assessment != null) {
                    Assessment a = new Assessment();
                    a.setUuid(assessment.getUuid());
                    hts.setAssessment(a);
                }
                return hts;
            })
            .collect(toList());
    }

    @GetMapping("/assessments")
    public List<Assessment> getAssessments(@RequestParam Long facilityId, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam LocalDateTime date) {
        return assessmentRepository.getMobileAssessments(facilityId, date.toLocalDate().minusWeeks(2));
    }

    @GetMapping("/index-contacts")
    public List<IndexContact> getIndexContacts(@RequestParam Long facilityId, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam LocalDateTime date) {
        //return indexContactRepository.getMobileIndexContacts(facilityId, date.toLocalDate().minusWeeks(2));
        return new ArrayList<>();
    }

    @GetMapping("/devolves")
    public List<Devolve> getDevolves(@RequestParam Long facilityId, @RequestParam LocalDateTime date) {
        Facility facility = new Facility();
        facility.setId(facilityId);

        return devolveRepository.findByFacilityAndLastModifiedAfter(facility, date).stream()
            .filter(devolve -> {
                JsonNode extra = devolve.getExtra();
                return extra != null && (extra.get("cparp") != null || !(extra.get("cparp") instanceof NullNode));
            }).collect(toList());
    }

    @GetMapping("/last-successful-sync")
    public LocalDateTime getLastSuccessfulSync(@RequestParam String nodeId) {
        try {
            return jdbcTemplate.queryForObject("select last_sync from upload_status where node_id = ?", LocalDateTime.class, nodeId);
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/available-updates")
    public List<Map<String, Object>> getAvailableUpdates() {
        return jdbcTemplate.queryForList("select name, version from module_update");
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    public void update() {
        jdbcTemplate.update("delete from update_status where node_id = '@@@'");
        jdbcTemplate.update("update hts set assessment_id = null where assessment_id not in " +
            "(select id from assessment where archived = false)");
    }
}
