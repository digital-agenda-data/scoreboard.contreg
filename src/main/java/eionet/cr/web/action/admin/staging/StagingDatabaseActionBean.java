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

import java.io.File;
import java.util.List;

import eionet.cr.staging.util.VirtuosoUtil;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.AnnotatedClassActionResolver;
import net.sourceforge.stripes.validation.ValidationMethod;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.StagingDatabaseDAO;
import eionet.cr.dto.StagingDatabaseDTO;
import eionet.cr.dto.StagingDatabaseTableColumnDTO;
import eionet.cr.staging.FileDownloader;
import eionet.cr.staging.imp.ImportRunner;
import eionet.cr.web.action.AbstractActionBean;
import eionet.cr.web.action.admin.AdminWelcomeActionBean;

/**
 * Action bean for operations with a single staging database.
 *
 * @author jaanus
 */
@UrlBinding("/admin/stagingDb.action")
public class StagingDatabaseActionBean extends AbstractActionBean {

    /** */
    private static final String VIEW_STAGING_DATABASE_JSP = "/pages/admin/staging/viewDatabase.jsp";

    /** */
    private static final String EDIT_STAGING_DATABASE_JSP = "/pages/admin/staging/editDatabase.jsp";

    /** */
    private static final String ADD_STAGING_DATABASE_JSP = "/pages/admin/staging/addDatabase.jsp";

    /** Static logger. */
    private static final Logger LOGGER = Logger.getLogger(StagingDatabaseActionBean.class);

    /** */
    private StagingDatabaseDTO database;

    /** */
    private int dbId;
    private String fileName;
    private String dbName;
    private String dbDescription;
    private String defaultQuery;
    private File file;

    /** */
    private StagingDatabaseDTO dbDTO;

    /** */
    private List<StagingDatabaseTableColumnDTO> tablesColumns;

    /** Indicates if the file where the database will be created from is a compressed file. Relevant for "add" method only. */
    private boolean isCompressedFile;

    /**
     * The bean's default event handler method.
     *
     * @return Resolution to go to.
     * @throws DAOException the dAO exception
     */
    @DefaultHandler
    public Resolution view() throws DAOException {

        StagingDatabaseDAO dao = DAOFactory.get().getDao(StagingDatabaseDAO.class);
        if (dbId > 0) {
            dbDTO = dao.getDatabaseById(dbId);
        } else if (!StringUtils.isBlank(dbName)) {
            dbDTO = dao.getDatabaseByName(dbName);
        }

        if (dbDTO == null) {
            addWarningMessage("Found no database by the given id or name!");
        } else {
            tablesColumns = dao.getTablesColumns(dbDTO.getName());
        }

        return new ForwardResolution(VIEW_STAGING_DATABASE_JSP);
    }

    /**
     * Handles edit event.
     *
     * @return the resolution
     * @throws DAOException If database access error happens.
     */
    public Resolution edit() throws DAOException {

        StagingDatabaseDAO dao = DAOFactory.get().getDao(StagingDatabaseDAO.class);
        if (dbId > 0) {
            dbDTO = dao.getDatabaseById(dbId);
        } else if (!StringUtils.isBlank(dbName)) {
            dbDTO = dao.getDatabaseByName(dbName);
        }

        if (dbDTO == null) {
            addWarningMessage("Found no database by the given id or name!");
        } else {
            dbName = dbDTO.getName();
            dbDescription = dbDTO.getDescription();
            defaultQuery = dbDTO.getDefaultQuery();
            tablesColumns = dao.getTablesColumns(dbDTO.getName());
        }

        return new ForwardResolution(EDIT_STAGING_DATABASE_JSP);
    }

    /**
     * Handles the save event.
     *
     * @return the resolution
     * @throws DAOException If database access error happens
     */
    public Resolution save() throws DAOException {

        DAOFactory.get().getDao(StagingDatabaseDAO.class).updateDatabaseMetadata(dbId, dbDescription, defaultQuery);
        addSystemMessage("Metadata saved successfully!");
        return new RedirectResolution(this.getClass(), "edit").addParameter("dbId", dbId);
    }

    /**
     * Handles the "Save & close" event.
     *
     * @return the resolution
     * @throws DAOException If database access error happens.
     */
    public Resolution saveAndClose() throws DAOException {

        DAOFactory.get().getDao(StagingDatabaseDAO.class).updateDatabaseMetadata(dbId, dbDescription, defaultQuery);
        addSystemMessage("Metadata saved successfully!");
        return new RedirectResolution(this.getClass(), "view").addParameter("dbId", dbId);
    }

    /**
     * Handles the Cancel edit.
     *
     * @return the resolution
     * @throws DAOException If database access error happens.
     */
    public Resolution cancelEdit() throws DAOException {

        return new RedirectResolution(this.getClass(), "view").addParameter("dbId", dbId);
    }

    /**
     * Handles the "add" event.
     *
     * @return the resolution
     * @throws DAOException If database access error happens.
     */
    public Resolution add() throws DAOException {

        String method = getContext().getRequest().getMethod();
        LOGGER.debug(method.toUpperCase() + ": going to create staging database from this file: " + file);

        if (method.equalsIgnoreCase("GET")) {
            return new ForwardResolution(ADD_STAGING_DATABASE_JSP);
        }

        LOGGER.debug("dbName: " + dbName);
        LOGGER.debug("dbDescription: " + dbDescription);

        // Create the database record.
        StagingDatabaseDTO dto = new StagingDatabaseDTO();
        dto.setName(dbName);
        dto.setDescription(dbDescription);
        int id = DAOFactory.get().getDao(StagingDatabaseDAO.class).createRecord(dto, getUserName());
        dto.setId(id);
        LOGGER.debug("New staging database record created, id = " + id);

        // Create the database in DBMS and populate it from the uploaded file.
        ImportRunner.start(dto, file);

        addSystemMessage("Database created and import started in the background!");
        return new RedirectResolution(StagingDatabasesActionBean.class);
    }

    /**
     * Sends back to db list.
     *
     * @return the resolution
     */
    public Resolution backToDbList() {
        return new RedirectResolution(StagingDatabasesActionBean.class);
    }

    /**
     *
     * @return
     */
    public Resolution toAvailableFilesList() {
        return new RedirectResolution(AvailableFilesActionBean.class);
    }

    /**
     * Gets the database.
     *
     * @return the database
     */
    public StagingDatabaseDTO getDatabase() {
        return database;
    }

    /**
     * Gets the db name.
     *
     * @return the dbName
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * Sets the db name.
     *
     * @param dbName the dbName to set
     */
    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    /**
     * Gets the db description.
     *
     * @return the dbDescription
     */
    public String getDbDescription() {
        return dbDescription;
    }

    /**
     * Sets the db description.
     *
     * @param dbDescription the dbDescription to set
     */
    public void setDbDescription(String dbDescription) {
        this.dbDescription = dbDescription;
    }

    /**
     * Validate request on "add" event.
     *
     * @throws DAOException the dAO exception
     */
    @ValidationMethod(on = {"add"})
    public void validateAdd() throws DAOException {

        if (StringUtils.isBlank(fileName)) {
            addGlobalValidationError("No file name supplied!");
        } else {
            file = new File(FileDownloader.FILES_DIR, fileName);
            if (!file.exists()) {
                addGlobalValidationError("No such file found in available files: " + fileName);
            }
        }

        // Validations for POST method only.
        if (getContext().getRequest().getMethod().equalsIgnoreCase("POST")) {

            if (StringUtils.isBlank(dbName)) {
                addGlobalValidationError("Database name must not be blank!");
            } else if (!VirtuosoUtil.isLegalDatabaseName(dbName)) {
                addGlobalValidationError("Database name must contain only latin letters, numbers and underscores, and must not start with a number!");
            }
            else if (DAOFactory.get().getDao(StagingDatabaseDAO.class).exists(dbName)) {
                addGlobalValidationError("A database with this name already exists: " + dbName);
            }
        }

        getContext().setSourcePageResolution(new ForwardResolution(ADD_STAGING_DATABASE_JSP));
    }

    /**
     * Validates the the user is authorised for any operations on this action bean. If user not authorised, redirects to the
     * {@link AdminWelcomeActionBean} which displays a proper error message. Will be run on any events, since no specific events
     * specified in the {@link ValidationMethod} annotation.
     */
    @ValidationMethod(priority = 1)
    public void validateUserAuthorised() {

        if (getUser() == null || !getUser().isAdministrator()) {
            addGlobalValidationError("You are not authorized for this operation!");
            getContext().setSourcePageResolution(new RedirectResolution(AdminWelcomeActionBean.class));
        }
    }

    /**
     * Sets the file name.
     *
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Gets the file name.
     *
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets the file size.
     *
     * @return the file size
     */
    public long getFileSize() {
        return file == null ? 0L : file.length();
    }

    /**
     * Gets the suggested db name.
     *
     * @return the suggested db name
     */
    public String getSuggestedDbName() {
        String s = StringUtils.isBlank(fileName) ? "" : StringUtils.substringBefore(fileName, ".");
        return VirtuosoUtil.sanitizedDatabaseName(s);
    }

    /**
     * Gets the db dto.
     *
     * @return the dbDTO
     */
    public StagingDatabaseDTO getDbDTO() {
        return dbDTO;
    }

    /**
     * Sets the db id.
     *
     * @param dbId the dbId to set
     */
    public void setDbId(int dbId) {
        this.dbId = dbId;
    }

    /**
     * Gets the staging databases action bean url binding.
     *
     * @return the staging databases action bean url binding
     */
    public String getStagingDatabasesActionBeanUrlBinding() {
        AnnotatedClassActionResolver resolver = new AnnotatedClassActionResolver();
        return resolver.getUrlBinding(StagingDatabasesActionBean.class);
    }

    /**
     * Gets the staging databases action bean class.
     *
     * @return the staging databases action bean class
     */
    public Class getStagingDatabasesActionBeanClass() {
        return StagingDatabasesActionBean.class;
    }

    /**
     * Gets the rdf export wizard action bean class.
     *
     * @return the rdf export wizard action bean class
     */
    public Class getRdfExportWizardActionBeanClass() {
        return RDFExportWizardActionBean.class;
    }

    /**
     * Gets the rdf exports action bean class.
     *
     * @return the rdf exports action bean class
     */
    public Class getRdfExportsActionBeanClass() {
        return RDFExportsActionBean.class;
    }

    /**
     * Gets the tables columns.
     *
     * @return the tablesColumns
     */
    public List<StagingDatabaseTableColumnDTO> getTablesColumns() {
        return tablesColumns;
    }

    /**
     * Gets the default query.
     *
     * @return the defaultQuery
     */
    public String getDefaultQuery() {
        return defaultQuery;
    }

    /**
     * Sets the default query.
     *
     * @param defaultQuery the defaultQuery to set
     */
    public void setDefaultQuery(String defaultQuery) {
        this.defaultQuery = defaultQuery;
    }
}
