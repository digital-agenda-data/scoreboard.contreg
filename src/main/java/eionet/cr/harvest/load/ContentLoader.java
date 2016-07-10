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
import java.sql.Connection;

import org.openrdf.OpenRDFException;
import org.openrdf.repository.RepositoryConnection;

/**
 * The callback methods of this interface allow to load a given content into a given repository connection.
 *
 * @author Jaanus Heinlaid
 */
public interface ContentLoader {

    /**
     * Callback method that loads content from the given input stream in the given repository connection. SQL connection is also
     * given, in case the loader needs it.
     *
     * @param inputStream The input stream to load.
     * @param repoConn The repository connection where the content is loaded into.
     * @param sqlConn SQL connection in case the loader needs to use it.
     * @param baseUri Base URI which the relative URIs in the content will be resolved against.
     * @param contextUri URI of the graph where the triples will be loaded into.
     *
     * @return Number of triples loaded.
     * @throws IOException if I/O error
     * @throws OpenRDFException if an error occurs in RDF parser
     * @throws ContentParsingException if content parsing fails
     */
    int load(InputStream inputStream, RepositoryConnection repoConn, Connection sqlConn, String baseUri, String contextUri)
            throws IOException, OpenRDFException, ContentParsingException;

    /**
     * Sets timeout to the loader. If the timeout is exceeded the loading stops.
     *
     * @param timeout Timeout in milliseconds
     */
    void setTimeout(long timeout);
}
