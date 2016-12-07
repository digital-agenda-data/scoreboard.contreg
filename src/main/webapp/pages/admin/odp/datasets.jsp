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

        <h1>Pack selected datasets for ODP upload</h1>

		<div style="margin-top:20px">
		    <p>
		        This page enables you to ZIP selected datasets' metadata for uploading into ODP (Open Data Portal).<br/>
		        Use the below controls to select and pack the desired datasets.<br/>
		        The selected ODP target action will be written into the package and automatically recognized by the ODP.
		    </p>
		</div>

        <%-- The section that displays the input controls and action buttons. --%>

        <div style="width:100%;padding-top:10px">

            <stripes:form id="filtersForm" method="post" beanclass="${actionBean['class'].name}">

                <div>
                    <div>
		                <label for="odpActionSelect">Target action in ODP:</label>
		                <stripes:select id="odpActionSelect" name="odpAction" value="${actionBean.odpAction}">
		                    <c:forEach items="${actionBean.odpActions}" var="odpAct">
		                        <stripes:option value="${odpAct}" label="${odpAct.label}"/>
		                    </c:forEach>
		                </stripes:select>
		            </div>
		            <div style="padding-top:10px">
                        <input type="submit" name="packAll" value="Pack all"/>
                        <input type="submit" name="packSelected" value="Pack selected"/>
                    </div>
                </div>

                <div style="clear:both;padding-top:20px">
                    <c:if test="${not empty actionBean.datasets}">

                        <display:table name="${actionBean.datasets}" class="sortable" id="dataset" sort="list" pagesize="20" requestURI="${actionBean.urlBinding}" style="width:100%">

                            <display:setProperty name="paging.banner.item_name" value="datatset"/>
                            <display:setProperty name="paging.banner.items_name" value="datatsets"/>
                            <display:setProperty name="paging.banner.all_items_found" value='<div class="pagebanner">{0} {1} found.</div>'/>
                            <display:setProperty name="paging.banner.onepage" value=""/>

                            <display:column>
                                <stripes:checkbox name="selectedDatasets" value="${dataset.left}" />
                            </display:column>

                            <display:column title='<span title="Dataset label/title/notation">Label</span>' sortable="true" sortProperty="right" style="width:30%">
                                <stripes:link beanclass="${actionBean.factsheetActionBeanClass.name}" title="${dataset.left}">
                                    <c:out value="${dataset.right}"/>
                                    <stripes:param name="uri" value="${dataset.left}"/>
                                </stripes:link>
                            </display:column>

                            <display:column title='<span title="Dataset URI">URI</span>' sortable="true" sortProperty="left" style="width:70%">
                                <c:out value="${dataset.left}"/>
                            </display:column>

                        </display:table>
                    </c:if>
                    <c:if test="${empty actionBean.datasets}">
                        <div class="system-msg">No datasets found!</div>
                    </c:if>
                </div>

            </stripes:form>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
