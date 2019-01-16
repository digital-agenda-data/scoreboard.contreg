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
 * The Original Code is Content Registry 3
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency. Portions created by TripleDev or Zero Technologies are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *        jaanus
 */

package eionet.cr.staging.util;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Index;
import com.healthmarketscience.jackcess.IndexData.ColumnDescriptor;
import com.healthmarketscience.jackcess.Table;

// TODO: Auto-generated Javadoc
/**
 *
 * @author jaanus
 *
 */
public final class VirtuosoUtil {

    /** */
    public static final Map<DataType, String> JACKCESS_TO_VIRTUOSO_DATATYPES = new HashMap<DataType, String>();

    /** */
    static {
        JACKCESS_TO_VIRTUOSO_DATATYPES.put(DataType.BYTE, "SMALLINT");
        JACKCESS_TO_VIRTUOSO_DATATYPES.put(DataType.INT, "SMALLINT");
        JACKCESS_TO_VIRTUOSO_DATATYPES.put(DataType.LONG, "INTEGER");
        JACKCESS_TO_VIRTUOSO_DATATYPES.put(DataType.FLOAT, "FLOAT");
        JACKCESS_TO_VIRTUOSO_DATATYPES.put(DataType.DOUBLE, "DOUBLE PRECISION");
        JACKCESS_TO_VIRTUOSO_DATATYPES.put(DataType.BOOLEAN, "SMALLINT");
        JACKCESS_TO_VIRTUOSO_DATATYPES.put(DataType.TEXT, "VARCHAR");
        JACKCESS_TO_VIRTUOSO_DATATYPES.put(DataType.MEMO, "LONG VARCHAR");
        JACKCESS_TO_VIRTUOSO_DATATYPES.put(DataType.BINARY, "LONG VARBINARY");
        JACKCESS_TO_VIRTUOSO_DATATYPES.put(DataType.MONEY, "DECIMAL");
        JACKCESS_TO_VIRTUOSO_DATATYPES.put(DataType.NUMERIC, "DECIMAL");
        JACKCESS_TO_VIRTUOSO_DATATYPES.put(DataType.SHORT_DATE_TIME, "DATETIME");
    }

    /**
     * Private constructor: do not allow instantiation of utility classes.
     */
    private VirtuosoUtil() {
        // Do nothing here.
    }

    /**
     * Creates the table statement.
     *
     * @param table the table
     * @param dbName the db name
     * @param dbUser the db user
     * @return the string
     */
    public static String createTableStatement(Table table, String dbName, String dbUser) {

        String qualifiedSanitizedTableName = fullyQualifiedSanitizedTableName(table.getName(), dbName, dbUser);

        StringBuilder sb = new StringBuilder("CREATE TABLE ");
        sb.append(qualifiedSanitizedTableName).append(" (\n");

        LinkedHashSet<String> primKeyColumns = new LinkedHashSet<String>();
        try {
            Index pkIndex = table.getPrimaryKeyIndex();
            if (pkIndex != null) {
                List<ColumnDescriptor> columns = pkIndex.getColumns();
                for (ColumnDescriptor colDescriptor : columns) {
                    primKeyColumns.add(colDescriptor.getColumn().getName());
                }
            }
        } catch (IllegalArgumentException e) {
            // This exception means no primary key index set on this table.
        }

        int i = 0;
        List<Column> columns = table.getColumns();
        for (Column column : columns) {

            sb.append("    ").append(sanitizeColumnName(column.getName())).append(" ");
            sb.append(jackcessToVirtuosoDataType(column));
            if (++i < columns.size() || !primKeyColumns.isEmpty()) {
                sb.append(",");
            }
            sb.append("\n");
        }

        if (!primKeyColumns.isEmpty()) {
            sb.append("    PRIMARY KEY (");
            i = 0;
            for (String pkCol : primKeyColumns) {
                if (i++ > 0) {
                    sb.append(", ");
                }
                sb.append(sanitizeColumnName(pkCol));
            }
            sb.append(")\n");
        }

        sb.append(")");

        return sb.toString();
    }

    /**
     * Parameterized insert statement.
     *
     * @param tableName the table name
     * @param columnNames the column names
     * @param dbName the db name
     * @param dbUser the db user
     * @return the string
     */
    public static String parameterizedInsertStatement(String tableName, List<String> columnNames, String dbName, String dbUser) {

        StringBuilder columnsStr = new StringBuilder();
        StringBuilder valuesStr = new StringBuilder();

        int i = 0;
        for (String column : columnNames) {

            if (i++ > 0) {
                columnsStr.append(", ");
                valuesStr.append(", ");
            }
            columnsStr.append(sanitizeColumnName(column));
            valuesStr.append("?");
        }

        return "INSERT INTO \"" + fullyQualifiedSanitizedTableName(tableName, dbName, dbUser) + "\" (" + columnsStr + ") values ("
                + valuesStr + ")";
    }

    /**
     * Sanitize column name.
     *
     * @param name the name
     * @return the string
     */
    private static String sanitizeColumnName(String name) {
        return new StringBuilder("\"").append(name.trim().replaceAll("\\s+", "_")).append("\"").toString();
        // return new StringBuilder(name.trim().replaceAll("\\s+", "_")).toString();
    }

    /**
     * Fully qualified sanitized table name.
     *
     * @param tableName the table name
     * @param dbName the db name
     * @param dbUser the db user
     * @return the string
     */
    private static String fullyQualifiedSanitizedTableName(String tableName, String dbName, String dbUser) {

        String sanitizedTableName = tableName.trim().replaceAll("\\s+", "_").replace('-', '_');
        return new StringBuilder(dbName).append(".").append(dbUser).append(".").append(sanitizedTableName).toString();
    }

    /**
     * Jackcess to virtuoso data type.
     *
     * @param column the column
     * @return the string
     */
    public static String jackcessToVirtuosoDataType(Column column) {

        String result = JACKCESS_TO_VIRTUOSO_DATATYPES.get(column.getType());
        if (result != null) {
            if (result.equalsIgnoreCase("VARCHAR")) {
                if (column.getLengthInUnits() > 0) {
                    result += "(" + column.getLengthInUnits() + ")";
                }
            }
        }

        return result == null ? "VARCHAR(255)" : result;
    }
}
