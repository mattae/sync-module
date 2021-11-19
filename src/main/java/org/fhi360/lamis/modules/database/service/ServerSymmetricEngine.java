package org.fhi360.lamis.modules.database.service;

import com.foreach.across.core.AcrossContext;
import org.jumpmind.security.SecurityServiceFactory;
import org.lamisplus.modules.base.config.ApplicationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@Profile("server")
@DependsOn({"updateStatusService", "syncService"})
public class ServerSymmetricEngine extends ClientSymmetricEngine {
    public ServerSymmetricEngine(DataSource dataSource, JdbcTemplate jdbcTemplate,
                                 ApplicationProperties applicationProperties, AcrossContext acrossContext) {
        super(dataSource, jdbcTemplate, applicationProperties, acrossContext);
        setDeploymentType("server");
        PROPERTIES_FILE_NAME = "server-000.properties";
    }

    @Override
    protected SecurityServiceFactory.SecurityServiceType getSecurityServiceType() {
        return SecurityServiceFactory.SecurityServiceType.SERVER;
    }
}
