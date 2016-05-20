package eionet.cr.service;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
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
    private static final String STARTED_FILENAME = "started";

    /** */
    private static final String FINISHED_FILENAME = "finished";

    /** */
    private static final String MIGRATION_PACKAGES_DIR_NAME = "migrations";

    /** */
    private static final File MIGRATION_PACKAGES_DIR = initMigrationsDirectory();

    /** */
    private static final String SELF_SPARQL_ENDPOINT_URL = GeneralConfig.getRequiredProperty(GeneralConfig.SELF_SPARQL_ENDPOINT_URL);

    /**
     *
     * @return
     */
    public List<DatasetMigrationPackageDTO> listAll() throws ServiceException {

        // Create the result list.

        ArrayList<DatasetMigrationPackageDTO> resultList = new ArrayList<DatasetMigrationPackageDTO>();
        try {
            File[] packageDirectories = MIGRATION_PACKAGES_DIR.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });

            for (File packageDir : packageDirectories) {

                DatasetMigrationPackageDTO dto = createPackageDTO(packageDir);
                resultList.add(dto);
            }
        } catch (IOException e) {
            throw new ServiceException(e.getMessage(), e);
        }

        // Sort the result list.

        Collections.sort(resultList, new Comparator<DatasetMigrationPackageDTO>() {
            @Override
            public int compare(DatasetMigrationPackageDTO pack1, DatasetMigrationPackageDTO pack2) {

                Date started1 = pack1.getStarted();
                Date started2 = pack2.getStarted();
                if (started1 == null && started2 == null) {
                    return 0;
                } else if (started1 == null && started2 != null) {
                    return 1;
                } else if (started1 != null && started2 == null) {
                    return -1;
                } else {
                    return started2.compareTo(started1);
                }
            }
        });

        return resultList;
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

        String packageIdentifier = dto.getIdentifier();
        String datasetUri = dto.getDatasetUri();

        String errorStackTrace = null;
        File packageDir = null;
        try {
            // Create package directory.
            packageDir = createPackageDirectory(packageIdentifier);

            // Fill package directory.
            fillPackageDirectory(packageDir, datasetUri);
        } catch (Exception e) {
            errorStackTrace = e.toString();
            throw new ServiceException(e.getMessage(), e);
        } finally {
            // Create the "finished" file.
            createFinishedFile(errorStackTrace, packageDir);
        }
    }

    /**
     *
     * @param errorStackTrace
     * @param packageDir
     */
    private void createFinishedFile(String errorStackTrace, File packageDir) {
        try {
            if (packageDir != null && packageDir.exists()) {
                File finishedFile = new File(packageDir, FINISHED_FILENAME);
                finishedFile.createNewFile();
                if (StringUtils.isNotBlank(errorStackTrace)) {
                    FileUtils.writeStringToFile(finishedFile, errorStackTrace);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create the finished file!");
        }
    }

    /**
     *
     * @param packageDir
     * @return
     * @throws IOException
     */
    private DatasetMigrationPackageDTO createPackageDTO(File packageDir) throws IOException {

        DatasetMigrationPackageDTO dto = new DatasetMigrationPackageDTO();
        dto.setIdentifier(packageDir.getName());

        File startedFile = new File(packageDir, STARTED_FILENAME);
        File finishedFile = new File(packageDir, FINISHED_FILENAME);

        Date startedDate = startedFile.exists() ? new Date(startedFile.lastModified()) : null;
        Date finishedDate = finishedFile.exists() ? new Date(finishedFile.lastModified()) : null;

        dto.setStarted(startedDate);
        dto.setFinished(finishedDate);

        if (finishedFile.exists() && finishedFile.isFile()) {
            String finishedFileContents = FileUtils.readFileToString(finishedFile);
            if (StringUtils.isNotBlank(finishedFileContents)) {
                dto.setFinishedErrorMessage(finishedFileContents);
            }
        }

        return dto;
    }

    /**
     *
     * @param packageIdentifier
     * @return
     * @throws IOException
     */
    private File createPackageDirectory(String packageIdentifier) throws IOException {

        File packageDir = new File(MIGRATION_PACKAGES_DIR, packageIdentifier);
        if (packageDir.exists() && packageDir.isDirectory()) {
            throw new IllegalArgumentException("Package with such a name already exists!");
        }

        packageDir.mkdir();

        // Create the "started" file.
        new File(packageDir, STARTED_FILENAME).createNewFile();

        return packageDir;
    }

    /**
     *
     * @param packageDir
     * @throws IOException
     * @throws SQLException
     */
    private void fillPackageDirectory(File packageDir, String datasetUri) throws IOException, SQLException {

        // Dump the dataset's metadata.
        dumpMetadataGraph(packageDir, datasetUri);

        // Dump the dataset's data.
        dumpDataGraph(packageDir, datasetUri);

        // Remove all *.graph files.
        removeFilesWithExtension(packageDir, ".graph");
    }

    /**
     *
     * @param dir
     * @param extension
     */
    private void removeFilesWithExtension(File dir, final String extension) {

        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return name.endsWith(extension.startsWith(".") ? extension : "." + extension);
            }
        });
        for (File file : files) {
            LOGGER.debug("Deleting file: " + file.getAbsolutePath());
            file.delete();
        }
    }

    /**
     *
     * @param packageDir
     * @param datasetUri
     * @throws IOException
     */
    private void dumpMetadataGraph(File packageDir, String datasetUri) throws IOException {

        File dumpFile = new File(packageDir, packageDir.getName() + "_metadata.ttl");

        String metadataQuery = String.format("CONSTRUCT {?s ?p ?o } WHERE {?s ?p ?o filter (?s = <%s>)}", datasetUri);
        String metadataQueryURL = String.format("%s?query=%s&format=%s", SELF_SPARQL_ENDPOINT_URL, URLEncoder.encode(metadataQuery , "UTF-8"),
                URLEncoder.encode("text/turtle", "UTF-8"));

        LOGGER.debug(String.format("Dumping %s metadata to %s", datasetUri, packageDir));
        FileUtils.copyURLToFile(new URL(metadataQueryURL), dumpFile);
    }

    /**
     *
     * @param packageDir
     * @param datasetUri
     * @throws SQLException
     * @throws SQLException
     */
    private void dumpDataGraph(File packageDir, String datasetUri) throws SQLException {

        File dumpFile = new File(packageDir, packageDir.getName() + "_data_");
        String dumpFilePath = dumpFile.getAbsolutePath().replace('\\', '/');

        String dataGraphUri = datasetUri.replace("/dataset/", "/data/");
        // String dataDumpSQL = String.format("DB.DBA.dump_one_graph('%s', '%s', %d)", dataGraphUri, dumpFilePath,
        // MAX_DUMP_FILE_SIZE);
        String dataDumpSQL = "DB.DBA.dump_one_graph(?, ?, ?)";

        LOGGER.debug(String.format("Dumping graph [%s] into [%s]", dataGraphUri, dumpFilePath));

        Connection sqlConn = null;
        PreparedStatement pstmt = null;
        try {
            sqlConn = SesameUtil.getSQLConnection();
            pstmt = sqlConn.prepareStatement("log_enable(2,1)");
            pstmt.execute();
            SQLUtil.close(pstmt);

            LOGGER.debug(String.format("Executing statement [%s] with parameters: [%s], [%s], [%d]", dataDumpSQL, dataGraphUri, dumpFile,
                    MAX_DUMP_FILE_SIZE));
            pstmt = sqlConn.prepareStatement(dataDumpSQL);
            pstmt.setString(1, dataGraphUri);
            pstmt.setString(2, dumpFilePath);
            pstmt.setInt(3, MAX_DUMP_FILE_SIZE);
            pstmt.execute();
        } finally {
            SQLUtil.close(pstmt);
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
