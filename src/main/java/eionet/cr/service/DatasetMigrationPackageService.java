package eionet.cr.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;

import eionet.cr.common.CRRuntimeException;
import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.config.GeneralConfig;
import eionet.cr.config.MigratableCR;
import eionet.cr.dto.DatasetMigrationPackageDTO;
import eionet.cr.migration.DatasetMigrationPackageFiller;

/**
 * A service for CRUD operations with dataset migration packages.
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMigrationPackageService {

    /** */
    private static final Logger LOGGER = Logger.getLogger(DatasetMigrationPackageService.class);

    /** */
    public static final String STARTED_FILENAME = "started";

    /** */
    public static final String FINISHED_FILENAME = "finished";

    /** */
    public static final String MIGRATION_PACKAGES_DIR_NAME = "migration_packages";

    /** */
    public static final File MIGRATION_PACKAGES_DIR = initMigrationsDirectory();

    /**
     *
     * @return
     */
    public List<DatasetMigrationPackageDTO> listLocal() throws ServiceException {

        return listPackages(MIGRATION_PACKAGES_DIR);
    }

    /**
     *
     * @param migratableCR
     * @return
     * @throws ServiceException
     */
    public List<DatasetMigrationPackageDTO> listRemote(MigratableCR migratableCR) throws ServiceException {

        if (migratableCR == null || StringUtils.isBlank(migratableCR.getMigrationPackagesDir())) {
            throw new IllegalArgumentException("Migratable CR and its migration packages directory property must not be blank!");
        }

        File packagesDir = new File(migratableCR.getMigrationPackagesDir());
        return listPackages(packagesDir);
    }

    /**
     *
     * @param packagesDir
     * @return
     * @throws ServiceException
     */
    private List<DatasetMigrationPackageDTO> listPackages(File packagesDir) throws ServiceException {

        if (packagesDir == null) {
            throw new IllegalArgumentException("Packages directory must not be null!");
        }

        ArrayList<DatasetMigrationPackageDTO> resultList = new ArrayList<DatasetMigrationPackageDTO>();
        if (!packagesDir.exists() || !packagesDir.isDirectory()) {
            return resultList;
        }

        try {
            File[] packageDirectories = packagesDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });

            for (File packageDir : packageDirectories) {

                DatasetMigrationPackageDTO dto = readPackageDTO(packageDir);
                resultList.add(dto);
            }
        } catch (Exception e) {
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

        try {
            // Create package directory.
            File packageDir = createPackageDirectory(dto);

            // Fill package directory (will be done in a separate thread).
            DatasetMigrationPackageFiller filler = new DatasetMigrationPackageFiller(packageDir, dto.getDatasetUri());
            filler.start();
        } catch (IOException ioe) {
            throw new ServiceException("Error when creating migration package directory", ioe);
        }
    }

    /**
     *
     * @param packageIdentifiers
     * @throws ServiceException
     */
    public void delete(List<String> packageIdentifiers) throws ServiceException {

        if (CollectionUtils.isEmpty(packageIdentifiers)) {
            return;
        }

        for (String identifier : packageIdentifiers) {

            File packageDir = getPackageDir(identifier);
            try {
                LOGGER.debug("Attempting to delete package directory: " + packageDir.getAbsolutePath());
                FileUtils.deleteDirectory(packageDir);
            } catch (IOException e) {
                throw new ServiceException("Error when attmepting to delete package directory: " + packageDir.getAbsolutePath(), e);
            }
        }
    }

    public void getPackageDatasetUri(String sourceCrUrl, String sourcePackageIdentifier) {
        ;
    }

    /**
     *
     * @param packageDir
     * @return
     * @throws IOException
     * @throws OpenRDFException
     * @throws ServiceException
     */
    private DatasetMigrationPackageDTO readPackageDTO(File packageDir) throws IOException, ServiceException, OpenRDFException {

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

        readDatasetMetadata(packageDir, dto);
        LOGGER.debug("Dataset URI of the read package: " + dto.getDatasetUri());

        return dto;
    }

    /**
     *
     * @param packageDir
     * @param packageDTO
     * @throws ServiceException
     * @throws OpenRDFException
     * @throws IOException
     */
    private void readDatasetMetadata(File packageDir, final DatasetMigrationPackageDTO packageDTO)
            throws ServiceException, IOException, OpenRDFException {

        File[] metadataFiles = packageDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().endsWith(DatasetMigrationPackageFiller.METADATA_FILE_SUFFIX);
            }
        });

        if (metadataFiles.length == 0) {
            throw new ServiceException("Was unable to find metadata file in package directory: " + packageDir);
        }

        readDatasetUri(metadataFiles[0], packageDTO);
    }

    /**
     *
     * @param metadataFile
     * @param packageDTO
     * @throws IOException
     * @throws OpenRDFException
     */
    private void readDatasetUri(File metadataFile, final DatasetMigrationPackageDTO packageDTO) throws IOException, OpenRDFException {

        LOGGER.debug("Attmepting to read dataset URI from " + metadataFile);

        RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
        rdfParser.setRDFHandler(new RDFHandler() {

            @Override
            public void startRDF() throws RDFHandlerException {
            }

            @Override
            public void handleStatement(Statement statement) throws RDFHandlerException {

                URI predicate = statement.getPredicate();
                Value object = statement.getObject();
                if (Predicates.RDF_TYPE.equals(predicate.stringValue()) && Subjects.DATACUBE_DATA_SET.equals(object.stringValue())) {
                    packageDTO.setDatasetUri(statement.getSubject().stringValue());
                    throw new CRRuntimeException();
                }
            }

            @Override
            public void handleNamespace(String str1, String str2) throws RDFHandlerException {
                // Not interested in this part.
            }

            @Override
            public void handleComment(String str) throws RDFHandlerException {
                // Not interested in this part.
            }

            @Override
            public void endRDF() throws RDFHandlerException {
            }
        });

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(metadataFile));
            rdfParser.parse(reader, StringUtils.EMPTY);
        } catch (CRRuntimeException cre) {
            // CRRuntimeException here means we found the dataset URI, no need to parse the file any further.
            LOGGER.debug("Dataset URI found: " + packageDTO.getDatasetUri());
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    /**
     *
     * @param dto
     * @return
     * @throws IOException
     */
    private File createPackageDirectory(DatasetMigrationPackageDTO dto) throws IOException {

        File packageDir = getPackageDir(dto.getIdentifier());
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
     * @return
     */
    public static DatasetMigrationPackageService newInstance() {
        return new DatasetMigrationPackageService();
    }

    /**
     *
     * @param packageIdentifier
     * @return
     */
    private File getPackageDir(String packageIdentifier) {
        return new File(MIGRATION_PACKAGES_DIR, packageIdentifier);
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
