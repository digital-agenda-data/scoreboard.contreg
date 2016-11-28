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
 * Agency. Portions created by TripleDev or Zero Technologies are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *        Juhan Voolaid
 */

package eionet.cr.dto;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

/**
 * Dataset DTO object.
 *
 * @author Juhan Voolaid
 */
public class DatasetDTO {

    /** URI. */
    private String uri;

    /** Label. */
    private String label;

    /** Last modified date. */
    private Date modified;

    /** */
    private String identifier;
    private String title;
    private String description;
    private String dsdUri;

    /**
     *
     * @param identifier
     * @param title
     * @param description
     * @param dsdUri
     * @return
     */
    public static DatasetDTO createNew(String identifier, String title, String description, String dsdUri) {

        if (StringUtils.isBlank(identifier)) {
            throw new IllegalArgumentException("Dataset identifier must not be blank!");
        }

        if (StringUtils.isBlank(title)) {
            title = identifier;
        }

        DatasetDTO dto = new DatasetDTO();
        dto.setIdentifier(identifier);
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setDsdUri(dsdUri);
        return dto;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof DatasetDTO)) {
            return false;
        } else if (obj == this) {
            return true;
        }

        DatasetDTO other = (DatasetDTO) obj;
        if (uri != null) {
            return StringUtils.equals(uri, other.getUri());
        } else {
            return StringUtils.equals(identifier, other.getIdentifier());
        }
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

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
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the modified
     */
    public Date getModified() {
        return modified;
    }

    /**
     * @param modified the modified to set
     */
    public void setModified(Date modified) {
        this.modified = modified;
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
     * @return the dsdUri
     */
    public String getDsdUri() {
        return dsdUri;
    }

    /**
     * @param dsdUri the dsdUri to set
     */
    public void setDsdUri(String dsdUri) {
        this.dsdUri = dsdUri;
    }
}
