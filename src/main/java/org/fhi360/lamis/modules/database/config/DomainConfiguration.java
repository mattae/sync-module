package org.fhi360.lamis.modules.database.config;

import org.fhi360.lamis.modules.database.domain.DatabaseDomain;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = DatabaseDomain.class)
public class DomainConfiguration {
}
