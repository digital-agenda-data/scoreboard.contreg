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

package eionet.cr.web.action.admin.staging;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.repository.RepositoryException;

import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.dao.StagingDatabaseDAO;
import eionet.cr.dto.SearchResultDTO;
import eionet.cr.dto.StagingDatabaseDTO;
import eionet.cr.dto.StagingDatabaseTableColumnDTO;
import eionet.cr.staging.exp.ExportRunner;
import eionet.cr.staging.exp.ObjectProperty;
import eionet.cr.staging.exp.ObjectType;
import eionet.cr.staging.exp.ObjectTypes;
import eionet.cr.staging.exp.QueryConfiguration;
import eionet.cr.util.LinkedCaseInsensitiveMap;
import eionet.cr.util.Pair;
import eionet.cr.web.action.AbstractActionBean;
import eionet.cr.web.action.admin.AdminWelcomeActionBean;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.SessionScope;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.ValidationMethod;

/**
 * Action bean that serves the "wizard" that helps user to run a RDF export query from a selected staging database. <b>Note that
 * because of its "wizardly" nature, this bean is kept in {@link SessionScope}, hence some add patterns below.</b>
 *
 * @author jaanus
 */
@SessionScope
@UrlBinding("/admin/exportRDF.action")
public class RDFExportWizardActionBean extends AbstractActionBean {

    /** */
    private static final Logger LOGGER = Logger.getLogger(RDFExportWizardActionBean.class);

    /** */
    private static final SimpleDateFormat DEFAULT_EXPORT_NAME_DATE_FORMAT = new SimpleDateFormat("yyMMdd_HHmmss");

    /**  */
    private static final String COLUMN_PROPERTY_PARAM_SUFFIX = ".property";

    /** */
    private static final String STEP1_JSP = "/pages/admin/staging/exportRDF1.jsp";

    /** */
    private static final String STEP2_JSP = "/pages/admin/staging/exportRDF2.jsp";

    /** */
    private static final int VIRTUOSO_SQL_ERROR = -8;

    /** */
    private String dbName;

    /** */
    private QueryConfiguration queryConf;

    /** */
    private String exportName;

    /** */
    private String prevDbName;

    /** */
    private String prevObjectTypeUri;

    /** */
    private Set<String> prevColumnNames;

    /** */
    private List<StagingDatabaseTableColumnDTO> tablesColumns;

    /** */
    private StagingDatabaseDTO dbDTO;

    /** */
    private List<Pair<String, String>> indicators;

    /** */
    private List<Pair<String, String>> datasets;

    /** Fields populated from the "create new dataset" form. */
    private String newDatasetIdentifier;
    private String newDatasetTitle;
    private String newDatasetDescription;

    /** */
    private ExportRunner testRun;

    /**
     * Event handler for the wizard's first step.
     *
     * @return the resolution
     * @throws DAOException
     *             the dAO exception
     */
    @DefaultHandler
    public Resolution step1() throws DAOException {

        // Handle GET request, just forward to the JSP and that's all.
        if (getContext().getRequest().getMethod().equalsIgnoreCase("GET")) {

            // If this event is GET-requested with a database name, nullify the query configuration.
            if (!dbName.equals(prevDbName)) {
                dbNameChanged();
            }
            prevDbName = dbName;

            return new ForwardResolution(STEP1_JSP);
        }

        // Handle POST request.
        try {
            // Compile the query on the database side, get the names of columns selected by the query.
            Set<String> columnNames = DAOFactory.get().getDao(StagingDatabaseDAO.class).prepareStatement(queryConf.getQuery(), dbName);

            // If column names changed, make corrections in the mappings map then too.
            if (!equalsCaseInsensitive(columnNames, prevColumnNames)) {
                selectedColumnsChanged(columnNames);
            }
            prevColumnNames = columnNames;

            // If object type changed, change the templates of dataset ID and objects ID
            if (!queryConf.getObjectTypeUri().equals(prevObjectTypeUri)) {
                objectTypeChanged();
            }
            prevObjectTypeUri = queryConf.getObjectTypeUri();

            // Finally, return resolution.
            return new ForwardResolution(STEP2_JSP);
        } catch (DAOException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause.getClass().getSimpleName().startsWith("VirtuosoException") && cause instanceof SQLException) {
                SQLException sqlE = (SQLException) cause;
                if (sqlE.getErrorCode() == VIRTUOSO_SQL_ERROR) {
                    addGlobalValidationError("An SQL error occurred:\n" + sqlE.getMessage());
                } else {
                    addGlobalValidationError("A database error occurred:\n" + sqlE.getMessage());
                }
                return new ForwardResolution(STEP1_JSP);
            } else {
                throw e;
            }
        }
    }

    /**
     * GET request to the wizard's 2nd step.
     *
     * @return
     * @throws DAOException
     */
    public Resolution step2() throws DAOException {

        // Just forward to the JSP and that's all.
        return new ForwardResolution(STEP2_JSP);
    }

    /**
     *
     * @return the resolution
     * @throws DAOException
     *             the dAO exception
     */
    public Resolution run() {

        if (queryConf == null) {
            queryConf = new QueryConfiguration();
        }
        queryConf.setClearDataset(StringUtils.equals(getContext().getRequestParameter("clearDataset"), Boolean.TRUE.toString()));

        try {
            StagingDatabaseDTO dto = DAOFactory.get().getDao(StagingDatabaseDAO.class).getDatabaseByName(dbName);
            ExportRunner.start(dto, exportName, getUserName(), queryConf);
        } catch (DAOException e) {
            LOGGER.error("Export start failed with technical error", e);
            addWarningMessage("Export start failed with technical error: " + e.getMessage());
            return new ForwardResolution(STEP2_JSP);
        }

        addSystemMessage("RDF export successfully started! " + "Use operations menu to list ongoing and finished RDF exports from this database.");
        return new RedirectResolution(StagingDatabaseActionBean.class).addParameter("dbName", dbName);
    }

    /**
     *
     * @return
     */
    public Resolution test() {

        try {
            StagingDatabaseDTO dto = DAOFactory.get().getDao(StagingDatabaseDAO.class).getDatabaseByName(dbName);
            testRun = ExportRunner.test(dto, queryConf);
            int rowCount = testRun.getRowCount();
            if (rowCount > 0) {
                addSystemMessage("Test run successful, see results below!");
            } else {
                addSystemMessage("The test returned no results!");
            }
        } catch (RepositoryException e) {
            LOGGER.error("A repository access error occurred", e);
            addGlobalValidationError("A repository access error occurred:\n" + e.getMessage());
        } catch (DAOException e) {
            LOGGER.error("A query execution error occurred", e);
            addGlobalValidationError("A query execution error occurred:\n" + e.getMessage());
        } catch (SQLException e) {
            LOGGER.error("A query execution error occurred", e);
            addGlobalValidationError("A query execution error occurred:\n" + e.getMessage());
        }

        return new ForwardResolution(STEP2_JSP);
    }

    /**
     * Back to step1.
     *
     * @return the resolution
     */
    public Resolution backToStep1() {
        return new ForwardResolution(STEP1_JSP);
    }

    /**
     * Back to step2.
     *
     * @return the resolution
     */
    public Resolution backToStep2() {
        return new ForwardResolution(STEP2_JSP);
    }

    /**
     * Cancel.
     *
     * @return the resolution
     */
    public Resolution cancel() {
        return new RedirectResolution(StagingDatabaseActionBean.class).addParameter("dbName", dbName);
    }

    /**
     *
     * @return
     */
    public Resolution createNewDataset() {

        ScoreboardSparqlDAO dao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
        try {
            String datasetUri = dao.createDataset(newDatasetIdentifier, newDatasetTitle, newDatasetDescription);
            addSystemMessage("A new dataset with identifier \"" + newDatasetIdentifier + "\" successfully created!");
            if (queryConf == null) {
                queryConf = new QueryConfiguration();
            }
            queryConf.setDatasetUri(datasetUri);
        } catch (DAOException e) {
            LOGGER.error("Dataset creation failed with technical error", e);
            addWarningMessage("Dataset creation failed with technical error: " + e.getMessage());
        }

        return new ForwardResolution(STEP2_JSP);
    }

    /**
     *
     * @throws DAOException
     */
    @ValidationMethod(on = {"createNewDataset"})
    public void validateCreateNewDataset() throws DAOException {

        if (getUser() == null || !getUser().isAdministrator()) {
            addGlobalValidationError("You are not authorized for this operation!");
            getContext().setSourcePageResolution(new RedirectResolution(getClass()));
            return;
        }

        if (StringUtils.isBlank(newDatasetIdentifier)) {
            addGlobalValidationError("The identifier is mandatory!");
        } else {
            String s = newDatasetIdentifier.replaceAll("[^a-zA-Z0-9-._]+", "");
            if (!s.equals(newDatasetIdentifier)) {
                addGlobalValidationError("Only digits, latin letters, underscores and dashes allowed in the identifier!");
            } else {
                boolean datasetExists = DAOFactory.get().getDao(ScoreboardSparqlDAO.class).datasetExists(newDatasetIdentifier);
                if (datasetExists) {
                    addGlobalValidationError("A dataset already exists by this identifier: " + newDatasetIdentifier);
                }
            }
        }

        if (StringUtils.isBlank(newDatasetTitle)) {
            addGlobalValidationError("The title is mandatory!");
        }

        getContext().setSourcePageResolution(new ForwardResolution(STEP2_JSP));
    }

    /**
     * Validate the GET request to step 2 or the POST request submitted from step2 page (i.e. the Run event)
     */
    @ValidationMethod(on = {"step2", "run", "test"})
    public void validateStep2AndRun() {

        boolean hasDatasetMapping = false;
        // Ensure that all selected columns have been mapped to a property.
        Map<String, ObjectProperty> colMappings = queryConf == null ? null : queryConf.getColumnMappings();
        if (colMappings == null || colMappings.isEmpty()) {
            addGlobalValidationError("Found no column mappings!");
        } else {

            for (Entry<String, ObjectProperty> entry : colMappings.entrySet()) {

                String colName = entry.getKey();
                ObjectProperty property = entry.getValue();
                if (property == null) {
                    addGlobalValidationError("Missing property selection for this column: " + colName);
                } else {
                    if (property.getId().equalsIgnoreCase("dataSet")) {
                        hasDatasetMapping = true;
                    }
                }
            }

            // Ensure that all required properties have a mapping.
            Collection<ObjectProperty> mappedProperties = colMappings.values();
            HashSet<ObjectProperty> requiredProperties = getObjectType().getRequiredProperties();
            for (ObjectProperty requiredProperty : requiredProperties) {
                if (!mappedProperties.contains(requiredProperty)) {
                    addGlobalValidationError("Missing a column mapping for this required property: " + requiredProperty.getLabel());
                }
            }
        }

        try {
            // Ensure that dataset is specified, either in the column mappings or fixed value (but not both!).
            queryConf.validateDatasetPresence();
        } catch (IllegalArgumentException e) {
            addGlobalValidationError(e.getMessage());
        }

        getContext().setSourcePageResolution(new ForwardResolution(STEP2_JSP));
    }

    /**
     * Validate step1.
     *
     * @throws DAOException
     *             the dAO exception
     */
    @ValidationMethod(on = {"step1"})
    public void validateStep1() throws DAOException {

        StagingDatabaseDAO dao = DAOFactory.get().getDao(StagingDatabaseDAO.class);

        // Validate the database name.
        if (StringUtils.isBlank(dbName)) {
            addGlobalValidationError("Database name must be given!");
        }

        if (getContext().getValidationErrors().isEmpty()) {

            // More validations if POST method.
            if (getContext().getRequest().getMethod().equalsIgnoreCase("POST")) {

                String query = queryConf == null ? null : queryConf.getQuery();
                if (StringUtils.isBlank(query)) {
                    addGlobalValidationError("The query must not be blank!");
                }

                String objectTypeUri = queryConf == null ? null : queryConf.getObjectTypeUri();
                if (StringUtils.isBlank(objectTypeUri)) {
                    addGlobalValidationError("The type of objects must not be blank!");
                }

                if (StringUtils.isBlank(exportName)) {
                    addGlobalValidationError("The name must not be blank!");
                } else if (dbDTO != null && dao.existsRDFExport(dbDTO.getId(), exportName)) {
                    addGlobalValidationError("An RDF export by this name for this database has already been run!");
                }
            }
        }

        // Set source page resolution to which the user will be returned.
        getContext().setSourcePageResolution(new ForwardResolution(STEP1_JSP));
    }

    /**
     * Special handling before any binding or validation takes place.
     */
    @Before(stages = {LifecycleStage.BindingAndValidation})
    public void beforeBindingAndValidation() {

        String eventName = getContext().getEventName();
        if (Arrays.asList("backToStep1", "step2", "run", "test").contains(eventName)) {

            ObjectType objectType = getObjectType();
            if (objectType != null) {

                Map<String, ObjectProperty> colMappings = queryConf == null ? null : queryConf.getColumnMappings();
                if (colMappings != null && !colMappings.isEmpty()) {

                    HttpServletRequest request = getContext().getRequest();
                    LinkedHashSet<String> keySet = new LinkedHashSet<String>(colMappings.keySet());
                    for (String colName : keySet) {

                        String propertyPredicate = request.getParameter(colName + COLUMN_PROPERTY_PARAM_SUFFIX);
                        if (!StringUtils.isBlank(propertyPredicate)) {
                            colMappings.put(colName, objectType.getPropertyByPredicate(propertyPredicate));
                        } else {
                            colMappings.put(colName, null);
                        }
                    }
                }
            }
        }
    }

    /**
     * Validate user authorised.
     */
    @ValidationMethod(priority = 1)
    public void validateUserAuthorised() {

        if (getUser() == null || !getUser().isAdministrator()) {
            addGlobalValidationError("You are not authorized for this operation!");
            getContext().setSourcePageResolution(new RedirectResolution(AdminWelcomeActionBean.class));
        }
    }

    /**
     * Before any event is handled.
     */
    @Before
    public void beforeEventHandling() {

        datasets = null;
        indicators = null;
    }

    /**
     * To be called when database name has changed.
     *
     * @throws DAOException
     */
    private void dbNameChanged() throws DAOException {

        dbDTO = DAOFactory.get().getDao(StagingDatabaseDAO.class).getDatabaseByName(dbName);
        if (dbDTO == null) {
            addGlobalValidationError("Found no staging database by this name: " + dbName);
        }

        this.queryConf = null;
        this.exportName = null;
        this.tablesColumns = null;
        this.prevColumnNames = null;
        this.prevObjectTypeUri = null;
    }

    /**
     * To be called when object type changed.
     */
    private void objectTypeChanged() {

        ObjectType objectType = getObjectType();
        if (objectType != null) {
            queryConf.setObjectUriTemplate(objectType.getObjectUriTemplate());
        }
    }

    /**
     * To be called when selected columns changed.
     *
     * @param selectedColumns
     *            the selected columns
     */
    private void selectedColumnsChanged(Set<String> selectedColumns) {

        if (queryConf == null) {
            queryConf = new QueryConfiguration();
        }

        Map<String, ObjectProperty> curMappings = queryConf.getColumnMappings();
        if (selectedColumns == null || selectedColumns.isEmpty()) {
            curMappings.clear();
            return;
        }

        ObjectType objectType = getObjectType();
        LinkedCaseInsensitiveMap<ObjectProperty> newMappings = new LinkedCaseInsensitiveMap<ObjectProperty>();
        for (String column : selectedColumns) {
            if (curMappings.containsKey(column)) {
                ObjectProperty curProperty = curMappings.get(column);
                if (curProperty == null) {
                    newMappings.put(column, null);
                } else if (!objectType.hasThisProperty(curProperty)) {
                    newMappings.put(column, objectType.getDefaultProperty(column));
                } else {
                    newMappings.put(column, curProperty);
                }
            } else {
                newMappings.put(column, objectType.getDefaultProperty(column));
            }
        }

        queryConf.setColumnMappings(newMappings);
    }

    /**
     * Return true if the two given string sets are equal case insensitively.
     *
     * @param set1
     *            the set1
     * @param set2
     *            the set2
     * @return true, if equal, otherwise false
     */
    private boolean equalsCaseInsensitive(Set<String> set1, Set<String> set2) {

        if (set1 == null && set2 == null) {
            return true;
        } else if (set1 == null && set2 != null) {
            return false;
        } else if (set1 != null && set2 == null) {
            return false;
        } else if (set1 == set2) {
            return true;
        } else if (set1.size() != set2.size()) {
            return false;
        } else {
            for (String str1 : set1) {
                boolean hasMatch = false;
                for (String str2 : set2) {
                    if (StringUtils.equalsIgnoreCase(str1, str2)) {
                        hasMatch = true;
                        break;
                    }
                }

                if (!hasMatch) {
                    return false;
                }
            }

            return true;
        }
    }

    /**
     * @return the dbName
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * @param dbName
     *            the dbName to set
     */
    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    /**
     * @return the queryConf
     */
    public QueryConfiguration getQueryConf() {
        return queryConf;
    }

    /**
     * @param queryConf
     *            the queryConf to set
     */
    public void setQueryConf(QueryConfiguration queryConf) {
        this.queryConf = queryConf;
    }

    /**
     * Gets the database action bean class.
     *
     * @return the database action bean class
     */
    public Class getDatabaseActionBeanClass() {
        return StagingDatabaseActionBean.class;
    }

    /**
     * Gets the possible object types.
     *
     * @return the object types
     */
    public Collection<ObjectType> getObjectTypes() {
        return ObjectTypes.getMap().values();
    }

    /**
     * Returns object type for the currently selected object type URI.
     *
     * @return the object type
     */
    public ObjectType getObjectType() {

        String objTypeUri = queryConf == null ? null : queryConf.getObjectTypeUri();
        if (StringUtils.isNotBlank(objTypeUri)) {
            return ObjectTypes.getByUri(objTypeUri);
        }

        return null;
    }

    /**
     * Returns properties for the object type of the currently selected object type URI.
     *
     * @return The properties.
     */
    public List<ObjectProperty> getTypeProperties() {

        ObjectType objectType = getObjectType();
        if (objectType != null) {
            return objectType.getProperties();
        }

        return null;
    }

    /**
     * @return the exportName
     */
    public String getExportName() {
        return exportName;
    }

    /**
     * @param exportName
     *            the exportName to set
     */
    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    /**
     * Validate column place-holders in the given template, using the given set of column names.
     *
     * @param template
     *            the template
     * @param colNames
     *            the col names
     * @return true, if successful
     */
    private boolean validateColumnPlaceholders(String template, Set<String> colNames) {

        if (colNames == null || colNames.isEmpty()) {
            return true;
        }

        int length = template.length();
        for (int i = 0; i < length; i++) {

            char c = template.charAt(i);
            if (c == '<') {
                int j = template.indexOf('>', i);
                if (j != -1) {
                    String colName = template.substring(i + 1, j);
                    if (!colNames.contains(colName)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Returns list of {@link StagingDatabaseTableColumnDTO} for the currently selected database.
     *
     * @return the list of {@link StagingDatabaseTableColumnDTO}
     * @throws DAOException
     *             when a database error happens
     */
    public List<StagingDatabaseTableColumnDTO> getTablesColumns() throws DAOException {

        if (tablesColumns == null && StringUtils.isNotBlank(dbName)) {
            tablesColumns = DAOFactory.get().getDao(StagingDatabaseDAO.class).getTablesColumns(dbName);
        }
        return tablesColumns;
    }

    /**
     * Get default export name if user hasn't supplied one.
     *
     * @return The default export name
     */
    public String getDefaultExportName() {

        return dbName + "_" + getUserName() + "_" + DEFAULT_EXPORT_NAME_DATE_FORMAT.format(new Date());
    }

    /**
     * Gets the DTO for currently selected database.
     *
     * @return the DTO
     */
    public StagingDatabaseDTO getDbDTO() {
        return dbDTO;
    }

    /**
     * @return the indicators
     * @throws DAOException
     */
    public List<Pair<String, String>> getIndicators() throws DAOException {

        if (indicators == null) {

            String[] labels = {Predicates.SKOS_PREF_LABEL, Predicates.SKOS_ALT_LABEL, Predicates.RDFS_LABEL, Predicates.SKOS_NOTATION};
            HelperDAO dao = DAOFactory.get().getDao(HelperDAO.class);
            SearchResultDTO<Pair<String, String>> searchResult = dao.getUriLabels(Subjects.DAS_INDICATOR, null, null, labels);
            if (searchResult != null) {
                indicators = searchResult.getItems();
            }
        }

        return indicators;
    }

    /**
     * @return the testRun
     */
    public ExportRunner getTestRun() {
        return testRun;
    }

    /**
     *
     * @return
     */
    public int getMaxTestResults() {
        return ExportRunner.MAX_TEST_RESULTS;
    }

    /**
     * Lazy getter for the datasets.
     *
     * @return the datasets
     * @throws DAOException
     */
    public List<Pair<String, String>> getDatasets() throws DAOException {

        if (datasets == null) {
            String[] labels = {Predicates.DCTERMS_TITLE, Predicates.RDFS_LABEL, Predicates.FOAF_NAME};
            HelperDAO dao = DAOFactory.get().getDao(HelperDAO.class);
            SearchResultDTO<Pair<String, String>> searchResult = dao.getUriLabels(Subjects.DATACUBE_DATA_SET, null, null, labels);
            if (searchResult != null) {
                datasets = searchResult.getItems();
            }
        }
        return datasets;
    }

    /**
     * @param newDatasetIdentifier
     *            the newDatasetIdentifier to set
     */
    public void setNewDatasetIdentifier(String newDatasetIdentifier) {
        this.newDatasetIdentifier = newDatasetIdentifier;
    }

    /**
     * @param newDatasetTitle
     *            the newDatasetTitle to set
     */
    public void setNewDatasetTitle(String newDatasetTitle) {
        this.newDatasetTitle = newDatasetTitle;
    }

    /**
     * @param newDatasetDescription
     *            the newDatasetDescription to set
     */
    public void setNewDatasetDescription(String newDatasetDescription) {
        this.newDatasetDescription = newDatasetDescription;
    }
}
