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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eionet.cr.staging.exp.ObjectTypes.DSD;
import eionet.cr.util.LinkedCaseInsensitiveMap;

/**
 * Describes a possible type (as in RDF) of objects returned by an RDF export query on a staging database.
 *
 * @author jaanus
 */
public class ObjectType {

    /** */
    private String uri;

    /** */
    private String label;

    /** */
    private DSD dsd;

    /** */
    private ArrayList<ObjectProperty> properties = new ArrayList<ObjectProperty>();

    /** */
    private String objectUriTemplate;

    /** */
    private LinkedCaseInsensitiveMap<ObjectProperty> columnToDefaultProperty = new LinkedCaseInsensitiveMap<ObjectProperty>();

    /** */
    private HashMap<String, ObjectProperty> predicateToProperty = new HashMap<String, ObjectProperty>();

    /** */
    private HashSet<ObjectHiddenProperty> hiddenProperties = new HashSet<ObjectHiddenProperty>();

    /** */
    private HashSet<ObjectProperty> requiredProperties = new HashSet<ObjectProperty>();

    /** */
    private HashMap<ObjectProperty, String[]> propertyToDefaultColumns = new HashMap<ObjectProperty, String[]>();

//    /** */
//    private String targetGraphValueTemplate = "http://semantic.digital-agenda-data.eu/data/<value>";

    /** */
    private Set<String> dynamicDatasetColumns;

    /** */
    private String fixedDatasetUri;

    /**
     * Constructs object type with the given type URI, label and associated DSD.
     *
     * @param uri the URI
     * @param label the label
     * @param dsd
     */
    public ObjectType(String uri, String label, DSD dsd) {

        super();
        this.uri = uri;
        this.label = label;
        this.dsd = dsd;
    }

    /**
     * Adds the property.
     *
     * @param property the property
     * @param isRequired indicates if this is a required property
     * @param defaultForColumn the default for column
     */
    public void addProperty(ObjectProperty property, boolean isRequired, String... defaultForColumn) {

        properties.add(property);
        predicateToProperty.put(property.getPredicate(), property);
        if (isRequired) {
            requiredProperties.add(property);
        }

        if (defaultForColumn != null && defaultForColumn.length > 0) {
            for (String column : defaultForColumn) {
                columnToDefaultProperty.put(column, property);
            }

            propertyToDefaultColumns.put(property, defaultForColumn);
        }
    }

    /**
     * Adds the hidden property.
     *
     * @param property the property
     */
    public void addHiddenProperty(ObjectHiddenProperty property) {
        hiddenProperties.add(property);
    }

    /**
     * Gets the default property.
     *
     * @param column the column
     * @return the default property
     */
    public ObjectProperty getDefaultProperty(String column) {
        return columnToDefaultProperty.get(column);
    }

    /**
     * Gets the properties.
     *
     * @return the properties
     */
    public List<ObjectProperty> getProperties() {
        return properties;
    }

    /**
     * Gets the property by predicate.
     *
     * @param predicateUri the predicate uri
     * @return the property by predicate
     */
    public ObjectProperty getPropertyByPredicate(String predicateUri) {
        return predicateToProperty.get(predicateUri);
    }

    /**
     * Gets the uri.
     *
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * Gets the label.
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns true if this type has this particular object property by plain '==' comparison. Otherwise returns false.
     *
     * @param property The property to check.
     * @return As indicated above.
     */
    public boolean hasThisProperty(ObjectProperty property) {

        for (ObjectProperty prop : properties) {
            if (prop == property) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return the requiredProperties
     */
    public HashSet<ObjectProperty> getRequiredProperties() {
        return requiredProperties;
    }

    /**
     * Gets the hidden properties.
     *
     * @return the hiddenProperties
     */
    public Set<ObjectHiddenProperty> getHiddenProperties() {
        return hiddenProperties;
    }

    /**
     * @return the propertyToDefaultColumns
     */
    public HashMap<ObjectProperty, String[]> getPropertyToDefaultColumns() {
        return propertyToDefaultColumns;
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
     * @return the dsd
     */
    public DSD getDsd() {
        return dsd;
    }
//
//    /**
//     * @return the targetGraphValueTemplate
//     */
//    public String getTargetGraphValueTemplate() {
//        return targetGraphValueTemplate;
//    }
//
//    /**
//     * @param targetGraphValueTemplate the targetGraphValueTemplate to set
//     */
//    public void setTargetGraphValueTemplate(String targetGraphValueTemplate) {
//        this.targetGraphValueTemplate = targetGraphValueTemplate;
//    }

    /**
     * @return the dynamicDatasetColumns
     */
    public Set<String> getDynamicDatasetColumns() {
        return dynamicDatasetColumns;
    }

    /**
     * @param dynamicDatasetColumns the dynamicDatasetColumns to set
     */
    public void setDynamicDatasetColumns(Set<String> dynamicDatasetColumns) {
        this.dynamicDatasetColumns = dynamicDatasetColumns;
    }

    /**
     * @return the fixedDatasetUri
     */
    public String getFixedDatasetUri() {
        return fixedDatasetUri;
    }

    /**
     * @param fixedDatasetUri the fixedDatasetUri to set
     */
    public void setFixedDatasetUri(String fixedDatasetUri) {
        this.fixedDatasetUri = fixedDatasetUri;
    }
}
