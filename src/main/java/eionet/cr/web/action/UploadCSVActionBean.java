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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import au.com.bytecode.opencsv.CSVReader;
import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.FolderDAO;
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.PostHarvestScriptDAO;
import eionet.cr.dao.helpers.CsvImportHelper;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.dto.ObjectDTO;
import eionet.cr.dto.PostHarvestScriptDTO;
import eionet.cr.dto.ScriptTemplateDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.filestore.FileStore;
import eionet.cr.filestore.ScriptTemplateDaoImpl;
import eionet.cr.util.FolderUtil;
import eionet.cr.util.Util;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.web.action.admin.postHarvest.PostHarvestScriptParser;
import eionet.cr.web.action.factsheet.FolderActionBean;
import eionet.cr.web.security.CRUser;
import eionet.cr.web.util.CharsetToolkit;

/**
 *
 * @author Jaanus Heinlaid
 *
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

    /** Columns detected in the uploaded file (it's the titles of the columns). */
    private List<String> columns;

    /** Column labels detected in the uploaded file (titles without type and language code). */
    private List<String> columnLabels;

    /** The type of objects contained in the file (user-given free text). */
    private String objectsType;

    /** The column (i.e. column title) representing the contained objects' labels. */
    private String labelColumn;

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

            // Store file as new source, but don't harvest it
            createFileMetadataAndSource(fileSize);

            // Add metadata about user folder update
            linkFileToFolder();

            // Pre-load wizard input values if this has been uploaded already before (so a re-upload now)
            preloadWizardInputs();

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
        try {
            csvReader = createCSVReader(true);
            extractObjects(csvReader);
            saveWizardInputs();
            if (addDataLinkingScripts) {
                saveDataLinkingScripts();
                runScripts();
            }
        } catch (Exception e) {
            LOGGER.error("Exception while reading uploaded file:", e);
            addWarningMessage(e.toString());
            return new ForwardResolution(JSP_PAGE);
        } finally {
            close(csvReader);
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

            if (uniqueColumns == null || uniqueColumns.size() == 0) {
                addGlobalValidationError("No unique column selected!");
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
     * @param csvReader
     * @throws IOException
     * @throws DAOException
     * @throws RepositoryException
     */
    @Deprecated
    public void extractObjects(CSVReader csvReader) throws IOException, DAOException, RepositoryException {

        // Set columns and columnLabels by reading the first line.
        String[] columnsArray = csvReader.readNext();
        if (columnsArray == null || columnsArray.length == 0) {
            columns = new ArrayList<String>();
            columnLabels = new ArrayList<String>();
        } else {
            // We do trimming, because CSV Reader doesn't do it
            columns = Arrays.asList(trimAll(columnsArray));
            if (columns != null && columns.size() > 0) {
                columnLabels = new ArrayList<String>();
                int emptyColCount = 1;
                for (String col : columns) {
                    if (StringUtils.isEmpty(col)) {
                        col = CsvImportHelper.EMPTY_COLUMN + emptyColCount++;
                    }
                    col = StringUtils.substringBefore(col, ":");
                    col = StringUtils.substringBefore(col, "@");
                    columnLabels.add(col.trim());
                }
            }
        }

        // Read the contained objects by reading the rest of lines.
        String[] line = null;
        String objectsTypeUri = fileUri + "/" + objectsType;
        HelperDAO helperDao = DAOFactory.get().getDao(HelperDAO.class);

        while ((line = csvReader.readNext()) != null) {
            SubjectDTO subject = extractObject(line, objectsTypeUri);
            helperDao.addTriples(subject);
        }

        // Construct a SPARQL query and store it as a property
        StringBuilder query = new StringBuilder();
        query.append("PREFIX tableFile: <" + fileUri + "#>\n\n");
        query.append("SELECT * FROM <").append(fileUri).append("> WHERE { \n");
        for (String column : columnLabels) {
            column = column.replace(" ", "_");
            String columnUri = "tableFile:" + column;
            query.append(" ?").append(objectsType).append(" ").append(columnUri).append(" ?").append(column).append(" . \n");
        }
        query.append("}");

        HarvestSourceDAO dao = DAOFactory.get().getDao(HarvestSourceDAO.class);
        dao.insertUpdateSourceMetadata(fileUri, Predicates.CR_SPARQL_QUERY, ObjectDTO.createLiteral(query.toString()));

        // Finally, make sure that the file has the correct number of harvested statements in its predicates.
        DAOFactory.get().getDao(HarvestSourceDAO.class).updateHarvestedStatementsTriple(fileUri);
    }

    @Deprecated
    public String[] trimAll(String[] strings) {

        if (strings != null) {
            for (int i = 0; i < strings.length; i++) {
                strings[i] = strings[i].trim();
            }
        }

        return strings;
    }

    /**
     * @param line
     * @param objectsTypeUri
     * @return
     */
    @Deprecated
    private SubjectDTO extractObject(String[] line, String objectsTypeUri) {

        // Construct subject URI and DTO object.
        String subjectUri = fileUri + "/" + extractObjectId(line);
        SubjectDTO subject = new SubjectDTO(subjectUri, false);

        // Add rdf:type to DTO.
        ObjectDTO typeObject = new ObjectDTO(objectsTypeUri, false);
        typeObject.setSourceUri(fileUri);
        subject.addObject(Predicates.RDF_TYPE, typeObject);

        // Add all other values.
        for (int i = 0; i < columns.size(); i++) {

            // If current columns index out of bounds for some reason, then break.
            if (i >= line.length) {
                break;
            }

            // Get column title, skip this column if it's the label column, otherwise replace spaces.
            String column = columns.get(i);

            // Extract column type and language code
            String type = StringUtils.substringAfter(column, ":");
            if (type != null && type.length() == 0) {
                type = null;
            }
            String lang = StringUtils.substringAfter(column, "@");
            if (lang != null && lang.length() == 0) {
                lang = null;
            }

            // Get column label
            column = columnLabels.get(i);
            column = column.replace(" ", "_");

            // Create ObjectDTO representing the given column's value on this line
            ObjectDTO objectDTO = createValueObject(column, line[i], type, lang);
            objectDTO.setSourceUri(fileUri);

            // Add ObjectDTO to the subject.
            String predicateUri = fileUri + "#" + column;
            subject.addObject(predicateUri, objectDTO);

            // If marked as label column, add label property as well
            if (column.equals(labelColumn)) {
                subject.addObject(Predicates.RDFS_LABEL, objectDTO);
            }
        }

        return subject;
    }

    /**
     * @param column
     * @param value
     * @return
     */
    @Deprecated
    private ObjectDTO createValueObject(String column, String value, String type, String lang) {

        HashMap<String, URI> types = new HashMap<String, URI>();
        types.put("url", null);
        types.put("uri", null);
        types.put("date", XMLSchema.DATE);
        types.put("datetime", XMLSchema.DATETIME);
        types.put("boolean", XMLSchema.BOOLEAN);
        types.put("integer", XMLSchema.INTEGER);
        types.put("int", XMLSchema.INT);
        types.put("long", XMLSchema.LONG);
        types.put("double", XMLSchema.DOUBLE);
        types.put("decimal", XMLSchema.DECIMAL);
        types.put("float", XMLSchema.FLOAT);

        // If type is not defined, but column name matches one of the types, then use column name as datatype
        if (type == null) {
            if (types.keySet().contains(column.toLowerCase())) {
                type = column.toLowerCase();
            }
        }

        ObjectDTO objectDTO = null;
        if (!StringUtils.isBlank(type)) {
            if (type.equalsIgnoreCase("url") || type.equalsIgnoreCase("uri")) {
                objectDTO = new ObjectDTO(value, lang, false, false, null);
            } else if (types.keySet().contains(type.toLowerCase())) {
                if (type.equalsIgnoreCase("boolean")) {
                    value = value.equalsIgnoreCase("true") ? "true" : "false";
                }
                URI datatype = types.get(type.toLowerCase());
                objectDTO = new ObjectDTO(value, lang, true, false, datatype);
            } else if (type.equalsIgnoreCase("number")) {
                try {
                    Integer.parseInt(value);
                    objectDTO = new ObjectDTO(value, lang, true, false, XMLSchema.INTEGER);
                } catch (NumberFormatException nfe1) {
                    try {
                        Long.parseLong(value);
                        objectDTO = new ObjectDTO(value, lang, true, false, XMLSchema.LONG);
                    } catch (NumberFormatException nfe2) {
                        try {
                            Double.parseDouble(value);
                            objectDTO = new ObjectDTO(value, lang, true, false, XMLSchema.DOUBLE);
                        } catch (NumberFormatException nfe3) {
                            // No need to throw or log it.
                        }
                    }
                }
            }
        }

        return objectDTO == null ? new ObjectDTO(value, lang, true, false, null) : objectDTO;
    }

    /**
     *
     * @param line
     * @return
     */
    @Deprecated
    public String extractObjectId(String[] line) {

        StringBuilder buf = new StringBuilder();
        if (uniqueColumns != null && !uniqueColumns.isEmpty()) {

            for (String uniqueCol : uniqueColumns) {
                int colIndex = columnLabels.indexOf(uniqueCol);
                if (colIndex >= 0 && colIndex < line.length && !StringUtils.isBlank(line[colIndex])) {
                    if (buf.length() > 0) {
                        buf.append("_");
                    }
                    buf.append(line[colIndex]);
                }
            }
        }

        return buf.length() == 0 ? UUID.randomUUID().toString() : buf.toString();
    }

    /**
     *
     * @throws DAOException
     */
    private void preloadWizardInputs() throws DAOException {

        // If, for some reason, all inputs already have a value, do nothing and return
        if (!StringUtils.isBlank(labelColumn) && !StringUtils.isBlank(objectsType) && !uniqueColumns.isEmpty()) {
            return;
        }

        dataLinkingScripts = new ArrayList<DataLinkingScript>();
        dataLinkingScripts.add(new DataLinkingScript());

        SubjectDTO fileSubject = DAOFactory.get().getDao(HelperDAO.class).getSubject(fileUri);
        if (fileSubject != null) {

            if (StringUtils.isBlank(labelColumn)) {
                labelColumn = fileSubject.getObjectValue(Predicates.CR_OBJECTS_LABEL_COLUMN);
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
        }

        if (StringUtils.isEmpty(fileLabel)) {
            fileLabel = fileName;
        }
    }

    /**
     * @throws IOException
     * @throws RepositoryException
     * @throws DAOException
     */
    @Deprecated
    private void saveWizardInputs() throws DAOException, RepositoryException, IOException {

        HarvestSourceDAO dao = DAOFactory.get().getDao(HarvestSourceDAO.class);

        dao.insertUpdateSourceMetadata(fileUri, Predicates.CR_OBJECTS_TYPE, ObjectDTO.createLiteral(objectsType));
        if (StringUtils.isNotEmpty(labelColumn)) {
            dao.insertUpdateSourceMetadata(fileUri, Predicates.CR_OBJECTS_LABEL_COLUMN, ObjectDTO.createLiteral(labelColumn));
        }
        if (StringUtils.isNotEmpty(fileLabel)) {
            dao.insertUpdateSourceMetadata(fileUri, Predicates.RDFS_LABEL, ObjectDTO.createLiteral(fileLabel));
        }

        ObjectDTO[] uniqueColTitles = new ObjectDTO[uniqueColumns.size()];
        for (int i = 0; i < uniqueColumns.size(); i++) {
            uniqueColTitles[i] = ObjectDTO.createLiteral(uniqueColumns.get(i));
        }

        dao.insertUpdateSourceMetadata(fileUri, Predicates.CR_OBJECTS_UNIQUE_COLUMN, uniqueColTitles);

        // Copyright information
        if (StringUtils.isNotEmpty(publisher)) {
            if (StringUtils.startsWithIgnoreCase(publisher, "http")) {
                dao.insertUpdateSourceMetadata(fileUri, Predicates.DCTERMS_PUBLISHER, ObjectDTO.createResource(publisher));
            } else {
                dao.insertUpdateSourceMetadata(fileUri, Predicates.DCTERMS_PUBLISHER, ObjectDTO.createLiteral(publisher));
            }
        }
        if (StringUtils.startsWithIgnoreCase(license, "http")) {
            dao.insertUpdateSourceMetadata(fileUri, Predicates.DCTERMS_LICENSE, ObjectDTO.createResource(license));
        } else {
            dao.insertUpdateSourceMetadata(fileUri, Predicates.DCTERMS_RIGHTS, ObjectDTO.createLiteral(license));
        }
        if (StringUtils.isNotEmpty(attribution)) {
            dao.insertUpdateSourceMetadata(fileUri, Predicates.DCTERMS_BIBLIOGRAPHIC_CITATION,
                    ObjectDTO.createLiteral(attribution));
        }
        if (StringUtils.isNotEmpty(source)) {
            if (StringUtils.startsWithIgnoreCase(source, "http")) {
                dao.insertUpdateSourceMetadata(fileUri, Predicates.DCTERMS_SOURCE, ObjectDTO.createResource(source));
            } else {
                dao.insertUpdateSourceMetadata(fileUri, Predicates.DCTERMS_SOURCE, ObjectDTO.createLiteral(source));
            }
        }
    }

    /**
     * Saves the selected data linking script information and stores it as source specific post harvest script.
     *
     * @throws DAOException
     */
    private void saveDataLinkingScripts() throws DAOException {
        LOGGER.debug("Saving data linking scripts:");

        PostHarvestScriptDAO dao = DAOFactory.get().getDao(PostHarvestScriptDAO.class);
        List<PostHarvestScriptDTO> scripts = dao.list(PostHarvestScriptDTO.TargetType.SOURCE, fileUri);

        for (DataLinkingScript dataLinkingScript : dataLinkingScripts) {

            String columnUri = fileUri + "#" + dataLinkingScript.getColumn();
            columnUri = "<" + columnUri.replace(" ", "_") + ">";

            ScriptTemplateDTO scriptTemplate = new ScriptTemplateDaoImpl().getScriptTemplate(dataLinkingScript.getScriptId());
            String script = StringUtils.replace(scriptTemplate.getScript(), "[TABLECOLUMN]", columnUri);

            try {
                int existingScriptId = isUniqueScript(scripts, fileUri, scriptTemplate.getName());
                if (existingScriptId == 0) {
                    dao.insert(PostHarvestScriptDTO.TargetType.SOURCE, fileUri, scriptTemplate.getName(), script, true, false);
                } else {
                    dao.save(existingScriptId, scriptTemplate.getName(), script, true, false);
                }
                LOGGER.debug("Data linking script successfully added: " + scriptTemplate.getName() + ", "
                        + dataLinkingScript.getColumn());
            } catch (DAOException e) {
                LOGGER.error("Failed to add data linking script: " + scriptTemplate.getName(), e);
                addWarningMessage("Failed to add data linking script '" + scriptTemplate.getName() + "': " + e.getMessage());
            }
        }
    }

    /**
     * Checks if script with given uri and name already exists in database. If so, the id of the script is returned.
     *
     * @param scripts
     * @param uri
     * @param name
     * @return
     */
    private int isUniqueScript(List<PostHarvestScriptDTO> scripts, String uri, String name) {
        for (PostHarvestScriptDTO script : scripts) {
            if (uri.equalsIgnoreCase(script.getTargetUrl()) && name.equalsIgnoreCase(script.getTitle())) {
                return script.getId();
            }
        }
        return 0;
    }

    /**
     * Runs all the source specific scripts that are stored for the file uri.
     */
    private void runScripts() {
        RepositoryConnection conn = null;
        try {
            conn = SesameUtil.getRepositoryConnection();
            conn.setAutoCommit(false);
            PostHarvestScriptDAO dao = DAOFactory.get().getDao(PostHarvestScriptDAO.class);

            List<PostHarvestScriptDTO> scripts = dao.listActive(PostHarvestScriptDTO.TargetType.SOURCE, fileUri);

            for (PostHarvestScriptDTO script : scripts) {
                runScript(script, conn);
            }

            conn.commit();
        } catch (Exception e) {
            SesameUtil.rollback(conn);
            LOGGER.error("Failed to run data linking scripts", e);
            addWarningMessage("Failed to run data linking scripts: " + e.getMessage());
        } finally {
            SesameUtil.close(conn);
        }
    }

    /**
     * Runs the script.
     *
     * @param scriptDto
     * @param conn
     */
    private void runScript(PostHarvestScriptDTO scriptDto, RepositoryConnection conn) {

        String targetUrl = scriptDto.getTargetUrl();
        String query = scriptDto.getScript();
        String title = scriptDto.getTitle();
        String parsedQuery = PostHarvestScriptParser.parseForExecution(query, targetUrl, null);

        try {
            int updateCount = SesameUtil.executeSPARUL(parsedQuery, conn);
            if (updateCount > 0 && !scriptDto.isRunOnce()) {
                // run maximum 100 times
                LOGGER.debug("Script's update count was " + updateCount
                        + ", running it until the count becomes 0, or no more than 100 times ...");
                int i = 0;
                int totalUpdateCount = updateCount;
                for (; updateCount > 0 && i < 100; i++) {
                    updateCount = SesameUtil.executeSPARUL(parsedQuery, conn, targetUrl);
                    totalUpdateCount += updateCount;
                }
                LOGGER.debug("Script was run for a total of " + (i + 1) + " times, total update count = " + totalUpdateCount);
            } else {
                LOGGER.debug("Script's update count was " + updateCount);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to run data linking post-harvest script '" + title + "': " + e.getMessage(), e);
            addWarningMessage("Failed to run data linking post-harvest script '" + title + "': " + e.getMessage());
        }
    }

    /**
     *
     * @param csvReader
     */
    private void close(CSVReader csvReader) {
        if (csvReader != null) {
            try {
                csvReader.close();
            } catch (Exception e) {
                // Ignore closing exceptions.
            }
        }
    }

    /**
     *
     * @throws DAOException
     */
    private void linkFileToFolder() throws DAOException {

        // prepare "folder hasFile file" statement
        ObjectDTO fileObject = ObjectDTO.createResource(fileUri);
        // fileObject.setSourceUri(folderUri);
        String folderContext = FolderUtil.folderContext(folderUri);
        fileObject.setSourceUri(folderContext);
        SubjectDTO folderSubject = new SubjectDTO(folderUri, false);
        folderSubject.addObject(Predicates.CR_HAS_FILE, fileObject);

        logger.debug("Creating the cr:hasFile predicate");

        // persist the prepared "folder hasFile file" statement
        DAOFactory.get().getDao(HelperDAO.class).addTriples(folderSubject);

        // since folder URI was used above as triple source, add it to HARVEST_SOURCE too
        // (but set interval minutes to 0, to avoid it being background-harvested)
        // HarvestSourceDTO folderHarvestSource = HarvestSourceDTO.create(folderUri, false, 0, getUserName());
        HarvestSourceDTO folderHarvestSource =
                HarvestSourceDTO.create(folderContext, false, 0, (getUser() != null ? getUserName() : null));
        DAOFactory.get().getDao(HarvestSourceDAO.class).addSourceIgnoreDuplicate(folderHarvestSource);
    }

    /**
     *
     * @param fileSize
     * @throws Exception
     */
    private void createFileMetadataAndSource(long fileSize) throws Exception {

        HarvestSourceDAO dao = DAOFactory.get().getDao(HarvestSourceDAO.class);
        dao.addSourceIgnoreDuplicate(HarvestSourceDTO.create(fileUri, false, 0, getUserName()));

        String mediaType = fileType.toString();
        String lastModified = Util.virtuosoDateToString(new Date());

        dao.insertUpdateSourceMetadata(fileUri, Predicates.RDF_TYPE, ObjectDTO.createResource(Subjects.CR_TABLE_FILE));
        dao.insertUpdateSourceMetadata(fileUri, Predicates.RDFS_LABEL, ObjectDTO.createLiteral(fileName));
        dao.insertUpdateSourceMetadata(fileUri, Predicates.CR_BYTE_SIZE, ObjectDTO.createLiteral(fileSize));
        dao.insertUpdateSourceMetadata(fileUri, Predicates.CR_MEDIA_TYPE, ObjectDTO.createLiteral(mediaType));
        dao.insertUpdateSourceMetadata(fileUri, Predicates.CR_LAST_MODIFIED, ObjectDTO.createLiteral(lastModified));
    }

    /**
     * @param guessEncoding
     * @return
     * @throws IOException
     */
    @Deprecated
    private CSVReader createCSVReader(boolean guessEncoding) throws IOException {

        CSVReader result = null;
        // File file = FileStore.getInstance(getUserName()).getFile(relativeFilePath);
        File file = FileStore.getInstance(FolderUtil.getUserDir(folderUri, getUserName())).getFile(relativeFilePath);
        if (file != null && file.exists()) {
            if (guessEncoding) {
                Charset charset = CharsetToolkit.guessEncoding(file, 4096, Charset.forName("UTF-8"));
                result = new CSVReader(new InputStreamReader(new FileInputStream(file), charset), getDelimiter());
            } else {
                result = new CSVReader(new FileReader(file), getDelimiter());
            }
        }

        return result;
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
     * @throws IOException
     */
    public List<String> getColumns() throws IOException {

        if (columns == null) {
            CSVReader csvReader = null;
            try {
                csvReader = createCSVReader(false);
                String[] columnsArray = csvReader.readNext();
                columns = new ArrayList<String>();
                if (columnsArray != null && columnsArray.length > 0) {
                    int emptyColCount = 1;
                    for (String col : columnsArray) {
                        if (StringUtils.isEmpty(col)) {
                            col = CsvImportHelper.EMPTY_COLUMN + emptyColCount++;
                        }
                        String colLabel = StringUtils.substringBefore(col, ":").trim();
                        colLabel = StringUtils.substringBefore(colLabel, "@").trim();
                        columns.add(colLabel);
                    }
                    // columns = Arrays.asList(trimAll(columnsArray));
                }
            } finally {
                close(csvReader);
            }
        }

        return columns;
    }

    /**
     * @return
     */
    public String getLabelColumn() {
        return labelColumn;
    }

    /**
     * @param labelColumn
     */
    public void setLabelColumn(String labelColumn) {
        this.labelColumn = labelColumn;
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
     *
     * @return
     */
    @Deprecated
    public char getDelimiter() {
        return fileType != null && fileType.equals(FileType.TSV) ? '\t' : ',';
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
