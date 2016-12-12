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
package eionet.cr.common;

/**
 * Constants indicating commonly used RDF subjects, mostly classes.
 *
 * @author Jaanus Heinlaid
 */
public interface Subjects {

    String ROD_OBLIGATION_CLASS = "http://rod.eionet.europa.eu/schema.rdf#Obligation";
    String ROD_INSTRUMENT_CLASS = "http://rod.eionet.europa.eu/schema.rdf#Instrument";
    String ROD_LOCALITY_CLASS = "http://rod.eionet.europa.eu/schema.rdf#Locality";
    String ROD_ISSUE_CLASS = "http://rod.eionet.europa.eu/schema.rdf#Issue";
    String ROD_DELIVERY_CLASS = "http://rod.eionet.europa.eu/schema.rdf#Delivery";

    String FULL_REPORT_CLASS = "http://reports.eea.europa.eu/reports_rdf#FullReport";
    String DCTYPE_DATASET_CLASS = "http://purl.org/dc/dcmitype/Dataset";

    String QAW_RESOURCE_CLASS = "http://qaw.eionet.europa.eu/schema.rdf#QawResource"; // deprecated
    String QA_REPORT_CLASS = "http://qaw.eionet.europa.eu/schema.rdf#QaReport"; // deprecated

    String RDF_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#RDF";

    String ANON_ID_PREFIX = "http://cr.eionet.europa.eu/anonymous/";

    String RDFS_LITERAL = "http://www.w3.org/2000/01/rdf-schema#Literal";
    String RDFS_CLASS = "http://www.w3.org/2000/01/rdf-schema#Class";

    String RDF_PROPERTY = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
    String RDF_SEQ = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq";

    String CR_FILE = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#File";
    String CR_TABLE_FILE = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#TableFile";
    String CR_FEEDBACK = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#Feedback";
    String CR_BOOKMARK = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#Bookmark";
    String CR_SPARQL_BOOKMARK = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#SparqlBookmark";
    String CR_FOLDER = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#Folder";
    String CR_USER_FOLDER = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#UserFolder";
    String CR_COMPILED_DATASET = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#CompiledDataset";
    String CR_REVIEW_FOLDER = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#ReviewFolder";
    String CR_BOOKMARKS_FILE = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#BookmarksFile";
    String CR_REGISTRATIONS_FILE = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#RegistrationsFile";
    String CR_HISTORY_FILE = "http://cr.eionet.europa.eu/ontologies/contreg.rdf#HistoryFile";

    String WGS_POINT = "http://www.w3.org/2003/01/geo/wgs84_pos#Point";
    String WGS_SPATIAL_THING = "http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing";

    String DUBLIN_CORE_SOURCE_URL = "http://purl.org/dc/elements/1.1/";
    String AMP_OUTPUT = "http://rdfdata.eionet.europa.eu/amp/ontology/Output";
    String AMPOLD_PRODUCT = "http://moonefish.eea.europa.eu:81/AMP-schema.rdf#Product"; // Phased out

    String FOAF_PERSON_CLASS = "http://xmlns.com/foaf/0.1/Person";

    String RSSNG_CHANNEL_CLASS = "http://cr.eionet.europa.eu/ontologies/news.rdf#Channel";
    String RSSNG_ANNOUNCEMENT_CLASS = "http://cr.eionet.europa.eu/ontologies/news.rdf#Announcement";
    String RSSNG_TEXTINPUT_CLASS = "http://cr.eionet.europa.eu/ontologies/news.rdf#TextInput";
    String RSSNG_IMAGE_CLASS = "http://cr.eionet.europa.eu/ontologies/news.rdf#Image";

    String DATACUBE_OBSERVATION = "http://purl.org/linked-data/cube#Observation";
    String DATACUBE_DATA_SET = "http://purl.org/linked-data/cube#DataSet";

    String DAS_INDICATOR_GROUP = "http://semantic.digital-agenda-data.eu/def/class/IndicatorGroup";
    String DAS_INDICATOR = "http://semantic.digital-agenda-data.eu/def/class/Indicator";
    String DAS_BREAKDOWN_GROUP = "http://semantic.digital-agenda-data.eu/def/class/BreakdownGroup";
    String DAS_BREAKDOWN = "http://semantic.digital-agenda-data.eu/def/class/Breakdown";
    String DAS_UNIT = "http://semantic.digital-agenda-data.eu/def/class/UnitMeasure";

    String SKOS_CONCEPT = "http://www.w3.org/2004/02/skos/core#Concept";
    String SKOS_CONCEPT_SCHEME = "http://www.w3.org/2004/02/skos/core#ConceptScheme";

    String DCAT_WEB_SERVICE = "http://www.w3.org/ns/dcat#WebService";
    String DCAT_DATASET = "http://www.w3.org/ns/dcat#Dataset";
    String DCAT_DISTRIBUTION = "http://www.w3.org/ns/dcat#Distribution";
    String DCAT_CATALOG = "http://www.w3.org/ns/dcat#Catalog";

    String ADMS_STATUS_COMPLETED = "http://purl.org/adms/status/Completed";

    String PUBLICATIONS_ACCESS_RIGHTS = "http://publications.europa.eu/resource/authority/access-right/PUBLIC";
    String PUBLICATIONS_ENGLISH = "http://publications.europa.eu/resource/authority/language/ENG";
    String PUBLICATIONS_CNECT = "http://publications.europa.eu/resource/authority/corporate-body/CNECT";
    String PUBLICATIONS_CNECT_F4 = "http://publications.europa.eu/resource/authority/corporate-body/F4";
    String PUBLICATIONS_SUBJECT = "http://purl.org/linked-data/sdmx/2009/subject#3.3.3";
    String PUBLICATIONS_THEME_GOVE = "http://publications.europa.eu/resource/authority/data-theme/GOVE";
    String PUBLICATIONS_THEME_SOCI = "http://publications.europa.eu/resource/authority/data-theme/SOCI";
    String PUBLICATIONS_THEME_TECH = "http://publications.europa.eu/resource/authority/data-theme/TECH";
    String PUBLICATIONS_RDF_TURTLE = "http://publications.europa.eu/resource/authority/file-type/RDF_TURTLE";
    String PUBLICATIONS_DOWNLOADABLE_FILE = "http://publications.europa.eu/resource/authority/distribution-type/DOWNLOADABLE_FILE";

    String DIGITAL_AGENDA_PAGE = "http://digital-agenda-data.eu/";
}
