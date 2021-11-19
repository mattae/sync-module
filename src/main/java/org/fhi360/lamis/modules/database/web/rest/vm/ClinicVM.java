package org.fhi360.lamis.modules.database.web.rest.vm;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.lamisplus.modules.lamis.legacy.domain.entities.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
public class ClinicVM {
    protected Long id;

    protected LocalDateTime lastModified;

    protected String uuid;

    protected FacilityVM facility;

    protected Boolean archived = false;

    private PatientVM patient;

    private LocalDate dateVisit;

    private String clinicStage;

    private String funcStatus;

    private String tbStatus;

    private Double viralLoad;

    private Double cd4;

    private Double cd4p;

    private RegimenType regimenType;

    private Regimen regimen;

    private Double bodyWeight;

    private Double height;

    private Double waist;

    private String bp;

    private LocalDate lmp;

    private Boolean breastfeeding;

    private String oiScreened;

    private String stiIds;

    private String stiTreated;

    Set<OpportunisticInfection> opportunisticInfections = new HashSet<>();

    private String adrScreened;

    Set<ClinicAdverseDrugReaction> adverseDrugReactions = new HashSet<>();

    private String adherenceLevel;

    Set<Adhere> adheres = new HashSet<>();

    private Boolean commence = false;

    private LocalDate nextAppointment;

    private String notes;

    private String gestationalAge;

    private String maternalStatusArt;

    private JsonNode extra;
}
