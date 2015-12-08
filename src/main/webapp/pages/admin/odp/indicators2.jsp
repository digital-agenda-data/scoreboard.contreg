<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="ODP datasets packaging">

    <stripes:layout-component name="head">

            <style type="text/css">
                fieldset {
                    padding: 1em;
                }
                #filterFieldset {
                    width:45%;
                    float:left;
                    position:relative;
                }
                #downloadFieldset {
                    width:45%;
                    float:right;
                    position:relative;
                }
                label {
                    font-weight:bold;
                }
                .indFilterLabel {
                    float:left;
                    width:30%;
                    margin-right:0.5em;
                    padding-top:0.2em;
                    text-align:right;
                }
                .indFilterSelect {
                    width:100%;
                    max-width:65%;
                }
                legend {
                    padding: 0.2em 0.5em;
                    border-style:solid;
                    border-width:1px;
                }
            </style>

        <script type="text/javascript">
        // <![CDATA[
        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <%-- The page's heading --%>

        <h1>Package selected indicators into ODP datasets metadata</h1>

        <div style="margin-top:20px">
                <p>
                    This page enables you to ZIP the selected indicators' metadata as datasets metadata for the ODP upload.<br/>
                    Using the indicator groups and sources below, you can filter out the indicators of your interest.<br/>
                    Then you can either choose to ZIP all listed indicators or only the ones you have selected with checkboxes.<br/>
                    As a result, you will get a ZIP file where every indicator is represented by a dataset metadata file for ODP upload.<br/>
                    The ZIP file is ready to be uploaded into ODP's batch upload section.
                </p>
        </div>

        <%-- The section that displays the filters and action buttons. --%>

        <div style="width:100%;padding-top:10px">

            <stripes:form id="filtersForm" method="post" beanclass="${actionBean['class'].name}">

                <div>
                    <fieldset id="filterFieldset">
                        <legend>Indicator filters</legend>
                        <stripes:label for="datasetSelect" class="indFilterLabel">Dataset:</stripes:label>
                        <stripes:select id="datasetSelect" name="filterDataset" value="${actionBean.filterDataset}" class="indFilterSelect" onchange="this.form.submit();">
                            <c:forEach items="${actionBean.datasets}" var="dst">
                                <stripes:option value="${dst.left}" label="${dst.right}" title="${dst.right}"/>
                            </c:forEach>
                        </stripes:select>
                        <br />
                        <stripes:label for="indSourceSelect" class="indFilterLabel">Indicator source:</stripes:label>
                        <stripes:select id="indSourceSelect" name="filterIndSource" value="${actionBean.filterIndSource}" multiple="multiple" size="5" class="indFilterSelect">
                            <c:forEach items="${actionBean.indSources}" var="indSource">
                                <stripes:option value="${indSource.skosNotation}" label="${indSource.skosNotation}" title="${indSource.skosPrefLabel}"/>
                            </c:forEach>
                        </stripes:select>
                        <br/>
                        <stripes:submit name="listIndicators" value="Filter by source(s)" style="position:absolute;bottom:15px;left:10px;"/>
                    </fieldset>

                    <fieldset id="downloadFieldset">
                        <legend>Download options</legend>
                        <input type="radio" checked="checked" name="zipWhich" id="radioZipSelected" value="SELECTED"/>
                        <label for="radioZipSelected">Zip selected</label>
                        <br/>
                        <input type="radio" name="zipWhich" id="radioZipAll" value="ALL"/>
                        <label for="radioZipAll">Zip all matching the filters</label>
                        <br/><br/>
                        <label for="odpActionSelect">Target action in ODP:</label>
                        <br/>
                        <stripes:select id="odpActionSelect" name="odpAction" value="${actionBean.odpAction}">
                            <c:forEach items="${actionBean.odpActions}" var="odpAct">
                                <stripes:option value="${odpAct}" label="${odpAct.label}"/>
                            </c:forEach>
                        </stripes:select>
                        <div style="position:absolute;bottom:10px;right:10px;">
                            <input type="button" onclick="toggleSelectAll('filtersForm');return false" value="Select all" name="selectAll"><br/>
                            <input type="submit" name="download" value="Download"/>
                        </div>
                    </fieldset>

                </div>

                <div style="clear:both;padding-top:20px">
	                <c:if test="${not empty actionBean.filteredIndicators}">

	                    <display:table name="${actionBean.filteredIndicators}" class="sortable" id="indicator" sort="list" pagesize="9999" requestURI="${actionBean.urlBinding}" style="width:100%">

	                        <display:setProperty name="paging.banner.item_name" value="indicator"/>
	                        <display:setProperty name="paging.banner.items_name" value="indicators"/>
	                        <display:setProperty name="paging.banner.all_items_found" value='<div class="pagebanner">{0} {1} found.</div>'/>
	                        <display:setProperty name="paging.banner.onepage" value=""/>

	                        <display:column>
	                            <stripes:checkbox name="selectedIndicators" value="${indicator.uri}" />
	                        </display:column>

	                        <display:column title='<span title="Indicator notation">Notation</span>' sortable="true" sortProperty="skosNotation" style="width:30%">
	                            <stripes:link beanclass="${actionBean.factsheetActionBeanClass.name}" title="${indicator.uri}">
	                                <c:out value="${indicator.skosNotation}"/>
	                                <stripes:param name="uri" value="${indicator.uri}"/>
	                            </stripes:link>
	                        </display:column>

	                        <display:column title='<span title="The preferred humanly understandable label of the indicator">Label</span>' sortable="true" sortProperty="skosPrefLabel" style="width:70%">
	                            <c:out value="${indicator.skosPrefLabel}"/>
	                        </display:column>

	                    </display:table>
	                </c:if>
	                <c:if test="${empty actionBean.filteredIndicators}">
	                    <div class="system-msg">No indicators found!</div>
	                </c:if>
                </div>

                <div style="display:none">
                    <stripes:hidden name="prevFilterDataset" value="${actionBean.filterDataset}"/>
                </div>

            </stripes:form>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
