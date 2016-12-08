<?xml version="1.0" encoding="UTF-8"?>
<rdf:RDF
        xmlns="http://open-data.europa.eu/ontologies/ec-odp#"
        xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
        xmlns:owl="http://www.w3.org/2002/07/owl#"
        xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:dct="http://purl.org/dc/terms/
        xmlns:dcam="http://purl.org/dc/dcam/"
        xmlns:dcat="http://www.w3.org/ns/dcat#"
        xmlns:ecodp="http://open-data.europa.eu/ontologies/ec-odp#"
        xmlns:foaf="http://xmlns.com/foaf/0.1/"
        xmlns:skos="http://www.w3.org/2004/02/skos/core#"
        xmlns:skos-xl="http://www.w3.org/2008/05/skos-xl#">
        
    <dcat:Dataset rdf:about="${dataset.uri}">
    <dct:title xml:lang="en">${dataset.title!dataset.identifier}</dct:title>
    <dct:alternative xml:lang="en">${dataset.title!dataset.identifier}</dct:alternative>
    <dct:description xml:lang="en">${dataset.description!dataset.identifier}</dct:description>
    <dct:identifier xml:lang="en">${dataset.identifier}</dct:identifier>
    <ecodp:interoperabilityLevel>
      <skos:Concept rdf:about="http://open-data.europa.eu/kos/interoperability-level/Legal"/>
    </ecodp:interoperabilityLevel>
    <ecodp:datasetType>
      <skos:Concept rdf:about="http://open-data.europa.eu/kos/dataset-type/Statistical"/>
    </ecodp:datasetType>
    <ecodp:isDocumentedBy rdf:parseType="Resource">
      <ecodp:documentationType>
        <skos:Concept rdf:about="http://open-data.europa.eu/kos/documentation-type/RelatedDocumentation"/>
      </ecodp:documentationType>
      <ecodp:accessURL rdf:datatype="http://www.w3.org/2001/XMLSchema#anyURI">https://digital-agenda-data.eu/datasets/${dataset.identifier!?lower_case}/visualizations</ecodp:accessURL>
      <dct:title xml:lang="en">Dataset visualizations.</dct:title>
      <dct:description xml:lang="en">Dynamically generated visualizations (i.e. charts, diagrams) of the dataset contents.</dct:description>
    </ecodp:isDocumentedBy>
    <ecodp:isDocumentedBy rdf:parseType="Resource">
      <ecodp:documentationType>
        <skos:Concept rdf:about="http://open-data.europa.eu/kos/documentation-type/MainDocumentation"/>
      </ecodp:documentationType>
      <ecodp:accessURL rdf:datatype="http://www.w3.org/2001/XMLSchema#anyURI">http://digital-agenda-data.eu/datasets/${dataset.identifier!?lower_case}</ecodp:accessURL>
      <dct:title xml:lang="en">Dataset home page.</dct:title>
      <dct:description xml:lang="en">Main information about the dataset metadata, structure, links to downloads, etc.</dct:description>
    </ecodp:isDocumentedBy>
    <ecodp:isDocumentedBy rdf:parseType="Resource">
      <ecodp:documentationType>
        <skos:Concept rdf:about="http://open-data.europa.eu/kos/documentation-type/RelatedDocumentation"/>
      </ecodp:documentationType>
      <ecodp:accessURL rdf:datatype="http://www.w3.org/2001/XMLSchema#anyURI">http://digital-agenda-data.eu/datasets/${dataset.identifier!?lower_case}/@@structure</ecodp:accessURL>
      <dct:title xml:lang="en">Data Structrue Definition of the dataset.</dct:title>
      <dct:description xml:lang="en">RDF/XML formatted Data Structrue Definition of the dataset.</dct:description>
    </ecodp:isDocumentedBy>
    <ecodp:isDocumentedBy rdf:parseType="Resource">
      <ecodp:documentationType>
        <skos:Concept rdf:about="http://open-data.europa.eu/kos/documentation-type/RelatedDocumentation"/>
      </ecodp:documentationType>
      <ecodp:accessURL rdf:datatype="http://www.w3.org/2001/XMLSchema#anyURI">http://digital-agenda-data.eu/datasets/${dataset.identifier!?lower_case}/@@codelists</ecodp:accessURL>
      <dct:title xml:lang="en">Metadata codelists used in the dataset.</dct:title>
      <dct:description xml:lang="en">RDF/XML formatted codelists for metadata used in the dataset.</dct:description>
    </ecodp:isDocumentedBy>
    <dcat:distribution rdf:parseType="Resource">
      <dct:title xml:lang="en">All available observations of the datset, in CSV format.</dct:title>
      <dcat:accessURL rdf:datatype="http://www.w3.org/2001/XMLSchema#anyURI">http://digital-agenda-data.eu/sparql?query=PREFIX+sdmx-measure%3A+%3Chttp%3A%2F%2Fpurl.org%2Flinked-data%2Fsdmx%2F2009%2Fmeasure%23%3E%0APREFIX+dad-prop%3A+%3Chttp%3A%2F%2Fsemantic.digital-agenda-data.eu%2Fdef%2Fproperty%2F%3E%0APREFIX+cube%3A+%3Chttp%3A%2F%2Fpurl.org%2Flinked-data%2Fcube%23%3E%0A%0ASELECT%0A++bif%3Asubseq(str(%3Ftime)%2C+bif%3Astrrchr(bif%3Areplace(str(%3Ftime)%2C+%27%2F%27%2C+%27%23%27)%2C+%27%23%27)+%2B+1)+as+%3Ftime_period%0A++bif%3Asubseq(str(%3FrefArea)%2C+bif%3Astrrchr(bif%3Areplace(str(%3FrefArea)%2C+%27%2F%27%2C+%27%23%27)%2C+%27%23%27)+%2B+1)+as+%3Fref_area%0A++bif%3Asubseq(str(%3Findic)%2C+bif%3Astrrchr(bif%3Areplace(str(%3Findic)%2C+%27%2F%27%2C+%27%23%27)%2C+%27%23%27)+%2B+1)+as+%3Findicator%0A++bif%3Asubseq(str(%3Fbrkdwn)%2C+bif%3Astrrchr(bif%3Areplace(str(%3Fbrkdwn)%2C+%27%2F%27%2C+%27%23%27)%2C+%27%23%27)+%2B+1)+as+%3Fbreakdown%0A++bif%3Asubseq(str(%3Funit)%2C+bif%3Astrrchr(bif%3Areplace(str(%3Funit)%2C+%27%2F%27%2C+%27%23%27)%2C+%27%23%27)+%2B+1)+as+%3Funit_measure%0A++str(%3Fval)+as+%3Fvalue%0AWHERE+%7B%0A++%3Fs+a+cube%3AObservation+.%0A++%3Fs+dad-prop%3Atime-period+%3Ftime+.%0A++%3Fs+dad-prop%3Aref-area+%3FrefArea+.%0A++%3Fs+dad-prop%3Abreakdown+%3Fbrkdwn+.%0A++%3Fs+dad-prop%3Aunit-measure+%3Funit+.%0A++%3Fs+sdmx-measure%3AobsValue+%3Fval+.%0A++%3Fs+cube%3AdataSet+%3C${dataset.uri}%3E%0A++optional+%7B%3Fs+dad-prop%3Aindicator+%3Findic+.%7D%0A%7D%0Aorder+by+%3Ftime_period+%3Fref_area+%3Findicator+%3Fbreakdown+%3Funit_measure&amp;format=text%2Fcsv</dcat:accessURL>
      <rdf:type rdf:resource="http://www.w3.org/TR/vocab-dcat#Download"/>
      <ecodp:distributionFormat>text/csv</ecodp:distributionFormat>
      <dct:description xml:lang="en">All dataset observations in CSV format. A simple query that returns table-file having a flat structure, with one row for each statistical observation and one column for each dimension or attribute.</dct:description>
    </dcat:distribution>
    <dcat:distribution rdf:parseType="Resource">
      <dct:title xml:lang="en">All available observations of the datset, in RDF/XML format.</dct:title>
      <dcat:accessURL rdf:datatype="http://www.w3.org/2001/XMLSchema#anyURI">http://digital-agenda-data.eu/sparql?query=PREFIX+sdmx-measure%3A+%3Chttp%3A%2F%2Fpurl.org%2Flinked-data%2Fsdmx%2F2009%2Fmeasure%23%3E%0APREFIX+dad-prop%3A+%3Chttp%3A%2F%2Fsemantic.digital-agenda-data.eu%2Fdef%2Fproperty%2F%3E%0APREFIX+cube%3A+%3Chttp%3A%2F%2Fpurl.org%2Flinked-data%2Fcube%23%3E%0A%0ASELECT%0A++bif%3Asubseq(str(%3Ftime)%2C+bif%3Astrrchr(bif%3Areplace(str(%3Ftime)%2C+%27%2F%27%2C+%27%23%27)%2C+%27%23%27)+%2B+1)+as+%3Ftime_period%0A++bif%3Asubseq(str(%3FrefArea)%2C+bif%3Astrrchr(bif%3Areplace(str(%3FrefArea)%2C+%27%2F%27%2C+%27%23%27)%2C+%27%23%27)+%2B+1)+as+%3Fref_area%0A++bif%3Asubseq(str(%3Findic)%2C+bif%3Astrrchr(bif%3Areplace(str(%3Findic)%2C+%27%2F%27%2C+%27%23%27)%2C+%27%23%27)+%2B+1)+as+%3Findicator%0A++bif%3Asubseq(str(%3Fbrkdwn)%2C+bif%3Astrrchr(bif%3Areplace(str(%3Fbrkdwn)%2C+%27%2F%27%2C+%27%23%27)%2C+%27%23%27)+%2B+1)+as+%3Fbreakdown%0A++bif%3Asubseq(str(%3Funit)%2C+bif%3Astrrchr(bif%3Areplace(str(%3Funit)%2C+%27%2F%27%2C+%27%23%27)%2C+%27%23%27)+%2B+1)+as+%3Funit_measure%0A++str(%3Fval)+as+%3Fvalue%0AWHERE+%7B%0A++%3Fs+a+cube%3AObservation+.%0A++%3Fs+dad-prop%3Atime-period+%3Ftime+.%0A++%3Fs+dad-prop%3Aref-area+%3FrefArea+.%0A++%3Fs+dad-prop%3Abreakdown+%3Fbrkdwn+.%0A++%3Fs+dad-prop%3Aunit-measure+%3Funit+.%0A++%3Fs+sdmx-measure%3AobsValue+%3Fval+.%0A++%3Fs+cube%3AdataSet+%3C${dataset.uri}%3E%0A++optional+%7B%3Fs+dad-prop%3Aindicator+%3Findic+.%7D%0A%7D%0Aorder+by+%3Ftime_period+%3Fref_area+%3Findicator+%3Fbreakdown+%3Funit_measure&amp;format=application%2Frdf%2Bxml</dcat:accessURL>
      <rdf:type rdf:resource="http://www.w3.org/TR/vocab-dcat#Download"/>
      <ecodp:distributionFormat>application/rdf+xml</ecodp:distributionFormat>
      <dct:description xml:lang="en">All dataset observations in RDF/XML format. A simple query that returns observations as triples in RDF/XML format.</dct:description>
    </dcat:distribution>
    <dcat:distribution rdf:parseType="Resource">
      <dct:title xml:lang="en">SPARQL endpoint of the entire dataset.</dct:title>
      <dcat:accessURL rdf:datatype="http://www.w3.org/2001/XMLSchema#anyURI">http://digital-agenda-data.eu/data/sparql</dcat:accessURL>
      <rdf:type rdf:resource="http://www.w3.org/TR/vocab-dcat#WebService"/>
      <ecodp:distributionFormat>webservice/sparql</ecodp:distributionFormat>
      <dct:description xml:lang="en">SPARQL endpoint for querying and creating applications based on the most recent data.</dct:description>
    </dcat:distribution>
    <dcat:distribution rdf:parseType="Resource">
      <dct:title xml:lang="en">CSV download of the entire dataset.</dct:title>
      <dcat:accessURL rdf:datatype="http://www.w3.org/2001/XMLSchema#anyURI">http://digital-agenda-data.eu/download/${dataset.identifier}.csv.zip</dcat:accessURL>
      <rdf:type rdf:resource="http://www.w3.org/TR/vocab-dcat#Download"/>
      <ecodp:distributionFormat>text/csv</ecodp:distributionFormat>
      <dct:description xml:lang="en">Zipped and CSV-formatted entire dataset: ${dataset.identifier}</dct:description>
      <dct:modified rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime">2016-12-06T09:32:03Z</dct:modified>
    </dcat:distribution>
    <dcat:distribution rdf:parseType="Resource">
      <dct:title xml:lang="en">N3/Turtle download of the entire dataset.</dct:title>
      <dcat:accessURL rdf:datatype="http://www.w3.org/2001/XMLSchema#anyURI">http://digital-agenda-data.eu/download/${dataset.identifier}.ttl.zip</dcat:accessURL>
      <rdf:type rdf:resource="http://www.w3.org/TR/vocab-dcat#Download"/>
      <ecodp:distributionFormat>text/n3</ecodp:distributionFormat>
      <dct:description xml:lang="en">Zipped and N3-formatted entire dataset: ${dataset.identifier}</dct:description>
    </dcat:distribution>
    <#list dataset.spatialUris as spatialUri>
        <dct:spatial>
            <skos:Concept rdf:about="${spatialUri}"/>
        </dct:spatial>
    </#list>
    <dct:publisher>
      <skos:Concept rdf:about="http://publications.europa.eu/resource/authority/corporate-body/CNECT"/>
    </dct:publisher>
    <ecodp:contactPoint>
      <foaf:agent rdf:about="http://publications.europa.eu/resource/authority/corporate-body/CNECT/C4">
        <foaf:mbox rdf:resource="mailto:CNECT-F4@ec.europa.eu"/>
        <foaf:workplaceHomepage rdf:resource="http://digital-agenda-data.eu/"/>
        <foaf:name xml:lang="en">DG CONNECT Unit F4 Knowledge Base</foaf:name>
      </foaf:agent>
    </ecodp:contactPoint>
    <dct:issued rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime">${dataset.issued!dataset.modified}</dct:issued>
    <dct:modified rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime">${dataset.modified!}</dct:modified>
    <dct:license>
      <skos:Concept rdf:about="http://open-data.europa.eu/kos/licence/EuropeanCommission"/>
    </dct:license>
    <ecodp:datasetStatus>
      <skos:Concept rdf:about="http://open-data.europa.eu/kos/dataset-status/Completed"/>
    </ecodp:datasetStatus>
    <dct:language>
      <skos:Concept rdf:about="http://publications.europa.eu/resource/authority/language/ENG"/>
    </dct:language>
    <ecodp:accrualPeriodicity rdf:resource="http://open-data.europa.eu/kos/accrual-periodicity/other"/>
    <dct:temporal rdf:parseType="Resource">
      <ecodp:periodStart>${dataset.periodStart!'2004'}</ecodp:periodStart>
    </dct:temporal>
  </dcat:Dataset>
</rdf:RDF>
