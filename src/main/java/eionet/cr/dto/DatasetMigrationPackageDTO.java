package eionet.cr.dto;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.time.DateFormatUtils;

import eionet.cr.util.URIUtil;

/**
 * Simple DTO for dataset migration packages.
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMigrationPackageDTO {

    /** */
    private String identifier;

    /** */
    private String datasetUri;

    /** */
    private Date started;

    /** */
    private Date finished;

    /** */
    private String finishedErrorMessage;

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the datasetUri
     */
    public String getDatasetUri() {
        return datasetUri;
    }

    /**
     * @param datasetUri the datasetUri to set
     */
    public void setDatasetUri(String datasetUri) {
        this.datasetUri = datasetUri;
    }

    /**
     * @return the started
     */
    public Date getStarted() {
        return started;
    }

    /**
     * @param started the started to set
     */
    public void setStarted(Date started) {
        this.started = started;
    }

    /**
     * @return the finished
     */
    public Date getFinished() {
        return finished;
    }

    /**
     * @param finished the finished to set
     */
    public void setFinished(Date finished) {
        this.finished = finished;
    }

    /**
     * @return the finishedErrorMessage
     */
    public String getFinishedErrorMessage() {
        return finishedErrorMessage;
    }

    /**
     * @param finishedErrorMessage the finishedErrorMessage to set
     */
    public void setFinishedErrorMessage(String finishedErrorMessage) {
        this.finishedErrorMessage = finishedErrorMessage;
    }

    /**
     *
     * @param datasetUri
     * @param username
     * @param date
     * @return
     */
    public static String buildPackageIdentifier(String datasetUri, String username, Date date) {

        if (StringUtils.isBlank(datasetUri) || StringUtils.isBlank(username) || date == null) {
            throw new IllegalArgumentException("Dataset URI, username and date must not be blank!");
        }

        String datasetIdentifier = URIUtil.extractURILabel(datasetUri);
        StringBuilder sb = new StringBuilder();
        sb = sb.append(datasetIdentifier.replaceAll("[^a-zA-Z0-9-._]+", " ").trim().replaceAll("\\s+", " ").toLowerCase());
        sb = sb.append("_").append(username).append("_").append(DateFormatUtils.format(date, "MMdd'_'HHmmss"));
        return sb.toString();
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
     *
     */
    public void validateForNew() {

        if (StringUtils.isBlank(datasetUri)) {
            throw new IllegalArgumentException("Dataset URI must not be blank!");
        }
    }
}
