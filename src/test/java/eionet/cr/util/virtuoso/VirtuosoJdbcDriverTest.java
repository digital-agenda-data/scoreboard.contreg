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
 * Agency. Portions created by TripleDev or Zero Technologies are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *        Enriko Käsper
 */

package eionet.cr.util.virtuoso;

import java.io.IOException;
import java.net.URI;
import java.sql.CallableStatement;
import java.sql.SQLException;

import junit.framework.TestCase;

import org.junit.Test;

import virtuoso.jdbc4.VirtuosoConnectionPoolDataSource;
import virtuoso.jdbc4.VirtuosoPooledConnection;
import eionet.cr.config.GeneralConfig;
import eionet.cr.test.helpers.RdfLoader;

/**
 * Test Virtuoso Jdbc driver.
 *
 * @author Enriko Käsper
 */
public class VirtuosoJdbcDriverTest extends TestCase {

    /**
     * Test if CR uses correct Virtuoso jdbc driver. It shouldn't get "Too many open statements" error.
     */
    @Test
    public void testTooManyOpenStmts() throws IOException, SQLException, InterruptedException {
        VirtuosoConnectionPoolDataSource dbsource = new VirtuosoConnectionPoolDataSource();

        String testDbURI = GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_URL);
        URI uri = URI.create(testDbURI.substring(5));

        dbsource.setServerName(uri.getHost());
        dbsource.setPortNumber(uri.getPort());
        dbsource.setPassword(GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_PWD));
        dbsource.setUser(GeneralConfig.getRequiredProperty(GeneralConfig.VIRTUOSO_DB_USR));
        dbsource.setCharset("UTF-8");
        VirtuosoPooledConnection pooledConnection = (VirtuosoPooledConnection) dbsource.getPooledConnection();
        virtuoso.jdbc4.VirtuosoConnection con = pooledConnection.getVirtuosoConnection();
        String jdbcComp = "DB.DBA.TTLP (?, ?, ?, ?)";
        CallableStatement stmt = null;
        int MAXIT = 10000;
        for (int i = 0; i < MAXIT; i++) {
            try {
                stmt = con.prepareCall(jdbcComp);
                stmt.setString(1, "");
                stmt.setString(2, "");
                stmt.setString(3, RdfLoader.getGraphUri("test.rdf"));
                stmt.setInt(4, 256);
                stmt.execute();
                con.commit();
            } catch (Exception e) {
                e.printStackTrace();
                fail("can't add data to virtuoso. ");
            } finally {
                if (stmt != null) {
                    stmt.close();
                }
            }
        }
    }
}
