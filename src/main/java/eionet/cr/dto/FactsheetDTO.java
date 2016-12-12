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
 * Agency. Portions created by Zero Technologies are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *        Jaanus Heinlaid
 */

package eionet.cr.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import eionet.cr.web.util.WebConstants;

/**
 *
 * @author Jaanus Heinlaid
 */
public class FactsheetDTO extends SubjectDTO {

    /**  */
    private static final long serialVersionUID = 5263573490631076276L;

    /** */
    private HashMap<String, Integer> predicateObjectCounts = new HashMap<String, Integer>();

    /** */
    // private Map<String,Integer> predicatePages;

    /** */
    private Map<String, String> predicateLabels = new HashMap<String, String>();

    /**
     * @param uri
     * @param anonymous
     */
    public FactsheetDTO(String uri, boolean anonymous) {
        super(uri, anonymous);
    }

    /**
     *
     * @param predicateUri
     * @param objectCount
     */
    public void setObjectCount(String predicateUri, int objectCount) {
        predicateObjectCounts.put(predicateUri, objectCount);
    }

    /**
     *
     * @param predicateUri
     * @param objectPage
     */
    public void setObjectPage(String predicateUri, int objectPage) {
        predicateObjectCounts.put(predicateUri, objectPage);
    }

    /**
     * @return the objectCounts
     */
    public HashMap<String, Integer> getPredicateObjectCounts() {
        return predicateObjectCounts;
    }

    /**
     * @return the predicateLabels
     */
    public Map<String, String> getPredicateLabels() {
        return predicateLabels;
    }

    /**
     *
     * @param predicateUri
     * @param label
     */
    public void addPredicateLabel(String predicateUri, String label) {
        predicateLabels.put(predicateUri, label);
    }

    /**
     *
     * @return
     */
    public LinkedHashMap<String, Collection<ObjectDTO>> getSortedPredicates() {

        ArrayList<String> predicateUris = new ArrayList<String>(getPredicates().keySet());
        Collections.sort(predicateUris, new PredicateComparator());

        LinkedHashMap<String, Collection<ObjectDTO>> result = new LinkedHashMap<String, Collection<ObjectDTO>>();
        for (String predicateUri : predicateUris) {
            result.put(predicateUri, getObjects(predicateUri));
        }
        return result;
    }

    /**
     *
     * @return
     */
    public int getMaxObjectLength() {
        return WebConstants.MAX_OBJECT_LENGTH;
    }

    /**
     * @author Jaanus Heinlaid
     */
    class PredicateComparator implements Comparator<String> {

        /*
         * (non-Javadoc)
         *
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(String predicateUri1, String predicateUri2) {

            String label1 = predicateLabels.get(predicateUri1);
            if (StringUtils.isBlank(label1)) {
                label1 = predicateUri1;
            }
            String label2 = predicateLabels.get(predicateUri2);
            if (StringUtils.isBlank(label2)) {
                label2 = predicateUri2;
            }

            return label1.compareTo(label2);
        }

    }
}
