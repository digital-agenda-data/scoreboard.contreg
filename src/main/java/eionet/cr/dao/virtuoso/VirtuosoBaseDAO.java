package eionet.cr.dao.virtuoso;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import eionet.cr.dao.DAOException;
import eionet.cr.dao.helpers.SearchHelper;
import eionet.cr.dao.readers.SubjectDataReader;
import eionet.cr.dto.ObjectDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.util.Bindings;
import eionet.cr.util.sesame.SPARQLResultSetReader;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.SQLResultSetReader;
import eionet.cr.util.sql.SQLUtil;
import eionet.cr.util.sql.SingleObjectReader;

/**
 *
 * @author jaanus
 *
 */
public abstract class VirtuosoBaseDAO {

    /** */
    // public static final String BNODE_URI_PREFIX = "nodeID://";
    public static final String BNODE_URI_PREFIX = "_:";

    /** */
    protected Logger logger = Logger.getLogger(VirtuosoBaseDAO.class);

    /**
     *
     * @return
     */
    protected Connection getSQLConnection() throws SQLException {

        return SesameUtil.getSQLConnection();
    }

    /**
     *
     * @param dbName
     * @return
     * @throws SQLException
     */
    protected Connection getSQLConnection(String dbName) throws SQLException {
        return SesameUtil.getSQLConnection(dbName);
    }

    /**
     *
     * @param graphUri
     * @throws DAOException
     */
    protected void clearGraph(String graphUri) throws DAOException {

        RepositoryConnection conn = null;
        try {
            conn = SesameUtil.getRepositoryConnection();
            conn.clear(conn.getValueFactory().createURI(graphUri));
        } catch (Exception e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SesameUtil.close(conn);
        }
    }

    /**
     *
     * @param <T>
     * @param sparql
     * @param bindings
     * @param reader
     * @return
     * @throws DAOException
     */
    protected <T> List<T> executeSPARQL(String sparql, Bindings bindings, SPARQLResultSetReader<T> reader) throws DAOException {

        RepositoryConnection conn = null;
        try {
            conn = SesameUtil.getRepositoryConnection();
            SesameUtil.executeQuery(sparql, bindings, reader, conn);
            return reader.getResultList();
        } catch (Exception e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SesameUtil.close(conn);
        }
    }

    /**
     *
     * @param <T>
     * @param sparql
     * @param reader
     * @return
     * @throws DAOException
     */
    // TODO - rename it to executeStaticSPARQL() ?
    protected <T> List<T> executeSPARQL(String sparql, SPARQLResultSetReader<T> reader) throws DAOException {

        return executeSPARQL(sparql, null, reader);
    }

    /**
     * Executes SPARQL query that updates data.
     *
     * @param sparql SPARQL
     * @param bindings Query bindings, if no bindings, null is accepted as the value
     * @param conn Virtuoso repository connection
     * @throws DAOException if update fails
     */
    protected void executeSPARUL(String sparql, Bindings bindings, RepositoryConnection conn) throws DAOException {
        try {
            SesameUtil.executeSPARUL(sparql, bindings, conn);
        } catch (Exception e) {
            throw new DAOException(e.toString(), e);
        }
    }

    /**
     *
     * @param sparul
     * @param defautGraphUri
     * @return
     * @throws DAOException
     */
    protected int executeSPARUL(String sparul, String... defautGraphUri) throws DAOException {

        RepositoryConnection conn = null;
        try {
            try {
                conn = SesameUtil.getRepositoryConnection();
                return SesameUtil.executeSPARUL(sparul, conn, defautGraphUri);
            } catch (OpenRDFException e) {
                throw new DAOException(e.getMessage(), e);
            }
        } finally {
            SesameUtil.close(conn);
        }
    }

    /**
     * Executes SPARQL with no bindings that is expected to have only one result and returns the unique value.
     *
     * @param <T>
     * @param sql
     * @param reader
     * @return
     * @throws DAOException if query fails
     */
    protected <T> T executeUniqueResultSPARQL(String sql, SPARQLResultSetReader<T> reader) throws DAOException {

        return executeUniqueResultSPARQL(sql, null, reader);
    }

    /**
     * Executes SPARQL that is expected to have only one result and returns the unique value.
     *
     * @param <T>
     * @param sparql
     * @param bindings Binding values for the prepared SPARQL
     * @param reader
     * @return
     * @throws DAOException if query fails
     */
    protected <T> T executeUniqueResultSPARQL(String sparql, Bindings bindings, SPARQLResultSetReader<T> reader)
            throws DAOException {

        List<T> result = executeSPARQL(sparql, bindings, reader);
        return (result == null || result.isEmpty()) ? null : result.get(0);
    }

    /**
     * Finds any triples for the given subject, and if at least one found, forms {@link SubjectDTO} and returns it. Otherwise
     * returns null.
     *
     * @param subjectUri The URI of the subject to look for. Can be null or blank, in which case null is returned,
     * @return The subject's {@link SubjectDTO} as described above.
     * @throws DAOException If any sort of data access error occurs.
     */
    protected SubjectDTO findSubject(String subjectUri) throws DAOException {

        if (StringUtils.isBlank(subjectUri)) {
            return null;
        }

        SubjectDTO subjectDTO = null;
        RepositoryConnection conn = null;
        RepositoryResult<Statement> statements = null;
        try {
            conn = SesameUtil.getRepositoryConnection();
            ValueFactory vf = conn.getValueFactory();
            statements = conn.getStatements(vf.createURI(subjectUri), (URI) null, (Value) null, true);
            if (statements != null) {

                boolean isFirstStatement = true;
                while (statements.hasNext()) {

                    Statement statement = statements.next();
                    Resource subject = statement.getSubject();

                    if (isFirstStatement) {
                        subjectDTO = new SubjectDTO(subject.stringValue(), subject instanceof BNode);
                        isFirstStatement = false;
                    }

                    Value object = statement.getObject();

                    boolean isLiteral = object instanceof Literal;
                    String language = isLiteral ? ((Literal) object).getLanguage() : null;
                    URI datatype = isLiteral ? ((Literal) object).getDatatype() : null;
                    boolean isAnonymous = isLiteral ? false : ((Resource) object) instanceof BNode;

                    ObjectDTO objectDTO = new ObjectDTO(object.stringValue(), language, isLiteral, isAnonymous, datatype);
                    subjectDTO.addObject(statement.getPredicate().stringValue(), objectDTO);
                }
            }
        } catch (RepositoryException e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SesameUtil.close(statements);
            SesameUtil.close(conn);
        }

        return subjectDTO;
    }

    /**
     * Returns list of {@link SubjectDTO} objects representing the given subject URIs. The second input of the method is the array
     * of URIs of predicates to query for. i.e. the returned {@link SubjectDTO} objects will contain predicates only from this
     * array. If none of these predicates was found for these subjects, the returned {@link SubjectDTO} objects will simply contain
     * no predicates. If the predicates array is null or empty, the method queries for all predicates!
     *
     * NB! This method assumes that the given subjects have already been found by the caller of this method, i.e. at least one
     * triple for each exists. This means that if the method will not find the specific predicates queried for, {@link SubjectDTO}
     * objects will still be returned for every such subject. Therefore this method should not be used to determine if a subject
     * exists, because it returns an "empty" {@link SubjectDTO} even if the repository contains no triples for this subject.
     *
     * @param subjectUris The list of URIs of subjects to query for.
     * @param predicateUris The list of URIs of predicates to query for.
     * @return List where there is a {@link SubjectDTO} for every queried subject.
     * @throws DAOException If any sort of data access error occurs.
     */
    protected List<SubjectDTO> getFoundSubjectsData(List<String> subjectUris, String[] predicateUris) throws DAOException {

        SubjectDataReader reader = SubjectDataReader.getInstance(subjectUris, predicateUris);
        Bindings bindings = new Bindings();
        String query = reader.getQuery(bindings);
        executeSPARQL(query, bindings, reader);
        List<SubjectDTO> resultList = reader.getResultList();

        // The point of the next loop is to ensure that none of the elements in the result list will be null,
        // because we are getting the data of already FOUND subjects, as indicated by the method signature and JavaDoc.
        // Null elements are possible when a subject doesn't have any of the predicates that the reader was told to retrieve,
        // yet it has some other predicates.
        int i = 0;
        for (SubjectDTO subjectDTO : resultList) {
            if (subjectDTO == null) {
                subjectDTO = new SubjectDTO(subjectUris.get(i), false);
                resultList.set(i, subjectDTO);
            }
            i++;
        }

        return resultList;
    }

    /**
     * Count the total number of rows retrieved by the query constructed in SearchHelper.
     *
     * @param helper SearchHelper object.
     * @return number of rows
     * @throws DAOException
     */
    protected int getExactRowCount(SearchHelper helper) throws DAOException {

        String query = helper.getCountQuery(new ArrayList<Object>());
        Bindings bindings = helper.getQueryBindings();
        Object resultObject = executeUniqueResultSPARQL(query, bindings, new SingleObjectReader<Long>());
        return Integer.valueOf(resultObject.toString());
    }

    /**
     * Count the total number of rows retrieved by the query constructed in SearchHelper.
     *
     * @param helper SearchHelper object.
     * @param inParams
     * @return number of rows
     * @throws DAOException
     */
    protected int getExactRowCount(SearchHelper helper, List<Object> inParams) throws DAOException {
        String query = helper.getCountQuery(inParams);
        Object resultObject = executeUniqueResultSPARQL(query, new SingleObjectReader<Long>());
        return Integer.valueOf(resultObject.toString());
    }

    /**
     * helper method to execute sql queries. Handles connection init, close. Wraps Exceptions into {@link DAOException}
     *
     * @param <T> - type of the returned object
     * @param sql - sql string
     * @param params - parameters to insert into sql string
     * @param reader - reader, to convert resultset
     * @return result of the sql query
     * @throws DAOException
     */
    protected <T> List<T> executeSQL(String sql, List<?> params, SQLResultSetReader<T> reader) throws DAOException {
        Connection conn = null;
        try {
            conn = getSQLConnection();
            SQLUtil.executeQuery(sql, params, reader, conn);
            List<T> list = reader.getResultList();
            return list;
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /**
     * executes insert or update with given sql and parameters.
     *
     * @param sql - sql string to execute
     * @param params - sql params
     * @throws DAOException
     */
    protected void executeSQL(String sql, List<?> params) throws DAOException {
        Connection conn = null;
        PreparedStatement statement = null;
        try {
            conn = getSQLConnection();
            if (params != null && !params.isEmpty()) {
                statement = SQLUtil.prepareStatement(sql, params, conn);
                statement.execute();
            } else {
                SQLUtil.executeUpdate(sql, conn);
            }
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(statement);
            SQLUtil.close(conn);
        }
    }

    /**
     *
     * @param <T>
     * @param sql
     * @param params
     * @param reader
     * @return
     * @throws DAOException
     */
    protected <T> T executeUniqueResultSQL(String sql, List<?> params, SQLResultSetReader<T> reader) throws DAOException {
        List<T> result = executeSQL(sql, params, reader);
        return result == null || result.isEmpty() ? null : result.get(0);
    }

    /**
     * Orders the map the same way as subjectUris list is ordered. Returns new list.
     *
     * @param <T>
     * @param subjectUris
     * @param map
     * @return List<T>
     * @throws DAOException
     */
    protected <T> List<T> getOrderedList(List<String> subjectUris, Map<String, T> map) throws DAOException {
        List<T> resultList = new ArrayList<T>();
        if (subjectUris != null && map != null) {
            for (String subjectUri : subjectUris) {
                if (map.containsKey(subjectUri)) {
                    resultList.add(map.get(subjectUri));
                }
            }
        }
        return resultList;
    }

    /**
     * Just delegates the call to {@link SesameUtil#createSPARQLVariablesCSV(String, int)}.
     *
     * @param varName
     * @param times
     * @return
     */
    protected String variablesCSV(String varName, int times) {
        return SesameUtil.createSPARQLVariablesCSV(varName, times);
    }
}
