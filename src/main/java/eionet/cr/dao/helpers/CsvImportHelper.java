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
 *        Juhan Voolaid
 */

package eionet.cr.dao.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.PostHarvestScriptDAO;
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
import eionet.cr.web.action.DataLinkingScript;
import eionet.cr.web.action.UploadCSVActionBean.FileType;
import eionet.cr.web.action.admin.postHarvest.PostHarvestScriptParser;
import eionet.cr.web.util.CharsetToolkit;

/**
 * Helper methods for importing CSV file.
 *
 * @author Juhan Voolaid
 */
public class CsvImportHelper {

    /** SpreadsheetColumn name for empty name. */
    public static final String EMPTY_COLUMN = "Empty";

    /** */
    private static final Logger LOGGER = Logger.getLogger(CsvImportHelper.class);

    /** Columns detected in the uploaded file (it's the titles of the columns). */
    private List<String> columns;

    /** SpreadsheetColumn labels detected in the uploaded file (titles without type and language code). */
    private List<String> columnLabels;

    /** The columns (i.e. column titles) forming the contained objects' unique identifiers. */
    private List<String> uniqueColumns;

    /** The URI that will be assigned to the resource representing the file. */
    private String fileUri;

    /** User can specify rdf:label for the file. */
    private String fileLabel;

    /** Uploaded file's type. */
    private FileType fileType;

    /** The type of objects contained in the file (user-given free text). */
    private String objectsType;

    /** Publisher of uploaded material. */
    private String publisher;

    /** License of uploaded material. */
    private String license;

    /** Attribution. */
    private String attribution;

    /** Source of the uploaded material. */
    private String source;

    /** Incremented id for a row, in case unique columns are not specified. */
    private int idCounter;

    /** Id formatter. */
    private DecimalFormat idFormatter;

    /**
     * Class constructor.
     *
     * @param uniqueColumns
     * @param fileUri
     * @param fileLabel
     * @param fileType
     * @param objectsType
     * @param publisher
     * @param license
     * @param attribution
     * @param source
     */
    public CsvImportHelper(List<String> uniqueColumns, String fileUri, String fileLabel, FileType fileType, String objectsType,
            String publisher, String license, String attribution, String source) {

        if (uniqueColumns == null) {
            uniqueColumns = new ArrayList<String>();
        }

        this.uniqueColumns = uniqueColumns;
        this.fileUri = fileUri;
        this.fileLabel = fileLabel;
        this.fileType = fileType;
        this.objectsType = objectsType;
        this.publisher = publisher;
        this.license = license;
        this.attribution = attribution;
        this.source = source;
        idFormatter = new DecimalFormat("000000");
    }

    /**
     * Quick way to extract the csv column labels.
     */
    public static List<String> extractColumnLabels(String folderUri, String relativeFilePath, String userName, FileType fileType)
            throws Exception {

        CsvImportHelper helper = new CsvImportHelper(null, null, null, fileType, null, null, null, null, null);
        CSVReader csvReader = null;
        try {
            csvReader = helper.createCSVReader(folderUri, relativeFilePath, userName, true);
            return helper.extractColumnLabels(helper.extractColumns(csvReader));
        } catch (Exception e) {
            throw e;
        } finally {
            close(csvReader);
        }
    }

    /**
     * Closes scv reader connection.
     *
     * @param csvReader
     */
    public static void close(CSVReader csvReader) {
        if (csvReader != null) {
            try {
                csvReader.close();
            } catch (Exception e) {
                // Ignore closing exceptions.
            }
        }
    }

    /**
     * Iserts file metadata.
     *
     * @param fileSize
     * @param userName
     * @throws Exception
     */
    public void insertFileMetadataAndSource(long fileSize, String userName) throws Exception {

        HarvestSourceDAO dao = DAOFactory.get().getDao(HarvestSourceDAO.class);
        dao.addSourceIgnoreDuplicate(HarvestSourceDTO.create(fileUri, false, 0, userName));

        String mediaType = fileType.toString();
        String lastModified = Util.virtuosoDateToString(new Date());

        dao.insertUpdateSourceMetadata(fileUri, Predicates.RDF_TYPE, ObjectDTO.createResource(Subjects.CR_TABLE_FILE));
        dao.insertUpdateSourceMetadata(fileUri, Predicates.CR_BYTE_SIZE, ObjectDTO.createLiteral(fileSize));
        dao.insertUpdateSourceMetadata(fileUri, Predicates.CR_MEDIA_TYPE, ObjectDTO.createLiteral(mediaType));
        dao.insertUpdateSourceMetadata(fileUri, Predicates.CR_LAST_MODIFIED, ObjectDTO.createLiteral(lastModified));
    }

    /**
     * Adds reference of the file to the given parent folder.
     *
     * @param folderUri
     * @param userName
     * @throws DAOException
     */
    public void linkFileToFolder(String folderUri, String userName) throws DAOException {

        // prepare "folder hasFile file" statement
        ObjectDTO fileObject = ObjectDTO.createResource(fileUri);
        // fileObject.setSourceUri(folderUri);
        String folderContext = FolderUtil.folderContext(folderUri);
        fileObject.setSourceUri(folderContext);
        SubjectDTO folderSubject = new SubjectDTO(folderUri, false);
        folderSubject.addObject(Predicates.CR_HAS_FILE, fileObject);

        // persist the prepared "folder hasFile file" statement
        DAOFactory.get().getDao(HelperDAO.class).addTriples(folderSubject);

        // since folder URI was used above as triple source, add it to HARVEST_SOURCE too
        // (but set interval minutes to 0, to avoid it being background-harvested)
        // HarvestSourceDTO folderHarvestSource = HarvestSourceDTO.create(folderUri, false, 0, getUserName());
        HarvestSourceDTO folderHarvestSource = HarvestSourceDTO.create(folderContext, false, 0, userName);
        DAOFactory.get().getDao(HarvestSourceDAO.class).addSourceIgnoreDuplicate(folderHarvestSource);
    }

    /**
     * @param guessEncoding
     * @return
     * @throws IOException
     */
    public CSVReader createCSVReader(String folderUri, String relativeFilePath, String userName, boolean guessEncoding)
            throws IOException {

        CSVReader result = null;
        // File file = FileStore.getInstance(getUserName()).getFile(relativeFilePath);
        File file = FileStore.getInstance(FolderUtil.getUserDir(folderUri, userName)).getFile(relativeFilePath);
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
     * Extracts data from csv file.
     *
     * @param csvReader
     * @throws IOException
     * @throws DAOException
     * @throws RepositoryException
     */
    public void extractObjects(CSVReader csvReader) throws IOException, DAOException, RepositoryException {

        // Set columns and columnLabels by reading the first line.
        columns = extractColumns(csvReader);
        columnLabels = extractColumnLabels(columns);

        // Read the contained objects by reading the rest of lines.
        String[] line = null;
        String objectsTypeUri = fileUri + "/" + objectsType;
        HelperDAO helperDao = DAOFactory.get().getDao(HelperDAO.class);

        RepositoryConnection conn = SesameUtil.getRepositoryConnection();
        conn.setAutoCommit(false);
        try {
            while ((line = csvReader.readNext()) != null) {
                SubjectDTO subject = extractObject(line, objectsTypeUri);
                helperDao.addTriples(conn, subject);
            }
            conn.commit();
        } catch (DAOException e) {
            SesameUtil.rollback(conn);
            throw e;
        } catch (RepositoryException e) {
            SesameUtil.rollback(conn);
            throw e;
        } finally {
            SesameUtil.close(conn);
        }

        // Construct a SPARQL query and store it as a property
        StringBuilder query = new StringBuilder();
        query.append("PREFIX tableFile: <" + fileUri + "#>\n\n");
        query.append("SELECT *\nFROM <").append(fileUri).append(">\nWHERE {\n");
        for (String column : columnLabels) {
            column = column.replace(" ", "_");
            String columnUri = "tableFile:" + column;
            query.append(" OPTIONAL { _:rec ").append(columnUri).append(" ?").append(column).append(" } .\n");
        }
        query.append("}");

        HarvestSourceDAO dao = DAOFactory.get().getDao(HarvestSourceDAO.class);
        dao.insertUpdateSourceMetadata(fileUri, Predicates.CR_SPARQL_QUERY, ObjectDTO.createLiteral(query.toString()));

        // Finally, make sure that the file has the correct number of harvested statements in its predicates.
        DAOFactory.get().getDao(HarvestSourceDAO.class).updateHarvestedStatementsTriple(fileUri);
    }

    /**
     * Stores the additional meta data from wizard inputs.
     *
     * @throws DAOException
     * @throws RepositoryException
     * @throws IOException
     */
    public void saveWizardInputs() throws DAOException, RepositoryException, IOException {

        HarvestSourceDAO dao = DAOFactory.get().getDao(HarvestSourceDAO.class);

        dao.insertUpdateSourceMetadata(fileUri, Predicates.CR_OBJECTS_TYPE, ObjectDTO.createLiteral(objectsType));
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
    public void saveDataLinkingScripts(List<DataLinkingScript> dataLinkingScripts) throws DAOException {
        PostHarvestScriptDAO dao = DAOFactory.get().getDao(PostHarvestScriptDAO.class);
        List<PostHarvestScriptDTO> scripts = dao.list(PostHarvestScriptDTO.TargetType.SOURCE, fileUri);

        for (DataLinkingScript dataLinkingScript : dataLinkingScripts) {

            String columnUri = fileUri + "#" + dataLinkingScript.getColumn();
            columnUri = "<" + columnUri.replace(" ", "_") + ">";

            ScriptTemplateDTO scriptTemplate = new ScriptTemplateDaoImpl().getScriptTemplate(dataLinkingScript.getScriptId());
            String script = StringUtils.replace(scriptTemplate.getScript(), "[TABLECOLUMN]", columnUri);

            int existingScriptId = isUniqueScript(scripts, fileUri, scriptTemplate.getName());
            if (existingScriptId == 0) {
                dao.insert(PostHarvestScriptDTO.TargetType.SOURCE, fileUri, scriptTemplate.getName(), script, true, true);
            } else {
                dao.save(existingScriptId, scriptTemplate.getName(), script, true, true);
            }
        }
    }

    /**
     * Runs all the source specific scripts that are stored for the file uri.
     *
     * @return warning messages
     * @throws Exception
     */
    public List<String> runScripts() throws Exception {
        RepositoryConnection conn = null;
        List<String> warnings = new ArrayList<String>();
        try {
            conn = SesameUtil.getRepositoryConnection();
            conn.setAutoCommit(false);
            PostHarvestScriptDAO dao = DAOFactory.get().getDao(PostHarvestScriptDAO.class);

            List<PostHarvestScriptDTO> scripts = dao.listActive(PostHarvestScriptDTO.TargetType.SOURCE, fileUri);

            for (PostHarvestScriptDTO script : scripts) {
                String warning = runScript(script, conn);
                if (StringUtils.isNotEmpty(warning)) {
                    warnings.add(warning);
                }
            }

            conn.commit();
        } catch (Exception e) {
            SesameUtil.rollback(conn);
            throw e;
        } finally {
            SesameUtil.close(conn);
        }

        return warnings;
    }

    /**
     * Runs the script.
     *
     * @param scriptDto
     * @param conn
     * @return warning message
     */
    private String runScript(PostHarvestScriptDTO scriptDto, RepositoryConnection conn) {

        String targetUrl = scriptDto.getTargetUrl();
        String query = scriptDto.getScript();
        String title = scriptDto.getTitle();
        String parsedQuery = PostHarvestScriptParser.parseForExecution(query, targetUrl, null);

        String warningMessage = null;

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
            warningMessage = "Failed to run data linking post-harvest script '" + title + "': " + e.getMessage();
        }

        return warningMessage;
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
     * Extracts columns (with language and type) from csv file.
     *
     * @param csvReader
     * @return
     * @throws IOException
     */
    private List<String> extractColumns(CSVReader csvReader) throws IOException {
        // Set columns and columnLabels by reading the first line.
        String[] columnsArray = csvReader.readNext();
        ArrayList<String> columnsResult = new ArrayList<String>();
        if (columnsArray != null && columnsArray.length > 0) {
            int emptyColCount = 1;
            for (String col : columnsArray) {
                if (StringUtils.isEmpty(col)) {
                    col = CsvImportHelper.EMPTY_COLUMN + emptyColCount++;
                }
                columnsResult.add(col.trim());
            }
        }

        return columnsResult;
    }

    /**
     * Extracts column labels (without language and type) from columns.
     *
     * @param rawColumns
     * @return
     */
    private List<String> extractColumnLabels(List<String> rawColumns) {
        ArrayList<String> columnsLabelResult = new ArrayList<String>();

        for (String col : rawColumns) {
            String colLabel = StringUtils.substringBefore(col, ":").trim();
            colLabel = StringUtils.substringBefore(colLabel, "@").trim();
            columnsLabelResult.add(colLabel);
        }

        return columnsLabelResult;
    }

    /**
     * Extracts object from csv row.
     *
     * @param line
     * @param objectsTypeUri
     * @return
     */
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

        }

        return subject;
    }

    /**
     * Retrurns unique object id.
     *
     * @param line
     * @return
     */
    private String extractObjectId(String[] line) {

        StringBuilder buf = new StringBuilder();
        String result = null;
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
            result = buf.toString();
        } else {
            result = idFormatter.format(++idCounter);
        }

        return result;
    }

    /**
     * Returns rdf object value with additional type and language definitions.
     *
     * @param column
     * @param value
     * @param type
     * @param lang
     * @return
     */
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
     * Returns deliminiter based of the file type.
     *
     * @return
     */
    private char getDelimiter() {
        return fileType != null && fileType.equals(FileType.TSV) ? '\t' : ',';
    }

}
