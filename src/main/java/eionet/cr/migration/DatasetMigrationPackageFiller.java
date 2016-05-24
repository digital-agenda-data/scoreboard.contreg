package eionet.cr.migration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import eionet.cr.config.GeneralConfig;
import eionet.cr.service.DatasetMigrationPackageService;
import eionet.cr.util.Util;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.SQLUtil;

/**
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMigrationPackageFiller extends Thread {

    /** */
    private static final Logger LOGGER = Logger.getLogger(DatasetMigrationPackageFiller.class);

    /** */
    private static final String SELF_SPARQL_ENDPOINT_URL = GeneralConfig.getRequiredProperty(GeneralConfig.SELF_SPARQL_ENDPOINT_URL);

    /** Maximum dump file size in bytes. */
    private static final int MAX_DUMP_FILE_SIZE = 1000000000;

    /** */
    private File packageDir;

    /** */
    private String datasetUri;

    /**
     * @param packageDir
     * @param datasetUri
     */
    public DatasetMigrationPackageFiller(File packageDir, String datasetUri) {
        super();
        this.packageDir = packageDir;
        this.datasetUri = datasetUri;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        LOGGER.debug(String.format("STARTED package filler package [%s] for dataset [%s]", packageDir, datasetUri));

        Exception exception = null;
        try {
            runInternal();
            LOGGER.debug(String.format("SUCCESS package filler package [%s] for dataset [%s]", packageDir, datasetUri));
        } catch (Exception e) {
            exception = e;
            LOGGER.error(String.format("FAILURE package filler package [%s] for dataset [%s]", packageDir, datasetUri), e);
        } finally {
            createFinishedFile(exception);
        }
    }

    /**
     * @throws IOException
     * @throws SQLException
     *
     */
    private void runInternal() throws IOException, SQLException {

        // Dump the dataset's metadata.
        dumpMetadataGraph(packageDir, datasetUri);

        // Dump the dataset's data.
        dumpDataGraph(packageDir, datasetUri);

        // Remove all *.graph files.
        removeFilesWithExtension(packageDir, ".graph");
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
        String metadataQueryURL = String.format("%s?query=%s&format=%s", SELF_SPARQL_ENDPOINT_URL, URLEncoder.encode(metadataQuery, "UTF-8"),
                URLEncoder.encode("text/turtle", "UTF-8"));

        LOGGER.debug(String.format("Dumping %s metadata to %s", datasetUri, packageDir));
        FileUtils.copyURLToFile(new URL(metadataQueryURL), dumpFile);
    }

    /**
     *
     * @param packageDir
     * @param datasetUri
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
     * @param exception
     */
    private void createFinishedFile(Exception exception) {
        try {
            if (packageDir != null && packageDir.exists()) {
                File finishedFile = new File(packageDir, DatasetMigrationPackageService.FINISHED_FILENAME);
                finishedFile.createNewFile();
                if (exception != null) {
                    FileUtils.writeStringToFile(finishedFile, Util.getStackTrace(exception));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create the finished file!");
        }
    }

}
