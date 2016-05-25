package eionet.cr.migration;

import java.io.File;
import java.io.FileFilter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eionet.cr.config.MigratableCR;
import eionet.cr.dto.DatasetMigrationDTO;
import eionet.cr.service.DatasetMigrationsService;
import eionet.cr.service.ServiceException;
import eionet.cr.util.Util;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.SQLUtil;

/**
 * Thread that runs a given dataset migration.
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMigrationRunner extends Thread {

    /** */
    private static final Logger LOGGER = Logger.getLogger(DatasetMigrationRunner.class);

    /** */
    private static final int TTLP_MASK = 1 + 2 + 4 + 8 + 16 + 32 + 64 + 128;

    /** */
    private int migrationId;

    /** */
    private DatasetMigrationsService migrationsService;

    /** */
    private DatasetMigrationDTO migrationDTO;

    /** */
    private Connection sqlConn;

    /**
     * @param migrationId
     * @param migrationsService
     * @throws ServiceException
     */
    public DatasetMigrationRunner(int migrationId, DatasetMigrationsService migrationsService) throws ServiceException {

        super();

        this.migrationDTO = DatasetMigrationsService.newInstance().findById(migrationId);
        if (this.migrationDTO == null) {
            throw new IllegalArgumentException("Failed to find a dataset migration object by this id: " + migrationId);
        }
        this.migrationId = migrationId;

        if (migrationsService == null) {
            migrationsService = DatasetMigrationsService.newInstance();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        LOGGER.debug("STARTED dataset migration by this id: " + migrationId);

        Exception exception = null;
        try {
            sqlConn = SesameUtil.getSQLConnection();
            runInternal();
            LOGGER.debug("SUCCESS when running dataset migration by this id: " + migrationId);
        } catch (Exception e) {
            exception = e;
            LOGGER.error("FAILURE when running dataset migration by this id: " + migrationId);
        } finally {
            setMigrationFinished(exception);
            SQLUtil.close(sqlConn);
        }
    }

    /**
     *
     * @param exception
     */
    private void setMigrationFinished(Exception exception) {
        try {
            migrationsService.setMigrationFinished(migrationId, new Date(), exception != null, Util.getStackTrace(exception));
        } catch (ServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @throws InterruptedException
     * @throws SQLException
     * @throws DatasetMigrationException
     *
     */
    private void runInternal() throws InterruptedException, SQLException, DatasetMigrationException {

        File packageDir = getPackageDirectory();
        if (!packageDir.exists() || !packageDir.isDirectory()) {
            throw new DatasetMigrationException("Found no such package directory: " + packageDir);
        }

        String metadataGraphUri = migrationDTO.getTargetDatasetUri();
        String dataGraphUri = metadataGraphUri.replace("/dataset/", "/data/");

        Statement stmt = null;
        try {
            stmt = sqlConn.createStatement();
            stmt.execute("log_enable(2,1)");

            // Import data.
            importData(dataGraphUri, packageDir, migrationDTO.isPrePurge());

            // Import metadata.
            importMetadata(metadataGraphUri, packageDir, migrationDTO.isPrePurge());
        } finally {
            SQLUtil.close(stmt);
        }
    }

    /**
     *
     * @return
     * @throws DatasetMigrationException
     */
    private File getPackageDirectory() throws DatasetMigrationException {
        File packageDir;
        String sourceCrUrl = migrationDTO.getSourceCrUrl();
        MigratableCR migratableCR = migrationsService.getMigratableCRByUrl(sourceCrUrl);
        if (migratableCR == null) {
            throw new DatasetMigrationException("Found no CR configuration by this URL: " + sourceCrUrl);
        }

        String packagesDir = migratableCR.getMigrationPackagesDir();
        String packageIdentifier = migrationDTO.getSourcePackageIdentifier();
        packageDir = new File(packagesDir, packageIdentifier);
        return packageDir;
    }

    /**
     *
     * @param dataGraphUri
     * @param packageDir
     * @param prePurge
     * @throws SQLException
     */
    private void importData(String dataGraphUri, File packageDir, boolean prePurge) throws SQLException {

        File[] dataFiles = packageDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().contains(DatasetMigrationPackageFiller.DATA_FILE_SUFFIX);
            }
        });

        if (dataFiles == null || dataFiles.length == 0) {
            LOGGER.warn("Found no data files in " + packageDir);
        }

        if (prePurge) {
            purgeGraphs(dataGraphUri);
        }

        importFiles(dataFiles, dataGraphUri);
    }

    /**
     *
     * @param metadataGraphUri
     * @param packageDir
     * @param prePurge
     * @throws SQLException
     */
    private void importMetadata(String metadataGraphUri, File packageDir, boolean prePurge) throws SQLException {

        File[] metadataFiles = packageDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().contains(DatasetMigrationPackageFiller.METADATA_FILE_SUFFIX);
            }
        });

        if (metadataFiles == null || metadataFiles.length == 0) {
            LOGGER.warn("Found no metadata files in " + packageDir);
        }

        if (prePurge) {
            purgeGraphs(metadataGraphUri);
        }

        importFiles(metadataFiles, metadataGraphUri);
    }

    /**
     *
     * @param files
     * @param targetGraphUri
     * @throws SQLException
     */
    private void importFiles(File[] files, String targetGraphUri) throws SQLException {

        if (StringUtils.isBlank(targetGraphUri)) {
            throw new IllegalArgumentException("Target graph URI must not be blank!");
        }

        PreparedStatement pstmt = null;
        try {
            pstmt = sqlConn.prepareStatement("DB.DBA.TTLP(file_to_string_output(?), '', ?, ?)");

            for (File file : files) {
                pstmt.setString(1, file.getAbsolutePath().replace('\\', '/'));
                pstmt.setString(2, targetGraphUri);
                pstmt.setInt(3, TTLP_MASK);
                pstmt.execute();
            }
        } finally {
            SQLUtil.close(pstmt);
        }
    }

    /**
     *
     * @param graphUris
     * @throws SQLException
     */
    private void purgeGraphs(String... graphUris) throws SQLException {

        for (String graphUri : graphUris) {
            LOGGER.debug("Purging graph: " + graphUri);
            SQLUtil.executeUpdate("sparql clear graph <" + graphUri + ">", sqlConn);
        }
    }
}
