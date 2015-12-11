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
 * Jaanus Heinlaid, Tieto Eesti
 */
package eionet.cr.config;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author heinljab
 *
 */
public final class GeneralConfig {

    /** */
    public static final String BUNDLE_NAME = "cr";
    public static final String PROPERTIES_FILE_NAME = "cr.properties";

    /** */
    public static final String HARVESTER_FILES_LOCATION = "harvester.tempFileDir";
    public static final String HARVESTER_BATCH_HARVESTING_HOURS = "harvester.batchHarvestingHours";
    public static final String HARVESTER_JOB_INTERVAL_SECONDS = "harvester.batchHarvestingIntervalSeconds";
    public static final String HARVESTER_REFERRALS_INTERVAL = "harvester.referrals.intervalMinutes";
    public static final String HARVESTER_SOURCES_UPPER_LIMIT = "harvester.batchHarvestingUpperLimit";
    public static final String HARVESTER_MAX_CONTENT_LENGTH = "harvester.maximumContentLength";
    public static final String HARVESTER_HTTP_TIMEOUT = "harvester.httpConnection.timeout";

    /** */
    public static final String XMLCONV_LIST_CONVERSIONS_URL = "xmlconv.listConversions.url";
    public static final String XMLCONV_CONVERT_URL = "xmlconv.convert.url";
    public static final String XMLCONV_CONVERT_PUSH_URL = "xmlconv.convertPush.url";
    public static final String XMLCONV_XSL_URL = "xmlconv.xsl.url";

    /** */
    public static final String MAIL_SYSADMINS = "mail.sysAdmins";

    /** */
    public static final String APPLICATION_VERSION = "application.version";
    public static final String APPLICATION_USERAGENT = "application.userAgent";
    public static final String APPLICATION_HOME_URL = "application.homeURL";

    /** */
    public static final String SUBJECT_SELECT_MODE = "subjectSelectMode";

    /** Cache-related constants */
    public static final String DELIVERY_SEARCH_PICKLIST_CACHE_UPDATE_INTERVAL = "deliverySearchPicklistCacheUpdateInterval";
    public static final String RECENT_DISCOVERED_FILES_CACHE_UPDATE_INTERVAL = "recentDiscoveredFilesCacheUpdateInterval";
    public static final String TYPE_CACHE_UPDATE_INTERVAL = "typeCacheUpdateInterval";
    public static final String TAG_CLOUD_CACHE_UPDATE_INTERVAL = "tagCloudCacheUpdateInterval";
    public static final String TYPE_CACHE_UPDATER_CRON_JOB = "typeCacheTablesUpdateCronJob";

    /*
     * TagCloud sizes.
     */
    public static final String TAGCLOUD_FRONTPAGE_SIZE = "tagcloud.frontpage.size";
    public static final String TAGCOLUD_TAGSEARCH_SIZE = "tagcloud.tagsearch.size";

    /** */
    public static final String VIRTUOSO_DB_DRV = "virtuoso.db.drv";
    public static final String VIRTUOSO_DB_URL = "virtuoso.db.url";
    public static final String VIRTUOSO_DB_USR = "virtuoso.db.usr";
    public static final String VIRTUOSO_DB_PWD = "virtuoso.db.pwd";

    /** */
    public static final String VIRTUOSO_DB_ROUSR = "virtuoso.db.rousr";
    public static final String VIRTUOSO_DB_ROPWD = "virtuoso.db.ropwd";

    /**
     * General ruleSet name for inferencing. Schema sources are added into that ruleset.
     * */
    public static final String VIRTUOSO_CR_RULESET_NAME = "virtuoso.cr.ruleset.name";

    /** */
    public static final String VIRTUOSO_REAL_TIME_FT_INDEXING = "virtuoso.unittest.realTimeFullTextIndexing";

    /** */
    public static final String FILESTORE_PATH = "filestore.path";

    /** */
    public static final String FILE_DELETION_JOB_INTERVAL = "tempFileDeletionJob.interval";

    /**
     * Property name for property indicating how many rows SPARQL endpoint returns in HTML.
     */
    public static final String SPARQLENDPOINT_MAX_ROWS_COUNT = "sparql.max.rows";

    /** */
    public static final String APPLICATION_DISPLAY_NAME = "application.displayName";

    /** */
    public static final String USE_CENTRAL_AUTHENTICATION_SERVICE = "useCentralAuthenticationService";

    /** */
    public static final String ENABLE_EEA_FUNCTIONALITY = "enableEEAFunctionality";

    /** */
    public static final String PING_WHITELIST = "pingWhitelist";

    /** */
    public static final String STAGING_FILES_DIR = "stagingFilesDir";

    /** */
    public static final int SEVERITY_INFO = 1;
    public static final int SEVERITY_CAUTION = 2;
    public static final int SEVERITY_WARNING = 3;

    /** */
    public static final String HARVESTER_URI = getRequiredProperty(APPLICATION_HOME_URL) + "/harvester";

    /** */
    private static Log logger = LogFactory.getLog(GeneralConfig.class);

    /** */
    private static Properties properties = null;

    /**
     * Hide utility class constructor.
     */
    private GeneralConfig() {
        // Hide utility class constructor.
    }

    /** */
    private static void init() {
        properties = new Properties();
        try {
            properties.load(GeneralConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME));

            // trim all the values (i.e. we don't allow preceding or trailing
            // white space in property values)
            for (Entry<Object, Object> entry : properties.entrySet()) {
                entry.setValue(entry.getValue().toString().trim());
            }

        } catch (IOException e) {
            logger.fatal("Failed to load properties from " + PROPERTIES_FILE_NAME, e);
        }
    }

    /**
     *
     * @param name
     * @return
     */
    public static synchronized String getProperty(String key) {

        if (properties == null) {
            init();
        }

        return properties.getProperty(key);
    }

    /**
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static synchronized String getProperty(String key, String defaultValue) {

        if (properties == null) {
            init();
        }

        return properties.getProperty(key, defaultValue);
    }

    /**
     * Returns integer property.
     *
     * @param key property key in the properties file
     * @param defaultValue default value that is returned if not specified or in incorrect format
     * @return property value or default if not specified correctly
     */
    public static synchronized int getIntProperty(final String key, final int defaultValue) {

        if (properties == null) {
            init();
        }
        String propValue = properties.getProperty(key);
        int value = defaultValue;
        if (propValue != null) {
            try {
                value = Integer.valueOf(propValue);
            } catch (Exception e) {
                // Ignore exceptions resulting from string-to-integer conversion here.
            }
        }

        return value;
    }

    /**
     *
     * @param key
     * @return
     * @throws CRConfigException
     */
    public static synchronized String getRequiredProperty(String key) {

        String value = getProperty(key);
        if (value == null || value.trim().length() == 0) {
            throw new CRConfigException("Missing required property: " + key);
        } else {
            return value;
        }
    }

    /**
     *
     * @return
     */
    public static synchronized Properties getProperties() {

        if (properties == null) {
            init();
        }

        return properties;
    }

    /**
     *
     * @return
     */
    public static synchronized boolean useVirtuoso() {

        String virtuosoDbUrl = getProperty(VIRTUOSO_DB_URL);
        return virtuosoDbUrl != null && virtuosoDbUrl.trim().length() > 0;
    }

    /**
     *
     * @return
     */
    public static synchronized boolean isUseCentralAuthenticationService() {

        String useCentralAuthenticationService = getProperty(USE_CENTRAL_AUTHENTICATION_SERVICE);
        return StringUtils.isBlank(useCentralAuthenticationService) || !useCentralAuthenticationService.equals("false");
    }

    /**
     * If ruleset name property is available in cr.properties, then use inferencing in queries.
     *
     * @return
     */
    public static synchronized boolean isUseInferencing() {

        String crRulesetName = getProperty(VIRTUOSO_CR_RULESET_NAME);
        return !StringUtils.isBlank(crRulesetName);
    }
}
