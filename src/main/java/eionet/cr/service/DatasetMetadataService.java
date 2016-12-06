package eionet.cr.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
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
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;

import eionet.cr.dao.readers.DatasetMetadataExportReader;
import eionet.cr.util.Util;
import eionet.cr.util.sesame.SesameUtil;

/**
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMetadataService {

    // @formatter:off

    /** */
    private static final String DATA_SHEET_NAME = "DATA";

    /** */
    private static final String CONFIGURATION_SHEET_NAME = "CONFIGURATION";

    /** */
    private static final String METADATA_SHEET_DEFAULT_INDEX = "0";

    /** */
    private static final String DATASET_METADATA_TTL_TEMPLATE_FILE = "velocity/new-dataset-metadata.vm";

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
    public static DatasetMetadataService newInstance() {
        return new DatasetMetadataService();
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

        Map<String, String> map = new HashMap<>();
        map.put(RdfTemplateVariable.DATASET_IDENTIFIER.name(), identifier);
        map.put(RdfTemplateVariable.DATASET_TITLE.name(), title);
        map.put(RdfTemplateVariable.DATASET_DESCRIPTION.name(), description);
        map.put(RdfTemplateVariable.DATASET_DSD.name(), dsdUri);
        Set<String> datasetUris = createDatasets(Arrays.asList(map), false);

        return datasetUris.isEmpty() ? StringUtils.EMPTY : datasetUris.iterator().next();
    }

    /**
     *
     * @param datasetMaps
     * @return
     * @throws ServiceException
     */
    public Set<String> createDatasets(List<Map<String, String>> datasetMaps, boolean clear) throws ServiceException {

        if (CollectionUtils.isEmpty(datasetMaps)) {
            return Collections.emptySet();
        }

        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
        Template template = ve.getTemplate(DATASET_METADATA_TTL_TEMPLATE_FILE);

        String modifiedDateStr = Util.virtuosoDateToString(new Date());

        RepositoryConnection repoConn = null;
        try {

            repoConn = SesameUtil.getRepositoryConnection();
            ValueFactory vf = repoConn.getValueFactory();

            Set<String> datasetUris = new HashSet<>();
            for (Map<String, String> datasetMap : datasetMaps) {

                VelocityContext context = new VelocityContext();
                for (Entry<String, String> entry : datasetMap.entrySet()) {
                    String variable = entry.getKey();
                    String value = entry.getValue();
                    context.put(variable, value);
                }
                context.put(RdfTemplateVariable.DATASET_MODIFIED.name(), modifiedDateStr);

                Writer writer = null;
                Reader reader = null;
                try {
                    writer = new StringWriter();
                    template.merge(context, writer);
                    String str = writer.toString();
                    reader = new StringReader(str);

                    String datasetUri = DATASET_URI_PREFIX + datasetMap.get(RdfTemplateVariable.DATASET_IDENTIFIER.name());
                    URI graphURI = vf.createURI(datasetUri);

                    // If clear requested and this dataset's metadata not yet added (because it could be added multiple times, e.g.
                    // for each row
                    // in imported spreadsheet file), then clear the graph.
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
            throw new ServiceException("Error when reading from file: " + file, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
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
    public int exportDatasetsMetadata(Map<String, Integer> propertiesToColumns, File templateFile, File targetFile)
            throws ServiceException {

        if (templateFile == null || !templateFile.exists() || !templateFile.isFile()) {
            throw new IllegalArgumentException("The given spreadsheet template must not be null and the file must exist!");
        }

        if (MapUtils.isEmpty(propertiesToColumns)) {
            throw new IllegalArgumentException("The given map of property-to-column mappings must not be null nor empty!");
        }

        RepositoryConnection repoConn = null;
        try {
            repoConn = SesameUtil.getRepositoryConnection();

            DatasetMetadataExportReader exporter = new DatasetMetadataExportReader(templateFile, targetFile);
            SesameUtil.executeQuery(EXPORT_DATASETS_METADATA_SPARQL, exporter, repoConn);
            exporter.saveAndClose();
            return exporter.getExportedCount();

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

        Map<String, String> columnsToVariables = getTemplateColumnMappings(workbook, TemplateColumnProperty.COLUMN_TITLE,
                TemplateColumnProperty.RDF_TEMPLATE_VARIABLE);
        if (columnsToVariables.isEmpty()) {
            throw new ServiceException("Could not detect column-to-variable mappings!");
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

        List<Map<String, String>> datasetMaps = new ArrayList<>();
        while (rows.hasNext()) {

            Row row = rows.next();
            Map<String, String> datasetMap = getVariablesMap(row, indexesToColumns, columnsToVariables);
            if (!datasetMap.isEmpty()) {
                datasetMaps.add(datasetMap);
            }
        }

        if (datasetMaps.size() > 0) {
            Set<String> datasetUris = createDatasets(datasetMaps, clear);
            return datasetUris.size();
        } else {
            return 0;
        }
    }

    /**
     *
     * @param row
     * @param indexesToColumns
     * @param columnsToVariables
     * @return
     */
    private Map<String, String> getVariablesMap(Row row, Map<Integer, String> indexesToColumns, Map<String, String> columnsToVariables) {

        Map<String, String> rowMap = new HashMap<>();

        Iterator<Cell> cells = row.cellIterator();
        while (cells.hasNext()) {

            Cell cell = cells.next();
            int columnIndex = cell.getColumnIndex();
            String column = indexesToColumns.get(columnIndex);
            if (column != null) {
                String variable = columnsToVariables.get(column);
                if (variable != null) {
                    String strValue = StringUtils.trimToEmpty(cell.getStringCellValue());
                    rowMap.put(variable, strValue);
                }
            }
        }
        return rowMap;
    }

    /**
     *
     * @param workbook
     * @return
     */
    private Map<String, String> getTemplateColumnMappings(Workbook workbook, TemplateColumnProperty keyProperty,
            TemplateColumnProperty valueProperty) {

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
     * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
     */
    public static enum RdfTemplateVariable {
        DATASET_IDENTIFIER, DATASET_TITLE, DATASET_DESCRIPTION, DATASET_DSD, DATASET_MODIFIED, DATASET_KEYWORD, DATASET_ISSUED,
        DATASET_LICENSE, DATASET_STATUS, DATASET_PERIODICITY;
    }

    /**
     * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
     */
    public static enum TemplateColumnProperty {
        COLUMN_TITLE, RDF_PROPERTY_URI, RDF_TEMPLATE_VARIABLE;
    }
}
