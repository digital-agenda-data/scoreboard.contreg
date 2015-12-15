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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;

import org.openrdf.OpenRDFException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;

/**
 * Implementation of {@link ContentLoader} for the content in {@link RDFFormat}.
 *
 * @author Jaanus Heinlaid
 */
public class RDFFormatLoader implements ContentLoader {

    /** */
    private final RDFFormat rdfFormat;

    /**
     * timeout for RDF loading.
     */
    private long timeout;

    /**
     * The loader will expect content in the given {@link RDFFormat}.
     *
     * @param rdfFormat
     */
    public RDFFormatLoader(RDFFormat rdfFormat) {
        this.rdfFormat = rdfFormat;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.harvest.load.ContentLoader#load(java.io.InputStream, org.openrdf.repository.RepositoryConnection,
     * java.sql.Connection, java.lang.String, java.lang.String)
     */
    @Override
    public int load(InputStream inputStream, RepositoryConnection repoConn, Connection sqlConn, String baseUri, String contextUri)
            throws IOException, OpenRDFException, ContentParsingException {

        // Let Sesame create an RDF parser.
        RDFParser rdfParser = Rio.createParser(rdfFormat, repoConn.getValueFactory());
        rdfParser.setVerifyData(true);
        rdfParser.setStopAtFirstError(true);

        // By default, Virtuoso's implementation of RepositoryConnection.add(File ...) ignores data types,
        // so doing the same here by default.
        rdfParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

        // Set the RDF parser's RDF handler to our implementation that loads triples into repository.
        RDFContentHandler rdfHandler = new RDFContentHandler(repoConn, sqlConn, contextUri, timeout);
        rdfParser.setRDFHandler(rdfHandler);

        // Parse the stream, return number of loaded triples.
        rdfParser.parse(new InputStreamReader(inputStream), baseUri);
        return rdfHandler.getNumberOfTriplesSaved();
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.harvest.load.ContentLoader#setTimeout(long)
     */
    @Override
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

}
