package eionet.cr.dto;

/**
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DcatCatalogTemplateDTO {

    /** */
    private String uri;
    private String identifier;
    private String title;
    private String description;
    private String issuedDateTimeStr;
    private String modifiedDateTimeStr;

    private String licenseUri;
    private String homepageUri;

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

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
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the modifiedDateTimeStr
     */
    public String getModifiedDateTimeStr() {
        return modifiedDateTimeStr;
    }

    /**
     * @param modifiedDateTimeStr the modifiedDateTimeStr to set
     */
    public void setModifiedDateTimeStr(String modifiedDateTimeStr) {
        this.modifiedDateTimeStr = modifiedDateTimeStr;
    }

    /**
     * @return the licenseUri
     */
    public String getLicenseUri() {
        return licenseUri;
    }

    /**
     * @param licenseUri the licenseUri to set
     */
    public void setLicenseUri(String licenseUri) {
        this.licenseUri = licenseUri;
    }

    /**
     * @return the homepageUri
     */
    public String getHomepageUri() {
        return homepageUri;
    }

    /**
     * @param homepageUri the homepageUri to set
     */
    public void setHomepageUri(String homepageUri) {
        this.homepageUri = homepageUri;
    }

    /**
     * @return the issuedDateTimeStr
     */
    public String getIssuedDateTimeStr() {
        return issuedDateTimeStr;
    }

    /**
     * @param issuedDateTimeStr the issuedDateTimeStr to set
     */
    public void setIssuedDateTimeStr(String issuedDateTimeStr) {
        this.issuedDateTimeStr = issuedDateTimeStr;
    }
}
