package eionet.cr.dao.virtuoso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import eionet.cr.common.Predicates;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.MockVirtuosoBaseDAOTest;
import eionet.cr.dto.SubjectDTO;

public class VirtuosoBaseDAOTest extends MockVirtuosoBaseDAOTest {

    public VirtuosoBaseDAOTest() {
        super("test-subjectsdata.nt");
    }

    @Test
    public void testSubjectsDataQuery() {

        String[] uris =
                {"http://rod.eionet.europa.eu/obligations/392", "http://rod.eionet.europa.eu/instruments/618",
                        "http://rod.eionet.europa.eu/issues/15", "http://planner.eionet.europa.eu/WorkPlan_2010/PRJ1752885689",
                        "http://planner.eionet.europa.eu/WorkPlan_2010/PRJ1607205326",
                        "http://planner.eionet.europa.eu/WorkPlan_2010/PRJ9599558008",
                        "http://planner.eionet.europa.eu/WorkPlan_2010/PRJ9193135010",
                        "http://www.eea.europa.eu/data-and-maps/figures/potential-climatic-tipping-elements",
                        "http://rod.eionet.europa.eu/obligations/171", "http://rod.eionet.europa.eu/obligations/661",
                        "http://rod.eionet.europa.eu/obligations/606", "http://rod.eionet.europa.eu/obligations/136",
                        "http://rod.eionet.europa.eu/obligations/520", "http://rod.eionet.europa.eu/obligations/522",
                        "http://rod.eionet.europa.eu/obligations/521"};

        List<String> subjectUris = Arrays.asList(uris);

        // only these predicates will be queried for
        String[] neededPredicates = {Predicates.RDF_TYPE, Predicates.RDFS_LABEL};

        // get the subjects data
        List<SubjectDTO> resultList = null;
        try {
            logger.trace("Free-text search, getting the data of the found subjects");
            resultList = getFoundSubjectsData(subjectUris, neededPredicates);
        } catch (DAOException e) {
            e.printStackTrace();
        }

        // String query = fakeDao.getSubjectsDataQ
        assertNotNull(resultList);
        // assertTrue(resultList.contains(arg0))
        assertEquals(resultList.size(), 15);
    }

    @Test
    public void testGetSubjectsData() {
        // subjecturis
        String[] s1 =
                {"http://rod.eionet.europa.eu/obligations/130", "http://rod.eionet.europa.eu/obligations/143",
                        "http://rod.eionet.europa.eu/instruments/381", "http://rod.eionet.europa.eu/instruments/273",
                        "http://rod.eionet.europa.eu/obligations/523", "http://rdfdata.eionet.europa.eu/eper/facilities/01035",
                        "http://rdfdata.eionet.europa.eu/eper/facilities/01039",
                        "http://rdfdata.eionet.europa.eu/eper/facilities/01046",
                        "http://rdfdata.eionet.europa.eu/eper/facilities/01047",
                        "http://rdfdata.eionet.europa.eu/eper/facilities/01052",
                        "http://rdfdata.eionet.europa.eu/eper/facilities/01053",
                        "http://rdfdata.eionet.europa.eu/eper/facilities/01054",
                        "http://rdfdata.eionet.europa.eu/eper/facilities/01055",
                        "http://rdfdata.eionet.europa.eu/eper/facilities/01068",
                        "http://rdfdata.eionet.europa.eu/eper/facilities/01074"};

        List<String> subjectUris = Arrays.asList(s1);

        // predicate uris
        String[] predicateUris = {"http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://www.w3.org/2000/01/rdf-schema#label"};
        try {
            getFoundSubjectsData(subjectUris, predicateUris);
        } catch (DAOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String expected = "select distinct "
                + "?s bif:either(isLiteral(?o0), bif:substring(str(?o0), 1, 800), ?o0) as ?val0 "
                + "bif:either(isLiteral(?o1), bif:substring(str(?o1), 1, 800), ?o1) as ?val1 "
                + "where {?s ?p ?o. optional {?s ?p0 ?o0} optional {?s ?p1 ?o1} "
                + "filter (?s IN (?subjectValue1,?subjectValue2,?subjectValue3,?subjectValue4,?subjectValue5,?subjectValue6,"
                + "?subjectValue7,?subjectValue8,?subjectValue9,?subjectValue10,?subjectValue11,?subjectValue12,?subjectValue13,"
                + "?subjectValue14,?subjectValue15)) } order by ?s";
        String actual = getSPARQL();
        assertEquals(expected, actual);

//        assertEquals(
//                "select ?g ?s ?p bif:either(isLiteral(?obj), fn:substring(str(?obj), 1, 2000), ?obj) as ?o where {graph ?g {?s ?p ?obj. filter (?s IN (?subjectValue1,?subjectValue2,?subjectValue3,"
//                        + "?subjectValue4,?subjectValue5,?subjectValue6,?subjectValue7,?subjectValue8,?subjectValue9,?subjectValue10,"
//                        + "?subjectValue11,?subjectValue12,?subjectValue13,?subjectValue14,?subjectValue15)) "
//                        + "filter (?p IN (?predicateValue1,?predicateValue2)) }} ORDER BY ?s ?p", getSPARQL());

    }

}
