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
 * Risto Alt
 */
package eionet.cr.web.action;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.ValidationMethod;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;
import eionet.cr.common.Predicates;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.FolderDAO;
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.helpers.CsvImportHelper;
import eionet.cr.dto.ScriptTemplateDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.filestore.FileStore;
import eionet.cr.filestore.ScriptTemplateDaoImpl;
import eionet.cr.util.FolderUtil;
import eionet.cr.web.action.factsheet.FolderActionBean;
import eionet.cr.web.security.CRUser;

/**
 * CSV upload action bean.
 *
 * @author Jaanus Heinlaid
 */
@UrlBinding("/uploadCSV.action")
public class UploadCSVActionBean extends AbstractActionBean {

    /** */
    private static final Logger LOGGER = Logger.getLogger(UploadCSVActionBean.class);

    /** */
    private static final String JSP_PAGE = "/pages/home/uploadCSV.jsp";

    /** */
    private static final String UPLOAD_EVENT = "upload";
    private static final String SAVE_EVENT = "save";

    /** */
    private static final String PARAM_DISPLAY_WIZARD = "displayWizard";

    /** Enum for uploaded files' types. */
    public enum FileType {
        CSV, TSV;
    }

    /** URI of the folder where the file will be uploaded. */
    private String folderUri;

    /** Uploaded file's bean object. */
    private FileBean fileBean;

    /** Uploaded file's type. */
    private FileType fileType;

    /** Uploaded file's name. */
    private String fileName;

    /** The URI that will be assigned to the resource representing the file. */
    private String fileUri;

    /** User can specify rdf:label for the file. */
    private String fileLabel;

    /** Stored file's relative path in the user's file-store. */
    private String relativeFilePath;

    /** The type of objects contained in the file (user-given free text). */
    private String objectsType;

    /** The column (i.e. column title) representing the contained objects' labels. */
    // private String labelColumn;

    /** The columns (i.e. column titles) forming the contained objects' unique identifiers. */
    private List<String> uniqueColumns;

    /** True, when upload is meant for overwriting existing file. */
    private boolean overwrite;

    /** Publisher of uploaded material. */
    private String publisher;

    /** License of uploaded material. */
    private String license;

    /** Attribution. */
    private String attribution;

    /** Source of the uploaded material. */
    private String source;

    /** Form parameter, when true, data linking scripts are added. */
    private boolean addDataLinkingScripts;

    /** Selected scripts/columns data. */
    private List<DataLinkingScript> dataLinkingScripts;

    /** Available scripts. */
    private List<ScriptTemplateDTO> scriptTemplates;

    /** SpreadsheetColumn labels detected in the uploaded file (titles without type and language code). */
    private List<String> columnLabels;

    /**
     * @return
     */
    @DefaultHandler
    public Resolution init() {
        return new ForwardResolution(JSP_PAGE);
    }

    /**
     *
     * @return
     * @throws DAOException
     */
    public Resolution upload() throws DAOException {

        // Prepare resolution.
        ForwardResolution resolution = new ForwardResolution(JSP_PAGE);

        fileName = fileBean.getFileName();

        FolderDAO folderDAO = DAOFactory.get().getDao(FolderDAO.class);
        if (overwrite) {

            // If doing overwrite, load wizard inputs from previous upload
            loadWizardInputsFromPreviousUpload();

            if (folderDAO.fileOrFolderExists(folderUri, StringUtils.replace(fileName, " ", "%20"))) {
                String oldFileUri = folderUri + "/" + StringUtils.replace(fileName, " ", "%20");
                // Delete existing data
                FileStore fileStore = FileStore.getInstance(FolderUtil.getUserDir(folderUri, getUserName()));
                folderDAO.deleteFileOrFolderUris(folderUri, Collections.singletonList(oldFileUri));
                DAOFactory.get().getDao(HarvestSourceDAO.class).removeHarvestSources(Collections.singletonList(oldFileUri));
                fileStore.delete(FolderUtil.extractPathInUserHome(folderUri + "/" + fileName));
            }
        } else {
            if (folderDAO.fileOrFolderExists(folderUri, StringUtils.replace(fileName, " ", "%20"))) {
                addCautionMessage("File or folder with the same name already exists.");
                return new RedirectResolution(UploadCSVActionBean.class).addParameter("folderUri", folderUri);
            }
        }

        try {
            // Save the file into user's file-store.
            long fileSize = fileBean.getSize();
            relativeFilePath = FolderUtil.extractPathInUserHome(folderUri + "/" + fileName);
            // FileStore fileStore = FileStore.getInstance(getUserName());
            FileStore fileStore = FileStore.getInstance(FolderUtil.getUserDir(folderUri, getUserName()));
            fileStore.addByMoving(relativeFilePath, true, fileBean);

            CsvImportHelper helper =
                    new CsvImportHelper(uniqueColumns, fileUri, fileLabel, fileType, objectsType, publisher, license, attribution,
                            source);

            // Store file as new source, but don't harvest it
            helper.insertFileMetadataAndSource(fileSize, getUserName());

            // Add metadata about user folder update
            helper.linkFileToFolder(folderUri, getUserName());

            // Prepare data linkins scripts dropdown
            dataLinkingScripts = new ArrayList<DataLinkingScript>();
            dataLinkingScripts.add(new DataLinkingScript());

            // If not given, the file's label equals the file's name
            if (StringUtils.isEmpty(fileLabel)) {
                fileLabel = fileName;
            }

            // Tell the JSP page that it should display the wizard.
            resolution.addParameter(PARAM_DISPLAY_WIZARD, "");

        } catch (Exception e) {
            LOGGER.error("Error while reading the file: ", e);
            addWarningMessage(e.getMessage());
        }

        return resolution;
    }

    /**
     *
     * @return
     */
    public Resolution save() {

        CSVReader csvReader = null;
        CsvImportHelper helper =
                new CsvImportHelper(uniqueColumns, fileUri, fileLabel, fileType, objectsType, publisher, license, attribution,
                        source);
        try {
            csvReader = helper.createCSVReader(folderUri, relativeFilePath, getUserName(), true);
            helper.extractObjects(csvReader);
            helper.saveWizardInputs();

            // Save data-linking scripts, if any added.
            if (addDataLinkingScripts) {
                try {
                    LOGGER.debug("Saving data-linking scripts for " + fileUri);
                    helper.saveDataLinkingScripts(dataLinkingScripts);
                } catch (DAOException e) {
                    LOGGER.error("Failed to add data linking script", e);
                    addWarningMessage("Failed to add data linking script: " + e.getMessage());
                }
            }

            // Run all post-harvest scripts specific to this source (i.e. the uploaded file).
            // This will run both the data data-linking scripts saved in the previous block, plus any that existed already.
            try {
                LOGGER.debug("Running all source-specific post-harvest scripts of " + fileUri);
                List<String> warnings = helper.runScripts();
                if (warnings.size() > 0) {
                    for (String w : warnings) {
                        addWarningMessage(w);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to run data linking scripts", e);
                addWarningMessage("Failed to run data linking scripts: " + e.getMessage());
            }

        } catch (Exception e) {
            LOGGER.error("Exception while reading uploaded file:", e);
            addWarningMessage(e.toString());
            return new ForwardResolution(JSP_PAGE);
        } finally {
            CsvImportHelper.close(csvReader);
        }

        // If everything went successfully then redirect to the folder items list
        return new RedirectResolution(FolderActionBean.class).addParameter("uri", folderUri);
    }

    /**
     * Form action, that adds aditional input for data linking scripts.
     *
     * @return
     */
    public Resolution addScript() {
        dataLinkingScripts.add(new DataLinkingScript());

        ForwardResolution resolution = new ForwardResolution(JSP_PAGE);
        resolution.addParameter(PARAM_DISPLAY_WIZARD, "");
        return resolution;
    }

    /**
     * Form action, that removes the last input for data linking scripts.
     *
     * @return
     */
    public Resolution removeScript() {
        dataLinkingScripts.remove(dataLinkingScripts.size() - 1);
        ForwardResolution resolution = new ForwardResolution(JSP_PAGE);
        resolution.addParameter(PARAM_DISPLAY_WIZARD, "");
        return resolution;
    }

    /**
     * Actions to be performed before starting any event handling.
     */
    @Before(stages = LifecycleStage.EventHandling)
    public void beforeEventHandling() {

        if (fileName == null && fileBean != null) {
            fileName = fileBean.getFileName();
        }
        fileUri = folderUri + "/" + StringUtils.replace(fileName, " ", "%20");
    }

    /**
     *
     * @throws DAOException
     */
    @ValidationMethod(on = {UPLOAD_EVENT, SAVE_EVENT})
    public void validatePostEvent() throws DAOException {

        // the below validation is relevant only when the event is requested through POST method
        if (!isPostRequest()) {
            return;
        }

        // for all the above POST events, user must be authorized
        String aclPath = FolderUtil.extractAclPath(folderUri);
        boolean actionAllowed = CRUser.hasPermission(aclPath, getUser(), "i", false);

        if (!actionAllowed) {
            addGlobalValidationError("You are not authorised for this operation!");
            return;
        }

        // if upload event, make sure the file bean is not null
        String eventName = getContext().getEventName();
        if (eventName.equals(UPLOAD_EVENT) && fileBean == null) {
            addGlobalValidationError("No file specified!");
        }

        // if insert event, make sure unique columns and object type are not null
        if (eventName.equals(SAVE_EVENT)) {

            if (StringUtils.isBlank(relativeFilePath)) {
                addGlobalValidationError("No file specified!");
            }

            if (StringUtils.isBlank(fileName)) {
                addGlobalValidationError("No file name specified!");
            }

            // File file = FileStore.getInstance(getUserName()).getFile(relativeFilePath);
            File file = FileStore.getInstance(FolderUtil.getUserDir(folderUri, getUserName())).getFile(relativeFilePath);
            if (file == null || !file.exists()) {
                addGlobalValidationError("Could not find stored file!");
            }

            if (StringUtils.isBlank(objectsType)) {
                addGlobalValidationError("No object type specified!");
            }

            if (StringUtils.isBlank(publisher)) {
                addGlobalValidationError("No original publisher specified!");
            }

            if (StringUtils.isBlank(attribution)) {
                addGlobalValidationError("No copyright attribution specified!");
            }

            if (StringUtils.isBlank(source)) {
                addGlobalValidationError("No source specified!");
            }
        }

        // if any validation errors were set above, make sure the right resolution is returned
        if (hasValidationErrors()) {
            ForwardResolution resolution = new ForwardResolution(JSP_PAGE);
            if (eventName.equals(SAVE_EVENT)) {
                resolution.addParameter(PARAM_DISPLAY_WIZARD, "");
            }
            getContext().setSourcePageResolution(resolution);
        }
    }

    /**
     * True, if there is more than one script bean available.
     *
     * @return
     */
    public boolean isRemoveScriptsAvailable() {
        return dataLinkingScripts.size() > 1;
    }

    /**
     *
     * @throws DAOException
     */
    private void loadWizardInputsFromPreviousUpload() throws DAOException {

        // If, for some reason, all inputs already have a value, do nothing and return
        if (!StringUtils.isBlank(objectsType) && !uniqueColumns.isEmpty()) {
            return;
        }

        SubjectDTO fileSubject = DAOFactory.get().getDao(HelperDAO.class).getSubject(fileUri);
        if (fileSubject != null) {

            if (StringUtils.isBlank(fileLabel)) {
                fileLabel = fileSubject.getObjectValue(Predicates.RDFS_LABEL);
            }

            if (StringUtils.isBlank(objectsType)) {
                objectsType = fileSubject.getObjectValue(Predicates.CR_OBJECTS_TYPE);
            }

            if (uniqueColumns == null || uniqueColumns.isEmpty()) {
                Collection<String> coll = fileSubject.getObjectValues(Predicates.CR_OBJECTS_UNIQUE_COLUMN);
                if (coll != null && !coll.isEmpty()) {
                    uniqueColumns = new ArrayList<String>();
                    uniqueColumns.addAll(coll);
                }
            }

            if (StringUtils.isBlank(publisher)) {
                publisher = fileSubject.getObjectValue(Predicates.DCTERMS_PUBLISHER);
            }

            if (StringUtils.isBlank(license)) {
                license = fileSubject.getObjectValue(Predicates.DCTERMS_LICENSE);
                if (StringUtils.isBlank(license)) {
                    license = fileSubject.getObjectValue(Predicates.DCTERMS_RIGHTS);
                }
            }

            if (StringUtils.isBlank(attribution)) {
                attribution = fileSubject.getObjectValue(Predicates.DCTERMS_BIBLIOGRAPHIC_CITATION);
            }

            if (StringUtils.isBlank(source)) {
                source = fileSubject.getObjectValue(Predicates.DCTERMS_SOURCE);
            }
        }
    }

    /**
     * Singleton getter for column labels.
     *
     * @return
     */
    public List<String> getColumnLabels() {
        if (columnLabels == null) {
            try {
                columnLabels = CsvImportHelper.extractColumnLabels(folderUri, relativeFilePath, getUserName(), fileType);
            } catch (Exception e) {
                LOGGER.error("Exception while reading uploaded file:", e);
                addWarningMessage(e.toString());
                return new ArrayList<String>();
            }
        }

        return columnLabels;
    }

    /**
     * @param file
     */
    public void setFileBean(FileBean file) {
        this.fileBean = file;
    }

    /**
     * @return
     */
    public FileType getFileType() {
        return fileType;
    }

    /**
     * @param type
     */
    public void setFileType(FileType type) {
        this.fileType = type;
    }

    /**
     * @return
     */
    public String getObjectsType() {
        return objectsType;
    }

    /**
     * @param objectType
     */
    public void setObjectsType(String objectType) {
        this.objectsType = objectType;
    }

    /**
     * @return
     */
    public List<String> getUniqueColumns() {
        return uniqueColumns;
    }

    /**
     * @param uniqueColumns
     */
    public void setUniqueColumns(List<String> uniqueColumns) {
        this.uniqueColumns = uniqueColumns;
    }

    /**
     * @return
     */
    public String getRelativeFilePath() {
        return relativeFilePath;
    }

    /**
     * @param filePath
     */
    public void setRelativeFilePath(String filePath) {
        this.relativeFilePath = filePath;
    }

    /**
     * @return the uri
     */
    public String getFolderUri() {
        return folderUri;
    }

    /**
     * @param uri
     *            the uri to set
     */
    public void setFolderUri(String uri) {
        this.folderUri = uri;
    }

    /**
     * @return
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName
     *            the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public String getFileLabel() {
        return fileLabel;
    }

    public void setFileLabel(String fileLabel) {
        this.fileLabel = fileLabel;
    }

    /**
     * @return the publisher
     */
    public String getPublisher() {
        return publisher;
    }

    /**
     * @param publisher
     *            the publisher to set
     */
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    /**
     * @return the license
     */
    public String getLicense() {
        return license;
    }

    /**
     * @param license
     *            the license to set
     */
    public void setLicense(String license) {
        this.license = license;
    }

    /**
     * @return the attribution
     */
    public String getAttribution() {
        return attribution;
    }

    /**
     * @param attribution
     *            the attribution to set
     */
    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source
     *            the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    public boolean isAddDataLinkingScripts() {
        return addDataLinkingScripts;
    }

    public void setAddDataLinkingScripts(boolean addDataLinkingScripts) {
        this.addDataLinkingScripts = addDataLinkingScripts;
    }

    /**
     * @return the dataLinkingScripts
     */
    public List<DataLinkingScript> getDataLinkingScripts() {
        return dataLinkingScripts;
    }

    /**
     * @param dataLinkingScripts
     *            the dataLinkingScripts to set
     */
    public void setDataLinkingScripts(List<DataLinkingScript> dataLinkingScripts) {
        this.dataLinkingScripts = dataLinkingScripts;
    }

    /**
     * @return the scriptTemplates
     */
    public List<ScriptTemplateDTO> getScriptTemplates() {
        if (scriptTemplates == null) {
            scriptTemplates = new ScriptTemplateDaoImpl().getScriptTemplates();
        }
        return scriptTemplates;
    }

    /**
     * @param scriptTemplates
     *            the scriptTemplates to set
     */
    public void setScriptTemplates(List<ScriptTemplateDTO> scriptTemplates) {
        this.scriptTemplates = scriptTemplates;
    }

}
