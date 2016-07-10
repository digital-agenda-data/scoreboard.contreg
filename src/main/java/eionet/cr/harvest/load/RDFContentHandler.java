/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Content Registry 3
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency. Portions created by Zero Technologies are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *        Jaanus Heinlaid
 */

package eionet.cr.harvest.load;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

import eionet.cr.harvest.TimeoutException;
import eionet.cr.util.sql.SQLUtil;

/**
 * Implementation of OpenRDF's {@link RDFHandler} that will be used by implementations of {@link ContentLoader}. Contains callback
 * methods for listening to the content coming from {@link ContentLoader} and loading them into repository.
 *
 * @author Jaanus Heinlaid
 */
public class RDFContentHandler implements RDFHandler {

    /** */
    private static final Logger LOGGER = Logger.getLogger(RDFContentHandler.class);

    /**
     * Triples will be logged and/or committed in batches of this size. Virtuoso's Sesame driver uses 5000 by default.
     */
    private static final int BATCH_SIZE = 5000;

    /** Connections to the repository and SQL database where the content is persisted into. */
    private RepositoryConnection repoConn;
    private Connection sqlConn;

    /** The graph where the triples will be loaded into. */
    private Resource context;

    /** Number of triples saved. */
    private int triplesSaved;

    /** Value factory created from the repository connection. */
    private ValueFactory valueFactory;

    /**
     * total time of processing RDF.
     */
    private long totalTime;

    /**
     * total time of processing RDF.
     */
    private long startTime;

    private final long timeout;

    /**
     * @param repoConn
     * @param sqlConn
     * @param contextUri
     *            The URI of the graph where the triples will be loaded into.
     */
    public RDFContentHandler(RepositoryConnection repoConn, Connection sqlConn, String contextUri, long timeout) {

        this.repoConn = repoConn;
        this.sqlConn = sqlConn;
        this.valueFactory = repoConn.getValueFactory();
        this.context = this.valueFactory.createURI(contextUri);

        this.timeout = timeout;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.rio.RDFHandler#startRDF()
     */
    @Override
    public void startRDF() throws RDFHandlerException {
        // No specific handling here.
        startTime = System.currentTimeMillis();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.rio.RDFHandler#handleNamespace(java.lang.String, java.lang.String)
     */
    @Override
    public void handleNamespace(String prefix, String uri) throws RDFHandlerException {

        // Code copy-pasted from VirtuosoRepositoryConnection.

        String str = "DB.DBA.XML_SET_NS_DECL(?, ?, 1)";
        PreparedStatement stmt = null;
        try {
            stmt = sqlConn.prepareStatement(str);
            stmt.setString(1, prefix);
            stmt.setString(2, uri);
            stmt.execute();
        } catch (SQLException e) {
            throw new RDFHandlerException("Problem executing query: " + str, e);
        } finally {
            SQLUtil.close(stmt);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.rio.RDFHandler#handleStatement(org.openrdf.model.Statement)
     */
    @Override
    public void handleStatement(Statement rdfStatement) throws RDFHandlerException {

        if ("http://semantic.digital-agenda-data.eu/data/lead-indicators/e_itspvac2/ent_sm_xfin/pc_ent/FR/2012".equals(rdfStatement.getSubject().stringValue())) {
            System.out.println("--- " + rdfStatement);
        }

        if ("http://semantic.digital-agenda-data.eu/data/lead-indicators/i_bfeu/cc_ext_eu/pc_ind/IT/2014".equals(rdfStatement.getSubject().stringValue())) {
            System.out.println("--- " + rdfStatement);
        }

        // Pre-process the statement.
        rdfStatement = preProcess(rdfStatement);

        // Add the given statement (i.e. triple) into repository.
        try {
            repoConn.add(rdfStatement, context);
            triplesSaved++;
            totalTime = System.currentTimeMillis();

            // check timeout:
            if (timeout > 0 && (totalTime - startTime > timeout)) {
                throw new TimeoutException("Timeout (" + timeout + "ms) exceeded when parsing triples");
            }

            if (triplesSaved % BATCH_SIZE == 0) {
                LOGGER.trace("Statement counter = " + triplesSaved);
            }

        } catch (Exception e) {
            LOGGER.debug("Exception when handling this statement: " + rdfStatement);
            throw new RDFHandlerException(e.getMessage(), e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.rio.RDFHandler#handleComment(java.lang.String)
     */
    @Override
    public void handleComment(String arg0) throws RDFHandlerException {
        // No specific handling here.
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.rio.RDFHandler#endRDF()
     */
    @Override
    public void endRDF() throws RDFHandlerException {
        // No specific handling here.
    }

    /**
     * Does some pre-processing on the given RDF statement if necessary, and returns the resulting the statement.
     *
     * For example objects of http://www.openlinksw.com/schemas/virtrdf#Geometry data-type are converted into
     * http://www.w3.org/2001/XMLSchema#string, because the former is not supported by Virtuoso's Open-Source edition.
     *
     * @param statement
     * @return
     */
    private Statement preProcess(Statement statement) {

        Statement result = statement;

        Value object = statement.getObject();
        if (object instanceof Literal) {
            URI datatype = ((Literal) object).getDatatype();
            if (datatype != null && datatype.stringValue().equals("http://www.openlinksw.com/schemas/virtrdf#Geometry")) {
                result =
                        valueFactory.createStatement(statement.getSubject(), statement.getPredicate(),
                                valueFactory.createLiteral(object.stringValue()));
            }
        }

        return result;
    }

    /**
     * @return the triplesLoaded
     */
    public int getNumberOfTriplesSaved() {
        return triplesSaved;
    }
}
