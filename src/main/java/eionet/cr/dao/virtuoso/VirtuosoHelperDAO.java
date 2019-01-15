package eionet.cr.dao.virtuoso;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

import eionet.cr.common.Namespace;
import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.config.GeneralConfig;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.readers.DeliverySearchPicklistReader;
import eionet.cr.dao.readers.FactsheetReader;
import eionet.cr.dao.readers.MapReader;
import eionet.cr.dao.readers.ObjectLabelReader;
import eionet.cr.dao.readers.PredicateLabelsReader;
import eionet.cr.dao.readers.RDFExporter;
import eionet.cr.dao.readers.RecentFilesReader;
import eionet.cr.dao.readers.RecentUploadsReader;
import eionet.cr.dao.readers.ResultSetReaderException;
import eionet.cr.dao.readers.SPOReader;
import eionet.cr.dao.readers.SubPropertiesReader;
import eionet.cr.dao.readers.TriplesReader;
import eionet.cr.dao.readers.UploadDTOReader;
import eionet.cr.dao.util.PredicateLabels;
import eionet.cr.dao.util.SubProperties;
import eionet.cr.dao.util.UriLabelPair;
import eionet.cr.dao.virtuoso.helpers.ResourceRenameHandler;
import eionet.cr.dto.FactsheetDTO;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.dto.ObjectDTO;
import eionet.cr.dto.PredicateDTO;
import eionet.cr.dto.SearchResultDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.dto.TripleDTO;
import eionet.cr.dto.UploadDTO;
import eionet.cr.dto.UserBookmarkDTO;
import eionet.cr.dto.UserHistoryDTO;
import eionet.cr.util.Bindings;
import eionet.cr.util.FolderUtil;
import eionet.cr.util.Hashes;
import eionet.cr.util.ObjectLabelPair;
import eionet.cr.util.Pair;
import eionet.cr.util.SortOrder;
import eionet.cr.util.SortingRequest;
import eionet.cr.util.URIUtil;
import eionet.cr.util.URLUtil;
import eionet.cr.util.Util;
import eionet.cr.util.pagination.PagingRequest;
import eionet.cr.util.sesame.SPARQLQueryUtil;
import eionet.cr.util.sesame.SPARQLResultSetReader;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.PairReader;
import eionet.cr.util.sql.SQLUtil;
import eionet.cr.util.sql.SingleObjectReader;
import eionet.cr.web.security.CRUser;
import eionet.cr.web.util.HTMLSelectOption;
import eionet.cr.web.util.HTMLSelectOptionReader;
import eionet.cr.web.util.WebConstants;

// TODO: Auto-generated Javadoc
/**
 * Virtuoso DAO helper methods.
 */
public class VirtuosoHelperDAO extends VirtuosoBaseDAO implements HelperDAO {

    /** Static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(VirtuosoHelperDAO.class);

    /** SPARQL for last harvested files cache. */
    private static final String LATEST_FILES_SPARQL = SPARQLQueryUtil.getCrInferenceDefinitionStr()
            + "SELECT DISTINCT ?s ?l ?d WHERE {?s a <" + Predicates.CR_FILE + "> " + ". OPTIONAL { ?s <"
            + Predicates.CR_FIRST_SEEN + "> ?d } . OPTIONAL { ?s <" + Predicates.RDFS_LABEL + "> ?l } } "
            + "ORDER BY DESC(?d) LIMIT ?filesCount";

    /**
     * Returns latest harvested files (type=cr:File) in descending order (cr:firstSeen).
     *
     * @param limit
     *            count of latest files
     * @return List of Pair containing URL and date
     * @throws DAOException
     *             if query fails
     * @see eionet.cr.dao.HelperDAO#getLatestFiles(int)
     */
    @Override
    public List<Pair<String, String>> getLatestFiles(final int limit) throws DAOException {
        Bindings bindings = new Bindings();
        bindings.setInt("filesCount", limit);

        RecentFilesReader reader = new RecentFilesReader();
        reader.setBlankNodeUriPrefix(VirtuosoBaseDAO.BNODE_URI_PREFIX);

        return executeSPARQL(LATEST_FILES_SPARQL, bindings, reader);

    }

    /**
     * SPARQL for filling the latest subjects cache.
     */
    private static final String LATEST_SUBJECTS_SPARQL = "prefix dc:<" + Subjects.DUBLIN_CORE_SOURCE_URL + "> "
            + "select distinct ?s ?d where {?s a ?rdfType . " + "OPTIONAL { ?s dc:date ?d }} ORDER BY DESC(?d) LIMIT ?queryLimit";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getLatestSubjects(java.lang.String, int)
     */
    @Override
    public Collection<SubjectDTO> getLatestSubjects(String rdfType, int limit) throws DAOException {

        // validate arguments
        if (StringUtils.isBlank(rdfType)) {
            throw new IllegalArgumentException("rdfType must not be blank!");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0!");
        }
        RecentUploadsReader<String> matchReader = new RecentUploadsReader<String>();
        matchReader.setBlankNodeUriPrefix(VirtuosoBaseDAO.BNODE_URI_PREFIX);
        Bindings bindings = new Bindings();
        bindings.setURI("rdfType", rdfType);
        bindings.setInt("queryLimit", limit);
        logger.trace("Recent uploads search, executing subject finder query: " + LATEST_SUBJECTS_SPARQL);
        logger.trace("Recent uploads search, executing subject finder query, bindings: " + bindings.toString());

        List<String> subjectUris = executeSPARQL(LATEST_SUBJECTS_SPARQL, bindings, matchReader);
        Map<String, Date> pairMap = matchReader.getResultMap();

        List<SubjectDTO> resultList = new ArrayList<SubjectDTO>();

        // if subjectUris not empty, get the subjects data and set their dates
        if (subjectUris != null && !subjectUris.isEmpty()) {

            logger.trace("Recent uploads search, getting the data of the found subjects");

            // only these predicates will be queried for
            String[] neededPredicates = null;
            if (rdfType.equals(Subjects.ROD_OBLIGATION_CLASS)) {
                // properties for obligations
                String[] neededPredicatesObl =
                        {Predicates.RDFS_LABEL, Predicates.ROD_ISSUE_PROPERTY, Predicates.ROD_INSTRUMENT_PROPERTY};
                neededPredicates = neededPredicatesObl;
            } else if (rdfType.equals(Subjects.ROD_DELIVERY_CLASS)) {
                // properties for deliveries
                String[] neededPredicatesDeliveries =
                        {Predicates.RDFS_LABEL, Predicates.ROD_OBLIGATION_PROPERTY, Predicates.ROD_LOCALITY_PROPERTY};
                neededPredicates = neededPredicatesDeliveries;
            }

            // get the subjects data
            resultList = getFoundSubjectsData(subjectUris, neededPredicates);

            // set dublin core date of found subjects
            if (pairMap != null) {
                for (SubjectDTO subject : resultList) {
                    subject.setDcDate(pairMap.get(subject.getUri()));
                }
            }
        }
        return resultList;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getSubjectsNewerThan(java.util.Date, int)
     */
    @Override
    public List<SubjectDTO> getSubjectsNewerThan(Date timestamp, int limit) throws DAOException {
        throw new UnsupportedOperationException("Method not implemented");

    }

    /**
     * SPARQL for picklist cache of the given predicate.
     */
    private static final String PREDICATE_PICKLIST_SPARQL = "SELECT DISTINCT ?label ?object WHERE { ?s ?predicateUri ?object "
            + "OPTIONAL {?object <" + Predicates.RDFS_LABEL + "> ?label }} ORDER BY ?label";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getPicklistForPredicate(java.lang.String, boolean)
     */
    @Override
    public Collection<ObjectLabelPair> getPicklistForPredicate(String predicateUri, boolean extractLabels) throws DAOException {
        if (StringUtils.isBlank(predicateUri)) {
            return Collections.emptyList();
        }

        Bindings bindings = new Bindings();
        bindings.setURI("predicateUri", predicateUri);

        long startTime = System.currentTimeMillis();
        logger.trace("getPicklistForPredicate query: " + PREDICATE_PICKLIST_SPARQL);
        logger.trace("getPicklistForPredicate predicate: " + bindings);

        ObjectLabelReader reader = new ObjectLabelReader(extractLabels);
        // execute the query, with the IN parameters
        executeSPARQL(PREDICATE_PICKLIST_SPARQL, bindings, reader);

        logger.trace("getPicklistForPredicate query took " + Util.durationSince(startTime));

        return reader.getResultList();

    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#addTriples(eionet.cr.dto.SubjectDTO)
     */
    @Override
    public void addTriples(SubjectDTO subjectDTO) throws DAOException {
        RepositoryConnection conn = null;
        try {

            conn = SesameUtil.getRepositoryConnection();

            addTriples(conn, subjectDTO);

        } catch (RepositoryException e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SesameUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#addTriples(org.openrdf.repository.RepositoryConnection, eionet.cr.dto.SubjectDTO)
     */
    @Override
    public void addTriples(RepositoryConnection conn, SubjectDTO subjectDTO) throws DAOException, RepositoryException {
        URI sub = conn.getValueFactory().createURI(subjectDTO.getUri());

        for (String predicateUri : subjectDTO.getPredicateUris()) {
            URI pred = conn.getValueFactory().createURI(predicateUri);

            Collection<ObjectDTO> objects = subjectDTO.getObjects(predicateUri);
            if (objects != null && !objects.isEmpty()) {
                for (ObjectDTO object : objects) {

                    String sourceUri = object.getSourceUri();
                    URI source = conn.getValueFactory().createURI(sourceUri);

                    if (object.isLiteral()) {
                        // Literal can't have both language and type
                        if (!StringUtils.isBlank(object.getLanguage())) {
                            Literal literalObject = conn.getValueFactory().createLiteral(object.toString(), object.getLanguage());
                            conn.add(sub, pred, literalObject, source);
                        } else {
                            Literal literalObject = conn.getValueFactory().createLiteral(object.toString(), object.getDatatype());
                            conn.add(sub, pred, literalObject, source);
                        }
                    } else {
                        URI resourceObject = conn.getValueFactory().createURI(object.toString());
                        conn.add(sub, pred, resourceObject, source);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#addTriples(java.lang.String, java.lang.String, java.lang.String[], java.lang.String[], int)
     */
    @Override
    public int addTriples(String constructQuery, String context, String[] defaultGraphUris, String[] namedGraphUris, int limit)
            throws DAOException {

        int ret = 0;
        RepositoryConnection conn = null;
        try {
            conn = SesameUtil.getRepositoryConnection();
            GraphQuery queryObject = conn.prepareGraphQuery(QueryLanguage.SPARQL, constructQuery);
            SesameUtil.setDatasetParameters(queryObject, conn, defaultGraphUris, namedGraphUris);

            URI source = conn.getValueFactory().createURI(context);
            GraphQueryResult bindings = queryObject.evaluate();
            while (bindings.hasNext() && ret < limit) {
                Statement statement = bindings.next();
                conn.add(statement.getSubject(), statement.getPredicate(), statement.getObject(), source);
                ret++;
            }

        } catch (Exception e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SesameUtil.close(conn);
        }
        return ret;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#addResource(java.lang.String, java.lang.String)
     */
    @Override
    public void addResource(String uri, String sourceUri) throws DAOException {
        throw new UnsupportedOperationException("Method not implemented");

    }

    /**
     * SPARQL for properties defined by Dublin Core.
     */
    private static final String PROPS_DUBLINCORE_QUERY = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " + "select distinct ?object ?label ?label as ?title where "
            + "{?object rdfs:label ?label . ?object rdf:type rdf:Property " + ". ?object rdfs:isDefinedBy <"
            + Subjects.DUBLIN_CORE_SOURCE_URL + ">}";

    /**
     * Search for predicates that is allowed to edit on factsheet page.
     *
     * @param subjectUri
     *            rdf:type resources of the subject
     * @return the list of properties that can be added by user.
     * @throws DAOException
     *             if query fails
     */
    @Override
    public HashMap<String, String> getAddibleProperties(String subjectUri) throws DAOException {

        HashMap<String, String> result = new HashMap<String, String>();

        Bindings bindings = new Bindings();
        bindings.setURI("subjectUri", subjectUri);
        List<String> subjectTypes =
                executeSPARQL("select distinct ?type where {?subjectUri a ?type}", bindings, new SingleObjectReader<String>());

        // Get DublinCore attributes.
        ObjectLabelReader reader = new ObjectLabelReader(true);
        executeSPARQL(PROPS_DUBLINCORE_QUERY, reader);

        // Get the properties for given subject types.
        if (subjectUri != null && !subjectUri.isEmpty()) {

            bindings = new Bindings();
            String subjectTypesCSV = SPARQLQueryUtil.urisToCSV(subjectTypes, "subjectValue", bindings);
            String sparql =
                    "PREFIX rdfs: <" + Namespace.RDFS.getUri() + "> "
                            + "select distinct ?object ?label WHERE { ?object rdfs:label ?label . ?object rdfs:domain ?o "
                            + ". FILTER (?o IN (" + subjectTypesCSV + "))}";
            executeSPARQL(sparql, bindings, reader);
        }

        if (reader != null && reader.getResultList() != null) {
            for (ObjectLabelPair objectLabelPair : reader.getResultList()) {
                result.put(objectLabelPair.getLeft(), objectLabelPair.getRight());
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getAddibleProperties(java.lang.String, java.util.Collection)
     */
    @Override
    public Map<String, HTMLSelectOption> getAddibleProperties(String resourceUri, Collection<String> resourceTypes)
            throws DAOException {

        // Initiate the result object.
        HashMap<String, HTMLSelectOption> result = new HashMap<String, HTMLSelectOption>();

        if (CollectionUtils.isNotEmpty(resourceTypes)) {

            // First, get all properties whose rdfs:domain is in the fiven types.

            Bindings bindings = new Bindings();
            String typesCSV = SPARQLQueryUtil.urisToCSV(resourceTypes, "typeValue", bindings);
            String sparql =
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
                            + "select distinct ?object ?label where { ?object rdfs:domain ?o " + "filter (?o IN (" + typesCSV
                            + ")) optional {?object rdfs:label ?label}}";

            HTMLSelectOptionReader reader = new HTMLSelectOptionReader();
            executeSPARQL(sparql, bindings, reader);

            // Second, get all properties already present for the given resource.
            bindings = new Bindings();
            String urisToCSV = SPARQLQueryUtil.urisToCSV(Collections.singleton(resourceUri), "uriValue", bindings);
            // @formatter:off
            sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                    "select\n" +
                    "  distinct ?object ?label\n" +
                    "where {\n" +
                    "  ?s ?object ?value\n" +
                    "  filter (?s in (" + urisToCSV + "))\n" +
                    "  optional {?object rdfs:label ?label}\n" +
                    "}";
            // @formatter:on
            executeSPARQL(sparql, bindings, reader);

            // Finally, sort out the duplicates (i.e. an option by the same value is considered duplicate).
            if (reader != null && reader.getResultList() != null) {
                for (HTMLSelectOption option : reader.getResultList()) {
                    result.put(option.getValue(), option);
                }
            }
        }

        return result;
    }

    /**
     * SPARQL for getting schema URI of the subject.
     */
    private static final String SUBJECT_SCHEMA_SPARQL = "select distinct ?o " + "where { ?subjectUri <" + Predicates.CR_SCHEMA
            + "> ?o } limit 1";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getSubjectSchemaUri(java.lang.String)
     */
    @Override
    public String getSubjectSchemaUri(String subjectUri) throws DAOException {

        if (StringUtils.isBlank(subjectUri)) {
            return null;
        }
        Bindings bindings = new Bindings();
        bindings.setURI("subjectUri", subjectUri);

        List<String> objectUri = executeSPARQL(SUBJECT_SCHEMA_SPARQL, bindings, new SingleObjectReader<String>());

        return (objectUri != null && objectUri.size() > 0) ? objectUri.get(0) : null;
    }

    /**
     * SPARQL for getting predicates used for type.
     */
    private static final String PREDICATES_FOR_TYPE_SPARQL = "SELECT ?p as ?" + PairReader.LEFTCOL + " bif:min(?label) as ?"
            + PairReader.RIGHTCOL
            + " WHERE {?s a ?rdfType. ?s ?p ?o OPTIONAL {?p <http://www.w3.org/2000/01/rdf-schema#label> ?label}}"
            + " GROUP BY ?p ORDER BY ?" + PairReader.RIGHTCOL;

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getPredicatesUsedForType(java.lang.String)
     */
    @Override
    public List<Pair<String, String>> getPredicatesUsedForType(String typeUri) throws DAOException {

        Bindings bindings = new Bindings();
        bindings.setURI("rdfType", typeUri);

        // Execute the query, with the given bindings.
        List<Pair<String, String>> result = executeSPARQL(PREDICATES_FOR_TYPE_SPARQL, bindings, new PairReader<String, String>());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getSpatialSources()
     */
    @Override
    public List<String> getSpatialSources() throws DAOException {
        throw new UnsupportedOperationException("Method not implemented");

    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getSubject(java.lang.String)
     */
    @Override
    public SubjectDTO getSubject(String subjectUri) throws DAOException {
        return StringUtils.isBlank(subjectUri) ? null : findSubject(subjectUri);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getPredicateLabels(java.util.Set)
     */
    @Override
    public PredicateLabels getPredicateLabels(Set<Long> subjectHashes) throws DAOException {

        // TODO: implement this method
        return null;
    }

    /**
     * Finds SubProperties of given subject list.
     *
     * @param subjects
     *            Collection<String> subject URIs
     * @return SubProperties
     * @throws DAOException
     *             if query fails
     */
    @Override
    public SubProperties getSubProperties(final Collection<String> subjects) throws DAOException {
        Bindings bindings = new Bindings();
        String sparql =
                "select ?p ?s    WHERE { ?p <" + Predicates.RDFS_SUBPROPERTY_OF + ">  ?s " + "FILTER (?s IN ("
                        + SPARQLQueryUtil.urisToCSV(subjects, "subjectValue", bindings) + ") ) }";

        SubProperties subProperties = new SubProperties();
        SubPropertiesReader reader = new SubPropertiesReader(subProperties);
        executeSPARQL(sparql, bindings, reader);

        return subProperties;
    }

    /**
     * Self-explanatory constant name.
     */
    private static final String DELIVERY_SEARCH_PICKLIST_SPARQL = "SELECT DISTINCT ?li_uri ?li_title ?ro_title ?ro_uri  WHERE "
            + "{?ro_uri <" + Predicates.ROD_INSTRUMENT_PROPERTY + "> ?li_uri " + ". ?li_uri <" + Predicates.DCTERMS_ALTERNATIVE
            + "> ?li_title " + ". ?ro_uri <" + Predicates.DCTERMS_TITLE + "> ?ro_title } ORDER BY ?li_title ?ro_title";

    /**
     * Returns picklist for delivery search.
     *
     * @return The picklist.
     * @throws DAOException
     *             if query fails.
     */
    @Override
    public HashMap<UriLabelPair, ArrayList<UriLabelPair>> getDeliverySearchPicklist() throws DAOException {

        long startTime = System.currentTimeMillis();
        logger.trace("Delivery search picklist query: " + DELIVERY_SEARCH_PICKLIST_SPARQL);

        // TODO types
        DeliverySearchPicklistReader<HashMap<String, ArrayList<UriLabelPair>>> reader =
                new DeliverySearchPicklistReader<HashMap<String, ArrayList<UriLabelPair>>>();
        executeSPARQL(DELIVERY_SEARCH_PICKLIST_SPARQL, reader);

        logger.trace("Delivery search picklist query took " + Util.durationSince(startTime));

        // FIXME
        return reader.getResultMap();

    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#registerUserUrl(eionet.cr.web.security.CRUser, java.lang.String, boolean, java.lang.String)
     */
    @Override
    public void registerUserUrl(CRUser user, String url, boolean isBookmark, String label) throws DAOException {

        // input arguments sanity checking
        if (user == null || StringUtils.isBlank(user.getUserName())) {
            throw new IllegalArgumentException("user must not be null and must have user name");
        }
        if (!URLUtil.isURL(url)) {
            throw new IllegalArgumentException("url must not be null and must be valid URL");
        }

        // get the subject that the user wants to register
        SubjectDTO registeredSubject = getSubject(url);

        // if subject did not exist or it isn't registered in user's registrations yet,
        // then add the necessary triples
        if (registeredSubject == null || !registeredSubject.isRegisteredBy(user)) {

            // add the rdf:type=cr:File triple into user's registrations
            registeredSubject = new SubjectDTO(url, registeredSubject == null ? false : registeredSubject.isAnonymous());
            ObjectDTO objectDTO = new ObjectDTO(Subjects.CR_FILE, false);
            objectDTO.setSourceUri(user.getRegistrationsUri());
            registeredSubject.addObject(Predicates.RDF_TYPE, objectDTO);

            addTriples(registeredSubject);

            // add the URL into user's history
            SubjectDTO userHomeItemSubject = new SubjectDTO(user.getHomeItemUri(url), false);
            objectDTO = new ObjectDTO(Util.virtuosoDateToString(new Date()), true);
            objectDTO.setSourceUri(user.getHistoryUri());
            userHomeItemSubject.addObject(Predicates.CR_SAVETIME, objectDTO);

            objectDTO = new ObjectDTO(url, false);
            objectDTO.setSourceUri(user.getHistoryUri());
            userHomeItemSubject.addObject(Predicates.CR_HISTORY, objectDTO);

            // store the history and bookmark triples
            addTriples(userHomeItemSubject);

            // let the user home item subject URI be stored in RESOURCE
            // since user registrations and history URIs were used as triple source, add them to
            // HARVEST_SOURCE (set interval minutes to 0, to avoid it being background-harvested)
            HarvestSourceDAO harvestSourceDao = DAOFactory.get().getDao(HarvestSourceDAO.class);
            harvestSourceDao.addSourceIgnoreDuplicate(HarvestSourceDTO.create(user.getRegistrationsUri(), true, 0,
                    user.getUserName()));
            harvestSourceDao.addSourceIgnoreDuplicate(HarvestSourceDTO.create(user.getHistoryUri(), true, 0, user.getUserName()));
        }

        if (isBookmark) {
            if (!isSubjectUserBookmark(user, url)) {
                addUserBookmark(user, url, label);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#addUserBookmark(eionet.cr.web.security.CRUser, java.lang.String, java.lang.String)
     */
    @Override
    public void addUserBookmark(CRUser user, String url, String label) throws DAOException {
        if (user == null || StringUtils.isBlank(user.getUserName())) {
            throw new IllegalArgumentException("user must not be null and must have user name");
        }
        if (!URLUtil.isURL(url)) {
            throw new IllegalArgumentException("url must not be null and must be valid URL");
        }

        SubjectDTO userHomeItemSubject = new SubjectDTO(user.getHomeItemUri(url), false);

        ObjectDTO objectDTO = new ObjectDTO(url, false);
        objectDTO.setSourceUri(user.getBookmarksUri());
        userHomeItemSubject.addObject(Predicates.CR_BOOKMARK, objectDTO);

        ObjectDTO typeObjectDTO = new ObjectDTO(Predicates.CR_BOOKMARK_TYPE, false);
        typeObjectDTO.setSourceUri(user.getBookmarksUri());
        userHomeItemSubject.addObject(Predicates.RDF_TYPE, typeObjectDTO);

        if (!StringUtils.isBlank(label)) {
            ObjectDTO labelObjectDTO = new ObjectDTO(label, true);
            labelObjectDTO.setSourceUri(user.getBookmarksUri());
            userHomeItemSubject.addObject(Predicates.RDFS_LABEL, labelObjectDTO);
        }

        addTriples(userHomeItemSubject);

        // since user's bookmarks URI was used above as triple source, add it to HARVEST_SOURCE too
        // (but set harvest interval minutes to 0, since we don't really want it to be harvested )
        // by background harvester)
        DAOFactory.get().getDao(HarvestSourceDAO.class)
                .addSourceIgnoreDuplicate(HarvestSourceDTO.create(user.getBookmarksUri(), true, 0, user.getUserName()));

    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#deleteUserBookmark(eionet.cr.web.security.CRUser, java.lang.String)
     */
    @Override
    public void deleteUserBookmark(CRUser user, String uri) throws DAOException {

        // TripleDTO triple = new TripleDTO(user.getHomeItemUri(url), null, null);
        TripleDTO triple = new TripleDTO(uri, null, null);
        triple.setSourceUri(user.getBookmarksUri());
        deleteTriples(Collections.singletonList(triple));

    }

    /**
     *
     */
    private static final String USER_BOOKMARKS_SPARQL =
            "select distinct ?subject ?bookmarkUrl ?bookmarkLabel ?type ?query from ?userBookmarksUri where { " + "{ ?subject a <"
                    + Predicates.CR_BOOKMARK_TYPE + ">, ?type . " + "?subject <" + Predicates.CR_BOOKMARK + "> ?bookmarkUrl .  "
                    + "OPTIONAL {?subject <" + Predicates.RDFS_LABEL + "> ?bookmarkLabel } " + "} UNION { ?subject a <"
                    + Predicates.CR_SPARQL_BOOKMARK_TYPE + ">, ?type . " + "?subject  <" + Predicates.CR_SPARQL_QUERY
                    + "> ?query . " + "OPTIONAL {?subject  <" + Predicates.RDFS_LABEL + "> ?bookmarkLabel}"
                    + "}} order by xsd:string(?bookmarkLabel) ?bookmarkUrl";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getUserBookmarks(eionet.cr.web.security.CRUser)
     */
    @Override
    public List<UserBookmarkDTO> getUserBookmarks(String userBookmarksUri) throws DAOException {
        Bindings bindings = new Bindings();
        bindings.setURI("userBookmarksUri", userBookmarksUri);

        RepositoryConnection conn = null;
        TupleQueryResult queryResult = null;
        List<UserBookmarkDTO> resultList = new ArrayList<UserBookmarkDTO>();
        try {
            conn = SesameUtil.getRepositoryConnection();
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, USER_BOOKMARKS_SPARQL);
            bindings.applyTo(tupleQuery, conn.getValueFactory());

            queryResult = tupleQuery.evaluate();

            while (queryResult.hasNext()) {
                BindingSet bindingSet = queryResult.next();
                Value urlValue = bindingSet.getValue("bookmarkUrl");
                Value labelValue = bindingSet.getValue("bookmarkLabel");
                Value queryValue = bindingSet.getValue("query");
                Value typeValue = bindingSet.getValue("type");
                Value uriValue = bindingSet.getValue("subject");

                if (uriValue != null) {
                    UserBookmarkDTO bookmark = new UserBookmarkDTO();
                    bookmark.setUri(uriValue.stringValue());
                    bookmark.setType(typeValue.stringValue());
                    if (labelValue != null) {
                        bookmark.setBookmarkLabel(labelValue.stringValue());
                    }
                    if (Predicates.CR_BOOKMARK_TYPE.equals(bookmark.getType())) {
                        bookmark.setTypeLabel("Bookmark");
                        bookmark.setBookmarkUrl(urlValue.stringValue());
                    }
                    if (Predicates.CR_SPARQL_BOOKMARK_TYPE.equals(bookmark.getType())) {
                        bookmark.setTypeLabel("SPARQL Bookmark");
                        bookmark.setQuery(queryValue.stringValue());
                    }

                    resultList.add(bookmark);
                }

            }
        } catch (OpenRDFException e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SesameUtil.close(queryResult);
            SesameUtil.close(conn);
        }

        return resultList;
    }

    /**
     * SPARQL for checking if the URI is listed in user bookmarks.
     */
    private static final String SUBJECT_BOOKMARK_CHECK_QUERY = "select count(1)  where "
            + "{ graph ?g {?s ?userBookmark ?o . filter( isIRI(?o)) filter (?o = ?subjectValue) "
            + "filter (?g = ?userBookmarksFolder ) } }";

    /**
     * Checks if given subject is bookmarked in user bookmarks.
     *
     * @param user
     *            Content Registry user
     * @param subject
     *            Subject URI to be checked
     * @return boolean
     * @throws DAOException
     *             if query fails.
     * @see eionet.cr.dao.HelperDAO#isSubjectUserBookmark(eionet.cr.web.security. CRUser, long)
     */
    @Override
    public boolean isSubjectUserBookmark(CRUser user, String subject) throws DAOException {
        Bindings bindings = new Bindings();
        bindings.setURI("userBookmark", Predicates.CR_BOOKMARK);
        bindings.setIRI("subjectValue", subject);

        bindings.setURI("userBookmarksFolder", CRUser.bookmarksUri(user.getUserName()));

        // reader works only with Strings with Sesame/Virtuoso
        SingleObjectReader<String> reader = new SingleObjectReader<String>();

        executeSPARQL(
                SPARQLQueryUtil.isIRI(subject) ? SUBJECT_BOOKMARK_CHECK_QUERY : SPARQLQueryUtil.parseIRIQuery(
                        SUBJECT_BOOKMARK_CHECK_QUERY, "subjectValue"), bindings, reader);

        // resultlist contains 1 row including count of bookmark matches
        String urlCount = reader.getResultList().get(0);
        return Integer.valueOf(urlCount) > 0;
    }

    /**
     * Updates user history for this URI.
     *
     * @param user
     *            Content Registry user
     * @param url
     *            URI which record is updated
     * @throws DAOException
     *             if query fails.
     */
    @Override
    public void updateUserHistory(CRUser user, String url) throws DAOException {

        // if URL not yet in user history, add it there
        SubjectDTO userHomeItemSubject = new SubjectDTO(user.getHomeItemUri(url), false);
        ObjectDTO objectDTO = new ObjectDTO(url, false);
        objectDTO.setSourceUri(user.getHistoryUri());
        userHomeItemSubject.addObject(Predicates.CR_HISTORY, objectDTO);

        addTriples(userHomeItemSubject);

        // delete old history object
        List<TripleDTO> triples = new ArrayList<TripleDTO>();
        TripleDTO triple = new TripleDTO(user.getHomeItemUri(url), Predicates.CR_SAVETIME, null);
        triple.setSourceUri(user.getHistoryUri());
        triples.add(triple);

        deleteTriples(triples);

        // now add new save-time
        userHomeItemSubject = new SubjectDTO(user.getHomeItemUri(url), false);
        objectDTO = new ObjectDTO(Util.virtuosoDateToString(new Date()), true, XMLSchema.DATETIME);
        objectDTO.setSourceUri(user.getHistoryUri());
        userHomeItemSubject.addObject(Predicates.CR_SAVETIME, objectDTO);

        addTriples(userHomeItemSubject);

    }

    /**
     * SPARQL for user history items.
     */
    private static final String USER_HISTORY_QUERY = "PREFIX cr: <" + Namespace.CR.getUri() + "> "
            + "select ?url ?time FROM ?userHistoryGraph "
            + "WHERE {?s cr:userHistory ?url . ?s cr:userSaveTime ?time} order by desc(?time)";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getUserHistory(eionet.cr.web.security.CRUser)
     */
    @Override
    public List<UserHistoryDTO> getUserHistory(String historyUri) throws DAOException {
        Bindings bindings = new Bindings();
        bindings.setURI("userHistoryGraph", historyUri);
        List<UserHistoryDTO> returnHistory = new ArrayList<UserHistoryDTO>();
        MapReader reader = new MapReader();

        executeSPARQL(USER_HISTORY_QUERY, bindings, reader);

        if (reader.getResultList() != null && reader.getResultList().size() > 0) {
            for (Map<String, String> resultItem : reader.getResultList()) {
                UserHistoryDTO historyItem = new UserHistoryDTO();
                historyItem.setUrl(resultItem.get("url"));
                historyItem.setLastOperation(resultItem.get("time"));
                returnHistory.add(historyItem);
            }
        }
        return returnHistory;
    }

    /**
     * SPARQL returning example triples of an harvest source.
     */
    public static final String SAMPLE_TRIPLES_QUERY = "SELECT ?s ?p ?o FROM ?source WHERE {?s ?p ?o} LIMIT 100";

    /**
     * Returns 100 sample triples of an harvest source in random order.
     *
     * @param sourceUrl
     *            harvest source url
     * @param pagingRequest
     *            PAging request of the UI
     * @return list of TripleDTO objects.
     * @throws DAOException
     *             if query fails.
     */
    @Override
    public List<TripleDTO> getSampleTriplesInSource(String sourceUrl, PagingRequest pagingRequest) throws DAOException {

        Bindings bindings = new Bindings();
        bindings.setURI("source", sourceUrl);

        TriplesReader reader = new TriplesReader();
        return executeSPARQL(SAMPLE_TRIPLES_QUERY, bindings, reader);

    }

    /**
     * SPARQL returning triples of an harvest source.
     */
    public static final String SOURCE_TRIPLES_QUERY = "SELECT ?s ?p ?o FROM ?source WHERE {?s ?p ?o}";

    /**
     * Returns triples of an harvest source.
     *
     * @param sourceUrl
     *            harvest source url
     * @return list of TripleDTO objects.
     * @throws DAOException
     *             if query fails.
     */
    @Override
    public List<TripleDTO> getTriplesInSource(String sourceUrl) throws DAOException {

        Bindings bindings = new Bindings();
        bindings.setURI("source", sourceUrl);

        TriplesReader reader = new TriplesReader();
        return executeSPARQL(SOURCE_TRIPLES_QUERY, bindings, reader);

    }

    /**
     * Returns full SPO objects of an harvest source.
     *
     * @param sourceUrl
     *            harvest source url
     * @return list of SubjectDTO objects.
     * @throws DAOException
     *             if query fails.
     */
    @Override
    public List<SubjectDTO> getSPOsInSource(String sourceUrl) throws DAOException {

        Bindings bindings = new Bindings();
        bindings.setURI("source", sourceUrl);

        SPOReader reader = new SPOReader();
        return executeSPARQL(SOURCE_TRIPLES_QUERY, bindings, reader);

    }

    /**
     * SPARQL returning triples of a subject.
     */
    public static final String SUBJECT_TRIPLES_QUERY = "SELECT ?s ?p ?o WHERE {?s ?p ?o . filter (?s = ?subject)}";

    /**
     * Returns full SPO objects of given subject.
     *
     * @param subjectUri
     *            subject URI
     * @return list of SubjectDTO objects.
     * @throws DAOException
     *             if query fails.
     */
    @Override
    public List<SubjectDTO> getSPOsInSubject(String subjectUri) throws DAOException {

        if (StringUtils.isBlank(subjectUri)) {
            throw new IllegalArgumentException("Subject URI must not be blank!");
        }

        Bindings bindings = new Bindings();
        bindings.setURI("subject", subjectUri);
        return executeSPARQL(SUBJECT_TRIPLES_QUERY, bindings, new SPOReader());
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#deleteTriples(java.util.Collection)
     */
    @Override
    public void deleteTriples(Collection<TripleDTO> triples) throws DAOException {

        Connection sqlConn = null;
        RepositoryConnection repoConn = null;
        try {
            sqlConn = SesameUtil.getSQLConnection();
            repoConn = SesameUtil.getRepositoryConnection();
            for (TripleDTO triple : triples) {
                deleteTriple(triple, sqlConn, repoConn);
            }
        } catch (Exception e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SQLUtil.close(sqlConn);
            SesameUtil.close(repoConn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#deleteTriple(eionet.cr.dto.TripleDTO)
     */
    @Override
    public void deleteTriple(TripleDTO triple) throws DAOException {

        Connection sqlConn = null;
        RepositoryConnection repoConn = null;
        try {
            sqlConn = SesameUtil.getSQLConnection();
            repoConn = SesameUtil.getRepositoryConnection();
            deleteTriple(triple, sqlConn, repoConn);
        } catch (Exception e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SQLUtil.close(sqlConn);
            SesameUtil.close(repoConn);
        }
    }

    /**
     * Deletes a given triple, using SPARUL.
     * The triple is identified by its graph (i.e. source), subject, predicate and object's string representation.
     * The latter means that if you delete a triple whose object string is "11", then it will also delete the triple where
     * that data type is xsd:integer and value is 11!
     *
     * @param triple The triple object.
     * @param sqlConn The SQL connection against which the SPARUL statement will be executed.
     * @param repoConn The repository connection whose value factory will be used to validate subj-pred-obj-graph values.
     * @throws RepositoryException If any sort of repository error happens.
     * @throws SQLException If any sort of SQL error happens.
     */
    private void deleteTriple(TripleDTO triple, Connection sqlConn, RepositoryConnection repoConn) throws RepositoryException,
            SQLException {

        ValueFactory vf = repoConn.getValueFactory();
        URI subject = vf.createURI(triple.getSubjectUri());
        URI predicate = triple.getPredicateUri() == null ? null : vf.createURI(triple.getPredicateUri());
        URI graph = vf.createURI(triple.getSourceUri());

        String objectString = triple.getObject();
        Literal object = objectString == null ? null : vf.createLiteral(objectString);

        StringBuilder sb = new StringBuilder();
        sb.append("sparql \n");
        sb.append("delete from <" + graph.stringValue() + "> {\n");
        sb.append("   ?s ?p ?o\n");
        sb.append("}\n");
        sb.append("where {\n");
        sb.append("   ?s ?p ?o\n");
        sb.append("   filter (?s = <" + subject.stringValue() + ">)\n");
        if (predicate != null) {
            sb.append("   filter (?p = <" + predicate.stringValue() + ">)\n");
        }
        if (StringUtils.isNotBlank(triple.getObjectMd5())) {
            sb.append("   filter (bif:md5(str(?o)) = '" + triple.getObjectMd5() + "')");
        } else if (object != null) {
            sb.append("   filter (str(?o) = '''" + object.stringValue() + "''')");
        }

        sb.append("}");

        java.sql.Statement stmt = null;
        try {
            stmt = sqlConn.createStatement();
            stmt.executeUpdate(sb.toString());
        } finally {
            SQLUtil.close(stmt);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#deleteTriplesOfSource(long)
     */
    @Override
    public void deleteTriplesOfSource(long sourceHash) throws DAOException {
        throw new UnsupportedOperationException("Method not implemented");

    }

    /**
     * SPARQL for user uploaded files.
     */
    private static final String USER_UPLOADS_QUERY = "select ?s ?p ?o where { ?s ?p ?o. "
            + "{ select distinct ?s where { ?userBookmarksFolder ?hasFile ?s }}} order by ?s ?p ?o";

    /**
     * User uploaded files.
     *
     * @param crUser
     *            CR user
     * @return List of user upload items.
     * @throws DAOException
     *             if query fails.
     * @see eionet.cr.dao.HelperDAO#getUserUploads(eionet.cr.web.security.CRUser)
     */
    @Override
    public Collection<UploadDTO> getUserUploads(CRUser crUser) throws DAOException {

        if (crUser == null) {
            throw new IllegalArgumentException("User object must not be null");
        }

        if (StringUtils.isBlank(crUser.getUserName())) {
            throw new IllegalArgumentException("User name must not be blank");
        }
        Bindings bindings = new Bindings();
        bindings.setURI("userBookmarksFolder", crUser.getHomeUri());
        bindings.setURI("hasFile", Predicates.CR_HAS_FILE);

        UploadDTOReader reader = new UploadDTOReader();
        executeSPARQL(USER_UPLOADS_QUERY, bindings, reader);

        // loop through all the found uploads and make sure they all have the
        // label set
        Collection<UploadDTO> uploads = reader.getResultList();
        for (UploadDTO uploadDTO : uploads) {

            String currentLabel = uploadDTO.getLabel();
            String subjectUri = uploadDTO.getSubjectUri();
            String uriLabel = URIUtil.extractURILabel(subjectUri, SubjectDTO.NO_LABEL);
            uriLabel = StringUtils.replace(uriLabel, "%20", " ");

            if (StringUtils.isBlank(currentLabel) && !StringUtils.isBlank(uriLabel)) {
                uploadDTO.setLabel(uriLabel);
            } else {
                uploadDTO.setLabel(uriLabel + " (" + currentLabel + ")");
            }
        }

        return uploads;
    }

    @Override
    public List<UploadDTO> getAllRdfUploads() throws DAOException {
        StringBuilder sb = new StringBuilder();
        sb.append("PREFIX cr: <http://cr.eionet.europa.eu/ontologies/contreg.rdf#> ");
        sb.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ");
        sb.append("select ?file, ?label, ?lastModified, ?statements where { ");
        sb.append("?s cr:hasFile ?file . ");
        sb.append("OPTIONAL {?file rdfs:label ?label} . ");
        sb.append("?file cr:harvestedStatements ?statements. ");
        sb.append("?file cr:contentLastModified ?lastModified ");
        sb.append("FILTER (?statements > 0) }");

        SPARQLResultSetReader<UploadDTO> reader = new SPARQLResultSetReader<UploadDTO>() {

            List<UploadDTO> result = new ArrayList<UploadDTO>();

            @Override
            public List<UploadDTO> getResultList() {
                return result;
            }

            @Override
            public void endResultSet() {
            }

            @Override
            public void startResultSet(List<String> bindingNames) {
            }

            @Override
            public void readRow(BindingSet bindingSet) throws ResultSetReaderException {
                Value fileValue = bindingSet.getValue("file");
                Value titleValue = bindingSet.getValue("label");
                Value lastModifiedValue = bindingSet.getValue("lastModified");
                Value statements = bindingSet.getValue("statements");

                UploadDTO uploadDTO = new UploadDTO(fileValue.stringValue());
                if (titleValue != null) {
                    uploadDTO.setLabel(titleValue.stringValue());
                }
                uploadDTO.setDateModified(lastModifiedValue.stringValue());
                uploadDTO.setTriples(statements.stringValue());

                result.add(uploadDTO);
            }

        };
        executeSPARQL(sb.toString(), null, reader);
        return reader.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#isExistingSubject(java.lang.String)
     */
    @Override
    public boolean isExistingSubject(String subjectUri) throws DAOException {

        if (StringUtils.isBlank(subjectUri)) {
            throw new IllegalArgumentException("Subject uri must not be empty");
        }

        RepositoryConnection conn = null;
        try {
            conn = SesameUtil.getRepositoryConnection();
            RepositoryResult<Statement> result =
                    conn.getStatements(conn.getValueFactory().createURI(subjectUri), null, null, true);
            return result != null && result.hasNext();
        } catch (RepositoryException e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SesameUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#deleteSubjects(java.util.List)
     */
    @Override
    public void deleteUserUploads(String userName, List<String> subjectUris) throws DAOException {

        if (StringUtils.isBlank(userName) || subjectUris == null || subjectUris.isEmpty()) {
            return;
        }

        Connection sqlConn = null;
        RepositoryConnection repoConn = null;
        try {
            repoConn = SesameUtil.getRepositoryConnection();
            ValueFactory valueFactory = repoConn.getValueFactory();

            StringBuilder strBuilder = new StringBuilder();
            for (String subjectUri : subjectUris) {

                URI subjectResource = valueFactory.createURI(subjectUri);
                URI userHomeContext = valueFactory.createURI(CRUser.homeUri(userName));
                URI harvesterContext = valueFactory.createURI(GeneralConfig.HARVESTER_URI);

                // JH190511: although Sesame API claims that in RepositoryConnection.remove(...)
                // the context is optional, Virtuoso requires the context (i.e. the graph) always
                // to be specified in triple removal commands. Virtuoso Sesame driver seems to
                // silently ignore the whole command, if no context specified.
                repoConn.remove(subjectResource, null, null, userHomeContext, harvesterContext);
                repoConn.remove(null, null, (Value) subjectResource, userHomeContext, harvesterContext);

                if (strBuilder.length() > 0) {
                    strBuilder.append(",");
                }
                strBuilder.append(Hashes.spoHash(subjectUri));
            }

            sqlConn = getSQLConnection();
            SQLUtil.executeUpdate("delete from SPO_BINARY where SUBJECT in (" + strBuilder + ")", sqlConn);

        } catch (RepositoryException e) {
            throw new DAOException(e.toString(), e);
        } catch (SQLException e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SesameUtil.close(repoConn);
            SQLUtil.close(sqlConn);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#renameSubjects(java.util.Map)
     */
    @Override
    public void renameUserUploads(Map<String, String> renamings) throws DAOException {

        if (renamings == null || renamings.isEmpty()) {
            return;
        }

        RepositoryConnection repoConn = null;
        try {
            repoConn = SesameUtil.getRepositoryConnection();
            ResourceRenameHandler handler = new ResourceRenameHandler(repoConn, renamings);

            ValueFactory valueFactory = repoConn.getValueFactory();
            for (Map.Entry<String, String> entry : renamings.entrySet()) {

                String oldUriString = entry.getKey();
                URI oldUri = valueFactory.createURI(oldUriString);

                logger.debug("Exporting statements with old URI " + oldUri);

                // export statements where the old URI is present in subject field
                repoConn.exportStatements(oldUri, null, (Value) null, true, handler);

                // export statements where the old URI is present in object field
                repoConn.exportStatements(null, null, oldUri, true, handler);
            }

            logger.debug("Total count of exported statments: " + handler.getHandledStatementCount());
            handler.execute();

        } catch (OpenRDFException e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SesameUtil.close(repoConn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#readDistinctPredicates(java.lang.Long)
     */
    @Override
    public List<PredicateDTO> readDistinctPredicates(Long sourceHash) throws DAOException {
        throw new UnsupportedOperationException("Method not implemented");

    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#readDistinctSubjectUrls(java.lang.Long)
     */
    @Override
    public List<String> readDistinctSubjectUrls(Long sourceHash) throws DAOException {
        throw new UnsupportedOperationException("Method not implemented");

    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#outputSourceTriples(eionet.cr.dao.readers.RDFExporter )
     */
    @Override
    public void outputSourceTriples(RDFExporter reader) throws DAOException {
        throw new UnsupportedOperationException("Method not implemented");

    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#deleteTriples(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void deleteSubjectPredicates(Collection<String> subjectUris, Collection<String> predicateUris,
            Collection<String> sourceUris) throws DAOException {

        // If no subjects specified, then nothing to do here. Same applies to specified sources,
        // because Virtuoso requires the graph (i.e. context i.e. source) to be specified. Otherwise
        // it simply seems to ignore Sesame's triple removal API.
        if (subjectUris == null || subjectUris.isEmpty() || sourceUris == null || sourceUris.isEmpty()) {
            return;
        }

        RepositoryConnection conn = null;
        try {

            conn = SesameUtil.getRepositoryConnection();
            ValueFactory factory = conn.getValueFactory();

            if (predicateUris == null || predicateUris.isEmpty()) {

                for (String subjectUri : subjectUris) {
                    for (String sourceUri : sourceUris) {
                        // no null-pointer checking, because we don't want to allow nulls here anyway
                        URI subject = factory.createURI(subjectUri);
                        URI source = factory.createURI(sourceUri);
                        conn.remove(subject, null, null, source);
                    }
                }
            } else {
                for (String subjectUri : subjectUris) {
                    for (String predicateUri : predicateUris) {
                        for (String sourceUri : sourceUris) {

                            URI predicate = predicateUri == null ? null : factory.createURI(predicateUri);

                            // no null-pointer checking, because we don't want to allow nulls here anyway
                            URI subject = factory.createURI(subjectUri);
                            URI source = factory.createURI(sourceUri);

                            conn.remove(subject, predicate, null, source);
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SesameUtil.close(conn);
        }
    }

    /** */
    private static final String SPARQL_BOOKMARKS_SPARQL = "select ?subj ?label ?queryString " + "from ?bookmarksHome " + "where {"
            + "?subj ?rdfType ?sparqlBookmark" + ". ?subj ?rdfsLabel ?label" + ". ?subj ?sparqlQuery ?queryString"
            + "} order by xsd:string(?label)";

    /**
     * Returns SPARQL bookmarks of the user.
     *
     * @param user
     *            CR user whose bookmarks are returned
     * @return List<Map<String, String>>
     * @throws DAOException
     *             if query fails.
     */
    @Override
    public List<Map<String, String>> getSparqlBookmarks(CRUser user) throws DAOException {

        Bindings bindings = new Bindings();
        bindings.setURI("bookmarksHome", user.getBookmarksUri());
        bindings.setURI("rdfType", Predicates.RDF_TYPE);
        bindings.setURI("sparqlBookmark", Subjects.CR_SPARQL_BOOKMARK);
        bindings.setURI("rdfsLabel", Predicates.RDFS_LABEL);
        bindings.setURI("sparqlQuery", Predicates.CR_SPARQL_QUERY);

        return executeSPARQL(SPARQL_BOOKMARKS_SPARQL, bindings, new MapReader());
    }

    /**
     * Query returns project bookmarks and calculates project name from the bookmarks uri.
     */
    private static final String PROJECT_BOOKMARKS_SPARQL =
            "select fn:substring-before(fn:substring-after(?subj, ?projFolder), '/') as ?proj ?subj ?label ?queryString "
                    + "from ?bookmarksHome " + "where {" + "?subj ?rdfType ?sparqlBookmark" + ". ?subj ?rdfsLabel ?label"
                    + ". ?subj ?sparqlQuery ?queryString" + "} order by xsd:string(?label)";

    @Override
    public List<Map<String, String>> getProjectSparqlBookmarks(CRUser user) throws DAOException {

        List<String> userAccessibleProjects = FolderUtil.getUserAccessibleProjectFolderNames(user, "v");

        List<Map<String, String>> result = new ArrayList<Map<String, String>>();

        Bindings bindings = new Bindings();
        bindings.setURI("rdfType", Predicates.RDF_TYPE);
        bindings.setURI("sparqlBookmark", Subjects.CR_SPARQL_BOOKMARK);
        bindings.setURI("rdfsLabel", Predicates.RDFS_LABEL);
        bindings.setURI("sparqlQuery", Predicates.CR_SPARQL_QUERY);
        bindings.setString("projFolder", FolderUtil.getProjectsFolder() + "/");

        for (String projectName : userAccessibleProjects) {

            bindings.setURI("bookmarksHome", FolderUtil.getProjectFolder(projectName) + "/bookmarks");

            result.addAll(executeSPARQL(PROJECT_BOOKMARKS_SPARQL, bindings, new MapReader()));
        }

        return result;

    }

    /**
     * Returns shared SPARQL bookmarks.
     *
     * @return List<Map<String, String>>
     * @throws DAOException
     *             if query fails.
     */
    @Override
    public List<Map<String, String>> getSharedSparqlBookmarks() throws DAOException {

        Bindings bindings = new Bindings();
        bindings.setURI("bookmarksHome", GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL)
                + "/sparqlbookmarks");
        bindings.setURI("rdfType", Predicates.RDF_TYPE);
        bindings.setURI("sparqlBookmark", Subjects.CR_SPARQL_BOOKMARK);
        bindings.setURI("rdfsLabel", Predicates.RDFS_LABEL);
        bindings.setURI("sparqlQuery", Predicates.CR_SPARQL_QUERY);

        return executeSPARQL(SPARQL_BOOKMARKS_SPARQL, bindings, new MapReader());
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getSourceLastModifiedDates(java.util.Set)
     */
    @Override
    public Map<String, Date> getSourceLastModifiedDates(Set<String> resourceUris) throws DAOException {

        Bindings bindings = new Bindings();

        StringBuilder query = new StringBuilder();
        query.append("select distinct ?s ?date where {graph ?g {?s ?p ?o}. filter (?s in (")
                .append(SPARQLQueryUtil.urisToCSV(resourceUris, "sValue", bindings)).append(")). ?g <")
                .append(Predicates.CR_LAST_MODIFIED).append("> ?date}");

        HashMap<String, Date> result = new HashMap<String, Date>();
        MapReader reader = new MapReader();
        List<Map<String, String>> list = executeSPARQL(query.toString(), bindings, reader);
        if (list != null) {
            for (Map<String, String> map : list) {

                Date date = Util.virtuosoStringToDate(map.get("date"));
                if (date != null) {
                    String resourceUri = map.get("s");
                    Date currentDate = result.get(resourceUri);
                    if (currentDate == null || (date.compareTo(currentDate) > 0)) {
                        result.put(resourceUri, date);
                    }
                }
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getLiteralRangeSubjects(java.util.Set)
     */
    @Override
    public Set<String> getLiteralRangeSubjects(Set<String> subjectsToCheck) throws DAOException {
        // TODO Auto-generated method stub
        return new HashSet<String>();
    }

    /** */
    // @formatter:off
    private static final String GET_FACTSHEET_ROWS = ""
            + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
            + "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
            + "select ?pred min(xsd:int(isBlank(?s))) as ?anonSubj "
            + "min(bif:either(isLiteral(?o),"
            + "bif:concat(bif:substring(str(?o),1,LEN),'<|>',lang(?o),'<|>',str(datatype(?o)),'<|><|>0<|>',str(?g),'<|>',str(bif:length(str(?o))),'<|>',bif:md5(str(?o))),"
            + "bif:concat(bif:coalesce(str(coalesce(?rdfsLabel,?prefLabel,?altLabel,?notation)),bif:left(str(?o),LEN)),'<|>',lang(coalesce(?rdfsLabel,?prefLabel,?altLabel,?notation)),'<|>',str(datatype(coalesce(?rdfsLabel,?prefLabel,?altLabel,?notation))),'<|>',bif:left(str(?o),LEN),'<|>',str(isBlank(?o)),'<|>',str(?g),'<|><|>')"
            + ")) as ?objData "
            + "count(distinct ?o) as ?objCount "
            + "where {"
            + "graph ?g {"
            + "?s ?pred ?o. filter(?s=iri(?subjectUri)) filter(!isLiteral(?o) || bif:starts_with(lang(?o),'en')=1 || lang(?o)='')}. "
            + "optional {?o rdfs:label ?rdfsLabel. filter(bif:starts_with(lang(?rdfsLabel),'en')=1 || lang(?rdfsLabel)='')} "
            + "optional {?o skos:prefLabel ?prefLabel. filter(bif:starts_with(lang(?prefLabel),'en')=1 || lang(?prefLabel)='')} "
            + "optional {?o skos:altLabel ?altLabel. filter(bif:starts_with(lang(?altLabel),'en')=1 || lang(?altLabel)='')} "
            + "optional {?o skos:notation ?notation. filter(bif:starts_with(lang(?notation),'en')=1 || lang(?notation)='')} "
            + "} group by ?pred";

    /** */
    private static final String GET_PREDICATE_LABELS = "select distinct ?pred ?label where " + "{" + "?pred <"
            + Predicates.RDFS_LABEL + "> ?label" + ". {select distinct ?pred where {?subjectUri ?pred ?o}}"
            + "} order by ?pred ?label";

    /** */
    private static final String GET_PREDICATE_OBJECTS = "select ?obj ?objLabel ?g " + "where {graph ?g {"
            + "?s ?p ?obj. filter(?s=iri(?subjectUri) and ?p=iri(?predicateUri))}" + ". optional {?obj <" + Predicates.RDFS_LABEL
            + "> ?objLabel}" + "} order by str(bif:either(isLiteral(?obj),?obj,bif:coalesce(?objLabel,str(?obj)))) " + "limit "
            + PredicateObjectsReader.PREDICATE_PAGE_SIZE + " offset ";
    // @formatter:on

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getFactsheet(java.lang.String, java.util.List, java.util.Map)
     */
    @Override
    public FactsheetDTO getFactsheet(String subjectUri, List<String> acceptedLanguages, Map<String, Integer> predicatePages)
            throws DAOException {

        if (StringUtils.isBlank(subjectUri)) {
            throw new IllegalArgumentException("Subject uri must not be blank!");
        }

        String query = StringUtils.replace(GET_FACTSHEET_ROWS, "LEN", String.valueOf(WebConstants.MAX_OBJECT_LENGTH));
        query = StringUtils.replace(query, "<|>", FactsheetReader.OBJECT_DATA_SPLITTER);
        if (logger.isTraceEnabled()) {
            logger.trace("Executing factsheet query: " + query);
        }

        Bindings bindings = new Bindings();
        bindings.setString("subjectUri", subjectUri);

        FactsheetReader factsheetReader = new FactsheetReader(subjectUri);
        executeSPARQL(query, bindings, factsheetReader);
        FactsheetDTO factsheetDTO = factsheetReader.getFactsheetDTO();

        if (factsheetDTO != null) {

            PredicateLabelsReader predicateLabelsReader = new PredicateLabelsReader(acceptedLanguages);
            executeSPARQL(GET_PREDICATE_LABELS, bindings, predicateLabelsReader);
            predicateLabelsReader.fillPredicateLabels(factsheetDTO);

            if (predicatePages != null) {
                for (Map.Entry<String, Integer> entry : predicatePages.entrySet()) {

                    String predicateUri = entry.getKey();
                    int pageNum = entry.getValue() == null ? 0 : entry.getValue().intValue();
                    if (!StringUtils.isBlank(predicateUri) && pageNum > 0) {

                        int offset = PagingRequest.create(pageNum, PredicateObjectsReader.PREDICATE_PAGE_SIZE).getOffset();
                        bindings.setString("predicateUri", predicateUri);
                        PredicateObjectsReader predicateObjectsReader = new PredicateObjectsReader(acceptedLanguages);
                        List<ObjectDTO> objects = executeSPARQL(GET_PREDICATE_OBJECTS + offset, bindings, predicateObjectsReader);
                        if (objects != null && !objects.isEmpty()) {
                            factsheetDTO.setObjects(predicateUri, objects);
                        }
                    }
                }
            }
        }

        return factsheetDTO;
    }

    /** */
    private static final String SOURCE_COUNT_SPARQL = "SELECT COUNT(*) FROM ?sourceUri WHERE {?s ?p ?o}";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getHarvestedStatements(java.lang.String)
     */
    @Override
    public int getHarvestedStatements(String sourceUri) throws DAOException {
        Bindings bindings = new Bindings();
        bindings.setURI("sourceUri", sourceUri);

        // StringBuilder sparql = new StringBuilder();
        // sparql.append("SELECT COUNT(*)");
        // sparql.append("FROM <" + sourceUri + "> ");
        // sparql.append("WHERE {?s ?p ?o}");

        String result = executeUniqueResultSPARQL(SOURCE_COUNT_SPARQL, bindings, new SingleObjectReader<String>());
        return Integer.parseInt(result);
    }

    /** */
    private static final String IS_TABULAR_DATA_SUBJECT = "select distinct ?g " + "where {graph ?g {?subject ?p ?o}" + ". ?g <"
            + Predicates.CR_MEDIA_TYPE + "> ?mediaType" + ". filter (?mediaType in ('tsv','csv'))}";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#isTabularDataSubject(java.lang.String)
     */
    @Override
    public boolean isTabularDataSubject(String subjectUri) throws DAOException {

        Bindings bindings = new Bindings();
        bindings.setURI("subject", subjectUri);
        String graphUri = executeUniqueResultSPARQL(IS_TABULAR_DATA_SUBJECT, bindings, new SingleObjectReader<String>());
        return !StringUtils.isBlank(graphUri);
    }

    /** */
    private static final String GET_LIT_OBJ_VALUE = "select ?o where {graph ?g {?s ?p ?o. "
            + "filter(?g=iri(?gV) && ?s=iri(?sV) && ?p=iri(?pV) && isLiteral(?o) && bif:md5(str(?o))=?objMD5)}}";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getLiteralObjectValue(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String getLiteralObjectValue(String subjectUri, String predicateUri, String objectMD5, String graphUri) {

        if (StringUtils.isBlank(subjectUri) || StringUtils.isBlank(predicateUri) || StringUtils.isBlank(objectMD5)
                || StringUtils.isBlank(graphUri)) {
            logger.warn("Missing subject or predicate or objectMD5 or graph!");
            return null;
        }

        Bindings bindings = new Bindings();
        bindings.setString("gV", graphUri);
        bindings.setString("sV", subjectUri);
        bindings.setString("pV", predicateUri);
        bindings.setString("objMD5", objectMD5);

        try {
            return executeUniqueResultSPARQL(GET_LIT_OBJ_VALUE, bindings, new SingleObjectReader<String>());
        } catch (Exception e) {
            String msg = "Error when trying to retrieve the object value: ";
            logger.error(msg, e);
            return "Error when trying to retrieve the object value: " + e.toString();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#isGraphExists(java.lang.String)
     */
    @Override
    public boolean isGraphExists(String grpahUri) throws DAOException {

        RepositoryConnection conn = null;
        try {
            conn = SesameUtil.getRepositoryConnection();
            // Check whether at least one statement from the graph is present. includeInferred=false.
            // The value factory's createURI() will take care of type safety for us here.
            return conn.hasStatement(null, null, null, false, conn.getValueFactory().createURI(grpahUri));
        } catch (RepositoryException e) {
            throw new DAOException(e.toString(), e);
        } finally {
            SesameUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#deleteProjectBookmark(java.lang.String)
     */
    @Override
    public void deleteProjectBookmark(String uri) throws DAOException {
        TripleDTO triple = new TripleDTO(uri, null, null);
        triple.setSourceUri(StringUtils.substringBeforeLast(uri, "/"));
        deleteTriples(Collections.singletonList(triple));

    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HelperDAO#getUriLabels(java.lang.String, eionet.cr.util.pagination.PagingRequest,
     * eionet.cr.util.SortingRequest, java.lang.String[])
     */
    @Override
    public SearchResultDTO<Pair<String, String>> getUriLabels(String rdfType, PagingRequest pageRequest,
            SortingRequest sortRequest, String... labelPredicates) throws DAOException {

        if (!URIUtil.isURI(rdfType)) {
            throw new IllegalArgumentException("rdfType must not be blank and it must be a legal URI!");
        }

        if (sortRequest == null) {
            sortRequest = SortingRequest.create(PairReader.RIGHTCOL, SortOrder.ASCENDING);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("select distinct").append("\n");
        sb.append("  ?s as ?").append(PairReader.LEFTCOL).append("\n");

        if (ArrayUtils.isEmpty(labelPredicates)) {
            sb.append("  bif:subseq(str(?s), coalesce(bif:strrchr(bif:replace(str(?s),'/','#'),'#'),0)+1) as ?")
                    .append(PairReader.RIGHTCOL).append("\n");
        } else {
            sb.append("  coalesce(");
            for (int i = 0; i < labelPredicates.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("?label").append(i);
            }
            sb.append(", bif:subseq(str(?s), coalesce(bif:strrchr(bif:replace(str(?s),'/','#'),'#'),0)+1)) as ?")
                    .append(PairReader.RIGHTCOL).append("\n");
        }

        sb.append("where {").append("\n");
        sb.append("  ?s a ?rdfType filter(?rdfType = ?rdfTypeValue)").append("\n");

        Bindings bindings = new Bindings();
        bindings.setURI("rdfTypeValue", rdfType);

        String s = "  optional {?s ?labelPred0 ?label0}\n";
        for (int i = 0; i < labelPredicates.length; i++) {
            sb.append(StringUtils.replace(s, "0", String.valueOf(i)));
            bindings.setURI("labelPred" + i, labelPredicates[i]);
        }
        sb.append("}\n");
        sb.append("order by ").append(sortRequest.getSortOrder()).append("(?").append(sortRequest.getSortingColumnName())
                .append(")");

        if (pageRequest != null) {
            sb.append(" limit ").append(pageRequest.getItemsPerPage()).append(" offset ").append(pageRequest.getOffset());
        }

        List<Pair<String, String>> list = executeSPARQL(sb.toString(), bindings, new PairReader<String, String>());
        int totalMatchCount = list.size();
        if (pageRequest != null) {
            String countQuery = "select count(distinct ?s) where {?s a ?rdfType filter(?rdfType = ?rdfTypeValue)}";
            bindings = new Bindings();
            bindings.setURI("rdfTypeValue", rdfType);
            String count = executeUniqueResultSPARQL(countQuery, bindings, new SingleObjectReader<String>());
            totalMatchCount = NumberUtils.toInt(count);
        }

        return new SearchResultDTO<Pair<String, String>>(list, totalMatchCount);
    }
}
