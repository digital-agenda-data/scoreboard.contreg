package eionet.cr.web.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eionet.cr.common.Predicates;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.dao.SearchDAO;
import eionet.cr.dto.SearchResultDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.util.Pair;
import eionet.cr.util.SortingRequest;
import eionet.cr.util.Util;
import eionet.cr.util.pagination.PagingRequest;
import eionet.cr.web.action.factsheet.FactsheetActionBean;
import eionet.cr.web.util.CustomPaginatedList;
import eionet.cr.web.util.ObservationFilter;

/**
 * An action bean for browsing the available DataCube observations (i.e. resources of type
 * http://purl.org/linked-data/cube#Observation).
 *
 * @author jaanus
 */
@UrlBinding("/observations")
public class BrowseObservationsActionBean extends DisplaytagSearchActionBean {

    /** */
    private static final Logger LOGGER = Logger.getLogger(BrowseObservationsActionBean.class);

    /** */
    private static final String FILTER_VALUES_ATTR_NAME_TEMPLATE = BrowseObservationsActionBean.class.getSimpleName()
            + ".alias.values";

    /** */
    private static final List<HashMap<String, String>> AVAIL_COLUMNS = createAvailColumns();

    /** */
    private static final String JSP = "/pages/browseObservations.jsp";

    /** */
    private CustomPaginatedList<SubjectDTO> observations;

    /** */
    private Map<ObservationFilter, String> selections = new LinkedHashMap<ObservationFilter, String>();

    /** */
    private String applyFilter;

    /**
     * @throws DAOException
     *
     */
    @DefaultHandler
    public Resolution search() throws DAOException {

        ScoreboardSparqlDAO scoreboardSparqlDao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
        ObservationFilter filter = ObservationFilter.getNextByAlias(applyFilter);
        if (filter == null) {
            filter = ObservationFilter.values()[0];
        }

        while (filter != null) {

            List<Pair<String, String>> filterValues = scoreboardSparqlDao.getFilterValues(selections, filter, isUserLoggedIn());

            boolean anySupprted = filter.isAnySupprted();
            if (anySupprted) {
                filterValues.add(0, new Pair<String, String>("", "any"));
            }

            if (CollectionUtils.isNotEmpty(filterValues)) {

                String selValue = selections.get(filter);
                boolean selValueBlank = StringUtils.isBlank(selValue);
                if ((selValueBlank && !anySupprted) || (!selValueBlank && !filterValuesContains(filterValues, selValue))) {
                    selections.put(filter, filterValues.iterator().next().getLeft());
                }
            }

            getContext().setSessionAttribute(getSessionAttrName(filter.getAlias()), filterValues);
            filter = ObservationFilter.getNext(filter);
        }

        SearchResultDTO<SubjectDTO> searchResult = new SearchResultDTO<SubjectDTO>(new ArrayList<SubjectDTO>(), 0);
        String sortPredicate = getColumnPredicateByAlias(sort);
        SortingRequest sortRequest = StringUtils.isBlank(sortPredicate) ? null : SortingRequest.create(sortPredicate, dir);
        PagingRequest pageRequest = PagingRequest.create(page);

        try {
            SearchDAO searchDao = DAOFactory.get().getDao(SearchDAO.class);
            LinkedHashMap<String, String> convertedSelections = convertFilterSelections(selections);

            if (MapUtils.isNotEmpty(convertedSelections)) {
                if (!Util.isAllNull(convertedSelections.values())) {
                    searchResult = searchDao.searchByFilters(convertedSelections, false, pageRequest, sortRequest, null, false);
                }
            }
        } catch (DAOException e) {
            LOGGER.error("Observation search error", e);
            addWarningMessage("A technical error occurred when searching by the given filters" + e.getMessage());
        }

        observations = new CustomPaginatedList<SubjectDTO>(this, searchResult, pageRequest.getItemsPerPage());
        return new ForwardResolution(JSP);
    }

    /**
     *
     * @return
     */
    public Resolution reset() {
        clearSession();
        return new RedirectResolution(getClass(), "search");
    }

    /**
     *
     * @param alias
     * @return
     */
    private String getColumnPredicateByAlias(String alias) {

        String predicate = null;
        if (StringUtils.isNotBlank(alias)) {
            for (HashMap<String, String> columnConf : AVAIL_COLUMNS) {
                if (alias.equals(columnConf.get("alias"))) {
                    predicate = columnConf.get("predicate");
                    break;
                }
            }
        }

        return predicate;
    }

    /**
     *
     */
    private void clearSession() {

        HttpSession session = getContext().getRequest().getSession();
        ObservationFilter[] filters = ObservationFilter.values();
        for (int i = 0; i < filters.length; i++) {

            ObservationFilter filter = filters[i];
            session.removeAttribute(getSessionAttrName(filter.getAlias()));
        }
    }

    /**
     *
     * @param filterAlias
     * @return
     */
    private static String getSessionAttrName(String filterAlias) {
        return StringUtils.replace(FILTER_VALUES_ATTR_NAME_TEMPLATE, "alias", filterAlias);
    }

    /**
     *
     */
    @Before(stages = LifecycleStage.CustomValidation)
    public void loadSelectionsFromRequest() {

        ObservationFilter[] filters = ObservationFilter.values();
        for (int i = 0; i < filters.length; i++) {

            ObservationFilter filter = filters[i];
            String value = getContext().getRequestParameter(filter.getAlias());
            selections.put(filter, value);
        }
    }

    /**
     *
     * @return
     */
    public String getFilterValuesAttrNameTemplate() {
        return FILTER_VALUES_ATTR_NAME_TEMPLATE;
    }

    /**
     *
     * @return
     */
    public ObservationFilter[] getAvailFilters() {
        return ObservationFilter.values();
    }

    /**
     * @return the selections
     */
    public Map<ObservationFilter, String> getSelections() {
        return selections;
    }

    /**
     * @return the applyFilter
     */
    public String getApplyFilter() {
        return applyFilter;
    }

    /**
     * @param changedFilter
     *            the changedFilter to set
     */
    public void setApplyFilter(String changedFilter) {
        this.applyFilter = changedFilter;
    }

    /**
     * @return the observations
     */
    public CustomPaginatedList<SubjectDTO> getObservations() {
        return observations;
    }

    /**
     *
     * @return
     */
    public Class getFactsheetActionBeanClass() {
        return FactsheetActionBean.class;
    }

    /**
     *
     * @param filterValues
     * @param filterUri
     * @return
     */
    private boolean filterValuesContains(List<Pair<String, String>> filterValues, String filterUri) {

        for (Pair<String, String> pair : filterValues) {
            if (pair.getLeft().equals(filterUri)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param selections
     * @return
     */
    private LinkedHashMap<String, String> convertFilterSelections(Map<ObservationFilter, String> selections) {

        if (selections == null) {
            return null;
        }

        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
        if (!selections.isEmpty()) {
            for (Entry<ObservationFilter, String> entry : selections.entrySet()) {
                result.put(entry.getKey().getPredicate(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * @return the availColumns
     */
    public List<HashMap<String, String>> getAvailColumns() {
        return AVAIL_COLUMNS;
    }

    /**
     *
     * @return
     */
    private static List<HashMap<String, String>> createAvailColumns() {

        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();

        HashMap<String, String> map = new HashMap<String, String>();
        map = new HashMap<String, String>();
        map.put("alias", "indicator");
        map.put("predicate", Predicates.DAS_INDICATOR);
        map.put("title", "Indicator");
        map.put("hint", "Indicator");
        map.put("sortable", Boolean.TRUE.toString());
        map.put("width", "16%");
        list.add(map);

        map = new HashMap<String, String>();
        map.put("alias", "breakdown");
        map.put("predicate", Predicates.DAS_BREAKDOWN);
        map.put("title", "Breakdown");
        map.put("hint", "Breakdown");
        map.put("sortable", Boolean.TRUE.toString());
        map.put("width", "16%");
        list.add(map);

        map = new HashMap<String, String>();
        map.put("alias", "refArea");
        map.put("predicate", Predicates.DAS_REFAREA);
        map.put("title", "Ref. area");
        map.put("hint", "Reference area");
        map.put("sortable", Boolean.TRUE.toString());
        map.put("width", "16%");
        list.add(map);

        map = new HashMap<String, String>();
        map.put("alias", "timePeriod");
        map.put("predicate", Predicates.DAS_TIMEPERIOD);
        map.put("title", "Time period");
        map.put("hint", "Time period");
        map.put("sortable", Boolean.TRUE.toString());
        map.put("width", "16%");
        list.add(map);

        map = new HashMap<String, String>();
        map.put("alias", "unit");
        map.put("predicate", Predicates.DAS_UNITMEASURE);
        map.put("title", "Unit");
        map.put("hint", "Unit of measure");
        map.put("sortable", Boolean.TRUE.toString());
        map.put("width", "16%");
        list.add(map);

        map = new HashMap<String, String>();
        map.put("alias", "obsValue");
        map.put("predicate", Predicates.SDMX_OBSVALUE);
        map.put("title", "Value");
        map.put("hint", "Observed value");
        map.put("sortable", Boolean.FALSE.toString());
        map.put("width", "16%");
        list.add(map);

        return Collections.unmodifiableList(list);
    }
}
