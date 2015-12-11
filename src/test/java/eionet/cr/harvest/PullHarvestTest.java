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
 * The Original Code is Content Registry 2.0.
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency.  Portions created by Tieto Eesti are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 * Søren Roug, European Environment Agency
 */
package eionet.cr.harvest;

import org.apache.commons.lang.math.NumberUtils;
import org.dbunit.dataset.IDataSet;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.XMLSchema;

import eionet.cr.common.Predicates;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.dto.ObjectDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.test.helpers.CRDatabaseTestCase;
import eionet.cr.test.helpers.JettyUtil;

/**
 *
 * @author roug
 *
 */
public class PullHarvestTest extends CRDatabaseTestCase {

    /** */
    // private static final String[] ignoreCols = {"SOURCE", "GEN_TIME"};

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.test.helpers.CRDatabaseTestCase#getDataSet()
     */
    @Override
    protected IDataSet getDataSet() throws Exception {
        return getXmlDataSet("emptydb.xml");
    }

    @Test
    public void testSimpleRdf() throws Exception {

        Server server = null;
        try {
            server = JettyUtil.startResourceServerMock(8999, "/testResources", "simple-rdf.xml");
            String url = "http://localhost:8999/testResources/simple-rdf.xml";

            HarvestSourceDTO source = new HarvestSourceDTO();
            source.setUrl(url);
            source.setIntervalMinutes(5);
            DAOFactory daoFactory = DAOFactory.get();
            daoFactory.getDao(HarvestSourceDAO.class).addSource(source);

            PullHarvest harvest = new PullHarvest(url);
            harvest.execute();
            assertTrue(harvest.isSourceAvailable());
            assertEquals(12, harvest.getStoredTriplesCount());

            SubjectDTO subject = daoFactory.getDao(HelperDAO.class).getSubject(url);
            assertNotNull("Expected existing subject for " + url, subject);
            assertEquals("Unexpected subject URI" + url, url, subject.getUri());

            ObjectDTO byteSizeObj = subject.getObject(Predicates.CR_BYTE_SIZE);
            assertNotNull("Expected existing object for " + Predicates.CR_BYTE_SIZE, byteSizeObj);

            URI datatype = byteSizeObj.getDatatype();
            assertNotNull("Expected object datatype", datatype);
            // Although we expect xsd:integer, the Sesame driver wrongly returns xsd:int.
            assertEquals("Unexpected datatype", XMLSchema.INT.stringValue(), datatype.stringValue());
            assertTrue("Unexpected byte size", NumberUtils.toInt(byteSizeObj.getValue(), -1) > 0);
        } finally {
            JettyUtil.close(server);
        }
    }

    @Test
    public void testHarvestNonExistingURL() {

        try {
            String url = "http://www.jaanusheinlaid.tw";
            HarvestSourceDTO source = new HarvestSourceDTO();
            source.setUrl(url);
            source.setIntervalMinutes(5);
            DAOFactory.get().getDao(HarvestSourceDAO.class).addSource(source);

            PullHarvest harvest = new PullHarvest(url);
            harvest.execute();
            assertFalse(harvest.isSourceAvailable());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Was not expecting this exception: " + e.toString());
        }
    }

    @Test
    public void testEncodingRdf() throws Exception {

        Server server = null;
        try {
            server = JettyUtil.startResourceServerMock(8999, "/testResources", "encoding-scheme-rdf.xml");
            String url = "http://localhost:8999/testResources/encoding-scheme-rdf.xml";

            HarvestSourceDTO source = new HarvestSourceDTO();
            source.setUrl(url);
            source.setIntervalMinutes(5);
            DAOFactory.get().getDao(HarvestSourceDAO.class).addSource(source);

            Harvest harvest = new PullHarvest(url);
            harvest.execute();

            assertEquals(3, harvest.getStoredTriplesCount());
        } finally {
            JettyUtil.close(server);
        }

    }

    @Test
    public void testInlineRdf() throws Exception {

        Server server = null;
        try {
            server = JettyUtil.startResourceServerMock(8999, "/testResources", "inline-rdf.xml");
            String url = "http://localhost:8999/testResources/inline-rdf.xml";

            HarvestSourceDTO source = new HarvestSourceDTO();
            source.setUrl(url);
            source.setIntervalMinutes(5);
            DAOFactory.get().getDao(HarvestSourceDAO.class).addSource(source);

            Harvest harvest = new PullHarvest(url.toString());
            harvest.execute();

            assertEquals(6, harvest.getStoredTriplesCount());
        } finally {
            JettyUtil.close(server);
        }
    }
}
