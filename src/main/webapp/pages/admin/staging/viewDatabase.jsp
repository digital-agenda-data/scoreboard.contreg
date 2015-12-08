<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Staging database detailed view">

    <c:if test="${actionBean.dbDTO != null}">
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
    </c:if>

    <stripes:layout-component name="contents">

        <c:if test="${actionBean.dbDTO != null}">

            <%-- Drop-down operations --%>

            <ul id="dropdown-operations">
                <li><a href="#">Operations</a>
                    <ul>
                        <li>
                            <stripes:link beanclass="${actionBean.rdfExportWizardActionBeanClass.name}" title="Run RDF export from this database">
                                <c:out value="Export RDF"/>
                                <stripes:param name="dbName" value="${actionBean.dbDTO.name}"/>
                            </stripes:link>
                        </li>
                        <li>
                            <stripes:link beanclass="${actionBean.rdfExportsActionBeanClass.name}" title="List RDF exports from this database">
                                <c:out value="List RDF exports"/>
                                <stripes:param name="databaseId" value="${actionBean.dbDTO.id}"/>
                            </stripes:link>
                        </li>
                        <li>
                            <stripes:link id="importLogLink_1" href="${actionBean.stagingDatabasesActionBeanUrlBinding}" event="openLog" title="View the database's import log">
                                <c:out value="View import log"/>
                                <stripes:param name="databaseId" value="${actionBean.dbDTO.id}"/>
                            </stripes:link>
                        </li>
                        <li>
                            <stripes:link beanclass="${actionBean['class'].name}" event="edit" title="Edit database metadata">
                                <c:out value="Edit metadata"/>
                                <stripes:param name="dbName" value="${actionBean.dbDTO.name}"/>
                            </stripes:link>
                        </li>
                        <li>
                            <stripes:link beanclass="${actionBean.stagingDatabasesActionBeanClass.name}" title="Go to list of available staging databases">
                                <c:out value="Go to list of databases"/>
                            </stripes:link>
                        </li>
                    </ul>
                </li>
            </ul>

            <%-- The page's heading --%>

            <h1>Staging database detailed view</h1>

            <div style="margin-top:20px">
                This page displays the details known about the below staging database, and provides operations in the upper right menu.
            </div>

            <%-- The table with the database's details. --%>

            <div style="padding-top:20px">
                <table class="datatable" style="width:85%">
                    <colgroup>
                        <col width="30%">
                        <col width="70%">
                    </colgroup>
                    <tr>
                        <th class="question" style="text-align:right" title="Name of the database">Name:</th>
                        <td><c:out value="${actionBean.dbDTO.name}"/></td>
                    </tr>
                    <tr>
                        <th class="question" style="text-align:right" title="The user who created this database">Creator:</th>
                        <td><c:out value="${actionBean.dbDTO.creator}"/></td>
                    </tr>
                    <tr>
                        <th class="question" style="text-align:right" title="The date and time the database was created">Created:</th>
                        <td><fmt:formatDate value="${actionBean.dbDTO.created}" pattern="yyyy-MM-dd HH:mm:ss" /></td>
                    </tr>
                    <tr>
                        <th class="question" style="text-align:right" title="The database's import status">Import status:</th>
                        <td>
                            <stripes:link id="importLogLink_2" href="${actionBean.stagingDatabasesActionBeanUrlBinding}" event="openLog" title="View the import log">
                                <c:out value="${actionBean.dbDTO.importStatus}"/>
                                <stripes:param name="databaseId" value="${actionBean.dbDTO.id}"/>
                            </stripes:link>
                        </td>
                    </tr>
                    <tr>
                        <th class="question" style="text-align:right" title="Description of the database's purpose and meaning">Description:</th>
                        <td><c:out value="${actionBean.dbDTO.description}"/></td>
                    </tr>
                    <tr>
                        <th class="question" style="text-align:right" title="This database's default query for the RDF export wizard">Default export query:</th>
                        <td>
                            <c:if test="${empty actionBean.dbDTO.defaultQuery}">
                                &nbsp;
                            </c:if>
                            <c:if test="${not empty actionBean.dbDTO.defaultQuery}">
                                <pre style="font-size:0.75em;max-height:130px;overflow:auto"><c:out value="${actionBean.dbDTO.defaultQuery}" /></pre>
                            </c:if>
                        </td>
                    </tr>
                </table>

                <c:if test="${not empty actionBean.tablesColumns}">
                    <display:table name="${actionBean.tablesColumns}" id="tableColumn" class="datatable" style="width:100%;margin-top:30px">
                       <display:caption style="text-align:left;margin-bottom:10px;">Tables and columns in this database:</display:caption>
                       <display:column property="table" title="Table" style="width:34%"/>
                       <display:column property="column" title="Column" style="width:33%"/>
                       <display:column property="dataType" title="Data type" style="width:33%"/>
                    </display:table>
                </c:if>

                <c:if test="${empty actionBean.tablesColumns}">
                    <div class="note-msg" style="width:75%;margin-top:30px">
                        <strong>Note</strong>
                        <p>Found no tables in this database!</p>
                    </div>
                </c:if>

            </div>

            <%-- The div that is invisible by default, but will be displayed and filled once the "open import log" operation is invoked. --%>
            <div id="importLogDialog" title="Import log"></div>

        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
