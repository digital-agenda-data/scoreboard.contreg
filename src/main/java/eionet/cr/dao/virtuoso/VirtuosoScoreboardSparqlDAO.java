package eionet.cr.dao.virtuoso;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eionet.cr.dto.SubjectDTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.dao.readers.CodelistExporter;
import eionet.cr.dao.readers.SkosItemsReader;
import eionet.cr.dto.SearchResultDTO;
import eionet.cr.dto.SkosItemDTO;
import eionet.cr.util.Bindings;
import eionet.cr.util.Pair;
import eionet.cr.util.SortOrder;
import eionet.cr.util.SortingRequest;
import eionet.cr.util.URIUtil;
import eionet.cr.util.Util;
import eionet.cr.util.pagination.PagingRequest;
import eionet.cr.util.sesame.SPARQLQueryUtil;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.PairReader;
import eionet.cr.util.sql.SQLUtil;
import eionet.cr.util.sql.SingleObjectReader;
import eionet.cr.util.xlwrap.XLWrapUploadType;
import eionet.cr.web.util.ObservationFilter;

/**
 * A Virtuoso-specific implementation of {@link ScoreboardSparqlDAO}.
 *
 * @author jaanus
 */
public class VirtuosoScoreboardSparqlDAO extends VirtuosoBaseDAO implements ScoreboardSparqlDAO {

    /** Static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(VirtuosoScoreboardSparqlDAO.class);

    // @formatter:off

    /** The Constant GET_CODELISTS_SPARQL. */
    private static final String GET_CODELISTS_SPARQL = "" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
            "select\n" +
            "  ?uri as ?" + PairReader.LEFTCOL + " min(?prefLabel) as ?" + PairReader.RIGHTCOL + "\n" +
            "where {\n" +
            "  ?uri a skos:ConceptScheme.\n" +
            "  filter (strStarts(str(?uri), ?uriStartsWith))\n" +
            "  optional {?uri skos:prefLabel ?prefLabel}\n" +
            "}\n" +
            "group by ?uri order by ?uri";

    /** The Constant GET_CODELIST_ITEMS_SPARQL. */
    private static final String GET_CODELIST_ITEMS_SPARQL = "" +
            "PREFIX cube: <http://purl.org/linked-data/cube#>\n" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
            "select\n" +
            "  ?uri min(distinct ?notation) as ?skosNotation min(distinct  ?prefLabel) as ?skosPrefLabel\n" +
            "where {\n" +
            "  ?scheme skos:hasTopConcept ?uri \n" +
            "  filter (?scheme = ?schemeUri) \n" +
            "#dst  ?obs a cube:Observation . \n" +
            "#dst  ?obs cube:dataSet ?dataset . \n" +
            "#dst  ?obs ?pred ?uri \n" +
            "#dst  filter(?dataset = ?datasetUri) \n" +
            "#txt  ?uri ?p ?o \n" +
            "#txt  filter bif:contains(?o, ?objectVal) \n" +
            "  optional {?uri skos:notation ?notation} \n" +
            "  optional {?uri skos:prefLabel ?prefLabel} \n" +
            "}\n" +
            "group by ?uri\n" +
            "order by ?uri";

    private static final String GET_SKOS_ITEMS_DATA_SPARQL = "" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
            "select \n" +
            "    ?uri min(?notation) as ?skosNotation min(?prefLabel) as ?skosPrefLabel \n" +
            "where { \n" +
            "    ?uri ?p ?o . \n" +
            "    filter (?uri IN (@item_uris@)) \n" +
            "    optional {?uri skos:notation ?notation} \n" +
            "    optional {?uri skos:prefLabel ?prefLabel} \n" +
            "} \n" +
            "group by ?uri \n" +
            "order by ?uri";

    /** The Constant EXPORT_CODELIST_ITEMS_SPARQL. */
    private static final String EXPORT_CODELIST_ITEMS_SPARQL = "" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
            "PREFIX prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
            "select\n" +
            "  ?s ?p ?o ?memberOf ?order\n" +
            "where {\n" +
            "  ?s a ?type.\n" +
            "  ?s ?p ?o\n" +
            "  filter (?type = ?typeValue)\n" +
            "  optional {filter (?p = prop:membership) \n" +
            "            ?o prop:member-of ?memberOf . \n" +
            "            optional {?o prop:order ?order}} \n" +
            "} \n" +
            "order by ?s ?p";

    /** The Constant GET_SUBJECTS_WITH_PROPERTY_SPARQL. */
    private static final String GET_SUBJECTS_WITH_PROPERTY_SPARQL = "" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
            "PREFIX dct: <http://purl.org/dc/terms/>\n" +
            "select\n" +
            "  distinct ?s\n" +
            "where {\n" +
            "  ?s ?p ?propValue\n" +
            "  filter (?s in (csvSubjects))\n" +
            "  filter (?p = ?pVal)\n" +
            "  filter (bound(?propValue))\n" +
            "}";

    /** The Constant DELETE_INVALID_CODELIST_GROUP_MEMBERSHIPS. */
    private static final String DELETE_INVALID_CODELIST_GROUP_MEMBERSHIPS = "" +
            "PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
            "DELETE {\n" +
            "  graph ?g {\n" +
            "    ?x dad-prop:membership ?y.\n" +
            "    ?y ?p ?o\n" +
            "  }\n" +
            "}\n" +
            "where {\n" +
            "  graph ?g {\n" +
            "    ?x dad-prop:membership ?y .\n" +
            "    ?y dad-prop:member-of <@group-graph-uri@>.\n" +
            "    ?y ?p ?o\n" +
            "  }\n" +
            "}";

    /** The Constant DELETE_DATASET_STATUS. */
    private static final String DELETE_DATASET_STATUS = "" +
            "PREFIX adms: <http://www.w3.org/ns/adms#>\n" +
            "DELETE {\n" +
            "  graph ?g {\n" +
            "    <DATASET_URI> adms:status ?status\n" +
            "  }\n" +
            "}\n" +
            "where {\n" +
            "  graph ?g {\n" +
            "    <DATASET_URI> adms:status ?status\n" +
            "  }\n" +
            "}";

    /** The Constant INSERT_DATASET_STATUS. */
    private static final String INSERT_DATASET_STATUS = "" +
            "PREFIX adms: <http://www.w3.org/ns/adms#>\n" +
            "INSERT DATA INTO GRAPH <DATASET_URI> {\n" +
            "  <DATASET_URI> adms:status <STATUS_URI>\n" +
            "}";

    /** The Constant INDICATORS_FOR_ODP_ZIPPING. */
    private static final String INDICATORS_FOR_ODP_ZIPPING = "" +
            "PREFIX dcterms: <http://purl.org/dc/terms/>\n" +
            "PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
            "PREFIX dad-class: <http://semantic.digital-agenda-data.eu/def/class/>\n" +
            "select ?uri min(?notation) as ?skosNotation min(?prefLabel) as ?skosPrefLabel where {\n" +
            "  ?uri a dad-class:Indicator .\n" +
            "  ?uri dcterms:source ?src .\n" +
            "  ?uri dad-prop:membership ?membership .\n" +
            "  ?membership dad-prop:member-of ?grp .\n" +
            "  optional {?uri skos:notation ?notation}\n" +
            "  optional {?uri skos:prefLabel ?prefLabel}\n" +
            "  @FILTER_GROUPS@\n" +
            "  @FILTER_SOURCES@\n" +
            "}\n" +
            "group by ?uri\n" +
            "order by ?uri";

    /** The Constant GET_DISTINCT_USED_REF_AREAS. */
    private static final String GET_DISTINCT_USED_REF_AREAS = "" +
            "PREFIX cube: <http://purl.org/linked-data/cube#>\n" +
            "PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
            "SELECT DISTINCT ?refArea WHERE {\n" +
            "  ?s a cube:Observation .\n" +
            "  ?s cube:dataSet ?dst .\n" +
            "  ?s dad-prop:indicator ?ind .\n" +
            "  ?s dad-prop:ref-area ?refArea\n" +
            "  @FILTER_DATASET@\n" +
            "  @FILTER_INDICATOR@\n" +
            "}\n" +
            "ORDER BY ?refArea";

    /** The Constant GET_INDICATOR_SOURCES_USED_IN_DATASET. */
    private static final String GET_INDICATOR_SOURCES_USED_IN_DATASET = "" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
            "PREFIX cube: <http://purl.org/linked-data/cube#>\n" +
            "PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
            "PREFIX dcterms: <http://purl.org/dc/terms/>\n" +
            "\n" +
            "select\n" +
            "    ?uri min(?notation) as ?skosNotation min(?prefLabel) as ?skosPrefLabel\n" +
            "where {\n" +
            "    ?obs a cube:Observation .\n" +
            "    ?obs cube:dataSet ?dst .\n" +
            "    ?obs dad-prop:indicator ?ind .\n" +
            "    ?ind dcterms:source ?uri\n" +
            "    optional {?uri skos:notation ?notation}\n" +
            "    optional {?uri skos:prefLabel ?prefLabel}\n" +
            "    filter (?dst = ?dstUri)\n" +
            "}\n" +
            "group by ?uri\n" +
            "order by ?uri";

    /** The Constant INDICATORS_FOR_ODP_ZIPPING2. */
    private static final String INDICATORS_FOR_ODP_ZIPPING2 = "" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
            "PREFIX cube: <http://purl.org/linked-data/cube#>\n" +
            "PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
            "PREFIX dcterms: <http://purl.org/dc/terms/>\n" +
            "\n" +
            "select\n" +
            "    ?uri min(?notation) as ?skosNotation min(?prefLabel) as ?skosPrefLabel\n" +
            "where {\n" +
            "    ?obs a cube:Observation .\n" +
            "    ?obs cube:dataSet ?dst .\n" +
            "    ?obs dad-prop:indicator ?uri .\n" +
            "    @SOURCE_PATTERN@\n" +
            "    optional {?uri skos:notation ?notation}\n" +
            "    optional {?uri skos:prefLabel ?prefLabel}\n" +
            "    filter (?dst = ?dstUri)\n" +
            "    @FILTER_SOURCES@\n" +
            "}\n" +
            "group by ?uri\n" +
            "order by ?uri";

    /** */
    private static final String DATASET_INDICATORS = "" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
            "PREFIX cube: <http://purl.org/linked-data/cube#>\n" +
            "PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
            "\n" +
            "select\n" +
            "    ?uri min(distinct ?notation) as ?skosNotation min(distinct ?prefLabel) as ?skosPrefLabel\n" +
            "where {\n" +
            "    ?obs a cube:Observation .\n" +
            "    ?obs cube:dataSet ?dst .\n" +
            "    ?obs dad-prop:indicator ?uri .\n" +
            "#per  ?obs dad-prop:time-period ?per. \n" +
            "#per  filter(?per = ?periodUri) \n" +
            "#txt  ?uri ?p ?o \n" +
            "#txt  filter bif:contains(?o, ?objectVal) \n" +
            "    optional {?uri skos:notation ?notation} \n" +
            "    optional {?uri skos:prefLabel ?prefLabel} \n" +
            "    filter (?dst = ?dstUri) \n" +
            "} \n" +
            "group by ?uri \n" +
            "order by ?uri";

    private static final String DATASET_TIME_PERIOD_URIS = "" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
            "PREFIX cube: <http://purl.org/linked-data/cube#>\n" +
            "PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "\n" +
            "select distinct ?uri \n" +
            "where {\n" +
            "    ?obs a cube:Observation .\n" +
            "    ?obs cube:dataSet ?dst .\n" +
            "    ?obs dad-prop:time-period ?uri \n" +
            "    filter (?dst = ?dstUri) \n" +
            "} \n" +
            "order by ?uri";

    /** The Constant GET_DISTINCT_DATASET_URIS. */
    private static final String GET_DISTINCT_DATASET_URIS = "" +
            "PREFIX cube: <http://purl.org/linked-data/cube#>\n" +
            "select distinct ?datasetUri where {\n" +
            "   ?datasetUri a cube:DataSet\n" +
            "}\n" +
            "order by ?datasetUri";

    /** The Constant DELETE_OBSERVATIONS_OF_INDICATOR. */
    private static final String DELETE_OBSERVATIONS_OF_INDICATOR = "" +
            "SPARQL \n" +
            "PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
            "PREFIX cube: <http://purl.org/linked-data/cube#>\n" +
            "DELETE FROM GRAPH iri(??) {\n" +
            "    ?obs ?p ?o\n" +
            "}\n" +
            "WHERE {\n" +
            "  GRAPH `iri(??)` {\n" +
            "    ?obs ?p ?o .\n" +
            "    ?obs a cube:Observation .\n" +
            "    ?obs dad-prop:indicator `iri(??)` \n" +
            "  }\n" +
            "}";

    /** The Constant DELETE_OBSERVATIONS_OF_INDICATOR. */
    private static final String DELETE_OBSERVATIONS_OF_INDICATOR_AND_TIMES = "" +
            "SPARQL \n" +
            "PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
            "PREFIX cube: <http://purl.org/linked-data/cube#>\n" +
            "DELETE FROM GRAPH iri(??) {\n" +
            "    ?obs ?p ?o\n" +
            "}\n" +
            "WHERE {\n" +
            "  GRAPH `iri(??)` {\n" +
            "    ?obs ?p ?o .\n" +
            "    ?obs a cube:Observation .\n" +
            "    ?obs dad-prop:indicator `iri(??)` .\n" +
            "    ?obs dad-prop:time-period ?timePeriod \n" +
            "    filter (?timePeriod in (@TIME_PERIOD_IRIS@)) \n" +
            "  }\n" +
            "}";

    /** SPARQL for deleting all triples of a given subject's given predicate. */
    private static final String DELETE_SUBJECT_PREDICATE =
            "SPARQL \n" +
            "PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
            "PREFIX cube: <http://purl.org/linked-data/cube#>\n" +
            "DELETE FROM GRAPH iri(??) {\n" +
            "    ?s ?p ?o \n" +
            "} \n" +
            "WHERE { \n" +
            "  GRAPH `iri(??)` {\n" +
            "    ?s ?p ?o \n" +
            "    filter (?s = iri(??) && ?p = iri(??)) \n" +
            "  }\n" +
            "}";

    /** The Constant GET_EARLIEST_OBSERVATION_BY_INDICATOR_AND_DATASET. */
    private static final String GET_EARLIEST_OBSERVATION_BY_INDICATOR_AND_DATASET = "" +
            "PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
    		"PREFIX cube: <http://purl.org/linked-data/cube#>\n" +
    		"\n" +
    		"select \n" +
    		"   min(bif:left(bif:subseq(str(?time), bif:strrchr(bif:replace(str(?time), '/', '#'), '#') + 1), 4)) as ?minTime \n" +
    		"where {\n" +
    		"   ?s a cube:Observation .\n" +
    		"   ?s cube:dataSet <@datasetUri@> .\n" +
    		"   ?s dad-prop:indicator <@indicatorUri@> .\n" +
    		"   ?s dad-prop:time-period ?time\n" +
    		"}";

    // @formatter:on

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#getCodelists(java.lang.String)
     */
    @Override
    public List<Pair<String, String>> getCodelists(String uriStartsWith) throws DAOException {

        if (StringUtils.isBlank(uriStartsWith)) {
            throw new IllegalArgumentException("URI start-prefix must not be blank!");
        }

        Bindings bindings = new Bindings();
        bindings.setString("uriStartsWith", uriStartsWith);

        List<Pair<String, String>> list = executeSPARQL(GET_CODELISTS_SPARQL, bindings, new PairReader<String, String>());
        return list;
    }

    @Override
    public List<SkosItemDTO> getCodelistItems(String codelistUri) throws DAOException {
        return getCodelistItems(codelistUri, null, null);
    }

    @Override
    public List<SkosItemDTO> getCodelistItems(String codelistUri, String datasetUri, String freeText) throws DAOException {

        if (StringUtils.isBlank(codelistUri)) {
            throw new IllegalArgumentException("Codelist URI must not be blank!");
        }

        Bindings bindings = new Bindings();
        bindings.setURI("schemeUri", codelistUri);

        String query = new String(GET_CODELIST_ITEMS_SPARQL);
        if (StringUtils.isNotBlank(datasetUri)) {
            query = StringUtils.replace(query, "#dst", "");
            bindings.setURI("datasetUri", datasetUri);
        }

        if (StringUtils.isNotBlank(freeText)) {
            query = StringUtils.replace(query, "#txt", "");
            bindings.setString("objectVal", "'" + freeText + "'");
        }

        List<SkosItemDTO> list = executeSPARQL(query, bindings, new SkosItemsReader());
        return list;
    }

    @Override
    public List<SkosItemDTO> getDatasetIndicators(String datasetUri, String timePeriodUri, String freeText) throws DAOException {

        if (StringUtils.isBlank(datasetUri)) {
            throw new IllegalArgumentException("Dataset URI must not be blank!");
        }

        Bindings bindings = new Bindings();
        bindings.setURI("dstUri", datasetUri);

        String query = new String(DATASET_INDICATORS);
        if (StringUtils.isNotBlank(timePeriodUri)) {
            query = StringUtils.replace(query, "#per", "");
            bindings.setURI("periodUri", timePeriodUri);
        }

        if (StringUtils.isNotBlank(freeText)) {
            query = StringUtils.replace(query, "#txt", "");
            bindings.setString("objectVal", "'" + freeText + "'");
        }

        List<SkosItemDTO> list = executeSPARQL(query, bindings, new SkosItemsReader());
        return list;
    }

    @Override
    public List<SkosItemDTO> getDatasetTimePeriods(String datasetUri) throws DAOException {

        if (StringUtils.isBlank(datasetUri)) {
            throw new IllegalArgumentException("Dataset URI must not be blank!");
        }

        Bindings bindings = new Bindings();
        bindings.setURI("dstUri", datasetUri);
        List<String> timePeriodUris = executeSPARQL(DATASET_TIME_PERIOD_URIS, bindings, new SingleObjectReader<String>());

        List<SkosItemDTO> resultList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(timePeriodUris)) {

            bindings = new Bindings();
            String itemsDataQuery = GET_SKOS_ITEMS_DATA_SPARQL.replace("@item_uris@", SPARQLQueryUtil.urisToCSV(timePeriodUris, "uriVal", bindings));
            resultList = executeSPARQL(itemsDataQuery, bindings, new SkosItemsReader());
        }

        return resultList;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#createDataCubeDataset(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String createDataset(String identifier, String dctermsTitle, String dctermsDescription) throws DAOException {

        // Assume input validations have been done by the caller!

        RepositoryConnection repoConn = null;
        try {
            repoConn = SesameUtil.getRepositoryConnection();
            repoConn.setAutoCommit(false);
            ValueFactory vf = repoConn.getValueFactory();

            // Predicate URIs.
            URI identifierPredicateURI = vf.createURI(Predicates.DCTERMS_IDENTIFIER);
            URI typePredicateURI = vf.createURI(Predicates.RDF_TYPE);
            URI titlePredicateURI = vf.createURI(Predicates.DCTERMS_TITLE);
            URI descriptionPredicateURI = vf.createURI(Predicates.DCTERMS_DESCRIPTION);
            URI distributionPredicateURI = vf.createURI(Predicates.DCAT_DISTRIBUTION);
            URI accessUrlPredicateURI = vf.createURI(Predicates.DCAT_ACCESS_URL);
            URI modifiedPredicateURI = vf.createURI(Predicates.DCTERMS_MODIFIED);
            URI labelPredicateURI = vf.createURI(Predicates.RDFS_LABEL);
            URI dcFormatPredicateURI = vf.createURI(Predicates.DCTERMS_FORMAT);
            URI ecodpFormatPredicateURI = vf.createURI(Predicates.ECODP_FORMAT);
            URI dsdPredicateURI = vf.createURI(Predicates.DATACUBE_STRUCTURE);

            // Some value URIs
            URI identifierURI = vf.createURI(DATASET_URI_PREFIX + identifier);
            // URI graphURI = vf.createURI(StringUtils.substringBeforeLast(DATASET_URI_PREFIX, "/"));
            URI graphURI = identifierURI;
            URI distributionURI = vf.createURI(identifierURI + "/distribution");
            URI accessURL = vf.createURI(StringUtils.replace(identifierURI.stringValue(), "/dataset/", "/data/"));
            URI dcFormatUri = vf.createURI("http://publications.europa.eu/resource/authority/file-type/RDF_XML");
            Literal dateModified = vf.createLiteral(Util.virtuosoDateToString(new Date()), XMLSchema.DATETIME);

            // Add properties for the dataset itself
            repoConn.add(identifierURI, identifierPredicateURI, identifierURI, graphURI);
            repoConn.add(identifierURI, typePredicateURI, vf.createURI(Subjects.DATACUBE_DATA_SET), graphURI);
            repoConn.add(identifierURI, titlePredicateURI, vf.createLiteral(dctermsTitle), graphURI);
            repoConn.add(identifierURI, labelPredicateURI, vf.createLiteral(identifier), graphURI);
            repoConn.add(identifierURI, distributionPredicateURI, distributionURI, graphURI);
            repoConn.add(identifierURI, modifiedPredicateURI, dateModified, graphURI);
            repoConn.add(identifierURI, dsdPredicateURI, vf.createURI(DEFAULT_DSD_URI), graphURI);
            if (StringUtils.isNotBlank(dctermsDescription)) {
                repoConn.add(identifierURI, descriptionPredicateURI, vf.createLiteral(dctermsDescription), graphURI);
            }

            // Add properties for linked resources
            repoConn.add(distributionURI, typePredicateURI, vf.createURI(Subjects.DCAT_WEB_SERVICE), graphURI);
            repoConn.add(distributionURI, accessUrlPredicateURI, accessURL, graphURI);
            repoConn.add(distributionURI, dcFormatPredicateURI, dcFormatUri, graphURI);
            repoConn.add(distributionURI, ecodpFormatPredicateURI, vf.createLiteral("rdf/xml"), graphURI);

            repoConn.commit();
            return identifierURI.stringValue();

        } catch (RepositoryException e) {
            SesameUtil.rollback(repoConn);
            throw new DAOException(e.getMessage(), e);
        } finally {
            SesameUtil.close(repoConn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#datasetExists(java.lang.String)
     */
    @Override
    public boolean datasetExists(String identifier) throws DAOException {

        RepositoryConnection repoConn = null;
        try {
            repoConn = SesameUtil.getRepositoryConnection();

            ValueFactory vf = repoConn.getValueFactory();
            URI identifierURI = vf.createURI(DATASET_URI_PREFIX + identifier);
            URI typeURI = vf.createURI(Predicates.RDF_TYPE);

            return repoConn.hasStatement(identifierURI, typeURI, vf.createURI(Subjects.DATACUBE_DATA_SET), false);
        } catch (RepositoryException e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SesameUtil.close(repoConn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#getFilterValues(java.util.Map, eionet.cr.web.util.ObservationFilter, boolean)
     */
    @Override
    public List<Pair<String, String>> getFilterValues(Map<ObservationFilter, String> selections, ObservationFilter filter, boolean isAdmin)
            throws DAOException {

        if (filter == null) {
            throw new IllegalArgumentException("Filter for which the values are being asked, must not be null!");
        }

        String filterAlias = filter.getAlias();
        int filterIndex = filter.ordinal();

        StringBuilder sb = new StringBuilder();
        sb.append("PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>\n");
        sb.append("PREFIX cube: <http://purl.org/linked-data/cube#>\n");
        sb.append("PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n");
        sb.append("\n");
        sb.append("select\n");
        sb.append("  ?").append(filterAlias).append(" min(str(coalesce(?prefLabel, ?").append(filterAlias).append("))) as ?label\n");
        sb.append("where {\n");
        sb.append("  ?s a cube:Observation.\n");

        ObservationFilter[] filters = ObservationFilter.values();
        if (selections != null && !selections.isEmpty()) {

            for (int i = 0; i < filterIndex && i < filters.length; i++) {

                ObservationFilter availFilter = filters[i];
                String selValue = selections.get(availFilter);
                if (StringUtils.isNotBlank(selValue)) {
                    if (URIUtil.isSchemedURI(selValue)) {
                        sb.append("  ?s <").append(availFilter.getPredicate()).append("> <").append(selValue).append(">.\n");
                    } else {
                        sb.append("  ?s <").append(availFilter.getPredicate()).append("> \"").append(selValue).append("\".\n");
                    }
                } else {
                    sb.append("  ?s <").append(availFilter.getPredicate()).append("> ?").append(availFilter.getAlias()).append(".\n");
                }
            }
        }

        sb.append("  ?s <").append(filter.getPredicate()).append("> ?").append(filterAlias).append(".\n");

        // If not an admin-user, allow selections from "Completed" datasets only.
        if (!isAdmin) {
            if (sb.toString().contains(" ?" + ObservationFilter.DATASET.getAlias())) {
                sb.append("  ?").append(ObservationFilter.DATASET.getAlias()).append(" <").append(Predicates.ADMS_STATUS).append("> <")
                        .append(Subjects.ADMS_STATUS_COMPLETED).append(">\n");
            }
        }

        sb.append("  optional {?").append(filterAlias);
        sb.append(" skos:prefLabel ?prefLabel filter(lang(?prefLabel) in ('en',''))}\n");
        sb.append("}\n");
        sb.append("group by ?").append(filterAlias).append("\n");
        sb.append("order by ?label");

        LOGGER.trace("\nsparql\n" + sb + ";");

        PairReader<String, String> reader = new PairReader<String, String>(filterAlias, "label");
        List<Pair<String, String>> resultList = executeSPARQL(sb.toString(), reader);
        return resultList;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#exportCodelistItems(java.lang.String, java.io.File, java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public int exportCodelistItems(String itemType, File templateFile, Map<String, Integer> mappings, File targetFile) throws DAOException {

        if (StringUtils.isBlank(itemType)) {
            throw new IllegalArgumentException("Items RDF type must not be blank!");
        }
        if (templateFile == null || !templateFile.exists() || !templateFile.isFile()) {
            throw new IllegalArgumentException("The given spreadsheet template must not be null and the file must exist!");
        }

        int result = 0;
        if (mappings != null && !mappings.isEmpty()) {

            Bindings bindings = new Bindings();
            bindings.setURI("typeValue", itemType);

            CodelistExporter exporter = new CodelistExporter(templateFile, mappings, targetFile);
            executeSPARQL(EXPORT_CODELIST_ITEMS_SPARQL, bindings, exporter);
            exporter.saveAndClose();

            result = exporter.getItemsExported();
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#updateDcTermsModified(java.lang.String, java.util.Date, java.lang.String)
     */
    @Override
    public void updateDcTermsModified(String subjectUri, Date date, String graphUri) throws DAOException {

        if (StringUtils.isBlank(subjectUri)) {
            throw new IllegalArgumentException("The subject URI must not be blank!");
        }
        if (StringUtils.isBlank(graphUri)) {
            throw new IllegalArgumentException("The graph URI must not be blank!");
        }
        if (date == null) {
            date = new Date();
        }

        LOGGER.debug("Setting dcterms:modified of <" + subjectUri + "> in graph <" + graphUri + "> to this value: " + date);

        RepositoryConnection repoConn = null;
        try {
            repoConn = SesameUtil.getRepositoryConnection();
            repoConn.setAutoCommit(false);
            ValueFactory vf = repoConn.getValueFactory();

            // Prepare some values
            URI subjectURI = vf.createURI(subjectUri);
            URI predicateURI = vf.createURI(Predicates.DCTERMS_MODIFIED);
            URI graphURI = vf.createURI(graphUri);
            Literal dateValue = vf.createLiteral(Util.virtuosoDateToString(date), XMLSchema.DATETIME);

            // Remove all previous dcterms:modified triples of the given subject in the given graph.
            repoConn.remove(subjectURI, predicateURI, null, graphURI);

            // Add the new dcterms:modified triple.
            repoConn.add(subjectURI, predicateURI, dateValue, graphURI);

            // Commit the transaction.
            repoConn.commit();

        } catch (RepositoryException e) {
            SesameUtil.rollback(repoConn);
            throw new DAOException("Failed to update dcterms:modified of " + subjectUri, e);
        } finally {
            SesameUtil.close(repoConn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#getSubjectsWithBoundProperty(java.lang.String, java.util.Set)
     */
    @Override
    public Set<String> getSubjectsWithBoundProperty(String propertyUri, Set<String> subjects) throws DAOException {

        Bindings bindings = new Bindings();
        bindings.setURI("pVal", propertyUri);
        String csvSubjects = SPARQLQueryUtil.urisToCSV(subjects, "sVal", bindings);

        String sparql = StringUtils.replace(GET_SUBJECTS_WITH_PROPERTY_SPARQL, "csvSubjects", csvSubjects);
        List<String> result = executeSPARQL(sparql, bindings, new SingleObjectReader<String>());

        return new HashSet<String>(result);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#fixGrouplessCodelistItems()
     */
    @Override
    public void fixGrouplessCodelistItems() throws DAOException {

        String breakdownsSPARUL =
                DELETE_INVALID_CODELIST_GROUP_MEMBERSHIPS.replace("@group-graph-uri@", XLWrapUploadType.BREAKDOWN_GROUP.getGraphUri());

        String indicatorsSPARUL =
                DELETE_INVALID_CODELIST_GROUP_MEMBERSHIPS.replace("@group-graph-uri@", XLWrapUploadType.INDICATOR_GROUP.getGraphUri());

        RepositoryConnection repoConn = null;
        try {
            repoConn = SesameUtil.getRepositoryConnection();
            SesameUtil.executeSPARUL(breakdownsSPARUL, repoConn);
            SesameUtil.executeSPARUL(indicatorsSPARUL, repoConn);
        } catch (OpenRDFException e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SesameUtil.close(repoConn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#getObservationPredicateValues(java.lang.String, boolean,
     * eionet.cr.util.pagination.PagingRequest, eionet.cr.util.SortingRequest, java.lang.String[])
     */
    @Override
    public SearchResultDTO<Pair<String, String>> getObservationPredicateValues(String predicateUri, boolean isAdmin, PagingRequest pageRequest,
            SortingRequest sortRequest, String... labelPredicates) throws DAOException {

        if (!URIUtil.isURI(predicateUri)) {
            throw new IllegalArgumentException("predicateUri must not be blank and it must be a legal URI!");
        }

        if (sortRequest == null) {
            sortRequest = SortingRequest.create(PairReader.RIGHTCOL, SortOrder.ASCENDING);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("select distinct").append("\n");
        sb.append("  ?s as ?").append(PairReader.LEFTCOL).append("\n");

        if (ArrayUtils.isEmpty(labelPredicates)) {
            sb.append("  bif:subseq(str(?s), coalesce(bif:strrchr(bif:replace(str(?s),'/','#'),'#'),0)+1) as ?").append(PairReader.RIGHTCOL)
                    .append("\n");
        } else {
            sb.append("  coalesce(");
            for (int i = 0; i < labelPredicates.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("?label").append(i);
            }
            sb.append(", bif:subseq(str(?s), coalesce(bif:strrchr(bif:replace(str(?s),'/','#'),'#'),0)+1)) as ?").append(PairReader.RIGHTCOL)
                    .append("\n");
        }

        sb.append("where {").append("\n");
        sb.append("  ?subj a <").append(Subjects.DATACUBE_OBSERVATION).append("> .").append("\n");

        // If not an admin-user, allow selections from "Completed" datasets only.
        if (!isAdmin) {
            sb.append("  ?subj <").append(Predicates.DATACUBE_DATA_SET).append("> ?ds .").append("\n");
            sb.append("  ?ds <").append(Predicates.ADMS_STATUS).append("> <").append(Subjects.ADMS_STATUS_COMPLETED).append("> .").append("\n");
        }

        sb.append("  ?subj ?pred ?s").append("\n");

        Bindings bindings = new Bindings();
        bindings.setURI("pred", predicateUri);

        String s = "  optional {?s ?labelPred0 ?label0}\n";
        for (int i = 0; i < labelPredicates.length; i++) {
            sb.append(StringUtils.replace(s, "0", String.valueOf(i)));
            bindings.setURI("labelPred" + i, labelPredicates[i]);
        }
        sb.append("}\n");
        sb.append("order by ").append(sortRequest.getSortOrder()).append("(?").append(sortRequest.getSortingColumnName()).append(")");

        if (pageRequest != null) {
            sb.append(" limit ").append(pageRequest.getItemsPerPage()).append(" offset ").append(pageRequest.getOffset());
        }

        List<Pair<String, String>> list = executeSPARQL(sb.toString(), bindings, new PairReader<String, String>());
        int totalMatchCount = list.size();
        if (pageRequest != null) {

            sb = new StringBuilder();
            sb.append("select count(distinct ?s) where {");
            sb.append("  ?subj a <").append(Subjects.DATACUBE_OBSERVATION).append("> .").append("\n");

            // If not an admin-user, allow selections from "Completed" datasets only.
            if (!isAdmin) {
                sb.append("  ?subj <").append(Predicates.DATACUBE_DATA_SET).append("> ?ds .").append("\n");
                sb.append("  ?ds <").append(Predicates.ADMS_STATUS).append("> <").append(Subjects.ADMS_STATUS_COMPLETED).append("> .").append("\n");
            }

            sb.append("  ?subj ?pred ?s").append("\n");
            sb.append("}");

            bindings = new Bindings();
            bindings.setURI("pred", predicateUri);

            String count = executeUniqueResultSPARQL(sb.toString(), bindings, new SingleObjectReader<String>());
            totalMatchCount = NumberUtils.toInt(count);
        }

        return new SearchResultDTO<Pair<String, String>>(list, totalMatchCount);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#getDistinctDatasets(boolean, eionet.cr.util.pagination.PagingRequest,
     * eionet.cr.util.SortingRequest, java.lang.String[])
     */
    @Override
    public SearchResultDTO<Pair<String, String>> getDistinctDatasets(boolean isAdmin, PagingRequest pageRequest, SortingRequest sortRequest,
            String... labelPredicates) throws DAOException {

        if (sortRequest == null) {
            sortRequest = SortingRequest.create(PairReader.RIGHTCOL, SortOrder.ASCENDING);
        }

        Bindings bindings = new Bindings();

        StringBuilder sb = new StringBuilder();
        sb.append("select distinct").append("\n");
        sb.append("  ?s as ?").append(PairReader.LEFTCOL).append("\n");

        if (ArrayUtils.isEmpty(labelPredicates)) {
            sb.append("  bif:subseq(str(?s), coalesce(bif:strrchr(bif:replace(str(?s),'/','#'),'#'),0)+1) as ?").append(PairReader.RIGHTCOL)
                    .append("\n");
        } else {
            sb.append("  coalesce(");
            for (int i = 0; i < labelPredicates.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("?label").append(i);
            }
            sb.append(", bif:subseq(str(?s), coalesce(bif:strrchr(bif:replace(str(?s),'/','#'),'#'),0)+1)) as ?").append(PairReader.RIGHTCOL)
                    .append("\n");
        }

        sb.append("where {").append("\n");
        sb.append("  ?s a <").append(Subjects.DATACUBE_DATA_SET).append("> .\n");

        // If not an admin-user, allow selections from "Completed" datasets only.
        if (!isAdmin) {
            sb.append("  ?s <").append(Predicates.ADMS_STATUS).append("> <").append(Subjects.ADMS_STATUS_COMPLETED).append("> .").append("\n");
        }

        String s = "  optional {?s ?labelPred0 ?label0}\n";
        for (int i = 0; i < labelPredicates.length; i++) {
            sb.append(StringUtils.replace(s, "0", String.valueOf(i)));
            bindings.setURI("labelPred" + i, labelPredicates[i]);
        }
        sb.append("}\n");
        sb.append("order by ").append(sortRequest.getSortOrder()).append("(?").append(sortRequest.getSortingColumnName()).append(")");

        if (pageRequest != null) {
            sb.append(" limit ").append(pageRequest.getItemsPerPage()).append(" offset ").append(pageRequest.getOffset());
        }

        List<Pair<String, String>> list = executeSPARQL(sb.toString(), bindings, new PairReader<String, String>());
        int totalMatchCount = list.size();
        if (pageRequest != null) {

            sb = new StringBuilder();
            sb.append("select count(distinct ?s) where {\n");
            sb.append("  ?s a <").append(Subjects.DATACUBE_DATA_SET).append("> .\n");

            // If not an admin-user, allow selections from "Completed" datasets only.
            if (!isAdmin) {
                sb.append("  ?s <").append(Predicates.ADMS_STATUS).append("> <").append(Subjects.ADMS_STATUS_COMPLETED).append("> .").append("\n");
            }
            sb.append("}");

            String count = executeUniqueResultSPARQL(sb.toString(), new SingleObjectReader<String>());
            totalMatchCount = NumberUtils.toInt(count);
        }

        return new SearchResultDTO<Pair<String, String>>(list, totalMatchCount);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#changeDatasetStatus(java.lang.String, java.lang.String)
     */
    @Override
    public void changeDatasetStatus(String uri, String newStatus) throws DAOException {

        if (StringUtils.isBlank(uri) || StringUtils.isBlank(newStatus)) {
            throw new IllegalArgumentException("The dataset URI and the new status must not be blank!");
        }

        RepositoryConnection repoConn = null;
        try {
            // Prepare the connection
            repoConn = SesameUtil.getRepositoryConnection();
            repoConn.setAutoCommit(false);

            // Avoid SQL-injection by forcing to go through ValueFactory.
            ValueFactory vf = repoConn.getValueFactory();
            URI datasetURI = vf.createURI(uri);
            URI statusURI = vf.createURI(newStatus);

            // Delete all status triples of the given dataset.
            String sparql = DELETE_DATASET_STATUS.replace("DATASET_URI", datasetURI.stringValue());
            SesameUtil.executeSPARUL(sparql, null, repoConn);

            // Insert the new status triple.
            sparql = INSERT_DATASET_STATUS.replace("DATASET_URI", uri);
            sparql = sparql.replace("STATUS_URI", statusURI.stringValue());
            SesameUtil.executeSPARUL(sparql, null, repoConn);

            // Commit the transaction.
            repoConn.commit();

        } catch (OpenRDFException e) {
            SesameUtil.rollback(repoConn);
            throw new DAOException("Failed to change dataset status of " + uri, e);
        } finally {
            SesameUtil.close(repoConn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#getIndicators(java.util.List, java.util.List)
     */
    @Override
    public List<SkosItemDTO> getIndicators(List<String> groupNotations, List<String> sourceNotations) throws DAOException {

        String sparql = new String(INDICATORS_FOR_ODP_ZIPPING);
        if (CollectionUtils.isEmpty(groupNotations)) {
            sparql = StringUtils.replace(sparql, "@FILTER_GROUPS@", StringUtils.EMPTY);
        } else {
            ArrayList<String> groupUris = new ArrayList<String>();
            for (String groupNotation : groupNotations) {

                String uri = IND_GROUP_CODELIST_URI + "/" + groupNotation;
                try {
                    groupUris.add(new URL(uri).toString());
                } catch (MalformedURLException e) {
                    throw new DAOException("Invalid URL: " + uri);
                }
            }
            String urisToCSV = SPARQLQueryUtil.urisToCSV(groupUris);
            String filterStr = "filter (?grp in (" + urisToCSV + "))";
            sparql = StringUtils.replace(sparql, "@FILTER_GROUPS@", filterStr);
        }

        if (CollectionUtils.isEmpty(sourceNotations)) {
            sparql = StringUtils.replace(sparql, "@FILTER_SOURCES@", StringUtils.EMPTY);
        } else {
            ArrayList<String> sourceUris = new ArrayList<String>();
            for (String sourceNotation : sourceNotations) {

                String uri = IND_SOURCE_CODELIST_URI + "/" + sourceNotation;
                try {
                    sourceUris.add(new URL(uri).toString());
                } catch (MalformedURLException e) {
                    throw new DAOException("Invalid URL: " + uri);
                }
            }
            String urisToCSV = SPARQLQueryUtil.urisToCSV(sourceUris);
            String filterStr = "filter (?src in (" + urisToCSV + "))";
            sparql = StringUtils.replace(sparql, "@FILTER_SOURCES@", filterStr);
        }

        List<SkosItemDTO> resultList = executeSPARQL(sparql, new SkosItemsReader());
        return resultList;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#getDistinctUsedRefAreas(java.lang.String, java.lang.String)
     */
    @Override
    public List<String> getDistinctUsedRefAreas(String datasetUri, String indicatorUri) throws DAOException {

        String sparql = GET_DISTINCT_USED_REF_AREAS;
        Bindings bindings = new Bindings();

        if (StringUtils.isNotBlank(datasetUri)) {
            sparql = StringUtils.replace(sparql, "@FILTER_DATASET@", "filter (?dst = ?dstUri)");
            bindings.setURI("dstUri", datasetUri);
        } else {
            sparql = StringUtils.replace(sparql, "@FILTER_DATASET@", StringUtils.EMPTY);
        }

        if (StringUtils.isNotBlank(indicatorUri)) {
            sparql = StringUtils.replace(sparql, "@FILTER_INDICATOR@", "filter (?ind = ?indUri)");
            bindings.setURI("indUri", indicatorUri);
        } else {
            sparql = StringUtils.replace(sparql, "@FILTER_INDICATOR@", StringUtils.EMPTY);
        }

        List<String> returnList = executeSPARQL(sparql, bindings, new SingleObjectReader<String>());
        return returnList;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#getIndicatorSourcesUsedInDataset(java.lang.String)
     */
    @Override
    public List<SkosItemDTO> getIndicatorSourcesUsedInDataset(String datasetUri) throws DAOException {

        if (StringUtils.isBlank(datasetUri)) {
            throw new IllegalArgumentException("Dataset URI must not be blank!");
        }

        Bindings bindings = new Bindings();
        bindings.setURI("dstUri", datasetUri);

        List<SkosItemDTO> list = executeSPARQL(GET_INDICATOR_SOURCES_USED_IN_DATASET, bindings, new SkosItemsReader());
        return list;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#getIndicators(java.lang.String, java.util.List)
     */
    @Override
    public List<SkosItemDTO> getIndicators(String datasetUri, List<String> sourceNotations) throws DAOException {

        if (StringUtils.isBlank(datasetUri)) {
            throw new IllegalArgumentException("Dataset URI must not be blank!");
        }
        Bindings bindings = new Bindings();
        bindings.setURI("dstUri", datasetUri);

        String sparql = new String(INDICATORS_FOR_ODP_ZIPPING2);

        if (CollectionUtils.isEmpty(sourceNotations)) {
            sparql = StringUtils.replace(sparql, "@SOURCE_PATTERN@", StringUtils.EMPTY);
            sparql = StringUtils.replace(sparql, "@FILTER_SOURCES@", StringUtils.EMPTY);
        } else {

            sparql = StringUtils.replace(sparql, "@SOURCE_PATTERN@", "?uri dcterms:source ?src");

            ArrayList<String> sourceUris = new ArrayList<String>();
            for (String sourceNotation : sourceNotations) {

                String uri = IND_SOURCE_CODELIST_URI + "/" + sourceNotation;
                try {
                    sourceUris.add(new URL(uri).toString());
                } catch (MalformedURLException e) {
                    throw new DAOException("Invalid URL: " + uri);
                }
            }
            String urisToCSV = SPARQLQueryUtil.urisToCSV(sourceUris);
            String filterStr = "filter (?src in (" + urisToCSV + "))";
            sparql = StringUtils.replace(sparql, "@FILTER_SOURCES@", filterStr);
        }

        List<SkosItemDTO> resultList = executeSPARQL(sparql, bindings, new SkosItemsReader());
        return resultList;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#getDistinctDatasetUris()
     */
    @Override
    public List<String> getDistinctDatasetUris() throws DAOException {

        List<String> resultList = executeSPARQL(GET_DISTINCT_DATASET_URIS, new SingleObjectReader<String>());
        return resultList;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#deleteObservations(java.lang.String, java.util.Collection, java.util.Collection)
     */
    @Override
    public Pair<Integer, String> deleteObservations(String datasetUri, Collection<String> indicatorUris, Collection<String> timePeriodUris)
            throws DAOException {

        if (StringUtils.isBlank(datasetUri)) {
            throw new IllegalArgumentException("Dataset URI must not be blank!");
        }

        if (CollectionUtils.isEmpty(indicatorUris)) {
            throw new IllegalArgumentException("The collection of indicator URIs must not be null or empty!");
        }

        String sql = "";
        boolean noTimes = CollectionUtils.isEmpty(timePeriodUris);
        String datasetGraphUri = StringUtils.replace(datasetUri, "/dataset/", "/data/");

        if (noTimes) {
            sql = DELETE_OBSERVATIONS_OF_INDICATOR;
        } else {
            sql = DELETE_OBSERVATIONS_OF_INDICATOR_AND_TIMES;
            sql = sql.replace("@TIME_PERIOD_IRIS@", Util.csv("iri(??)", timePeriodUris.size()));
        }

        int updateCount = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = SesameUtil.getSQLConnection();

            pstmt = conn.prepareStatement(sql);
            boolean datasetUpdated = false;

            for (String indicatorUri : indicatorUris) {

                LOGGER.debug(String.format("Deleting indicator %s of %d time periods ...", indicatorUri, noTimes ? 0 : timePeriodUris.size()));

                int i = 0;
                pstmt.setString(++i, datasetGraphUri);
                pstmt.setString(++i, datasetGraphUri);
                pstmt.setString(++i, indicatorUri);

                if (!noTimes) {
                    for (String timePeriodUri : timePeriodUris) {
                        pstmt.setString(++i, timePeriodUri);
                    }
                }

                updateCount += pstmt.executeUpdate();
                if (updateCount > 0 &&!datasetUpdated) {
                    updateDatasetModificationDate(conn, datasetUri, datasetGraphUri);
                    datasetUpdated = true;
                }
            }

            LOGGER.debug(String.format("A total of %d triples deleted!", updateCount));
            return new Pair<Integer, String>(Integer.valueOf(updateCount), sql);
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(pstmt);
            SQLUtil.close(conn);
        }
    }

    /**
     *
     * @param conn
     * @param datasetUri
     * @param datasetGraphUri
     * @throws SQLException
     */
    private void updateDatasetModificationDate(Connection conn, String datasetUri, String datasetGraphUri) throws SQLException {

        LOGGER.debug("Querying dcterms:modified graphs ...");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("sparql select distinct ?g where {graph ?g {`iri(??)` `iri(??)` ?o}}");
            pstmt.setString(1, datasetUri);
            pstmt.setString(2, Predicates.DCTERMS_MODIFIED);
            rs = pstmt.executeQuery();
            HashSet<String> dcTermsModifiedGraphs = new HashSet<String>();
            while (rs.next()) {
                dcTermsModifiedGraphs.add(rs.getString(1));
            }
            SQLUtil.close(pstmt);

            LOGGER.debug("Found these dcterms:modified graphs: " + dcTermsModifiedGraphs);

            if (!dcTermsModifiedGraphs.isEmpty()) {
                pstmt = conn.prepareStatement(DELETE_SUBJECT_PREDICATE);
                for (String graphUri : dcTermsModifiedGraphs) {

                    LOGGER.debug("Deleting dcterms:modified from graph " + graphUri);

                    pstmt.setString(1, graphUri);
                    pstmt.setString(2, graphUri);
                    pstmt.setString(3, datasetUri);
                    pstmt.setString(4, Predicates.DCTERMS_MODIFIED);
                    pstmt.executeUpdate();
                }
                SQLUtil.close(pstmt);
            }

            LOGGER.debug("Inserting new dcterms:modified ...");

            pstmt = conn.prepareStatement("sparql prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
                    + "insert into graph iri(??) {`iri(??)` `iri(??)` `xsd:dateTime(??)`}");
            pstmt.setString(1, datasetGraphUri);
            pstmt.setString(2, datasetUri);
            pstmt.setString(3, Predicates.DCTERMS_MODIFIED);
            pstmt.setString(4, DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            pstmt.executeUpdate();
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(pstmt);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.ScoreboardSparqlDAO#getEarliestObservationDate(java.lang.String, java.lang.String)
     */
    @Override
    public int getEarliestObservationYear(String indicatorUri, String datasetUri) throws DAOException {

        if (StringUtils.isBlank(indicatorUri) || StringUtils.isBlank(datasetUri)) {
            throw new IllegalArgumentException("URIs of indicator and dataset must not be blank!");
        }

        String sparql = GET_EARLIEST_OBSERVATION_BY_INDICATOR_AND_DATASET.replace("@indicatorUri@", indicatorUri);
        sparql = sparql.replace("@datasetUri@", datasetUri);

        String value = executeUniqueResultSPARQL(sparql, new SingleObjectReader<String>());
        int result = NumberUtils.toInt(value, 0);
        return result;
    }
}
