package eionet.cr.web.action;

import at.jku.xlwrap.common.XLWrapException;
import eionet.cr.common.Predicates;
import eionet.cr.common.TempFilePathGenerator;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.dao.util.UriLabelPair;
import eionet.cr.dto.SearchResultDTO;
import eionet.cr.dto.SkosItemDTO;
import eionet.cr.util.FileDeletionJob;
import eionet.cr.util.Pair;
import eionet.cr.util.export.CodelistExportType;
import eionet.cr.util.xlwrap.XLWrapUploadType;
import eionet.cr.util.xlwrap.XLWrapUtil;
import eionet.cr.web.action.factsheet.FactsheetActionBean;
import eionet.cr.web.security.CRUser;
import net.sourceforge.stripes.action.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;

/**
 * An action bean for browsing codelists starting with a particular URI prefix.
 *
 * @author jaanus
 */
@UrlBinding("/codelists")
public class BrowseCodelistsActionBean extends AbstractActionBean {

    /** */
    private static final Logger LOGGER = Logger.getLogger(BrowseCodelistsActionBean.class);

    /** */
    public static final String CODELISTS_PREFIX = "http://semantic.digital-agenda-data.eu/codelist/";

    /** */
    private static final String JSP = "/pages/browseCodelists.jsp";

    /** */
    private static final String[] LABEL_PREDICATES = {Predicates.DCTERMS_TITLE, Predicates.RDFS_LABEL, Predicates.DC_TITLE,
            Predicates.FOAF_NAME};

    /** */
    private List<UriLabelPair> codelists;

    /** */
    private List<SkosItemDTO> codelistItems;

    /** */
    private String codelistUri;
    private String datasetUri;
    private String freeText;
    private List<String> templateColumnNames;

    /**
     *
     * @return
     */
    @DefaultHandler
    public Resolution defaultEvent() {

        ScoreboardSparqlDAO dao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
        try {
            codelists = dao.getCodelists();
        } catch (DAOException e) {
            LOGGER.error("Error when retrieving codelists", e);
            addWarningMessage("A technical error occurred when when retrieving available codelists" + e.getMessage());
        }

        if (StringUtils.isBlank(codelistUri) && codelists != null && !codelists.isEmpty()) {
            codelistUri = codelists.iterator().next().getUri();
        }

        if (StringUtils.isNotBlank(codelistUri)) {

            try {
                codelistItems = dao.getCodelistItems(codelistUri, datasetUri, freeText);
            } catch (DAOException e) {
                LOGGER.error("Error when retrieving items of this codelist: " + codelistUri, e);
                addWarningMessage("A technical error occurred when retrieving items of the selected codelist"
                        + e.getMessage());
            }

            XLWrapUploadType codelistType = XLWrapUploadType.getByCodelistUri(codelistUri);
            if (codelistType != null) {
                try {
                    templateColumnNames = XLWrapUtil.getTemplateColumnNames(codelistType);
                } catch (Exception e) {
                    LOGGER.error("Error when retrieving codelist template column names", e);
                    addWarningMessage("A technical error occurred when retrieving codelist template column names");
                }
            } else {
                LOGGER.error("Failed to find codelist upload type by this codelist URI: " + codelistUri);
            }
        }

        return new ForwardResolution(JSP);
    }

    /**
     *
     * @return
     */
    public Resolution metadata() {
        if (StringUtils.isNotBlank(codelistUri)) {
            return new RedirectResolution(FactsheetActionBean.class).addParameter("uri", codelistUri);
        } else {
            addWarningMessage("No codelist selected!");
            return new ForwardResolution(JSP);
        }
    }

    /**
     *
     * @return
     */
    public Resolution createItem() {

        if (StringUtils.isBlank(codelistUri)) {
            LOGGER.error(getContext().getEventName() + ": found no codelist URI in request!");
            addWarningMessage("An unexpected technical error occurred!");
            return new RedirectResolution(getClass());
        }

        RedirectResolution resolution = new RedirectResolution(getClass()).addParameter("codelistUri", this.codelistUri);

        XLWrapUploadType codelistType = XLWrapUploadType.getByCodelistUri(codelistUri);
        if (codelistType != null) {
        } else {
            LOGGER.error("Failed to find codelist upload type by this codelist URI: " + codelistUri);
            addWarningMessage("A technical error occurred when detecting codelist type!");
            return resolution;
        }

        Map<Integer, String> columnsMap = new HashMap<>();
        HttpServletRequest request = getContext().getRequest();
        Enumeration paramNames = request.getParameterNames();
        String colParamPrefix = "col_";
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement().toString();
            if (paramName.startsWith(colParamPrefix)) {
                int colIndex = NumberUtils.toInt(StringUtils.substringAfter(paramName, colParamPrefix), -1);
                if (colIndex >= 0) {
                    columnsMap.put(colIndex, request.getParameter(paramName));
                }
            }
        }

        List<String> colValues = new ArrayList<>();
        for (int i = 0; i < columnsMap.size(); i++) {
            colValues.add(StringUtils.trimToEmpty(columnsMap.get(i)));
        }

        try {
            int resourceCount = XLWrapUtil.importCodelistItem(codelistType, colValues);
            if (resourceCount > 0) {
                addSystemMessage("Item successfully created in the below list!");
            } else {
                addWarningMessage("Failed to create the codelist item!");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create the codelist item", e);
            addWarningMessage("A technical error occurred when trying to create the codelist item!");
        }

        return resolution;
    }

    /**
     *
     * @return
     */
    public Resolution export() {

        if (StringUtils.isBlank(codelistUri)) {
            addWarningMessage("No codelist selected!");
            return defaultEvent();
        }

        String itemRdfType = null;
        Map<String, Integer> propsToSpreadsheetCols = null;
        File spreadsheetTemplate = null;
        File destinationFile = null;

        String codelistGraphUri = codelistUri.endsWith("/") ? codelistUri : codelistUri + "/";
        XLWrapUploadType uploadType = XLWrapUploadType.getByGraphUri(codelistGraphUri);
        if (uploadType != null) {

            itemRdfType = uploadType.getSubjectsTypeUri();
            if (StringUtils.isBlank(itemRdfType)) {
                addWarningMessage("Technical error: failed to detect the RDF type of this codelist's items!");
                return defaultEvent();
            }

            File mappingTemplate = uploadType.getMappingTemplate();
            if (mappingTemplate == null || !mappingTemplate.exists() || !mappingTemplate.isFile()) {
                addWarningMessage("Technical error: failed to locate the corresponding spreadsheet mapping file!");
                return defaultEvent();
            }

            LOGGER.debug("uploadType = " + uploadType);
            LOGGER.debug("itemRdfType = " + itemRdfType);
            LOGGER.debug("mappingTemplate = " + mappingTemplate);

            try {
                propsToSpreadsheetCols = XLWrapUtil.getPropsToSpreadsheetCols(mappingTemplate);
                if (propsToSpreadsheetCols == null || propsToSpreadsheetCols.isEmpty()) {
                    addWarningMessage("Found no property-to-spreadsheet-column mappings in the mapping file!");
                    return defaultEvent();
                }
            } catch (IOException e) {
                LOGGER.error("I/O error when trying to parse the spreadsheet mapping file!", e);
                addWarningMessage("Technical error: I/O error when trying to parse the spreadsheet mapping file!");
                return defaultEvent();
            } catch (XLWrapException e) {
                LOGGER.error("XLWrapException when trying to parse the spreadsheet mapping file!", e);
                addWarningMessage("Technical error: parsing error when parsing the spreadsheet mapping file!");
                return defaultEvent();
            }

            LOGGER.debug("propsToSpreadsheetCols = " + propsToSpreadsheetCols);

            spreadsheetTemplate = uploadType.getSpreadsheetTemplate();
            if (spreadsheetTemplate == null || !spreadsheetTemplate.exists() || !spreadsheetTemplate.isFile()) {
                addWarningMessage("Technical error: failed to locate the corresponding spreadsheet template!");
                return defaultEvent();
            }

            destinationFile = TempFilePathGenerator.generate(XLWrapUploadType.SPREADSHEET_FILE_EXTENSION);
        } else {

            CodelistExportType exportType = CodelistExportType.getByCodelistUri(codelistUri);
            if (exportType == null) {
                addWarningMessage("Export not supported for this codelist type!");
                return defaultEvent();
            }

            propsToSpreadsheetCols = exportType.getPropertiesToColumnsMapping();
            spreadsheetTemplate = exportType.getSpreadsheetTemplateFile();
            destinationFile = TempFilePathGenerator.generate(XLWrapUploadType.SPREADSHEET_FILE_EXTENSION);
        }

        try {
            FileUtils.copyFile(spreadsheetTemplate, destinationFile);
        } catch (IOException e) {
            LOGGER.error("Error when creating instance file from the located spreadsheet template!", e);
            addWarningMessage("Technical error when creating instance file from the located spreadsheet template!");
            return defaultEvent();
        }

        ScoreboardSparqlDAO dao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
        try {
            int itemCount = dao.exportCodelistItems(codelistUri, spreadsheetTemplate, propsToSpreadsheetCols, destinationFile);
            LOGGER.debug("Number of exported codelist items = " + itemCount);
            return streamToResponse(destinationFile, spreadsheetTemplate.getName());
        } catch (DAOException e) {
            LOGGER.error("Error when exporting " + codelistUri + " to " + destinationFile, e);
            addWarningMessage("Codelist export failed with technical error: " + e.getMessage());
            return defaultEvent();
        }
    }

    /**
     *
     * @param file
     * @param targetName
     * @return
     */
    private StreamingResolution streamToResponse(final File file, String targetName) {

        return new StreamingResolution("application/vnd.ms-excel") {

            /*
             * (non-Javadoc)
             *
             * @see net.sourceforge.stripes.action.StreamingResolution#stream(javax.servlet.http.HttpServletResponse)
             */
            @Override
            public void stream(HttpServletResponse response) throws Exception {

                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = new FileInputStream(file);
                    outputStream = response.getOutputStream();
                    IOUtils.copy(inputStream, outputStream);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                    IOUtils.closeQuietly(outputStream);
                    FileDeletionJob.register(file);
                }
            }
        }.setFilename(StringUtils.isBlank(targetName) ? file.getName() : targetName);
    }

    /**
     * @return the codelistUri
     */
    public String getCodelistUri() {
        return codelistUri;
    }

    /**
     * @param codelistUri
     *            the codelistUri to set
     */
    public void setCodelistUri(String codelistUri) {
        this.codelistUri = codelistUri;
    }

    /**
     * @return the codelists
     */
    public List<UriLabelPair> getCodelists() {
        return codelists;
    }

    /**
     * @return the codelistItems
     */
    public List<SkosItemDTO> getCodelistItems() {
        return codelistItems;
    }

    /**
     *
     * @return
     */
    public Class getFactsheetActionBeanClass() {
        return FactsheetActionBean.class;
    }

    public String getDatasetUri() {
        return datasetUri;
    }

    public void setDatasetUri(String datasetUri) {
        this.datasetUri = datasetUri;
    }

    public String getFreeText() {
        return freeText;
    }

    public void setFreeText(String freeText) {
        this.freeText = freeText;
    }

    public List<Pair<String, String>> getDatasets() throws DAOException {

        ScoreboardSparqlDAO dao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
        SearchResultDTO<Pair<String, String>> searchResult =
                dao.getDistinctDatasets(isUserLoggedIn(), null, null, LABEL_PREDICATES);
        return searchResult == null ? new ArrayList<Pair<String, String>>() : searchResult.getItems();
    }

    public List<String> getTemplateColumnNames() {
        return templateColumnNames;
    }

    public boolean isModifyPermitted() {
        return getUser() != null && CRUser.hasPermission(getContext().getRequest().getSession(), "/registrations", "u");
    }
}
