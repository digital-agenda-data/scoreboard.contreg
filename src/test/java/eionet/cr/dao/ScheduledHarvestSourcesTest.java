/*
 * The contents of this file are subject to the Mozilla Public
 *
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
 * Agency. Portions created by Tieto Eesti are Copyright
 * (C) European Environment Agency. All Rights Reserved.
 *
 * Contributor(s):
 * Jaanus Heinlaid, Tieto Eesti*/
package eionet.cr.dao;

import java.util.Date;
import java.util.List;

import org.dbunit.dataset.IDataSet;
import org.junit.Test;

import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.test.helpers.CRDatabaseTestCase;

public class ScheduledHarvestSourcesTest extends CRDatabaseTestCase {

    /*
     * (non-Javadoc)
     *
     * @see org.dbunit.DatabaseTestCase#getDataSet()
     */
    @Override
    protected IDataSet getDataSet() throws Exception {
        return getXmlDataSet("emptydb.xml");
    }

    @Test
    public void testScheduledSources() throws Exception {

        HarvestSourceDAO dao = DAOFactory.get().getDao(HarvestSourceDAO.class);

        HarvestSourceDTO source = new HarvestSourceDTO();
        source.setUrl("http://rod.eionet.europa.eu/testObligations");
        source.setIntervalMinutes(5);
        source.setPrioritySource(false);
        source.setEmails("bob@europe.eu");
        dao.addSource(source);

        Thread.sleep(1100);

        // finish harvest
        source.setStatements(100);
        source.setLastHarvestFailed(false);
        source.setLastHarvest(new Date());
        source.setOwner("system");
        dao.updateSourceHarvestFinished(source);

        Thread.sleep(1100);

        source.setUrl("http://rod.eionet.europa.eu/testObligations2");
        source.setIntervalMinutes(10);
        dao.addSource(source);

        Thread.sleep(1100);

        List<HarvestSourceDTO> dtos = dao.getNextScheduledSources(10);
        assertNotNull(dtos);
        // The first harvest_source has Harvest date field filled with now() and it will be available again in 5 minutes
        // The second harvest_soruce is not yet harvested and this is returned by the method.
        assertEquals(1, dtos.size());
    }
}
