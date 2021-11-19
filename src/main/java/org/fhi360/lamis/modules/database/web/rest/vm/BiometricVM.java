package org.fhi360.lamis.modules.database.web.rest.vm;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BiometricVM {
    private String id;

    private PatientVM patient;

    private FacilityVM facility;

    @NotNull
    private byte[] template;

    private String biometricType;

    private String templateType;

    private LocalDate date;

    private Boolean archived = false;

    private Boolean iso = false;

    private LocalDateTime lastModified;

    private JsonNode extra;
}
