package eionet.cr.web.action.factsheet;

import eionet.cr.dao.*;
import eionet.cr.dto.FactsheetDTO;
import eionet.cr.dto.SkosItemDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.util.Pair;
import eionet.cr.web.action.AbstractSearchActionBean;
import eionet.cr.web.util.columns.SearchResultColumn;
import eionet.cr.web.util.tabs.FactsheetTabMenuHelper;
import eionet.cr.web.util.tabs.TabElement;
import eionet.cr.web.util.tabs.TabId;
import net.sourceforge.stripes.action.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
@UrlBinding("/datasetIndicators.action")
public class DatasetIndicatorsActionBean extends AbstractSearchActionBean<SubjectDTO> {

    /** */
    private static final Logger LOGGER = Logger.getLogger(DatasetIndicatorsActionBean.class);

    /** */
    private static final String JSP = "/pages/factsheet/datasetIndicators.jsp";

    /** */
    private String datasetUri;

    /** */
    private String timePeriodUri;

    /** */
    private String freeText;

    /** */
    private List<SkosItemDTO> indicators = new ArrayList<>();

    /** */
    private List<SkosItemDTO> timePeriods = new ArrayList<>();

    /** */
    private List<TabElement> tabs;
    private FactsheetDTO datasetFactsheetDTO;

    /** */
    private List<String> selIndicUris;
    private String executedDeletionSparql;

    @Override
    @DefaultHandler
    public Resolution search() throws DAOException {

        HelperDAO helperDAO = DAOFactory.get().getDao(HelperDAO.class);
        datasetFactsheetDTO = helperDAO.getFactsheet(datasetUri, null, null);

        if (StringUtils.isNotBlank(datasetUri)) {
            ScoreboardSparqlDAO ssDao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
            indicators = ssDao.getDatasetIndicators(datasetUri, timePeriodUri, freeText);
            timePeriods = ssDao.getDatasetTimePeriods(datasetUri);
        }

        FactsheetTabMenuHelper helper = new FactsheetTabMenuHelper(datasetUri, datasetFactsheetDTO, factory.getDao(HarvestSourceDAO.class));
        tabs = helper.getTabs(TabId.DATASET_INDICATORS);

        return new ForwardResolution(JSP);
    }

    public Resolution deleteSelected() throws DAOException {

        Resolution resolution = new RedirectResolution(this.getClass(), "search").addParameter("datasetUri", datasetUri);
        try {
            ScoreboardSparqlDAO ssDao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
            Pair<Integer, String> resultPair = ssDao.deleteObservations(datasetUri, selIndicUris, StringUtils.isBlank(timePeriodUri) ? null : Arrays.asList(timePeriodUri));
            int updateCount = resultPair.getLeft().intValue();
            executedDeletionSparql = resultPair.getRight();
            addSystemMessage("Operation successfully executed! Number of deleted triples: " + updateCount);
        } catch (DAOException e) {
            addWarningMessage("A technical error occurred: " + e.toString());
            LOGGER.error("Failed to delete observations", e);
            return new ForwardResolution(JSP);
        }

        return resolution;
    }

    public Resolution deleteAllMatching() throws DAOException {

        Resolution resolution = new RedirectResolution(this.getClass(), "search").addParameter("datasetUri", datasetUri);
        try {
            ScoreboardSparqlDAO ssDao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
            indicators = ssDao.getDatasetIndicators(datasetUri, timePeriodUri, freeText);
            List<String> indicatorUris = indicators.stream().map(i -> i.getUri()).collect(Collectors.toList());
            Pair<Integer, String> resultPair = ssDao.deleteObservations(datasetUri, indicatorUris, Arrays.asList(timePeriodUri));
            int updateCount = resultPair.getLeft().intValue();
            executedDeletionSparql = resultPair.getRight();
            addSystemMessage("Operation successfully executed! Number of deleted triples: " + updateCount);
        } catch (DAOException e) {
            addWarningMessage("A technical error occurred: " + e.toString());
            LOGGER.error("Failed to delete observations", e);
            return new ForwardResolution(JSP);
        }

        return new RedirectResolution(this.getClass(), "search").addParameter("datasetUri", datasetUri);
    }

    @Override
    public List<SearchResultColumn> getColumns() throws DAOException {
        return null;
    }

    public String getDatasetUri() {
        return datasetUri;
    }

    public void setDatasetUri(String datasetUri) {
        this.datasetUri = datasetUri;
    }

    public List<SkosItemDTO> getIndicators() {
        return indicators;
    }

    public List<SkosItemDTO> getTimePeriods() {
        return timePeriods;
    }

    public List<TabElement> getTabs() {
        return tabs;
    }

    public Class getFactsheetActionBeanClass() {
        return FactsheetActionBean.class;
    }

    public String getTimePeriodUri() {
        return timePeriodUri;
    }

    public void setTimePeriodUri(String timePeriodUri) {
        this.timePeriodUri = timePeriodUri;
    }

    public String getFreeText() {
        return freeText;
    }

    public void setFreeText(String freeText) {
        this.freeText = freeText;
    }

    public void setSelIndicUris(List<String> selIndicUris) {
        this.selIndicUris = selIndicUris;
    }

    public String getExecutedDeletionSparql() {
        return executedDeletionSparql;
    }
}
