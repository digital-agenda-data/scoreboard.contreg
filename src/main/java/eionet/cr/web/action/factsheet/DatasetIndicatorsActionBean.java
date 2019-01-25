package eionet.cr.web.action.factsheet;

import eionet.cr.dao.*;
import eionet.cr.dto.FactsheetDTO;
import eionet.cr.dto.SkosItemDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.web.action.AbstractSearchActionBean;
import eionet.cr.web.util.columns.SearchResultColumn;
import eionet.cr.web.util.tabs.FactsheetTabMenuHelper;
import eionet.cr.web.util.tabs.TabElement;
import eionet.cr.web.util.tabs.TabId;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@UrlBinding("/datasetIndicators.action")
public class DatasetIndicatorsActionBean extends AbstractSearchActionBean<SubjectDTO> {

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
}
