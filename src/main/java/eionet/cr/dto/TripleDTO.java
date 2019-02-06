/*
 * The contents of this file are subjectUri to the Mozilla Public
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

import eionet.cr.util.Hashes;

/**
 *
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
public class TripleDTO {

    /** */
    private long subjectHash;
    /** Local variable. */
    private long predicateHash;
    /** Local variable. */
    private long objectHash;

    /** */
    private Long sourceHash;
    /** Local variable. */
    private Long objectDerivSourceHash;
    /** Local variable. */
    private Long objectSourceObjectHash;

    /** */
    private boolean isAnonymousSubject;
    /** */
    private boolean isAnonymousObject;
    /** */
    private boolean isLiteralObject;

    /** */
    private String object;
    /** */
    private String objectLanguage;
    /** */
    private Double objectDouble;

    /** */
    private Long genTime;
    /** */
    private Long objectDerivGenTime;

    /** */
    private String subjectUri;
    /** Local holder of predicateUri. */
    private String predicateUri;
    /** Local holder of sourceUri. */
    private String sourceUri;
    /** Local holder of object derivation source uri. */
    private String objectDerivSourceUri;

    /** */
    private String objectMd5;

    /**
     * Creates a new TripleDTO object.
     *
     * @param subjectUri subject URI
     * @param predicateUri predicate Uri
     * @param objectValue Object value
     */
    public TripleDTO(String subjectUri, String predicateUri, String objectValue) {
        this.subjectUri = subjectUri;
        this.predicateUri = predicateUri;
        this.object = objectValue;
    }

    /**
     *
     * @param subjectUri
     * @param predicateUri
     * @param objectValue
     * @param graphUri
     */
    public TripleDTO(String subjectUri, String predicateUri, String objectValue, String graphUri) {
        this.subjectUri = subjectUri;
        this.predicateUri = predicateUri;
        this.object = objectValue;
        this.sourceUri = graphUri;
    }

    /**
     * Constructor of a new TripleDTO object.
     *
     * @param subjectHash Hashed subject
     * @param predicateHash hashed predicate
     * @param objectHash hashed object
     */
    public TripleDTO(long subjectHash, long predicateHash, long objectHash) {

        this.subjectHash = subjectHash;
        this.predicateHash = predicateHash;
        this.objectHash = objectHash;
    }

    /**
     * Constructor of a new TripleDTO object.
     *
     * @param subjectHash hashed subject
     * @param predicateHash hashed predicate of the triple
     * @param object Object of the triple
     */
    public TripleDTO(long subjectHash, long predicateHash, String object) {

        if (object == null) {
            throw new IllegalArgumentException("object must not be null!");
        }

        this.subjectHash = subjectHash;
        this.predicateHash = predicateHash;
        this.object = object;
        this.objectHash = Hashes.spoHash(object);
    }

    /**
     * @return the subjectHash
     */
    public long getSubjectHash() {
        return subjectHash;
    }

    /**
     * @return the predicateHash
     */
    public long getPredicateHash() {
        return predicateHash;
    }

    /**
     * @return the objectHash
     */
    public long getObjectHash() {
        return objectHash;
    }

    /**
     * @return the subjectUri
     */
    public String getSubjectUri() {
        return subjectUri;
    }

    /**
     * @param subjectUri the subjectUri to set
     */
    public void setSubjectUri(String subjectUri) {
        this.subjectUri = subjectUri;
    }

    /**
     * @return the predicateUri
     */
    public String getPredicateUri() {
        return predicateUri;
    }

    /**
     * @param predicateUri the predicateUri to set
     */
    public void setPredicateUri(String predicateUri) {
        this.predicateUri = predicateUri;
    }

    /**
     * @return the sourceUri
     */
    public String getSourceUri() {
        return sourceUri;
    }

    /**
     * @param sourceUri the sourceUri to set
     */
    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    /**
     * @return the objectDerivSourceUri
     */
    public String getObjectDerivSourceUri() {
        return objectDerivSourceUri;
    }

    /**
     * @param objectDerivSourceUri the objectDerivSourceUri to set
     */
    public void setObjectDerivSourceUri(String objectDerivSourceUri) {
        this.objectDerivSourceUri = objectDerivSourceUri;
    }

    /**
     * @return the isAnonymousSubject
     */
    public boolean isAnonymousSubject() {
        return isAnonymousSubject;
    }

    /**
     * @param isAnonymousSubject the isAnonymousSubject to set
     */
    public void setAnonymousSubject(boolean isAnonymousSubject) {
        this.isAnonymousSubject = isAnonymousSubject;
    }

    /**
     * @return the isAnonymousObject
     */
    public boolean isAnonymousObject() {
        return isAnonymousObject;
    }

    /**
     * @param isAnonymousObject the isAnonymousObject to set
     */
    public void setAnonymousObject(boolean isAnonymousObject) {
        this.isAnonymousObject = isAnonymousObject;
    }

    /**
     * @return the isLiteralObject
     */
    public boolean isLiteralObject() {
        return isLiteralObject;
    }

    /**
     * @param isLiteralObject the isLiteralObject to set
     */
    public void setLiteralObject(boolean isLiteralObject) {
        this.isLiteralObject = isLiteralObject;
    }

    /**
     * @return the object
     */
    public String getObject() {
        return object;
    }

    /**
     * @return the objectLanguage
     */
    public String getObjectLanguage() {
        return objectLanguage;
    }

    /**
     * @param objectLanguage the objectLanguage to set
     */
    public void setObjectLanguage(String objectLanguage) {
        this.objectLanguage = objectLanguage;
    }

    /**
     * @return the objectDouble
     */
    public Double getObjectDouble() {
        return objectDouble;
    }

    /**
     * @param objectDouble the objectDouble to set
     */
    public void setObjectDouble(Double objectDouble) {
        this.objectDouble = objectDouble;
    }

    /**
     * @return the sourceHash
     */
    public Long getSourceHash() {
        return sourceHash;
    }

    /**
     * @param sourceHash the sourceHash to set
     */
    public void setSourceHash(Long sourceHash) {
        this.sourceHash = sourceHash;
    }

    /**
     * @return the objectDerivSourceHash
     */
    public Long getObjectDerivSourceHash() {
        return objectDerivSourceHash;
    }

    /**
     * @param objectDerivSourceHash the objectDerivSourceHash to set
     */
    public void setObjectDerivSourceHash(Long objectDerivSourceHash) {
        this.objectDerivSourceHash = objectDerivSourceHash;
    }

    /**
     * @return the objectSourceObjectHash
     */
    public Long getObjectSourceObjectHash() {
        return objectSourceObjectHash;
    }

    /**
     * @param objectSourceObjectHash the objectSourceObjectHash to set
     */
    public void setObjectSourceObjectHash(Long objectSourceObjectHash) {
        this.objectSourceObjectHash = objectSourceObjectHash;
    }

    /**
     * @return the genTime
     */
    public Long getGenTime() {
        return genTime;
    }

    /**
     * @param genTime the genTime to set
     */
    public void setGenTime(Long genTime) {
        this.genTime = genTime;
    }

    /**
     * @return the objectDerivGenTime
     */
    public Long getObjectDerivGenTime() {
        return objectDerivGenTime;
    }

    /**
     * @param objectDerivGenTime the objectDerivGenTime to set
     */
    public void setObjectDerivGenTime(Long objectDerivGenTime) {
        this.objectDerivGenTime = objectDerivGenTime;
    }

    public String getObjectMd5() {
        return objectMd5;
    }

    public void setObjectMd5(String objectMd5) {
        this.objectMd5 = objectMd5;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("<").append(subjectUri).append(">");
        sb.append(" <").append(predicateUri).append(">");

        if (!isLiteralObject) {
            sb.append(" <").append(object).append(">");
        } else {
            sb.append(" \"").append(object).append("\"");
        }

        return sb.toString();
    }
}
