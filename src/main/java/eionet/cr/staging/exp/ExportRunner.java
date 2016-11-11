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

package eionet.cr.staging.exp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.StagingDatabaseDAO;
import eionet.cr.dto.StagingDatabaseDTO;
import eionet.cr.staging.util.TimePeriodsHarvester;
import eionet.cr.util.LogUtil;
import eionet.cr.util.Util;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.SQLUtil;

/**
 * A thread runs a given RDF export query with a given query configuration on a given staging database.
 *
 * @author jaanus
 */
public final class ExportRunner extends Thread {

    /** */
    private static final int EXPORT_PAGE_SIZE = 5000;

    /** */
    private static final String DEFAULT_INDICATOR_CODE = "*";

    /** */
    private static final String DEFAULT_BREAKDOWN_CODE = "total";

    /** */
    public static final String EXPORT_URI_PREFIX = "http://semantic.digital-agenda-data.eu/import/";

    /**  */
    private static final String REF_AREA = "refArea";

    /**  */
    private static final String UNIT = "unit";

    /**  */
    private static final String BREAKDOWN = "breakdown";

    /**  */
    private static final String INDICATOR = "indicator";

    /** */
    private static final Logger LOGGER = Logger.getLogger(ExportRunner.class);

    /** */
    public static final int MAX_TEST_RESULTS = 500;

    /** */
    private StagingDatabaseDTO dbDTO;

    /** */
    private int exportId;

    /** */
    private String userName;

    /** */
    private QueryConfiguration queryConf;

    /** */
    private URI objectTypeURI;

    /** */
    private URI rdfTypeURI;

    /** */
    private int tripleCount;

    /** */
    private int subjectCount;

    /** */
    private Logger exportLogger;

    /** The {@link StagingDatabaseDAO} used by this thread to access the database. */
    private StagingDatabaseDAO dao;

    /** The export's descriptive name. */
    private String exportName;

    /** */
    private Set<ObjectHiddenProperty> hiddenProperties;

    /** */
    private URI datasetPredicateURI;

    /** */
    private Set<String> existingIndicators;
    private Set<String> existingBreakdowns;
    private Set<String> existingUnits;
    private Set<String> existingRefAreas;

    /** */
    private Set<String> missingIndicators = new LinkedHashSet<String>();
    private Set<String> missingBreakdowns = new LinkedHashSet<String>();
    private Set<String> missingUnits = new LinkedHashSet<String>();
    private Set<String> missingRefAreas = new LinkedHashSet<String>();

    /** */
    private List<Map<String, String>> testResults = new ArrayList<Map<String, String>>();

    /** */
    private HashSet<String> graphs = new HashSet<String>();

    /** */
    private int rowCount;

    /** */
    private HashSet<String> timePeriods = new HashSet<String>();

    /** */
    private Set<URI> touchedDatasets = new HashSet<>();

    /** */
    private Set<URI> clearedGraphs = new HashSet<>();

    /**
     * Private class constructor, to be used for running the export.
     *
     * @param dbDTO
     *            The DTO of the staging database on which the query shall be run.
     * @param exportId
     *            The ID of the export being run.
     * @param exportName
     *            The export's descriptive name.
     * @param userName
     *            User who initiated the export.
     * @param queryConf
     *            The query configuration to run.
     */
    private ExportRunner(StagingDatabaseDTO dbDTO, int exportId, String exportName, String userName, QueryConfiguration queryConf) {

        super();

        if (dbDTO == null || queryConf == null) {
            throw new IllegalArgumentException("Staging database DTO and query configuration must not be null!");
        }
        if (StringUtils.isBlank(userName)) {
            throw new IllegalArgumentException("User name must not be blank!");
        }

        this.dbDTO = dbDTO;
        this.exportId = exportId;
        this.exportName = exportName;
        this.queryConf = queryConf;
        this.userName = userName;
        this.exportLogger = createLogger(exportId);

        ObjectType objectType = ObjectTypes.getByDsd(queryConf.getObjectTypeDsd());
        if (objectType != null) {
            hiddenProperties = objectType.getHiddenProperties();
        }
    }

    /**
     * Private class constructor, to be used for test-running the export.
     *
     * @param dbDTO
     * @param queryConf
     */
    private ExportRunner(StagingDatabaseDTO dbDTO, QueryConfiguration queryConf) {

        if (dbDTO == null || queryConf == null) {
            throw new IllegalArgumentException("Staging database DTO and query configuration must not be null!");
        }

        this.dbDTO = dbDTO;
        this.queryConf = queryConf;
    }

    /**
     * Creates the logger.
     *
     * @param exportId
     *            the export id
     * @return the export logger
     */
    private ExportLogger createLogger(int exportId) {

        String loggerName = "RDF_export_" + exportId;
        ExportLogger logger = (ExportLogger) Logger.getLogger(loggerName, ExportLoggerFactory.INSTANCE);
        logger.setExportId(exportId);
        logger.setLevel(Level.TRACE);
        return logger;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        // Log start event.
        long started = System.currentTimeMillis();
        LogUtil.debug("RDF export (id=" + exportId + ") started by " + userName, exportLogger, LOGGER);

        boolean failed = false;
        RepositoryConnection repoConn = null;
        try {
            // Create repository connection, set its auto-commit to false.
            repoConn = SesameUtil.getRepositoryConnection();
            repoConn.setAutoCommit(false);

            // Prepare re-occurring instances of Sesame's Value and URI, for better performance.
            ValueFactory valueFactory = repoConn.getValueFactory();
            prepareValues(valueFactory);

            // Run the export query and export its results.
            executeExport(repoConn);

            // Update all "touched" datasets.
            updateTouchedDatasets(repoConn, valueFactory);

            // Commit the transaction.
            repoConn.commit();

            // Log finish event.
            long millis = System.currentTimeMillis() - started;
            LogUtil.debug("RDF export (id=" + exportId + ") finished in " + (millis / 1000L) + " sec", exportLogger, LOGGER);

        } catch (Exception e) {
            failed = true;
            SesameUtil.rollback(repoConn);
            LogUtil.debug("RDF export (id=" + exportId + ") failed with error", e, exportLogger, LOGGER);
        } finally {
            SesameUtil.close(repoConn);
        }

        // Start post harvests.
        harvestTimePeriods();

        // Update export status to finished in the DB.
        try {
            getDao().finishRDFExport(exportId, this, failed ? ExportStatus.ERROR : ExportStatus.COMPLETED);
        } catch (DAOException e) {
            LOGGER.error("Failed to finish RDF export record with id = " + exportId, e);
        }
    }

    /**
     * Run the export query and export its results..
     *
     * @param repoConn
     *            the repo conn
     * @throws RepositoryException
     *             the repository exception
     * @throws SQLException
     *             the sQL exception
     * @throws DAOException
     */
    private void executeExport(RepositoryConnection repoConn) throws RepositoryException, SQLException, DAOException {

        // Nothing to do here if query or column mappings is empty.
        String query = queryConf.getQuery();
        if (StringUtils.isBlank(query) || queryConf.getPropertyMappings().isEmpty()) {
            return;
        }

        Connection sqlConn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            // Prepare SQL connection.
            sqlConn = SesameUtil.getSQLConnection(dbDTO.getName());

            // Prepare ValueFactory to be used by each "export row" call below.
            ValueFactory valueFactory = repoConn.getValueFactory();

            rowCount = 0;
            int offset = 0;
            int limit = EXPORT_PAGE_SIZE;
            int rsSize = 0;
            int queryCounter = 0;
            do {
                queryCounter++;

                String pageQuery = buildPageQuery(query, offset, limit);
                offset = offset + limit;

                if (queryCounter <= 2) {
                    LOGGER.debug(String.format("Going to execute page query nr %d:\n%s\n", queryCounter, pageQuery));
                }

                rsSize = 0;
                pstmt = sqlConn.prepareStatement(pageQuery);
                rs = pstmt.executeQuery();

                while (rs.next()) {

                    rowCount++;
                    rsSize++;

                    exportRow(rs, rowCount, repoConn, valueFactory);

                    // Log progress after every 1000 rows, but not more than 50 times.
                    if (rowCount % 1000 == 0) {
                        if (rowCount == 50000) {
                            LogUtil.debug(rowCount + " rows exported, no further row-count logged until export finished...",
                                    exportLogger, LOGGER);
                        } else if (rowCount < 50000) {
                            LogUtil.debug(rowCount + " rows exported", exportLogger, LOGGER);
                        }
                    }
                }

                SQLUtil.close(rs);
                SQLUtil.close(pstmt);

            } while (rsSize == limit);

            LOGGER.debug("Total number of page queries executed: " + queryCounter);
            LogUtil.debug("A total of " + rowCount + " rows exported", exportLogger, LOGGER);

        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(pstmt);
            SQLUtil.close(sqlConn);
        }
    }

    /**
     * @param vf
     */
    private void prepareValues(ValueFactory vf) {

        setPredicateURIs(vf);
        setHiddenPropertiesValues(vf);

        objectTypeURI = vf.createURI(queryConf.getObjectTypeUri());
        rdfTypeURI = vf.createURI(Predicates.RDF_TYPE);

//        String datasetUriTemplate = queryConf.getDatasetUriTemplate();
//        boolean isFixedDataset = datasetUriTemplate != null && !datasetUriTemplate.contains("<value>");
//        if (isFixedDataset) {
//            fixedDatasetURI = vf.createURI(datasetUriTemplate);
//            fixedDatasetGraphURI = vf.createURI(datasetUriTemplate.replace("/dataset/", "/data/"));
//        }

        datasetPredicateURI = vf.createURI(Predicates.DATACUBE_DATA_SET);
    }

    /**
     * Sets the predicate ur is.
     *
     * @param vf
     *            the new predicate ur is
     */
    private void setPredicateURIs(ValueFactory vf) {

        Map<ObjectProperty, String> propertyMappings = queryConf.getPropertyMappings();
        Set<ObjectProperty> properties = propertyMappings.keySet();
        for (ObjectProperty property : properties) {
            property.setPredicateURI(vf);
        }
    }

    /**
     * Export row.
     *
     * @param rs
     *            the rs
     * @param rowIndex
     *            the row index
     * @param repoConn
     *            the repo conn
     * @param vf
     *            the vf
     * @throws SQLException
     *             the sQL exception
     * @throws RepositoryException
     *             the repository exception
     * @throws DAOException
     */
    private void exportRow(ResultSet rs, int rowIndex, RepositoryConnection repoConn, ValueFactory vf)
            throws SQLException, RepositoryException, DAOException {

        if (rowIndex == 1) {
            loadExistingCodelists();
        }

        // Prepare subject URI on the basis of the template in the query configuration.
        String subjectUri = queryConf.getObjectUriTemplate();
        if (StringUtils.isBlank(subjectUri)) {
            throw new IllegalArgumentException("The object URI template in the query configuration must not be blank!");
        }

        // Prepare the map of ObjectDTO to be added to the subject later.
        LinkedHashMap<URI, ArrayList<Value>> valuesByPredicate = new LinkedHashMap<URI, ArrayList<Value>>();

        // Add rdf:type predicate-value.
        addPredicateValue(valuesByPredicate, rdfTypeURI, objectTypeURI);

        // Add predicate-value pairs for hidden properties.
        if (hiddenProperties != null) {
            for (ObjectHiddenProperty hiddenProperty : hiddenProperties) {
                addPredicateValue(valuesByPredicate, hiddenProperty.getPredicateURI(), hiddenProperty.getValueValue());
            }
        }

        // Loop through the query configuration's column mappings, construct ObjectDTO for each.
        for (Entry<ObjectProperty, String> entry : queryConf.getPropertyMappings().entrySet()) {

            String colName = entry.getValue();
            String colValue = rs.getString(colName);
            ObjectProperty property = entry.getKey();

            if (StringUtils.isBlank(colValue)) {
                if (property.getId().equals(BREAKDOWN)) {
                    colValue = DEFAULT_BREAKDOWN_CODE;
                } else if (property.getId().equals(INDICATOR)) {
                    colValue = DEFAULT_INDICATOR_CODE;
                }
            }

            if (StringUtils.isNotBlank(colValue)) {

                // Replace property place-holder in subject ID
                subjectUri = StringUtils.replace(subjectUri, "<" + property.getId() + ">", colValue);

                URI predicateURI = property.getPredicateURI();
                if (predicateURI != null) {

                    String propertyValue = property.getValueTemplate();
                    if (propertyValue == null) {
                        propertyValue = colValue;
                    } else {
                        // Replace the column value place-holder in the value template (the latter cannot be specified by user)
                        propertyValue = StringUtils.replace(propertyValue, "<value>", colValue);
                    }

                    recordMissingConcepts(property, colValue, propertyValue);

                    Value value = null;
                    if (property.isLiteralRange()) {
                        try {
                            String dataTypeUri = property.getDataType();
                            value = vf.createLiteral(propertyValue, dataTypeUri == null ? null : vf.createURI(dataTypeUri));
                        } catch (IllegalArgumentException e) {
                            value = vf.createLiteral(propertyValue);
                        }
                    } else {
                        value = vf.createURI(propertyValue);
                    }

                    addPredicateValue(valuesByPredicate, predicateURI, value);
                }
            }
        }

        // Prepare target dataset URI and take into account the dynamic dataset identifier column if specified.
        String datasetIdentifier = null;
        String targetDatasetUri = queryConf.getDatasetUriTemplate();
        String datasetIdentifierColumn = queryConf.getDatasetIdentifierColumn();
        if (StringUtils.isNotBlank(datasetIdentifierColumn)) {
                try {
                    datasetIdentifier = rs.getString(datasetIdentifierColumn);
                    if (StringUtils.isNotBlank(datasetIdentifier)) {
                        targetDatasetUri = targetDatasetUri.replace("<identifier>", datasetIdentifier);
                    }
                } catch (Exception e) {
                    // Ignore.
                }
        } else if (StringUtils.isNotBlank(targetDatasetUri)) {
            datasetIdentifier = StringUtils.substringAfterLast(targetDatasetUri, "/dataset/");
        }

        subjectUri = subjectUri.replace("<dataSet>", datasetIdentifier);

        // Add cube:dataSet predicate.
        if (StringUtils.isNotBlank(targetDatasetUri)) {
            addPredicateValue(valuesByPredicate, datasetPredicateURI, vf.createURI(targetDatasetUri));
        } else {
            throw new IllegalArgumentException("Could not detect target dataset URI!");
        }

        // If <indicator> column placeholder not replaced yet, then use the default.
        if (subjectUri.indexOf("<indicator>") != -1) {
            subjectUri = StringUtils.replace(subjectUri, "<indicator>", DEFAULT_INDICATOR_CODE);
        }

        // If <breakdown> column placeholder not replaced yet, then use the default.
        if (subjectUri.indexOf("<breakdown>") != -1) {
            subjectUri = StringUtils.replace(subjectUri, "<breakdown>", DEFAULT_BREAKDOWN_CODE);
        }

        // Loop over predicate-value pairs and create the triples in the triple store.
        if (!valuesByPredicate.isEmpty()) {

            URI targetGraphURI = vf.createURI(targetDatasetUri.replace("/dataset/", "/data/"));

            int tripleCountBefore = tripleCount;
            URI subjectURI = vf.createURI(subjectUri);
            for (Entry<URI, ArrayList<Value>> entry : valuesByPredicate.entrySet()) {

                ArrayList<Value> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    URI predicateURI = entry.getKey();
                    for (Value value : values) {

                        if (queryConf.isClearDataset() && !clearedGraphs.contains(targetGraphURI)) {
                            LogUtil.debug("Clearing the graph: " + targetGraphURI, exportLogger, LOGGER);
                            try {
                                repoConn.clear(targetGraphURI);
                                clearedGraphs.add(targetGraphURI);
                            } catch (RepositoryException e) {
                                throw new DAOException("Failed clearing graph: " + targetGraphURI, e);
                            }
                        }

                        LOGGER.trace(String.format("Adding triple: <%s> <%s> <%s> <%s>", subjectURI.stringValue(),
                                predicateURI.stringValue(), value.toString(), targetGraphURI.stringValue()));

                        repoConn.add(subjectURI, predicateURI, value, targetGraphURI);
                        graphs.add(targetGraphURI.stringValue());

                        if (Predicates.DATACUBE_DATA_SET.equals(predicateURI.stringValue())) {
                            if (value instanceof URI) {
                                touchedDatasets.add((URI) value);
                            }
                        }

                        tripleCount++;
                        if (tripleCount % 5000 == 0) {
                            LOGGER.debug(tripleCount + " triples exported so far");
                        }

                        // Time periods should be harvested afterwards.
                        if (Predicates.DAS_TIMEPERIOD.equals(predicateURI.stringValue())) {
                            if (value instanceof URI) {
                                timePeriods.add(value.stringValue());
                            }
                        }
                    }
                }
            }

            if (tripleCount > tripleCountBefore) {
                subjectCount++;
            }
        }
    }

    /**
     * Adds the predicate value.
     *
     * @param valuesByPredicate
     *            the values by predicate
     * @param predicateURI
     *            the predicate uri
     * @param value
     *            the value
     */
    private void addPredicateValue(LinkedHashMap<URI, ArrayList<Value>> valuesByPredicate, URI predicateURI, Value value) {

        ArrayList<Value> values = valuesByPredicate.get(predicateURI);
        if (values == null) {
            values = new ArrayList<Value>();
            valuesByPredicate.put(predicateURI, values);
        }
        values.add(value);
    }

    private void updateTouchedDatasets(RepositoryConnection repoConn, ValueFactory vf) throws RepositoryException {

        // Prepare some URIs.

        URI dcTermsModifiedURI = vf.createURI(Predicates.DCTERMS_MODIFIED);
        Literal modifiedDateValue = vf.createLiteral(Util.virtuosoDateToString(new Date()), XMLSchema.DATETIME);

        URI rdfTypeURI = vf.createURI(Predicates.RDF_TYPE);
        URI cubeDataSetURI = vf.createURI(Subjects.DATACUBE_DATA_SET);

        URI dcTermsIdentifierURI = vf.createURI(Predicates.DCTERMS_IDENTIFIER);
        URI rdfsLabelURI = vf.createURI(Predicates.RDFS_LABEL);

        URI cubeStructureURI = vf.createURI(Predicates.DATACUBE_STRUCTURE);
        URI dsdURI = queryConf.getObjectTypeDsd() == null ? null : vf.createURI(queryConf.getObjectTypeDsd().getUri());

        Resource[] emptyResourceArray = new Resource[0];

        for (URI datasetURI : touchedDatasets) {

            URI graphURI = datasetURI;

            // TODO: obtain dataset identifer in a less hardcoded way.
            String datasetIdentifier = StringUtils.substringAfterLast(datasetURI.stringValue(), "/");

            // Remove all previous dcterms:modified triples of the given dataset.
            repoConn.remove(datasetURI, dcTermsModifiedURI, null, graphURI);

            // Add new dcterms:modified triple for the given dataset.
            repoConn.add(datasetURI, dcTermsModifiedURI, modifiedDateValue, graphURI);

            // Add other triples.

            repoConn.add(datasetURI, rdfTypeURI, cubeDataSetURI, graphURI);

            boolean hasIdentifier = repoConn.hasStatement(datasetURI, dcTermsIdentifierURI, null, false, emptyResourceArray);
            if (!hasIdentifier) {
                repoConn.add(datasetURI, dcTermsIdentifierURI, vf.createLiteral(datasetIdentifier), graphURI);
            }

            boolean hasDSD = repoConn.hasStatement(datasetURI, cubeStructureURI, null, false, emptyResourceArray);
            if (!hasDSD && dsdURI != null) {
                repoConn.add(datasetURI, cubeStructureURI, dsdURI, graphURI);
            }

            boolean hasLabel = repoConn.hasStatement(datasetURI, rdfsLabelURI, null, false, emptyResourceArray);
            if (!hasLabel) {
                repoConn.add(datasetURI, rdfsLabelURI, vf.createLiteral(datasetIdentifier), graphURI);
            }
        }
    }

    /**
     * Lazy getter for the {@link #dao}.
     *
     * @return the DAO
     */
    private StagingDatabaseDAO getDao() {

        if (dao == null) {
            dao = DAOFactory.get().getDao(StagingDatabaseDAO.class);
        }

        return dao;
    }

    /**
     * Start.
     *
     * @param dbDTO
     *            the db dto
     * @param exportName
     *            the export name
     * @param userName
     *            the user name
     * @param queryConf
     *            the query conf
     * @return the export runner
     * @throws DAOException
     *             the dAO exception
     */
    public static synchronized ExportRunner start(StagingDatabaseDTO dbDTO, String exportName, String userName,
            QueryConfiguration queryConf) throws DAOException {

        // Create the export record in the database.
        int exportId =
                DAOFactory.get().getDao(StagingDatabaseDAO.class).startRDEExport(dbDTO.getId(), exportName, userName, queryConf);

        ExportRunner exportRunner = new ExportRunner(dbDTO, exportId, exportName, userName, queryConf);
        exportRunner.start();
        return exportRunner;
    }

    /**
     * Gets the export id.
     *
     * @return the exportId
     */
    public int getExportId() {
        return exportId;
    }

    /**
     * Gets the triple count.
     *
     * @return the tripleCount
     */
    public int getTripleCount() {
        return tripleCount;
    }

    /**
     * Gets the subject count.
     *
     * @return the subjectCount
     */
    public int getSubjectCount() {
        return subjectCount;
    }

    /**
     * Gets the export name.
     *
     * @return the exportName
     */
    public String getExportName() {
        return exportName;
    }

    /**
     * Sets the hidden properties values.
     *
     * @param vf
     *            the new hidden properties values
     */
    private void setHiddenPropertiesValues(ValueFactory vf) {

        if (hiddenProperties != null && !hiddenProperties.isEmpty() && vf != null) {
            for (ObjectHiddenProperty hiddenProperty : hiddenProperties) {
                hiddenProperty.setValues(vf);
            }
        }
    }

    /**
     *
     * @param dbDTO
     * @param queryConf
     * @return
     * @throws SQLException
     * @throws DAOException
     * @throws RepositoryException
     */
    public static ExportRunner test(StagingDatabaseDTO dbDTO, QueryConfiguration queryConf)
            throws RepositoryException, DAOException, SQLException {

        ExportRunner exportRunner = new ExportRunner(dbDTO, queryConf);
        exportRunner.test();
        return exportRunner;
    }

    /**
     *
     * @throws SQLException
     * @throws RepositoryException
     * @throws DAOException
     */
    private void test() throws RepositoryException, SQLException, DAOException {

        // Nothing to do here if query or property mappings empty.
        String query = queryConf.getQuery();
        if (StringUtils.isBlank(query) || queryConf.getPropertyMappings().isEmpty()) {
            return;
        }

        String countQuery = buildCountQuery(query);

        ResultSet rs = null;
        Connection sqlConn = null;
        PreparedStatement pstmt = null;
        try {
            sqlConn = SesameUtil.getSQLConnection(dbDTO.getName());

            pstmt = sqlConn.prepareStatement(countQuery);
            LOGGER.debug("Executing count query:\n" + countQuery + "\n");
            rs = pstmt.executeQuery();
            rowCount = rs.next() ? rs.getInt(1) : 0;
            LOGGER.debug("Count query returned " + rowCount);

            SQLUtil.close(rs);
            SQLUtil.close(pstmt);

            if (rowCount > 0) {

                String firstRowsQuery = buildPageQuery(query, 0, MAX_TEST_RESULTS);

                LOGGER.debug("Executing test results query:\n" + firstRowsQuery + "\n");
                pstmt = sqlConn.prepareStatement(firstRowsQuery);
                rs = pstmt.executeQuery();

                int i = 0;
                while (rs.next()) {
                    if (++i == 1) {
                        LOGGER.debug("Loading existing codelists ...");
                        loadExistingCodelists();
                        LOGGER.debug("Processing the first test results row ...");
                    }
                    processTestRow(rs);
                }
                LOGGER.debug(i + " test results rows processed!");
            }
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(pstmt);
            SQLUtil.close(sqlConn);
        }
    }

    /**
     *
     * @param query
     * @param offset
     * @param limit
     * @return
     * @throws DAOException
     */
    private String buildPageQuery(String query, int offset, int limit) throws DAOException {

        String trimmedQuery = query.trim();
        if (!StringUtils.startsWithIgnoreCase(trimmedQuery, "SELECT")) {
            throw new DAOException("Was expecting the query to strat with a 'SELECT' statement!");
        }

        String resultQuery = new StringBuilder().append(String.format("SELECT TOP %d,%d * FROM (", offset, limit))
                .append(query.trim()).append(") as QRY").toString();
        return resultQuery;
    }

    /**
     *
     * @param query
     * @return
     * @throws DAOException
     */
    private String buildCountQuery(String query) throws DAOException {

        String upperQuery = query.toUpperCase().trim();
        if (!upperQuery.startsWith("SELECT")) {
            throw new DAOException("Was expecting query to start with a 'SELECT' statement!");
        }

        String resultQuery =
                new StringBuilder().append("SELECT COUNT(*) FROM (").append(query.trim()).append(") as QRY").toString();
        return resultQuery;
    }

    /**
     *
     * @param rs
     * @throws SQLException
     * @throws DAOException
     */
    private void processTestRow(ResultSet rs) throws SQLException, DAOException {

        LinkedHashMap<String, String> rowMap = new LinkedHashMap<String, String>();
        for (Entry<ObjectProperty, String> entry : queryConf.getPropertyMappings().entrySet()) {

            ObjectProperty property = entry.getKey();
            String colName = entry.getValue();
            String colValue = rs.getString(colName);

            String valueTemplate = property.getValueTemplate();
            String propertyValue = valueTemplate == null ? colValue : StringUtils.replace(valueTemplate, "<value>", colValue);
            recordMissingConcepts(property, colValue, propertyValue);

            rowMap.put(colName, colValue);
        }

        if (!rowMap.isEmpty()) {
            testResults.add(rowMap);
        }
    }

    /**
     * @param property
     * @param colValue
     * @param propertyValue
     */
    private void recordMissingConcepts(ObjectProperty property, String colValue, String propertyValue) {

        if (INDICATOR.equals(property.getId()) && !existingIndicators.contains(propertyValue)) {
            missingIndicators.add(colValue);
        }
        if (BREAKDOWN.equals(property.getId()) && !existingBreakdowns.contains(propertyValue)) {
            missingBreakdowns.add(colValue);
        }
        if (UNIT.equals(property.getId()) && !existingUnits.contains(propertyValue)) {
            missingUnits.add(colValue);
        }
        if (REF_AREA.equals(property.getId()) && !existingRefAreas.contains(propertyValue)) {
            missingRefAreas.add(colValue);
        }
    }

    /**
     * @throws DAOException
     */
    private void loadExistingCodelists() throws DAOException {

        existingIndicators = getDao().getIndicators().keySet();
        existingBreakdowns = getDao().getBreakdowns().keySet();
        existingUnits = getDao().getUnits().keySet();
        existingRefAreas = getDao().getRefAreas().keySet();
    }

    /**
     * @return the testResults
     */
    public List<Map<String, String>> getTestResults() {
        return testResults;
    }

    /**
     * @return the missingIndicators
     */
    public Set<String> getMissingIndicators() {
        return missingIndicators;
    }

    /**
     * @return the missingBreakdowns
     */
    public Set<String> getMissingBreakdowns() {
        return missingBreakdowns;
    }

    /**
     * @return the missingUnits
     */
    public Set<String> getMissingUnits() {
        return missingUnits;
    }

    /**
     * @return the missingRefAreas
     */
    public Set<String> getMissingRefAreas() {
        return missingRefAreas;
    }

    /**
     *
     * @return
     */
    public int getMaxTestResults() {
        return MAX_TEST_RESULTS;
    }

    /**
     *
     * @return
     */
    public boolean isFoundMissingConcepts() {

        return !missingIndicators.isEmpty() || !missingBreakdowns.isEmpty() || !missingUnits.isEmpty()
                || !missingRefAreas.isEmpty();
    }

    /**
     * @return the rowCount
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     *
     * @return
     */
    public String missingConceptsToString() {

        if (!isFoundMissingConcepts()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (!missingIndicators.isEmpty()) {
            sb.append("Indicators: ").append(missingIndicators).append("\n");
        }
        if (!missingBreakdowns.isEmpty()) {
            sb.append("Breakdowns: ").append(missingBreakdowns).append("\n");
        }
        if (!missingUnits.isEmpty()) {
            sb.append("Units: ").append(missingUnits).append("\n");
        }
        if (!missingRefAreas.isEmpty()) {
            sb.append("Ref. areas: ").append(missingRefAreas).append("\n");
        }
        return sb.toString();
    }

    /**
     *
     * @param str
     * @return
     */
    public static LinkedHashMap<String, List<String>> missingConceptsFromString(String str) {

        LinkedHashMap<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        if (StringUtils.isNotBlank(str)) {
            String[] lines = StringUtils.split(str, '\n');
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                String type = StringUtils.substringBefore(line, ":").trim();
                if (StringUtils.isNotBlank(type)) {
                    String csv = StringUtils.substringBefore(StringUtils.substringAfter(line, "["), "]").trim();
                    if (StringUtils.isNotBlank(csv)) {
                        List<String> values = Arrays.asList(StringUtils.split(csv, ", "));
                        if (!values.isEmpty()) {
                            result.put(type, values);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * @return the graphs
     */
    public Set<String> getGraphs() {
        return graphs;
    }

    /**
     *
     */
    private void harvestTimePeriods() {

        if (timePeriods.isEmpty()) {
            return;
        }

        LOGGER.debug("Going to harvest time periods ...");
        TimePeriodsHarvester tpHarvester = new TimePeriodsHarvester(timePeriods);
        tpHarvester.execute();
        int harvestedCount = tpHarvester.getHarvestedCount();
        int newCount = tpHarvester.getNoOfNewPeriods();
        LOGGER.debug(harvestedCount + " time periods harvested, " + newCount + " of them were new");
    }
}
