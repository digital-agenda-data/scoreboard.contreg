package eionet.cr.config;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration2.ConfigurationLookup;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.interpol.ConfigurationInterpolator;
import org.apache.commons.configuration2.interpol.InterpolatorSpecification;
import org.apache.commons.configuration2.interpol.Lookup;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class ConfigUtils {

    /** */
    private static final Logger LOGGER = Logger.getLogger(ConfigUtils.class);

    /** */
    private static final String ENV_PROPS_PATH = "cr.env.properties";

    /**
     *
     * @param propFileName
     * @return
     * @throws ConfigurationException
     */
    public static Properties getInterpolatedProperties(String propFileName) throws ConfigurationException {
        return getInterpolatedProperties(propFileName, getInterpolator());
    }

    /**
     *
     * @param propFileName
     * @param interpolator
     * @return
     * @throws ConfigurationException
     */
    public static Properties getInterpolatedProperties(String propFileName, ConfigurationInterpolator interpolator) throws ConfigurationException {

        URL propsResource = ConfigUtils.class.getClassLoader().getResource(propFileName);
        if (propsResource == null) {
            return null;
        }

        PropertiesConfiguration propConfig = new Configurations().properties(propsResource);
        propConfig.setInterpolator(interpolator);

        Properties interpolatedProperties = new Properties();
        Iterator<String> keys = propConfig.getKeys();
        while (keys != null && keys.hasNext()) {

            String key = keys.next();
            String value = propConfig.getString(key);
            interpolatedProperties.setProperty(key, value);
        }

        return  interpolatedProperties;
    }

    /**
     *
     * @return
     */
    public static ConfigurationInterpolator getInterpolator() throws ConfigurationException {

        ArrayList<Lookup> defaultLookups = new ArrayList();

        String envPropsFilePath = System.getProperty(ENV_PROPS_PATH);
        if (!StringUtils.isBlank(envPropsFilePath)) {

            File envPropsFile = new File(envPropsFilePath);
            if (envPropsFile.exists() && envPropsFile.isFile()) {
                PropertiesConfiguration envConfig = new Configurations().properties(envPropsFile);
                defaultLookups.add(new ConfigurationLookup(envConfig));
            }
        }

        Map<String, Lookup> prefixLookups = ConfigurationInterpolator.getDefaultPrefixLookups();
        defaultLookups.add(prefixLookups.get("sys"));
        defaultLookups.add(prefixLookups.get("env"));

        InterpolatorSpecification spec = (new InterpolatorSpecification.Builder()).withPrefixLookups(prefixLookups)
                .withDefaultLookups(defaultLookups).create();
        return ConfigurationInterpolator.fromSpecification(spec);
    }

    /**
     *
     * @param propFileNames
     */
    public static void interpolatePropertyFiles(Collection<String> propFileNames) throws ConfigurationException {

        if (CollectionUtils.isEmpty(propFileNames)) {
            return;
        }

        ConfigurationInterpolator interpolator = ConfigUtils.getInterpolator();
        for (String propFileName : propFileNames) {
            try {
                LOGGER.debug("Interpolating properties in " + propFileName);
                interpolatePropertyFile(propFileName, interpolator);
            } catch (Exception e) {
                LOGGER.warn(String.format("Failed to interpolate property file %s: %s", propFileName, e.toString()));
            }
        }
    }

    /**
     *
     * @param propFileName
     * @param interpolator
     * @throws ConfigurationException
     * @throws URISyntaxException
     * @throws IOException
     */
    public static void interpolatePropertyFile(String propFileName, ConfigurationInterpolator interpolator) throws ConfigurationException, URISyntaxException, IOException {

        Properties interpolatedProperties = ConfigUtils.getInterpolatedProperties(propFileName, interpolator);

        URL propsResource = ConfigUtils.class.getClassLoader().getResource(propFileName);
        if (propsResource == null) {
            return;
        }

        File propsFile = new File(propsResource.toURI());
        if (!propsFile.exists() || !propsFile.isFile()) {
            return;
        }

        OutputStream out = null;
        try {
            out = new FileOutputStream(propsFile);
            interpolatedProperties.store(out, "Interpolated");
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
}
