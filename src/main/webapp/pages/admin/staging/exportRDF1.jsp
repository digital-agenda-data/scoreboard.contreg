<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Staging databases: export RDF: step 1">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){

                        //////////////////////////////////////////////////

                        $("#openTableAndColumnsPopup").click(function() {
                            $('#tableAndColumnsPopup').dialog('open');
                            return false;
                        });

                        $('#tableAndColumnsPopup').dialog({
                            autoOpen: false,
                            height: 400,
                            width: 800,
                            maxHeight: 800,
                            maxWidth: 800
                        });

                        $("#closeTableAndColumnsPopup").click(function() {
                            $('#tableAndColumnsPopup').dialog("close");
                            return true;
                        });

                        //////////////////////////////////////////////////

                        $("#createNewDatasetLink").click(function() {
                            $('#createNewDatasetDialog').dialog('option','width', 800);
                            $('#createNewDatasetDialog').dialog('open');
                            return false;
                        });

                        $('#createNewDatasetDialog').dialog({
                            autoOpen: false,
                            width: 800
                        });

                        $("#closeCreateNewDatasetDialog").click(function() {
                            $('#createNewDatasetDialog').dialog("close");
                            return true;
                        });

                    });
            } ) ( jQuery );
        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <%-- The page's heading --%>

        <h1>RDF export: step 1</h1>

        <div style="margin-top:20px">
            You have chosen to run an RDF export from database <stripes:link beanclass="${actionBean.databaseActionBeanClass.name}"><c:out value="${actionBean.dbName}"/><stripes:param name="dbName" value="${actionBean.dbName}"/></stripes:link>.<br/>
            As the first step, please type the SQL query whose results will be exported, and select the type of objects that the query returns.<br/>
            Also please give this RDF export a descriptive name that will help you to distinguish it from others later.<br/>
            Mandatory inputs are marked with <img src="${pageContext.request.contextPath}/images/mandatory.gif"/>.
        </div>

        <%-- The form --%>

        <div style="padding-top:10px">
            <crfn:form id="form1" beanclass="${actionBean['class'].name}" method="post">
                <table>
                    <tr>
                        <td style="text-align:right;vertical-align:top">
                            <stripes:label for="txtName" class="question required" title="Human-readable name of this export that will help to distinguish it from other exports when viewing exports' history.">Name:</stripes:label>
                        </td>
                        <td>
                            <stripes:text id="txtName" name="exportName" size="80" value="${actionBean.defaultExportName}"/>&nbsp;&nbsp;<a href="#" id="openTableAndColumnsPopup">View tables and columns &#187;</a>
                        </td>
                    </tr>
                    <tr>
                         <td style="text-align:right;vertical-align:top">
                             <stripes:label for="txtQuery" class="question required" title="SQL query whose results are to be exported.">Query:</stripes:label>
                         </td>
                         <td>
                             <stripes:textarea id="txtQuery" name="queryConf.query" cols="80" rows="15" value="${actionBean.dbDTO.defaultQuery}"/>
                         </td>
                     </tr>
                    <tr>
                        <td style="text-align:right;vertical-align:top">
                            <stripes:label for="selObjectType" class="question required" title="The type of objects that the query will return.">Objects type:</stripes:label>
                        </td>
                        <td>
                            <stripes:select name="queryConf.objectTypeDsd" id="selObjectType" value="${actionBean.queryConf.objectTypeDsd}">
                                <c:forEach items="${actionBean.objectTypes}" var="objectType">
                                    <stripes:option value="${objectType.dsd}" label="${objectType.label}"/>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td>&nbsp;</td>
                        <td>
                            <stripes:submit name="step1" value="Next >"/>
                            <stripes:submit name="cancel" value="Cancel"/>
                        </td>
                    </tr>
                </table>
            </crfn:form>
        </div>

        <%-- The tables and columns popup. Displayed when user clicks on the relevant popup link. --%>

        <div id="tableAndColumnsPopup" title="Tables and columns in this database">

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

    </stripes:layout-component>
</stripes:layout-render>
