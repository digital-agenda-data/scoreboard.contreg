<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Browse DataCube observations">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

    <h1>Browse DataCube observations</h1>

    <p>
        This page enables you to browse DataCube observations available in the system. You must specify at least one of the below filters.<br/>
        Please note that loading the available values of the filters is a time-consuming operation, therefore you must press "Reload filters"<br/>
        in order to refresh with potentially new values from the system.
    </p>

    <crfn:form beanclass="${actionBean.class.name}" method="get">
        <table>
            <c:forEach items="${actionBean.availFilters}" var="filter" varStatus="filtersLoopStatus">
                <tr>
                    <td>
                        <label for="filterSelect${filtersLoopStatus.index}" class="question">${filter.title}:</label>
                    </td>
                    <td>
                        <stripes:select id="filterSelect${filtersLoopStatus.index}" name="${filter.alias}" title="${filter.hint}" value="${param[filter.alias]}">
                            <stripes:option value="" label=" - select a value -"/>
                            <c:forEach items="${sessionScope[filter.sessionAttrName]}" var="uriLabelPair">
                                <stripes:option value="${uriLabelPair.left}" label="${uriLabelPair.right}" title="${uriLabelPair.right}"/>
                            </c:forEach>
                        </stripes:select>
                    </td>
                </tr>
            </c:forEach>
            <tr>
                <td colspan="2">
                    <stripes:submit name="search" value="Search"/>&nbsp;
                    <stripes:submit name="reloadFilters" value="Reload filters"/>
                </td>
            </tr>
        </table>
    </crfn:form>

    <c:if test="${actionBean.context.eventName eq 'search'}">
        <div style="margin-top:20px;width:100%">
		    <display:table name="${actionBean.observations}" id="observation" class="sortable" sort="external" requestURI="${actionBean.urlBinding}" style="width:100%">

		        <display:setProperty name="paging.banner.item_name" value="observation"/>
		        <display:setProperty name="paging.banner.items_name" value="observations"/>
		        <display:setProperty name="paging.banner.all_items_found" value='<div class="pagebanner">{0} {1} found.</div>'/>
		        <display:setProperty name="paging.banner.onepage" value=""/>

		        <display:column>
		            <stripes:link beanclass="${actionBean.factsheetActionBeanClass.name}" style="font-size:0.8em">
		                <img src="${pageContext.request.contextPath}/images/properties.gif"/>
		                <stripes:param name="uri" value="${observation.uri}"/>
		            </stripes:link>
		        </display:column>

		        <c:forEach items="${actionBean.availColumns}" var="column" varStatus="columnsLoopStatus">
		            <display:column title="${column.title}" sortable="${column.sortable}" sortProperty="${column.alias}" style="width:${column.width}">
                        <c:out value="${crfn:joinCollection(observation.predicates[column.predicate], ',', true, 3)}"/>
                    </display:column>
		        </c:forEach>

		    </display:table>
	    </div>
    </c:if>

</stripes:layout-component>
</stripes:layout-render>
