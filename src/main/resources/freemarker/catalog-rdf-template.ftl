@prefix rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs:   <http://www.w3.org/2000/01/rdf-schema#> .
@prefix xsd:    <http://www.w3.org/2001/XMLSchema#> .
@prefix qb: <http://purl.org/linked-data/cube#> .
@prefix dcat:   <http://www.w3.org/ns/dcat#> .
@prefix dcterms:    <http://purl.org/dc/terms/> .
@prefix org:    <http://www.w3.org/ns/org#> .
@prefix foaf:   <http://xmlns.com/foaf/0.1/> .
@prefix odp:    <http://ec.europa.eu/open-data/ontologies/ec-odp#> .
@prefix adms:   <http://www.w3.org/ns/adms#> .
@prefix schema: <http://schema.org/> .
@prefix vcard:  <http://www.w3.org/2006/vcard/ns#> .
@prefix skos:   <http://www.w3.org/2004/02/skos/core#> .
@prefix sdmx:   <http://purl.org/linked-data/sdmx#> .
@prefix sdmx-attribute: <http://purl.org/linked-data/sdmx/2009/attribute#> .
@prefix sdmx-concept:   <http://purl.org/linked-data/sdmx/2009/concept#> .
@prefix sdmx-dimension: <http://purl.org/linked-data/sdmx/2009/dimension#> .
@prefix sdmx-measure:   <http://purl.org/linked-data/sdmx/2009/measure#> .
@prefix sdmx-subject:   <http://purl.org/linked-data/sdmx/2009/subject#> .

@prefix ld-api: <http://purl.org/linked-data/api/vocab#> .

@prefix dad-attribute:  <http://semantic.digital-agenda-data.eu/def/dsd/scoreboard/attribute/> .
@prefix dad-class:  <http://semantic.digital-agenda-data.eu/def/class/> .
@prefix dad-codelist:   <http://semantic.digital-agenda-data.eu/codelist/> .
@prefix dad-concept:    <http://semantic.digital-agenda-data.eu/def/concept/> .
@prefix dad-def:    <http://semantic.digital-agenda-data.eu/def/> .
@prefix dad-dimension:  <http://semantic.digital-agenda-data.eu/def/dsd/scoreboard/dimension/> .
@prefix dad-dsd:    <http://semantic.digital-agenda-data.eu/def/dsd/> .
@prefix dad-prop:   <http://semantic.digital-agenda-data.eu/def/property/> .

# CATALOG METADATA
<http://semantic.digital-agenda-data.eu/catalog/${catalog.identifier}>
    rdf:type dcat:Catalog ;
# DCAT Mandatory
    dcterms:title   "${catalog.title!catalog.identifier}"@en ;
    dcterms:description "${catalog.description!catalog.identifier}"@en ;
# QB recommended properties
    rdfs:label  "${catalog.title!catalog.identifier}"@en ;
    rdfs:comment    "${catalog.description!catalog.identifier}"@en ;
    dcterms:subject sdmx-subject:3.3.3 ;
        # TODO: here all concepts from dcat:theme could be listed

    dcterms:license  <${catalog.licenseUri!'http://ec.europa.eu/geninfo/legal_notices_en.htm#copyright'}> ;
    
# DCAT-AP recommended properties

    dcat:contactPoint   <http://publications.europa.eu/resource/authority/corporate-body/CNECT/F4> ;
    dcterms:publisher   <http://publications.europa.eu/resource/authority/corporate-body/CNECT> ;
    dcat:theme  
        <http://publications.europa.eu/resource/authority/data-theme/GOVE> ,
        <http://publications.europa.eu/resource/authority/data-theme/SOCI> ,
        <http://publications.europa.eu/resource/authority/data-theme/TECH> ;

# DCAT-AP optional properties / DCAT recommended properties
    dcterms:accessRights    <http://publications.europa.eu/resource/authority/access-right/PUBLIC>;
    foaf:homepage  <${catalog.homepageUri!'http://digital-agenda-data.eu/'}> ;
    dcat:landingPage  <${catalog.homepageUri!'http://digital-agenda-data.eu/'}> ;
    
    dcterms:language <http://publications.europa.eu/resource/authority/language/ENG> ;
    dcterms:spatial  <http://publications.europa.eu/resource/authority/country/EUR> ;
    dcterms:identifier  "${catalog.identifier}" ;
    dcterms:issued    "${catalog.issuedDateTimeStr!'2011-05-01T00:00:00Z'}"^^xsd:dateTime ;
    dcterms:modified    "${catalog.modifiedDateTimeStr}"^^xsd:dateTime .