<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>


<%@page import="net.sourceforge.stripes.action.ActionBean"%><stripes:layout-render name="/pages/common/template.jsp" pageTitle="Administration pages">

    <stripes:layout-component name="contents">

    <c:choose>
        <c:when test="${actionBean.adminLoggedIn}">
            <h1>Administration pages</h1>
            <ul style="padding-top:10px">
                <li><stripes:link href="/admin/harvestedurl">Harvested Urls</stripes:link></li>
                <li><stripes:link href="/admin/harveststats">Last 100 Harvests Statistics</stripes:link></li>
                <li><stripes:link href="/admin/nhus">Next Harvest Urgency Score </stripes:link></li>
            </ul>
            <ul style="padding-top:10px">
                <li><stripes:link href="/admin/bulkharvest">Bulk Add/Delete Sources</stripes:link></li>
                <li><stripes:link href="/admin/postHarvestScripts">Post-harvest scripts</stripes:link></li>
                <li><stripes:link href="/admin/registerUrl.action">Register URL</stripes:link></li>
                <li><stripes:link href="/admin/endpointQueries.action">SPARQL endpoint harvest queries</stripes:link></li>
            </ul>
            <ul style="padding-top:10px">
                <li><stripes:link href="/admin/stagingDbs.action">Staging databases</stripes:link></li>
                <li><stripes:link href="/admin/xlwrapUpload.action">Spreadsheet upload</stripes:link></li>
                <li><stripes:link href="/admin/odpPackaging.action">ODP datasets packaging</stripes:link></li>
                <li><stripes:link href="/admin/obsDelete.action">Delete observations of specified indicators</stripes:link></li>
            </ul>
            <ul style="padding-top:10px">
                <li><stripes:link href="/admin/migrationPackages.action">Prepared migration packages</stripes:link></li>
            </ul>
        </c:when>
        <c:otherwise>
            <div class="error-msg">Access not allowed!</div>
        </c:otherwise>
    </c:choose>
    </stripes:layout-component>

</stripes:layout-render>
