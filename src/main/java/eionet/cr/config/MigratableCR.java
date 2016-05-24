package eionet.cr.config;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * A simple POJO representing configuration of a migratable CR instance.
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class MigratableCR {

    /** */
    private String name;
    private String url;
    private String migrationPackagesDir;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }
    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }
    /**
     * @return the migrationPackagesDir
     */
    public String getMigrationPackagesDir() {
        return migrationPackagesDir;
    }
    /**
     * @param migrationPackagesDir the migrationPackagesDir to set
     */
    public void setMigrationPackagesDir(String migrationPackagesDir) {
        this.migrationPackagesDir = migrationPackagesDir;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
