<?xml version="1.0" encoding="UTF-8"?>
<ecodp:manifest
    xmlns:ecodp="http://open-data.europa.eu/ontologies/ec-odp#"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    ecodp:package-id="Digital_Agenda_Scoreboard_${.now?string['yyyyMMdd_HHmmss']}"
    ecodp:publisher="http://publications.europa.eu/resource/authority/corporate-body/CNECT"
    xsi:schemaLocation="http://open-data.europa.eu/ontologies/protocol-v1.0/odp-protocol.xsd"
    ecodp:version="1.0"
    ecodp:priority="normal"
    ecodp:creation-date-time="${.now?iso_nz('GMT+01')}">

    <#list manifestEntries as manifestEntry>
    
        <ecodp:action ecodp:id="${manifestEntry.odpAction.idPrefix}${manifestEntry?index}" ecodp:object-ckan-name="${manifestEntry.dataset.identifier}" ecodp:object-type="dataset" ecodp:object-uri="${manifestEntry.dataset.uri}">
        <#if manifestEntry.odpAction == 'REMOVE'>
            <ecodp:remove/>
        <#else>
            <ecodp:${manifestEntry.odpAction.tagLocalName} ecodp:object-status="${manifestEntry.odpAction.objectStatus}" ecodp:package-path="/datasets/${manifestEntry.dataset.identifier}.rdf"/>
        </#if>
        </ecodp:action>
    </#list>
    
</ecodp:manifest>