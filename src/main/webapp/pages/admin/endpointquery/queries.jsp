<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="SPARQL endpoint harvest queries">

    <stripes:layout-component name="contents">

        <%-- Drop-down operations --%>

        <ul id="dropdown-operations">
            <li><a href="#">Operations</a>
                <ul>
                    <c:if test="${not empty actionBean.endpointUrl}">
                        <li>
                            <stripes:link beanclass="${actionBean.endpointQueryActionBeanClass.name}">
                               <c:out value="Add query for this endpoint"/>
                               <stripes:param name="query.endpointUrl" value="${actionBean.endpointUrl}"/>
                            </stripes:link>
                        </li>
                        <li>
	                        <stripes:link beanclass="${actionBean.harvestSourceActionBeanClass.name}">
	                            <c:out value="Source details"/>
	                            <stripes:param name="harvestSource.url" value="${actionBean.endpointUrl}"/>
	                        </stripes:link>
	                    </li>
                    </c:if>
                    <li>
                        <stripes:link beanclass="${actionBean.harvestSourceActionBeanClass.name}" event="add">
                            <c:out value="Add new endpoint"/>
                            <stripes:param name="harvestSource.sparqlEndpoint" value="true"/>
                        </stripes:link>
                    </li>
                </ul>
            </li>
        </ul>

        <%-- The page's heading --%>

        <h1>SPARQL endpoint harvest queries</h1>

        <div style="margin-top:20px">
            <p>
                This is a list of available SPARQL endpoints.<br/>
                Select an endpoint to see its harvest queries, and use operations menu to add a new harvest query to the selected endpoint.
            </p>
        </div>

        <%-- The section that displays available endpoints and selected endpoint's queries. --%>

        <c:if test="${not empty actionBean.endpoints}">

            <%-- The form and selection box of endpoints. --%>

            <div style="margin-top:2em;width:100%">
                <crfn:form id="endpointsForm" beanclass="${actionBean['class'].name}" method="get">
                    <label for="endpointSelect" class="question">Select a SPARQL endpoint:</label><br/>
                    <stripes:select id="endpointSelect" name="endpointUrl" value="${actionBean.endpointUrl}" onchange="this.form.submit();" size="10" style="width:70%">
                        <c:forEach items="${actionBean.endpoints}" var="endpoint">
                            <stripes:option value="${endpoint}" label="${endpoint}"/>
                        </c:forEach>
                    </stripes:select>
                </crfn:form>
            </div>

            <%-- The list and form of the selected endpoint's queries. --%>

            <c:if test="${not empty actionBean.endpointUrl}">
                <c:if test="${not empty actionBean.queries}">
                    <div style="margin-top:20px">
                        <crfn:form id="queriesForm" beanclass="${actionBean['class'].name}" method="post">

                            <display:table name="${actionBean.queries}" class="datatable" id="query" sort="list" pagesize="20" requestURI="${actionBean.urlBinding}" style="width:80%">

                                <display:setProperty name="paging.banner.item_name" value="query"/>
                                <display:setProperty name="paging.banner.items_name" value="queries"/>
                                <display:setProperty name="paging.banner.all_items_found" value='<div class="pagebanner">{0} {1} found.</div>'/>
                                <display:setProperty name="paging.banner.onepage" value=""/>

                                <display:column style="width:2em;text-align:center">
                                    <input type="checkbox" name="selectedIds" value="${query.id}" title="Select this query"/>
                                </display:column>
                                <display:column title='<span title="Title assigned to the query">Title</span>'>
                                    <stripes:link beanclass="${actionBean.endpointQueryActionBeanClass.name}" title="View, edit and test this query">
                                        <c:out value="${query.title}"/>
                                        <stripes:param name="query.id" value="${query.id}"/>
                                    </stripes:link>
                                </display:column>
                                <display:column style="width:3em;text-align:center" title='<span title="Indicates whether the query is currently turned off">Active</span>'>
                                    <c:out value="${query.active ? 'Yes' : 'No'}"/>
                                </display:column>
                                <display:column style="width:12em;text-align:center" title='<span title="Last modification time of the query">Last modified</span>'>
                                    <c:out value="${query.lastModified}"/>
                                </display:column>

                            </display:table>

                            <div>
                                <stripes:submit name="delete" value="Delete" title="Delete selected queries"/>
                                <stripes:submit name="activateDeactivate" value="Activate/deactivate" title="Activate/deactivate (i.e. turn on/off) selected queries"/>
                                <c:if test="${fn:length(actionBean.queries) > 1}">
                                    <stripes:submit name="moveUp" value="Move up" title="Move selected queries up"/>
                                    <stripes:submit name="moveDown" value="Move down" title="Move selected queries down"/>
                                </c:if>
                                <input type="button" onclick="toggleSelectAll('queriesForm');return false" value="Select all" name="selectAll"/>
                            </div>

                            <c:if test="${not empty actionBean.endpointUrl}">
                                <fieldset style="display:none">
                                    <stripes:hidden name="endpointUrl"/>
                                </fieldset>
                            </c:if>

                        </crfn:form>
                    </div>
                </c:if>
            </c:if>
            <c:if test="${empty actionBean.queries}">
                <div class="system-msg">
                    <p>Found no queries defined for this SPARQL endpoint! Use operations menu to add one.</p>
                </div>
            </c:if>
        </c:if>

        <%-- The message when no endpoints available yet. --%>

        <c:if test="${empty actionBean.endpoints}">
            <div style="margin-top:3em" class="system-msg">
                <p>Found no SPARQL endpoints defined yet! Use operations menu to add one.</p>
            </div>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
