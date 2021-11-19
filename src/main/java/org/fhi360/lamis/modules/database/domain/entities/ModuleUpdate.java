package org.fhi360.lamis.modules.database.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.domain.Persistable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Date;

@Data
@Entity
public class ModuleUpdate implements Serializable, Persistable<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String version;

    private String fileName;

    @JsonIgnore
    private byte[] data;

    private Boolean install = true;

    private Boolean uninstall;

    private Date buildTime;

    @Override
    public boolean isNew() {
        return id == null;
    }
}
