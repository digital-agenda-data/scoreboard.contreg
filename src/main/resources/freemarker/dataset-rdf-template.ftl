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

# DATASET METADATA
<http://semantic.digital-agenda-data.eu/dataset/${dataset.identifier}>
    rdf:type qb:DataSet , dcat:Dataset ;
# DCAT Mandatory
    dcterms:title   "${dataset.title!dataset.identifier}"@en ;
    dcterms:description "${dataset.description!dataset.identifier}"@en ;
# QB recommended properties
    rdfs:label  "${dataset.title!dataset.identifier}"@en ;
    rdfs:comment    "${dataset.description!dataset.identifier}"@en ;
    dcterms:subject sdmx-subject:3.3.3 ;
        # TODO: here all concepts from dcat:theme could be listed

    dcterms:license  <${dataset.licenseUri!'http://ec.europa.eu/geninfo/legal_notices_en.htm#copyright'}> ;
    
# DCAT-AP recommended properties
    dcat:contactPoint   <http://publications.europa.eu/resource/authority/corporate-body/CNECT/F4> ;
    dcat:distribution   <http://semantic.digital-agenda-data.eu/dataset/${dataset.identifier}/distribution/download> ;
    dcat:distribution   <http://semantic.digital-agenda-data.eu/dataset/${dataset.identifier}/distribution/visualisation> ;
    dcat:keyword    
        "information-society"@en ,
        "digital agenda"@en ,
        "${dataset.keyword!'digital agenda'}"@en ;

    dcterms:publisher   <http://publications.europa.eu/resource/authority/corporate-body/CNECT> ;
    dcat:theme  
        <http://publications.europa.eu/resource/authority/data-theme/GOVE> ,
        <http://publications.europa.eu/resource/authority/data-theme/SOCI> ,
        <http://publications.europa.eu/resource/authority/data-theme/TECH> ;

# DCAT-AP optional properties / DCAT recommended properties
    dcterms:accessRights    <http://publications.europa.eu/resource/authority/access-right/PUBLIC>;
    dcat:landingPage    <http://digital-agenda-data.eu/> ;
    foaf:page   <http://digital-agenda-data.eu/> ;
    
    dcat:accrualPeriodicity  <${dataset.periodicityUri!'http://publications.europa.eu/resource/authority/frequency/ANNUAL_2'}> ;
    dcterms:language <http://publications.europa.eu/resource/authority/language/ENG> ;
    dcterms:spatial  <http://publications.europa.eu/resource/authority/country/EUR> ;
    dcterms:temporal    [ schema:startDate "2002-01-01"^^xsd:date ] ;
    dcterms:identifier  "${dataset.identifier}" ;
    dcterms:issued    "${dataset.issuedDateTimeStr!'2011-05-01T00:00:00Z'}"^^xsd:dateTime ;
    dcterms:modified    "${dataset.modifiedDateTimeStr}"^^xsd:dateTime ;

# QB
    qb:structure  <${dataset.dsdUri!'http://semantic.digital-agenda-data.eu/def/dsd/scoreboard'}> ;

# Linked data
    foaf:isPrimaryTopicOf   <http://semantic.digital-agenda-data.eu/API/dataset/${dataset.identifier}.rdf> ;

# ODP
    adms:status   <${dataset.statusUri!'http://purl.org/adms/status/UnderDevelopment'}> .

# RELATED ENTITIES

# License
<http://ec.europa.eu/geninfo/legal_notices_en.htm#copyright>    dcterms:title   "Europa Legal Notice" .

# DCAT Distribution
<http://semantic.digital-agenda-data.eu/dataset/${dataset.identifier}/distribution/download> 
    rdf:type    dcat:Distribution ;
    dcat:accessURL  <http://digital-agenda-data.eu/datasets/${dataset.identifier}#download> ;
    dcterms:description "Download instructions";
    dcterms:type    <http://publications.europa.eu/resource/authority/distribution-type/DOWNLOADABLE_FILE> ;
    dcterms:format  <http://publications.europa.eu/resource/authority/file-type/RDF_TURTLE> ;
    dcat:mediaType  "text/turtle" .

# DCAT Distribution
<http://semantic.digital-agenda-data.eu/dataset/${dataset.identifier}/distribution/visualisation>
    rdf:type    dcat:Distribution ;
    dcterms:type    <http://publications.europa.eu/resource/authority/distribution-type/VISUALIZATION> ;
    dcat:accessURL  <http://digital-agenda-data.eu/datasets/${dataset.identifier}> ;
    dcterms:description "Dataset visualisation".

# LD API page
<http://semantic.digital-agenda-data.eu/API/dataset/${dataset.identifier}.rdf>
    rdf:type    ld-api:Page ;
    foaf:primaryTopic   <http://semantic.digital-agenda-data.eu/dataset/${dataset.identifier}> ;
    ld-api:definition   <http://semantic.digital-agenda-data.eu/API/meta/dataset/_id.rdf> ;
    ld-api:extendedMetadataVersion  <http://semantic.digital-agenda-data.eu/API/dataset/${dataset.identifier}.rdf?_metadata=all> .

# Organization (contact point)
<http://publications.europa.eu/resource/authority/corporate-body/CNECT/F4>
    a vcard:Organization, foaf:Organization;
    rdfs:label "DG CONNECT - Digital Economy and Skills (Unit F.4)" ;
    foaf:mbox   "mailto:CNECT-F4@ec.europa.eu" ;
    foaf:name   "DG CONNECT - Digital Economy and Skills (Unit F.4)" ;
    foaf:workplaceHomepage  <https://ec.europa.eu/digital-single-market/en/dg-connect> .

