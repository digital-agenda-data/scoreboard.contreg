<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<%@page import="net.sourceforge.stripes.action.ActionBean"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Post-harvest scripts">
    <stripes:layout-component name="contents">

        <h1>Post-harvest scripts</h1>

        <div style="margin-top:20px">
            <p>
                This page enables to search any type of post-harvest scripts by text that is present either in the script's title or content.<br/>
                All matching scripts regardless of target type or URL will be returned.<br/>
                If no text is supplied, no search is performed.
            </p>
        </div>

        <div style="margin-top:20px">
           <crfn:form id="searchForm" beanclass="${actionBean['class'].name}" method="get" focus="first">
               <stripes:label for="txtSearch" class="question">Search text:</stripes:label><br/>
               <stripes:text name="search" id="txtSearch" size="70"/>
               <button type="submit">Search</button>
           </crfn:form>
        </div>

        <c:if test="${not empty param.search}">
            <display:table name="${actionBean.scripts}" class="sortable" id="script" sort="list" pagesize="15" requestURI="${actionBean.urlBinding}" style="width:100%;margin-top:10px">

                <display:setProperty name="paging.banner.item_name" value="script"/>
                <display:setProperty name="paging.banner.items_name" value="scripts"/>
                <display:setProperty name="paging.banner.all_items_found" value='<div class="pagebanner">{0} {1} found.</div>'/>
                <display:setProperty name="paging.banner.onepage" value=""/>

                <display:column title='<span title="Title assigned to the script">Title</span>' style="width:40%" sortProperty="title" sortable="true">
                    <stripes:link href="/admin/postHarvestScript" title="View, edit and test this script">
                        <c:out value="${script.title}"/>
                        <stripes:param name="id" value="${script.id}"/>
                    </stripes:link>
                </display:column>
                <display:column title='<span title="Source or type to which the script is specific, empty if all-source script">Specific to</span>' style="width:60%">
                    <c:if test="${not empty script.targetType}">
                        <stripes:link href="/admin/postHarvestScripts" title="List scripts of this target ${fn:toLowerCase(script.targetType)}" style="font-size:0.85em">
                            <c:out value="${script.targetUrl}"/>
                            <stripes:param name="targetType" value="${script.targetType}"/>
                            <stripes:param name="targetUrl" value="${script.targetUrl}"/>
                        </stripes:link>
                    </c:if>
                    <c:if test="${empty script.targetType}">
                        <stripes:link href="/admin/postHarvestScripts" title="List all-source scripts" style="font-size:0.8em">
                            <c:out value="All sources"/>
                        </stripes:link>
                    </c:if>
                </display:column>

            </display:table>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
