<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Type search">

    <stripes:layout-component name="contents">
        <ul id="dropdown-operations">
            <li><a href="#">Operations</a>
                <ul>
                    <c:if test="${not empty actionBean.resultList}">
                        <li><a href="#" id="eport_link">Export</a></li>
                    </c:if>
                    <c:if test="${not empty actionBean.queryString}">
                    <li>
                        <a href="#" id="queryLink">Search query</a>
                    </li>
                    </c:if>
                </ul>
            </li>
        </ul>

        <div style="max-width: 750px;" id="export_form_container">
       <h1>Type search</h1>
       <p>
           This page enables to find content by type. Below is the list of types known to CR.
           The search will return the list of all resources having the type you selected.
           To view a resource's factsheet, click the relevant action icon next to it.
       </p>
        <crfn:form action="/typeSearch.action" method="get">
            <c:choose>
            <c:when test="${not empty actionBean.type}">
                <script language="javascript">
// <![CDATA[
                function hidediv() {
                    if (document.getElementById) { // DOM3 = IE5, NS6
                        document.body.className = 'fullscreen';
                    } else {
                        if (document.layers) { // Netscape 4
                            document.body.className = 'fullscreen';
                        } else { // IE 4
                            document.body.className = 'fullscreen';
                        }
                    }
                }

                hidediv();

                ( function($) {
                    $(document).ready(
                        function(){
                            // Open dialog
                            $("#eport_link").click(function() {
                                $('#dialog').dialog('open');
                                return false;
                            });

                            // Dialog setup
                            $('#dialog').dialog({
                                autoOpen: false,
                                width: 500
                            });

                            // Close dialog
                            $("#export_form_submit").click(function() {
                                $('#dialog').dialog("close");
                                return true;
                            });

                            // Open dialog
                            $("#queryLink").click(function() {
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

                <stripes:select name="type">
                    <c:forEach var="groups" items="${actionBean.availableTypes}">
                    <optgroup label="${groups.left}">
                        <c:forEach var="type" items="${groups.right}">
                            <stripes:option value="${type.left}">${type.right}</stripes:option>
                        </c:forEach>
                    </optgroup>
                    </c:forEach>
                </stripes:select>
               </c:when>
               <c:otherwise>

               <div id="tabbedmenu">
                <ul>
                    <c:choose>
                        <c:when test="${actionBean.typesByName}">
                            <li>
                                <stripes:link href="${actionBean.urlBinding}">
                                    By namespace URI
                                    <stripes:param name="typesByName" value="false"/>
                                </stripes:link>
                            </li>
                            <li id="currenttab"><span>By Name</span></li>
                        </c:when>
                        <c:otherwise>
                            <li id="currenttab"><span>By namespace URI</span></li>
                            <li>
                                <stripes:link href="${actionBean.urlBinding}">
                                    By Name
                                    <stripes:param name="typesByName" value="true"/>
                                </stripes:link>
                            </li>
                        </c:otherwise>
                    </c:choose>
                </ul>
                </div>
                <c:choose>
                    <c:when test="${actionBean.typesByName}">
                        <stripes:select name="type" size="20" style="min-width:550px; width:550px;" title="Select a type by name.">
                            <c:forEach var="type" items="${actionBean.availableTypesByName}">
                                <stripes:option value="${type.left}">${type.right}</stripes:option>
                            </c:forEach>
                        </stripes:select>
                    </c:when>
                    <c:otherwise>
                        <stripes:select name="type" size="20" style="min-width:550px; width:550px;" title="Select a type by namespace URI.">
                            <c:forEach var="groups" items="${actionBean.availableTypes}">
                            <optgroup label="${groups.left}">
                                <c:forEach var="type" items="${groups.right}">
                                    <stripes:option value="${type.left}">${type.right}</stripes:option>
                                </c:forEach>
                            </optgroup>
                            </c:forEach>
                        </stripes:select>
                    </c:otherwise>
                </c:choose>
               </c:otherwise>
               </c:choose>


            <stripes:submit name="search" value="Search" />
            <c:if test='${crfn:userHasPermission(pageContext.session, "/", "u")}'>
                &nbsp;<stripes:submit name="introspect" value="Introspect"/>
            </c:if>
        </crfn:form>


            <br/>
            <c:if test="${not empty actionBean.type}">

            <div id="dialog" title="Export options">
                <crfn:form action="/typeSearch.action" method="post">
                    <stripes:hidden name="type" value="${actionBean.type }"/>
                    <fieldset style="border: 0px;">
                        <label for="export_resource" style="width: 200px; float: left;">Resource identifier</label>
                        <stripes:select id="export_resource" name="uriResourceIdentifier">
                            <stripes:option value="true">Uri</stripes:option>
                            <stripes:option value="false">Label</stripes:option>
                        </stripes:select>
                    </fieldset>
                    <fieldset style="border: 0px;">
                        <label for="export_format" style="width: 200px; float: left;">Export format</label>
                        <c:set var="XLS"><%=eionet.cr.util.export.ExportFormat.XLS.getName()%></c:set>
                        <stripes:select id="export_format" name="exportFormat">
                            <c:forEach items="${ actionBean.exportFormats}" var="format">
                                <c:if test="${ (format.name eq XLS and actionBean.showExcelExport) or format.name ne XLS}">
                                    <stripes:option value="${format.name}">${format.name}</stripes:option>
                                </c:if>
                            </c:forEach>
                        </stripes:select>
                    </fieldset>
                    <fieldset style="border: 0px;">
                        <label for="export_columns" style="width: 200px; float: left;">Select columns to be exported</label><br/>
                        <stripes:select id="export_columns" name="exportColumns" multiple="multiple" size="5" style="min-width:250px; width:250px;">
                            <c:forEach items="${actionBean.availableColumns }" var="column">
                                <stripes:option value="${column.key}">${column.value}</stripes:option>
                            </c:forEach>
                        </stripes:select>
                    </fieldset>
                    <stripes:submit name="export" value="Export" id="export_form_submit" style="float: right;"/>
                </crfn:form>
            </div>
            <div id="select_filters" style="margin-bottom:10px; float:right;  min-width:400px; width:50%;">
                <fieldset>
                <legend>Select filters</legend>
                <c:if test="${not empty actionBean.availableFilters}">
                <crfn:form action="/typeSearch.action" method="post">
                    <stripes:hidden name="addFilter" value="addFilter"/>
                    <stripes:hidden name="type" value="${actionBean.type }"/>
                    <stripes:select name="newFilter" onchange="this.form.submit();">
                        <stripes:option value="">Select filter to add </stripes:option>
                        <c:forEach items="${actionBean.availableFiltersSorted}" var="column">
                            <stripes:option value="${column.key}">${column.value}</stripes:option>
                        </c:forEach>
                    </stripes:select>
                    <noscript>
                        <stripes:submit name="addFilter" value="Add filter"/>
                    </noscript>
                </crfn:form>
                <br/>
                </c:if>
                    <c:if test="${not empty actionBean.displayFilters}">
                        <crfn:form action="/typeSearch.action" method="post">
                        <stripes:hidden name="type" value="${actionBean.type }"/>
                        <table>
                            <tbody>
                                <c:forEach items="${actionBean.displayFilters}" var="filter">
                                    <tr>
                                        <td>${filter.key}</td>
                                        <td><stripes:text name="selectedFilters[${filter.value.left}]" size="40" value="${filter.value.right}"/></td>
                                        <td>
                                            <stripes:submit name="removeFilter_${filter.value.left}" value="-"/>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                        <stripes:submit name="applyFilters" value="Apply filters"/>
                        </crfn:form>
                        <br/>
                    </c:if>
                </fieldset>
            </div>
            </c:if>
        <c:if test="${not empty actionBean.type and not empty actionBean.availableColumns}">
            <div style="max-width: 350px;" id="select_columns">
                <fieldset>
                <legend>Select columns to be displayed</legend>
                <crfn:form action="/typeSearch.action" method="post">
                    <stripes:hidden name="type" value="${actionBean.type }"/>
                    <stripes:select name="selectedColumns" multiple="multiple" size="5" style="min-width:250px; width:250px;">
                        <c:forEach items="${actionBean.availableColumnsSorted}" var="column">
                            <stripes:option value="${column.key}">${column.value}</stripes:option>
                        </c:forEach>
                    </stripes:select>
                    <stripes:submit name="setSearchColumns" value="Set"/>
                </crfn:form>
                </fieldset>
            </div>
        </c:if>
        <c:if test="${! empty actionBean.type}">
            <stripes:layout-render name="/pages/common/subjectsResultList.jsp" tableClass="sortable" />
        </c:if>
        </div>

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
