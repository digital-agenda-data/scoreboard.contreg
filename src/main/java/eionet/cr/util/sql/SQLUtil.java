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
 * Jaanus Heinlaid, Tieto Eesti
 */
package eionet.cr.util.sql;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import eionet.cr.common.CRException;
import eionet.cr.dao.readers.ResultSetReaderException;

/**
 *
 * @author heinljab
 *
 */
public final class SQLUtil {

    /** Static logger. */
    protected static final Logger LOGGER = Logger.getLogger(SQLUtil.class);

    /**
     * Hide utility class constructor.
     */
    private SQLUtil() {
        // Just an empty private constructor to avoid instantiating this utility class.
    }

    /**
     *
     * @param sql
     * @param conn
     * @return Object
     * @throws SQLException
     */
    public static Object executeSingleReturnValueQuery(String sql, Connection conn) throws SQLException {

        ResultSet rs = null;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            return (rs != null && rs.next()) ? rs.getObject(1) : null;
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(stmt);
        }
    }

    /**
     *
     * @param parameterizedSQL
     * @param values
     * @param conn
     * @return Object
     * @throws SQLException
     */
    public static Object executeSingleReturnValueQuery(String parameterizedSQL, List<?> values, Connection conn) throws SQLException {

        ResultSet rs = null;
        PreparedStatement pstmt = null;
        try {
            pstmt = prepareStatement(parameterizedSQL, values, conn);
            rs = pstmt.executeQuery();
            return (rs != null && rs.next()) ? rs.getObject(1) : null;
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(pstmt);
        }
    }

    /**
     *
     * @param parameterizedSQL
     * @param values
     * @param rsReader
     * @param conn
     * @throws SQLException
     * @throws ResultSetReaderException
     */
    @SuppressWarnings("rawtypes")
    public static void executeQuery(String parameterizedSQL, List<?> values, SQLResultSetReader rsReader, Connection conn) throws SQLException,
            ResultSetReaderException {

        ResultSet rs = null;
        PreparedStatement pstmt = null;
        try {
            pstmt = prepareStatement(parameterizedSQL, values, conn);
            rs = pstmt.executeQuery();
            if (rs != null) {
                ResultSetMetaData rsMd = rs.getMetaData();
                if (rsMd != null && rsMd.getColumnCount() > 0) {
                    rsReader.startResultSet(rsMd);
                    while (rs.next()) {
                        rsReader.readRow(rs);
                    }
                }
            }
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(pstmt);
        }

    }

    /**
     *
     * @param sql
     * @param conn
     * @return List<Map<String,SQLValue>>
     * @throws SQLException
     * @throws ResultSetReaderException
     */
    public static List<Map<String, SQLValue>> executeQuery(String sql, Connection conn) throws SQLException, ResultSetReaderException {

        SQLValueReader sqlValueReader = new SQLValueReader();
        executeQuery(sql, sqlValueReader, conn);
        return sqlValueReader.getResultList();
    }

    /**
     *
     * @param sql
     * @param rsReader
     * @param conn
     * @throws SQLException
     * @throws ResultSetReaderException
     */
    @SuppressWarnings("rawtypes")
    public static void executeQuery(String sql, SQLResultSetReader rsReader, Connection conn) throws SQLException, ResultSetReaderException {

        ResultSet rs = null;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs != null) {
                ResultSetMetaData rsMd = rs.getMetaData();
                if (rsMd != null && rsMd.getColumnCount() > 0) {
                    rsReader.startResultSet(rsMd);
                    while (rs.next()) {
                        rsReader.readRow(rs);
                    }
                }
            }
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(stmt);
        }
    }

    /**
     *
     * @param parameterizedSQL
     * @param values
     * @param conn
     * @return int
     * @throws SQLException
     */
    public static int executeUpdate(String parameterizedSQL, List<?> values, Connection conn) throws SQLException {

        PreparedStatement pstmt = null;
        try {
            pstmt = prepareStatement(parameterizedSQL, values, conn);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            return 0;
        } finally {
            close(pstmt);
        }
    }

    /**
     * @param parameterizedSQL
     * @param values
     * @param conn
     * @return int
     * @throws CRException
     */
    public static int executeUpdateReturnAutoID(String parameterizedSQL, List<?> values, Connection conn) throws CRException {

        PreparedStatement pstmt = null;
        try {
            pstmt = prepareStatement(parameterizedSQL, values, conn, true);
            pstmt.executeUpdate();
            SQLUtil.close(pstmt);
            pstmt = conn.prepareStatement("select identity_value()");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new CRException("No auto-generated keys returned!");
            }
        } catch (SQLException sqle) {
            throw new CRException(sqle.toString(), sqle);
        } finally {
            SQLUtil.close(pstmt);
        }
    }

    /**
     *
     * @param sql
     * @param conn
     * @return int
     * @throws SQLException
     */
    public static int executeUpdate(String sql, Connection conn) throws SQLException {

        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            return stmt.executeUpdate(sql);
        } finally {
            SQLUtil.close(stmt);
        }
    }

    /**
     *
     * @param parameterizedSQL
     * @param values
     * @param conn
     * @return PreparedStatement
     * @throws SQLException
     */
    public static PreparedStatement prepareStatement(String parameterizedSQL, List<?> values, Connection conn) throws SQLException {

        PreparedStatement pstmt = conn.prepareStatement(parameterizedSQL);
        populateStatement(pstmt, values);

        return pstmt;
    }

    /**
     *
     * @param parameterizedSQL
     * @param values
     * @param conn
     * @param autoGeneratedKeys
     * @return PreparedStatement
     * @throws SQLException
     */
    public static PreparedStatement prepareStatement(String parameterizedSQL, List<?> values, Connection conn, boolean autoGeneratedKeys)
            throws SQLException {

        PreparedStatement pstmt =
                conn.prepareStatement(parameterizedSQL, autoGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
        populateStatement(pstmt, values);
        return pstmt;
    }

    /**
     *
     * @param pstmt
     * @param values
     * @throws SQLException
     */
    private static void populateStatement(PreparedStatement pstmt, List<?> values) throws SQLException {

        if (values == null || values.isEmpty()) {
            return;
        }

        int valueCount = values.size();
        for (int i = 0; i < valueCount; i++) {

            int paramIndex = i + 1;
            Object value = values.get(i);

            if (value == null) {
                pstmt.setNull(paramIndex, Types.NULL);
            } else if (value instanceof Boolean) {
                pstmt.setBoolean(paramIndex, ((Boolean) value).booleanValue());
            } else if (value instanceof Byte) {
                pstmt.setByte(paramIndex, ((Byte) value).byteValue());
            } else if (value instanceof Double) {
                pstmt.setDouble(paramIndex, ((Double) value).doubleValue());
            } else if (value instanceof Float) {
                pstmt.setFloat(paramIndex, ((Float) value).floatValue());
            } else if (value instanceof Integer) {
                pstmt.setInt(paramIndex, ((Integer) value).intValue());
            } else if (value instanceof Long) {
                pstmt.setLong(paramIndex, ((Long) value).longValue());
            } else if (value instanceof String) {
                pstmt.setString(paramIndex, (String) value);
            } else if (value instanceof BigDecimal) {
                pstmt.setBigDecimal(paramIndex, (BigDecimal) value);
            } else if (value instanceof java.sql.Date) {
                pstmt.setDate(paramIndex, (java.sql.Date) value);
            } else if (value instanceof java.sql.Timestamp) {
                pstmt.setTimestamp(paramIndex, (java.sql.Timestamp) value);
            } else if (value instanceof java.sql.Time) {
                pstmt.setTime(paramIndex, (java.sql.Time) value);
            } else if (value instanceof java.util.Date) {
                pstmt.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) value).getTime()));
            } else {
                pstmt.setObject(paramIndex, value);
            }
        }
    }

    /**
     *
     * @param conn
     */
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (Throwable e) {
                // Ignore closing exceptions.
                LOGGER.warn("Exception when closing connection", e);
            }
        }
    }

    /**
     *
     * @param stmt
     */
    public static void close(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Throwable e) {
                // Ignore closing exceptions.
                LOGGER.warn("Exception when closing statement", e);
            }
        }
    }

    /**
     *
     * @param rs
     */
    public static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Throwable e) {
                // Ignore closing exceptions.
                LOGGER.warn("Exception when closing result set", e);
            }
        }
    }

    /**
     *
     * @param conn
     */
    public static void rollback(Connection conn) {

        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                // Ignore rollback exceptions.
                LOGGER.warn("Exception when rollbacking connection", e);
            }
        }
    }
}
