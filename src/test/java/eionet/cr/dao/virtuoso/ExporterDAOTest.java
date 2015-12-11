package eionet.cr.dao.virtuoso;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import eionet.cr.dao.DAOException;
import eionet.cr.dao.readers.SubjectExportReader;
import eionet.cr.util.Bindings;
import eionet.cr.util.export.XmlExporter;
import eionet.cr.util.sesame.SPARQLQueryUtil;
import eionet.cr.util.sesame.SPARQLResultSetReader;

/**
 * Mock for exporterDAO.
 */
public class ExporterDAOTest extends VirtuosoExporterDAO {

    /**
     * SPARQL is stored here.
     */
    private String sparql;
    /**
     * Query bindings.
     */
    private Bindings bindings;

    /**
     * test building query.
     */
    @Test
    public void testGetQuery() {
        HashMap<String, String> filters = new HashMap<String, String>();
        filters.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://rod.eionet.europa.eu/schema.rdf#Obligation");
        filters.put("http://www.w3.org/2000/01/rdf-schema#label", "CLRTAP");
        List<String> selectedPredicates = new ArrayList<String>();
        selectedPredicates.add("http://www.w3.org/2000/01/rdf-schema#label");
        selectedPredicates.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        selectedPredicates.add("http://purl.org/dc/terms/title");
        selectedPredicates.add("http://purl.org/dc/terms/abstract");
        selectedPredicates.add("http://rod.eionet.europa.eu/schema.rdf#terminated");

        // VirtuosoFilteredSearchHelper helper = new VirtuosoFilteredSearchHelper(filters, null,
        // null, null);
        XmlExporter exporter = new XmlExporter();
        SubjectExportReader reader = new SubjectExportReader(exporter);

        try {
            exportByTypeAndFilters(filters, selectedPredicates, reader);
        } catch (DAOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String expected =
                SPARQLQueryUtil.getCrInferenceDefinitionStr() + "select distinct * where {?s ?p ?o .?s ?p1 ?o1 . filter(?p1 = ?p1Val) . "
                        + "filter(?o1 = ?o1Val) . ?s ?p2 ?o2 . filter(?p2 = ?p2Val) . filter (regex(?o2, ?o2Val, \"i\")) . "
                        + "filter (?p IN (?exportPredicateValue1,?exportPredicateValue2,?exportPredicateValue3,"
                        + "?exportPredicateValue4,?exportPredicateValue5))} ORDER BY ?s";

        if (Runtime.class.getPackage().getImplementationVersion().trim().startsWith("1.8")) {
            expected =
                    SPARQLQueryUtil.getCrInferenceDefinitionStr() + "select distinct * where {" + "?s ?p ?o .?s ?p1 ?o1 . filter(?p1 = ?p1Val) . "
                            + "filter (regex(?o1, ?o1Val, \"i\")) . " + "?s ?p2 ?o2 . filter(?p2 = ?p2Val) . " + "filter(?o2 = ?o2Val) . "
                            + "filter (?p IN ("
                            + "?exportPredicateValue1,?exportPredicateValue2,?exportPredicateValue3,?exportPredicateValue4,?exportPredicateValue5))"
                            + "} ORDER BY ?s";
        }
        String actual = sparql;
        assertEquals(expected, actual);
    }

    @Override
    protected <T> List<T> executeSPARQL(String sparql, Bindings bindings, SPARQLResultSetReader<T> reader) throws DAOException {
        // TODO Auto-generated method stub
        this.sparql = sparql;
        this.bindings = bindings;

        return null;
    }

    @Override
    protected <T> List<T> executeSPARQL(String sparql, SPARQLResultSetReader<T> reader) throws DAOException {
        // TODO Auto-generated method stub
        return executeSPARQL(sparql, null, reader);
    }
}
