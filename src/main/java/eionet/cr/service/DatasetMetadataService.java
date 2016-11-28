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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
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
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;

import eionet.cr.dao.DAOException;
import eionet.cr.util.Util;
import eionet.cr.util.sesame.SesameUtil;

/**
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMetadataService {

    /** */
    private static final String DATASET_METADATA_TTL_TEMPLATE_FILE = "velocity/new-dataset-metadata.vm";

    /** */
    private static final String DATASET_URI_PREFIX = "http://semantic.digital-agenda-data.eu/dataset/";

    /**
     *
     * @return
     */
    public static DatasetMetadataService newInstance() {
        return new DatasetMetadataService();
    }

    /**
     *
     * @param file
     * @return
     * @throws ServiceException
     */
    public int importFile(File file) throws ServiceException {

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            Workbook workbook = new XSSFWorkbook(inputStream);
            return importWorkbook(workbook);
        } catch (IOException e) {
            throw new ServiceException("IO error when reading from file: " + file, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     *
     * @param workbook
     * @throws ServiceException
     */
    private int importWorkbook(Workbook workbook) throws ServiceException {

        Map<Integer, SpreadsheetImportColumn> columnsMap = new HashMap<>();

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
            createDatasets(rowMaps);
        }

        return rowMaps.size();
    }

    /**
     *
     * @param columnsMap
     * @param row
     * @return
     */
    private Map<RdfTemplateVariable, String> getRowMap(Map<Integer, SpreadsheetImportColumn> columnsMap, Row row) {
        Map<RdfTemplateVariable, String> rowMap = new HashMap<>();

        Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {

            Cell cell = cellIterator.next();
            int columnIndex = cell.getColumnIndex();
            SpreadsheetImportColumn column = columnsMap.get(columnIndex);
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
    private void loadColumns(Map<Integer, SpreadsheetImportColumn> columnsMap, Row row) {
        Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {

            Cell cell = cellIterator.next();
            if (Cell.CELL_TYPE_STRING == cell.getCellType()) {
                String cellValue = cell.getStringCellValue();
                if (StringUtils.isNotBlank(cellValue)) {
                    String columnName = cellValue.trim().replace(' ', '_').replace('-', '_').toUpperCase();
                    try {
                        SpreadsheetImportColumn column = SpreadsheetImportColumn.valueOf(columnName);
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
        createDatasets(Arrays.asList(map));

        return DATASET_URI_PREFIX + identifier;
    }

    /**
     *
     * @param datasetMaps
     * @return
     * @throws ServiceException
     * @throws DAOException
     */
    public int createDatasets(List<Map<RdfTemplateVariable, String>> datasetMaps) throws ServiceException {

        if (CollectionUtils.isEmpty(datasetMaps)) {
            return 0;
        }

        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
        Template template = ve.getTemplate(DATASET_METADATA_TTL_TEMPLATE_FILE);

        String modifiedDateStr = Util.virtuosoDateToString(new Date());

        int addedCount = 0;
        RepositoryConnection repoConn = null;
        try {

            repoConn = SesameUtil.getRepositoryConnection();
            ValueFactory vf = repoConn.getValueFactory();

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

                    String datasetUri = DATASET_URI_PREFIX + datasetMap.get(SpreadsheetImportColumn.IDENTIFIER);
                    repoConn.add(reader, datasetUri, RDFFormat.TURTLE, vf.createURI(datasetUri));
                    addedCount++;
                } finally {
                    IOUtils.closeQuietly(reader);
                    IOUtils.closeQuietly(writer);
                }
            }

            return addedCount;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        } finally {
            SesameUtil.close(repoConn);
        }
    }

    /**
     * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
     */
    public static enum SpreadsheetImportColumn {

        IDENTIFIER(RdfTemplateVariable.DATASET_IDENTIFIER),
        TITLE(RdfTemplateVariable.DATASET_TITLE),
        DESCRIPTION(RdfTemplateVariable.DATASET_DESCRIPTION),
        DSD_URI(RdfTemplateVariable.DATASET_DSD);

        RdfTemplateVariable rdfTemplateVariable;

        /**
         * @param rdfTemplateVariable
         */
        SpreadsheetImportColumn(RdfTemplateVariable rdfTemplateVariable) {
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
        DATASET_IDENTIFIER, DATASET_TITLE, DATASET_DESCRIPTION, DATASET_DSD, DATASET_MODIFIED;
    }
}
