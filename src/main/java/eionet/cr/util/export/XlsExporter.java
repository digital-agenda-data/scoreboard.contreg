/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Content Registry 2.0.
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency.  Portions created by Tieto Eesti are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 * Enriko Käsper, Tieto Estonia
 */
package eionet.cr.util.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;

import eionet.cr.common.Predicates;
import eionet.cr.config.GeneralConfig;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.readers.SubjectExportReader;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.util.FormatUtils;
import eionet.cr.util.Pair;
import eionet.cr.util.pagination.PagingRequest;

/**
 * @author Enriko Käsper
 */

public class XlsExporter extends Exporter implements SubjectExportEvent {

    // config param in cr.properties
    private static final String EXPORT_ROW_LIMIT = "exporter.xls.row.limit";
    private HSSFSheet sheet;
    int rowNumber = 1;
    int[] columnWidth;

    /**
     * exports custom search to XLS format.
     *
     * @return
     * @throws IOException
     * @throws DAOException
     */
    protected InputStream doExport() throws IOException, DAOException {
        HSSFWorkbook workbook = new HSSFWorkbook();
        sheet = workbook.createSheet("exported data");

        // some pretty print with headers
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerStyle.setFont(headerFont);

        // output headers
        HSSFRow headers = sheet.createRow(0);
        // store width of each column +1 for Uri or Label column
        columnWidth = new int[getSelectedColumns().size() + 1];
        // output Uri or Label column
        String uriOrLabelColumn = getUriOrLabel();

        columnWidth[0] = uriOrLabelColumn.length();
        HSSFCell uriOrLabelCell = headers.createCell(0);
        XlsUtil.setCellValue(uriOrLabelCell, uriOrLabelColumn);
        uriOrLabelCell.setCellStyle(headerStyle);

        // output rest of the headers
        int columnNumber = 1;
        for (Pair<String, String> columnPair : getSelectedColumns()) {
            // label is already added to the list of elements
            if (Predicates.RDFS_LABEL.equals(columnPair.getLeft()))
                continue;

            String column = columnPair.getRight() != null ? columnPair.getRight() : columnPair.getLeft();
            columnWidth[columnNumber] = column.length();
            HSSFCell cell = headers.createCell(columnNumber++);
            XlsUtil.setCellValue(cell, column);
            cell.setCellStyle(headerStyle);
        }
        sheet.createFreezePane(0, 1);

        // output serarch results
        SubjectExportReader reader = new SubjectExportReader(this);
        doExportQueryAndWriteDataIntoOutput(reader);

        // set column width
        for (int i = 0; i < getSelectedColumns().size() + 1; i++) {
            sheet.setColumnWidth(i, Math.min(256 * columnWidth[i], 256 * 255));
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        workbook.write(output);
        return new ByteArrayInputStream(output.toByteArray());
    }

    /**
     * call-back method.
     */
    public void writeSubjectIntoExporterOutput(SubjectDTO subject) throws ExportException {
        HSSFRow row = sheet.createRow(rowNumber++);

        // output uri or label column value
        String value = getUriOrLabelValue(subject);

        columnWidth[0] = Math.max(columnWidth[0], value.length());
        XlsUtil.setCellValue(row.createCell(0), value);

        // output other columns
        int columnNumber = 1;
        for (Pair<String, String> columnPair : getSelectedColumns()) {
            // label is already written
            if (Predicates.RDFS_LABEL.equals(columnPair.getLeft()))
                continue;

            value = FormatUtils.getObjectValuesForPredicate(columnPair.getLeft(), subject, getLanguages());
            columnWidth[columnNumber] = Math.max(columnWidth[columnNumber], value.length());
            XlsUtil.setCellValue(row.createCell(columnNumber++), value);
        }
    }

    @Override
    protected PagingRequest getRowLimitPagingRequest() {

        if (getRowsLimit() > 0) {
            return PagingRequest.create(1, getRowsLimit());
        } else {
            return null;
        }
    }

    public static Integer getRowsLimit() {
        return new Integer(GeneralConfig.getRequiredProperty(EXPORT_ROW_LIMIT));
    }
}
