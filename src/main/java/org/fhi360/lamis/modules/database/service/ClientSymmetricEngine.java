package org.fhi360.lamis.modules.database.service;

import com.foreach.across.core.AcrossContext;
import com.foreach.across.core.context.AcrossContextUtils;
import com.foreach.across.core.context.info.AcrossContextInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.fhi360.lamis.modules.database.util.OrphanedRecordFilter;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.platform.generic.GenericJdbcDatabasePlatform;
import org.jumpmind.db.sql.LogSqlBuilder;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.BasicDataSourcePropertyConstants;
import org.jumpmind.extension.IExtensionPoint;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.security.ISecurityService;
import org.jumpmind.security.SecurityServiceFactory;
import org.jumpmind.symmetric.AbstractSymmetricEngine;
import org.jumpmind.symmetric.ISymmetricEngine;
import org.jumpmind.symmetric.ITypedPropertiesFactory;
import org.jumpmind.symmetric.common.Constants;
import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.common.SystemConstants;
import org.jumpmind.symmetric.db.ISymmetricDialect;
import org.jumpmind.symmetric.db.JdbcSymmetricDialectFactory;
import org.jumpmind.symmetric.io.stage.BatchStagingManager;
import org.jumpmind.symmetric.io.stage.IStagingManager;
import org.jumpmind.symmetric.job.IJobManager;
import org.jumpmind.symmetric.job.JobManager;
import org.jumpmind.symmetric.security.INodePasswordFilter;
import org.jumpmind.symmetric.service.*;
import org.jumpmind.symmetric.service.impl.ClientExtensionService;
import org.jumpmind.symmetric.service.impl.MonitorService;
import org.jumpmind.symmetric.service.impl.NodeService;
import org.jumpmind.symmetric.statistic.IStatisticManager;
import org.jumpmind.symmetric.statistic.StatisticManager;
import org.jumpmind.symmetric.transport.IConcurrentConnectionManager;
import org.jumpmind.symmetric.transport.ITransportManager;
import org.jumpmind.symmetric.util.LogSummaryAppenderUtils;
import org.jumpmind.symmetric.util.SnapshotUtil;
import org.jumpmind.symmetric.util.SymmetricUtils;
import org.jumpmind.symmetric.util.TypedPropertiesFactory;
import org.jumpmind.symmetric.web.AuthenticationInterceptor;
import org.jumpmind.symmetric.web.IInterceptor;
import org.jumpmind.symmetric.web.NodeConcurrencyInterceptor;
import org.lamisplus.modules.base.config.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.apache.commons.lang.StringUtils.isBlank;

@Service
@Profile("!server")
@Slf4j
@DependsOn({"updateStatusService", "syncService"})
public class ClientSymmetricEngine extends AbstractSymmetricEngine {
    public static final String DEPLOYMENT_TYPE_CLIENT = "client";

    public static final String PROPERTIES_FACTORY_CLASS_NAME = "properties.factory.class.name";

    private JdbcTemplate jdbcTemplate;

    protected File propertiesFile;

    protected String PROPERTIES_FILE = "classpath:config.properties";

    protected String PROPERTIES_FILE_NAME = "facility.properties";

    protected Properties properties = System.getProperties();

    private static IDatabasePlatform databasePlatform;

    protected DataSource dataSource;

    protected ApplicationContext springContext;

    protected IMonitorService monitorService;

    private AcrossContext acrossContext;

    protected final ApplicationProperties applicationProperties;

    @Autowired
    private OrphanedRecordFilter orphanedRecordFilter;

    public ClientSymmetricEngine(DataSource dataSource, JdbcTemplate jdbcTemplate, ApplicationProperties applicationProperties, AcrossContext acrossContext) {
        super(false);
        setDeploymentType(DEPLOYMENT_TYPE_CLIENT);
        setDeploymentSubTypeByProperties(properties);
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.applicationProperties = applicationProperties;
        this.springContext = acrossContext.getParentApplicationContext();
        this.acrossContext = acrossContext;
    }

    protected void setDeploymentSubTypeByProperties(Properties properties) {
        if (properties != null) {
            String loadOnly = properties.getProperty(ParameterConstants.NODE_LOAD_ONLY);
            setDeploymentSubType(loadOnly != null && loadOnly.equals("true") ? Constants.DEPLOYMENT_SUB_TYPE_LOAD_ONLY : null);
        }
    }

    protected String getConfigurationFile() {
        return PROPERTIES_FILE;
    }

    @Override
    protected SecurityServiceFactory.SecurityServiceType getSecurityServiceType() {
        return SecurityServiceFactory.SecurityServiceType.CLIENT;
    }

    @Override
    protected void init() {
        try {
            LogSummaryAppenderUtils.registerLogSummaryAppender();

            if (getSecurityServiceType().equals(SecurityServiceFactory.SecurityServiceType.CLIENT)) {
                SymmetricUtils.logNotices();
            }
            super.init();

            this.monitorService = new MonitorService(parameterService, symmetricDialect, nodeService, extensionService,
                clusterService, contextService);
            this.dataSource = platform.getDataSource();

            try {
                extensionService.addExtensionPoint(orphanedRecordFilter);
                ((ClientExtensionService) this.extensionService).setSpringContext(springContext);
                this.extensionService.refresh();
            } catch (Exception ex) {
                log.error(
                    "Failed to initialize the extension points.  Please fix the problem and restart the server.",
                    ex);
            }

            if (nodeService instanceof NodeService) {
                ((NodeService) nodeService).setNodePasswordFilter(extensionService.getExtensionPoint(INodePasswordFilter.class));
            }

        } catch (RuntimeException ex) {
            destroy();
            throw ex;
        }
    }

    @Override
    public ITypedPropertiesFactory createTypedPropertiesFactory() {
        return createTypedPropertiesFactory(new File(getConfigurationFile()), properties);
    }

    @Override
    public synchronized boolean start() {
        if (this.springContext instanceof AbstractApplicationContext) {
            AbstractApplicationContext ctx = (AbstractApplicationContext) this.springContext;
            try {
                if (!ctx.isActive()) {
                    ctx.start();
                }
            } catch (Exception ignored) {
            }
        }

        return super.start();
    }

    @Override
    public synchronized void stop() {
        if (this.springContext instanceof AbstractApplicationContext) {
            AbstractApplicationContext ctx = (AbstractApplicationContext) this.springContext;
            try {
                if (ctx.isActive()) {
                    ctx.stop();
                }
            } catch (Exception ex) {
            }
        }
        super.stop();
    }

    @Override
    protected ISymmetricDialect createSymmetricDialect() {
        return new JdbcSymmetricDialectFactory(parameterService, platform).create();
    }

    @Override
    protected ISymmetricDialect createTargetDialect() {
        if (parameterService.is(ParameterConstants.NODE_LOAD_ONLY, false)) {
            TypedProperties properties = new TypedProperties();
            String prefix = ParameterConstants.LOAD_ONLY_PROPERTY_PREFIX;
            copyProperties(properties, prefix, BasicDataSourcePropertyConstants.ALL_PROPS);
            copyProperties(properties, prefix, ParameterConstants.ALL_JDBC_PARAMS);
            copyProperties(properties, "", ParameterConstants.ALL_KAFKA_PARAMS);

            IDatabasePlatform targetPlatform = createDatabasePlatform(properties, dataSource);

            if (targetPlatform instanceof GenericJdbcDatabasePlatform) {
                targetPlatform.getDatabaseInfo().setNotNullColumnsSupported(parameterService.is(prefix +
                    ParameterConstants.CREATE_TABLE_NOT_NULL_COLUMNS, true));
            }

            return new JdbcSymmetricDialectFactory(parameterService, targetPlatform).create();
        } else {
            return getSymmetricDialect();
        }
    }

    private void copyProperties(TypedProperties properties, String prefix, String[] parameterNames) {
        for (String name : parameterNames) {
            properties.put(name, parameterService.getString(prefix + name));
        }
    }

    @Override
    protected IDatabasePlatform createDatabasePlatform(TypedProperties properties) {
        IDatabasePlatform platform = createDatabasePlatform(springContext, properties, dataSource,
            Boolean.parseBoolean(System.getProperty(SystemConstants.SYSPROP_WAIT_FOR_DATABASE, "true")));
        return platform;
    }

    public static IDatabasePlatform createDatabasePlatform(ApplicationContext springContext, TypedProperties properties,
                                                           DataSource dataSource, boolean waitOnAvailableDatabase) {
        return createDatabasePlatform(properties, dataSource);
    }

    public static IDatabasePlatform createDatabasePlatform(TypedProperties properties, DataSource dataSource) {
        boolean delimitedIdentifierMode = properties.is(
            ParameterConstants.DB_DELIMITED_IDENTIFIER_MODE, true);
        boolean caseSensitive = !properties.is(ParameterConstants.DB_METADATA_IGNORE_CASE, true);
        databasePlatform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource,
            createSqlTemplateSettings(properties), delimitedIdentifierMode, caseSensitive, false);
        return databasePlatform;
    }

    protected static SqlTemplateSettings createSqlTemplateSettings(TypedProperties properties) {
        SqlTemplateSettings settings = new SqlTemplateSettings();
        settings.setFetchSize(properties.getInt(ParameterConstants.DB_FETCH_SIZE, 1000));
        settings.setQueryTimeout(properties.getInt(ParameterConstants.DB_QUERY_TIMEOUT_SECS, 300));
        settings.setBatchSize(properties.getInt(ParameterConstants.JDBC_EXECUTE_BATCH_SIZE, 100));
        settings.setBatchBulkLoaderSize(properties.getInt(ParameterConstants.JDBC_EXECUTE_BULK_BATCH_SIZE, 25));
        settings.setOverrideIsolationLevel(properties.getInt(ParameterConstants.JDBC_ISOLATION_LEVEL, -1));
        settings.setReadStringsAsBytes(properties.is(ParameterConstants.JDBC_READ_STRINGS_AS_BYTES, false));
        settings.setTreatBinaryAsLob(properties.is(ParameterConstants.TREAT_BINARY_AS_LOB_ENABLED, true));
        settings.setRightTrimCharValues(properties.is(ParameterConstants.RIGHT_TRIM_CHAR_VALUES, false));
        settings.setAllowUpdatesWithResults(properties.is(ParameterConstants.ALLOW_UPDATES_WITH_RESULTS, false));

        LogSqlBuilder logSqlBuilder = new LogSqlBuilder();
        logSqlBuilder.setLogSlowSqlThresholdMillis(properties.getInt(ParameterConstants.LOG_SLOW_SQL_THRESHOLD_MILLIS, 20000));
        logSqlBuilder.setLogSqlParametersInline(properties.is(ParameterConstants.LOG_SQL_PARAMETERS_INLINE, true));
        settings.setLogSqlBuilder(logSqlBuilder);

        if (settings.getOverrideIsolationLevel() >= 0) {
            log.info("Overriding isolation level to " + settings.getOverrideIsolationLevel());
        }
        return settings;
    }

    @Override
    protected IExtensionService createExtensionService() {
        extensionService = new ClientExtensionService(this, springContext);
        return extensionService;
    }

    @Override
    protected IJobManager createJobManager() {
        return new JobManager(this);
    }

    @Override
    protected IStagingManager createStagingManager() {
        String directory = parameterService.getString(ParameterConstants.STAGING_DIR);
        if (isBlank(directory)) {
            directory = parameterService.getTempDirectory();
        }
        String stagingManagerClassName = parameterService.getString(ParameterConstants.STAGING_MANAGER_CLASS);
        if (stagingManagerClassName != null) {
            try {
                Constructor<?> cons = Class.forName(stagingManagerClassName).getConstructor(ISymmetricEngine.class, String.class);
                return (IStagingManager) cons.newInstance(this, directory);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return new BatchStagingManager(this, directory);
    }

    @Override
    protected IStatisticManager createStatisticManager() {
        String statisticManagerClassName = parameterService.getString(ParameterConstants.STATISTIC_MANAGER_CLASS);
        if (statisticManagerClassName != null) {
            try {
                Constructor<?> cons = Class.forName(statisticManagerClassName).getConstructor(ISymmetricEngine.class);
                return (IStatisticManager) cons.newInstance(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return new StatisticManager(parameterService, nodeService,
            configurationService, statisticService, clusterService);
    }

    protected static ITypedPropertiesFactory createTypedPropertiesFactory(File propFile, Properties prop) {
        String propFactoryClassName = System.getProperties().getProperty(PROPERTIES_FACTORY_CLASS_NAME);
        ITypedPropertiesFactory factory = null;
        if (propFactoryClassName != null) {
            try {
                factory = (ITypedPropertiesFactory) Class.forName(propFactoryClassName).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            factory = new TypedPropertiesFactory();
        }
        factory.init(propFile, prop);
        return factory;
    }

    @Override
    public synchronized void destroy() {
        super.destroy();
    }

    public List<File> listSnapshots() {
        File snapshotsDir = SnapshotUtil.getSnapshotDirectory(this);
        List<File> files = new ArrayList<File>(FileUtils.listFiles(snapshotsDir, new String[]{"zip"}, false));
        files.sort((o1, o2) -> -o1.compareTo(o2));
        return files;
    }

    public ApplicationContext getSpringContext() {
        return springContext;
    }

    public File snapshot() {
        return SnapshotUtil.createSnapshot(this);
    }

    public IMonitorService getMonitorService() {
        return monitorService;
    }

    @SneakyThrows
    @PostConstruct
    public void startEngine() {
        List<Long> ids = jdbcTemplate.queryForList("select max(facility_id) from pharmacy where last_modified > " +
            "'2020-08-31'", Long.class);

        if (!ids.isEmpty()) {
            Long id = ids.get(0);
            Resource resource = new ClassPathResource("classpath:" + PROPERTIES_FILE_NAME);
            String config = new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
            try {
                config = config.replaceAll("@@@", StringUtils.leftPad(id.toString(), 4, "0"));
                config = config.replaceAll("&&&", SyncService.PROXY_URL + "/sync/server-000");
            } catch (Exception ignored) {
            }
            Path moduleRuntimePath = Paths.get(applicationProperties.getModulePath(), "runtime", "LAMISDatabaseSyncModule");

            PROPERTIES_FILE = moduleRuntimePath.resolve("config.properties").toAbsolutePath().toString();
            Files.write(moduleRuntimePath.resolve("config.properties"), config.getBytes(), StandardOpenOption.CREATE);

            this.init();
            new Thread(this::start).start();
        }
    }

    @Bean
    public IMonitorService getMonitorServiceBean() {
        return monitorService;
    }

    @Bean
    public ISecurityService getSecurityServiceBean() {
        return securityService;
    }

    @Bean
    public ISymmetricDialect getSymmetricDialectBean() {
        return createSymmetricDialect();
    }

    @Bean
    public IParameterService getParameterServiceBean() {
        return parameterService;
    }

    @Bean
    public IMailService getMailServiceBean() {
        return mailService;
    }

    @Bean
    public INodeService getNodeServiceBean() {
        return nodeService;
    }

    @Bean
    public IConfigurationService getConfigurationServiceBean() {
        return configurationService;
    }

    @Bean
    public IBandwidthService getBandwidthServiceBean() {
        return bandwidthService;
    }

    @Bean
    public IStatisticService getStatisticServiceBean() {
        return statisticService;
    }

    @Bean
    public IStatisticManager getStatisticManagerBean() {
        return statisticManager;
    }

    @Bean
    public IConcurrentConnectionManager getConcurrentConnectionManagerBean() {
        return concurrentConnectionManager;
    }

    @Bean
    public ITransportManager getTransportManagerBean() {
        return transportManager;
    }

    @Bean
    public IClusterService getClusterServiceBean() {
        return clusterService;
    }

    @Bean
    public IPurgeService getPurgeServiceBean() {
        return purgeService;
    }

    @Bean
    public ITransformService getTransformServiceBean() {
        return transformService;
    }

    @Bean
    public ILoadFilterService getLoadFilterServiceBean() {
        return loadFilterService;
    }

    @Bean
    public ITriggerRouterService getTriggerRouterServiceBean() {
        return triggerRouterService;
    }

    @Bean
    public IOutgoingBatchService getOutgoingBatchServiceBean() {
        return outgoingBatchService;
    }

    @Bean
    public IDataService getDataServiceBean() {
        return dataService;
    }

    @Bean
    public IRouterService getRouterServiceBean() {
        return routerService;
    }

    @Bean
    public IRegistrationService getRegistrationServiceBean() {
        return registrationService;
    }

    @Bean
    public IJobManager getJobManagerBean() {
        return this.jobManager;
    }

    @Bean
    public IAcknowledgeService getAcknowledgeServiceBean() {
        return this.acknowledgeService;
    }

    @Bean
    public IDataExtractorService getDataExtractorServiceBean() {
        return this.dataExtractorService;
    }

    //@Bean
    public IDataExtractorService getFileSyncExtractorServiceBean() {
        return this.fileSyncExtractorService;
    }

    @Bean
    public IDataLoaderService getDataLoaderServiceBean() {
        return this.dataLoaderService;
    }

    @Bean
    public IIncomingBatchService getIncomingBatchServiceBean() {
        return this.incomingBatchService;
    }

    @Bean
    public IPullService getPullServiceBean() {
        return this.pullService;
    }

    @Bean
    public IPushService getPushServiceBean() {
        return this.pushService;
    }

    @Bean
    public IOfflinePullService getOfflinePullServiceBean() {
        return this.offlinePullService;
    }

    @Bean
    public IOfflinePushService getOfflinePushServiceBean() {
        return this.offlinePushService;
    }

    @Bean
    public IStagingManager getStagingManagerBean() {
        return stagingManager;
    }

    @Bean
    public ISequenceService getSequenceServiceBean() {
        return sequenceService;
    }

    @Bean
    public IExtensionService getIExtensionServiceBean() {
        return extensionService;
    }

    @Bean
    public IGroupletService getGroupletServiceBean() {
        return groupletService;
    }

    @Bean
    public INodeCommunicationService getNodeCommunicationServiceBean() {
        return nodeCommunicationService;
    }

    @Bean
    public IFileSyncService getFileSyncServiceBean() {
        return fileSyncService;
    }

    @Bean
    public IContextService getIContextServiceBean() {
        return contextService;
    }

    @Bean
    public IUpdateService getUpdateServiceBean() {
        return updateService;
    }

    @Bean
    public IDatabasePlatform getDatabasePlatformBean() {
        return databasePlatform;
    }

    @Bean
    public IInterceptor getAuthenticationInterceptor(INodeService nodeService) {
        return new AuthenticationInterceptor(nodeService, securityService, true, 2000, 6000);
    }

    @Bean
    public IInterceptor getNodeConcurrencyInterceptor(
        IConcurrentConnectionManager concurrentConnectionManager,
        IConfigurationService configurationService,
        INodeService nodeService,
        IStatisticManager statisticManager) {
        return new NodeConcurrencyInterceptor(
            concurrentConnectionManager, configurationService, nodeService, statisticManager);
    }
}
