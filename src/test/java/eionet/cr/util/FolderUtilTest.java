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
package eionet.cr.util;

import junit.framework.TestCase;
import eionet.cr.config.GeneralConfig;

/**
 *
 * @author altnyris
 *
 */
public class FolderUtilTest extends TestCase {

    /**
     *
     */
    public void testIsUserHomeUri() {

        String appHomeUrl = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        assertTrue(FolderUtil.startsWithUserHome(appHomeUrl + "/home/heinlja"));
        assertTrue(FolderUtil.startsWithUserHome(appHomeUrl + "/home/heinlja/"));
        assertTrue(FolderUtil.startsWithUserHome(appHomeUrl + "/home/heinlja/folder"));
        assertFalse(FolderUtil.startsWithUserHome(appHomeUrl + "/home/"));
        assertFalse(FolderUtil.startsWithUserHome(appHomeUrl + "/home"));
        assertFalse(FolderUtil.startsWithUserHome(appHomeUrl));
        assertFalse(FolderUtil.startsWithUserHome(""));
        assertFalse(FolderUtil.startsWithUserHome(" "));
        try {
            assertFalse(FolderUtil.startsWithUserHome(null));
        } catch (NullPointerException e) {
            fail("Wasn't expecting this exception: " + e.toString());
        }
    }

    /**
     *
     */
    public void testExtarctUserName() {

        try {
            assertEquals(null, FolderUtil.extractUserName(null));
        } catch (NullPointerException e) {
            fail("Wasn't expecting this exception: " + e.toString());
        }

        assertEquals(null, FolderUtil.extractUserName(""));
        assertEquals(null, FolderUtil.extractUserName(" "));

        String appHomeUrl = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        assertEquals(null, FolderUtil.extractUserName(appHomeUrl));
        assertEquals(null, FolderUtil.extractUserName(appHomeUrl + "/home"));
        assertEquals(null, FolderUtil.extractUserName(appHomeUrl + "/home/"));
        assertEquals(null, FolderUtil.extractUserName(appHomeUrl + "/heinlja"));
        assertEquals(null, FolderUtil.extractUserName(appHomeUrl + "/heinlja/"));
        assertEquals("heinlja", FolderUtil.extractUserName(appHomeUrl + "/home/heinlja"));
        assertEquals("heinlja", FolderUtil.extractUserName(appHomeUrl + "/home/heinlja/"));
        assertEquals("heinlja", FolderUtil.extractUserName(appHomeUrl + "/home/heinlja/aaa"));
    }

    /**
     *
     */
    public void testIsUserReservedUri() {

        try {
            assertFalse(FolderUtil.isUserReservedUri(null));
            assertFalse(FolderUtil.isUserReservedUri(""));
            assertFalse(FolderUtil.isUserReservedUri(" "));

            String appHomeUrl = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
            assertTrue(FolderUtil.isUserReservedUri(appHomeUrl + "/home/heinlja/reviews"));
            assertTrue(FolderUtil.isUserReservedUri(appHomeUrl + "/home/heinlja/bookmarks"));
            assertTrue(FolderUtil.isUserReservedUri(appHomeUrl + "/home/heinlja/history"));
            assertTrue(FolderUtil.isUserReservedUri(appHomeUrl + "/home/heinlja/registrations"));
            assertFalse(FolderUtil.isUserReservedUri(appHomeUrl + "/home/heinlja/some"));
            assertFalse(FolderUtil.isUserReservedUri(appHomeUrl + "/home/heinlja/"));
            assertFalse(FolderUtil.isUserReservedUri(appHomeUrl + "/home/heinlja"));
            assertFalse(FolderUtil.isUserReservedUri(appHomeUrl + "/home/"));
        } catch (NullPointerException e) {
            fail("Wasn't expecting this exception: " + e.toString());
        }
    }

    public void testExtractAcl() {

        String appHomeUrl = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        String aclPath = FolderUtil.extractAclPath(appHomeUrl + "/abc/cba");

        assertEquals("/abc/cba", aclPath);
    }

    public void testExtractProjectAcl() {

        String appHomeUrl = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        String aclPath = FolderUtil.extractAclPath(appHomeUrl + "/project/aux/poux");

        assertEquals("/project/aux/poux", aclPath);
    }

    public void testEmtpyProcectAcl() {
        String appHomeUrl = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        String aclPath = FolderUtil.extractAclPath(appHomeUrl);

        assertEquals("", aclPath);
    }

    public void testExtractParentFolderUri() {
        assertEquals("http://aa.bb.com/level1/level2/level3", FolderUtil.extractParentFolderUri("http://aa.bb.com/level1/level2/level3/somethingattheen"));
    }
}
