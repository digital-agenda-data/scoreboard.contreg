package eionet.cr.util.jena;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.BNode;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFHandler;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;

import eionet.cr.common.CRRuntimeException;
import eionet.cr.common.Predicates;
import eionet.cr.util.Pair;
import eionet.cr.util.sesame.SesameUtil;

/**
 * Utility class for operations with Jena API.
 *
 * @author Jaanus
 */
public class JenaUtil {

    /** */
    private static final Logger LOGGER = Logger.getLogger(JenaUtil.class);

    /**
     * Disable utility class constructor.
     */
    private JenaUtil() {
        // Empty constructor.
    }

    /**
     *
     * @param model
     */
    public static void close(Model model) {
        if (model != null) {
            try {
                model.close();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    /**
     *
     * @param model
     * @param graphUri
     * @param clearGraph
     * @param stmtListener
     * @return
     * @throws OpenRDFException
     */
    public static Pair<Integer, Integer> saveModel(Model model, String fixedGraphUri, boolean clearGraph, RDFHandler stmtListener)
            throws OpenRDFException {

        HashSet<URI> clearedGraphs = new HashSet<>();
        HashSet<String> distinctNonAnonymousResources = new HashSet<String>();
        RepositoryConnection repoConn = null;
        try {
            repoConn = SesameUtil.getRepositoryConnection();
            repoConn.setAutoCommit(false);

            ValueFactory vf = repoConn.getValueFactory();
            int addedStmtCounter = 0;

            Map<String, String> dynamicSubjectGraphs = new HashMap<>();
            if (StringUtils.isBlank(fixedGraphUri)) {
                fillSubjectGraphs(model, dynamicSubjectGraphs);
            }

            StmtIterator statements = model.listStatements();
            while (statements.hasNext()) {

                Statement statement = statements.next();

                com.hp.hpl.jena.rdf.model.Resource jenaSubject = statement.getSubject();
                String subjectUri = jenaSubject.getURI();
                org.openrdf.model.Resource sesameSubject =
                        subjectUri != null ? vf.createURI(subjectUri.trim()) : vf.createBNode(jenaSubject.getId().toString());

                Property jenaPredicate = statement.getPredicate();
                String predicateUri = jenaPredicate.getURI();
                // Skip anonymous predicates as they should be theoretically impossible, and in case practically useless.
                if (predicateUri == null) {
                    continue;
                } else {
                    predicateUri = predicateUri.trim();
                }
                URI sesamePredicate = vf.createURI(predicateUri);

                RDFNode jenaObject = statement.getObject();
                Value sesameObject = null;

                if (jenaObject.isLiteral()) {

                    Literal jenaLiteral = jenaObject.asLiteral();
                    String language = jenaLiteral.getLanguage();
                    String lexicalForm = jenaLiteral.getLexicalForm();

                    if (StringUtils.isNotBlank(language)) {
                        sesameObject = vf.createLiteral(lexicalForm, language);
                    } else {
                        String datatypeUri = jenaLiteral.getDatatypeURI();
                        if (StringUtils.isNotBlank(datatypeUri)) {
                            sesameObject = vf.createLiteral(lexicalForm, vf.createURI(datatypeUri));
                        } else {
                            sesameObject = vf.createLiteral(lexicalForm);
                        }
                    }
                } else {
                    com.hp.hpl.jena.rdf.model.Resource jenaResource = jenaObject.asResource();
                    String uri = jenaResource.getURI();
                    if (uri != null) {
                        sesameObject = vf.createURI(uri.trim());
                    } else {
                        sesameObject = vf.createBNode(jenaResource.getId().toString());
                    }
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("Processing statement: <%s>\t<%s>\t<%s>", sesameSubject.stringValue(),
                            sesamePredicate.stringValue(), sesameObject.stringValue()));
                }

                URI graphURI = StringUtils.isBlank(fixedGraphUri) ? null : vf.createURI(fixedGraphUri);
                if (graphURI == null) {
                    String graphUriString = dynamicSubjectGraphs.get(sesameSubject.stringValue());
                    if (StringUtils.isNotBlank(graphUriString)) {
                        graphURI = vf.createURI(graphUriString);
                    }
                }

                if (graphURI == null) {
                    throw new CRRuntimeException("Failed to detect graph URI for subject " + sesameSubject.stringValue());
                } else if (clearGraph && !clearedGraphs.contains(graphURI)) {

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Clearing graph: " + graphURI);
                    }

                    repoConn.clear(graphURI);
                    clearedGraphs.add(graphURI);
                }

                repoConn.add(sesameSubject, sesamePredicate, sesameObject, graphURI);
                if (stmtListener != null) {
                    stmtListener
                            .handleStatement(new ContextStatementImpl(sesameSubject, sesamePredicate, sesameObject, graphURI));
                }

                addedStmtCounter++;
                if (!(sesameSubject instanceof BNode)) {
                    distinctNonAnonymousResources.add(sesameSubject.stringValue());
                }
            }

            if (addedStmtCounter > 0) {
                repoConn.commit();
            }

            return new Pair<Integer, Integer>(addedStmtCounter, distinctNonAnonymousResources.size());
        } catch (Error e) {
            SesameUtil.rollback(repoConn);
            throw e;
        } catch (RuntimeException e) {
            SesameUtil.rollback(repoConn);
            throw e;
        } catch (OpenRDFException e) {
            SesameUtil.rollback(repoConn);
            throw e;
        } finally {
            SesameUtil.close(repoConn);
        }
    }

    /**
     *
     * @param model
     * @param dynamicSubjectGraphs
     */
    private static void fillSubjectGraphs(Model model, Map<String, String> dynamicSubjectGraphs) {
        StmtIterator datasetStatements =
                model.listStatements((Resource) null, new PropertyImpl(Predicates.DATACUBE_DATA_SET), (RDFNode) null);
        while (datasetStatements != null && datasetStatements.hasNext()) {
            Statement stmt = datasetStatements.next();
            String datasetUri = stmt.getObject().toString();
            String graphUri = StringUtils.replace(datasetUri, "/dataset/", "/data/");
            dynamicSubjectGraphs.put(stmt.getSubject().getURI(), graphUri);
        }
    }

}
