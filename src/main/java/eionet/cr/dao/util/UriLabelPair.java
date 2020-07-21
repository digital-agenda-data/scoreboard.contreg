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
package eionet.cr.dao.util;

import eionet.cr.util.URIUtil;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
public class UriLabelPair implements Comparable {

    /** */
    private String uri;
    private String label;

    /**
     *
     * @param uri
     * @param label
     * @return
     */
    public static UriLabelPair create(String uri, String label) {

        UriLabelPair pair = new UriLabelPair();
        pair.setUri(uri);
        pair.setLabel(label);
        return pair;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return StringUtils.isBlank(label) ? URIUtil.extractURILabel(uri, uri) : label;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {

        // the following assignment can be done even if o == null and it will
        // throw ClassCastException if (o instanceof UriLabelPair) == false
        UriLabelPair otherOption = (UriLabelPair) o;

        if (otherOption == null) {
            return 1;
        } else if (label == null) {
            return otherOption.getLabel() == null ? 0 : -1;
        } else {
            return label.compareToIgnoreCase(otherOption.getLabel());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {

        return compareTo(obj) == 0;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return new StringBuffer().append(uri).append(" [").append(label).append("]").toString();
    }
}
