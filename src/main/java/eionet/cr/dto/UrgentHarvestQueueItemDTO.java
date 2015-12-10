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
package eionet.cr.dto;

import java.io.Serializable;

// TODO: Auto-generated Javadoc
/**
 * A DTO representing urgent harvest queue items.
 *
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 */
public class UrgentHarvestQueueItemDTO implements Serializable {

    /** The url. */
    private String url;

    /** The time added. */
    private java.util.Date timeAdded;

    /** The pushed content. */
    private String pushedContent;

    /**
     * Instantiates a new urgent harvest queue item dto.
     */
    public UrgentHarvestQueueItemDTO() {
    }

    /**
     * Gets the url.
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url.
     *
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the time added.
     *
     * @return the timeAdded
     */
    public java.util.Date getTimeAdded() {
        return timeAdded;
    }

    /**
     * Sets the time added.
     *
     * @param timeAdded the timeAdded to set
     */
    public void setTimeAdded(java.util.Date timeAdded) {
        this.timeAdded = timeAdded;
    }

    /**
     * Gets the pushed content.
     *
     * @return the pushedContent
     */
    public String getPushedContent() {
        return pushedContent;
    }

    /**
     * Sets the pushed content.
     *
     * @param pushedContent the pushedContent to set
     */
    public void setPushedContent(String pushedContent) {
        this.pushedContent = pushedContent;
    }

    /**
     * Checks if is push harvest.
     *
     * @return true, if is push harvest
     */
    public boolean isPushHarvest() {
        return pushedContent != null;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("url: [%s], time: [%s]", url, timeAdded.toString());
    }
}
