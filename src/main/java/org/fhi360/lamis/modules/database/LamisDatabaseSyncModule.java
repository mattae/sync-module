package org.fhi360.lamis.modules.database;

import com.foreach.across.core.AcrossModule;
import com.foreach.across.core.annotations.AcrossDepends;
import com.foreach.across.core.context.configurer.ComponentScanConfigurer;
import com.foreach.across.modules.hibernate.jpa.AcrossHibernateJpaModule;
import lombok.extern.slf4j.Slf4j;

@AcrossDepends(required = AcrossHibernateJpaModule.NAME)
@Slf4j
public class LamisDatabaseSyncModule extends AcrossModule {
    public static final String NAME = "LAMISDatabaseSyncModule";

    public LamisDatabaseSyncModule() {
        super();
        addApplicationContextConfigurer(
                new ComponentScanConfigurer(getClass().getPackage().getName() + ".service",
                        getClass().getPackage().getName() + ".util", getClass().getPackage().getName() + ".web"));
    }

    @Override
    public String getName() {
        return NAME;
    }
}
