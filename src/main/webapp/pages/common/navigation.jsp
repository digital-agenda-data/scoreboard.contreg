<%@ page import="eionet.cr.web.security.CRUser" %>

<%@ include file="/pages/common/taglibs.jsp"%>

<div id="leftcolumn" class="localnav">
    <ul>
        <li><a href="${pageContext.request.contextPath}/simpleSearch.action">Simple search</a></li>
        <li><a href="${pageContext.request.contextPath}/customSearch.action">Custom search</a></li>
        <li><a href="${pageContext.request.contextPath}/typeSearch.action">Type search</a></li>
        <li><a href="${pageContext.request.contextPath}/dataCubeDatasets">Browse datasets</a></li>
        <li><a href="${pageContext.request.contextPath}/observations">Browse observations</a></li>
        <li><a href="${pageContext.request.contextPath}/searchObservations">Search observations</a></li>
        <li><a href="${pageContext.request.contextPath}/codelists">Browse codelists</a></li>
        <li><a href="${pageContext.request.contextPath}/sparql">SPARQL endpoint</a></li>
        <li><a href="${pageContext.request.contextPath}/sources.action">Harvesting sources</a></li>
        <li><a href="${pageContext.request.contextPath}/harvestQueue.action">Harvest queue</a></li>
        <c:if test="${not empty sessionScope.crUser && sessionScope.crUser.administrator}">
            <li><a href="${pageContext.request.contextPath}/admin" title="Administrative activities">Admin actions</a></li>
        </c:if>
    </ul>
</div>
