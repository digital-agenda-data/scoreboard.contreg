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
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.XMLSchema;

import eionet.cr.util.Hashes;
import eionet.cr.util.NamespaceUtil;
import eionet.cr.util.Util;
import eionet.cr.web.util.FactsheetObjectId;

// TODO: Auto-generated Javadoc
/**
 * A DTO representing an object of a triple.
 *
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 */
public class ObjectDTO implements Serializable {

    /** The serial ID. */
    private static final long serialVersionUID = 1L;

    /**
     * Types of an object in a triple.
     *
     * @author Jaanus
     */
    public enum Type {

        /** The literal. */
        LITERAL,
        /** The resource. */
        RESOURCE;
    }

    /** The value. */
    private String value;

    /** The hash. */
    private long hash;

    /** The anonymous. */
    private boolean anonymous;

    /** The literal. */
    private boolean literal;

    /** The language. */
    private String language;

    /** The datatype. */
    private URI datatype;

    /** The deriv source uri. */
    private String derivSourceUri;

    /** The deriv source hash. */
    private long derivSourceHash;

    /** The deriv source gen time. */
    private long derivSourceGenTime;

    /** The source object hash. */
    private long sourceObjectHash;

    /** The dervied literal value. */
    private String derviedLiteralValue;

    /** The source uri. */
    private String sourceUri;

    /** The source hash. */
    private long sourceHash;

    /** The label object. */
    private ObjectDTO labelObject;

    /**
     * Repository-returned MD5 hash of the object. Used to indicate {@link #value} is different than the actual value in the
     * repository is different. For example {@link #value} might contain only a substring of what's really in the database.
     */
    private String objectMD5;

    /**
     * Instantiates a new object dto.
     *
     * @param value the value
     * @param language the language
     * @param literal the literal
     * @param anonymous the anonymous
     * @param datatype the datatype
     */
    public ObjectDTO(String value, String language, boolean literal, boolean anonymous, URI datatype) {

        this.value = value;
        this.language = language;
        this.literal = literal;
        this.anonymous = anonymous;
        this.datatype = datatype;
        this.hash = Hashes.spoHash(value);
    }

    /**
     * Instantiates a new object dto.
     *
     * @param value the value
     * @param language the language
     * @param literal the literal
     * @param anonymous the anonymous
     */
    public ObjectDTO(String value, String language, boolean literal, boolean anonymous) {
        this(value, language, literal, anonymous, null);
    }

    /**
     * Instantiates a new object dto.
     *
     * @param value the value
     * @param literal the literal
     */
    public ObjectDTO(String value, boolean literal) {
        this(value, null, literal, false, null);
    }

    /**
     * Instantiates a new object dto.
     *
     * @param value the value
     * @param literal the literal
     * @param datatype the datatype
     */
    public ObjectDTO(String value, boolean literal, URI datatype) {
        this(value, null, literal, false, datatype);
    }

    /**
     * Instantiates a new object dto.
     *
     * @param hash the hash
     * @param sourceHash the source hash
     * @param derivSourceHash the deriv source hash
     * @param sourceObjectHash the source object hash
     */
    private ObjectDTO(long hash, long sourceHash, long derivSourceHash, long sourceObjectHash) {

        this.hash = hash;
        this.sourceHash = sourceHash;
        this.derivSourceHash = derivSourceHash;
        this.sourceObjectHash = sourceObjectHash;
    }

    /**
     * Creates an object DTO from given inputs.
     *
     * @param hash the hash
     * @param sourceHash the source hash
     * @param derivSourceHash the deriv source hash
     * @param sourceObjectHash the source object hash
     * @return ObjectDTO
     */
    public static ObjectDTO create(long hash, long sourceHash, long derivSourceHash, long sourceObjectHash) {

        return new ObjectDTO(hash, sourceHash, derivSourceHash, sourceObjectHash);
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the language.
     *
     * @return the language
     */
    public String getLanguage() {

        if (isLiteral()) {
            return language;
        } else {
            return labelObject != null ? labelObject.getLanguage() : null;
        }
    }

    /**
     * Checks if is literal.
     *
     * @return the literal
     */
    public boolean isLiteral() {
        return literal;
    }

    /**
     * Checks if is anonymous.
     *
     * @return the anonymous
     */
    public boolean isAnonymous() {
        return anonymous;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (!(other instanceof ObjectDTO)) {
            return false;
        }

        String otherValue = ((ObjectDTO) other).getValue();
        return getValue() == null ? otherValue == null : getValue().equals(otherValue);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getValue() == null ? 0 : getValue().hashCode();
    }

    /**
     * Gets the hash.
     *
     * @return long
     */
    public long getHash() {
        return hash;
    }

    /**
     * Gets the deriv source uri.
     *
     * @return the derivSourceUri
     */
    public String getDerivSourceUri() {
        return derivSourceUri;
    }

    /**
     * Sets the deriv source uri.
     *
     * @param derivSource the derivSourceUri to set
     */
    public void setDerivSourceUri(String derivSource) {
        this.derivSourceUri = derivSource;
    }

    /**
     * Gets the source uri.
     *
     * @return the sourceUri
     */
    public String getSourceUri() {
        return sourceUri;
    }

    /**
     * Sets the source uri.
     *
     * @param source the sourceUri to set
     */
    public void setSourceUri(String source) {
        this.sourceUri = source;
    }

    /**
     * Gets the source smart.
     *
     * @return String
     */
    public String getSourceSmart() {

        if (derivSourceUri != null && derivSourceUri.trim().length() > 0) {
            return derivSourceUri;
        } else if (sourceUri != null && sourceUri.trim().length() > 0) {
            return sourceUri;
        } else {
            return null;
        }
    }

    /**
     * Gets the source object hash.
     *
     * @return the sourceObjectHash
     */
    public long getSourceObjectHash() {
        return sourceObjectHash;
    }

    /**
     * Sets the source object hash.
     *
     * @param sourceObjectHash the sourceObjectHash to set
     */
    public void setSourceObjectHash(long sourceObjectHash) {
        this.sourceObjectHash = sourceObjectHash;
    }

    /**
     * Sets the hash.
     *
     * @param hash the new hash
     */
    public void setHash(long hash) {
        this.hash = hash;
    }

    /**
     * Gets the deriv source gen time.
     *
     * @return the derivSourceGenTime
     */
    public long getDerivSourceGenTime() {
        return derivSourceGenTime;
    }

    /**
     * Sets the deriv source gen time.
     *
     * @param derivSourceGenTime the derivSourceGenTime to set
     */
    public void setDerivSourceGenTime(long derivSourceGenTime) {
        this.derivSourceGenTime = derivSourceGenTime;
    }

    /**
     * Gets the deriv source hash.
     *
     * @return the derivSourceHash
     */
    public long getDerivSourceHash() {
        return derivSourceHash;
    }

    /**
     * Sets the deriv source hash.
     *
     * @param derivSourceHash the derivSourceHash to set
     */
    public void setDerivSourceHash(long derivSourceHash) {
        this.derivSourceHash = derivSourceHash;
    }

    /**
     * Gets the source hash.
     *
     * @return the sourceHash
     */
    public long getSourceHash() {
        return sourceHash;
    }

    /**
     * Gets the source hash smart.
     *
     * @return long
     */
    public long getSourceHashSmart() {
        return derivSourceHash != 0 ? derivSourceHash : sourceHash;
    }

    /**
     * Sets the source hash.
     *
     * @param sourceHash the sourceHash to set
     */
    public void setSourceHash(long sourceHash) {
        this.sourceHash = sourceHash;
    }

    /**
     * Gets the id.
     *
     * @return String
     */
    public String getId() {

        return FactsheetObjectId.format(this);
    }

    /**
     * Gets the dervied literal value.
     *
     * @return the derviedLiteralValue
     */
    public String getDerviedLiteralValue() {
        return labelObject == null ? derviedLiteralValue : labelObject.getValue();
    }

    /**
     * Sets the dervied literal value.
     *
     * @param sourceObjectValue the derviedLiteralValue to set
     */
    public void setDerviedLiteralValue(String sourceObjectValue) {
        this.derviedLiteralValue = sourceObjectValue;
    }

    /**
     * Gets the datatype.
     *
     * @return the datatype
     */
    public URI getDatatype() {
        return datatype;
    }

    /**
     * Returns datatype label to display. If the namespace is known replaces it with the prefix defined in Namespace otherwise
     * returns URL with full namespace
     *
     * @return String datatype label
     */
    public String getDataTypeLabel() {
        if (datatype == null) {
            return "Not specified";
        }
        // if datatype is from XSD schema, replace http://www.w3.org/2001/XMLSchema with xsd
        String ns = datatype.getNamespace();
        if (NamespaceUtil.getKnownNamespace(datatype.getNamespace()) != null) {
            ns = NamespaceUtil.getKnownNamespace(datatype.getNamespace()) + ":";
        }
        String local = datatype.getLocalName();
        return ns + local;
    }

    /**
     * Sets the datatype.
     *
     * @param datatype the new datatype
     */
    public void setDatatype(URI datatype) {
        this.datatype = datatype;
    }

    /**
     * Creates the literal.
     *
     * @param value the value
     * @return the object dto
     */
    public static ObjectDTO createLiteral(Object value) {
        return new ObjectDTO(value.toString(), true);
    }

    /**
     * Utility method for constructing a literal with datatype set to {@link XMLSchema#INT}.
     *
     * @param value The value.
     * @return The literal object.
     */
    public static ObjectDTO createLiteral(int value) {
        return createLiteral(Integer.valueOf(value), XMLSchema.INTEGER);
    }

    /**
     * Creates the literal.
     *
     * @param value the value
     * @param datatype the datatype
     * @return the object dto
     */
    public static ObjectDTO createLiteral(Object value, URI datatype) {
        return new ObjectDTO(value.toString(), true, datatype);
    }

    /**
     * Creates the resource.
     *
     * @param uri the uri
     * @return the object dto
     */
    public static ObjectDTO createResource(String uri) {
        return new ObjectDTO(uri, false);
    }

    /**
     * Gets the date value.
     *
     * @return the date value
     */
    public Date getDateValue() {
        return Util.virtuosoStringToDate(getValue());
    }

    /**
     * Gets the display value.
     *
     * @return the display value
     */
    public String getDisplayValue() {

        if (isLiteral()) {
            return value;
        } else {
            String displayValue = getDerviedLiteralValue();
            return StringUtils.isBlank(displayValue) ? value : displayValue;
        }
    }

    /**
     * Sets the label object.
     *
     * @param labelObject the labelObject to set
     */
    public void setLabelObject(ObjectDTO labelObject) {
        this.labelObject = labelObject;
    }

    /**
     * Gets the object m d5.
     *
     * @return the objectMD5
     */
    public String getObjectMD5() {
        return objectMD5;
    }

    /**
     * Sets the object m d5.
     *
     * @param objectMD5 the objectMD5 to set
     */
    public void setObjectMD5(String objectMD5) {
        this.objectMD5 = objectMD5;
    }
}
