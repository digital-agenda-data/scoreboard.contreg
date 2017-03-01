package eionet.cr.dao.readers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;

import eionet.cr.dao.DAOException;
import eionet.cr.service.CubeDatasetMetadataService;
import eionet.cr.util.sesame.SPARQLResultSetBaseReader;

/**
 * Exports datasets metadata SPARQL results into a given targetFile file which has to be based on an agreed Excel template.
 *
 * @author Jaanus Heinlaid
 *
 */
@SuppressWarnings("rawtypes")
public class DatasetMetadataExportReader extends SPARQLResultSetBaseReader {

    /** */
    private static final Logger LOGGER = Logger.getLogger(DatasetMetadataExportReader.class);

    /** */
    private static final String DATA_SHEET_NAME = "METADATA";

    /** */
    private static final String DUMMY_CATALOG_IDENTIFIER_PROPERTY = "dummy:catalogIdentifier";

    /** Maps RDF properties of datasets to corresponding columns (by column index) of targetFile spreadsheet. */
    private Map<String, Integer> propertiesToColumns;

    /** The targetFile spreadsheet file where the exported workbook will be saved to. */
    private File targetFile;

    /** Number of datasets exported. */
    private int exportedCount;

    /** The total number of rows added by this exporter at any moment of execution. */
    private int worksheetRowsAdded;

    /**
     * Currently processed dataset's map. Key = Excel column index, value = list of strings to write into column.
     * Column may have multiple values. For every value the row will be duplicated by repeating other columns' values.
     * The map is lazily initialized.
     */
    private HashMap<Integer, List<String>> currentDatasetMap;

    /** The current dataset URI as the SPARQL result set is traversed. Lazily initialized. */
    private String currentDatasetUri;

    /** The workbook object representing the given template file. */
    private Workbook workbook;

    /** The sheet where the datasets metadata should be written into. */
    private Sheet dataSheet;

    /** */
    private Map<String, Set<String>> datasetCatalogs;

    /**
     *
     * @param workbook
     * @param datasetCatalogs
     * @param targetFile
     * @throws ResultSetReaderException
     */
    public DatasetMetadataExportReader(Workbook workbook, Map<String, Integer> propertiesToColumns,
            Map<String, Set<String>> datasetCatalogs, File targetFile) throws ResultSetReaderException {

        this.workbook = workbook;
        this.targetFile = targetFile;
        this.propertiesToColumns = propertiesToColumns;
        this.datasetCatalogs = datasetCatalogs;

        dataSheet = workbook.getSheet(DATA_SHEET_NAME);
        if (dataSheet == null) {
            dataSheet = workbook.getSheetAt(0);
            if (dataSheet == null) {
                throw new ResultSetReaderException(
                        String.format("Could not find data sheet neither by name (%s), nor by index (%d)!", DATA_SHEET_NAME, 0));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readRow(BindingSet bindingSet) throws ResultSetReaderException {

        if (bindingSet == null || bindingSet.size() == 0) {
            return;
        }

        String subjectUri = getStringValue(bindingSet, "s");
        if (subjectUri == null) {
            return;
        }

        if (!subjectUri.equals(currentDatasetUri)) {

            // Adds dataset catalogs to the current dataset map before the map gets actually saved into workbook.
            Set<String> catalogs = datasetCatalogs.get(currentDatasetUri);
            if (catalogs != null && !catalogs.isEmpty()) {
                Integer catalogIdentifierColumnIndex = propertiesToColumns.get(DUMMY_CATALOG_IDENTIFIER_PROPERTY);
                if (catalogIdentifierColumnIndex != null && catalogIdentifierColumnIndex >= 0) {
                    if (currentDatasetMap == null) {
                        currentDatasetMap = new HashMap<Integer, List<String>>();
                    }
                    currentDatasetMap.put(catalogIdentifierColumnIndex, new ArrayList<>(catalogs));
                }
            }

            if (!MapUtils.isEmpty(currentDatasetMap)) {
                saveCurrentDataset();
                exportedCount++;
            }
            currentDatasetUri = subjectUri;
            currentDatasetMap = new HashMap<Integer, List<String>>();
        }

        String predicateUri = getStringValue(bindingSet, "p");
        Integer columnIndex = propertiesToColumns.get(predicateUri);

        if (columnIndex != null && columnIndex.intValue() >= 0) {
            Value value = bindingSet.getValue("o");
            if (value != null) {
                addToCurrentDatasetMap(columnIndex, value);
            }
        }
    }

    /**
     * Puts the given column-index-to-value pair into the {@link #currentDatasetMap}. The latter is initialized if null.
     *
     * @param columnIndex
     * @param value
     */
    private void addToCurrentDatasetMap(Integer columnIndex, Value value) {

        if (value instanceof Literal) {
            addToMap(currentDatasetMap, columnIndex, value.stringValue());
        } else if (!(value instanceof BNode)) {

            String strValue = value.stringValue();
            if (strValue.startsWith(CubeDatasetMetadataService.DATASET_URI_PREFIX)) {

                strValue = StringUtils.substringAfterLast(strValue.replace('/', '#'), "#");
                addToMap(currentDatasetMap, columnIndex, strValue);
            } else {
                addToMap(currentDatasetMap, columnIndex, value.stringValue());
            }
        }
    }

    /**
     * Helper method: adds given value to the list behind the given key in the given map.
     *
     * @param map The given map.
     * @param key Key in the map.
     * @param value Value to add to the list behind the given key.
     */
    private static void addToMap(HashMap<Integer, List<String>> map, Integer key, String value) {

        List<String> list = map.get(key);
        if (list == null) {
            list = new ArrayList<String>();
            map.put(key, list);
        }

        if (list.isEmpty() || !list.contains(value)) {
            list.add(value);
        }
    }

    /**
     * Saves the contents of the current dataset's map into the target metadata sheet.
     */
    private void saveCurrentDataset() {

        if (MapUtils.isEmpty(currentDatasetMap)) {
            return;
        }

        LOGGER.trace("Saving dataset #" + exportedCount);

        // Convert the map to the set of distinct rows, following above-described principle.
        HashSet<ArrayList<String>> datasetRows = createDatasetRows(currentDatasetMap);

        // Loop over dataset rows, save each row.
        for (Iterator<ArrayList<String>> iter = datasetRows.iterator(); iter.hasNext();) {
            ArrayList<String> row = iter.next();
            if (row != null && !row.isEmpty()) {
                saveDatasetRow(row);
                worksheetRowsAdded++;
            }
        }
    }

    /**
     *
     * @param datasetColumnsMap
     * @return
     */
    private HashSet<ArrayList<String>> createDatasetRows(HashMap<Integer, List<String>> datasetColumnsMap) {

        int maxColumnIndex = Collections.max(datasetColumnsMap.keySet()).intValue();

        // Determine the maximum number of multiple values in any column.
        int maxNumberOfMultipleValues = 0;
        for (Entry<Integer, List<String>> entry : datasetColumnsMap.entrySet()) {
            List<String> columnValues = entry.getValue();
            maxNumberOfMultipleValues = Math.max(maxNumberOfMultipleValues, columnValues.size());
        }

        HashSet<ArrayList<String>> rowSet = new LinkedHashSet<ArrayList<String>>();
        for (int rowIndex = 0; rowIndex < maxNumberOfMultipleValues; rowIndex++) {

            ArrayList<String> row = new ArrayList<String>();
            for (int colIndex = 0; colIndex <= maxColumnIndex; colIndex++) {
                row.add(StringUtils.EMPTY);
            }

            for (Entry<Integer, List<String>> entry : datasetColumnsMap.entrySet()) {

                int columnIndex = entry.getKey();
                List<String> columnValues = entry.getValue();
                if (!columnValues.isEmpty()) {
                    int rowToGet = Math.min(rowIndex, columnValues.size() - 1);
                    row.set(columnIndex, columnValues.get(rowToGet));
                }
            }

            rowSet.add(row);
        }

        return rowSet;
    }

    /**
     * Saves dataset row into the target metadata sheet.
     *
     * @param datasetRow The row to save.
     */
    private void saveDatasetRow(ArrayList<String> datasetRow) {

        if (datasetRow == null || datasetRow.isEmpty()) {
            return;
        }

        int rowIndex = worksheetRowsAdded + 1;
        Row row = dataSheet.getRow(rowIndex);
        if (row == null) {
            row = dataSheet.createRow(rowIndex);
        }

        LOGGER.trace("Populating metadata row at position " + worksheetRowsAdded);

        int maxLines = 1;
        for (int i = 0; i < datasetRow.size(); i++) {

            int cellIndex = i;
            String cellValue = datasetRow.get(i);

            Cell cell = row.getCell(cellIndex);
            if (cell == null) {
                cell = row.createCell(cellIndex);
            }
            cell.setCellValue(cellValue);
            CellStyle cellStyle = cell.getCellStyle();
            if (cellStyle == null) {
                cellStyle = workbook.createCellStyle();
                cell.setCellStyle(cellStyle);
            }
            cellStyle.setWrapText(true);

            String[] split = StringUtils.split(cellValue.replace("\r\n", "\n"), "\n");
            maxLines = Math.max(maxLines, split.length);
        }

        if (maxLines > 1) {
            maxLines = Math.min(maxLines, 30);
            float defaultRowHeightInPoints = dataSheet.getDefaultRowHeightInPoints();
            row.setHeightInPoints(maxLines * defaultRowHeightInPoints);
        }
    }

    /**
     * To be called after last dataset has been exported. The purpose of the method is to properly save the spreadsheet
     * template file and close all resources.
     *
     * @throws DAOException
     */
    public void saveAndClose() throws ResultSetReaderException {

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(targetFile);
            dataSheet.showInPane((short) 1, (short) 0);
            workbook.write(fos);
        } catch (IOException e) {
            throw new ResultSetReaderException("Error when saving workbook to " + targetFile, e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.util.sesame.SPARQLResultSetBaseReader#endResultSet()
     */
    @Override
    public void endResultSet() {

        if (!MapUtils.isEmpty(currentDatasetMap)) {
            saveCurrentDataset();
            exportedCount++;
        }
    }

    /**
     *
     * @return
     */
    public int getExportedCount() {
        return exportedCount;
    }
}