package eionet.cr.service;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import eionet.cr.common.CRRuntimeException;
import eionet.cr.config.GeneralConfig;
import eionet.cr.dto.DatasetMigrationPackageDTO;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.SQLUtil;

/**
 * A service for CRUD operations with dataset migration packages.
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMigrationPackageService {

    /** */
    private static final Logger LOGGER = Logger.getLogger(DatasetMigrationPackageService.class);

    /** Maximum dump file size in bytes. */
    private static final int MAX_DUMP_FILE_SIZE = 1000000000;

    /** */
    private static final String MIGRATION_PACKAGES_DIR_NAME = "migrations";

    /** */
    private static final File MIGRATION_PACKAGES_DIR = initMigrationsDirectory();

    /**
     *
     * @return
     */
    public List<DatasetMigrationPackageDTO> listAll() throws ServiceException {
        return new ArrayList<DatasetMigrationPackageDTO>();
    }

    /**
     *
     * @param dto
     * @throws ServiceException
     */
    public void createNew(DatasetMigrationPackageDTO dto) throws ServiceException {

        if (dto == null) {
            throw new IllegalArgumentException("Migration package DTO must not be null!");
        } else {
            dto.validateForNew();
        }

        String packageName = dto.getName();
        String datasetUri = dto.getDatasetUri();

        File packageDir = createPackageDirectory(packageName);
        fillPackageDirectory(packageDir, datasetUri);
    }

    /**
     *
     * @param packageName
     * @return
     * @throws ServiceException
     */
    private File createPackageDirectory(String packageName) throws ServiceException {

        File packageDir = new File(MIGRATION_PACKAGES_DIR, packageName);
        if (packageDir.exists() && packageDir.isDirectory()) {
            throw new ServiceException("Package with such a name already exists!");
        } else {
            packageDir.mkdir();
        }

        return packageDir;
    }

    /**
     *
     * @param packageDir
     * @throws ServiceException
     */
    private void fillPackageDirectory(File packageDir, String datasetUri) throws ServiceException {

        dumpDataGraph(packageDir, datasetUri);
    }

    /**
     *
     * @param packageDir
     * @param datasetUri
     * @throws ServiceException
     */
    private void dumpDataGraph(File packageDir, String datasetUri) throws ServiceException {

        File dumpFile = new File(packageDir, packageDir.getName() + "_data_");
        String dumpFilePath = dumpFile.getAbsolutePath().replace('\\', '/');

        String dataGraphUri = datasetUri.replace("/dataset/", "/data/");
        String dataDumpSQL = String.format("DB.DBA.dump_one_graph('%s', '%s', %d)", dataGraphUri, dumpFilePath, MAX_DUMP_FILE_SIZE);

        LOGGER.debug(String.format("Dumping graph [%s] into [%s]", dataGraphUri, dumpFilePath));

        Connection sqlConn = null;
        Statement stmt = null;
        try {
            sqlConn = SesameUtil.getSQLConnection();
            stmt = sqlConn.createStatement();
            stmt.execute("log_enable(2,1)");

            LOGGER.debug("Executing statement: " + dataDumpSQL);
            // FIXME SQL injection possible here, but does Virtuoso JDBC work with procedure inputs being parameterized?
            boolean result = stmt.execute(dataDumpSQL);
            LOGGER.debug("Statement result: " + result);
        } catch (Exception e) {
            throw new ServiceException("Failure when dumping data graph", e);
        } finally {
            SQLUtil.close(stmt);
            SQLUtil.close(sqlConn);
        }
    }

    /**
     *
     * @return
     */
    public static DatasetMigrationPackageService newInstance() {
        return new DatasetMigrationPackageService();
    }

    /**
     * @return
     *
     */
    private static File initMigrationsDirectory() {

        String appHomeDir = GeneralConfig.getRequiredProperty(GeneralConfig.APP_HOME_DIR);
        File migrationPackagesDir = new File(appHomeDir, MIGRATION_PACKAGES_DIR_NAME);
        if (!migrationPackagesDir.exists() || !migrationPackagesDir.isDirectory()) {
            try {
                boolean success = migrationPackagesDir.mkdirs();
                if (!success) {
                    throw new CRRuntimeException("Failed to create directory: " + migrationPackagesDir);
                }
            } catch (SecurityException e) {
                throw new CRRuntimeException("Failed to create directory: " + migrationPackagesDir, e);
            }
        }

        return migrationPackagesDir;
    }
}
