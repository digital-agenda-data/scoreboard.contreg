package eionet.cr.web.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import at.jku.xlwrap.common.XLWrapException;
import eionet.cr.common.TempFilePathGenerator;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.dto.SkosItemDTO;
import eionet.cr.util.FileDeletionJob;
import eionet.cr.util.Pair;
import eionet.cr.util.xlwrap.XLWrapUploadType;
import eionet.cr.util.xlwrap.XLWrapUtil;
import eionet.cr.web.action.factsheet.FactsheetActionBean;

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
    private List<Pair<String, String>> codelists;

    /** */
    private List<SkosItemDTO> codelistItems;

    /** */
    private String codelistUri;

    /**
     *
     * @return
     */
    @DefaultHandler
    public Resolution defaultEvent() {

        ScoreboardSparqlDAO dao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
        try {
            codelists = dao.getCodelists(CODELISTS_PREFIX);
        } catch (DAOException e) {
            LOGGER.error("Error when retrieving codelists whose URI starts with " + CODELISTS_PREFIX, e);
            addWarningMessage("A technical error occurred when when retrieving available codelists" + e.getMessage());
        }

        if (StringUtils.isBlank(codelistUri) && codelists != null && !codelists.isEmpty()) {
            codelistUri = codelists.iterator().next().getLeft();
        }

        if (StringUtils.isNotBlank(codelistUri)) {
            try {
                codelistItems = dao.getCodelistItems(codelistUri);
            } catch (DAOException e) {
                LOGGER.error("Error when retrieving items of this codelist: " + codelistUri, e);
                addWarningMessage("A technical error occurred when when retrieving items of the selected codelist"
                        + e.getMessage());
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
    public Resolution export() {

        if (StringUtils.isBlank(codelistUri)) {
            addWarningMessage("No codelist selected!");
            return defaultEvent();
        }

        String codelistGraphUri = codelistUri.endsWith("/") ? codelistUri : codelistUri + "/";
        XLWrapUploadType uploadType = XLWrapUploadType.getByGraphUri(codelistGraphUri);
        if (uploadType == null) {
            addWarningMessage("Technical error: failed to detect codelist type from submitted inputs!");
            return defaultEvent();
        }

        String itemRdfType = uploadType.getSubjectsTypeUri();
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

        Map<String, Integer> propsToSpreadsheetCols = null;
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

        File spreadsheetTemplate = uploadType.getSpreadsheetTemplate();
        if (spreadsheetTemplate == null || !spreadsheetTemplate.exists() || !spreadsheetTemplate.isFile()) {
            addWarningMessage("Technical error: failed to locate the corresponding spreadsheet template!");
            return defaultEvent();
        }
        LOGGER.debug("spreadsheetTemplate = " + spreadsheetTemplate);

        File destFile = TempFilePathGenerator.generateWithExtension(XLWrapUploadType.SPREADSHEET_FILE_EXTENSION);
        try {
            FileUtils.copyFile(spreadsheetTemplate, destFile);
        } catch (IOException e) {
            LOGGER.error("Error when creating instance file from the located spreadsheet template!", e);
            addWarningMessage("Technical error when creating instance file from the located spreadsheet template!");
            return defaultEvent();
        }
        LOGGER.debug("destFile = " + destFile);

        ScoreboardSparqlDAO dao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
        try {
            int itemCount = dao.exportCodelistItems(itemRdfType, spreadsheetTemplate, propsToSpreadsheetCols, destFile);
            LOGGER.debug("Number of exported codelist items = " + itemCount);
            return streamToResponse(destFile, spreadsheetTemplate.getName());
        } catch (DAOException e) {
            LOGGER.error("Error when exporting " + codelistUri + " to " + destFile, e);
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
    public List<Pair<String, String>> getCodelists() {
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
}
