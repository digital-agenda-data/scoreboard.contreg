package eionet.cr.web.context;

import eionet.cr.common.CRRuntimeException;
import eionet.cr.config.GeneralConfig;
import eionet.cr.util.sesame.SesameConnectionProvider;
import eionet.liquibase.VirtuosoDatabase;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.apache.commons.configuration2.ConfigurationLookup;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.interpol.ConfigurationInterpolator;
import org.apache.commons.configuration2.interpol.InterpolatorSpecification;
import org.apache.commons.configuration2.interpol.Lookup;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.util.*;

/**
 *
 */
public class CRServletContextListener implements ServletContextListener {

    /** */
    private static final Logger LOGGER = Logger.getLogger(CRServletContextListener.class);

    /** */
    private static final String[] PROP_FILES = {"uit.properties", "eionetdir.properties"};

    /** */
    private static final Map<String, String> CONF_TO_INIT_PARAMS_MAP = new HashMap<>();

    static {
        CONF_TO_INIT_PARAMS_MAP.put(GeneralConfig.APPLICATION_DISPLAY_NAME, "appDispName");
        CONF_TO_INIT_PARAMS_MAP.put(GeneralConfig.TEMPLATE_JSP_FILE_NAME, "templateJsp");
        CONF_TO_INIT_PARAMS_MAP.put(GeneralConfig.TRACKING_JAVASCRIPT_FILE, "trackingJsFile");
        CONF_TO_INIT_PARAMS_MAP.put(GeneralConfig.EEA_TEMPLATE_FOLDER, "templateCacheFolder");
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        LOGGER.debug(getClass().getSimpleName() + " initializing");

        try {
            initProperties();
        } catch (Exception e) {
            LOGGER.error("Failed to initialize properties", e);
        }

        initServletContextParams(servletContextEvent);
        initLiquibase();
    }

    /**
     *
     */
    private void initProperties() throws ConfigurationException {

        if (PROP_FILES == null || PROP_FILES.length == 0) {
            return;
        }

        String envPropsFilePath = System.getProperty("cr.external.props");
        if (StringUtils.isBlank(envPropsFilePath)) {
            return;
        }

        File envPropsFile = new File(envPropsFilePath);
        if (!envPropsFile.exists() || !envPropsFile.isFile()) {
            return;
        }

        PropertiesConfiguration envConfig = new Configurations().properties(envPropsFile);
        Map<String, Lookup> prefixLookups = ConfigurationInterpolator.getDefaultPrefixLookups();
        ArrayList<Lookup> defaultLookups = new ArrayList();

        defaultLookups.add(new ConfigurationLookup(envConfig));
        defaultLookups.add(prefixLookups.get("sys"));
        defaultLookups.add(prefixLookups.get("env"));

        InterpolatorSpecification spec = (new InterpolatorSpecification.Builder()).withPrefixLookups(prefixLookups)
                .withDefaultLookups(defaultLookups).create();
        ConfigurationInterpolator interpolator = ConfigurationInterpolator.fromSpecification(spec);

        for (String propFileName : PROP_FILES) {
            try {
                initPropertyFile(propFileName, interpolator);
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to init propety file %s: %s", propFileName, e.toString()));
            }
        }
    }

    /**
     *
     * @param propFileName
     * @param interpolator
     */
    private void initPropertyFile(String propFileName, ConfigurationInterpolator interpolator) throws URISyntaxException, ConfigurationException, IOException {

        URL propsResource = getClass().getClassLoader().getResource(propFileName);
        if (propsResource == null) {
            return;
        }

        PropertiesConfiguration propConfig = new Configurations().properties(propsResource);
        propConfig.setInterpolator(interpolator);

        Properties interpolatedProperties = new Properties();
        Iterator<String> keys = propConfig.getKeys();
        while (keys != null && keys.hasNext()) {

            String key = keys.next();
            String value = propConfig.getString(key);
            interpolatedProperties.setProperty(key, value);
        }

        File propsFile = new File(propsResource.toURI());
        if (!propsFile.exists() || !propsFile.isFile()) {
            return;
        }

        OutputStream out = null;
        try {
            out = new FileOutputStream(propsFile);
            interpolatedProperties.store(out, "Interpolated");
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     *
     * @param servletContextEvent
     */
    private void initServletContextParams(ServletContextEvent servletContextEvent) {

        ServletContext context = servletContextEvent.getServletContext();
        for (Map.Entry<String, String> entry : CONF_TO_INIT_PARAMS_MAP.entrySet()) {

            String initParamName = entry.getValue();
            String propertyKey = entry.getKey();
            String propertyValue = GeneralConfig.getProperty(propertyKey);
            if (propertyValue != null) {
                int i = propertyValue.indexOf("${");
                int j = propertyValue.indexOf("}");
                if (i < 0 || j < 0) {
                    context.setInitParameter(initParamName, propertyValue.trim());
                }
            }
        }
    }

    /**
     *
     */
    private void initLiquibase() {

        String changelogFile = GeneralConfig.getRequiredProperty(GeneralConfig.LIQUIBASE_CHANGELOG);
        String contexts = GeneralConfig.getProperty(GeneralConfig.LIQUIBASE_CONTEXTS, "");

        Thread currentThread = Thread.currentThread();
        ClassLoader contextClassLoader = currentThread.getContextClassLoader();
        ResourceAccessor threadClFO = new ClassLoaderResourceAccessor(contextClassLoader);
        ResourceAccessor clFO = new ClassLoaderResourceAccessor();
        ResourceAccessor fsFO = new FileSystemResourceAccessor();

        Connection conn = null;
        try {
            conn = SesameConnectionProvider.getSQLConnection();

            VirtuosoDatabase database = new VirtuosoDatabase();
            database.setConnection(new JdbcConnection(conn));
            Liquibase liquibase = new Liquibase(changelogFile, new CompositeResourceAccessor(clFO, fsFO, threadClFO), database);
            liquibase.update(contexts);

        } catch (Exception e) {
            throw new CRRuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {}
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
