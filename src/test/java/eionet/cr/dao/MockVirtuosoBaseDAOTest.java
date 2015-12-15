package eionet.cr.dao;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.n3.N3ParserFactory;

import eionet.cr.dao.readers.ResultSetReaderException;
import eionet.cr.dao.virtuoso.VirtuosoBaseDAO;
import eionet.cr.util.Bindings;
import eionet.cr.util.sesame.SPARQLResultSetReader;

/**
 * Mock for testing queries. Working only for testing readers. Each implementing class must have a corresponding file in N3 format
 * that can be exported from SPARQL endpoint.
 */
@Ignore
public class MockVirtuosoBaseDAOTest extends VirtuosoBaseDAO {
    /** Tag name in N3. */
    private static final String RESULTVARIABLE_NAME = "http://www.w3.org/2005/sparql-results#resultVariable";
    /** Tag name in N3. */
    private static final String VARIABLE_NAME = "http://www.w3.org/2005/sparql-results#variable";
    /** Tag name in N3. */
    private static final String VALUE_NAME = "http://www.w3.org/2005/sparql-results#value";
    /** Tag name in N3. */
    private static final String SOLUTION_NAME = "http://www.w3.org/2005/sparql-results#solution";

    /**
     * to catch SPARQL.
     */
    private String sparql;
    private Bindings bindings;

    /**
     * Fake list of bindingSet.
     */
    private ArrayList<MapBindingSet> bindingSet;
    /** custom RDF handler. */
    private MockRDFHandler rdfHandler = new MockRDFHandler();
    /** binding names araylist. */
    private List<String> bindingNames = new ArrayList<String>();

    /**
     * Default construtor. File name with test data is given as the parameter. Supports files in N3 format exported from SPARQL
     * endpoint.
     *
     * @param fileName Test data file name in test-resources.
     */
    public MockVirtuosoBaseDAOTest(String fileName) {
        bindingSet = new ArrayList<MapBindingSet>();
        // TODO study why RDF reader is not working properly
        // RDFParserFactory factory = new
        // RDFParserRegistry().get(RDFFormat.forFileName(fileName));
        N3ParserFactory factory = new N3ParserFactory();
        RDFParser parser = factory.getParser();

        parser.setStopAtFirstError(false);
        parser.setRDFHandler(rdfHandler);

        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(fileName));
            parser.parse(reader, "http://127.0.0.1/test");
        } catch (RDFParseException e) {
            e.printStackTrace();
        } catch (RDFHandlerException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    @Override
    protected <T> List<T> executeSPARQL(String sparql, SPARQLResultSetReader<T> reader) throws DAOException {
        // TODO Auto-generated method stub
        return executeSPARQL(sparql, null, reader);
    }

    @Override
    protected <T> List<T> executeSPARQL(String sparql, Bindings bindings, SPARQLResultSetReader<T> reader) throws DAOException {

        for (BindingSet binding : bindingSet) {
            try {
                reader.readRow(binding);
            } catch (ResultSetReaderException e) {
                e.printStackTrace();
            }
        }

        this.sparql = sparql;
        this.bindings = bindings;
        return reader.getResultList();
    }

    /**
     * internal RDF handler for creating fake bindingset objects.
     */
    class MockRDFHandler implements RDFHandler {
        private boolean bindingsAlreadyAdded = false;

        private MapBindingSet currentBinding;
        private String currentVarName;

        @Override
        public void endRDF() throws RDFHandlerException {
            // add last value
            if (currentBinding != null) {
                bindingSet.add(currentBinding);
            }

        }

        @Override
        public void handleComment(String arg0) throws RDFHandlerException {

        }

        @Override
        public void handleNamespace(String arg0, String arg1) throws RDFHandlerException {

        }

        /**
         * creates a fake bindingset object.
         */
        @Override
        public void handleStatement(Statement statement) throws RDFHandlerException {
            // new binding started:
            if (statement.getPredicate() != null && statement.getPredicate().stringValue().equals(SOLUTION_NAME)) {
                if (!bindingsAlreadyAdded) {
                    bindingsAlreadyAdded = true;
                } else {
                    bindingSet.add(currentBinding);
                }
                currentBinding = new MapBindingSet();
            }
            // its a binding name
            if (statement.getPredicate() != null && statement.getPredicate().stringValue().equals(RESULTVARIABLE_NAME)) {
                bindingNames.add(statement.getObject().stringValue());
            }
            // new variable Starts
            if (statement.getPredicate() != null && statement.getPredicate().stringValue().equals(VARIABLE_NAME)) {
                currentVarName = statement.getObject().stringValue();
            }

            // actual value
            if (statement.getPredicate() != null && statement.getPredicate().stringValue().equals(VALUE_NAME)) {
                Value value = statement.getObject();
                currentBinding.addBinding(currentVarName, value);
            }
        }

        @Override
        public void startRDF() throws RDFHandlerException {
        }
    }

    public String getSPARQL() {
        return sparql;
    }

    public Bindings getBindings() {
        return bindings;
    }
}
