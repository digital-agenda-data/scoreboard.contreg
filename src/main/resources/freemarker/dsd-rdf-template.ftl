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

# DSD METADATA

dad-dsd:${dsd.identifier}
    rdf:type    sdmx:DataStructureDefinition, qb:DataStructureDefinition .
dad-dsd:${dsd.identifier}
    qb:component
        dad-attribute:flag ,
        dad-attribute:note ,
        dad-dimension:indicator ,
        dad-dimension:breakdown ,
        dad-dimension:ref-area ,
        dad-dimension:time-period ,
        dad-dimension:unit-measure ,
        <http://semantic.digital-agenda-data.eu/def/dsd/scoreboard/measure/obsValue> ;
    sdmx:primaryMeasure sdmx-measure:obsValue .

# DSD Component Specifications

dad-dimension:indicator rdf:type    qb:ComponentSpecification ;
    qb:dimension    dad-prop:indicator ;
    qb:order    1 .
dad-dimension:breakdown rdf:type    qb:ComponentSpecification ;
    qb:dimension    dad-prop:breakdown ;
    qb:order    2 .
dad-dimension:unit-measure  rdf:type    qb:ComponentSpecification ;
    qb:dimension    dad-prop:unit-measure ;
    qb:order    3 .
dad-dimension:ref-area  rdf:type    qb:ComponentSpecification ;
    qb:dimension    dad-prop:ref-area ;
    qb:order    4 .
dad-dimension:time-period   rdf:type    qb:ComponentSpecification ;
    qb:dimension    dad-prop:time-period ;
    qb:order    5 .

dad-attribute:flag  rdf:type    qb:ComponentSpecification ;
    qb:attribute    dad-prop:flag .
dad-attribute:note  rdf:type    qb:ComponentSpecification ;
    qb:attribute    dad-prop:note .