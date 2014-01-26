/*
 * The contents of this file are subject to the Mozilla Public
 *
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
 * Agency. Portions created by Tieto Eesti are Copyright
 * (C) European Environment Agency. All Rights Reserved.
 *
 * Contributor(s):
 * Jaanus Heinlaid, Tieto Eesti
 */
package eionet.cr.common;

/**
 * Prefixes for common namespaces.
 * 
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 * 
 */
public enum Namespace {

    /** The XML namespace. */
    XML("http://www.w3.org/XML/1998/namespace", "xml"),
    /** RDF base prefix. */
    RDF("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf"),
    /** RDF Schema base prefix. */
    RDFS("http://www.w3.org/2000/01/rdf-schema#", "rdfs"),
    /** OWL base prefix. */
    OWL("http://www.w3.org/2002/07/owl#", "owl"),
    /** Dublin Core 1.1 base prefix. */
    DC("http://purl.org/dc/elements/1.1/", "dc"),
    /** Content Registry base prefix. */
    CR("http://cr.eionet.europa.eu/ontologies/contreg.rdf#", "cr"),
    /** Indicator Management System base prefix - EEA internal. */
    IMS("http://rdfdata.eionet.europa.eu/ims/ontology/", "ims"),
    /** Annual Management Plan base prefix - EEA internal. */
    AMP("http://rdfdata.eionet.europa.eu/amp/ontology/", "amp"),
    /** Annual Management Plan base prefix - EEA internal. */
    AMP_IGN("http://moonefish.eea.europa.eu:81/AMP-schema.rdf#", "ampign"), // Phased out
    /** Reporting Obligations base prefix - EEA internal. */
    ROD("http://rod.eionet.europa.eu/schema.rdf#", "rod"),
    /** Reporting Obligations base prefix - EEA internal. */
    ROD_OLD("http://rod.eionet.eu.int/schema.rdf#", "rodold"), // Phased out
    /** EPER dataflow base prefix - EEA internal. */
    EPER("http://rdfdata.eionet.europa.eu/eper/dataflow", "eper"),
    /** XML Schema namespace. */
    XSD("http://www.w3.org/2001/XMLSchema#", "xsd"),
    /** Dublin Core Metadata Initiative Abstract Model. */
    DCAM("http://purl.org/dc/dcam/", "dcam"),
    /** W3C Data Catalog Vocabulary. */
    DCAT("http://www.w3.org/ns/dcat#", "dcat"),
    /** Dublin Core terms. */
    DCT("http://purl.org/dc/terms/", "dct"),
    /** European Commission's Open Data Portal. */
    ECODP("http://open-data.europa.eu/ontologies/ec-odp#", "ecodp"),
    /** Friend of a friend. */
    FOAF("http://xmlns.com/foaf/0.1/", "foaf"),
    /** Simple Knowledge Organisation System. */
    SKOS("http://www.w3.org/2004/02/skos/core#", "skos"),
    /** Simple Knowledge Organisation System XL. */
    SKOS_XL("http://www.w3.org/2008/05/skos-xl#", "skos-xl"),
    /** XMLSchema-instance namespace. */
    XSI("http://www.w3.org/2001/XMLSchema-instance", "xsi");

    /** Namespace URI. */
    private final String uri;
    /** Namespace prefix. */
    private final String prefix;

    /**
     * Simple constructor for the given namespace uri-prefix pair.
     * 
     * @param uri The URI.
     * @param prefix The prefix.
     */
    private Namespace(String uri, String prefix) {
        this.uri = uri;
        this.prefix = prefix;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }
}
