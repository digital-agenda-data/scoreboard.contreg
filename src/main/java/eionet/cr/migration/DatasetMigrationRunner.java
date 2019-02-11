package eionet.cr.migration;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.ScoreboardSparqlDAO;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eionet.cr.config.MigratableCR;
import eionet.cr.dto.DatasetMigrationDTO;
import eionet.cr.service.DatasetMigrationsService;
import eionet.cr.service.ServiceException;
import eionet.cr.util.FileDeletionJob;
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
    private static final String TEMP_GRAPH_SUFFIX = "_TEMP";

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

        this.migrationsService = migrationsService;
        if (this.migrationsService == null) {
            this.migrationsService = DatasetMigrationsService.newInstance();
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
            LOGGER.debug("Obtaining SQL connection ...");
            sqlConn = SesameUtil.getSQLConnection();
            runInternal();
            LOGGER.debug("SUCCESS when running dataset migration by this id: " + migrationId);
        } catch (Exception e) {
            exception = e;
            LOGGER.error("FAILURE when running dataset migration by this id: " + migrationId, e);
        } finally {
            setMigrationFinished(exception);
            SQLUtil.close(sqlConn);
        }
    }

    /**
     * @throws InterruptedException
     * @throws SQLException
     * @throws DatasetMigrationException
     * @throws IOException
     *
     */
    private void runInternal() throws InterruptedException, SQLException, DatasetMigrationException, IOException {

        File packageDir = getPackageDirectory();
        if (!packageDir.exists() || !packageDir.isDirectory()) {
            throw new DatasetMigrationException("Found no such package directory: " + packageDir);
        }

        String metadataGraphUri = migrationDTO.getTargetDatasetUri();
        String dataGraphUri = metadataGraphUri.replace("/dataset/", "/data/");

        makeCheckpoint();

        // Import data.
        importData(dataGraphUri, packageDir, migrationDTO.isPrePurge());

        // Import metadata.
        setAutoCommit(false);
        importMetadata(metadataGraphUri, packageDir, migrationDTO.isPrePurge());

        // Update dataset modification date.
        DAOFactory.get().getDao(ScoreboardSparqlDAO.class).updateSubjectModificationDate(sqlConn, metadataGraphUri, metadataGraphUri);

        // If we have reached this point without any exceptions, make a checkpoint.
        makeCheckpoint();
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
     * @throws IOException
     */
    private void importData(String dataGraphUri, File packageDir, boolean prePurge) throws SQLException, IOException {

        File[] dataFiles = packageDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().contains(DatasetMigrationPackageFiller.DATA_FILE_SUFFIX);
            }
        });

        if (dataFiles == null || dataFiles.length == 0) {
            LOGGER.warn("Found no data files in " + packageDir);
            return;
        }

        LOGGER.debug(String.format("Importing data files from [%s] into [%s]", packageDir, dataGraphUri));
        importFiles(dataFiles, dataGraphUri, prePurge);
    }

    /**
     *
     * @param metadataGraphUri
     * @param packageDir
     * @param prePurge
     * @throws SQLException
     * @throws IOException
     */
    private void importMetadata(String metadataGraphUri, File packageDir, boolean prePurge) throws SQLException, IOException {

        File[] metadataFiles = packageDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().contains(DatasetMigrationPackageFiller.METADATA_FILE_SUFFIX);
            }
        });

        if (metadataFiles == null || metadataFiles.length == 0) {
            LOGGER.warn("Found no metadata files in " + packageDir);
            return;
        }

        LOGGER.debug(String.format("Importing metadata files from [%s] into [%s]", packageDir, metadataGraphUri));
        importFiles(metadataFiles, metadataGraphUri, prePurge);
    }

    /**
     *
     * @param dataFiles
     * @param graphUri
     * @param prePurge
     * @throws IOException
     * @throws SQLException
     */
    private void importFiles(File[] dataFiles, String graphUri, boolean prePurge) throws IOException, SQLException {

        // TODO: do something with prepUrge!

        if (StringUtils.isBlank(graphUri)) {
            throw new IllegalArgumentException("Data graph URI must not be blank!");
        }

        // Unzip all zipped files (remember to delete the unzipped ones afterwards).

        ArrayList<File> filesToDeleteFinally = new ArrayList<File>();
        List<File> filesToImport = prepareFiles(dataFiles, filesToDeleteFinally);

        String tempGraphUri = graphUri + TEMP_GRAPH_SUFFIX;
        try {
            // Ensure temporary graph is clear.
            setAutoCommit(false);
            clearGraphs(tempGraphUri);

            // Import files into temporary graph.
            for (File file : filesToImport) {
                setAutoCommit(false);
                importFile(file, tempGraphUri);
            }

            // Delete the real graph.
            setAutoCommit(false);
            clearGraphs(graphUri);

            // Rename temporary graph to real one.
            setAutoCommit(false);
            renameGraph(tempGraphUri, graphUri);
        } finally {
            FileDeletionJob.register(filesToDeleteFinally);
            setAutoCommit(false);
            clearGraphsQuietly(tempGraphUri);
        }
    }

    /**
     *
     * @param file
     * @param graphUri
     * @throws SQLException
     */
    private void importFile(File file, String graphUri) throws SQLException {

        LOGGER.debug(String.format("Importing file [%s] into graph [%s]", file.getAbsolutePath(), graphUri));

        String filePath = file.getAbsolutePath().replace('\\', '/');
        String sql = String.format("DB.DBA.TTLP(file_to_string_output('%s'), '', '%s', %d)", filePath, graphUri, TTLP_MASK);

        Statement stmt = null;
        try {
            stmt = sqlConn.createStatement();
            stmt.execute(sql);
        } finally {
            SQLUtil.close(stmt);
        }
    }

    /**
     *
     * @param exception
     */
    private void setMigrationFinished(Exception exception) {
        try {
            String messages = exception == null ? null : Util.getStackTrace(exception);
            migrationsService.setMigrationFinished(migrationId, new Date(), exception != null, messages);
        } catch (Exception e) {
            LOGGER.error("Failed to set migration finished", e);
        }
    }

    /**
     *
     * @param files
     * @param filesToDeleteAfterwards
     * @return
     * @throws IOException
     */
    private List<File> prepareFiles(File[] files, ArrayList<File> filesToDeleteAfterwards) throws IOException {

        ArrayList<File> filesToImport = new ArrayList<File>();
        for (File file : files) {

            File fileToImport = file;

            boolean isGzippedFile = file.getName().toLowerCase().endsWith(".gz");
            if (isGzippedFile) {
                File gunzippedFile = gunzipFile(file);
                if (gunzippedFile != null && gunzippedFile.exists()) {
                    filesToDeleteAfterwards.add(gunzippedFile);
                    fileToImport = gunzippedFile;
                }
            }

            filesToImport.add(fileToImport);
        }

        return filesToImport;
    }

    /**
     *
     * @param graphUris
     * @throws SQLException
     */
    private void clearGraphs(String... graphUris) throws SQLException {

        if (ArrayUtils.isEmpty(graphUris)) {
            return;
        }

        Statement stmt = null;
        try {
            stmt = sqlConn.createStatement();
            for (String graphUri : graphUris) {
                LOGGER.debug("Clearing graph: " + graphUri);
                stmt.execute(String.format("SPARQL CLEAR GRAPH <%s>", graphUri));
            }
        } finally {
            SQLUtil.close(stmt);
        }
    }

    /**
     *
     * @param graphUris
     * @throws SQLException
     */
    private void clearGraphsQuietly(String... graphUris) throws SQLException {

        try {
            clearGraphs(graphUris);
        } catch (Exception e) {
            // Ignore deliberately.
        }
    }

    /**
     *
     * @param oldUri
     * @param newUri
     * @throws SQLException
     */
    private void renameGraph(String oldUri, String newUri) throws SQLException {

        LOGGER.debug(String.format("Renaming graph [%s] to [%s]", oldUri, newUri));

        // We're not using prepared statement here, because its imply does not with graph rename query for some reason.
        String sql = String.format("UPDATE DB.DBA.RDF_QUAD TABLE OPTION (index RDF_QUAD_GS) SET g = iri_to_id ('%s') WHERE g = iri_to_id ('%s',0)",
                newUri, oldUri);

        Statement stmt = null;
        try {
            stmt = sqlConn.createStatement();
            stmt.execute(sql);
        } finally {
            SQLUtil.close(stmt);
        }
    }

    /**
     *
     * @param file
     * @return
     * @throws IOException
     */
    private File gunzipFile(File file) throws IOException {

        String fileName = file.getName();
        int suffixIndex = fileName.toLowerCase().indexOf(".gz");

        String outFileName = suffixIndex > 0 ? fileName.substring(0, suffixIndex) : fileName;
        File outFile = new File(file.getParentFile(), outFileName);

        LOGGER.debug("Unzipping " + file);

        GZIPInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new GZIPInputStream(new FileInputStream(file));
            outputStream = new FileOutputStream(outFile);
            IOUtils.copy(inputStream, outputStream);
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
        }

        return outFile;
    }

    /**
     *
     * @param flag
     * @throws SQLException
     */
    private void setAutoCommit(boolean flag) throws SQLException {

        if (flag == true) {
            logEnable(1, 1); // TODO Not sure about this actually...
        } else {
            logEnable(2, 1);
        }
    }

    /**
     *
     * @param mode
     * @param quietly
     * @throws SQLException
     */
    private void logEnable(int mode, int quietly) throws SQLException {

        Statement stmt = null;
        try {
            stmt = sqlConn.createStatement();
            stmt.execute(String.format("log_enable(%d,%d)", mode, quietly));
        } finally {
            SQLUtil.close(stmt);
        }
    }

    /**
     *
     * @throws SQLException
     */
    private void makeCheckpoint() throws SQLException {

        LOGGER.debug("Making checkpoint ...");

        Statement stmt = null;
        try {
            stmt = sqlConn.createStatement();
            stmt.execute("checkpoint");
        } finally {
            SQLUtil.close(stmt);
        }
    }
}
