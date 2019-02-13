<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Browse Scoreboard codelists">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){

                        // Ensure a codelist select box title is updated with the hint of the currently selected codelist.
                        $("#codelistsSelect").change(function() {
                            $(this).attr("title", $("option:selected",this).attr('title'));
                            return true;
                        });

                        // Actions for the display/closing of the "Change dataset status" popup
                        $("#btnCreateCodelistItem").click(function() {
                            $('#codelistItemCreationDialog').dialog('option','width', 800);
                            $('#codelistItemCreationDialog').dialog('open');
                            return false;
                        });

                        $('#codelistItemCreationDialog').dialog({
                            autoOpen: false,
                            width: 800
                        });

                        $("#closeCodelistItemCreationDialog").click(function() {
                            $('#codelistItemCreationDialog').dialog("close");
                            return true;
                        });

                    });
            } ) ( jQuery );
        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

    <h1>Browse Scoreboard codelists</h1>

    <p>
        This page enables you to browse the Digital Agenda Scoreboard codelists (i.e. indicators, breakdowns, etc) available in the system.<br/>
        The below dropdown lists the codelists the system knows about. Selecting one will list all the available codes in the selected codelist.<br/>
        Selecting one, and clicking "Codelist metadata" will go to the factsheet about the metadata of the selected codelist.
    </p>

    <div style="margin-top:20px;width:100%">
        <crfn:form id="codelistsForm" beanclass="${actionBean['class'].name}" method="get">

            <div>
                <label for="codelistsSelect" class="question inline-left w-80">Codelist:</label>
                <stripes:select id="codelistsSelect" name="codelistUri" value="${actionBean.codelistUri}" onchange="this.form.submit();" title="${actionBean.codelistUri}">
                    <c:forEach items="${actionBean.codelists}" var="codelist">
                        <stripes:option value="${codelist.left}" label="${codelist.right}" title="${codelist.left}"/>
                    </c:forEach>
                </stripes:select>&nbsp;
                <stripes:submit name="metadata" value="Codelist metadata" title="Go to the factsheet about the metadata of the selected codelist."/>&nbsp;
                <stripes:submit name="export" value="Excel export" title="Download the selected codelist as an MS Excel file."/>
                <c:if test="${actionBean.modifyPermitted}"><input type="button" id="btnCreateCodelistItem" name="btnAddItem" value="Add item ..."/></c:if>
            </div>

            <div>
                <label for="datasetSelect" class="question inline-left w-80">Dataset:</label>
                <stripes:select id="datasetSelect" name="datasetUri" value="${actionBean.datasetUri}" onchange="this.form.submit();" title="${actionBean.datasetUri}">
                    <option value="">-- all datasets --</option>
                    <c:forEach items="${actionBean.datasets}" var="dst">
                        <stripes:option value="${dst.left}" label="${dst.right}" title="${dst.left}"/>
                    </c:forEach>
                </stripes:select>
            </div>
            <div>
                <label for="freeText" class="question inline-left w-80">Free text:</label>
                <stripes:text id="freeText" name="freeText"/>&nbsp;<stripes:submit name="search" value="Search" title="Search based on above selections and free text."/>
            </div>
        </crfn:form>
    </div>

    <c:if test="${not empty actionBean.codelistUri && empty actionBean.codelistItems}">
        <div style="margin-top:20px;width:100%" class="system-msg">
            No codes found in the selected codelist!
        </div>
    </c:if>

    <c:if test="${not empty actionBean.codelistUri && not empty actionBean.codelistItems}">
        <div style="margin-top:20px;width:100%">
            <display:table name="${actionBean.codelistItems}" id="codelistItem" class="sortable" sort="list" pagesize="20" requestURI="${actionBean.urlBinding}" style="width:100%">

                <display:setProperty name="paging.banner.item_name" value="item"/>
                <display:setProperty name="paging.banner.items_name" value="items"/>
                <display:setProperty name="paging.banner.all_items_found" value='<div class="pagebanner">{0} {1} found.</div>'/>
                <display:setProperty name="paging.banner.onepage" value=""/>

                <display:column title='<span title="Notation, i.e. the code itself">Notation</span>' sortable="true" sortProperty="skosNotation" style="width:30%">
                    <stripes:link beanclass="${actionBean.factsheetActionBeanClass.name}" title="${codelistItem.uri}">
                        <c:out value="${codelistItem.skosNotation}"/>
                        <stripes:param name="uri" value="${codelistItem.uri}"/>
                    </stripes:link>
                </display:column>

                <display:column title='<span title="The preferred humanly understandable lanbel of the code">Label</span>' sortable="true" sortProperty="skosPrefLabel" style="width:70%">
                    <c:out value="${codelistItem.skosPrefLabel}"/>
                </display:column>

            </display:table>
        </div>
    </c:if>

    <c:if test="${actionBean.modifyPermitted}">
        <div id="codelistItemCreationDialog" title="Codelist item creation">

            <div class="tip-msg">
                <strong>Tip</strong>
                <p><small>Create an item in this codelist: <stripes:link href="/factsheet.action" target="_blank"><c:out value="${actionBean.codelistUri}"/>
                    <stripes:param name="uri" value="${actionBean.codelistUri}"/>
                </stripes:link><br/>Fill the inputs just as you would fill them in spreadsheet upload template!</small></p>
            </div>

            <stripes:form beanclass="${actionBean['class'].name}" method="post">

                <table>
                    <c:forEach items="${actionBean.templateColumnNames}" var="colName" varStatus="colNameLoop">
                        <tr>
                            <td style="padding-right:5px">
                                <label class="question ${fn:endsWith(colName, 'notation') ? 'required' : ''}" for="col_${colNameLoop.index}"><c:out value="${colName}"/>:</label>
                            </td>
                            <td>
                                <input type="text" id="col_${colNameLoop.index}" name="col_${colNameLoop.index}" ${fn:endsWith(colName, 'notation') ? 'required' : ''} size="65"/>
                            </td>
                        </tr>
                    </c:forEach>
                </table>

                <div style="padding-top:10px">
                    <stripes:hidden name="codelistUri" value="${actionBean.codelistUri}"/>
                    <input type="submit" name="createItem" value="Create"/>
                    <input type="button" id="closeCodelistItemCreationDialog" value="Cancel"/>
                </div>

            </stripes:form>
        </div>
    </c:if>

</stripes:layout-component>
</stripes:layout-render>
