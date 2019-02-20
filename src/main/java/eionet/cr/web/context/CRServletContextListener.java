package eionet.cr.web.context;

import eionet.cr.common.CRRuntimeException;
import eionet.cr.config.ConfigUtils;
import eionet.cr.config.GeneralConfig;
import eionet.cr.util.sesame.SesameConnectionProvider;
import eionet.liquibase.VirtuosoDatabase;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
            ConfigUtils.interpolatePropertyFiles(Arrays.asList(PROP_FILES));
        } catch (Exception e) {
            LOGGER.error("Failed to initialize properties", e);
        }

        initServletContextParams(servletContextEvent);
        initLiquibase();

        try {
            initAcls();
        } catch (Exception e) {
            LOGGER.error("Failed to initialize ACL resources", e);
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

    /**
     *
     * @throws URISyntaxException
     */
    private void initAcls() throws URISyntaxException, IOException {

        LOGGER.debug("Initializing ACL resources");

        URL resource = getClass().getClassLoader().getResource("acl/");
        LOGGER.debug("ACL resources URL = " + resource);
        if (resource == null) {
            return;
        }

        File resourceDir = new File(resource.toURI());
        if (!resourceDir.exists() || !resourceDir.isDirectory()) {
            return;
        }

        File appHomeDir = new File(GeneralConfig.getRequiredProperty(GeneralConfig.APP_HOME_DIR));
        if (!appHomeDir.exists() || !appHomeDir.isDirectory()) {
            LOGGER.error("Found no such app-home directory: " + appHomeDir);
            return;
        }

        File aclTargetDir = new File(appHomeDir, "acl");
        if (!aclTargetDir.exists() || !aclTargetDir.isDirectory()) {
            LOGGER.debug("Creating target ACL directory: " + aclTargetDir);
            aclTargetDir.mkdir();
        }

        File[] aclFiles = resourceDir.listFiles();
        if (aclFiles != null && aclFiles.length > 0) {
            for (File aclFile : aclFiles) {

                String fileName = aclFile.getName();
                File destFile = new File(aclTargetDir, fileName);
                if (fileName.contains(".prms") || fileName.contains(".permissions") || !destFile.exists()) {
                    LOGGER.debug(String.format("Copying %s to %s", fileName, destFile));
                    FileUtils.copyFile(aclFile, destFile);
                } else {
                    LOGGER.debug(String.format("%s already exists in destination, so not overwriting it", fileName));
                }
            }
        } else {
            LOGGER.debug("Found no ACL resource files in " + resourceDir);
        }
    }

    /**
     *
     * @param servletContextEvent
     */
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        LOGGER.debug(getClass().getSimpleName() + " context shutdown ...");
    }
}
