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
 *        jaanus
 */

package eionet.cr.staging.exp;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import eionet.cr.staging.exp.ObjectTypes.DSD;

/**
 * A bean that represents an RDF export query's configuration (in the context of staging databases).
 *
 * @author jaanus
 */
public class QueryConfiguration implements Serializable {

    /** */
    private static final String LINE_BREAK = "\n";

    /** The query. */
    private String query;

    /** The object type uri. */
    private String objectTypeUri;

    /** The object type DSD. */
    private DSD objectTypeDsd;

    /** */
    private Map<ObjectProperty, String> propertyMappings = new LinkedHashMap<>();

    /** */
    private String objectUriTemplate;

    /** The indicator URI. */
    private String indicatorUri;

    /** The datasetUri URI. */
    private String datasetUri;

    /** If true, the dataset should be cleared before the export query is executed. */
    private boolean clearDataset;

    /**
     * @return the query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query the query to set
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * @return the objectTypeUri
     */
    public String getObjectTypeUri() {
        return objectTypeUri;
    }

    /**
     * @param objectTypeUri the objectTypeUri to set
     */
    public void setObjectTypeUri(String objectTypeUri) {
        this.objectTypeUri = objectTypeUri;
    }

    /**
     *
     * @param objectProperty
     * @param selectorColumn
     */
    public void putPropertyMapping(ObjectProperty objectProperty, String selectorColumn) {
        propertyMappings.put(objectProperty, selectorColumn);
    }

    /**
     * Clear column mappings.
     */
    public void clearPropertyMappings() {
        propertyMappings.clear();
    }

    /**
     * @return the indicatorUri
     */
    public String getIndicatorUri() {
        return indicatorUri;
    }

    /**
     * @param indicatorUri the indicatorUri to set
     */
    public void setIndicatorUri(String indicator) {
        this.indicatorUri = indicator;
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
    public void setDatasetUri(String dataset) {
        this.datasetUri = dataset;
    }

    /**
     * Returns a string "dump" of this {@link QueryConfiguration} that is suitable for storage into the RDF export table in the
     * database.
     *
     * @return The string "dump".
     */
    public String toLongString() {

        StringBuilder sb = new StringBuilder();
        sb.append("[Query]").append(LINE_BREAK);
        sb.append(query).append(LINE_BREAK);
        sb.append(LINE_BREAK);
        sb.append("[Property mappings]").append(LINE_BREAK);
        for (Entry<ObjectProperty, String> entry : propertyMappings.entrySet()) {
            sb.append(entry.getKey().getLabel()).append(" = ").append(entry.getValue()).append(LINE_BREAK);
        }
        sb.append(LINE_BREAK);
        sb.append("[Other settings]").append(LINE_BREAK);
        sb.append("Objects type: ").append(objectTypeUri).append(LINE_BREAK);
        sb.append("Indicator: ").append(indicatorUri).append(LINE_BREAK);
        sb.append("Dataset: ").append(datasetUri);
        sb.append(LINE_BREAK);

        return sb.toString();
    }

    /**
     * @return the objectUriTemplate
     */
    public String getObjectUriTemplate() {
        return objectUriTemplate;
    }

    /**
     * @param objectUriTemplate the objectUriTemplate to set
     */
    public void setObjectUriTemplate(String objectUriTemplate) {
        this.objectUriTemplate = objectUriTemplate;
    }

    /**
     * @param clearDataset the clearDataset to set
     */
    public void setClearDataset(boolean clearDataset) {
        this.clearDataset = clearDataset;
    }

    /**
     * @return the clearDataset
     */
    public boolean isClearDataset() {
        return clearDataset;
    }

    /**
     *
     */
    public void validateDatasetPresence() {

        // TODO: rewrite in the light of new property mappings

//        boolean hasDatasetMapping= true;
//        for (Entry<String, ObjectProperty> entry : columnMappings.entrySet()) {
//            ObjectProperty property = entry.getValue();
//            if (property != null && property.getId().equalsIgnoreCase("dataSet")) {
//                hasDatasetMapping = true;
//            }
//        }
//
//        boolean hasFixedDataset = StringUtils.isNotBlank(datasetUri);
//        if (!hasDatasetMapping && !hasFixedDataset) {
//            throw new IllegalArgumentException("Dataset must be specified!");
//        } else if (hasDatasetMapping && hasFixedDataset) {
//            throw new IllegalArgumentException("Dataset cannot be specieid through column mappings and fixed value at the same time!");
//        }
    }

    /**
     * @return the objectTypeDsd
     */
    public DSD getObjectTypeDsd() {
        return objectTypeDsd;
    }

    /**
     * @param objectTypeDsd the objectTypeDsd to set
     */
    public void setObjectTypeDsd(DSD objectTypeDsd) {
        this.objectTypeDsd = objectTypeDsd;
    }

    /**
     * @return the propertyMappings
     */
    public Map<ObjectProperty, String> getPropertyMappings() {
        return propertyMappings;
    }

    /**
     * @param propertyMappings the propertyMappings to set
     */
    public void setPropertyMappings(Map<ObjectProperty, String> propertyMappings) {
        this.propertyMappings = propertyMappings;
    }
}
