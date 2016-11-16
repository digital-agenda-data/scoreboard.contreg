package eionet.cr.web.action.admin;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.util.Pair;
import eionet.cr.web.action.AbstractActionBean;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationMethod;

/**
 * An action that enables administrators to perform various delete operations on specified DataCube observations.
 *
 * @author Jaanus
 */
@UrlBinding("/admin/obsDelete.action")
public class ObservationDeletionActionBean extends AbstractActionBean {

    /** Static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ObservationDeletionActionBean.class);

    /** The default JSP to forward to. */
    private static final String DEFAULT_JSP = "/pages/admin/observationsDeletion.jsp";

    /** URI of the dataset where the observations should be deleted from. */
    private String datasetUris;

    /** Whitespace-separated URIs of indicators whose observations in specified dataset must be deleted. */
    private String indicatorUris;

    /**
     * Whitespace-separated URIs of the time-periods whose observations
     * in the above dataset and of the above indicators must be deleted.
     */
    private String timePeriodUris;

    /** List of the URIs of all available datasets. */
    private List<String> availableDatasetUris;

    /** The SPARQL delete statement that was executed. */
    private String executedSparql;

    /**
     * Default request handler.
     *
     * @return Resolution to go to.
     */
    @DefaultHandler
    public Resolution defaultHandler() {

        ForwardResolution resolution = new ForwardResolution(DEFAULT_JSP);
        if (!"POST".equalsIgnoreCase(getContext().getRequest().getMethod())) {
            return resolution;
        }

        // Validate supplied datasets.
        List<String> datasetUrls = null;
        try {
            datasetUrls = parseUrls(datasetUris);
            if (CollectionUtils.isEmpty(datasetUrls)) {
                addCautionMessage("At least one dataset is mandatory!!");
                return resolution;
            }
        } catch (MalformedURLException e) {
            addCautionMessage("At least one of the supplied indicators is invalid URL!");
            return resolution;
        }

        // Validate supplied indicators.
        List<String> indicatorUrls = null;
        try {
            indicatorUrls = parseUrls(indicatorUris);
        } catch (MalformedURLException e) {
            addCautionMessage("At least one of the supplied indicators is invalid URL!");
            return resolution;
        }

        // Validate supplied time periods.
        List<String> timePeriodUrls = null;
        try {
            timePeriodUrls = parseUrls(timePeriodUris);
        } catch (MalformedURLException e) {
            addCautionMessage("At least one of the supplied time periods is invalid URL!");
            return resolution;
        }

        // Perform execution.
        try {
            ScoreboardSparqlDAO dao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
            Pair<Integer, String> resultPair = dao.deleteObservations(datasetUrls, indicatorUrls, timePeriodUrls);
            int updateCount = resultPair.getLeft().intValue();
            executedSparql = resultPair.getRight();
            addSystemMessage("Operation successfully executed! Number of deleted triples: " + updateCount);
        } catch (DAOException e) {
            addWarningMessage("A technical error occurred: " + e.toString());
            LOGGER.error("Failed to delete observations", e);
        }

        return resolution;
    }

    /**
     * Parses the given string as a whitespace-separated list of URLs, and returns them as a list of strings.
     * Also performs URL validity checking by the constructor of {@link java.net.URL}. If any of them invalid, throws
     * {@link MalformedURLException}.
     *
     * @param str The string to parse.
     * @return The list of URLs.
     * @throws MalformedURLException if invalid URL is encountered.
     */
    private List<String> parseUrls(String str) throws MalformedURLException {

        ArrayList<String> resultList = new ArrayList<String>();
        if (!StringUtils.isBlank(str)) {

            String[] urls = StringUtils.split(str);
            for (String url : urls) {
                resultList.add(new URL(url).toString());
            }
        }
        return resultList;
    }

    /**
     * Lazy getter for the availableDatasetUris.
     *
     * @return The availableDatasetUris.
     * @throws DAOException When data access error.
     */
    public List<String> getAvailableDatasetUris() throws DAOException {

        if (availableDatasetUris == null) {
            availableDatasetUris = DAOFactory.get().getDao(ScoreboardSparqlDAO.class).getDistinctDatasetUris();
        }

        return availableDatasetUris;
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
     * @return the datasetUris
     */
    public String getDatasetUris() {
        return datasetUris;
    }

    /**
     * @param datasetUris the datasetUris to set
     */
    public void setDatasetUris(String datasetUri) {
        this.datasetUris = datasetUri;
    }

    /**
     * @return the indicatorUris
     */
    public String getIndicatorUris() {
        return indicatorUris;
    }

    /**
     * @param indicatorUris the indicatorUris to set
     */
    public void setIndicatorUris(String indicatorUris) {
        this.indicatorUris = indicatorUris;
    }

    /**
     * @return the timePeriodUris
     */
    public String getTimePeriodUris() {
        return timePeriodUris;
    }

    /**
     * @param timePeriodUris the timePeriodUris to set
     */
    public void setTimePeriodUris(String timePeriodUris) {
        this.timePeriodUris = timePeriodUris;
    }

    /**
     * @return the executedSparql
     */
    public String getExecutedSparql() {
        return executedSparql;
    }
}
