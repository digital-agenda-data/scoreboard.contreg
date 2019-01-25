<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Dataset indicators">

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
            <crfn:form id="indicatorsForm" beanclass="${actionBean['class'].name}" method="get">

                <div>
                    <label for="timePeriodSelect" class="question inline-left w-100">Time period:</label>
                    <stripes:select id="timePeriodSelect" name="timePeriodUri" value="${actionBean.timePeriodUri}" onchange="this.form.submit();" title="${actionBean.timePeriodUri}">
                        <option value="">-- all periods --</option>
                        <c:forEach items="${actionBean.timePeriods}" var="timePeriod">
                            <stripes:option value="${timePeriod.uri}" label="${timePeriod.skosPrefLabel}" title="${timePeriod.skosPrefLabel}"/>
                        </c:forEach>
                    </stripes:select>
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
                No indicator references found in this dataset!
            </div>
        </c:if>

        <c:if test="${not empty actionBean.indicators}">
            <div style="margin-top:20px;width:100%">
                <display:table name="${actionBean.indicators}" id="indicator" class="sortable" sort="list" pagesize="20" requestURI="${actionBean.urlBinding}" style="width:100%">

                    <display:setProperty name="paging.banner.item_name" value="indicator"/>
                    <display:setProperty name="paging.banner.items_name" value="indicators"/>
                    <display:setProperty name="paging.banner.all_items_found" value='<div class="pagebanner">{0} {1} found.</div>'/>
                    <display:setProperty name="paging.banner.onepage" value=""/>

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
            </div>
        </c:if>

    </stripes:layout-component>
</stripes:layout-render>
