package eionet.cr.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;

import eionet.cr.dao.DAOException;
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

        Map<RdfTemplateVariable, String> map = new HashMap<>();
        map.put(RdfTemplateVariable.DATASET_IDENTIFIER, identifier);
        map.put(RdfTemplateVariable.DATASET_TITLE, title);
        map.put(RdfTemplateVariable.DATASET_DESCRIPTION, description);
        map.put(RdfTemplateVariable.DATASET_DSD, dsdUri);
        Set<String> datasetUris = createDatasets(Arrays.asList(map), false);

        return datasetUris.isEmpty() ? StringUtils.EMPTY : datasetUris.iterator().next();
    }

    /**
     *
     * @param datasetMaps
     * @return
     * @throws ServiceException
     * @throws DAOException
     */
    public Set<String> createDatasets(List<Map<RdfTemplateVariable, String>> datasetMaps, boolean clear) throws ServiceException {

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
            for (Map<RdfTemplateVariable, String> datasetMap : datasetMaps) {

                VelocityContext context = new VelocityContext();
                for (Entry<RdfTemplateVariable, String> entry : datasetMap.entrySet()) {
                    RdfTemplateVariable variable = entry.getKey();
                    String value = entry.getValue();
                    context.put(variable.name(), value);
                }
                context.put(RdfTemplateVariable.DATASET_MODIFIED.name(), modifiedDateStr);

                Writer writer = null;
                Reader reader = null;
                try {
                    writer = new StringWriter();
                    template.merge(context, writer);
                    String str = writer.toString();
                    reader = new StringReader(str);

                    String datasetUri = DATASET_URI_PREFIX + datasetMap.get(RdfTemplateVariable.DATASET_IDENTIFIER);
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
            Workbook workbook = new XSSFWorkbook(inputStream);
            return importWorkbook(workbook, clear);
        } catch (IOException e) {
            throw new ServiceException("IO error when reading from file: " + file, e);
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

        Map<Integer, SpreadsheetColumn> columnsMap = new HashMap<>();

        Sheet firstSheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = firstSheet.iterator();
        if (rowIterator.hasNext()) {
            Row firstRow = rowIterator.next();
            loadColumns(columnsMap, firstRow);
        }

        if (columnsMap.isEmpty()) {
            throw new ServiceException("Could not detect supported columns in first row of first sheet!");
        }

        List<Map<RdfTemplateVariable, String>> rowMaps = new ArrayList<>();
        while (rowIterator.hasNext()) {

            Row row = rowIterator.next();
            Map<RdfTemplateVariable, String> rowMap = getRowMap(columnsMap, row);
            if (!rowMap.isEmpty()) {
                rowMaps.add(rowMap);
            }
        }

        if (rowMaps.size() > 0) {
            Set<String> datasetUris = createDatasets(rowMaps, clear);
            return datasetUris.size();
        } else {
            return 0;
        }
    }

    /**
     *
     * @param columnsMap
     * @param row
     * @return
     */
    private Map<RdfTemplateVariable, String> getRowMap(Map<Integer, SpreadsheetColumn> columnsMap, Row row) {
        Map<RdfTemplateVariable, String> rowMap = new HashMap<>();

        Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {

            Cell cell = cellIterator.next();
            int columnIndex = cell.getColumnIndex();
            SpreadsheetColumn column = columnsMap.get(columnIndex);
            if (column != null) {
                String strValue = StringUtils.trimToEmpty(cell.getStringCellValue());
                rowMap.put(column.getRdfTemplateVariable(), strValue);
            }
        }
        return rowMap;
    }

    /**
     *
     * @param columnsMap
     * @param row
     */
    private void loadColumns(Map<Integer, SpreadsheetColumn> columnsMap, Row row) {
        Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {

            Cell cell = cellIterator.next();
            if (Cell.CELL_TYPE_STRING == cell.getCellType()) {
                String cellValue = cell.getStringCellValue();
                if (StringUtils.isNotBlank(cellValue)) {
                    String columnName = cellValue.trim().replace(' ', '_').replace('-', '_').toUpperCase();
                    try {
                        SpreadsheetColumn column = SpreadsheetColumn.valueOf(columnName);
                        if (column != null) {
                            columnsMap.put(cell.getColumnIndex(), column);
                        }
                    } catch (Exception e) {
                        // Ignore.
                    }
                }
            }
        }
    }

    /**
     * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
     */
    public static enum SpreadsheetColumn {

        IDENTIFIER(RdfTemplateVariable.DATASET_IDENTIFIER), TITLE(RdfTemplateVariable.DATASET_TITLE),
        DESCRIPTION(RdfTemplateVariable.DATASET_DESCRIPTION), KEYWORD(RdfTemplateVariable.DATASET_KEYWORD),
        DATE_ISSUED(RdfTemplateVariable.DATASET_ISSUED), DSD_URI(RdfTemplateVariable.DATASET_DSD),
        LICENSE_URI(RdfTemplateVariable.DATASET_LICENSE), STATUS_URI(RdfTemplateVariable.DATASET_STATUS),
        PERIODICITY_URI(RdfTemplateVariable.DATASET_PERIODICITY);

        RdfTemplateVariable rdfTemplateVariable;

        /**
         * @param rdfTemplateVariable
         */
        SpreadsheetColumn(RdfTemplateVariable rdfTemplateVariable) {
            this.rdfTemplateVariable = rdfTemplateVariable;
        }

        /**
         * @return the rdfTemplateVariable
         */
        public RdfTemplateVariable getRdfTemplateVariable() {
            return rdfTemplateVariable;
        }
    }

    /**
     * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
     */
    public static enum RdfTemplateVariable {
        DATASET_IDENTIFIER, DATASET_TITLE, DATASET_DESCRIPTION, DATASET_DSD, DATASET_MODIFIED, DATASET_KEYWORD, DATASET_ISSUED,
        DATASET_LICENSE, DATASET_STATUS, DATASET_PERIODICITY;
    }
}
