package eionet.cr.web.util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import eionet.cr.common.Predicates;

/**
 *
 * Type definition ...
 *
 * @author jaanus
 */
public enum ObservationFilter {

    DATASET(Predicates.DATACUBE_DATA_SET, "Dataset"), INDICATOR(Predicates.DAS_INDICATOR, "Indicator", false), TIME_PERIOD(
            Predicates.DAS_TIMEPERIOD, "Time period", false), BREAKDOWN(Predicates.DAS_BREAKDOWN, "Breakdown", false),
    UNIT_MEASURE(Predicates.DAS_UNITMEASURE, "Unit", false), REF_AREA(Predicates.DAS_REFAREA, "Ref. area", true);

    /** */
    private static final char[] WORD_SEPARATORS = {'_'};

    /** */
    private String predicate;

    /** */
    private String alias;

    /** */
    private String title;

    /** */
    private boolean isAnySupprted;

    /**
     *
     * Class constructor.
     *
     * @param predicate
     * @param title
     */
    private ObservationFilter(String predicate, String title) {
        this(predicate, title, false);
    }

    /**
     * Constructor.
     *
     * @param predicate
     * @param title
     */
    private ObservationFilter(String predicate, String title, boolean isAnySupprted) {

        this.predicate = predicate;
        this.title = title;
        this.isAnySupprted = isAnySupprted;
    }

    /**
     *
     * @return
     */
    public String getAlias() {
        if (alias == null) {
            alias = WordUtils.uncapitalize(WordUtils.capitalizeFully(name(), WORD_SEPARATORS)).replace("_", "");
        }
        return alias;
    }

    /**
     * @return the predicate
     */
    public String getPredicate() {
        return predicate;
    }

    /**
     *
     * @param alias
     * @return
     */
    public static ObservationFilter getByAlias(String alias) {

        ObservationFilter[] filters = ObservationFilter.values();
        for (int i = 0; i < filters.length; i++) {
            ObservationFilter filter = filters[i];
            if (filter.getAlias().equals(alias)) {
                return filter;
            }
        }

        return null;
    }

    /**
     *
     * @param filter
     * @return
     */
    public static ObservationFilter getNext(ObservationFilter filter) {

        ObservationFilter[] filters = ObservationFilter.values();
        for (int i = 0; i < filters.length; i++) {
            if (filters[i].equals(filter)) {
                try {
                    return filters[i + 1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     *
     * @param alias
     * @return
     */
    public static ObservationFilter getNextByAlias(String alias) {

        ObservationFilter[] filters = ObservationFilter.values();
        for (int i = 0; i < filters.length; i++) {
            if (filters[i].getAlias().equals(alias)) {
                try {
                    return filters[i + 1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return StringUtils.isNotBlank(title) ? title : getAlias();
    }

    /**
     * @return the isAnySupprted
     */
    public boolean isAnySupprted() {
        return isAnySupprted;
    }
}
