package eionet.cr.dao.virtuoso.helpers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openrdf.repository.RepositoryConnection;

import eionet.cr.util.Bindings;
import eionet.cr.util.SortOrder;
import eionet.cr.util.SortingRequest;
import eionet.cr.util.pagination.PagingRequest;
import eionet.cr.util.sesame.SPARQLQueryUtil;
import eionet.cr.util.sesame.SesameConnectionProvider;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.SingleObjectReader;

public class VirtuosoFilteredSearchHelperTest {

    @Test
    public void testBuildTypeSearchQuery() {
        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest = new SortingRequest(null, SortOrder.parse(SortOrder.ASCENDING.toString()));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://rod.eionet.europa.eu/schema.rdf#Obligation");

        VirtuosoFilteredSearchHelper helper = new VirtuosoFilteredSearchHelper(filters, null, pagingRequest, sortingRequest, true);

        ArrayList<Object> inParams = new ArrayList<Object>();

        String query = helper.getQuery(inParams);

        assertEquals(query, SPARQLQueryUtil.getCrInferenceDefinitionStr() + "select distinct ?s where {?s ?p1 ?o1 . "
                + "filter(?p1 = ?p1Val) . filter(?o1 = ?o1Val)} limit 15 offset 0");

    }

    @Test
    public void testOrderedQuery() {
        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest =
                new SortingRequest("http://www.w3.org/2000/01/rdf-schema#label", SortOrder.parse(SortOrder.DESCENDING.toString()));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("http://purl.org/dc/elements/1.1/creator", "Roug");
        VirtuosoFilteredSearchHelper helper = new VirtuosoFilteredSearchHelper(filters, null, pagingRequest, sortingRequest, true);

        ArrayList<Object> inParams = new ArrayList<Object>();

        String query = helper.getOrderedQuery(inParams);

        assertEquals(query, SPARQLQueryUtil.getCrInferenceDefinitionStr() + "select distinct ?s where {?s ?p1 ?o1 . "
                + "filter(?p1 = ?p1Val) . filter (regex(?o1, ?o1Val, \"i\")) . OPTIONAL {?s ?sortPred ?sortObjVal}} "
                + "order by desc(bif:either( bif:isnull(?sortObjVal) , (bif:lcase(bif:subseq (bif:replace (?s, '/', '#'), "
                + "bif:strrchr (bif:replace (?s, '/', '#'), '#')+1))) , bif:lcase(?sortObjVal)))");
    }

    @Test
    public void testUnOrderedQuery() {
        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest = new SortingRequest(null, SortOrder.parse(SortOrder.ASCENDING.toString()));

        Map<String, String> filters = new HashMap<String, String>();
        // http://rod.eionet.europa.eu/schema.rdf#obligation=http://rod.eionet.europa.eu/obligations/15,
        // http://www.w3.org/2000/01/rdf-schema#label=CLRTAP}
        filters.put("http://rod.eionet.europa.eu/schema.rdf#obligation", "http://rod.eionet.europa.eu/obligations/15");
        filters.put("http://www.w3.org/2000/01/rdf-schema#label", "CLRTAP");
        VirtuosoFilteredSearchHelper helper = new VirtuosoFilteredSearchHelper(filters, null, pagingRequest, sortingRequest, true);

        ArrayList<Object> inParams = new ArrayList<Object>();

        String query = helper.getUnorderedQuery(inParams);

        assertEquals(query, SPARQLQueryUtil.getCrInferenceDefinitionStr() + "select distinct ?s where {?s ?p1 ?o1 . "
                + "filter(?p1 = ?p1Val) . filter(?o1 = ?o1Val) . ?s ?p2 ?o2 . filter(?p2 = ?p2Val) . filter (regex(?o2, ?o2Val, \"i\"))}");
    }

    @Test
    public void testQueryParameters() {
        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest = new SortingRequest(null, SortOrder.parse(SortOrder.ASCENDING.toString()));

        Map<String, String> filters = new HashMap<String, String>();
        // http://rod.eionet.europa.eu/schema.rdf#obligation=http://rod.eionet.europa.eu/obligations/15,
        // http://www.w3.org/2000/01/rdf-schema#label=CLRTAP}
        filters.put("http://rod.eionet.europa.eu/schema.rdf#obligation", "http://rod.eionet.europa.eu/obligations/15");
        filters.put("http://www.w3.org/2000/01/rdf-schema#label", "CLRTAP");
        filters.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://rod.eionet.europa.eu/schema.rdf#Delivery");
        VirtuosoFilteredSearchHelper helper = new VirtuosoFilteredSearchHelper(filters, null, pagingRequest, sortingRequest, true);

        String paramStr = helper.getWhereContents();

        String expected =
                "?s ?p1 ?o1 . filter(?p1 = ?p1Val) . filter(?o1 = ?o1Val) . ?s ?p2 ?o2 . filter(?p2 = ?p2Val) . "
                        + "filter(?o2 = ?o2Val) . ?s ?p3 ?o3 . filter(?p3 = ?p3Val) . filter (regex(?o3, ?o3Val, \"i\"))";
        if (Runtime.class.getPackage().getImplementationVersion().trim().startsWith("1.8")) {
            expected =
                    "?s ?p1 ?o1 . filter(?p1 = ?p1Val) . filter(?o1 = ?o1Val) . ?s ?p2 ?o2 . filter(?p2 = ?p2Val) . "
                            + "filter (regex(?o2, ?o2Val, \"i\")) . ?s ?p3 ?o3 . filter(?p3 = ?p3Val) . filter(?o3 = ?o3Val)";
        }
        assertEquals(expected, paramStr);
    }

    private void checkQuery(String query, Bindings bindings) {
        RepositoryConnection conn = null;
        try {
            conn = SesameConnectionProvider.getReadOnlyRepositoryConnection();
            SingleObjectReader<String> reader = new SingleObjectReader<String>();

            SesameUtil.executeQuery(query, bindings, reader, conn);

            List<String> results = reader.getResultList();

            for (String url : results) {
                System.out.println("URL " + url);
            }
        } catch (Exception e) {
            System.out.println("ERROR " + e);
        } finally {
            SesameUtil.close(conn);
        }

    }

    @Test
    public void testOrderedQueryIncludingLabel() {
        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest =
                new SortingRequest("http://www.w3.org/2000/01/rdf-schema#label", SortOrder.parse(SortOrder.ASCENDING.toString()));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("http://www.w3.org/2000/01/rdf-schema#label", "ippc");
        VirtuosoFilteredSearchHelper helper = new VirtuosoFilteredSearchHelper(filters, null, pagingRequest, sortingRequest, true);

        ArrayList<Object> inParams = new ArrayList<Object>();

        String query = helper.getOrderedQuery(inParams);

        String expected =
                SPARQLQueryUtil.getCrInferenceDefinitionStr() + "select distinct ?s where {?s ?p1 ?o1 . "
                        + "filter(?p1 = ?p1Val) . filter (regex(?o1, ?sortObjVal, \"i\"))} order by asc(bif:either( bif:isnull(?sortObjVal) , "
                        + "(bif:lcase(bif:subseq (bif:replace (?s, '/', '#'), bif:strrchr (bif:replace (?s, '/', '#'), '#')+1))) , "
                        + "bif:lcase(?sortObjVal)))";
        String actual = query;
        assertEquals(expected, actual);
    }

    @Test
    public void testOrderedQueryIncludingLabelNoInference() {
        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest =
                new SortingRequest("http://www.w3.org/2000/01/rdf-schema#label", SortOrder.parse(SortOrder.ASCENDING.toString()));

        Map<String, String> filters = new HashMap<String, String>();
        filters.put("http://www.w3.org/2000/01/rdf-schema#label", "ippc");
        VirtuosoFilteredSearchHelper helper = new VirtuosoFilteredSearchHelper(filters, null, pagingRequest, sortingRequest, false);
        ArrayList<Object> inParams = new ArrayList<Object>();

        String query = helper.getOrderedQuery(inParams);

        assertEquals("select distinct ?s where {?s ?p1 ?o1 . filter(?p1 = ?p1Val) . filter (regex(?o1, ?sortObjVal, \"i\"))} "
                + "order by asc(bif:either( bif:isnull(?sortObjVal) , (bif:lcase(bif:subseq (bif:replace (?s, '/', '#'), "
                + "bif:strrchr (bif:replace (?s, '/', '#'), '#')+1))) , bif:lcase(?sortObjVal)))", query);
    }

}
