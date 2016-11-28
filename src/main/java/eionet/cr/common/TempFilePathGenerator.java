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

package eionet.cr.common;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import eionet.cr.config.GeneralConfig;

/**
 * @author Jaanus Heinlaid
 */
public final class TempFilePathGenerator {

    /**
     * Hide utility class constructor.
     */
    private TempFilePathGenerator() {
        // Hide utility class constructor.
    }

    /** */
    public static final String PREFIX = "eionet.cr.tempfile-";

    /** */
    private static final String DASH = "-";

    /** */
    private static final String PERIOD = ".";

    /** */
    public static final Collection<File> TEMP_FILE_DIRECTORIES;

    /**
     *
     */
    static {
        TEMP_FILE_DIRECTORIES =
                Collections.singleton(new File(GeneralConfig.getRequiredProperty(GeneralConfig.HARVESTER_FILES_LOCATION)));
    }

    /** */
    public static File generate() {

        String fileName = PREFIX + System.currentTimeMillis() + DASH + UUID.randomUUID();
        return new File(GeneralConfig.getRequiredProperty(GeneralConfig.HARVESTER_FILES_LOCATION), fileName);
    }

    /**
     *
     * @param extension
     * @return
     */
    public static File generateWithExtension(String extension) {

        String fileName = PREFIX + System.currentTimeMillis() + DASH + UUID.randomUUID() + PERIOD + extension;
        return new File(GeneralConfig.getRequiredProperty(GeneralConfig.HARVESTER_FILES_LOCATION), fileName);
    }
}
