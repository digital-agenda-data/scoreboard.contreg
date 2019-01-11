<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Custom search">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
        ( function($) {
            $(document).ready(function() {
                // Open delete bookmarked queries dialog
                $("#queryDialogLink").click(function() {
                    $('#queryDialog').dialog('open');
                    return false;
                });

                // Dialog setup
                $('#queryDialog').dialog({
                    autoOpen: false,
                    width: 500
                });

                // Close dialog
                $("#closeQueryDialog").click(function() {
                    $('#queryDialog').dialog("close");
                    return true;
                });
            });

        } ) ( jQuery );

        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <c:if test="${not empty actionBean.queryString}">
        <ul id="dropdown-operations">
            <li><a href="#">Operations</a>
                <ul>
                    <li>
                        <a href="#" id="queryDialogLink">Search query</a>
                    </li>
                </ul>
            </li>
        </ul>
        </c:if>

        <h1>Custom search</h1>
        <p>
            Add a search filter and give it a value. For some filters the system provides a list of existing values.
            Text inputs of such filters have a special icon to the right of them which opens the list. You can remove
            added filters by using the removal icons that are displayed to the left of them.
        </p>

        <c:choose>
            <c:when test="${actionBean.availableFilters!=null && fn:length(actionBean.availableFilters)>0}">

                <div id="filterSelectionArea" style="margin-top:20px">

                    <crfn:form name="customSearchForm" action="/customSearch.action" method="get" id="customSearchForm" acceptcharset="UTF-8">

                        <c:if test="${fn:length(actionBean.selectedFilters)<fn:length(actionBean.availableFilters)}">
                            <stripes:select name="addedFilter" id="filterSelect" onchange="this.form.submit();" title="Add a filter.">
                                <stripes:option value="" label="-- add a filter --"/>
                                <c:forEach var="availableFilter" items="${actionBean.availableFilters}">
                                    <c:if test="${actionBean.selectedFilters[availableFilter.key]==null}">
                                        <stripes:option value="${availableFilter.key}" label="${availableFilter.value.title}" title="${availableFilter.value.uri}"/>
                                    </c:if>
                                </c:forEach>
                            </stripes:select>&nbsp;
                            <noscript>
                                <stripes:submit name="addFilter" value="Add filter"/>
                            </noscript>
                        </c:if>

                        <c:if test="${actionBean.selectedFilters!=null && fn:length(actionBean.selectedFilters)>0}">
                            <table style="margin-top:20px;margin-bottom:20px">
                                <c:forEach var="availableFilter" items="${actionBean.availableFilters}">
                                    <c:if test="${actionBean.selectedFilters[availableFilter.key]!=null}">
                                        <tr>
                                            <td style="padding-right:12px">
                                                <input type="image" name="removeFilter_${availableFilter.key}" src="${pageContext.request.contextPath}/images/delete_small.gif" title="Remove filter" alt="Remove filter"/>
                                            </td>
                                            <td style="text-align:right"><label for="inp_value_${availableFilter.key}"><c:out value="${availableFilter.value.title}"/>:</label></td>
                                            <td>
                                                <c:if test="${!actionBean.showPicklist || actionBean.picklistFilter!=availableFilter.key || actionBean.picklist==null || fn:length(actionBean.picklist)==0}">
                                                    <div style="float: left">
                                                        <c:choose>
                                                            <c:when test="${actionBean.showPicklist && actionBean.picklistFilter==availableFilter.key && (actionBean.picklist==null || fn:length(actionBean.picklist)==0)}">
                                                                <input id="inp_value_${availableFilter.key}" type="text" name="value_${availableFilter.key}" size="30" placeholder="No picklist found!"/>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <input id="inp_value_${availableFilter.key}" type="text" name="value_${availableFilter.key}"
                                                                       value="${fn:escapeXml(actionBean.selectedFilters[availableFilter.key])}" size="30"/>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </div>
                                                </c:if>
                                                <c:if test="${availableFilter.value.provideValues}">
                                                    <c:if test="${actionBean.showPicklist && actionBean.picklistFilter==availableFilter.key && actionBean.picklist!=null && fn:length(actionBean.picklist)>0}">
                                                        <div style="float: left">
                                                            <select id="inp_value_${availableFilter.key}" name="value_${availableFilter.key}" style="max-width:100%">
                                                                <option value="" selected="selected">- select a value -</option>
                                                                <c:if test="${actionBean.picklist!=null}">
                                                                    <c:forEach var="picklistItem" items="${actionBean.picklist}">
                                                                        <option value="${fn:escapeXml(crfn:addQuotesIfWhitespaceInside(picklistItem.left))}" title="${fn:escapeXml(picklistItem.left)}" style="max-width:100%"><c:out value="${picklistItem.right}"/></option>
                                                                    </c:forEach>
                                                                </c:if>
                                                            </select>
                                                        </div>
                                                    </c:if>
                                                    <c:if test="${!actionBean.showPicklist || actionBean.picklistFilter!=availableFilter.key}">
                                                        <div style="float: right">
                                                            <input type="image" name="showPicklist_${availableFilter.key}" src="${pageContext.request.contextPath}/images/list.gif" title="Get a picklist of existing values..." alt="Get a picklist of existing values..." style="position:absolute;padding-top:1px"/>
                                                        </div>
                                                    </c:if>
                                                </c:if>
                                            </td>
                                        </tr>
                                    </c:if>
                                </c:forEach>
                            </table>
                            <stripes:submit name="search" value="Search"/>
                        </c:if>

                        <c:if test="${(actionBean.resultList!=null && fn:length(actionBean.resultList)>0) || not empty param.search}">
                            <stripes:layout-render name="/pages/common/subjectsResultList.jsp" tableClass="sortable"/>
                        </c:if>

                    </crfn:form>
                </div>

            </c:when>
            <c:otherwise>
                No available filters found!
            </c:otherwise>
        </c:choose>

        <div id="queryDialog" title="Search query">
            <c:if test="${not empty actionBean.queryString}">
                <pre><c:out value="${actionBean.queryString}" /></pre>
                <crfn:form action="/sparql" method="get">
                    <stripes:hidden name="query" value="${actionBean.queryString}" />
                    <br />
                    <br />
                    <stripes:submit name="noEvent" value="Edit SPARQL query" />
                </crfn:form>
            </c:if>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
