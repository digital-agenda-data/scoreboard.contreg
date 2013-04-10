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
import java.util.Collection;
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
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import eionet.cr.common.Predicates;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dao.StagingDatabaseDAO;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.dto.StagingDatabaseDTO;
import eionet.cr.harvest.OnDemandHarvester;
import eionet.cr.util.LogUtil;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.SQLUtil;
import eionet.cr.web.security.CRUser;

/**
 * A thread runs a given RDF export query with a given query configuration on a given staging database.
 *
 * @author jaanus
 */
public class ExportRunner extends Thread {

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
    private URI graphURI;

    /** */
    private URI datasetPredicateURI;
    private URI datasetValueURI;
    private String datasetIdentifier;

    /** */
    private URI indicatorPredicateURI;
    private URI indicatorValueURI;

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

    private HashSet<String> harvestUris = new HashSet<String>();

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

        ObjectType objectType = ObjectTypes.getByUri(queryConf.getObjectTypeUri());
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

        long started = System.currentTimeMillis();
        LogUtil.debug("RDF export (id=" + exportId + ") started by " + userName, exportLogger, LOGGER);

        boolean failed = false;
        RepositoryConnection repoConn = null;
        try {
            if (queryConf.isClearDataset() && StringUtils.isNotBlank(queryConf.getDatasetUri())) {
                LogUtil.debug("Clearing the dataset: " + queryConf.getDatasetUri(), exportLogger, LOGGER);
                DAOFactory.get().getDao(HarvestSourceDAO.class).clearGraph(queryConf.getDatasetUri());
            }

            repoConn = SesameUtil.getRepositoryConnection();
            repoConn.setAutoCommit(false);

            doRun(repoConn);
            repoConn.commit();

            long millis = System.currentTimeMillis() - started;
            LogUtil.debug("RDF export (id=" + exportId + ") finished in " + (millis / 1000L) + " sec", exportLogger, LOGGER);

        } catch (Exception e) {
            failed = true;
            SesameUtil.rollback(repoConn);
            LogUtil.debug("RDF export (id=" + exportId + ") failed with error", e, exportLogger, LOGGER);
        } finally {
            SesameUtil.close(repoConn);
        }

        startPostHarvests();

        try {
            getDao().finishRDFExport(exportId, this, failed ? ExportStatus.ERROR : ExportStatus.COMPLETED);
        } catch (DAOException e) {
            LOGGER.error("Failed to finish RDF export record with id = " + exportId, e);
        }
    }

    /**
     * Update export status.
     *
     * @param status
     *            the status
     */
    private void updateExportStatus(ExportStatus status) {
        try {
            getDao().updateExportStatus(exportId, status);
        } catch (DAOException e) {
            LOGGER.error("Failed to update the status of RF export with id = " + exportId, e);
        }
    }

    /**
     * Do run.
     *
     * @param repoConn
     *            the repo conn
     * @throws RepositoryException
     *             the repository exception
     * @throws SQLException
     *             the sQL exception
     * @throws DAOException
     */
    private void doRun(RepositoryConnection repoConn) throws RepositoryException, SQLException, DAOException {

        // Nothing to do here if query or column mappings is empty.
        if (StringUtils.isBlank(queryConf.getQuery()) || queryConf.getColumnMappings().isEmpty()) {
            return;
        }

        Connection sqlConn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            ValueFactory valueFactory = repoConn.getValueFactory();
            prepareValues(valueFactory);

            sqlConn = SesameUtil.getSQLConnection(dbDTO.getName());
            pstmt = sqlConn.prepareStatement(queryConf.getQuery());
            rs = pstmt.executeQuery();

            rowCount = 0;
            while (rs.next()) {
                rowCount++;
                exportRow(rs, rowCount, repoConn, valueFactory);
                if (rowCount % 1000 == 0) {
                    LogUtil.debug(rowCount + " rows exported so far", exportLogger, LOGGER);
                }
            }
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

        indicatorPredicateURI = vf.createURI(Predicates.DAS_INDICATOR);
        String indicatorUri = queryConf.getIndicatorUri();
        if (StringUtils.isNotBlank(indicatorUri)) {
            indicatorValueURI = vf.createURI(indicatorUri);
        }

        String datasetUri = queryConf.getDatasetUri();
        datasetIdentifier = StringUtils.substringAfterLast(datasetUri, "/");
        if (StringUtils.isBlank(datasetIdentifier)) {
            throw new IllegalArgumentException("Unable to extract identifier from this dataset URI: " + datasetUri);
        }
        datasetPredicateURI = vf.createURI(Predicates.DATACUBE_DATA_SET);
        datasetValueURI = vf.createURI(datasetUri);

        graphURI = datasetValueURI;
    }

    /**
     * Sets the predicate ur is.
     *
     * @param vf
     *            the new predicate ur is
     */
    private void setPredicateURIs(ValueFactory vf) {

        Map<String, ObjectProperty> columnMappings = queryConf.getColumnMappings();
        Collection<ObjectProperty> objectProperties = columnMappings.values();
        for (ObjectProperty objectProperty : objectProperties) {
            objectProperty.setPredicateURI(vf);
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
    private void exportRow(ResultSet rs, int rowIndex, RepositoryConnection repoConn, ValueFactory vf) throws SQLException,
            RepositoryException, DAOException {

        if (rowIndex == 1) {
            loadExistingConcepts();
        }

        // Prepare subject URI on the basis of the template in the query configuration.
        String subjectUri = queryConf.getObjectUriTemplate();
        if (StringUtils.isBlank(subjectUri)) {
            throw new IllegalArgumentException("The object URI template in the query configuration must not be blank!");
        }
        subjectUri = StringUtils.replace(subjectUri, "<dataset>", datasetIdentifier);

        // Prepare the map of ObjectDTO to be added to the subject later.
        LinkedHashMap<URI, ArrayList<Value>> valuesByPredicate = new LinkedHashMap<URI, ArrayList<Value>>();

        // Add rdf:type predicate-value.
        addPredicateValue(valuesByPredicate, rdfTypeURI, objectTypeURI);

        // Add the DataCube dataset predicate-value. Assume this point cannot be reached if dataset value is empty.
        addPredicateValue(valuesByPredicate, datasetPredicateURI, datasetValueURI);

        // Add predicate-value pairs for hidden properties.
        if (hiddenProperties != null) {
            for (ObjectHiddenProperty hiddenProperty : hiddenProperties) {
                addPredicateValue(valuesByPredicate, hiddenProperty.getPredicateURI(), hiddenProperty.getValueValue());
            }
        }

        boolean hasIndicatorMapping = false;

        // Loop through the query configuration's column mappings, construct ObjectDTO for each.
        for (Entry<String, ObjectProperty> entry : queryConf.getColumnMappings().entrySet()) {

            String colName = entry.getKey();
            String colValue = rs.getString(colName);
            ObjectProperty property = entry.getValue();
            if (property.getId().equals(INDICATOR)) {
                hasIndicatorMapping = true;
            }

            if (StringUtils.isNotBlank(colValue)) {

                // Replace property place-holders in subject ID
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

        // If there was no column mapping for the indicator, but a fixed indicator URI has been provided then use the latter.
        if (!hasIndicatorMapping && indicatorValueURI != null) {
            addPredicateValue(valuesByPredicate, indicatorPredicateURI, indicatorValueURI);
        }

        // If <indicator> column placeholder not replaced yet, then use the fixed indicator URI if given.
        if (subjectUri.indexOf("<indicator>") != -1) {
            String indicatorCode = StringUtils.substringAfterLast(queryConf.getIndicatorUri(), "/");
            if (StringUtils.isBlank(indicatorCode)) {
                // No fixed indicator URI given either, resort to the default.
                indicatorCode = DEFAULT_INDICATOR_CODE;
            }
            subjectUri = StringUtils.replace(subjectUri, "<indicator>", indicatorCode);
        }

        // If <breakdown> column placeholder not replaced yet, then use the default.
        if (subjectUri.indexOf("<breakdown>") != -1) {
            subjectUri = StringUtils.replace(subjectUri, "<breakdown>", DEFAULT_BREAKDOWN_CODE);
        }

        // Loop over predicate-value pairs and create the triples in the triple store.
        if (!valuesByPredicate.isEmpty()) {

            int tripleCountBefore = tripleCount;
            URI subjectURI = vf.createURI(subjectUri);
            for (Entry<URI, ArrayList<Value>> entry : valuesByPredicate.entrySet()) {

                ArrayList<Value> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    URI predicateURI = entry.getKey();
                    for (Value value : values) {
                        repoConn.add(subjectURI, predicateURI, value, graphURI);
                        graphs.add(graphURI.stringValue());
                        tripleCount++;
                        if (tripleCount % 5000 == 0) {
                            LOGGER.debug(tripleCount + " triples exported so far");
                        }

                        // Time periods should be harvested afterwards.
                        if (Predicates.DAS_TIMEPERIOD.equals(predicateURI.stringValue())) {
                            if (value instanceof URI) {
                                harvestUris.add(value.stringValue());
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
    public static ExportRunner test(StagingDatabaseDTO dbDTO, QueryConfiguration queryConf) throws RepositoryException,
            DAOException, SQLException {

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

        // Nothing to do here if query or column mappings is empty.
        if (StringUtils.isBlank(queryConf.getQuery()) || queryConf.getColumnMappings().isEmpty()) {
            return;
        }

        ResultSet rs = null;
        Connection sqlConn = null;
        PreparedStatement pstmt = null;
        RepositoryConnection repoConn = null;

        try {
            sqlConn = SesameUtil.getSQLConnection(dbDTO.getName());
            repoConn = SesameUtil.getRepositoryConnection();

            pstmt = sqlConn.prepareStatement(queryConf.getQuery());
            rs = pstmt.executeQuery();

            rowCount = 0;
            while (rs.next()) {
                rowCount++;
                testRow(rs, rowCount);
            }
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(pstmt);
            SQLUtil.close(sqlConn);
            SesameUtil.close(repoConn);
        }
    }

    /**
     *
     * @param rs
     * @param rowIndex
     * @throws SQLException
     * @throws DAOException
     */
    private void testRow(ResultSet rs, int rowIndex) throws SQLException, DAOException {

        if (rowIndex == 1) {
            loadExistingConcepts();
        }

        LinkedHashMap<String, String> rowMap = new LinkedHashMap<String, String>();
        for (Entry<String, ObjectProperty> entry : queryConf.getColumnMappings().entrySet()) {

            ObjectProperty property = entry.getValue();
            String colName = entry.getKey();
            String colValue = rs.getString(colName);

            String valueTemplate = property.getValueTemplate();
            String propertyValue = valueTemplate == null ? colValue : StringUtils.replace(valueTemplate, "<value>", colValue);
            recordMissingConcepts(property, colValue, propertyValue);

            if (rowIndex <= MAX_TEST_RESULTS) {
                rowMap.put(colName, colValue);
            }
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
    private void loadExistingConcepts() throws DAOException {

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
    private void startPostHarvests() {

        if (harvestUris.isEmpty()) {
            return;
        }

        HarvestSourceDAO dao = DAOFactory.get().getDao(HarvestSourceDAO.class);
        for (String uri : harvestUris) {
            LOGGER.debug("Going to harvest " + uri);
            startPostHarvest(uri, dao);
        }
    }

    /**
     *
     * @param uri
     * @param dao
     */
    private void startPostHarvest(String uri, HarvestSourceDAO dao) {

        HarvestSourceDTO dto = new HarvestSourceDTO();
        dto.setUrl(StringUtils.substringBefore(uri, "#"));
        dto.setEmails("");
        dto.setIntervalMinutes(0);
        dto.setPrioritySource(false);
        dto.setOwner(null);
        try {
            dao.addSourceIgnoreDuplicate(dto);
            OnDemandHarvester.harvest(dto.getUrl(), CRUser.APPLICATION.getUserName());
        } catch (Exception e) {
            LOGGER.error("Failed to harvest " + uri, e);
            LogUtil.warn("Failed to harvest " + uri, exportLogger, LOGGER);
        }
    }
}
