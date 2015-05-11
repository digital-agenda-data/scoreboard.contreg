package eionet.cr.util.sesame;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import virtuoso.sesame2.driver.VirtuosoRepository;
import virtuoso.sesame2.driver.VirtuosoRepositoryConnection;
import eionet.cr.common.CRRuntimeException;
import eionet.cr.config.GeneralConfig;

/**
 *
 * @author jaanus
 *
 */
public final class SesameConnectionProvider {

    /** */
    private static final Logger LOGGER = Logger.getLogger(SesameConnectionProvider.class);

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
                    createRepository(GeneralConfig.getRequiredProperty(urlProperty),
                            GeneralConfig.getRequiredProperty(usrProperty), GeneralConfig.getRequiredProperty(pwdProperty));
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
    private static synchronized DataSource getReadWriteDataSource() {

        if (readWriteDataSource == null) {
            readWriteDataSource = lookupDataSource(READWRITE_DATASOURCE_NAME);
        }
        return readWriteDataSource;
    }

    /**
     * @return the readOnlyDataSource
     */
    private static synchronized DataSource getReadOnlyDataSource() {

        if (readOnlyDataSource == null) {
            readOnlyDataSource = lookupDataSource(READONLY_DATASOURCE_NAME);
        }
        return readOnlyDataSource;
    }

    /**
     *
     * @param dataSourceName
     * @return
     */
    private static DataSource lookupDataSource(String dataSourceName) {

        try {
            Context initContext = new InitialContext();
            Context context = (Context) initContext.lookup("java:comp/env");
            return (javax.sql.DataSource) context.lookup(dataSourceName);
        } catch (NamingException e) {
            return null;
        }
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
                LOGGER.debug(MessageFormat.format("Found no data source with name {0}, going to create a direct connection",
                        READONLY_DATASOURCE_NAME));
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
     * @param dbName
     * @return
     */
    public static Connection getSQLConnection(String dbName) throws SQLException {

        // first try to create connection through data source (but not when connection is requested to specific DB)
        DataSource dataSource = getReadWriteDataSource();
        if (dbName == null || dbName.trim().length() == 0) {
            if (dataSource != null) {
                return dataSource.getConnection();
            } else if (!isReadWriteDataSourceMissingLogged()) {
                LOGGER.debug(MessageFormat.format(
                        "Found no data source with name {0}, going to create a connection through DriverManager",
                        READWRITE_DATASOURCE_NAME));
                SesameConnectionProvider.readWriteDataSourceMissingLogged = true;
            }
        }

        // no data source was found above, so create the connection through DriverManager

        String urlProperty = GeneralConfig.VIRTUOSO_DB_URL;
        String usrProperty = GeneralConfig.VIRTUOSO_DB_USR;
        String pwdProperty = GeneralConfig.VIRTUOSO_DB_PWD;

        String drv = GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_DRV);
        String url = GeneralConfig.getRequiredProperty(urlProperty);
        String usr = GeneralConfig.getRequiredProperty(usrProperty);
        String pwd = GeneralConfig.getRequiredProperty(pwdProperty);

        if (drv == null || drv.trim().length() == 0) {
            throw new SQLException("Failed to get connection, missing property: " + GeneralConfig.VIRTUOSO_DB_DRV);
        }

        if (url == null || url.trim().length() == 0) {
            throw new SQLException("Failed to get connection, missing property: " + GeneralConfig.VIRTUOSO_DB_URL);
        }

        if (usr == null || usr.trim().length() == 0) {
            throw new SQLException("Failed to get connection, missing property: " + GeneralConfig.VIRTUOSO_DB_USR);
        }

        if (pwd == null || pwd.trim().length() == 0) {
            throw new SQLException("Failed to get connection, missing property: " + GeneralConfig.VIRTUOSO_DB_PWD);
        }

        if (dbName != null && dbName.trim().length() > 0) {
            url = url.trim();
            if (!url.endsWith("/")) {
                url = url + "/";
            }
            url = url + "DATABASE=" + dbName;
        }

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
