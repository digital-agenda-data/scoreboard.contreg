<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Dataset indicators">

    <stripes:layout-component name="head">
        <script type="text/javascript">
            // <![CDATA[

            ( function($) {
                $(document).ready(
                    function(){

                        $('#searchForm').submit(function() {
                            $('#loader').show();
                            return true;
                        });

                        $('#timePeriodSelect').change(function() {
                            $('#loader').show();
                            $('#searchForm').submit();
                            return true;
                        });

                    });
            } ) ( jQuery );
            // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <cr:tabMenu tabs="${actionBean.tabs}" />

        <br style="clear:left" />

        <div style="margin-top:20px">
            Indicators referenced in dataset:
            <stripes:link href="/factsheet.action"><c:out value="${actionBean.datasetUri}"/>
                <stripes:param name="uri" value="${actionBean.datasetUri}"/>
            </stripes:link>
        </div>

        <div style="margin-top:20px;width:100%">
            <crfn:form id="searchForm" beanclass="${actionBean['class'].name}" method="get">

                <div>
                    <label for="timePeriodSelect" class="question inline-left w-100">Time period:</label>
                    <stripes:select id="timePeriodSelect" name="timePeriodUri" value="${actionBean.timePeriodUri}" title="${actionBean.timePeriodUri}">
                        <option value="">-- all periods --</option>
                        <c:forEach items="${actionBean.timePeriods}" var="timePeriod">
                            <stripes:option value="${timePeriod.uri}" label="${timePeriod.skosPrefLabel}" title="${timePeriod.skosPrefLabel}"/>
                        </c:forEach>
                    </stripes:select>
                    <label><img id="loader" src="${pageContext.request.contextPath}/images/animated-loader.gif" style="display:none"/></label>
                </div>

                <div>
                    <label for="freeText" class="question inline-left w-100">Free text:</label>
                    <stripes:text id="freeText" name="freeText"/>&nbsp;<stripes:submit name="search" value="Search" title="Search based on above selections and free text."/>
                </div>

                <div style="display: none">
                    <stripes:hidden name="datasetUri"/>
                </div>
            </crfn:form>
        </div>

        <c:if test="${empty actionBean.indicators}">
            <div style="margin-top:20px;width:100%" class="system-msg">
                No matching indicator references found in this dataset!
            </div>
        </c:if>

        <div id="executed_deletion_sparql" style="display:none">
	        <pre>
                    ${actionBean.executedDeletionSparql}
            </pre>
        </div>

        <c:if test="${not empty actionBean.indicators}">
            <div style="margin-top:20px;width:100%">
                <crfn:form id="deleteForm" beanclass="${actionBean['class'].name}" method="post">

                    <c:set var="deleteAllowed" value='${crfn:userHasPermission(pageContext.session, "/registrations", "u")}'/>

                    <c:if test="${deleteAllowed}">
                        <div style="padding-bottom: 10px">
                            <stripes:submit name="deleteSelected" value="Delete selected" title="Delete only the selected indicators." onclick="return confirm('Are you absolutely sure you want to delete selected indicators from this dataset?');"/>
                            <stripes:submit name="deleteAllMatching" value="Delete all matching" title="Delete all indicators that matched your search." onclick="return confirm('Are you absolutely sure you want to delete matching indicators from this dataset?');"/>
                            <stripes:hidden name="datasetUri"/>
                            <stripes:hidden name="freeText"/>
                            <stripes:hidden name="timePeriodUri"/>
                        </div>
                    </c:if>
                    <display:table name="${actionBean.indicators}" id="indicator" class="sortable" sort="list" pagesize="20" requestURI="${actionBean.urlBinding}" style="width:100%">

                        <display:setProperty name="paging.banner.item_name" value="indicator"/>
                        <display:setProperty name="paging.banner.items_name" value="indicators"/>
                        <display:setProperty name="paging.banner.all_items_found" value='<div class="pagebanner">{0} {1} found.</div>'/>
                        <display:setProperty name="paging.banner.onepage" value=""/>

                        <c:if test="${deleteAllowed}">
                            <display:column style="width:2em;text-align:center">
                                <input type="checkbox" name="selIndicUris" value="${indicator.uri}" title="Select this indicator."/>
                            </display:column>
                        </c:if>

                        <display:column title='<span title="Notation, i.e. the indicator code">Notation</span>' sortable="true" sortProperty="skosNotation" style="width:30%">
                            <stripes:link beanclass="${actionBean.factsheetActionBeanClass.name}" title="${indicator.uri}">
                                <c:out value="${indicator.skosNotation}"/>
                                <stripes:param name="uri" value="${indicator.uri}"/>
                            </stripes:link>
                        </display:column>

                        <display:column title='<span title="The preferred humanly understandable label of the indicator">Label</span>' sortable="true" sortProperty="skosPrefLabel" style="width:70%">
                            <c:out value="${indicator.skosPrefLabel}"/>
                        </display:column>

                </display:table>
                </crfn:form>
            </div>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
