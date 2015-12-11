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

package eionet.cr.dao.virtuoso;

import java.util.ArrayList;
import java.util.List;

import org.dbunit.dataset.IDataSet;
import org.junit.Test;

import com.ibm.icu.util.Calendar;

import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.PostHarvestScriptDAO;
import eionet.cr.dto.PostHarvestScriptDTO;
import eionet.cr.dto.PostHarvestScriptDTO.TargetType;
import eionet.cr.test.helpers.CRDatabaseTestCase;

/**
 *
 *
 * @author Enriko Käsper
 */
public class PostHarvestScriptDAOTest extends CRDatabaseTestCase {

    /*
     * (non-Javadoc)
     *
     * @see org.dbunit.DatabaseTestCase#getDataSet()
     */
    @Override
    protected IDataSet getDataSet() throws Exception {
        return getXmlDataSet("emptydb.xml");
    }

    String script =
            "PREFIX cr: <http://cr.eionet.europa.eu/ontologies/contreg.rdf#> INSERT INTO ?harvestedSource  { ?subject a cr:File } FROM ?harvestedSource";
    String script2 =
            "PREFIX cr: <http://cr.eionet.europa.eu/ontologies/contreg.rdf#> INSERT INTO ?harvestedSource  { ?subject <http://www.w3.org/2000/01/rdf-schema#label> \"new soruce\" } FROM ?harvestedSource";

    @Test
    public void testInsert() throws DAOException {
        String targetUrl = "http://rod.eionet.europa.eu/schema.rdf#Delivery";
        String title = "Post harvest script";
        PostHarvestScriptDAO postHarvestDao = DAOFactory.get().getDao(PostHarvestScriptDAO.class);
        postHarvestDao.insert(TargetType.SOURCE, targetUrl, title, script, true, false);

        assertTrue(postHarvestDao.exists(TargetType.SOURCE, targetUrl, title));
        assertEquals(1, postHarvestDao.listActive(TargetType.SOURCE, targetUrl).size());
    }

    @Test
    public void testActivate() throws DAOException {
        String targetUrl = "http://rod.eionet.europa.eu/schema.rdf#Delivery";
        String title = "Post harvest script";
        PostHarvestScriptDAO postHarvestDao = DAOFactory.get().getDao(PostHarvestScriptDAO.class);
        postHarvestDao.insert(TargetType.SOURCE, targetUrl, title, script, true, false);

        List<PostHarvestScriptDTO> scriptsList = postHarvestDao.listActive(TargetType.SOURCE, targetUrl);

        assertEquals(1, postHarvestDao.listActive(TargetType.SOURCE, targetUrl).size());

        PostHarvestScriptDTO script = scriptsList.get(0);
        List<Integer> ids = new ArrayList<Integer>();
        ids.add(script.getId());
        postHarvestDao.activateDeactivate(ids);

        assertEquals(0, postHarvestDao.listActive(TargetType.SOURCE, targetUrl).size());
    }

    @Test
    public void testIsScriptsModified() throws DAOException, InterruptedException {
        String targetUrl = "http://rod.eionet.europa.eu/schema.rdf";
        String title1 = "Post harvest script 1";
        String title2 = "Post harvest script 2";
        PostHarvestScriptDAO postHarvestDao = DAOFactory.get().getDao(PostHarvestScriptDAO.class);
        postHarvestDao.insert(TargetType.SOURCE, targetUrl, title1, script, true, false);

        // There must definitely be no script newer than two days in the future.
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 2);
        assertFalse(postHarvestDao.isScriptsModified(cal.getTime(), targetUrl));

        // The above-inserted script is definitely modified after two days in the past.
        cal.add(Calendar.DATE, -1);
        cal.add(Calendar.DATE, -2);
        assertTrue(postHarvestDao.isScriptsModified(cal.getTime(), targetUrl));

        Calendar calNow = Calendar.getInstance();
        Thread.sleep(1000);
        // add second script with newer date_modified
        postHarvestDao.insert(TargetType.SOURCE, targetUrl, title2, script2, true, false);
        // one script is newer than harvest data
        assertTrue(postHarvestDao.isScriptsModified(calNow.getTime(), targetUrl));
    }
}
