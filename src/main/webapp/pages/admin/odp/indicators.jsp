<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="ODP datasets packaging">

    <stripes:layout-component name="head">
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

                <div style="padding-bottom:20px">
                    <table>
                        <tr>
                            <td>
                                <stripes:label for="indGroupSelect" class="question">Indicator groups:</stripes:label><br/>
                                <stripes:select id="indGroupSelect" name="filterIndGroup" value="${actionBean.filterIndGroup}" size="7" multiple="multiple">
                                    <c:forEach items="${actionBean.indGroups}" var="indGroup">
                                        <stripes:option value="${indGroup.skosNotation}" label="${indGroup.skosNotation}" title="${indGroup.skosPrefLabel}"/>
                                    </c:forEach>
                                </stripes:select>
                            </td>
                            <td style="padding-left:20px">
                                <stripes:label for="indSourceSelect" class="question">Indicator sources:</stripes:label><br/>
                                <stripes:select id="indSourceSelect" name="filterIndSource" value="${actionBean.filterIndSource}" size="7" multiple="multiple">
                                    <c:forEach items="${actionBean.indSources}" var="indSource">
                                        <stripes:option value="${indSource.skosNotation}" label="${indSource.skosNotation}" title="${indSource.skosPrefLabel}"/>
                                    </c:forEach>
                                </stripes:select>
                            </td>
                        </tr>
                    </table>

                    <stripes:submit name="listIndicators" value="Apply filters" title="Apply selected filters."/>
                    <c:if test="${not empty actionBean.filteredIndicators}">
                        <stripes:submit name="zipSelected" value="ZIP selected" title="ZIP indicators you have marked with checkboxes."/>
                        <stripes:submit name="zipAll" value="ZIP all matching" title="ZIP all indicators matching the filters you have applied."/>
                        <input type="button" onclick="toggleSelectAll('filtersForm');return false" value="Select all" name="selectAll">
                    </c:if>
                </div>

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

            </stripes:form>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
