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
 *        jaanus
 */

package eionet.cr.staging.imp;

/**
 * Enumeration for the import statuses of staging databases.
 * 
 * @author jaanus
 */
public enum ImportStatus {

    /** */
    NOT_STARTED("Not started"), STARTED("Started"), ERROR("Error"), COMPLETED("Completed"), COMPLETED_WARNINGS(
            "Completed with warnings");

    /** */
    private String friendlyName;

    /**
     * The constructor that takes friendly name as the input.
     * 
     * @param friendlyName the friendly name
     */
    private ImportStatus(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return friendlyName;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return name();
    }
}
