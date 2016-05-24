package eionet.cr.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import eionet.cr.common.CRRuntimeException;
import eionet.cr.dao.readers.ResultSetReaderException;
import eionet.cr.util.sesame.SPARQLResultSetReader;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.sql.SQLUtil;
import virtuoso.sesame2.driver.VirtuosoRepository;

/**
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class TestGraphMigration {

    private static final int BATCH_SIZE = 9000;

    /**
     *
     * Class constructor.
     */
    public TestGraphMigration() {
    }

    /**
     * @throws SQLException
     * @throws OpenRDFException
     * @throws ResultSetReaderException
     *
     */
    private void testGraphMigration() throws SQLException, ResultSetReaderException, OpenRDFException {

        System.out.println("Starting ...");

        String queryTemplate = "select ?s ?p ?o from <http://semantic.digital-agenda-data.eu/data/digital-agenda-scoreboard-key-indicators> "
                + "where {?s ?p ?o} order by ?s ?p ?o limit @LIMIT@ offset @OFFSET@";

        RepositoryConnection sourceRepoConn = null;
        RepositoryConnection targetRepoConn = null;
        try {
            sourceRepoConn = getSourceRepositoryConnection();
            targetRepoConn = getTargetRepositoryConnection();

            int totalRowCount = 0;
            int queryCounter = 0;
            int limit = BATCH_SIZE;
            int offset = 0;
            boolean stop = true;
            do {
                queryCounter++;
                offset = offset == 0 ? 1 : offset + limit;
                String query = queryTemplate.replace("@LIMIT@", "" + limit).replace("@OFFSET@", "" + offset);

                if (queryCounter <= 3) {
                    System.out.println("query: " + query);
                }

                ResultSetReader reader = new ResultSetReader(targetRepoConn);
                SesameUtil.executeQuery(query, reader, sourceRepoConn);
                int rowCount = reader.getRowCount();
                totalRowCount = totalRowCount + rowCount;
                System.out.println("totalRowCount = " + totalRowCount);
                stop = rowCount == 0;
            } while (!stop);

            System.out.println("Finished!");
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            SesameUtil.close(targetRepoConn);
            SesameUtil.close(sourceRepoConn);
        }
    }

    /**
     * @throws SQLException
     * @throws OpenRDFException
     * @throws ResultSetReaderException
     *
     */
    private void testGraphDump() throws SQLException, ResultSetReaderException, OpenRDFException {

        System.out.println("Starting ...");

        Connection sqlConn = null;
        Statement stmt = null;
        try {
            sqlConn = getSourceSQLConnection();
            stmt = sqlConn.createStatement();
            stmt.execute("log_enable(2,1)");
            stmt.execute("DB.DBA.dump_one_graph('http://digital-agenda-data.eu/upload/scoreboard.model/rdf/external/geo.rdf', 'C:/dev/projects/scoreboard/apphome3/tmp/geo', 1000000000)");

//            SQLUtil.close(stmt);
//
//            String sparul2 = GRAPH_SYNC_SPARUL2.replace("%perm_graph%", permGraphStr);
//            sparul2 = sparul2.replace("%temp_graph%", tempGraphStr);
//            stmt = sqlConn.createStatement();
//            LOGGER.debug(BaseHarvest.loggerMsg("Executing 2nd XOR query", permGraph.stringValue()));
//            stmt.execute("log_enable(2,1)");
//            stmt.execute("SPARQL " + sparul2);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            SQLUtil.close(stmt);
            SQLUtil.close(sqlConn);
        }

        System.out.println("Finished!");
    }

    /**
     *
     * @throws SQLException
     * @throws ResultSetReaderException
     * @throws OpenRDFException
     */
    private void testLoadGraph() throws SQLException, ResultSetReaderException, OpenRDFException {

        System.out.println("Starting ...");

        Connection sqlConn = null;
        Statement stmt = null;
        try {
            sqlConn = getSourceSQLConnection();
            stmt = sqlConn.createStatement();

            System.out.println("Attempting ld_dir ...");
            stmt.execute("log_enable(2,1)");
            stmt.execute("DB.DBA.ld_dir('C:/dev/projects/scoreboard/apphome3/tmp', 'CORP%.gz', 'http://semantic.digital-agenda-data.eu/data/CORP')");

            SQLUtil.close(stmt);
            stmt = sqlConn.createStatement();

            System.out.println("Attempting loader_run ...");
            stmt.execute("log_enable(2,1)");
            stmt.execute("DB.DBA.rdf_loader_run()");
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            SQLUtil.close(stmt);
            SQLUtil.close(sqlConn);
        }

        System.out.println("Finished!");
    }

    private void testLoadGraph1() throws SQLException, ResultSetReaderException, OpenRDFException {

        System.out.println("Starting ...");

        Connection sqlConn = null;
        Statement stmt = null;
        try {
            sqlConn = getSourceSQLConnection();
            stmt = sqlConn.createStatement();

            System.out.println("Attempting ld_dir ...");
            stmt.execute("log_enable(2,1)");
            stmt.execute("DB.DBA.ld_dir('C:/dev/projects/scoreboard/apphome3/migration_packages/digital-agenda-scoreboard-key-indicators_heinlja_0523_153149', '%.ttl', 'http://mygraph.lt')");

            SQLUtil.close(stmt);
            stmt = sqlConn.createStatement();

            System.out.println("Attempting loader_run ...");
            stmt.execute("log_enable(2,1)");
            stmt.execute("DB.DBA.rdf_loader_run()");
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            SQLUtil.close(stmt);
            SQLUtil.close(sqlConn);
        }

        System.out.println("Finished!");
    }

    /**
     *
     * @throws SQLException
     * @throws ResultSetReaderException
     * @throws OpenRDFException
     */
    private void testLoadGraph2() throws SQLException, ResultSetReaderException, OpenRDFException {

        System.out.println("Starting ...");
        long startTime = System.currentTimeMillis();

        Connection sqlConn = null;
        Statement stmt = null;
        try {
            sqlConn = getTargetSQLConnection();
            stmt = sqlConn.createStatement();

            int mask = 1+2+4+8+16+32+64+128;
            stmt.execute("log_enable(2,1)");
            stmt.execute(String.format("DB.DBA.TTLP(file_to_string_output('C:/dev/projects/scoreboard/apphome3/migration_packages/lead-indicators_heinlja_0523_163154/lead-indicators_heinlja_0523_163154_data_000001.ttl'), '', 'http://mygraph.se', %d)", mask));
            //stmt.execute(String.format("DB.DBA.TTLP(file_to_string_output('C:/dev/projects/scoreboard/apphome3/migration_packages/digital-agenda-scoreboard-key-indicators_heinlja_0523_153149/digital-agenda-scoreboard-key-indicators_heinlja_0523_153149_data_000001.ttl'), '', 'http://mygraph.fi', %d)", mask));

        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            SQLUtil.close(stmt);
            SQLUtil.close(sqlConn);
        }

        long durationSeconds = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("Finished! Time: " + durationSeconds + " sec");
    }

    public static RepositoryConnection getSourceRepositoryConnection() throws RepositoryException {

        String url = "jdbc:virtuoso://localhost:1117/charset=UTF-8/log_enable=2/DATABASE=CR";
        String usr = "dba";
        String pwd = "y3VJRxSGmPDLsR/W";

        // The true in the last paramater means that Virtuoso will batch optimization for mass-executions
        // of VirtuosoRepositoryConnection's add(Resource subject ...) and add(Statement statement ...) methods.
        // Without this parameter there is a danger of running into "virtuoso.jdbc4.VirtuosoException:
        // SR491: Too many open statements" when using a pooled connection.
        Repository repository = new VirtuosoRepository(url, usr, pwd, true);
        try {
            repository.initialize();
            return repository.getConnection();
        } catch (RepositoryException e) {
            throw new CRRuntimeException(MessageFormat.format("Failed to initialize source repository {0} with user {1}", url, usr), e);
        }
    }

    public static RepositoryConnection getTargetRepositoryConnection() throws RepositoryException {

        String url = "jdbc:virtuoso://localhost:1116/charset=UTF-8/log_enable=2/DATABASE=CRTEST";
        String usr = "dba";
        String pwd = "dba";

        // The true in the last paramater means that Virtuoso will batch optimization for mass-executions
        // of VirtuosoRepositoryConnection's add(Resource subject ...) and add(Statement statement ...) methods.
        // Without this parameter there is a danger of running into "virtuoso.jdbc4.VirtuosoException:
        // SR491: Too many open statements" when using a pooled connection.
        Repository repository = new VirtuosoRepository(url, usr, pwd, true);
        try {
            repository.initialize();
            return repository.getConnection();
        } catch (RepositoryException e) {
            throw new CRRuntimeException(MessageFormat.format("Failed to initialize target repository {0} with user {1}", url, usr), e);
        }
    }

    public static Connection getTargetSQLConnection() throws SQLException {

        String drv = "virtuoso.jdbc4.Driver";
        String url = "jdbc:virtuoso://localhost:1116/charset=UTF-8/log_enable=2/DATABASE=CRTEST";
        String usr = "dba";
        String pwd = "dba";

        try {
            Class.forName(drv);
            return DriverManager.getConnection(url, usr, pwd);
        } catch (ClassNotFoundException e) {
            throw new CRRuntimeException("Failed to get connection, driver class not found: " + drv, e);
        }
    }

    public static Connection getSourceSQLConnection() throws SQLException {

        String drv = "virtuoso.jdbc4.Driver";
        String url = "jdbc:virtuoso://localhost:1117/charset=UTF-8/log_enable=2/DATABASE=CR";
        String usr = "dba";
        String pwd = "y3VJRxSGmPDLsR/W";

        try {
            Class.forName(drv);
            return DriverManager.getConnection(url, usr, pwd);
        } catch (ClassNotFoundException e) {
            throw new CRRuntimeException("Failed to get connection, driver class not found: " + drv, e);
        }
    }

    public static void main(String[] args) throws SQLException, ResultSetReaderException, OpenRDFException {

        TestGraphMigration test = new TestGraphMigration();
        //test.testGraphMigration();
        //test.testGraphDump();
        //test.testLoadGraph2();
        //test.testLoadGraph1();
    }

    public class ResultSetReader implements SPARQLResultSetReader<String> {

        private RepositoryConnection targetRepoConn;
        private URI testGraphURI;
        int rowCount = 0;

        /**
         *
         * Class constructor.
         *
         * @param targetRepoConn
         */
        public ResultSetReader(RepositoryConnection targetRepoConn) {
            this.targetRepoConn = targetRepoConn;
            testGraphURI = targetRepoConn.getValueFactory().createURI("http://test.graph");
        }

        @Override
        public List<String> getResultList() {
            return new ArrayList<String>();
        }

        @Override
        public void endResultSet() {
        }

        @Override
        public void startResultSet(List<String> bindingNames) {
        }

        @Override
        public void readRow(BindingSet bindingSet) throws ResultSetReaderException {

            Resource subject = (Resource) bindingSet.getValue("s");
            URI predicate = (URI) bindingSet.getValue("p");
            Value object = bindingSet.getValue("o");

            if (++rowCount == 1) {
                System.out.println(subject + "   " + predicate + "   " + object);
            }

            // try {
            // targetRepoConn.add(subject, predicate, object, testGraphURI);
            // } catch (RepositoryException e) {
            // throw new CRRuntimeException(e);
            // }
        }

        /**
         * @return the counter
         */
        public int getRowCount() {
            return rowCount;
        }
    }
}
