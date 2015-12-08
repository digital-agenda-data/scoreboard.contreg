<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="RDF exports">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){

                        // onClick handler for links that open database import log popups
                        $("[id^=exportLogLink]").click(function() {
                            $('#exportLogDialog').dialog('open');
                            $('#exportLogDialog').append('<img src="${pageContext.request.contextPath}/images/wait.gif" alt="Wait clock"/>');
                            $('#exportLogDialog').load($(this).attr("href"));
                            return false;
                        });

                        // Setup of the modal dialog that displays a database's import log
                        $('#exportLogDialog').dialog({
                            autoOpen: false,
                            height: 500,
                            width: 700,
                            maxHeight: 800,
                            maxWidth: 800,
                            modal: true,
                            closeOnEscape: true
                        });
                    });
            } ) ( jQuery );
        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <%-- Drop-down operations --%>

        <ul id="dropdown-operations">
            <li><a href="#">Operations</a>
                <ul>
                    <li>
                        <stripes:link beanclass="${actionBean.databasesActionBeanClass.name}">List databases</stripes:link>
                    </li>
                    <c:if test="${actionBean.databaseDTO != null}">
	                    <li>
	                        <stripes:link beanclass="${actionBean.exportWizardActionBeanClass.name}">
	                           <c:out value="Start new export"/>
	                           <stripes:param name="dbName" value="${actionBean.databaseDTO.name}"/>
	                        </stripes:link>
	                    </li>
                    </c:if>
                </ul>
            </li>
        </ul>

        <%-- The page's heading --%>

        <c:if test="${actionBean.databaseDTO != null}">
            <h1>RDF exports from database <stripes:link beanclass="${actionBean.databaseActionBeanClass.name}"><c:out value="${actionBean.databaseDTO.name}"/><stripes:param name="dbName" value="${actionBean.databaseDTO.name}"/></stripes:link></h1>
        </c:if>
        <c:if test="${actionBean.databaseDTO == null}">
            <h1>RDF exports from all databases</h1>
        </c:if>

        <div style="margin-top:20px">
            <p>
                <c:if test="${actionBean.databaseDTO != null}">
                    This pages lists the RDF exports that have been run or are currently running from database <stripes:link beanclass="${actionBean.databaseActionBeanClass.name}"><c:out value="${actionBean.databaseDTO.name}"/><stripes:param name="dbName" value="${actionBean.databaseDTO.name}"/></stripes:link>.<br/>
                </c:if>
                <c:if test="${actionBean.databaseDTO == null}">
                    This pages lists RDF exports that have been run or are currently running from any of the available databases.<br/>
                    The databases are noted in the "Database column" which is clickable and takes you to the database's detailed view.<br/>
                </c:if>
                An export's progress can be seen from the "Status" column which is clickable and opens the export's log.<br/>
                You need to refresh the page to see if the status has changed.<br/>
                An export's further detailed view can be accessed by clicking on the export's "Name".
            </p>
        </div>

        <%-- The section that displays the list of exports. --%>

        <c:if test="${not empty actionBean.rdfExports}">

            <c:if test="${actionBean.databaseDTO == null}">
                <c:set var="colWidths" value="${fn:split('28,27,15,15,15',',')}"/>
            </c:if>
            <c:if test="${actionBean.databaseDTO != null}">
                <c:set var="colWidths" value="${fn:split('40,20,20,20',',')}"/>
            </c:if>
            <c:set var="currColNum" value="0"/>

            <div style="width:75%;padding-top:10px">
                <stripes:form id="exportsForm" method="post" beanclass="${actionBean['class'].name}">

                    <display:table name="${actionBean.rdfExports}" class="sortable" id="rdfExport" sort="list" requestURI="${actionBean.urlBinding}" style="width:100%">

                        <c:if test="${actionBean.databaseDTO == null}">
	                        <display:column title="Database" sortable="true" sortProperty="databaseName" style="width:${colWidths[currColNum]}%">
	                            <stripes:link beanclass="${actionBean.databaseActionBeanClass}">
                                    <c:out value="${rdfExport.databaseName}"/>
                                    <stripes:param name="dbName" value="${rdfExport.databaseName}"/>
                                </stripes:link>
	                            <c:set var="currColNum" value="${currColNum + 1}"/>
	                        </display:column>
                        </c:if>

                        <display:column title="Name" sortable="true" sortProperty="exportName" style="width:${colWidths[currColNum]}%">
                            <c:choose>
                                <c:when test="${rdfExport.status.name == 'COMPLETED' || rdfExport.status.name == 'COMPLETED_WARNINGS'}">
                                    <stripes:link beanclass="${actionBean.exportActionBeanClass}">
                                        <c:out value="${rdfExport.exportName}"/>
                                        <stripes:param name="exportId" value="${rdfExport.exportId}"/>
                                    </stripes:link>
                                </c:when>
                                <c:otherwise>
                                    <c:out value="${rdfExport.exportName}"/>
                                </c:otherwise>
                            </c:choose>
                            <c:set var="currColNum" value="${currColNum + 1}"/>
                        </display:column>

                        <display:column title="User" sortable="true" sortProperty="userName" style="width:${colWidths[currColNum]}%">
                            <c:out value="${rdfExport.userName}"/>
                            <c:set var="currColNum" value="${currColNum + 1}"/>
                        </display:column>

                        <display:column title="Started" sortable="true" sortProperty="started" style="width:${colWidths[currColNum]}%">
                            <fmt:formatDate value="${rdfExport.started}" pattern="yyyy-MM-dd HH:mm:ss" />
                            <c:set var="currColNum" value="${currColNum + 1}"/>
                        </display:column>

                        <display:column title="Status" sortable="true" sortProperty="status" style="width:${colWidths[currColNum]}%">
                            <c:choose>
                                <c:when test="${rdfExport.status.name == 'NOT_STARTED'}">
                                   <c:out value="${rdfExport.status}"/>
                                </c:when>
                                <c:otherwise>
                                    <stripes:link id="exportLogLink_${rdfExport.exportId}" beanclass="${actionBean.exportActionBeanClass}" event="openLog" title="View the export log">
                                        <c:out value="${rdfExport.status}"/>
                                        <stripes:param name="exportId" value="${rdfExport.exportId}"/>
                                    </stripes:link>
                                </c:otherwise>
                            </c:choose>
                            <c:set var="currColNum" value="${currColNum + 1}"/>
                        </display:column>

                    </display:table>

                </stripes:form>
            </div>

            <%-- The div that is invisible by default, but will be displayed and filled once the "open export log" operation is invoked. --%>
            <div id="exportLogDialog" title="Export log"></div>
        </c:if>

        <%-- Message if no exports found. --%>

        <c:if test="${empty actionBean.rdfExports}">
            <c:if test="${actionBean.databaseDTO == null}">
                <div class="system-msg">No RDF exports found! You can start one from the detailed view of any staging database.</div>
            </c:if>
            <c:if test="${actionBean.databaseDTO != null}">
                <div class="system-msg">No RDF exports found! You can start one from the operations menu.</div>
            </c:if>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
