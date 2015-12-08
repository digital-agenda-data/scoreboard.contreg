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
            <label for="codelistsSelect" class="question">Displaying items of this codelist:</label><br/>
            <stripes:select id="codelistsSelect" name="codelistUri" value="${actionBean.codelistUri}" onchange="this.form.submit();" title="${actionBean.codelistUri}">
                <c:forEach items="${actionBean.codelists}" var="codelist">
                    <stripes:option value="${codelist.left}" label="${codelist.right}" title="${codelist.left}"/>
                </c:forEach>
            </stripes:select>&nbsp;
            <stripes:submit name="metadata" value="Codelist metadata" title="Go to the factsheet about the metadata of the selected codelist."/>&nbsp;
            <stripes:submit name="export" value="Excel export" title="Download the selected codelist as an MS Excel file."/>
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

</stripes:layout-component>
</stripes:layout-render>
