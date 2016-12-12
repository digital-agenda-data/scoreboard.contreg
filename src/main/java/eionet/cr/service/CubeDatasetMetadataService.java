package eionet.cr.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;

import eionet.cr.common.CRRuntimeException;
import eionet.cr.common.TempFilePathGenerator;
import eionet.cr.dao.readers.DatasetMetadataExportReader;
import eionet.cr.dto.CubeDatasetTemplateDTO;
import eionet.cr.freemarker.TemplatesConfiguration;
import eionet.cr.util.Pair;
import eionet.cr.util.Util;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.xlwrap.XLWrapUploadType;
import freemarker.template.Template;

/**
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class CubeDatasetMetadataService {

    // @formatter:off

    /** */
    public static final String DATASETS_SPREADSHEET_TEMPLATE_FILE_NAME = "datasets.xlsx";

    /** */
    private static final String DATA_SHEET_NAME = "DATA";

    /** */
    private static final String CONFIGURATION_SHEET_NAME = "CONFIGURATION";

    /** */
    private static final String DATASET_RDF_TEMPLATE_PATH = "freemarker/dataset-rdf-template.ftl";

    /** */
    public static final String DATASET_URI_PREFIX = "http://semantic.digital-agenda-data.eu/dataset/";

    /** */
    private static final String EXPORT_DATASETS_METADATA_SPARQL = "" +
            "PREFIX cube: <http://purl.org/linked-data/cube#> \n" +
            "select \n" +
            "  ?s ?p ?o \n" +
            "where { \n" +
            "  ?s a cube:DataSet . \n" +
            "  ?s ?p ?o \n" +
            "} \n" +
            "order by ?s ?p ?o";

    // @formatter:on

    /**
     *
     * @return
     */
    public static CubeDatasetMetadataService newInstance() {
        return new CubeDatasetMetadataService();
    }

    /**
     *
     * @param identifier
     * @param title
     * @param description
     * @param dsdUri
     * @return
     * @throws ServiceException
     */
    public String createDataset(String identifier, String title, String description, String dsdUri) throws ServiceException {

        CubeDatasetTemplateDTO dataset = new CubeDatasetTemplateDTO();
        dataset.setUri(DATASET_URI_PREFIX + identifier);
        dataset.setIdentifier(identifier);
        dataset.setTitle(title);
        dataset.setDescription(description);
        dataset.setDsdUri(dsdUri);

        Set<String> datasetUris = createDatasets(Arrays.asList(dataset), false);
        return datasetUris.isEmpty() ? StringUtils.EMPTY : datasetUris.iterator().next();
    }

    /**
     *
     * @param datasets
     * @return
     * @throws ServiceException
     */
    public Set<String> createDatasets(List<CubeDatasetTemplateDTO> datasets, boolean clear) throws ServiceException {

        if (CollectionUtils.isEmpty(datasets)) {
            return Collections.emptySet();
        }

        RepositoryConnection repoConn = null;
        try {

            repoConn = SesameUtil.getRepositoryConnection();
            ValueFactory vf = repoConn.getValueFactory();
            Template template = TemplatesConfiguration.getInstance().getTemplate(DATASET_RDF_TEMPLATE_PATH);
            String modifiedDateTimeStr = Util.virtuosoDateToString(new Date());

            Set<String> datasetUris = new HashSet<>();
            for (CubeDatasetTemplateDTO dataset : datasets) {

                Util.trimToNullAllStringProperties(dataset);
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("dataset", dataset);
                dataset.setModifiedDateTimeStr(modifiedDateTimeStr);

                Writer writer = null;
                Reader reader = null;
                try {
                    writer = new StringWriter();
                    template.process(data, writer);
                    String str = writer.toString();
                    reader = new StringReader(str);

                    String datasetUri = dataset.getUri();
                    URI graphURI = vf.createURI(datasetUri);

                    // If clear requested and this dataset's metadata not yet added (because it could be added multiple times,
                    // e.g. for each row in imported spreadsheet file), then clear the graph.
                    if (clear && !datasetUris.contains(datasetUri)) {
                        repoConn.clear(graphURI);
                    }

                    datasetUris.add(datasetUri);
                    repoConn.add(reader, datasetUri, RDFFormat.TURTLE, graphURI);
                } finally {
                    IOUtils.closeQuietly(reader);
                    IOUtils.closeQuietly(writer);
                }
            }

            return datasetUris;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        } finally {
            SesameUtil.close(repoConn);
        }
    }

    /**
     *
     * @param file
     * @return
     * @throws ServiceException
     */
    public int importDatasetsSpreadsheet(File file, boolean clear) throws ServiceException {

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            Workbook workbook = WorkbookFactory.create(inputStream);
            return importWorkbook(workbook, clear);
        } catch (Exception e) {
            throw new ServiceException("Technical error when importing from file: " + file, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     *
     * @return
     * @throws ServiceException
     */
    public Pair<Integer, File> exportDatasetsMetadata() throws ServiceException {

        URL templateURL = getClass().getClassLoader()
                .getResource(XLWrapUploadType.SPREADSHEETS_PATH + DATASETS_SPREADSHEET_TEMPLATE_FILE_NAME);
        if (templateURL == null) {
            throw new CRRuntimeException(
                    "Could not locate spreadsheet template by the name of " + DATASETS_SPREADSHEET_TEMPLATE_FILE_NAME);
        }

        try {
            File templateFile = new File(templateURL.toURI());
            File targetFile = TempFilePathGenerator.generateWithExtension(FilenameUtils.getExtension(templateFile.getName()));
            FileUtils.copyFile(templateFile, targetFile);

            int exportedCount = exportDatasetsMetadata(templateFile, targetFile);
            return new Pair<Integer, File>(exportedCount, targetFile);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        }
    }

    /**
     *
     * @param propertiesToColumns
     * @param templateFile
     * @param targetFile
     * @return
     * @throws ServiceException
     */
    public int exportDatasetsMetadata(File templateFile, File targetFile) throws ServiceException {

        if (templateFile == null || !templateFile.exists() || !templateFile.isFile()) {
            throw new IllegalArgumentException("The given spreadsheet template must not be null and the file must exist!");
        }

        if (targetFile == null || !targetFile.getParentFile().exists()) {
            throw new IllegalArgumentException("The given spreadsheet targetFile file must not be null and its path must exist!");
        }

        RepositoryConnection repoConn = null;
        try {
            Workbook workbook = WorkbookFactory.create(templateFile);
            Map<String, Integer> rdfPropertiesToColumnIndexes = getRdfPropertiesToColumnIndexes(workbook);
            if (rdfPropertiesToColumnIndexes.isEmpty()) {
                throw new ServiceException("Failed to detect rdf-properties-to-column-indexes mappings from template file!");
            }

            DatasetMetadataExportReader exporter =
                    new DatasetMetadataExportReader(workbook, rdfPropertiesToColumnIndexes, targetFile);
            repoConn = SesameUtil.getRepositoryConnection();

            SesameUtil.executeQuery(EXPORT_DATASETS_METADATA_SPARQL, exporter, repoConn);
            exporter.saveAndClose();
            return exporter.getExportedCount();

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(e.toString(), e);
        } finally {
            SesameUtil.close(repoConn);
        }
    }

    /**
     *
     * @param workbook
     * @throws ServiceException
     */
    private int importWorkbook(Workbook workbook, boolean clear) throws ServiceException {

        Map<String, String> columnsToBeanProperties =
                getTemplateColumnMappings(workbook, SpreadsheetConfigurationProperty.COLUMN_TITLE, SpreadsheetConfigurationProperty.BEAN_PROPERTY);
        if (columnsToBeanProperties.isEmpty()) {
            throw new ServiceException("Could not detect column-to-bean-property mappings!");
        }

        Sheet dataSheet = getDataSheet(workbook);
        if (dataSheet == null) {
            throw new IllegalArgumentException("Failed to find data sheet!");
        }

        Map<Integer, String> indexesToColumns = new HashMap<>();

        Iterator<Row> rows = dataSheet.iterator();
        if (rows.hasNext()) {
            Iterator<Cell> firstRowCells = rows.next().cellIterator();
            while (firstRowCells.hasNext()) {
                Cell cell = firstRowCells.next();
                String strValue = cell == null ? null : StringUtils.trimToNull(cell.getStringCellValue());
                if (strValue != null) {
                    indexesToColumns.put(cell.getColumnIndex(), strValue);
                }
            }
        }

        List<CubeDatasetTemplateDTO> datasets = new ArrayList<>();
        while (rows.hasNext()) {
            Row row = rows.next();
            CubeDatasetTemplateDTO dataset = getDatasetDTO(row, indexesToColumns, columnsToBeanProperties);
            datasets.add(dataset);
        }

        if (datasets.size() > 0) {
            Set<String> datasetUris = createDatasets(datasets, clear);
            return datasetUris.size();
        } else {
            return 0;
        }
    }

    /**
     *
     * @param row
     * @param indexesToColumns
     * @param columnsToBeanProperties
     * @return
     * @throws ServiceException
     */
    private CubeDatasetTemplateDTO getDatasetDTO(Row row, Map<Integer, String> indexesToColumns,
            Map<String, String> columnsToBeanProperties) throws ServiceException {

        CubeDatasetTemplateDTO datasetDTO = new CubeDatasetTemplateDTO();

        Iterator<Cell> cells = row.cellIterator();
        while (cells.hasNext()) {

            Cell cell = cells.next();
            int columnIndex = cell.getColumnIndex();
            String column = indexesToColumns.get(columnIndex);
            if (column != null) {
                String beanPropertyName = columnsToBeanProperties.get(column);
                if (StringUtils.isNotBlank(beanPropertyName)) {
                    String strValue = StringUtils.trimToNull(cell.getStringCellValue());
                    try {
                        BeanUtils.setProperty(datasetDTO, beanPropertyName, strValue);
                    } catch (IllegalAccessException e) {
                        throw new ServiceException(e.getMessage(), e);
                    } catch (InvocationTargetException e) {
                        throw new ServiceException(e.getMessage(), e);
                    }
                }
            }
        }

        if (StringUtils.isBlank(datasetDTO.getUri())) {
            datasetDTO.setUri(DATASET_URI_PREFIX + StringUtils.trimToNull(datasetDTO.getIdentifier()));
        }
        return datasetDTO;
    }

    /**
     *
     * @param workbook
     * @return
     */
    private Map<String, String> getTemplateColumnMappings(Workbook workbook, SpreadsheetConfigurationProperty keyProperty,
            SpreadsheetConfigurationProperty valueProperty) {

        Sheet sheet = workbook.getSheet(CONFIGURATION_SHEET_NAME);
        if (sheet == null) {
            throw new IllegalArgumentException("Failed to find sheet by the name " + CONFIGURATION_SHEET_NAME);
        }

        Iterator<Row> rows = sheet.rowIterator();
        if (!rows.hasNext()) {
            throw new IllegalArgumentException("Missing first row in sheet " + CONFIGURATION_SHEET_NAME);
        }

        Map<String, Integer> columnTitlesToIndexes = new HashMap<>();
        Iterator<Cell> firstRowCells = rows.next().cellIterator();
        while (firstRowCells.hasNext()) {
            Cell cell = firstRowCells.next();
            String strValue = cell == null ? null : StringUtils.trimToNull(cell.getStringCellValue());
            if (strValue != null) {
                columnTitlesToIndexes.put(strValue, cell.getColumnIndex());
            }
        }

        if (columnTitlesToIndexes.isEmpty()) {
            throw new IllegalArgumentException("Expected some column titles in the first row in sheet " + CONFIGURATION_SHEET_NAME);
        }

        Map<String, String> mappings = new LinkedHashMap<>();
        while (rows.hasNext()) {

            Row row = rows.next();
            try {
                String key = row.getCell(columnTitlesToIndexes.get(keyProperty.name())).getStringCellValue();
                String value = row.getCell(columnTitlesToIndexes.get(valueProperty.name())).getStringCellValue();
                if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                    mappings.put(key, value);
                }
            } catch (NullPointerException e) {
                // Ignore deliberately.
            }
        }

        return mappings;
    }

    /**
     *
     * @param workbook
     * @return
     */
    private Sheet getDataSheet(Workbook workbook) {

        Sheet dataSheet = workbook.getSheet(DATA_SHEET_NAME);
        if (dataSheet == null) {
            dataSheet = workbook.getSheetAt(0);
        }

        return dataSheet;
    }

    /**
     *
     * @param workbook
     * @return
     * @throws ServiceException
     */
    private Map<String, Integer> getRdfPropertiesToColumnIndexes(Workbook workbook) throws ServiceException {

        Map<String, String> columnsToRdfProperties =
                getTemplateColumnMappings(workbook, SpreadsheetConfigurationProperty.COLUMN_TITLE, SpreadsheetConfigurationProperty.RDF_PROPERTY_URI);
        if (columnsToRdfProperties.isEmpty()) {
            throw new ServiceException("Could not detect column-to-property mappings!");
        }

        Sheet dataSheet = getDataSheet(workbook);
        if (dataSheet == null) {
            throw new IllegalArgumentException("Failed to find data sheet!");
        }

        Map<String, Integer> rdfPropertiesToColumnIndexes = new HashMap<>();

        Iterator<Row> rows = dataSheet.iterator();
        if (rows.hasNext()) {
            Iterator<Cell> firstRowCells = rows.next().cellIterator();
            while (firstRowCells.hasNext()) {
                Cell cell = firstRowCells.next();
                String columnTitle = cell == null ? null : StringUtils.trimToNull(cell.getStringCellValue());
                if (columnTitle != null) {
                    String rdfProperty = columnsToRdfProperties.get(columnTitle);
                    if (StringUtils.isNotBlank(rdfProperty)) {
                        rdfPropertiesToColumnIndexes.put(rdfProperty, cell.getColumnIndex());
                    }
                }
            }
        }

        return rdfPropertiesToColumnIndexes;
    }

    /**
     * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
     */
    public static enum SpreadsheetConfigurationProperty {
        COLUMN_TITLE, RDF_PROPERTY_URI, BEAN_PROPERTY;
    }
}
