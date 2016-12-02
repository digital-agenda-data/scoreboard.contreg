<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Browse DataCube datasets">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){

                        $("#creationLink").click(function() {
                            $('#creationDialog').dialog('option','width', 800);
                            $('#creationDialog').dialog('open');
                            return false;
                        });

                        $('#creationDialog').dialog({
                            autoOpen: false,
                            width: 800
                        });

                        $("#closeCreationDialog").click(function() {
                            $('#creationDialog').dialog("close");
                            return true;
                        });
                        
                        //////////////////////////////////////////////////
                        
                        $("#importLink").click(function() {
                            $('#importDialog').dialog('option','width', 800);
                            $('#importDialog').dialog('open');
                            return false;
                        });
                        
                        $('#importDialog').dialog({
                            autoOpen: false,
                            width: 800
                        });

                        $("#closeImportDialog").click(function() {
                            $('#importDialog').dialog("close");
                            return true;
                        });

                    });
            } ) ( jQuery );
        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

    <%-- Drop-down operations --%>

    <c:set var="registrationsAllowed" value='${crfn:userHasPermission(pageContext.session, "/registrations", "u")}'/>

    <c:if test="${registrationsAllowed}">
        <ul id="dropdown-operations">
            <li><a href="#">Operations</a>
                <ul>
                    <li><a href="#" id="creationLink" title="Create a new dataset">New dataset</a></li>
                    <li><a href="#" id="importLink" title="Import multiple datasets metadata">Import datasets</a></li>
                </ul>
            </li>
        </ul>
    </c:if>

    <%-- Title and intro. --%>

    <h1>Browse DataCube datasets</h1>

    <p>
        This page enables you to browse DataCube datasets available in the system.<br/>
        It simply lists all datasets found, and provides paging functions if there is more than <c:out value="${actionBean != null && actionBean.datasets != null ? actionBean.datasets.objectsPerPage : 20}"/> DataCube datasets in the system.<br/>
        Clicking on a listed dataset leads to the detailed view page of its metadata which is further browseable.
    </p>

    <%-- The "working area". --%>

    <div style="margin-top:20px;width:100%">
        <display:table name="${actionBean.datasets}" id="dataset" class="sortable" sort="external" requestURI="${actionBean.urlBinding}" style="width:100%">

            <display:setProperty name="paging.banner.item_name" value="dataset"/>
            <display:setProperty name="paging.banner.items_name" value="datasets"/>
            <display:setProperty name="paging.banner.all_items_found" value='<div class="pagebanner">{0} {1} found.</div>'/>
            <display:setProperty name="paging.banner.onepage" value=""/>

            <c:forEach items="${actionBean.availColumns}" var="column" varStatus="columnsLoopStatus">

                <display:column title="${column.title}" sortable="${column.sortable}" sortProperty="${column.alias}" style="width:${column.width}">

                    <c:if test="${not empty column.isFactsheetLink}">
                        <stripes:link beanclass="${actionBean.factsheetActionBeanClass.name}">
                            <c:out value="${dataset.left}"/>
                            <stripes:param name="uri" value="${dataset.left}"/>
                        </stripes:link>
                    </c:if>
                    <c:if test="${empty column.isFactsheetLink}">
                        <c:out value="${dataset.right}"/>
                    </c:if>

                </display:column>

            </c:forEach>

        </display:table>
    </div>
    
    <c:if test="${actionBean.datasets == null || empty actionBean.datasets.list}">
        <div class="tip-msg">
            <strong>Tip</strong>
            <p>
                If you don't see any results then it might be that there is simply no data,<br/>
                or you don't have sufficient privileges. <stripes:link title="Login" href="/login.action" event="login">Please try logging in</stripes:link>.
            </p>
        </div>
    </c:if>

    <%-- The "create new dataset" dialog. Hidden unless activated. --%>

    <div id="creationDialog" title="Create a new dataset">
        <stripes:form beanclass="${actionBean['class'].name}" method="post">

            <p>
                The following properties are sufficient to create a new dataset. The ones mandatory, are marked with <img src="${pageContext.request.contextPath}/images/mandatory.gif"/>.<br/>
                More information is displayed when placing the mouse over properties' labels.<br/>
                Once the dataset is created, you can add more properties on the dataset's detailed view page.
            </p>

            <table>
                <tr>
                    <td><stripes:label for="txtTitle" class="question required" title="The dataset's unique identifier used by the system to distinguish it from others. Only digits, latin letters, underscores and dashes allowed! Will go into the dataset URI and also into the property identified by http://purl.org/dc/terms/identifier">Identifier:</stripes:label></td>
                    <td><stripes:text name="identifier" id="txtIdentifier" size="60"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="txtTitle" class="question required" title="Friendly name of the dataset. Any free text allowed here. Will go into the property identified by http://purl.org/dc/terms/title">Title:</stripes:label></td>
                    <td><stripes:text name="dctermsTitle" id="txtTitle" size="80"/></td>
                </tr>
                <tr>
                    <td><stripes:label for="txtDescription" class="question" title="Humanly understandable detailed description of the dataset. Any free text allowed here. Will go into the property identified by http://purl.org/dc/terms/description">Description:</stripes:label></td>
                    <td>
                        <stripes:textarea id="txtDescription" name="dctermsDescription" cols="80" rows="10"/>
                    </td>
                </tr>
                <tr>
                    <td>&nbsp;</td>
                    <td style="padding-top:10px">
                        <stripes:submit name="createNew" value="Create"/>
                        <input type="button" id="closeCreationDialog" value="Cancel"/>
                    </td>
                </tr>
            </table>

        </stripes:form>
    </div>
    
    <%-- Datasets metadata importing dialog. Hidden unless activated. --%>
    
    <div id="importDialog" title="Import datasets">
        <stripes:form beanclass="${actionBean['class'].name}" method="post">

            <p>
                You can import the metadata of multiple datasets with a specially designed Excel spreadsheet file below.<br/>
                Select the file and click "Import". You will get feedback on import results.
            </p>

            <table>
                <tr>
                    <td style="text-align:right">
                        <label for="fileInput" class="question required">Spreadsheet file:</label>
                    </td>
                    <td>
                       <stripes:file name="importFileBean" id="fileInput" size="120"/>
                    </td>
                </tr>
                <tr>
                    <td>&nbsp;</td>
                    <td>
                       <input type="checkbox" name="clearExisting" id="chkClear" value="true"/><label for="chkClear">&nbsp;clear existing metadata of imported datasets</label>
                    </td>
                </tr>
                <tr>
                    <td>&nbsp;</td>
                    <td style="padding-top:10px">
                        <stripes:submit name="importNew" value="Import"/>
                        <input type="button" id="closeImportDialog" value="Cancel"/>
                    </td>
                </tr>
            </table>

        </stripes:form>
    </div>

</stripes:layout-component>
</stripes:layout-render>
