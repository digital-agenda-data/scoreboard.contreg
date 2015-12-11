package eionet.cr.dao.virtuoso.helpers;

import junit.framework.TestCase;
import eionet.cr.util.SortOrder;
import eionet.cr.util.SortingRequest;
import eionet.cr.util.pagination.PagingRequest;

public class VirtuosoSearchBySourceHelperTest extends TestCase {

    public static void testOrderByLabel() {
        String sourceUrl = "http://www.eionet.europa.eu/rdf/portal_types.rdf";
        PagingRequest pagingRequest = PagingRequest.create(1);
        SortingRequest sortingRequest =
                new SortingRequest("http://www.w3.org/2000/01/rdf-schema#label", SortOrder.parse(SortOrder.ASCENDING.toString()));

        VirtuosoSearchBySourceHelper helper = new VirtuosoSearchBySourceHelper(sourceUrl, pagingRequest, sortingRequest);

        String sparql = helper.getQuery(null);
        String expected = "select distinct ?s from ?sourceUrl where {?s ?p ?o filter(!isBlank(?s)) optional {?s ?sortPredicate ?ord} } "
                + "ORDER BY asc(bif:lcase(bif:either(bif:isnull(?ord), (bif:subseq "
                + "(bif:replace (?s, '/', '#'), bif:strrchr (bif:replace (?s, '/', '#'), '#')+1)), ?ord))) limit 15 offset 0";
        assertEquals(expected, sparql);

        assertTrue(helper.getQueryBindings().toString().indexOf("sourceUrl=http://www.eionet.europa.eu/rdf/portal_types.rdf") != -1);
        assertTrue(helper.getQueryBindings().toString().indexOf("sortPredicate=http://www.w3.org/2000/01/rdf-schema#label") != -1);
    }
}
