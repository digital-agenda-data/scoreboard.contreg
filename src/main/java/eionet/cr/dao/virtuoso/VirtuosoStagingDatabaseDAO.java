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

package eionet.cr.dao.virtuoso;

import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import eionet.cr.common.CRException;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.StagingDatabaseDAO;
import eionet.cr.dao.readers.StagingDatabaseDTOReader;
import eionet.cr.dto.StagingDatabaseDTO;
import eionet.cr.dto.StagingDatabaseTableColumnDTO;
import eionet.cr.staging.exp.ExportDTO;
import eionet.cr.staging.exp.ExportDTOReader;
import eionet.cr.staging.exp.ExportRunner;
import eionet.cr.staging.exp.ExportStatus;
import eionet.cr.staging.exp.QueryConfiguration;
import eionet.cr.staging.imp.ImportStatus;
import eionet.cr.util.sql.PairReader;
import eionet.cr.util.sql.SQLUtil;
import eionet.cr.util.sql.SingleObjectReader;

/**
 * Virtuoso-specific implementation of {@link StagingDatabaseDAO}.
 *
 * @author jaanus
 */
public class VirtuosoStagingDatabaseDAO extends VirtuosoBaseDAO implements StagingDatabaseDAO {

    /** */
    private static final Logger LOGGER = Logger.getLogger(VirtuosoStagingDatabaseDAO.class);

    /** */
    private static final String GET_INDICATORS_SPARQL = "select distinct ?s as ?" + PairReader.LEFTCOL + " ?notation as ?"
            + PairReader.RIGHTCOL + " where {" + " ?s a <http://semantic.digital-agenda-data.eu/def/class/Indicator>."
            + " ?s <http://www.w3.org/2004/02/skos/core#notation> ?notation}" + " order by ?notation";

    /** */
    private static final String GET_BREAKDOWNS_SPARQL = "select distinct ?s as ?" + PairReader.LEFTCOL + " ?notation as ?"
            + PairReader.RIGHTCOL + " where {" + " ?s a <http://semantic.digital-agenda-data.eu/def/class/Breakdown>."
            + " ?s <http://www.w3.org/2004/02/skos/core#notation> ?notation}" + " order by ?notation";

    /** */
    private static final String GET_UNITS_SPARQL = "select distinct ?s as ?" + PairReader.LEFTCOL + " ?notation as ?"
            + PairReader.RIGHTCOL + " where {" + " ?s a <http://semantic.digital-agenda-data.eu/def/class/UnitMeasure>."
            + " ?s <http://www.w3.org/2004/02/skos/core#notation> ?notation}" + " order by ?notation";

    /** */
    private static final String GET_REFAREAS_SPARQL = "select distinct ?s as ?" + PairReader.LEFTCOL + " ?notation as ?"
            + PairReader.RIGHTCOL + " where {?s a <http://www.w3.org/2004/02/skos/core#Concept>."
            + " ?s <http://www.w3.org/2004/02/skos/core#inScheme> <http://eurostat.linked-statistics.org/dic/geo>."
            + " ?s <http://www.w3.org/2004/02/skos/core#notation> ?notation} order by ?notation";

    /** */
    private static final String GET_EXPORTED_RESOURCES_SPARQL = "SELECT distinct ?s FROM <" + ExportRunner.EXPORT_URI_PREFIX
            + "@id@> WHERE {?s ?p ?o} order by ?s";

    /** */
    private static final String ADD_NEW_DB_SQL =
            "insert into STAGING_DB (NAME,CREATOR,CREATED,DESCRIPTION,IMPORT_STATUS,IMPORT_LOG) values (?,?,NOW(),?,?,?)";

    /** */
    private static final String UPDATE_DB_METADATA_SQL =
            "update STAGING_DB set DESCRIPTION=?, DEFAULT_QUERY=? where DATABASE_ID=?";

    /** */
    private static final String GET_DATABASE_BY_ID_SQL = "select * from STAGING_DB where DATABASE_ID=?";

    /** */
    private static final String GET_DATABASE_BY_NAME_SQL = "select * from STAGING_DB where NAME=?";

    /** */
    private static final String UPDATE_IMPORT_STATUS_SQL = "update STAGING_DB set IMPORT_STATUS=? where DATABASE_ID=?";

    /** */
    private static final String UPDATE_EXPORT_STATUS_SQL = "update STAGING_DB_RDF_EXPORT set STATUS=? where EXPORT_ID=?";

    /** */
    private static final String ADD_IMPORT_LOG_MESSAGE_SQL =
            "update STAGING_DB set IMPORT_LOG=concat(IMPORT_LOG, ?) where DATABASE_ID=?";

    /** */
    private static final String START_RDF_EXPORT_SQL =
            "insert into STAGING_DB_RDF_EXPORT (DATABASE_ID,EXPORT_NAME,USER_NAME,QUERY_CONF,STARTED,STATUS) values (?,?,?,?,NOW(),?)";

    /** */
    private static final String FINISH_RDF_EXPORT_SQL = "update STAGING_DB_RDF_EXPORT "
            + "set FINISHED=NOW(),STATUS=?,ROW_COUNT=?,NOOF_SUBJECTS=?,NOOF_TRIPLES=?,MISSING_CONCEPTS=?,GRAPHS=? "
            + "where EXPORT_ID=?";

    /** */
    private static final String ADD_EXPORT_LOG_MESSAGE_SQL =
            "update STAGING_DB_RDF_EXPORT set EXPORT_LOG=concat(EXPORT_LOG, ?) where EXPORT_ID=?";

    /** */
    private static final String LIST_ALL_SQL = "select * from STAGING_DB order by NAME asc";

    /** */
    private static final String DELETE_DB_SQL = "delete from STAGING_DB where NAME in (?)";

    /** */
    private static final String COUNT_EXISTING_DBS_SQL = "select count(*) from STAGING_DB where NAME in (?)";

    /** */
    private static final String GET_IMPORT_LOG_SQL = "select IMPORT_LOG from STAGING_DB where DATABASE_ID=?";

    /** */
    private static final String GET_EXPORT_LOG_SQL = "select EXPORT_LOG from STAGING_DB_RDF_EXPORT where EXPORT_ID=?";

    /** */
    private static final String LIST_RDF_EXPORTS_SQL = "select STAGING_DB_RDF_EXPORT.*, STAGING_DB.NAME as DATABASE_NAME"
            + " from STAGING_DB_RDF_EXPORT, STAGING_DB"
            + " where STAGING_DB_RDF_EXPORT.DATABASE_ID=coalesce(?, STAGING_DB_RDF_EXPORT.DATABASE_ID)"
            + " and STAGING_DB_RDF_EXPORT.DATABASE_ID=STAGING_DB.DATABASE_ID"
            + " order by STAGING_DB_RDF_EXPORT.DATABASE_ID, STARTED desc";

    /** */
    private static final String GET_RDF_EXPORT_SQL = "select STAGING_DB_RDF_EXPORT.*, STAGING_DB.NAME as DATABASE_NAME"
            + " from STAGING_DB_RDF_EXPORT, STAGING_DB"
            + " where EXPORT_ID=? and STAGING_DB_RDF_EXPORT.DATABASE_ID=STAGING_DB.DATABASE_ID";

    /** */
    private static final String EXISTS_RDF_EXPORT_SQL =
            "select count(*) from STAGING_DB_RDF_EXPORT where DATABASE_ID=? and EXPORT_NAME=?";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#createDatabase()
     */
    @Override
    public void createDatabase(String databaseName) throws DAOException {

        // We do nothing here, because in Virtuoso there's actually no such thing as "create a database". Instead, tables in
        // Virtuoso's RDBMS are sectioned into so-called qualifiers. A qualifier/database becomes "created" the moment a first
        // table is created with that qualifier.
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#createRecord(eionet.cr.dto.StagingDatabaseDTO, java.lang.String)
     */
    @Override
    public int createRecord(StagingDatabaseDTO databaseDTO, String userName) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(databaseDTO.getName());
        params.add(userName);
        params.add(databaseDTO.getDescription());
        params.add(ImportStatus.NOT_STARTED.name());
        params.add("");

        Connection conn = null;
        try {
            conn = getSQLConnection();
            return SQLUtil.executeUpdateReturnAutoID(ADD_NEW_DB_SQL, params, conn);
        } catch (SQLException e) {
            throw new DAOException(e.getMessage(), e);
        } catch (CRException e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#updateDatabaseMetadata(int, java.lang.String, java.lang.String)
     */
    @Override
    public void updateDatabaseMetadata(int id, String description, String defaultQuery) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(description);
        params.add(defaultQuery);
        params.add(id);

        Connection conn = null;
        try {
            conn = getSQLConnection();
            SQLUtil.executeUpdate(UPDATE_DB_METADATA_SQL, params, conn);
        } catch (SQLException e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#startRDEExport(int, java.lang.String, eionet.cr.staging.exp.QueryConfiguration)
     */
    @Override
    public int startRDEExport(int databaseId, String exportName, String userName, QueryConfiguration queryConf)
            throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(databaseId);
        params.add(exportName);
        params.add(userName);
        params.add(queryConf.toLongString());
        params.add(ExportStatus.STARTED.name());

        Connection conn = null;
        try {
            conn = getSQLConnection();
            return SQLUtil.executeUpdateReturnAutoID(START_RDF_EXPORT_SQL, params, conn);
        } catch (SQLException e) {
            throw new DAOException(e.getMessage(), e);
        } catch (CRException e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#getExportedResourceUris(int)
     */
    @Override
    public List<String> getExportedResourceUris(int exportId) throws DAOException {

        String query = StringUtils.replace(GET_EXPORTED_RESOURCES_SPARQL, "@id@", String.valueOf(exportId));
        return executeSPARQL(query, null, new SingleObjectReader<String>());
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#finishRDFExport(int, eionet.cr.staging.exp.ExportRunner,
     * eionet.cr.staging.exp.ExportStatus)
     */
    @Override
    public void finishRDFExport(int exportId, ExportRunner exportRunner, ExportStatus status) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(status.name());
        params.add(exportRunner.getRowCount());
        params.add(exportRunner.getSubjectCount());
        params.add(exportRunner.getTripleCount());
        params.add(exportRunner.missingConceptsToString());
        params.add(StringUtils.join(exportRunner.getGraphs(), '\n'));
        params.add(exportId);

        Connection conn = null;
        try {
            conn = getSQLConnection();
            SQLUtil.executeUpdate(FINISH_RDF_EXPORT_SQL, params, conn);
        } catch (SQLException e) {
            throw new DAOException("Finishing RDF export record failed with " + e.getClass().getSimpleName(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#getDatabaseById(int)
     */
    @Override
    public StagingDatabaseDTO getDatabaseById(int id) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(id);
        List<StagingDatabaseDTO> list = executeSQL(GET_DATABASE_BY_ID_SQL, params, new StagingDatabaseDTOReader());
        return list == null || list.isEmpty() ? null : list.iterator().next();
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#getDatabaseByName(java.lang.String)
     */
    @Override
    public StagingDatabaseDTO getDatabaseByName(String name) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(name);
        List<StagingDatabaseDTO> list = executeSQL(GET_DATABASE_BY_NAME_SQL, params, new StagingDatabaseDTOReader());
        return list == null || list.isEmpty() ? null : list.iterator().next();
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#getTablesColumns(java.lang.String)
     */
    @Override
    public List<StagingDatabaseTableColumnDTO> getTablesColumns(String dbName) throws DAOException {

        ArrayList<StagingDatabaseTableColumnDTO> result = new ArrayList<StagingDatabaseTableColumnDTO>();

        ResultSet rs = null;
        Connection conn = null;
        try {
            conn = getSQLConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            rs = metaData.getColumns(dbName, null, null, null);
            while (rs.next()) {
                String table = rs.getString("TABLE_NAME");
                String column = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("TYPE_NAME");
                StagingDatabaseTableColumnDTO dto = new StagingDatabaseTableColumnDTO(table, column, dataType);
                result.add(dto);
            }
        } catch (SQLException e) {
            throw new DAOException("Failure getting the tables and columns of database: " + dbName, e);
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(conn);
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#updateImportStatus(int, eionet.cr.staging.ImportStatus)
     */
    @Override
    public void updateImportStatus(int databaseId, ImportStatus importStatus) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(importStatus.name());
        params.add(databaseId);

        Connection conn = null;
        try {
            conn = getSQLConnection();
            SQLUtil.executeUpdate(UPDATE_IMPORT_STATUS_SQL, params, conn);
        } catch (SQLException e) {
            throw new DAOException("Updating database import status failed with " + e.getClass().getSimpleName(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#updateExportStatus(int, eionet.cr.staging.exp.ExportStatus)
     */
    @Override
    public void updateExportStatus(int exportId, ExportStatus status) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(status.name());
        params.add(exportId);

        Connection conn = null;
        try {
            conn = getSQLConnection();
            SQLUtil.executeUpdate(UPDATE_EXPORT_STATUS_SQL, params, conn);
        } catch (SQLException e) {
            throw new DAOException("Updating database RDF export status failed with " + e.getClass().getSimpleName(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#addImportLogMessage(java.lang.String, eionet.cr.staging.ImportLogLevel)
     */
    @Override
    public void appendToImportLog(int databaseId, String message) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(message);
        params.add(databaseId);

        Connection conn = null;
        try {
            conn = getSQLConnection();
            SQLUtil.executeUpdate(ADD_IMPORT_LOG_MESSAGE_SQL, params, conn);
        } catch (SQLException e) {
            throw new DAOException("Adding database import log message failed with " + e.getClass().getSimpleName(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#appendToExportLog(int, java.lang.String)
     */
    @Override
    public void appendToExportLog(int exportId, String message) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(message);
        params.add(exportId);

        Connection conn = null;
        try {
            conn = getSQLConnection();
            SQLUtil.executeUpdate(ADD_EXPORT_LOG_MESSAGE_SQL, params, conn);
        } catch (SQLException e) {
            throw new DAOException("Adding database RDF export log message failed with " + e.getClass().getSimpleName(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#listAll()
     */
    @Override
    public List<StagingDatabaseDTO> listAll() throws DAOException {

        return executeSQL(LIST_ALL_SQL, null, new StagingDatabaseDTOReader());
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#delete(java.util.List)
     */
    @Override
    public void delete(List<String> dbNames) throws DAOException {

        if (dbNames == null || dbNames.isEmpty()) {
            return;
        }

        Connection conn = null;
        try {
            conn = getSQLConnection();
            conn.setAutoCommit(false);

            // First, ensure the given databases are present in staging databases table (to avoid deleting other databases).
            String questionMarks = StringUtils.join(Collections.nCopies(dbNames.size(), "?"), ',');
            String sql = COUNT_EXISTING_DBS_SQL.replace("?", questionMarks);
            int count = NumberUtils.toInt(SQLUtil.executeSingleReturnValueQuery(sql, dbNames, conn).toString());
            if (dbNames.size() > count) {
                throw new DAOException("At least one of the given databases is unknown");
            }

            // Second, delete the databases themselves.
            deleteDatabases(dbNames, conn);

            // Third, delete rows from the staging databases table.
            sql = DELETE_DB_SQL.replace("?", questionMarks);
            SQLUtil.executeUpdate(sql, dbNames, conn);

            conn.commit();
        } catch (SQLException e) {
            SQLUtil.rollback(conn);
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /**
     * Deletes the given databases from Virtuoso.
     *
     * @param dbNames
     *            The database names.
     * @param conn
     *            The SQL connection to use.
     * @throws SQLException
     *             When database access error happens.
     */
    private void deleteDatabases(List<String> dbNames, Connection conn) throws SQLException {

        HashMap<String, HashSet<String>> dbTables = new HashMap<String, HashSet<String>>();
        String userName = conn.getMetaData().getUserName();

        // First, get the tables present in each of the given databases.

        ResultSet rs = null;
        try {
            for (String dbName : dbNames) {

                HashSet<String> tables = new HashSet<String>();
                rs = conn.getMetaData().getTables(dbName, userName, null, null);
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
                if (!tables.isEmpty()) {
                    dbTables.put(dbName, tables);
                }
            }
        } finally {
            SQLUtil.close(rs);
        }

        // Second, loop through the above-found tables, delete each.
        for (Entry<String, HashSet<String>> entry : dbTables.entrySet()) {

            String dbName = entry.getKey();
            HashSet<String> tables = entry.getValue();
            for (String tableName : tables) {
                SQLUtil.executeUpdate("drop table " + dbName + "." + userName + ".\"" + tableName + "\"", conn);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#exists(java.lang.String)
     */
    @Override
    public boolean exists(String dbName) throws DAOException {

        ResultSet rs = null;
        Connection conn = null;
        try {
            conn = getSQLConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            rs = metaData.getTables(dbName, metaData.getUserName(), null, null);
            return rs != null && rs.next();
        } catch (SQLException e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#getImportLog(int)
     */
    @Override
    public String getImportLog(int databaseId) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(Integer.valueOf(databaseId));

        ResultSet rs = null;
        Statement stmt = null;
        Connection conn = null;
        try {
            conn = getSQLConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(GET_IMPORT_LOG_SQL.replace("?", String.valueOf(databaseId)));
            if (rs.next()) {
                Blob blob = rs.getBlob(1);
                if (blob != null) {
                    try {
                        return blob.length() == 0 ? "" : IOUtils.toString(blob.getBinaryStream());
                    } catch (IOException e) {
                        LOGGER.warn("Could not retreive import log of database #" + databaseId + ": " + e);
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }

        } catch (SQLException e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(stmt);
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#getExportLog(int)
     */
    @Override
    public String getExportLog(int exportId) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(Integer.valueOf(exportId));

        ResultSet rs = null;
        Statement stmt = null;
        Connection conn = null;
        try {
            conn = getSQLConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(GET_EXPORT_LOG_SQL.replace("?", String.valueOf(exportId)));
            if (rs.next()) {
                Blob blob = rs.getBlob(1);
                if (blob != null) {
                    try {
                        return blob.length() == 0 ? "" : IOUtils.toString(blob.getBinaryStream());
                    } catch (IOException e) {
                        LOGGER.warn("Could not retreive log of the RDF export with id = " + exportId + ": " + e);
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }

        } catch (SQLException e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(stmt);
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#prepareStatement(java.lang.String, java.lang.String)
     */
    @Override
    public Set<String> prepareStatement(String sql, String dbName) throws DAOException {

        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("The given SQL statement must not be blank!");
        }

        LinkedHashSet<String> result = new LinkedHashSet<String>();

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getSQLConnection(dbName);
            pstmt = SQLUtil.prepareStatement(sql, null, conn);
            ResultSetMetaData metaData = pstmt.getMetaData();
            int colCount = metaData.getColumnCount();
            for (int i = 1; i <= colCount; i++) {
                String colName = metaData.getColumnName(i);
                result.add(colName);
            }
        } catch (SQLException e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(pstmt);
            SQLUtil.close(conn);
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#listRDFExports(int)
     */
    @Override
    public List<ExportDTO> listRDFExports(int databaseId) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(databaseId <= 0 ? (Integer) null : Integer.valueOf(databaseId));
        return executeSQL(LIST_RDF_EXPORTS_SQL, params, new ExportDTOReader());
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#getRDFExport(int)
     */
    @Override
    public ExportDTO getRDFExport(int exportId) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(exportId);
        List<ExportDTO> list = executeSQL(GET_RDF_EXPORT_SQL, params, new ExportDTOReader());
        return list == null || list.isEmpty() ? null : list.iterator().next();
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#existsRDFExport(int, java.lang.String)
     */
    @Override
    public boolean existsRDFExport(int databaseId, String exportName) throws DAOException {

        ArrayList<Object> params = new ArrayList<Object>();
        params.add(databaseId);
        params.add(exportName);

        Object o = executeUniqueResultSQL(EXISTS_RDF_EXPORT_SQL, params, new SingleObjectReader<Object>());
        return o == null ? false : Integer.parseInt(o.toString()) > 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#getIndicators()
     */
    @Override
    public LinkedHashMap<String, String> getIndicators() throws DAOException {

        PairReader<String, String> reader = new PairReader<String, String>();
        executeSPARQL(GET_INDICATORS_SPARQL, null, reader);
        return reader.getResultMap();
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#getBreakdowns()
     */
    @Override
    public LinkedHashMap<String, String> getBreakdowns() throws DAOException {

        PairReader<String, String> reader = new PairReader<String, String>();
        executeSPARQL(GET_BREAKDOWNS_SPARQL, null, reader);
        return reader.getResultMap();
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#getUnits()
     */
    @Override
    public LinkedHashMap<String, String> getUnits() throws DAOException {

        PairReader<String, String> reader = new PairReader<String, String>();
        executeSPARQL(GET_UNITS_SPARQL, null, reader);
        return reader.getResultMap();
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.StagingDatabaseDAO#getRefAreas()
     */
    @Override
    public LinkedHashMap<String, String> getRefAreas() throws DAOException {

        PairReader<String, String> reader = new PairReader<String, String>();
        executeSPARQL(GET_REFAREAS_SPARQL, null, reader);
        return reader.getResultMap();
    }
}
