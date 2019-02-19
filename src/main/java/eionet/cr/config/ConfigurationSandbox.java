package eionet.cr.config;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ConfigurationLookup;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.interpol.ConfigurationInterpolator;
import org.apache.commons.configuration2.interpol.InterpolatorSpecification;
import org.apache.commons.configuration2.interpol.Lookup;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class ConfigurationSandbox {

    /**
     *
     * @param args
     * @throws ConfigurationException
     */
    public static void main(String[] args) throws ConfigurationException {

        System.setProperty("application.homeDir", "C:/jaanus");

        Configurations configs = new Configurations();
        Configuration config = configs.properties(new File("C:\\dev\\projects\\scoreboard\\git\\scoreboard.contreg\\src\\main\\resources\\cr.properties"));

        PropertiesConfiguration localConfig = configs.properties(new File("C:\\dev\\projects\\scoreboard\\git\\scoreboard.contreg\\local.properties"));

        Map<String, Lookup> prefixLookups = ConfigurationInterpolator.getDefaultPrefixLookups();
        ArrayList<Lookup> defaultLookups = new ArrayList();

        defaultLookups.add(new ConfigurationLookup(localConfig));
        defaultLookups.add(prefixLookups.get("sys"));
        defaultLookups.add(prefixLookups.get("env"));


//        config.installInterpolator(prefixLookups, defaultLookups);
//        config.getInterpolator().addDefaultLookup(envLookup);

        InterpolatorSpecification spec = (new InterpolatorSpecification.Builder()).withPrefixLookups(prefixLookups)
                .withDefaultLookups(defaultLookups).create();
        config.setInterpolator(ConfigurationInterpolator.fromSpecification(spec));

        String uploadsDir = config.getString("uploads.dir");
        System.out.println(uploadsDir);

//        List<Lookup> defaultLookups = config.getInterpolator().getDefaultLookups();
//        if (defaultLookups == null) {
//            System.out.println("defaultLookups = null");
//        } else {
//            for (Lookup defaultLookup : defaultLookups) {
//                System.out.println(defaultLookup.getClass().getName());
//            }
//
//        }
    }
}
