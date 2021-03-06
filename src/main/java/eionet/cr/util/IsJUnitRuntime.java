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
 * Jaanus Heinlaid
 */

package eionet.cr.util;

import org.apache.log4j.Logger;

/**
 * Singleton class indicating if you are currently in JUnit runtime.
 *
 * @author Jaanus Heinlaid
 */
public final class IsJUnitRuntime {

    /** */
    private static final Logger LOGGER = Logger.getLogger(IsJUnitRuntime.class);

    /** */
    public static final boolean VALUE = isJUnitRuntime();

    /**
     * Hide utility class constructor.
     */
    private IsJUnitRuntime() {
        // Just an empty private constructor to avoid instantiating this utility class.
    }

    /**
     *
     * @return
     */
    private static boolean isJUnitRuntime() {

        String stackTrace = Util.getStackTrace(new Throwable());
        boolean result = Boolean.valueOf(stackTrace.indexOf("at junit.framework.TestCase.run") > 0);
        if (result) {
            LOGGER.info("Detected that the code is running in JUnit runtime");
        }
        return result;
    }
}
