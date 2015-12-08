<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Staging databases">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){

                        // onClick handler for links that open database import log popups
                        $("[id^=importLogLink]").click(function() {
                            $('#importLogDialog').dialog('open');
                            $('#importLogDialog').append('<img src="${pageContext.request.contextPath}/images/wait.gif" alt="Wait clock"/>');
                            $('#importLogDialog').load($(this).attr("href"));
                            return false;
                        });

                        // Setup of the modal dialog that displays a database's import log
                        $('#importLogDialog').dialog({
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
                        <stripes:link beanclass="${actionBean.availableFilesActionBeanClass.name}">Available files</stripes:link>
                    </li>
                    <li>
                        <stripes:link beanclass="${actionBean.rdfExportsActionBeanClass.name}">List RDF exports</stripes:link>
                    </li>
                </ul>
            </li>
        </ul>

        <%-- The page's heading --%>

        <h1>Staging databases</h1>

        <div style="margin-top:20px">
                <p>
                    New staging databases can be created from the currently <stripes:link beanclass="${actionBean.availableFilesActionBeanClass.name}">available files</stripes:link>.
                    <c:if test="${not empty actionBean.databases}">
                        <br/>
                        If a database's import has been started, the "Import status" column is clickable and opens the import log.<br/>
                        Refresh this page to monitor the import progress of listed databases.<br/>
                        Once a database's import is complete, it becomes clickable and leads to a detailed view page with further operations available.
                    </c:if>
                </p>
        </div>

        <%-- The section that displays the databases list. --%>

        <c:if test="${not empty actionBean.databases}">

            <div style="width:75%;padding-top:10px">
                <stripes:form id="databasesForm" method="post" beanclass="${actionBean['class'].name}">

                    <display:table name="${actionBean.databases}" class="sortable" id="database" sort="list" requestURI="${actionBean.urlBinding}" style="width:100%">
                        <display:column style="width:5%">
                            <stripes:checkbox name="dbNames" value="${database.name}" />
                        </display:column>
                        <display:column title="Name" sortable="true" sortProperty="name" style="width:30%">
                            <c:choose>
                                <c:when test="${database.importStatus.name == 'COMPLETED' || database.importStatus.name == 'COMPLETED_WARNINGS'}">
                                    <stripes:link beanclass="${actionBean.databaseActionBeanClass}">
                                        <c:out value="${database.name}"/>
                                        <stripes:param name="dbName" value="${database.name}"/>
                                    </stripes:link>
                                </c:when>
                                <c:otherwise>
                                    <c:out value="${database.name}"/>
                                </c:otherwise>
                            </c:choose>
                        </display:column>
                        <display:column property="creator" title="Creator" sortable="true" style="width:20%"/>
                        <display:column title="Import status" sortable="true" sortProperty="importStatus" style="width:25%">
                            <c:choose>
                                <c:when test="${database.importStatus.name == 'NOT_STARTED'}">
                                   <c:out value="${actionBean.importStatuses[database.importStatus]}"/>
                                </c:when>
                                <c:otherwise>
                                    <stripes:link id="importLogLink_${database.id}" href="${actionBean.urlBinding}" event="openLog" title="View the import log">
                                        <c:out value="${database.importStatus}"/>
                                        <stripes:param name="databaseId" value="${database.id}"/>
                                    </stripes:link>
                                </c:otherwise>
                            </c:choose>
                        </display:column>
                        <display:column title="Created" sortable="true" sortProperty="created" style="width:20%">
                            <fmt:formatDate value="${database.created}" pattern="yyyy-MM-dd HH:mm:ss" />
                        </display:column>
                    </display:table>

                    <stripes:submit name="delete" value="Delete" />
                    <input type="button" onclick="toggleSelectAll('databasesForm');return false" value="Select all" name="selectAll">

                </stripes:form>
            </div>

            <%-- The div that is invisible by default, but will be displayed and filled once the "open import log" operation is invoked. --%>
            <div id="importLogDialog" title="Import log"></div>
        </c:if>

        <%-- Message if no databases found. --%>

        <c:if test="${empty actionBean.databases}">
            <div class="system-msg">No staging databases found! You can create one from the list of available files.</div>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
