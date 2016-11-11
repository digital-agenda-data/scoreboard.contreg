<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="An RDF export">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){

                        // onClick handler for links that open database export log popups
                        $("[id^=exportLogLink]").click(function() {
                            $('#exportLogDialog').dialog('open');
                            $('#exportLogDialog').append('<img src="${pageContext.request.contextPath}/images/wait.gif" alt="Wait clock"/>');
                            $('#exportLogDialog').load($(this).attr("href"));
                            return false;
                        });

                        // Setup of the modal dialog that displays an export's log
                        $('#exportLogDialog').dialog({
                            autoOpen: false,
                            height: 500,
                            width: 700,
                            maxHeight: 800,
                            maxWidth: 800,
                            modal: true,
                            closeOnEscape: true
                        });

                        // Open the query configuration popup
                        $("#openQueryConfPopup").click(function() {
                            $('#queryConfPopup').dialog('open');
                            return false;
                        });

                        // Setup the query configuration popup.
                        $('#queryConfPopup').dialog({
                            autoOpen: false,
                            height: 500,
                            width: 700,
                            maxHeight: 800,
                            maxWidth: 800
                        });

                        // Close the query configuration popup
                        $("#closeQueryConfPopup").click(function() {
                            $('#queryConfPopup').dialog("close");
                            return true;
                        });

                        <c:if test="${actionBean.exportDTO.status.finished && not empty actionBean.missingConcepts}">
                        // Open the missing concepts popup
                        $("#openMissingConceptsPopup").click(function() {
                            $('#missingConceptsPopup').dialog('open');
                            return false;
                        });

                        // Setup the tables and columns popup
                        $('#missingConceptsPopup').dialog({
                            autoOpen: false,
                            height: 400,
                            width: 800,
                            maxHeight: 800,
                            maxWidth: 800
                        });
                        </c:if>

                    });
            } ) ( jQuery );
        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <c:if test="${actionBean.exportDTO != null}">

            <%-- The page's heading --%>

            <h1>RDF export: detailed view</h1>

            <div style="margin-top:20px">
                <p>
                    This is a detailed view of an RDF export that was given the below "Name", and "Started" on the below "Database" by the below "User".<br/>
                    The export's log can be seen by cliking on the export's "Status".<br/>
                    The exact query and other configuration of this export can be seen by clicking on the "Query configuration" link below.<br/>
                    The last section of the page provides links to the dataset(s) where this export's result were stored into. These lead to pages that<br/>
                    list the objects (e.g. Observations) in the dataset(s).
                </p>
            </div>

            <%-- The table with the export's fields. --%>

            <div style="padding-top:10px">
                <table class="datatable" style="width:75%">
                    <colgroup>
                        <col width="25%">
                        <col width="75%">
                    </colgroup>
                    <tr>
                        <th class="question" style="text-align:right" title="The export's descriptive name">Name:</th>
                        <td><c:out value="${actionBean.exportDTO.exportName}"/></td>
                    </tr>
                    <tr>
                        <th class="question" style="text-align:right" title="The database from which the export was run">Database:</th>
                        <td>
                            <stripes:link beanclass="${actionBean.databaseActionBeanClass.name}">
                                <c:out value="${actionBean.exportDTO.databaseName}"/>
                                <stripes:param name="dbName" value="${actionBean.exportDTO.databaseName}"/>
                            </stripes:link>
                        </td>
                    </tr>
                    <tr>
                        <th class="question" style="text-align:right" title="The user who ran the export">User:</th>
                        <td><c:out value="${actionBean.exportDTO.userName}"/></td>
                    </tr>
                    <tr>
                        <th class="question" style="text-align:right" title="The date and time when the export was started">Started:</th>
                        <td><fmt:formatDate value="${actionBean.exportDTO.started}" pattern="yyyy-MM-dd HH:mm:ss" /></td>
                    </tr>
                    <tr>
                        <th class="question" style="text-align:right" title="The date and time when the export was finished (regadless of whether successfully or not)">Finished:</th>
                        <td><fmt:formatDate value="${actionBean.exportDTO.finished}" pattern="yyyy-MM-dd HH:mm:ss" /></td>
                    </tr>
                    <tr>
                        <th class="question" style="text-align:right" title="The export's status as of right now">Status:</th>
                        <td>
                            <stripes:link id="exportLogLink_1" beanclass="${actionBean['class'].name}" event="openLog" title="View the export's log">
                                <c:out value="${actionBean.exportDTO.status}"/>
                                <stripes:param name="exportId" value="${actionBean.exportDTO.exportId}"/>
                            </stripes:link>
                        </td>
                    </tr>
                    <c:if test="${actionBean.exportDTO.status.finished}">
                        <tr>
                            <th class="question" style="text-align:right" title="The number of rows returned by the executed query">Row count:</th>
                            <td><c:out value="${actionBean.exportDTO.rowCount}"/></td>
                        </tr>
                        <tr>
                            <th class="question" style="text-align:right" title="The number of objects that were exported">Objects exported:</th>
                            <td><c:out value="${actionBean.exportDTO.noOfSubjects}"/></td>
                        </tr>
                        <tr>
                            <th class="question" style="text-align:right" title="The number of triples that were exported">Triples exported:</th>
                            <td><c:out value="${actionBean.exportDTO.noOfTriples}"/></td>
                        </tr>
                    </c:if>
                    <tr>
                        <th class="question" style="text-align:right" title="The export query and its configuration">Query configuration:</th>
                        <td>
                            <a href="#" id="openQueryConfPopup">View the export query and its configuration</a>
                        </td>
                    </tr>
                </table>

                <c:if test="${actionBean.exportDTO.status.finished}">

                    <c:if test="${not empty actionBean.missingConcepts}">
                        <a href="#" id="openMissingConceptsPopup" class="important-msg" style="float:right" title="Click to open the list of concepts for which no metadata was found">No metadata was found for these concepts &#187;</a>
                    </c:if>

                    <div style="width:100%;padding-top:10px;">

                        <p>The results of this export were stored into the following dataset(s):</p>
                        <c:if test="${not empty actionBean.exportDTO.graphs}">
                            <ul style="list-style:none;margin:0;padding:0;">
                                <c:forEach items="${actionBean.exportDTO.graphsList}" var="graphUri">
                                    <li>
                                        <stripes:link beanclass="${actionBean.objectsInSourceActionBeanClass.name}">
                                            <c:out value="${graphUri}"/>
                                            <stripes:param name="search" value=""/>
                                            <stripes:param name="uri" value="${graphUri}"/>
                                        </stripes:link>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:if>

                    </div>
                </c:if>
            </div>

            <%-- The div that is invisible by default, but will be displayed and filled once the "open export log" operation is invoked. --%>
            <div id="exportLogDialog" title="Export log"></div>

            <div id="queryConfPopup" title="Export query and its configuration">
                <%-- Assume this has already been XML-escaped. --%>
                ${actionBean.queryConfigurationDump}
            </div>

            <c:if test="${actionBean.exportDTO.status.finished && not empty actionBean.missingConcepts}">
                <div id="missingConceptsPopup" title="Concepts with no metadata in the system yet">
                    <table class="datatable" style="width:100%">
                        <tr>
                            <c:forEach items="${actionBean.missingConcepts}" var="missingConceptsEntry">
                                <th>
                                    <c:out value="${missingConceptsEntry.key}"/>
                                </th>
                            </c:forEach>
                        </tr>
                        <tr>
                            <c:forEach items="${actionBean.missingConcepts}" var="missingConceptsEntry">
                                <td>
                                    <c:forEach items="${missingConceptsEntry.value}" var="missingConcept">
                                        <ul style="list-style-type:none">
                                            <li><c:out value="${missingConcept}"/></li>
                                        </ul>
                                    </c:forEach>
                                </td>
                            </c:forEach>
                        </tr>
                    </table>
                </div>
            </c:if>

        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
