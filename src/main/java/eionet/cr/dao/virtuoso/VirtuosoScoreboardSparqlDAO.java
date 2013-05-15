package eionet.cr.dao.virtuoso;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
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
import eionet.cr.dto.SkosItemDTO;
import eionet.cr.util.Bindings;
import eionet.cr.util.Pair;
import eionet.cr.util.URIUtil;
import eionet.cr.util.Util;
import eionet.cr.util.sesame.SPARQLQueryUtil;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.PairReader;
import eionet.cr.util.sql.SingleObjectReader;
import eionet.cr.util.xlwrap.XLWrapUploadType;
import eionet.cr.web.util.ObservationFilter;

/**
 * A Virtuoso-specific implementation of {@link ScoreboardSparqlDAO}.
 * 
 * @author jaanus
 */
public class VirtuosoScoreboardSparqlDAO extends VirtuosoBaseDAO implements ScoreboardSparqlDAO {

    /** */
    private static final Logger LOGGER = Logger.getLogger(VirtuosoScoreboardSparqlDAO.class);

    // @formatter:off
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

    private static final String GET_CODELIST_ITEMS_SPARQL = "" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
            "select\n" +
            "  ?uri min(?notation) as ?skosNotation min(?prefLabel) as ?skosPrefLabel\n" +
            "where {\n" +
            "  ?scheme skos:hasTopConcept ?uri\n" +
            "  filter (?scheme = ?schemeUri)\n" +
            "  optional {?uri skos:notation ?notation}\n" +
            "  optional {?uri skos:prefLabel ?prefLabel}\n" +
            "}\n" +
            "group by ?uri\n" +
            "order by ?uri";

    private static final String EXPORT_CODELIST_ITEMS_SPARQL = "" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
            "PREFIX prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
            "select\n" +
            "  ?s ?p ?o ?memberOf ?order\n" +
            "where {\n" +
            "  ?s a ?type.\n" +
            "  ?s ?p ?o\n" +
            "  filter (?type = ?typeValue)\n" +
            "  optional {?s prop:membership ?membership}\n" +
            "  optional {?membership prop:member-of ?memberOf}\n" +
            "  optional {?membership prop:order ?order}\n" +
            "}\n" +
            "order by ?s ?p";

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

    private static final String GET_NOGROUP_MEMBERSHIPS = "" +
            "PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>\n" +
            "select ?y where {\n" +
            "  ?x dad-prop:membership ?y .\n" +
            "  ?y dad-prop:member-of ?groupUri.\n" +
            "  filter (?groupUri = ?groupUriValue)\n" +
            "}";

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

    /*
     * (non-Javadoc)
     * 
     * @see eionet.cr.dao.ScoreboardSparqlDAO#getCodelistItems(java.lang.String)
     */
    @Override
    public List<SkosItemDTO> getCodelistItems(String codelistUri) throws DAOException {

        if (StringUtils.isBlank(codelistUri)) {
            throw new IllegalArgumentException("Codelist URI must not be blank!");
        }

        Bindings bindings = new Bindings();
        bindings.setURI("schemeUri", codelistUri);

        List<SkosItemDTO> list = executeSPARQL(GET_CODELIST_ITEMS_SPARQL, bindings, new SkosItemsReader());
        return list;
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
            repoConn.add(identifierURI, descriptionPredicateURI, vf.createLiteral(dctermsDescription), graphURI);
            repoConn.add(identifierURI, distributionPredicateURI, distributionURI, graphURI);
            repoConn.add(identifierURI, modifiedPredicateURI, dateModified, graphURI);
            repoConn.add(identifierURI, dsdPredicateURI, vf.createURI(DEFAULT_DSD_URI), graphURI);

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
     * @see eionet.cr.dao.ScoreboardSparqlDAO#getFilterValues(java.util.Map, eionet.cr.web.util.ObservationFilter)
     */
    @Override
    public List<Pair<String, String>> getFilterValues(Map<ObservationFilter, String> selections, ObservationFilter filter)
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
        sb.append("  ?").append(filterAlias).append(" min(str(coalesce(?prefLabel, ?").append(filterAlias)
        .append("))) as ?label\n");
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
                    sb.append("  ?s <").append(availFilter.getPredicate()).append("> ?").append(availFilter.getAlias())
                    .append(".\n");
                }
            }
        }

        // sb.append("  ?s <").append(nextFilter.getPredicate()).append("> ?").append(forFilterAlias).append(".\n");
        sb.append("  ?s <").append(filter.getPredicate()).append("> ?").append(filterAlias).append(".\n");
        sb.append("  optional {?").append(filterAlias)
        .append(" skos:prefLabel ?prefLabel filter(lang(?prefLabel) in ('en',''))}\n");
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
    public int exportCodelistItems(String itemType, File templateFile, Map<String, Integer> mappings, File targetFile)
            throws DAOException {

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
                DELETE_INVALID_CODELIST_GROUP_MEMBERSHIPS.replace("@group-graph-uri@",
                        XLWrapUploadType.BREAKDOWN_GROUP.getGraphUri());

        String indicatorsSPARUL =
                DELETE_INVALID_CODELIST_GROUP_MEMBERSHIPS.replace("@group-graph-uri@",
                        XLWrapUploadType.INDICATOR_GROUP.getGraphUri());

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
}
