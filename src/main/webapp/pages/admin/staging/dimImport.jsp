<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="An RDF export">

    <stripes:layout-component name="contents">

            <%-- The page's heading --%>

            <h1>Import dimension metadata</h1>

            <%-- The form. --%>

            <div style="padding-top:10px">

                <stripes:form id="filesForm" method="post" beanclass="${actionBean['class'].name}">

                    <p>Query:</p>
                    <stripes:textarea id="txtQuery" name="query" cols="80" rows="15" value="${actionBean.query}"/>

                    <p>Dimension:</p>
                    <stripes:select name="dimension" value="${actionBean.dimension}">
                        <c:forEach items="${actionBean.dimensions}" var="dim">
                            <stripes:option value="${dim}" label="${dim}"/>
                        </c:forEach>
                    </stripes:select>

                    <stripes:submit name="execute" value="Execute"/>

                    <c:if test="${not empty actionBean.dbName}">
                        <stripes:hidden name="dbName"/>
                    </c:if>

                </stripes:form>
            </div>

    </stripes:layout-component>
</stripes:layout-render>
