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

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;

import eionet.cr.dao.DAOException;
import eionet.cr.util.sesame.SPARQLResultSetBaseReader;

/**
 * An extension of {@link SPARQLResultSetBaseReader} that exports Scoreboard codelist items into a given spreadsheet template file.
 * The mapping of codelist item properties to corresponding spreadsheet columns is also a pre-requisite.
 *
 * @author Jaanus Heinlaid
 *
 */
@SuppressWarnings("rawtypes")
public class CodelistExporter extends SPARQLResultSetBaseReader {

    /** */
    private static final Logger LOGGER = Logger.getLogger(CodelistExporter.class);

    /** */
    private static final String CODELIST_URI_PREFIX = "http://semantic.digital-agenda-data.eu/codelist/";

    /** */
    private static final String DAD_PROPERTY_NAMESPACE = "http://semantic.digital-agenda-data.eu/def/property/";

    /** */
    private static final Map<String, String> SPECIAL_BINDINGS_MAP = createSpecialBindingsMap();

    /** The properties to spreadsheet columns mapping */
    private Map<String, Integer> mappings;

    /** The target spreadsheet file where the exported workbook will be saved to. */
    private File target;

    /** Number of codelist items exported. */
    private int itemsExported;

    /** The total number of worksheet rows added by this exporter at any moment of execution. */
    private int worksheetRowsAdded;

    /**
     * This map represents current codelist item to be written into spreadsheet.
     * Keys are spreadsheet columns (0-based index), values are lists of strings to be written into those columns.
     * So the idea is that a column may have multiple values. For every such value the worksheet row will simply be duplicated
     * by repeating the values of the other columns.
     * This fields is to be initialized at first need.
     */
    private HashMap<Integer, List<String>> currentCodelistItemMap;

    /** The current subject URI as the SPARQL result set is traversed. To be initialized at first need. */
    private String currentSubjectUri;

    /** The workbook object that represents the given spreadsheet template file. */
    private Workbook workbook;

    /** The current worksheet in the workbook. */
    private Sheet worksheet;

    /**
     * Construct new instance with the given spreadsheet template reference and the properties to spreadsheet columns mapping.
     *
     * @param template The spreadsheet template reference.
     * @param mappings The properties to spreadsheet columns mapping.
     * @param target The target spreadsheet file where the exported workbook will be saved to.
     * @throws DAOException
     */
    public CodelistExporter(File template, Map<String, Integer> mappings, File target) throws DAOException {

        if (template == null || !template.exists() || !template.isFile()) {
            throw new IllegalArgumentException("The given spreadsheet template must not be null and the file must exist!");
        }

        if (mappings == null || mappings.isEmpty()) {
            throw new IllegalArgumentException("The given properties to spreadsheet columns mapping must not be null or empty!");
        }

        if (target == null || !target.getParentFile().exists()) {
            throw new IllegalArgumentException("The given spreadsheet target file must not be null and its path must exist!");
        }

        try {
            workbook = WorkbookFactory.create(template);
        } catch (InvalidFormatException e) {
            throw new DAOException("Failed to recognize workbook at " + template, e);
        } catch (IOException e) {
            throw new DAOException("IOException when trying to create workbook object from " + template, e);
        }

        worksheet = workbook.getSheetAt(0);
        if (worksheet == null) {
            worksheet = workbook.createSheet();
        }
        if (worksheet == null) {
            throw new DAOException("Failed to get or create the workbook's first sheet: simply got null as the result");
        }

        this.mappings = mappings;
        this.target = target;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.util.sesame.SPARQLResultSetReader#readRow(org.openrdf.query.BindingSet)
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

        if (!subjectUri.equals(currentSubjectUri)) {
            if (!MapUtils.isEmpty(currentCodelistItemMap)) {
                saveCurrentCodelistItem();
                itemsExported++;
            }
            currentSubjectUri = subjectUri;
            currentCodelistItemMap = new HashMap<Integer, List<String>>();
        }

        String predicateUri = getStringValue(bindingSet, "p");
        Integer columnIndex = mappings.get(predicateUri);

        if (columnIndex != null && columnIndex.intValue() >= 0) {
            Value value = bindingSet.getValue("o");
            if (value != null) {
                addToCurrentCodelistItemMap(columnIndex, value);
            }
        } else {
            for (Entry<String, String> entry : SPECIAL_BINDINGS_MAP.entrySet()) {

                String bindingName = entry.getKey();
                String bindingPredicate = entry.getValue();

                Value value = bindingSet.getValue(bindingName);
                if (value != null) {
                    columnIndex = mappings.get(bindingPredicate);
                    if (columnIndex != null && columnIndex.intValue() >= 0) {
                        addToCurrentCodelistItemMap(columnIndex, value);
                    }
                }
            }
        }
    }

    /**
     * Puts the given column-index-to-value pair into the {@link #currentCodelistItemMap}. The latter is initialized if null.
     *
     * @param columnIndex
     * @param value
     */
    private void addToCurrentCodelistItemMap(Integer columnIndex, Value value) {

        if (value instanceof Literal) {
            addToMap(currentCodelistItemMap, columnIndex, value.stringValue());
        } else if (!(value instanceof BNode)) {

            String strValue = value.stringValue();
            if (strValue.startsWith(CODELIST_URI_PREFIX)) {

                strValue = StringUtils.substringAfterLast(strValue.replace('/', '#'), "#");
                addToMap(currentCodelistItemMap, columnIndex, strValue);
            } else {
                addToMap(currentCodelistItemMap, columnIndex, value.stringValue());
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
     * Saves the contents of the current codelist item's map into the target worksheet.
     */
    private void saveCurrentCodelistItem() {

        if (MapUtils.isEmpty(currentCodelistItemMap)) {
            return;
        }

        LOGGER.trace("Saving item #" + itemsExported);

        // The codelist item map represents the current codelist item to be written into the target rowsheet.
        // Each key-value pair in the row-map represents worksheet column index and corresponding values.
        // Yes, the column can have multiple values. We handle this by repeating worksheet row for every such value.
        // Imagine map like this: {1=["james"], 2="bond" 3=["tall", "handsome"]}
        // In the worksheet the outcome must be 2 "distinct rows", like this (columns ordered by column index starting from 1):
        // "james", "bond", "tall"
        // "james", "bond", "handsome".

        // Convert the map to the set of distinct rows, following above-described principle.
        HashSet<ArrayList<String>> codelistItemRows = createCodelistItemRows(currentCodelistItemMap);

        // Loop over codelist item rows, save each row into worksheet.
        for (Iterator<ArrayList<String>> iter = codelistItemRows.iterator(); iter.hasNext();) {
            ArrayList<String> codelistItemRow = iter.next();
            if (codelistItemRow != null && !codelistItemRow.isEmpty()) {
                saveCodelistItemRow(codelistItemRow);
                worksheetRowsAdded++;
            }
        }
    }

    /**
     * Saves codelist item row into the target worksheet.
     *
     * @param codelistItemRow The row to save.
     */
    private void saveCodelistItemRow(ArrayList<String> codelistItemRow) {

        if (codelistItemRow == null || codelistItemRow.isEmpty()) {
            return;
        }

        int rowIndex = worksheetRowsAdded + 1;
        Row row = worksheet.getRow(rowIndex);
        if (row == null) {
            row = worksheet.createRow(rowIndex);
        }

        LOGGER.trace("Populating worksheet row at position " + worksheetRowsAdded);

        int maxLines = 1;
        for (int i = 0; i < codelistItemRow.size(); i++) {

            int cellIndex = i;
            String cellValue = codelistItemRow.get(i);

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
            float defaultRowHeightInPoints = worksheet.getDefaultRowHeightInPoints();
            row.setHeightInPoints(maxLines * defaultRowHeightInPoints);
        }
    }

    /**
     * To be called after last codelist item has been exported. The purpose of the method is to properly save the spreadsheet
     * template file and close all resources.
     *
     * @throws DAOException
     */
    public void saveAndClose() throws DAOException {

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(target);
            worksheet.showInPane((short) 1, (short) 0);
            workbook.write(fos);
        } catch (IOException e) {
            throw new DAOException("Error when saving workbook to " + target, e);
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

        if (!MapUtils.isEmpty(currentCodelistItemMap)) {
            saveCurrentCodelistItem();
            itemsExported++;
        }
    }

    /**
     * Returns the number of codelist items exported by this exporter at the time this method is called.
     *
     * @return As described.
     */
    public int getItemsExported() {
        return itemsExported;
    }

    /**
     *
     * @return
     */
    private static Map<String, String> createSpecialBindingsMap() {

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("memberOf", DAD_PROPERTY_NAMESPACE + "member-of");
        map.put("order", DAD_PROPERTY_NAMESPACE + "order");
        return map;
    }

    /**
     *
     * @param codelistItemMap
     * @return
     */
    private HashSet<ArrayList<String>> createCodelistItemRows(HashMap<Integer, List<String>> codelistItemMap) {

        int maxColumnIndex = Collections.max(codelistItemMap.keySet()).intValue();

        // Determine the maximum number of multiple values in any column.
        int maxNumberOfMultipleValues = 0;
        for (Entry<Integer, List<String>> entry : codelistItemMap.entrySet()) {
            List<String> columnValues = entry.getValue();
            maxNumberOfMultipleValues = Math.max(maxNumberOfMultipleValues, columnValues.size());
        }

        HashSet<ArrayList<String>> rowSet = new LinkedHashSet<ArrayList<String>>();
        for (int rowIndex = 0; rowIndex < maxNumberOfMultipleValues; rowIndex++) {

            ArrayList<String> row = new ArrayList<String>();
            for (int colIndex = 0; colIndex <= maxColumnIndex; colIndex++) {
                row.add(StringUtils.EMPTY);
            }

            for (Entry<Integer, List<String>> entry : codelistItemMap.entrySet()) {

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
}
