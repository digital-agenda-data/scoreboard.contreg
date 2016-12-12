package eionet.cr.dao;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import eionet.cr.dto.SearchResultDTO;
import eionet.cr.dto.SkosItemDTO;
import eionet.cr.staging.exp.ObjectTypes.DSD;
import eionet.cr.util.Pair;
import eionet.cr.util.SortingRequest;
import eionet.cr.util.pagination.PagingRequest;
import eionet.cr.web.util.ObservationFilter;

/**
 * A DAO interface for the SPARQL queries specific to the DG Connect's Digital Agenda Scoreboard project.
 *
 * @author jaanus
 */
public interface ScoreboardSparqlDAO extends DAO {

    /** */
    String OBSERVATION_URI_PREFIX = "http://semantic.digital-agenda-data.eu/data/";

    /** */
    String DATASET_URI_PREFIX = "http://semantic.digital-agenda-data.eu/dataset/";

    /** */
    String DATASET_STRUCTURE_PREFIX = "http://semantic.digital-agenda-data.eu/def/dsd/";

    /** */
    String DEFAULT_DSD_URI = "http://semantic.digital-agenda-data.eu/def/dsd/scoreboard";

    /** URI of the indicator groups codelist. */
    String IND_GROUP_CODELIST_URI = "http://semantic.digital-agenda-data.eu/codelist/indicator-group";

    /** URI of the indicator sources codelist. */
    String IND_SOURCE_CODELIST_URI = "http://semantic.digital-agenda-data.eu/codelist/source";

    /** */
    String DATASET_DISTRIBUTION_ACCESS_URL_TEMPLATE = "http://digital-agenda-data.eu/datasets/<dataset-identifier>#<distribution-type>";

    /**
     * Return URI-label pairs of codelists that have type skos:conceptScheme and whose URI starts with the given
     * input string.
     *
     * @param uriStartsWith
     *            The codelist URI must start with this string.
     * @return The URI-label pairs of codelists.
     * @throws DAOException
     *             If database access error happens.
     */
    List<Pair<String, String>> getCodelists(String uriStartsWith) throws DAOException;

    /**
     * Return all items in the codelist by the given URI.
     *
     * @param codelistUri
     *            Codelist URI.
     * @return List of items in the given codelist.
     * @throws DAOException
     *             If database access error happens.
     */
    List<SkosItemDTO> getCodelistItems(String codelistUri) throws DAOException;

    /**
     * Returns a list of observations filter values matching the given selections and starting after the given filter. Example:
     * if user changes the value of a particular filter (this is the one given in the method input), then all filters "below" it
     * must be re-populated. And the already made selections must be taken into account we well.
     *
     * @param selections The selections as indicated above.
     * @param filter The filter that user has just changed.
     * @param isAdmin True if the user is an administrator, otherwise false.
     * @return
     * @throws DAOException
     */
    List<Pair<String, String>> getFilterValues(Map<ObservationFilter, String> selections, ObservationFilter filter, boolean isAdmin)
            throws DAOException;

    /**
     *
     * @param identifier
     * @return
     * @throws DAOException
     */
    boolean datasetExists(String identifier) throws DAOException;

    /**
     * Exports codelist items of given RDF type into the given spreadsheet template file, using the given properties-to-columns map.
     *
     * @param itemType
     *            The RDF type of codelist items to export.
     * @param templateFile
     *            Reference to the spreadsheet template file to export into.
     * @param mappings
     *            Maps the properties of codelist items to corresponding spreadsheet columns (e.g. 0,1,2 ...)
     * @param targetFile
     *            The target spreadsheet file where the exported workbook will be saved to.
     * @return The number of items exported.
     * @throws DAOException
     *             Any sort of exception is wrapped into this one.
     */
    int exportCodelistItems(String itemType, File templateFile, Map<String, Integer> mappings, File targetFile) throws DAOException;

    /**
     * Removes all triples where the subject is the given subject URI and the predicate is dcterms:modified and the graph is the
     * given graph URI. Then adds a single triple where the subject, predicate and graph are the same, but the object is the given
     * date value.
     *
     * @param subjectUri
     *            The given subject URI.
     * @param date
     *            The given date value. If null, then current system date will be used.
     * @param graphUri
     *            The given graph URI.
     * @throws DAOException
     *             Any sort of exception that happens is wrapped into this one.
     */
    void updateDcTermsModified(String subjectUri, Date date, String graphUri) throws DAOException;

    /**
     * From the given subjects returns those that have the given property bound.
     *
     * @param propertyUri
     *            URI of property to check.
     * @param subjects
     *            Subjects to check.
     * @return Those of the checked subjects that have the given property bound.
     * @throws DAOException
     *             Any sort of exception that happens is wrapped into this one.
     */
    Set<String> getSubjectsWithBoundProperty(String propertyUri, Set<String> subjects) throws DAOException;

    /**
     * This is a post-import action for breakdowns, indicators and other codelist items that do not belong into any group and
     * were therefore wrongly mapped into e.g. http://semantic.digital-agenda-data.eu/codelist/breakdown-group/ as the group.
     * Correct solution should be to handle this in the Trig mapping file, but Trig does not seem to support such a construct.
     *
     * @throws DAOException
     *             Any sort of exception that happens is wrapped into this one.
     */
    void fixGrouplessCodelistItems() throws DAOException;

    /**
     * Gets all the distinct values of the given predicate used in DataCube observations.
     *
     * @param predicateUri URI of predicate whose values are to be returned.
     * @param isAdmin Only limited-access datasets are searched in if this is false (i.e. user is not an admin).
     * @param pageRequest The paging request.
     * @param sortRequest The sorting request
     * @param labelPredicates Predicates to be preferred as labels of the returned values.
     * @return A {@link SearchResultDTO} where the pairs represent uri-label pairs of the found values.
     * @throws DAOException
     */
    SearchResultDTO<Pair<String, String>> getObservationPredicateValues(String predicateUri, boolean isAdmin,
            PagingRequest pageRequest, SortingRequest sortRequest, String... labelPredicates) throws DAOException;

    /**
     * Returns all distinct DataCube datasets found in the system. Each dataset is represented by a {@link Pair<String, String>},
     * matching uri-label respectively.
     *
     * @param isAdmin Only limited-access datasets are searched in if this is false (i.e. user is not an admin).
     * @param pageRequest The paging request.
     * @param sortRequest The sorting request
     * @param labelPredicates Predicates to be preferred as labels of the returned values.
     * @return A {@link SearchResultDTO} where the pairs represent uri-label pairs of the found datasets.
     * @throws DAOException Any sort of exception that happens is wrapped into this one.
     */
    SearchResultDTO<Pair<String, String>> getDistinctDatasets(boolean isAdmin, PagingRequest pageRequest,
            SortingRequest sortRequest, String... labelPredicates) throws DAOException;

    /**
     * Changes the "http://www.w3.org/ns/adms#status" of the dataset by the given URI.
     *
     * @param uri The URI of the dataset whose status is to be changed.
     * @param newStatus The URI identifying the new status.
     * @throws DAOException Any sort of exception that happens is wrapped into this one.
     */
    void changeDatasetStatus(String uri, String newStatus) throws DAOException;

    /**
     * Returns compact metadata about the indicators matching the given indicator groups and indicator sources. The metadata is
     * returned as a list of {@link SkosItemDTO} where every member represents exactly one indicator.
     *
     * @param groupNotations List of SKOS notations of the indicator groups to match.
     * @param sourceNotations List of SKOS notations of the indicator sources to match
     * @return The list of matching indicators.
     * @throws DAOException Any sort of exception that happens is wrapped into this one.
     */
    List<SkosItemDTO> getIndicators(List<String> groupNotations, List<String> sourceNotations) throws DAOException;

    /**
     * Returns a list of URIs of all reference areas used by the DataCube observations in the given dataset in the given indicator.
     *
     * @param datasetUri The given dataset URI.
     * @param indicatorUri The given indicator URI.
     *
     * @return The list.
     * @throws DAOException Any sort of exception that happens is wrapped into this one.
     */
    List<String> getDistinctUsedRefAreas(String datasetUri, String indicatorUri) throws DAOException;

    /**
     *
     * @param datasetUri
     * @return
     * @throws DAOException
     */
    List<SkosItemDTO> getIndicatorSourcesUsedInDataset(String datasetUri) throws DAOException;

    /**
     *
     * @param datasetUri
     * @param sourceNotations
     * @return
     * @throws DAOException
     */
    List<SkosItemDTO> getIndicators(String datasetUri, List<String> sourceNotations) throws DAOException;

    /**
     * Returns a list of URIs of distinct datasets present in the triplestore.
     *
     * @return The list.
     * @throws DAOException Any sort of exception that happens is wrapped into this one.
     */
    List<String> getDistinctDatasetUris() throws DAOException;

    /**
     * Deletes all observations from the given dataset, matching the given indicators and time periods.
     * The logical operator between the list of indicators and the list of time periods is AND.
     * Dataset URI and indicator URIs are mandatory, time periods are not.
     *
     * @param datasetUri The given dataset URI.
     * @param indicatorUris The given indicator URIs.
     * @param timePeriodUris The given time period URIs.
     * @return Pair where left-side is the number of triples deleted, and right-side is the executed SPARQL delete statement.
     * @throws DAOException Any sort of exception that happens is wrapped into this one.
     */
    Pair<Integer, String> deleteObservations(Collection<String> datasetUri, Collection<String> indicatorUris, Collection<String> timePeriodUris)
            throws DAOException;

    /**
     * Returns earliest observation year of the given indicator in the given dataset.
     *
     * @param indicatorUri The indicator URI.
     * @param datasetUri The dataset URI.
     * @return Year of the earliest observation. Returns 0 if the year could not be determined.
     * @throws DAOException Any sort of exception that happens is wrapped into this one.
     */
    int getEarliestObservationYear(String indicatorUri, String datasetUri) throws DAOException;

    /**
     * Creates repository connection and value factory and then delegates call to
     * {@link #updateTouchedDatasets(Set, DSD, RepositoryConnection, ValueFactory)}.
     *
     * @param datasetUris
     * @param dsd
     * @throws RepositoryException
     */
    void updateTouchedDatasets(Set<String> datasetUris, DSD dsd) throws RepositoryException;

    /**
     * Updates given datasets after observations import into them.
     *
     * @param datasetUris Given dataset URIs.
     * @param dsd The DSD of given datasets.
     * @param repoConn
     * @param vf
     * @throws RepositoryException
     */
    void updateTouchedDatasets(Set<String> datasetUris, DSD dsd, RepositoryConnection repoConn, ValueFactory vf)
            throws RepositoryException;
}
