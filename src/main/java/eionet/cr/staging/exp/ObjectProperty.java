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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;

// TODO: Auto-generated Javadoc
/**
 * An RDF property's metadata bean. Will be mapped to selected SQL columns at SQL-result-set-to-RDF export-
 *
 * @author jaanus
 */
public class ObjectProperty {

    /** */
    private String id;

    /** */
    private String label;

    /**  */
    private String predicate;

    /** */
    private Range range;

    /** */
    private String valueTemplate;

    /** */
    private String dataType;

    /** */
    private URI predicateURI;

    /** */
    private String hint;

    /**
     * Class constructor.
     *
     * @param predicate The property's underlying predicate.
     * @param id The property's alpha-numeric ID for internal use.
     * @param label The property's label as it should be displayed to the users of staging databases functionality.
     * @param range Indicates whether the property's values should be literals or resources.
     */
    public ObjectProperty(String predicate, String id, String label, Range range) {

        if (StringUtils.isBlank(predicate) || StringUtils.isBlank(label) || StringUtils.isBlank(id) || range == null) {
            throw new IllegalArgumentException("None of the constrcutor inputs must be null or blank!");
        }

        this.predicate = predicate;
        this.label = label;
        this.range = range;
        this.id = id;
    }

    /**
     * Gets the predicate.
     *
     * @return the predicate
     */
    public String getPredicate() {
        return predicate;
    }

    /**
     * Gets the range.
     *
     * @return the range
     */
    public Range getRange() {
        return range;
    }

    /**
     * Gets the value template.
     *
     * @return the valueTemplate
     */
    public String getValueTemplate() {
        return valueTemplate;
    }

    /**
     * Sets the value template.
     *
     * @param valueTemplate the valueTemplate to set
     */
    public void setValueTemplate(String valueTemplate) {
        this.valueTemplate = valueTemplate;
    }

    /**
     * Gets the data type.
     *
     * @return the dataType
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Sets the data type.
     *
     * @param dataType the dataType to set
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    /**
     * Returns true if the property's values are resources.
     *
     * @return As said above.
     */
    public boolean isResourceRange() {
        return Range.RESOURCE.equals(range);
    }

    /**
     * Returns true if the property's values are literals.
     *
     * @return As said above.
     */
    public boolean isLiteralRange() {
        return Range.LITERAL.equals(range);
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
     * Getter for {@link #predicateURI}.
     *
     * @return {@link #predicateURI}
     */
    protected URI getPredicateURI() {
        return predicateURI;
    }

    /**
     * Sets {@link #predicateURI} for the {@link #predicate}, using the given {@link ValueFactory}.
     *
     * @param vf the new predicate uri
     */
    protected void setPredicateURI(ValueFactory vf) {
        this.predicateURI = vf.createURI(predicate);
    }

    /**
     * Gets the hint.
     *
     * @return the hint
     */
    public String getHint() {
        return hint;
    }

    /**
     * Sets the hint.
     *
     * @param hint the hint to set
     */
    public void setHint(String hint) {
        this.hint = hint;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new StringBuilder().append(label).append(": ").append(predicate).toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {

        if (other == null) {
            return false;
        }

        if (other == this) {
            return true;
        }

        if (other.getClass() != getClass()) {
            return false;
        }

        ObjectProperty otherProperty = (ObjectProperty) other;
        return new EqualsBuilder().append(predicate, otherProperty.predicate).isEquals();
    }

    /**
     * Enumeration for indicating the value range of a given RDF property.
     *
     * @author jaanus
     */
    public static enum Range {

        /** */
        RESOURCE("Resource"), LITERAL("Literal");

        /** */
        private String friendlyName;

        /**
         * Constructor, allowing a friendly name.
         *
         * @param friendlyName The friendly name.
         */
        private Range(String friendlyName) {
            this.friendlyName = friendlyName;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return friendlyName;
        }
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }
}
