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
import java.util.Map;
import java.util.Map.Entry;

import eionet.cr.util.LinkedCaseInsensitiveMap;

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

    /** The column mappings. */
    private LinkedCaseInsensitiveMap<ObjectProperty> columnMappings = new LinkedCaseInsensitiveMap<ObjectProperty>();

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
     * @return the columnMappings
     */
    public Map<String, ObjectProperty> getColumnMappings() {
        return columnMappings;
    }

    /**
     * Put column mapping.
     * 
     * @param columnName the column name
     * @param propertyConf the property conf
     */
    public void putColumnMapping(String columnName, ObjectProperty propertyConf) {
        columnMappings.put(columnName, propertyConf);
    }

    /**
     * Put column names.
     * 
     * @param columnNames the column names
     */
    public void putColumnNames(Iterable<String> columnNames) {

        for (String colName : columnNames) {
            columnMappings.put(colName, null);
        }
    }

    /**
     * Clear column mappings.
     */
    public void clearColumnMappings() {
        this.columnMappings.clear();
    }

    /**
     * @param columnMappings the columnMappings to set
     */
    public void setColumnMappings(LinkedCaseInsensitiveMap<ObjectProperty> columnMappings) {
        this.columnMappings = columnMappings;
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
        sb.append("[Column mappings]").append(LINE_BREAK);
        for (Entry<String, ObjectProperty> entry : columnMappings.entrySet()) {
            sb.append(entry.getKey()).append(" = ").append(entry.getValue().getLabel()).append(LINE_BREAK);
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
}
