package eionet.cr.web.action.admin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationMethod;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;

import at.jku.xlwrap.common.XLWrapException;
import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.common.TempFilePathGenerator;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.dto.SearchResultDTO;
import eionet.cr.staging.util.TimePeriodsHarvester;
import eionet.cr.util.FileDeletionJob;
import eionet.cr.util.Pair;
import eionet.cr.util.xlwrap.StatementListener;
import eionet.cr.util.xlwrap.XLWrapUploadType;
import eionet.cr.util.xlwrap.XLWrapUtil;
import eionet.cr.web.action.AbstractActionBean;
import eionet.cr.web.action.factsheet.ObjectsInSourceActionBean;

/**
 * Action bean for uploading an MS Excel or OpenDocument spreadsheet file into the RDF model and triple store. Pre-configured types
 * of files are supported, e.g. a file containing metadata of Digital Agenda Scoreboard indicators, a file containing metadata of
 * Digital Agenda Scoreboard breakdowns, etc.
 *
 * {@link XLWrapUtil} is used for performing the parsing and storage into triple store.
 *
 * @author jaanus
 */
@UrlBinding("/admin/xlwrapUpload.action")
public class XLWrapUploadActionBean extends AbstractActionBean {

    /** */
    private static final String UPLOADED_GRAPH_ATTR = XLWrapUploadActionBean.class.getSimpleName() + ".uploadedGraph";

    /** */
    private static final Logger LOGGER = Logger.getLogger(XLWrapUploadActionBean.class);

    /** */
    public static final String JSP = "/pages/admin/staging/xlwUpload.jsp";

    /** */
    private XLWrapUploadType uploadType = XLWrapUploadType.INDICATOR;

    /** */
    private FileBean fileBean;

    /** */
    private String uploadedGraphUri;

    /** */
    private boolean clearGraph = false;

    /** */
    private List<Pair<String, String>> datasets;

    /** */
    private String targetDataset;

    /** */
    private boolean clearDataset;

    /** Fields populated from the "create new dataset" form. */
    private String newDatasetIdentifier;
    private String newDatasetTitle;
    private String newDatasetDescription;

    /**
     *
     * @return
     */
    @DefaultHandler
    public Resolution get() {
        HttpSession session = getContext().getRequest().getSession();
        if (session != null) {
            uploadedGraphUri = ObjectUtils.toString(session.getAttribute(UPLOADED_GRAPH_ATTR), null);
            session.removeAttribute(UPLOADED_GRAPH_ATTR);
        }
        return new ForwardResolution(JSP);
    }

    /**
     *
     * @return
     * @throws Exception
     */
    public Resolution upload() {

        boolean isObservationsUpload = false;
        if (uploadType == null) {
            addGlobalValidationError("Missing upload type!");
            return new ForwardResolution(JSP);
        } else if (fileBean == null || fileBean.getSize() == 0) {
            addGlobalValidationError("Uploaded file missing or empty!");
            return new ForwardResolution(JSP);
        } else if (XLWrapUploadType.OBSERVATION_SCOREBOARD.equals(uploadType)) {
            if (StringUtils.isBlank(targetDataset)) {
                addGlobalValidationError("Target dataset must be selected!");
                return new ForwardResolution(JSP);
            } else {
                isObservationsUpload = true;
            }
        }

        File spreadsheetFile = TempFilePathGenerator.generate("xls");
        try {
            fileBean.save(spreadsheetFile);
        } catch (IOException e) {
            addCautionMessage("Failed saving the upload file to a temporary location!");
            LOGGER.error("Failed saving " + fileBean.getFileName() + " to " + spreadsheetFile);
            return new ForwardResolution(JSP);
        }

        try {

            // If uploading observations, prepare the target data graph URI.
            String dataGraphUri = isObservationsUpload ? targetDataset : null;
            dataGraphUri = StringUtils.replace(dataGraphUri, "/dataset/", "/data/");

            // Prepare the "clear" flag, depending on whether we're clearing a target dataset of observations or codelist graph.
            boolean clear = isObservationsUpload ? clearDataset : clearGraph;
            StatementListener stmtListener = new StatementListener(uploadType.getSubjectsTypeUri());

            // Execute the import.
            XLWrapUtil.importMapping(uploadType, spreadsheetFile, dataGraphUri, clear, stmtListener);

            // If this far, then lets update dataset or codelist modification date, depending on whether
            // we're uploading observations or a codelist.
            if (isObservationsUpload) {
                DAOFactory.get().getDao(ScoreboardSparqlDAO.class).updateDcTermsModified(targetDataset, new Date(), targetDataset);
            } else {
                String graphUri = uploadType.getGraphUri();
                String codelistUri = StringUtils.substringBeforeLast(graphUri, "/");
                DAOFactory.get().getDao(ScoreboardSparqlDAO.class).updateDcTermsModified(codelistUri, new Date(), graphUri);
            }

            // Run post-import fixes.
            postImportFixes();

            // Harvest registered time-periods.
            harvestTimePeriods(stmtListener.getTimePeriodUris());

            // Feedback to the user.
            addSystemMessage(stmtListener.getSubjects().size()
                    + " items of selected type successfully imported!\n Click on on the below link to explore them further.");

            // Update the uploaded graph attribute in session and redirect to defaulkt event.
            getContext().setSessionAttribute(UPLOADED_GRAPH_ATTR, isObservationsUpload ? dataGraphUri : uploadType.getGraphUri());
            return new RedirectResolution(getClass());

        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            addCautionMessage("An I/O error occurred!");
            return new ForwardResolution(JSP);
        } catch (XLWrapException e) {
            LOGGER.error(e.getMessage(), e);
            addCautionMessage("A mapping failure occurred!");
            return new ForwardResolution(JSP);
        } catch (OpenRDFException e) {
            LOGGER.error(e.getMessage(), e);
            addCautionMessage("A repository access error occurred!");
            return new ForwardResolution(JSP);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            addCautionMessage("An unexpected technical error occurred!");
            return new ForwardResolution(JSP);
        } finally {
            FileDeletionJob.register(spreadsheetFile);
        }
    }

    /**
     *
     * @return
     */
    public Resolution cancel() {
        return new RedirectResolution(AdminWelcomeActionBean.class);
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
            return new RedirectResolution(getClass()).addParameter("targetDataset", datasetUri)
                    .addParameter("clearDataset", clearDataset).addParameter("uploadType", XLWrapUploadType.OBSERVATION_SCOREBOARD.name());
        } catch (DAOException e) {
            LOGGER.error("Dataset creation failed with technical error", e);
            addWarningMessage("Dataset creation failed with technical error: " + e.getMessage());
            return new ForwardResolution(JSP);
        }
    }

    /**
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

        getContext().setSourcePageResolution(new ForwardResolution(JSP));
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
     * @param uploadType
     *            the uploadType to set
     */
    public void setUploadType(XLWrapUploadType uploadType) {
        this.uploadType = uploadType;
    }

    /**
     * @param fileBean
     *            the fileBean to set
     */
    public void setFileBean(FileBean fileBean) {
        this.fileBean = fileBean;
    }

    /**
     *
     * @return
     */
    public List<XLWrapUploadType> getUploadTypes() {
        return Arrays.asList(XLWrapUploadType.values());
    }

    /**
     *
     * @return
     */
    public Class getObjectsInSourceActionBeanClass() {
        return ObjectsInSourceActionBean.class;
    }

    /**
     * @return the uploadType
     */
    public XLWrapUploadType getUploadType() {
        return uploadType;
    }

    /**
     * @return the uploadedGraphUri
     */
    public String getUploadedGraphUri() {
        return uploadedGraphUri;
    }

    /**
     * @param clearGraph
     *            the clearGraph to set
     */
    public void setClearGraph(boolean clearGraph) {
        this.clearGraph = clearGraph;
    }

    /**
     * @return the clearGraph
     */
    public boolean isClearGraph() {
        return clearGraph;
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
     * @param clearDataset
     *            the clearDataset to set
     */
    public void setClearDataset(boolean clearDataset) {
        this.clearDataset = clearDataset;
    }

    /**
     * @param targetDataset
     *            the targetDataset to set
     */
    public void setTargetDataset(String targetDataset) {
        this.targetDataset = targetDataset;
    }

    /**
     * @return the targetDataset
     */
    public String getTargetDataset() {
        return targetDataset;
    }

    /**
     * @return the clearDataset
     */
    public boolean isClearDataset() {
        return clearDataset;
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

    /**
     *
     * @param timePeriodUris
     */
    private void harvestTimePeriods(Set<String> timePeriodUris) {

        if (timePeriodUris == null || timePeriodUris.isEmpty()) {
            return;
        }

        LOGGER.debug("Going to harvest time periods ...");
        TimePeriodsHarvester tpHarvester = new TimePeriodsHarvester(timePeriodUris);
        tpHarvester.execute();
        int harvestedCount = tpHarvester.getHarvestedCount();
        int newCount = tpHarvester.getNoOfNewPeriods();
        LOGGER.debug(harvestedCount + " time periods harvested, " + newCount + " of them were new");
    }

    /**
     * Post-import fix actions.
     *
     * @throws DAOException
     */
    private void postImportFixes() throws DAOException {

        // Fix groupless breakdowns and indicators.
        if (XLWrapUploadType.BREAKDOWN.equals(uploadType) || XLWrapUploadType.INDICATOR.equals(uploadType)) {
            DAOFactory.get().getDao(ScoreboardSparqlDAO.class).fixGrouplessCodelistItems();
        }
    }
}
