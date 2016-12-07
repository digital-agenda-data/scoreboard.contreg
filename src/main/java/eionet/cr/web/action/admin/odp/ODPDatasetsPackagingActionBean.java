package eionet.cr.web.action.admin.odp;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;

import eionet.cr.common.Predicates;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.dto.SearchResultDTO;
import eionet.cr.util.Pair;
import eionet.cr.util.odp.ODPAction;
import eionet.cr.util.odp.ODPDatasetsPacker;
import eionet.cr.web.action.AbstractActionBean;
import eionet.cr.web.action.admin.AdminWelcomeActionBean;
import eionet.cr.web.action.factsheet.FactsheetActionBean;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationMethod;

/**
 * Action bean for generating datasets' metadata packages for uploading into ODP (Open Data Portal, http://open-data.europa.eu).
 *
 * @author Jaanus
 */
@UrlBinding("/admin/odpPackaging.action")
public class ODPDatasetsPackagingActionBean extends AbstractActionBean {

    /** */
    private static final Logger LOGGER = Logger.getLogger(ODPDatasetsPackagingActionBean.class);

    /** */
    private static final String DATASETS_JSP = "/pages/admin/odp/datasets.jsp";

    /** */
    private static final String[] LABEL_PREDICATES = {Predicates.DCTERMS_TITLE, Predicates.RDFS_LABEL, Predicates.DC_TITLE,
            Predicates.FOAF_NAME};

    /** */
    private List<Pair<String, String>> datasets;

    /** */
    private List<String> selectedDatasets;

    /** */
    private ODPAction odpAction;

    /**
     * @return Resolution to go to.
     * @throws DAOException In case data access error occurs.
     */
    @DefaultHandler
    public Resolution defaultHandler() throws DAOException {

        return new ForwardResolution(DATASETS_JSP);
    }

    /**
     *
     * @return
     * @throws DAOException
     */
    public Resolution packAll() throws DAOException {

        List<Pair<String, String>> datasetPairs = getDatasets();
        if (CollectionUtils.isEmpty(datasetPairs)) {
            addCautionMessage("Found no datasets to pack!");
            return new ForwardResolution(DATASETS_JSP);
        }

        List<String> datasetUris = new ArrayList<>();
        for (Pair<String, String> pair : datasetPairs) {
            datasetUris.add(pair.getLeft());
        }

//        if (true) {
//            addCautionMessage(String.format("Found %d datasets to pack with action=%s, but packing not implemented yet :)", datasetUris.size(), odpAction));
//            return new ForwardResolution(DATASETS_JSP);
//        }

        try {
            return generateAndStream(datasetUris);
        } catch (DAOException e) {
            return new ForwardResolution(DATASETS_JSP);
        }
    }

    /**
     * @return
     * @throws DAOException
     */
    public Resolution packSelected() throws DAOException {

        if (CollectionUtils.isEmpty(selectedDatasets)) {
            addCautionMessage("No datasets selected!!");
            return new ForwardResolution(DATASETS_JSP);
        }

//        if (true) {
//            addCautionMessage(String.format("%d datasets selected with action=%s, but packing not implemented yet :)", selectedDatasets.size(), odpAction));
//            return new ForwardResolution(DATASETS_JSP);
//        }

        try {
            return generateAndStream(selectedDatasets);
        } catch (DAOException e) {
            return new ForwardResolution(DATASETS_JSP);
        }
    }

    /**
     *
     * @param datasetUris
     * @return
     * @throws DAOException
     */
    private StreamingResolution generateAndStream(List<String> datasetUris) throws DAOException {

        final ODPDatasetsPacker packer = new ODPDatasetsPacker(datasetUris, odpAction);
        try {
            packer.prepare();
        } catch (DAOException e) {
            if (e.getCause() == null) {
                addWarningMessage(e.getMessage());
            } else {
                addWarningMessage("Data access error occurred: " + e.getMessage());
            }
            throw e;
        }

        String filename = String.format("ODP-%s-%s.zip", getUserName(), DateFormatUtils.format(new Date(), "yyyy.MM.dd-HHmm"));
        return new StreamingResolution("application/zip") {
            @Override
            public void stream(HttpServletResponse response) throws Exception {

                OutputStream outputStream = null;
                try {
                    outputStream = response.getOutputStream();
                    packer.execute(outputStream);
                } finally {
                    IOUtils.closeQuietly(outputStream);
                }
            }
        }.setFilename(filename);
    }

    /**
     * Returns the class representing {@link FactsheetActionBean}. Handy for use in JSP.
     *
     * @return The class.
     */
    public Class<FactsheetActionBean> getFactsheetActionBeanClass() {
        return FactsheetActionBean.class;
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
     * @return the datasets
     * @throws DAOException
     */
    public List<Pair<String, String>> getDatasets() throws DAOException {
        if (datasets == null) {
            ScoreboardSparqlDAO dao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
            SearchResultDTO<Pair<String, String>> searchResult =
                    dao.getDistinctDatasets(isUserLoggedIn(), null, null, LABEL_PREDICATES);
            datasets = searchResult == null ? new ArrayList<Pair<String, String>>() : searchResult.getItems();
        }
        return datasets;
    }

    /**
     *
     * @return
     */
    public ODPAction[] getOdpActions() {
        return ODPAction.values();
    }

    /**
     * @return the odpAction
     */
    public ODPAction getOdpAction() {
        return odpAction;
    }

    /**
     * @param odpAction the odpAction to set
     */
    public void setOdpAction(ODPAction odpAction) {
        this.odpAction = odpAction;
    }

    /**
     * @param selectedDatasets the selectedDatasets to set
     */
    public void setSelectedDatasets(List<String> selectedDatasets) {
        this.selectedDatasets = selectedDatasets;
    }
}
