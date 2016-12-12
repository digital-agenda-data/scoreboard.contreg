package eionet.cr.freemarker;

import eionet.cr.util.odp.ODPDatasetsPacker;
import freemarker.template.Configuration;
import freemarker.template.Version;

/**
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class TemplatesConfiguration {

    /** */
    private static final Configuration TEMPLATES_CONFIGURATION = createTemplatesConfiguration();

    /**
     *
     * @return
     */
    public static Configuration getInstance() {
        return TEMPLATES_CONFIGURATION;
    }

    /**
     *
     * @return
     */
    private static Configuration createTemplatesConfiguration() {
        Configuration cfg = new Configuration(new Version(2, 3, 25));
        cfg.setClassForTemplateLoading(ODPDatasetsPacker.class, "/");
        return cfg;
    }
}
