package eionet.cr.dao.virtuoso.helpers;

import java.util.ArrayList;

import junit.framework.TestCase;
import eionet.cr.util.SortOrder;
import eionet.cr.util.SortingRequest;
import eionet.cr.util.pagination.PagingRequest;
import eionet.cr.web.util.columns.ReferringPredicatesColumn;

public class VirtuosoReferencesSearchHelperTest extends TestCase {
    public static void testUnOrderedQuery() {

        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest = new SortingRequest(null, SortOrder.parse(SortOrder.ASCENDING.toString()));
        String subjectUri = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#tag";
        // create query helper
        VirtuosoReferencesSearchHelper helper = new VirtuosoReferencesSearchHelper(subjectUri, pagingRequest, sortingRequest);

        String sparql = helper.getQuery(null);
        assertEquals("select distinct ?s where {?s ?p ?o. filter(isURI(?o) && ?o=?subjectUri)} limit 15 offset 0", sparql);
        assertTrue(helper.getQueryBindings().toString()
                .indexOf("subjectUri=http://cr.eionet.europa.eu/ontologies/contreg.rdf#tag") != -1);
        // select ?s where {?s ?p ?o. filter(isURI(?o) && ?o=<http://www.eea.europa.eu/hu/jelzesek/cikkek/a-talaj>)}
        // select ?s where {?s ?p ?o. filter(isURI(?o) && ?o=<http://cr.eionet.europa.eu/ontologies/contreg.rdf#tag>)}
    }

    public static void testOrderedQuery() {
        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest =
                new SortingRequest("http://www.w3.org/2000/01/rdf-schema#label", SortOrder.parse(SortOrder.ASCENDING.toString()));
        String subjectUri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
        // create query helper
        VirtuosoReferencesSearchHelper helper = new VirtuosoReferencesSearchHelper(subjectUri, pagingRequest, sortingRequest);

        String sparql = helper.getQuery(null);

        assertEquals(
                "select distinct ?s where {?s ?p ?o. filter(isURI(?o) && ?o=?subjectUri) . OPTIONAL {?s ?sortPredicate ?oorderby }}"
                        + " ORDER BY asc(bif:either( bif:isnull(?oorderby) , (bif:lcase(bif:subseq (bif:replace (?s, '/', '#'), "
                        + "bif:strrchr (bif:replace (?s, '/', '#'), '#')+1))) , bif:lcase(?oorderby))) limit 15 offset 0", sparql);
        assertTrue(helper.getQueryBindings().toString().indexOf("sortPredicate=http://www.w3.org/2000/01/rdf-schema#label") != -1);
    }

    public static void testOrderedByReference() {
        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest =
                new SortingRequest(ReferringPredicatesColumn.class.getSimpleName(), SortOrder.parse(SortOrder.DESCENDING
                        .toString()));
        String subjectUri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
        // create query helper
        VirtuosoReferencesSearchHelper helper = new VirtuosoReferencesSearchHelper(subjectUri, pagingRequest, sortingRequest);

        String sparql = helper.getQuery(null);
        assertEquals(
                "select distinct ?s where {?s ?p ?o. filter(isURI(?o) && ?o=?subjectUri)} ORDER BY desc(bif:lcase(?o)) limit 15 offset 0",
                sparql);
        assertTrue(helper.getQueryBindings().toString().indexOf("subjectUri=http://www.w3.org/1999/02/22-rdf-syntax-ns#Property") != -1);

        // Must NOT include sortPredicate in bindings
        assertTrue(helper.getQueryBindings().toString().indexOf("sortPredicate=") == -1);

    }

    public static void testGetSubjectsData() {
        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest = new SortingRequest(null, SortOrder.parse(SortOrder.ASCENDING.toString()));
        String subjectUri = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#tag";
        // create query helper
        VirtuosoReferencesSearchHelper helper = new VirtuosoReferencesSearchHelper(subjectUri, pagingRequest, sortingRequest);

        ArrayList<String> subjectUris = new ArrayList<String>();
        subjectUris.add("http://subjecturi1.nowhere.com");
        subjectUris.add("http://subjecturispecial21.somwhere.eu");
        subjectUris.add("_:blankuri");

        String sourceUri = "http://goodsource.eea.europa.eu";
        String query = helper.getSubjectsDataQuery(subjectUris, sourceUri);
        String expected = "select ?g ?s ?p bif:either(isLiteral(?o), bif:substring(str(?o), 1, 800), ?o) as ?o "
                + "where {graph ?g {?s ?p ?o. filter (?s IN (?subjectUriValue1,?subjectUriValue2,?subjectUriValue3)) . "
                + "filter(?p = <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> || "
                + "<http://www.w3.org/2000/01/rdf-schema#label> || (isURI(?o) && ?o=?sourceUri))}} ORDER BY ?s";
        assertEquals(expected, query);
    }
}
