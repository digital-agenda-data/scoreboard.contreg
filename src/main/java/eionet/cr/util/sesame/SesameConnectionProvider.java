package eionet.cr.util.sesame;

import eionet.cr.common.CRRuntimeException;
import eionet.cr.config.GeneralConfig;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import virtuoso.sesame2.driver.VirtuosoRepository;
import virtuoso.sesame2.driver.VirtuosoRepositoryConnection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;

/**
 *
 * @author jaanus
 *
 */
public final class SesameConnectionProvider {

    /** */
    private static final Logger LOGGER = Logger.getLogger(SesameConnectionProvider.class);

    /** */
    private static final String VIRTUOSO_DB_URL_DB_NAME_PARAM = "DATABASE";

    /** */
    public static final String READWRITE_DATASOURCE_NAME = "jdbc/readWriteRepo";
    public static final String READONLY_DATASOURCE_NAME = "jdbc/readOnlyRepo";

    /** Repository and data source for read-write connection. */
    private static Repository readWriteRepository;
    private static DataSource readWriteDataSource;

    /** Repository and data source for read-only connection. */
    private static Repository readOnlyRepository;
    private static DataSource readOnlyDataSource;

    /** */
    private static boolean readWriteDataSourceMissingLogged = false;
    private static boolean readOnlyDataSourceMissingLogged = false;

    /**
     * Hide utility class constructor.
     */
    private SesameConnectionProvider() {
        // Just an empty private constructor to avoid instantiating this utility class.
    }

    /**
     * @return the readWriteRepository
     */
    private static synchronized Repository getReadWriteRepository() {

        if (readWriteRepository == null) {

            String urlProperty = GeneralConfig.VIRTUOSO_DB_URL;
            String usrProperty = GeneralConfig.VIRTUOSO_DB_USR;
            String pwdProperty = GeneralConfig.VIRTUOSO_DB_PWD;

            readWriteRepository =
                    createRepository(GeneralConfig.getRequiredProperty(urlProperty), GeneralConfig.getRequiredProperty(usrProperty),
                            GeneralConfig.getRequiredProperty(pwdProperty));
        }
        return readWriteRepository;
    }

    /**
     * @return the readOnlyRepository
     */
    private static synchronized Repository getReadOnlyRepository() {

        if (readOnlyRepository == null) {
            readOnlyRepository =
                    createRepository(GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_URL),
                            GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_ROUSR),
                            GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_ROPWD));
        }
        return readOnlyRepository;
    }

    /**
     *
     * @param url
     * @param usr
     * @param pwd
     * @return
     */
    private static Repository createRepository(String url, String usr, String pwd) {

        // The true in the last paramater means that Virtuoso will batch optimization for mass-executions
        // of VirtuosoRepositoryConnection's add(Resource subject ...) and add(Statement statement ...) methods.
        // Without this parameter there is a danger of running into "virtuoso.jdbc4.VirtuosoException:
        // SR491: Too many open statements" when using a pooled connection.
        Repository repository = new VirtuosoRepository(url, usr, pwd, true);
        try {
            repository.initialize();
        } catch (RepositoryException e) {
            throw new CRRuntimeException(MessageFormat.format("Failed to initialize repository {0} with user {1}", url, usr), e);
        }

        return repository;
    }

    /**
     * @return the readWriteDataSource
     */
    public static synchronized DataSource getReadWriteDataSource() {

        if (readWriteDataSource == null) {
            readWriteDataSource = createDataSource(GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_USR),
                    GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_PWD));
        }
        return readWriteDataSource;
    }

    /**
     * @return the readOnlyDataSource
     */
    public static synchronized DataSource getReadOnlyDataSource() {

        if (readOnlyDataSource == null) {
            readOnlyDataSource = createDataSource(GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_ROUSR),
                    GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_ROPWD));
        }
        return readOnlyDataSource;
    }

    /**
     *
     * @param usr
     * @param pwd
     * @return
     */
    private static DataSource createDataSource(String usr, String pwd) {

        String drv = GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_DRV);
        String url = GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_URL);

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(drv);
        dataSource.setUrl(url);
        dataSource.setUsername(usr);
        dataSource.setPassword(pwd);

        dataSource.setMaxTotal(GeneralConfig.getIntProperty(GeneralConfig.VIRTUOSO_DB_POOL_MAX_ACTIVE, 100));
        dataSource.setMaxIdle(GeneralConfig.getIntProperty(GeneralConfig.VIRTUOSO_DB_POOL_MAX_IDLE, 30));
        dataSource.setInitialSize(GeneralConfig.getIntProperty(GeneralConfig.VIRTUOSO_DB_POOL_MAX_INIT_SIZE, 5));
        dataSource.setMaxWaitMillis((long) GeneralConfig.getIntProperty(GeneralConfig.VIRTUOSO_DB_POOL_MAX_WAIT_MS, 10000));

        return dataSource;
    }

    /**
     * Returns read-write connection to the repository.
     *
     * @return RepositoryConnection
     * @throws RepositoryException
     */
    public static RepositoryConnection getRepositoryConnection() throws RepositoryException {

        DataSource dataSource = getReadWriteDataSource();
        if (dataSource == null) {

            if (!isReadWriteDataSourceMissingLogged()) {
                LOGGER.debug(MessageFormat.format("Found no data source with name {0}, going to create a direct connection",
                        READWRITE_DATASOURCE_NAME));
                SesameConnectionProvider.readWriteDataSourceMissingLogged = true;
            }
            return getReadWriteRepository().getConnection();
        } else {
            try {
                return new VirtuosoRepositoryConnection((VirtuosoRepository) getReadWriteRepository(), dataSource.getConnection());
            } catch (SQLException e) {
                throw new RepositoryException("Could not create repository connection from the given SQL connection", e);
            }
        }
    }

    /**
     * Returns read-only connection to the repository.
     *
     * @return RepositoryConnection connection
     * @throws RepositoryException
     */
    public static RepositoryConnection getReadOnlyRepositoryConnection() throws RepositoryException {

        DataSource dataSource = getReadOnlyDataSource();
        if (dataSource == null) {

            if (!isReadOnlyDataSourceMissingLogged()) {
                LOGGER.debug(MessageFormat
                        .format("Found no data source with name {0}, going to create a direct connection", READONLY_DATASOURCE_NAME));
                SesameConnectionProvider.readOnlyDataSourceMissingLogged = true;
            }
            return getReadOnlyRepository().getConnection();
        } else {
            try {
                return new VirtuosoRepositoryConnection((VirtuosoRepository) getReadOnlyRepository(), dataSource.getConnection());
            } catch (SQLException e) {
                throw new RepositoryException("Could not create repository connection from the given SQL connection", e);
            }
        }
    }

    /**
     * Returns a {@link java.sql.Connection} to the underlying repository. Uses a {@link javax.sql.DataSource} with name
     * {@link #READWRITE_DATASOURCE_NAME} if such can be found. Otherwise creates a direct connection using the "classical" way
     * through {@link java.sql.DriverManager}.
     *
     * @return Connection
     * @throws SQLException
     */
    public static Connection getSQLConnection() throws SQLException {
        return getSQLConnection(null);
    }

    /**
     * @param databaseName
     * @return
     */
    public static Connection getSQLConnection(String databaseName) throws SQLException {

        boolean specificDatabaseRequested = databaseName != null && databaseName.trim().length() > 0;

        // First, try to create a connection through the data source (but not when connection is requested to specific DB!).

        DataSource dataSource = getReadWriteDataSource();
        if (!specificDatabaseRequested) {

            if (dataSource != null) {
                return dataSource.getConnection();
            } else if (!isReadWriteDataSourceMissingLogged()) {
                LOGGER.debug(MessageFormat.format("Found no data source with name {0}, going to create a connection through DriverManager",
                        READWRITE_DATASOURCE_NAME));
                SesameConnectionProvider.readWriteDataSourceMissingLogged = true;
            }
        }

        // No data source was found above, so create the connection through DriverManager.

        String drv = GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_DRV);
        String url = GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_URL);
        String usr = GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_USR);
        String pwd = GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_PWD);

        if (drv == null || drv.trim().length() == 0) {
            throw new SQLException("Failed to get connection, missing property: " + GeneralConfig.VIRTUOSO_DB_DRV);
        } else {
            drv = drv.trim();
        }

        if (url == null || url.trim().length() == 0) {
            throw new SQLException("Failed to get connection, missing property: " + GeneralConfig.VIRTUOSO_DB_URL);
        } else {
            url = url.trim();
        }

        if (usr == null || usr.trim().length() == 0) {
            throw new SQLException("Failed to get connection, missing property: " + GeneralConfig.VIRTUOSO_DB_USR);
        } else {
            usr = usr.trim();
        }

        if (pwd == null || pwd.trim().length() == 0) {
            throw new SQLException("Failed to get connection, missing property: " + GeneralConfig.VIRTUOSO_DB_PWD);
        } else {
            pwd = pwd.trim();
        }

        // If specific database requested, then ensure that the connection is asked indeed for that particular database.
        if (specificDatabaseRequested) {

            // If no database name already requested in the URL, then just append it, otherwise overwrite with the requested database name.
            String databaseEqualsStr = VIRTUOSO_DB_URL_DB_NAME_PARAM + "=";
            if (!url.contains(databaseEqualsStr)) {
                url = (url.endsWith("/") ? "" : "/") + databaseEqualsStr + databaseName;
            } else {
                int i = url.indexOf(databaseEqualsStr);
                int j = url.indexOf('/', i);
                String strToReplace = url.substring(i, j == -1 ? url.length() : j);
                url = url.replace(strToReplace, databaseEqualsStr + databaseName);
            }
        }

        // Finally, load the driver and get the database connection.
        try {
            Class.forName(drv);
            return DriverManager.getConnection(url, usr, pwd);
        } catch (ClassNotFoundException e) {
            throw new CRRuntimeException("Failed to get connection, driver class not found: " + drv, e);
        }
    }

    /**
     * @return the readWriteDataSourceMissingLogged
     */
    private static synchronized boolean isReadWriteDataSourceMissingLogged() {
        return readWriteDataSourceMissingLogged;
    }

    /**
     * @return the readOnlyDataSourceMissingLogged
     */
    private static synchronized boolean isReadOnlyDataSourceMissingLogged() {
        return readOnlyDataSourceMissingLogged;
    }
}
