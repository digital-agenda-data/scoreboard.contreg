<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="SPARQL endpoint harvest query">

    <stripes:layout-component name="contents">

        <%-- The page's heading and explanation text. --%>

        <h1>Remote resource properties</h1>

        <div style="margin-top:20px">
            <p>
                The below table lists the properties of <a href="${fn:escapeXml(actionBean.url)}"><c:out value="${actionBean.url}"/></a><br/>
                at a remote SPARQL endpoint located at <a href="${fn:escapeXml(actionBean.endpoint)}"><c:out value="${actionBean.endpoint}"/></a>.
            </p>
        </div>

        <%-- The section that displays the query's properties, editable or not, depending on the event. --%>

        <c:if test="${not empty actionBean.queryResult}">
            <div style="margin-top:20px">
                <display:table name="${actionBean.queryResult}" id="statement" sort="page" class="datatable" style="width:80%">
                    <display:column title="Property">
                        <stripes:link beanclass="${actionBean['class'].name}">
                            <c:out value="${statement.predicate}"/>
                            <stripes:param name="url" value="${statement.predicate}"/>
                            <stripes:param name="endpoint" value="${actionBean.endpoint}"/>
                        </stripes:link>
                    </display:column>
                    <display:column title="Value">
                        <c:choose>
                            <c:when test="${fn:contains(statement.object['class'].name, 'Literal')}">
                                <c:out value="${statement.object.label}"/>
                            </c:when>
                            <c:otherwise>
                                <stripes:link beanclass="${actionBean['class'].name}">
                                    <c:out value="${statement.object}"/>
                                    <stripes:param name="url" value="${statement.object}"/>
                                    <stripes:param name="endpoint" value="${actionBean.endpoint}"/>
                                </stripes:link>
                            </c:otherwise>
                        </c:choose>
                    </display:column>
                </display:table>
            </div>
        </c:if>
        <c:if test="${empty actionBean.queryResult}">
            <div class="system-msg" style="margin-top:20px">
                <p>No results found!</p>
            </div>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
