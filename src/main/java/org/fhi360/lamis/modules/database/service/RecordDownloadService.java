package org.fhi360.lamis.modules.database.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lamisplus.modules.lamis.legacy.domain.entities.*;
import org.lamisplus.modules.lamis.legacy.domain.repositories.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Example;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Profile("!server")
@Slf4j
public class RecordDownloadService {
    private final EncounterRepository encounterRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PatientRepository patientRepository;
    private final RegimenRepository regimenRepository;
    private final RegimenDrugRepository regimenDrugRepository;
    private final DevolveRepository devolveRepository;
    private final ClinicRepository clinicRepository;
    private final AssessmentRepository assessmentRepository;
    private final HtsRepository htsRepository;
    private final IndexContactRepository indexContactRepository;
    private final BiometricRepository biometricRepository;
    private final JdbcTemplate jdbcTemplate;
    private final static RestTemplate restTemplate = new RestTemplate();
    private final static ObjectMapper OBJECT_MAPPER;

    public void syncEncounter(Long facilityId) {
        LocalDateTime lastModified = jdbcTemplate.queryForObject("select coalesce(max(last_modified), '2000-01-01') " +
            "from encounter where facility_id = ?", LocalDateTime.class, facilityId);


        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(SyncService.PROXY_URL + "/sync/encounters")
            .queryParam("facilityId", facilityId)
            .queryParam("date", lastModified);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Encounter> requestEntity = new HttpEntity<>(requestHeaders);
        ResponseEntity<List<Encounter>> response = restTemplate.exchange(
            uriBuilder.toUriString(),
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<List<Encounter>>() {
            }
        );

        List<Encounter> encounters = response.getBody();
        if (encounters != null) {
            encounters = encounters.stream()
                .map(encounter -> {
                    patientRepository.findByUuid(encounter.getPuuid()).ifPresent(encounter::setPatient);
                    return encounter;
                }).collect(Collectors.toList());

            encounterRepository.saveAll(encounters);

            encounters.forEach(encounter -> {
                Pharmacy pharmacy = pharmacyRepository.findByUuid(encounter.getUuid()).orElse(new Pharmacy());
                pharmacy.setDateVisit(encounter.getDateVisit());
                pharmacy.setNextAppointment(encounter.getNextRefill());
                pharmacy.setPatient(encounter.getPatient());
                pharmacy.setFacility(encounter.getFacility());
                pharmacy.setUuid(encounter.getUuid());

                List<PharmacyLine> lines = new ArrayList<>();
                Regimen reg = new Regimen();
                reg.setDescription(encounter.getRegimen1());
                regimenRepository.findAll(Example.of(reg)).stream()
                    .min(Comparator.comparing(r -> r.getRegimenType().getId()))
                    //regimenRepository.findByDescription(encounter.getRegimen1())
                    .ifPresent(regimen -> {
                        regimenDrugRepository.findByRegimen(regimen).forEach(regimenDrug -> {
                            Drug drug = regimenDrug.getDrug();
                            PharmacyLine line = new PharmacyLine();
                            line.setDuration(encounter.getDuration1());
                            line.setRegimenId(regimen.getId());
                            line.setRegimenTypeId(regimen.getRegimenType().getId());
                            line.setRegimenDrugId(regimenDrug.getId());
                            line.setMorning(drug.getMorning().doubleValue());
                            line.setAfternoon(drug.getAfternoon().doubleValue());
                            line.setEvening(drug.getEvening().doubleValue());

                            lines.add(line);
                        });
                    });

                if (!StringUtils.isEmpty(encounter.getRegimen2())) {
                    reg.setDescription(encounter.getRegimen2());
                    regimenRepository.findAll(Example.of(reg)).stream()
                        .min(Comparator.comparing(r -> r.getRegimenType().getId()))
                        //regimenRepository.findByDescription(encounter.getRegimen2())
                        .ifPresent(regimen -> {
                            regimenDrugRepository.findByRegimen(regimen).forEach(regimenDrug -> {
                                Drug drug = regimenDrug.getDrug();
                                PharmacyLine line = new PharmacyLine();
                                line.setDuration(encounter.getDuration2());
                                line.setRegimenId(regimen.getId());
                                line.setRegimenTypeId(regimen.getRegimenType().getId());
                                line.setRegimenDrugId(regimenDrug.getId());
                                line.setMorning(drug.getMorning().doubleValue());
                                line.setAfternoon(drug.getAfternoon().doubleValue());
                                line.setEvening(drug.getEvening().doubleValue());

                                lines.add(line);
                            });
                        });
                }

                if (!StringUtils.isEmpty(encounter.getRegimen3())) {
                    reg.setDescription(encounter.getRegimen3());
                    regimenRepository.findAll(Example.of(reg)).stream()
                        .min(Comparator.comparing(r -> r.getRegimenType().getId()))
                        //regimenRepository.findByDescription(encounter.getRegimen3())
                        .ifPresent(regimen -> {
                            regimenDrugRepository.findByRegimen(regimen).forEach(regimenDrug -> {
                                Drug drug = regimenDrug.getDrug();
                                PharmacyLine line = new PharmacyLine();
                                line.setDuration(encounter.getDuration3());
                                line.setRegimenId(regimen.getId());
                                line.setRegimenTypeId(regimen.getRegimenType().getId());
                                line.setRegimenDrugId(regimenDrug.getId());
                                line.setMorning(drug.getMorning().doubleValue());
                                line.setAfternoon(drug.getAfternoon().doubleValue());
                                line.setEvening(drug.getEvening().doubleValue());

                                lines.add(line);
                            });
                        });
                }

                if (!StringUtils.isEmpty(encounter.getRegimen4())) {
                    reg.setDescription(encounter.getRegimen4());
                    regimenRepository.findAll(Example.of(reg)).stream()
                        .min(Comparator.comparing(r -> r.getRegimenType().getId()))
                        //regimenRepository.findByDescription(encounter.getRegimen4())
                        .ifPresent(regimen -> {
                            regimenDrugRepository.findByRegimen(regimen).forEach(regimenDrug -> {
                                Drug drug = regimenDrug.getDrug();
                                PharmacyLine line = new PharmacyLine();
                                line.setDuration(encounter.getDuration4());
                                line.setRegimenId(regimen.getId());
                                line.setRegimenTypeId(regimen.getRegimenType().getId());
                                line.setRegimenDrugId(regimenDrug.getId());
                                line.setMorning(drug.getMorning().doubleValue());
                                line.setAfternoon(drug.getAfternoon().doubleValue());
                                line.setEvening(drug.getEvening().doubleValue());

                                lines.add(line);
                            });
                        });
                }

                int duration = lines.stream()
                    .map(line -> {
                        if (line.getDuration() == null) {
                            line.setDuration(30);
                        }
                        return line;
                    })
                    .filter(line -> Arrays.asList(1L, 2L, 3L, 4L, 14L).contains(line.getRegimenTypeId()))
                    .map(PharmacyLine::getDuration).max(Comparator.naturalOrder()).orElse(30);

                String mmd;
                if (duration == 90) {
                    mmd = "MMD-3";
                } else if (duration == 120) {
                    mmd = "MMD-4";
                } else if (duration == 150) {
                    mmd = "MMD-5";
                } else if (duration == 180) {
                    mmd = "MMD-6";
                } else {
                    mmd = null;
                }
                pharmacy.setMmdType(mmd);

                if (!lines.isEmpty()) {
                    pharmacy.setLines(new HashSet<>(lines));
                    pharmacyRepository.save(pharmacy);
                }
            });

            lastModified = jdbcTemplate.queryForObject("select coalesce(max(last_modified), '2000-01-01') " +
                "from devolve where facility_id = ? and cast(extra->>'cparp' as boolean)", LocalDateTime.class, facilityId);

            uriBuilder = UriComponentsBuilder.fromHttpUrl(SyncService.PROXY_URL + "/sync/devolves")
                .queryParam("facilityId", facilityId)
                .queryParam("date", lastModified);
            requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Devolve> requestEntity1 = new HttpEntity<>(requestHeaders);
            ResponseEntity<List<Devolve>> response1 = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                requestEntity1,
                new ParameterizedTypeReference<List<Devolve>>() {
                }
            );
            List<Devolve> devolves = response1.getBody();
            if (devolves != null) {
                devolves.forEach(d -> devolveRepository.findByUuid(d.getUuid()).ifPresent(devolve -> {
                    devolve.setDateReturnedToFacility(d.getDateReturnedToFacility());
                    devolve.setReasonDiscontinued(d.getReasonDiscontinued());
                    devolveRepository.save(devolve);
                }));
            }
        }
    }

    public void updateRecords(Long facilityId) {
        List<Map<String, Object>> records = downLoadRecords(facilityId, lastSynced(facilityId, "patient"), "patients");
        List<Patient> patients = OBJECT_MAPPER.convertValue(records, new TypeReference<List<Patient>>() {
        });
        patients.forEach(patient -> {
            patient.setId(null);
            patient = patientRepository.findByUuid(patient.getUuid()).orElse(patient);
            try {
                patientRepository.save(patient);
            } catch (Exception ignored) {
            }
        });

        records = downLoadRecords(facilityId, lastSynced(facilityId, "clinic"), "clinics");
        List<Clinic> clinics = OBJECT_MAPPER.convertValue(records, new TypeReference<List<Clinic>>() {
        });
        clinics.forEach(clinic -> {
            clinic.setId(null);
            clinic = clinicRepository.findByUuid(clinic.getUuid()).orElse(clinic);
            Patient patient = patientRepository.findByUuid(clinic.getPatient().getUuid()).orElse(null);
            if (patient != null) {
                clinic.setPatient(patient);
                try {
                    clinicRepository.save(clinic);
                } catch (Exception ignored) {
                }
            }
        });
        records = downLoadRecords(facilityId, lastSynced(facilityId, "biometric"), "biometrics");
        List<Biometric> biometrics = OBJECT_MAPPER.convertValue(records, new TypeReference<List<Biometric>>() {
        });
        biometrics.forEach(biometric -> {
            patientRepository.findByUuid(biometric.getPatient().getUuid()).ifPresent(patient -> {
                biometric.setPatient(patient);
                biometricRepository.save(biometric);
            });
        });
        records = downLoadRecords(facilityId, lastSynced(facilityId, "assessment"), "assessments");
        List<Assessment> assessments = OBJECT_MAPPER.convertValue(records, new TypeReference<List<Assessment>>() {
        });
        assessments.forEach(assessment -> {
            assessment.setId(null);
            assessment = assessmentRepository.findByUuid(assessment.getUuid()).orElse(assessment);
            try {
                assessmentRepository.save(assessment);
            } catch (Exception ignored) {
            }
        });
        records = downLoadRecords(facilityId, lastSynced(facilityId, "hts"), "hts");
        List<Hts> hts = OBJECT_MAPPER.convertValue(records, new TypeReference<List<Hts>>() {
        });
        hts.forEach(h -> {
            h.setId(null);
            h = htsRepository.findByUuid(h.getUuid()).orElse(h);
            h.setAssessment(null);
            Assessment assessment = h.getAssessment();
            if (assessment != null) {
                assessment.setId(null);
                assessment = assessmentRepository.findByUuid(assessment.getUuid()).orElse(assessment);
                assessment = assessmentRepository.save(assessment);
                h.setAssessment(assessment);
            }
            try {
                htsRepository.save(h);
            } catch (Exception ignored) {
            }
        });
        records = downLoadRecords(facilityId, lastSynced(facilityId, "index_contact"), "index-contacts");
        List<IndexContact> indexContacts = OBJECT_MAPPER.convertValue(records, new TypeReference<List<IndexContact>>() {
        });
        indexContacts.forEach(indexContact -> {
            indexContact.setId(null);
            indexContact = indexContactRepository.findByUuid(indexContact.getUuid()).orElse(indexContact);
            Hts h = indexContact.getHts();
            if (h != null) {
                h.setId(null);
                h = htsRepository.findByUuid(indexContact.getHts().getUuid()).orElse(h);
                h = htsRepository.save(h);
                indexContact.setHts(h);
                try {
                    indexContactRepository.save(indexContact);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private LocalDateTime lastSynced(Long facilityId, String table) {
        return jdbcTemplate.queryForObject(String.format("select coalesce(max((extra->'source'->>'date')::date), '2000-01-01') " +
            "from %s where facility_id = ?", table), LocalDateTime.class, facilityId);
    }

    private List<Patient> downLoadPatients(Long facilityId, LocalDateTime lastModified) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(SyncService.PROXY_URL + "/sync/patients")
            .queryParam("facilityId", facilityId)
            .queryParam("date", lastModified);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Patient> requestEntity = new HttpEntity<>(requestHeaders);
        ResponseEntity<List<Patient>> response = restTemplate.exchange(
            uriBuilder.toUriString(),
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<List<Patient>>() {
            }
        );
        return response.getBody();
    }

    private List<Assessment> downLoadAssessments(Long facilityId, LocalDateTime lastModified) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(SyncService.PROXY_URL + "/sync/assessments")
            .queryParam("facilityId", facilityId)
            .queryParam("date", lastModified);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Assessment> requestEntity = new HttpEntity<>(requestHeaders);
        ResponseEntity<List<Assessment>> response = restTemplate.exchange(
            uriBuilder.toUriString(),
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<List<Assessment>>() {
            }
        );
        return response.getBody();
    }

    private List<Clinic> downLoadClinics(Long facilityId, LocalDateTime lastModified) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(SyncService.PROXY_URL + "/sync/clinics")
            .queryParam("facilityId", facilityId)
            .queryParam("date", lastModified);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Clinic> requestEntity = new HttpEntity<>(requestHeaders);
        try {
            ResponseEntity<List<Clinic>> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<Clinic>>() {
                }
            );
            return response.getBody();
        } catch (Exception e) {

        }
        return new ArrayList<>();
    }

    private List<IndexContact> downLoadIndexContacts(Long facilityId, LocalDateTime lastModified) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(SyncService.PROXY_URL + "/sync/index-contacts")
            .queryParam("facilityId", facilityId)
            .queryParam("date", lastModified);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<IndexContact> requestEntity = new HttpEntity<>(requestHeaders);
        ResponseEntity<List<IndexContact>> response = restTemplate.exchange(
            uriBuilder.toUriString(),
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<List<IndexContact>>() {
            }
        );
        return response.getBody();
    }

    private List<Hts> downLoadHts(Long facilityId, LocalDateTime lastModified) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(SyncService.PROXY_URL + "/sync/hts")
            .queryParam("facilityId", facilityId)
            .queryParam("date", lastModified);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Hts> requestEntity = new HttpEntity<>(requestHeaders);
        ResponseEntity<List<Hts>> response = restTemplate.exchange(
            uriBuilder.toUriString(),
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<List<Hts>>() {
            }
        );
        return response.getBody();
    }

    private List<Biometric> downLoadBiometrics(Long facilityId, LocalDateTime lastModified) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(SyncService.PROXY_URL + "/sync/biometrics")
            .queryParam("facilityId", facilityId)
            .queryParam("date", lastModified);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Biometric> requestEntity = new HttpEntity<>(requestHeaders);
        ResponseEntity<List<Biometric>> response = restTemplate.exchange(
            uriBuilder.toUriString(),
            HttpMethod.GET,
            requestEntity,
            new ParameterizedTypeReference<List<Biometric>>() {
            }
        );
        return response.getBody();
    }

    private List<Map<String, Object>> downLoadRecords(Long facilityId, LocalDateTime lastModified, String path) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(SyncService.PROXY_URL + "/sync/" + path)
            .queryParam("facilityId", facilityId)
            .queryParam("date", lastModified);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestHeaders);
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }
            );
            return response.getBody();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    //@Scheduled(cron = "0 0/30 * * * ?")
    public void checkAvailableUpdates() {
        try {
            AtomicBoolean download = new AtomicBoolean(false);
            List<Map<String, Object>> updates = downLoadRecords(1L, LocalDateTime.now(), "available-updates");
            List<Map<String, Object>> installed = jdbcTemplate.queryForList("select name, version from module");
            List<Map<String, Object>> downloaded = jdbcTemplate.queryForList("select name, version from module_update");
            if (updates.size() > installed.size() || updates.size() > downloaded.size()) {
                download.set(true);
            }
            if (!download.get()) {
                updates.forEach(m -> {
                    String name = (String) m.get("name");
                    String version = (String) m.get("version");
                    installed.forEach(i -> {
                        if (i.get("name").equals(name) && !i.get("version").equals(version)) {
                            download.set(true);
                        }
                    });
                    downloaded.forEach(i -> {
                        if (i.get("name").equals(name) && !i.get("version").equals(version)) {
                            download.set(true);
                        }
                    });
                });
            }
            if (download.get()) {
                List<String> nodeId = jdbcTemplate.queryForList("select node_id from sym_node_identity", String.class);
                if (!nodeId.isEmpty()) {
                    String sync = "insert into SYM_TABLE_RELOAD_REQUEST (target_node_id, source_node_id, trigger_id, router_id, create_time, create_table, last_update_time) " +
                        " values ('@', '000', 'public.modules_update', 'server_2_facility', current_timestamp, 0, current_timestamp)";
                    jdbcTemplate.update(sync.replaceAll("@", nodeId.get(0)));
                }
            }
        } catch (Exception ignored) {

        }
    }

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.registerModules(new JavaTimeModule());
    }
}
