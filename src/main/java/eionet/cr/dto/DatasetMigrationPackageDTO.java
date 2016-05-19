package eionet.cr.dto;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Simple DTO for dataset migration packages.
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMigrationPackageDTO {

    /** The package's name helping to distinguish it from others. */
    private String name;

    /**  */
    private String datasetUri;

    /**  */
    private String datasetTitle;

    /**  */
    private Date started;

    /**  */
    private Date finished;

    // /**
    // * @param name
    // * @param datasetUri
    // * @param datasetTitle
    // */
    // public DatasetMigrationPackageDTO(String name, String datasetUri, String datasetTitle) {
    // this(name, datasetUri, datasetTitle, null);
    // }
    //
    // /**
    // * @param name
    // * @param datasetUri
    // * @param datasetTitle
    // * @param started
    // */
    // public DatasetMigrationPackageDTO(String name, String datasetUri, String datasetTitle, Date started) {
    //
    // if (StringUtils.isBlank(name) || StringUtils.isBlank(datasetUri) || StringUtils.isBlank(datasetTitle)) {
    // throw new IllegalArgumentException("Package name, dataset URI and dataset title must not be blank!");
    // }
    //
    // this.name = name;
    // this.datasetUri = datasetUri;
    // this.datasetTitle = datasetTitle;
    // this.started = started;
    //
    // // Identifier is dataset title with only [a-zA-Z0-9_] characters.
    // //this.identifier = datasetTitle.replaceAll("\\W+", " ").trim().replaceAll("\\W+", "_").replaceAll("[_]+",
    // "_").toLowerCase();
    // }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the datasetUri
     */
    public String getDatasetUri() {
        return datasetUri;
    }

    /**
     * @return the datasetTitle
     */
    public String getDatasetTitle() {
        return datasetTitle;
    }

    /**
     * @return the started
     */
    public Date getStarted() {
        return started;
    }

    /**
     * @return the finished
     */
    public Date getFinished() {
        return finished;
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

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param datasetUri the datasetUri to set
     */
    public void setDatasetUri(String datasetUri) {
        this.datasetUri = datasetUri;
    }

    /**
     * @param datasetTitle the datasetTitle to set
     */
    public void setDatasetTitle(String datasetTitle) {
        this.datasetTitle = datasetTitle;
    }

    /**
     * @param started the started to set
     */
    public void setStarted(Date started) {
        this.started = started;
    }

    /**
     * @param finished the finished to set
     */
    public void setFinished(Date finished) {
        this.finished = finished;
    }

    /**
     *
     */
    public void validateForNew() {

        if (StringUtils.isBlank(name) || StringUtils.isBlank(datasetUri)) {
            throw new IllegalArgumentException("Migration package name and dataset URI must not be blank!");
        }

        if (!name.replaceAll("[^a-zA-Z0-9-._]+", "").equals(name)) {
            throw new IllegalArgumentException("Only latin characters, underscore, dash and period allowed in package name!");
        }
    }
}
