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
        This page enables you to browse DataCube observations available in the system. It lists the observations matching the selected filters below.<br/>
        The provided values of every filter reflect the actual contents of the system, i.e. the values of the available observations.<br/>
        By default, the first value of every filter is selected. Changing a filter reloads all filters below it.
    </p>

    <crfn:form beanclass="${actionBean['class'].name}" method="get">
        <table>
            <c:forEach items="${actionBean.availFilters}" var="filter" varStatus="filtersLoopStatus">
                <tr>
                    <td>
                        <label for="filterSelect${filtersLoopStatus.index}" class="question">${filter.title}:</label>
                    </td>
                    <td>
                        <stripes:select id="filterSelect${filtersLoopStatus.index}" name="${filter.alias}" title="${filter.title}" value="${actionBean.selections[filter]}" onchange="this.form.submit();" style="max-width:600px">
                            <c:forEach items="${sessionScope[fn:replace(actionBean.filterValuesAttrNameTemplate, 'alias', filter.alias)]}" var="uriLabelPair">
                                <stripes:option value="${uriLabelPair.left}" label="${uriLabelPair.left eq uriLabelPair.right ? crfn:extractUriLabel(uriLabelPair.left) : uriLabelPair.right}" title="${uriLabelPair.left}"/>
                            </c:forEach>
                        </stripes:select>
                    </td>
                </tr>
            </c:forEach>
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
        <c:if test="${actionBean.observations == null || empty actionBean.observations.list}">
            <div class="tip-msg">
                <strong>Tip</strong>
                <p>
	                If you don't see any results then it might be that there is simply no data,<br/>
	                or you don't have sufficient privileges. <stripes:link title="Login" href="/login.action" event="login">Please try logging in</stripes:link>.
                </p>
            </div>
        </c:if>
    </c:if>

<%--
this.form.elements['applyFilter'].value=this.name
--%>
</stripes:layout-component>
</stripes:layout-render>
