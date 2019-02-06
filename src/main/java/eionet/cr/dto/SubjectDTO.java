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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.util.Hashes;
import eionet.cr.util.URIUtil;
import eionet.cr.util.URLUtil;
import eionet.cr.web.security.CRUser;

/**
 *
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
/**
 *
 * @author Jaanus Heinlaid
 */
public class SubjectDTO implements Serializable {

    /**
     * serial.
     */
    private static final long serialVersionUID = 1L;

    /** */
    public static final String URI_ANONYMOUS = "anonymous";
    public static final String NO_LABEL = "No label";

    /** */
    private String uri;
    private long uriHash;
    private boolean anonymous;
    private Map<String, Collection<ObjectDTO>> predicates = new HashMap<String, Collection<ObjectDTO>>();
    private Date dcDate;
    private Date lastModifiedDate;

    /** */
    private long hitSource;

    /**
     *
     * @param uri
     * @param anonymous
     */
    public SubjectDTO(String uri, boolean anonymous) {

        if (uri == null || uri.trim().length() == 0) {
            throw new IllegalArgumentException("Trying to construct a subject with a blank URI");
        }

        this.uri = uri;
        this.anonymous = anonymous;

        uriHash = Hashes.spoHash(uri);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString() This toString is used in the Unit-tests.
     */
    @Override
    public String toString() {
        return "SubjectDTO[uri=" + uri + ",predicates=" + predicates.toString() + "]";
    }

    /**
     *
     * @param predicate
     * @param object
     */
    public void addObject(String predicate, ObjectDTO object) {

        Collection<ObjectDTO> objects = predicates.get(predicate);
        if (objects == null) {
            objects = new ArrayList<ObjectDTO>();
            predicates.put(predicate, objects);
        }
        objects.add(object);
    }

    /**
     *
     * @param predicate
     * @param object
     */
    public void setObject(String predicate, ObjectDTO object) {

        Collection<ObjectDTO> objects = new ArrayList<ObjectDTO>();
        objects.add(object);
        predicates.put(predicate, objects);
    }

    /**
     *
     * @param predicate
     * @param objects
     */
    public void setObjects(String predicate, Collection<ObjectDTO> objects) {

        predicates.put(predicate, objects);
    }

    /**
     *
     * @return
     */
    public Map<String, Collection<ObjectDTO>> getPredicates() {
        return predicates;
    }

    /**
     *
     * @return
     */
    public Set<String> getPredicateUris() {
        return predicates.keySet();
    }

    /**
     *
     * @param predicate
     * @return
     */
    public Collection<ObjectDTO> getObjects(String predicate) {

        return predicates.get(predicate);
    }

    /**
     *
     * @param predicateUri
     * @param languages
     * @return
     */
    public Collection<ObjectDTO> getObjectsForSearchResultsDisplay(String predicateUri, List<String> languages) {

        Collection<ObjectDTO> result = new ArrayList<ObjectDTO>();
        Collection<ObjectDTO> fromHitSources = new ArrayList<ObjectDTO>();

        Collection<ObjectDTO> objects = getObjects(predicateUri);
        if (objects != null && !objects.isEmpty()) {

            // remember the values of literals which have been derived from resource objects
            HashMap<Long, String> derivedLiterals = new HashMap<Long, String>();
            for (ObjectDTO object : objects) {

                if (object.isLiteral() && object.getSourceObjectHash() != 0) {
                    derivedLiterals.put(Long.valueOf(object.getSourceObjectHash()), object.getValue());
                }
            }

            for (ObjectDTO object : objects) {

                // check that object value is not blank
                String objectValue = object.getValue().trim();
                if (!StringUtils.isBlank(objectValue)) {

                    // skip literals that have been derived from resource objects
                    if (object.isLiteral() && object.getSourceObjectHash() != 0) {
                        continue;
                    }

                    // skip literals which have a language that is not present in the
                    // language preferences
                    if (object.isLiteral() && languages != null && !languages.isEmpty() && object.getLanguage() != null
                            && !languages.contains(object.getLanguage())) {
                        continue;
                    }

                    // if object is from the subject's hit source, add it to a separate list
                    if (hitSource > 0 && object.getSourceHashSmart() == hitSource) {
                        fromHitSources.add(object);
                    } else {
                        result.add(object);
                    }

                    // if resource object, set its derived literal
                    // (will be set to null if no derived literals found)
                    if (!object.isLiteral()) {
                        object.setDerviedLiteralValue(derivedLiterals.get(Long.valueOf(Hashes.spoHash(objectValue))));
                    }
                }
            }
        }

        return new LinkedHashSet<ObjectDTO>(!fromHitSources.isEmpty() ? fromHitSources : result);
    }

    /**
     *
     * @param predicate
     * @param objectType
     * @return
     */
    public Collection<ObjectDTO> getObjects(String predicate, ObjectDTO.Type objectType) {

        ArrayList<ObjectDTO> result = new ArrayList<ObjectDTO>();

        Collection<ObjectDTO> coll = getObjects(predicate);
        if (coll != null && !coll.isEmpty()) {
            for (Iterator<ObjectDTO> iter = coll.iterator(); iter.hasNext();) {
                ObjectDTO objectDTO = iter.next();
                if (objectType.equals(ObjectDTO.Type.LITERAL) && objectDTO.isLiteral()) {
                    result.add(objectDTO);
                } else if (objectType.equals(ObjectDTO.Type.RESOURCE) && !objectDTO.isLiteral()) {
                    result.add(objectDTO);
                }
            }
        }

        return result;
    }

    /**
     *
     * @param predicate
     * @return
     */
    public ObjectDTO getObject(String predicate) {
        Collection<ObjectDTO> objects = getObjects(predicate);
        return objects == null || objects.isEmpty() ? null : objects.iterator().next();
    }

    /**
     *
     * @param predicate
     * @param objectType
     * @return
     */
    public ObjectDTO getObject(String predicate, ObjectDTO.Type objectType) {
        Collection<ObjectDTO> objects = getObjects(predicate, objectType);
        return objects == null || objects.isEmpty() ? null : objects.iterator().next();
    }

    /**
     *
     * @param predicate
     * @return
     */
    public String getObjectValue(String predicate) {
        ObjectDTO objectDTO = getObject(predicate);
        return objectDTO == null ? null : objectDTO.getValue();
    }

    /**
     * Create the list of object values.
     *
     * @param predicate URL
     * @return the list of object values.
     */
    public List<String> getObjectValues(String predicate) {

        List<String> objectValues = new ArrayList<String>();
        Collection<ObjectDTO> objects = getObjects(predicate);
        if (objects != null && !objects.isEmpty()) {

            for (ObjectDTO object : objects) {
                objectValues.add(object.getValue());
            }
        }
        return objectValues;
    }

    /**
     *
     * @param predicate
     * @return
     */
    public String getObjectValue(String predicate, ObjectDTO.Type objectType) {
        ObjectDTO objectDTO = getObject(predicate, objectType);
        return objectDTO == null ? null : objectDTO.getValue();
    }

    /**
     *
     * @return
     */
    public int getPredicateCount() {
        return predicates.size();
    }

    /**
     *
     * @return
     */
    public String getUri() {
        return uri;
    }

    /**
     *
     * @return
     */
    public boolean isAnonymous() {
        return anonymous;
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

        if (!(other instanceof SubjectDTO)) {
            return false;
        }

        String otherUri = ((SubjectDTO) other).getUri();
        return getUri() == null ? otherUri == null : getUri().equals(otherUri);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {

        return getUri() == null ? 0 : getUri().hashCode();
    }

    /**
     *
     * @return
     */
    public String getUrl() {

        if (uri != null && URLUtil.isURL(uri)) {
            return uri;
        } else {
            ObjectDTO o = getObject(Predicates.DC_IDENTIFIER);
            return o == null || !URLUtil.isURL(o.getValue()) ? null : o.getValue();
        }
    }

    /**
     *
     * @param predicateUri
     * @return
     */
    public boolean hasPredicate(String predicateUri) {

        return predicates != null && predicates.containsKey(predicateUri);
    }

    /**
     *
     * @param predicate
     * @param objectValue
     * @return
     */
    public boolean hasPredicateObject(String predicate, String objectValue) {

        boolean result = false;
        Collection<ObjectDTO> objects = getObjects(predicate);
        if (objects != null && !objects.isEmpty()) {
            for (Iterator<ObjectDTO> i = objects.iterator(); i.hasNext();) {
                if (objectValue.equals(i.next().getValue())) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * @return the uriHash
     */
    public long getUriHash() {
        return uriHash;
    }

    /**
     * @param uriHash the uriHash to set
     */
    public void setUriHash(long uriHash) {
        this.uriHash = uriHash;
    }

    /**
     *
     * @param predicate
     * @param objectValue
     * @param sourceHash
     * @return
     */
    public boolean existsPredicateObjectSource(String predicate, String objectValue, String sourceUri) {

        Collection<ObjectDTO> objects = getObjects(predicate);
        if (objects != null && !objects.isEmpty()) {
            for (ObjectDTO objectDTO : objects) {
                if (objectDTO.getValue().equals(objectValue)) {
                    if (objectDTO.getSourceUri().equals(sourceUri)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * @return the hitSource
     */
    public long getHitSource() {
        return hitSource;
    }

    /**
     * @param hitSource the hitSource to set
     */
    public void setHitSource(long hitSource) {
        this.hitSource = hitSource;
    }

    /**
     * @return the lastModifiedTime
     */
    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * @param lastModifiedDate the lastModifiedTime to set
     */
    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     *
     * @return
     */
    public String getLabel() {

        ObjectDTO object = getObject(Predicates.RDFS_LABEL, ObjectDTO.Type.LITERAL);
        String label = object != null ? object.getValue() : null;
        if (label != null && label.trim().length() > 0) {
            return label;
        } else if (isAnonymous()) {
            return "Anonymous resource";
        } else {
            return URIUtil.extractURILabel(getUri(), NO_LABEL);
        }
    }

    /**
     *
     * @param user
     * @return
     */
    public boolean isRegisteredBy(CRUser user) {

        return existsPredicateObjectSource(Predicates.RDF_TYPE, Subjects.CR_FILE, user.getRegistrationsUri());
    }

    /**
     * @return
     */
    public Date getDcDate() {
        return dcDate;
    }

    /**
     * @param dcDate
     */
    public void setDcDate(Date dcDate) {
        this.dcDate = dcDate;
    }

    /**
     *
     * @return
     */
    public int getTripleCount() {

        int result = 0;
        for (Collection<ObjectDTO> objects : predicates.values()) {
            if (objects != null) {
                result = result + objects.size();
            }
        }
        return result;
    }

    /**
     * Returns a set of URIs of all distinct harvest sources in the triples of this subject.
     *
     * @return Set of distinct harvest source URIs.
     */
    public Set<String> getSources() {

        HashSet<String> result = new HashSet<String>();
        if (predicates != null && !predicates.isEmpty()) {

            Collection<Collection<ObjectDTO>> objectCollections = predicates.values();
            for (Collection<ObjectDTO> collection : objectCollections) {

                if (collection != null) {
                    for (ObjectDTO objectDTO : collection) {
                        if (!StringUtils.isEmpty(objectDTO.getSourceUri())) {
                            result.add(objectDTO.getSourceUri());
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     *
     * @param rdfType
     * @return
     */
    public boolean hasRdfType(String rdfType) {

        List<String> values = this.getObjectValues(Predicates.RDF_TYPE);
        return values != null && values.contains(rdfType);
    }

    /**
     *
     * @param predicateUri
     * @param objectValue
     * @return
     */
    public ObjectDTO getPredicateObject(String predicateUri, String objectValue) {

        Collection<ObjectDTO> objects = getObjects(predicateUri);
        if (objects != null && !objects.isEmpty()) {
            for (ObjectDTO object : objects) {
                if (object.getValue().equals(objectValue)) {
                    return object;
                }
            }
        }

        return null;
    }
}
