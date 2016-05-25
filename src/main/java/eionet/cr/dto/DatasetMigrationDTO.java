package eionet.cr.dto;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Simple DTO for dataset migrations.
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMigrationDTO {

    /** */
    private int id;
    private String sourceCrUrl;
    private String sourcePackageIdentifier;
    private String targetDatasetUri;
    private boolean prePurge;
    private String userName;
    private Date startedTime;
    private Date finishedTime;
    private boolean failed;
    private String messages;

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the sourceCrUrl
     */
    public String getSourceCrUrl() {
        return sourceCrUrl;
    }

    /**
     * @param sourceCrUrl the sourceCrUrl to set
     */
    public void setSourceCrUrl(String sourceCrUrl) {
        this.sourceCrUrl = sourceCrUrl;
    }

    /**
     * @return the sourcePackageIdentifier
     */
    public String getSourcePackageIdentifier() {
        return sourcePackageIdentifier;
    }

    /**
     * @param sourcePackageIdentifier the sourcePackageIdentifier to set
     */
    public void setSourcePackageIdentifier(String sourcePackageIdentifier) {
        this.sourcePackageIdentifier = sourcePackageIdentifier;
    }

    /**
     * @return the targetDatasetUri
     */
    public String getTargetDatasetUri() {
        return targetDatasetUri;
    }

    /**
     * @param targetDatasetUri the targetDatasetUri to set
     */
    public void setTargetDatasetUri(String targetDatasetUri) {
        this.targetDatasetUri = targetDatasetUri;
    }

    /**
     * @return the prePurge
     */
    public boolean isPrePurge() {
        return prePurge;
    }

    /**
     * @param prePurge the prePurge to set
     */
    public void setPrePurge(boolean prePurge) {
        this.prePurge = prePurge;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the startedTime
     */
    public Date getStartedTime() {
        return startedTime;
    }

    /**
     * @param startedTime the startedTime to set
     */
    public void setStartedTime(Date startedTime) {
        this.startedTime = startedTime;
    }

    /**
     * @return the finishedTime
     */
    public Date getFinishedTime() {
        return finishedTime;
    }

    /**
     * @param finishedTime the finishedTime to set
     */
    public void setFinishedTime(Date finishedTime) {
        this.finishedTime = finishedTime;
    }

    /**
     * @return the failed
     */
    public boolean isFailed() {
        return failed;
    }

    /**
     * @param failed the failed to set
     */
    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    /**
     * @return the messages
     */
    public String getMessages() {
        return messages;
    }

    /**
     * @param messages the messages to set
     */
    public void setMessages(String messages) {
        this.messages = messages;
    }

    /**
     *
     */
    public void validateForStart() {

        if (StringUtils.isBlank(sourceCrUrl) || StringUtils.isBlank(sourcePackageIdentifier) || StringUtils.isBlank(targetDatasetUri) || StringUtils.isBlank(userName)) {
            throw new IllegalArgumentException("Source CR, package name, target dataset and username must all be specified!");
        }
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
