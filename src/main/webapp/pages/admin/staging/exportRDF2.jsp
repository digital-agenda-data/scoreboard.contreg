<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Staging databases: export RDF: step 2">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){
                    	
                        // Ensure a property select box title is updated with the hint of the currently selected property.
                        $("select[id$=propertySelect]").change(function() {
                        	
                        	var selectedValue = $("option:selected",this).attr('value');
                        	if (selectedValue == "http://purl.org/linked-data/cube#dataSet") {
                        		if ($('#selDataset').has('[selected]')) {
                        			var fixedDatasetValue = $("option:selected",$('#selDataset')).attr('value');
                        			if (fixedDatasetValue) {
                        				alert("A fixed dataset is already mapped below!");
                        				$(this).val('').change();
                        				return false;
                        			}
                        		}
                        	}
                        	
                            $(this).attr("title", $("option:selected",this).attr('title'));
                            return true;
                        });
                        
                        $("#selDataset").change(function() {
                            
                            var fixedDatasetValue = $("option:selected",this).attr('value');
                            if (fixedDatasetValue) {
                            	var mappedDatasetValue = $("option:selected",$('#variable\\.propertySelect')).attr('value');
                            	if (mappedDatasetValue == "http://purl.org/linked-data/cube#dataSet") {
                            		alert("Dataset already mapped in above mappings!");
                                    $(this).val('').change();
                                    return false;
                            	}
                            }
                            
                            return true;
                        });

                        // Actions for the display/closing of the "create new dataset" popup
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

                        <c:if test="${actionBean.testRun != null && actionBean.testRun.foundMissingConcepts}">
                        // Open the missing concepts popup
                        $("#openMissingConceptsPopup").click(function() {
                            $('#missingConceptsPopup').dialog('open');
                            return false;
                        });

                        // Setup the tables and columns popup
                        $('#missingConceptsPopup').dialog({
                            autoOpen: false,
                            height: 400,
                            width: 800,
                            maxHeight: 800,
                            maxWidth: 800
                        });
                        </c:if>
                    });
            } ) ( jQuery );
        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <%-- The page's heading --%>

        <h1>RDF export: step 2</h1>

        <div style="margin-top:20px">
            <p>
                Your query has been compiled on the database side, and the following selected columns have been detected.<br/>
                For each column, please specify a mapping to the corresponding RDF property.<br/>
                If none of the selected columns is mapped to the "Indicator (code)" property, please also select an indicator from picklist.<br/>
                It is also mandatory to select the dataset where the query's returned objects will go into.<br/>
                Defaults have been selected by the system where possible.
                Mandatory inputs are marked with <img src="${pageContext.request.contextPath}/images/mandatory.gif"/>. Conditional inputs are marked with <img src="${pageContext.request.contextPath}/images/conditional.gif"/>.
            </p>
            <p>
                <strong>NB!</strong>
                It is advised that you click "Test" before you click "Run". This will run the query without actually exporting anything yet,<br/>
                but it will display the first ${actionBean.maxTestResults} rows that the query returned, and you will get a notification if the results contained<br/>
                any concepts for which there is no metadata in the system yet!
            </p>
        </div>

        <%-- The form --%>

        <div style="padding-top:20px">
            <crfn:form id="form1" beanclass="${actionBean['class'].name}" method="post">
                <fieldset>
                    <legend style="font-weight:bold">Query:</legend>
                    <pre style="font-size:0.75em;max-height:130px;overflow:auto"><c:out value="${actionBean.queryConf.query}" /></pre>
                </fieldset>
                <fieldset style="margin-top:20px">

                    <legend style="font-weight:bold">Mapping of observation properties to queried SQL columns:</legend>
                    <table>
                        <c:forEach items="${actionBean.queryConf.propertyMappings}" var="propertyMapping">
                            <tr>
                                <td style="text-align:right;padding-right:10px">
                                    <label for="${propertyMapping.key.id}.columnSelect" <c:if test="${not empty actionBean.requiredProperties[propertyMapping.key.predicate]}">class="required"</c:if> title="${propertyMapping.key.hint}" ><c:out value="${propertyMapping.key.label}"/>:</label>
                                </td>
<%--                                <td>
                                    <stripes:text name="${propertyMapping.key.id}.valueTemplate" value="${actionBean.queryConf.propertyValueTemplates[propertyMapping.key.id]}" size="70" id="${propertyMapping.key.id}.valueTemplateText"/>&nbsp;
                                </td> --%>
                                <td>
                                    <stripes:select name="${propertyMapping.key.id}.column" value="${propertyMapping.value}" id="${propertyMapping.key.id}.columnSelect">
                                        <stripes:option value="" label=""/>
                                        <c:forEach items="${actionBean.selectedColumns}" var="selectedColumn">
                                            <stripes:option value="${selectedColumn}" label="${selectedColumn}" title="${selectedColumn}"/>
                                        </c:forEach>
                                    </stripes:select>
                                </td>
                            </tr>
                        </c:forEach>
                    </table>

                </fieldset>
                
                <fieldset style="margin-top:20px">
                    <legend style="font-weight:bold">Target dataset:</legend>
                    <table>
                        <tr>
                            <td>
                                <stripes:radio id="dynamicDatasetRadio" name="datasetType" value="DYNAMIC" checked="DYNAMIC" />
                            </td>
                            <td>
                                <label for="dynamicDatasetRadio" style="padding-right: 12px;background: url(${pageContext.request.contextPath}/images/conditional.gif) center right no-repeat;">Dataset identifier dynamically expected from this SQL column:</label>
                                <stripes:select name="queryConf.datasetIdentifierColumn" value="${actionBean.queryConf.datasetIdentifierColumn}" id="selDynamicDatasetColumn">
                                    <stripes:option value="" label=" - choose SQL column - "/>
                                    <c:forEach items="${actionBean.selectedColumns}" var="selectedColumn">
                                        <stripes:option value="${selectedColumn}" label="${selectedColumn}" title="${selectedColumn}"/>
                                    </c:forEach>
                                </stripes:select>
                                <stripes:select name="datasetCatalogUri" id="selDatasetCatalog">
	                                <stripes:option value="" label="-- select catalog for datasets --"/>
	                                <c:if test="${not empty actionBean.catalogs}">
	                                    <c:forEach items="${actionBean.catalogs}" var="catalog">
	                                        <stripes:option value="${catalog.left}" label="${catalog.right}"/>
	                                    </c:forEach>
	                                </c:if>
	                            </stripes:select>
                            </td>
                        </tr>
                        <tr>
                            <td style="padding-top:5px">
                                <stripes:radio id="fixedDatasetRadio" name="datasetType" value="FIXED" checked="FIXED" />
                            </td>
                            <td style="padding-top:5px">
                                <label for="fixedDatasetRadio" style="padding-right: 12px;background: url(${pageContext.request.contextPath}/images/conditional.gif) center right no-repeat;">Dataset will be fixed to this:</label>
                                <stripes:select name="queryConf.datasetUriTemplate" value="${actionBean.queryConf.datasetUriTemplate}" id="selDatasetUri">
                                    <stripes:option value="" label=" - choose existing dataset - "/>
                                    <c:forEach items="${actionBean.datasets}" var="dataset">
                                        <stripes:option value="${dataset.left}" label="${dataset.right}"/>
                                    </c:forEach>
                                </stripes:select>&nbsp;&nbsp;<a href="#" id="createNewDatasetLink" title="Opens a pop-up where you can start a brand new dataset.">Create new &#187;</a><br/>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2" style="vertical-align:middle;padding-top:10px">
                                <stripes:label for="chkClear" title="If checked, the contents of the dataset(s) will be cleared before the export runs.">Clear dataset(s) old content:</stripes:label><input type="checkbox" name="clearDataset" id="chkClear" value="true"/>
                            </td>
                        </tr>
                    </table>
                </fieldset>
                
                <div style="margin-top:20px">
                    <stripes:submit name="backToStep1" value="< Back"/>&nbsp;
                    <stripes:submit name="test" id="testButton" value="Test"/>&nbsp;
                    <stripes:submit name="run" id="runButton" value="Run" onclick="return this.form.elements['chkClear'].checked ? confirm('You have chosen to clear the dataset before the export is run? Click OK to confirm, otherwise click Cancel.') : true;"/>&nbsp;
                    <stripes:submit name="cancel" value="Cancel"/>
                </div>
            </crfn:form>

            <c:if test="${actionBean.context.eventName eq 'test' && actionBean.testRun != null && not empty actionBean.testRun.testResults}">

                <div style="width:100%;padding-top:20px">

                    <p>
                        <c:if test="${actionBean.testRun.rowCount > actionBean.maxTestResults}">
                            <strong>Test results (${actionBean.testRun.rowCount} found, displaying first ${actionBean.maxTestResults}):</strong>
                        </c:if>
                        <c:if test="${actionBean.testRun.rowCount <= actionBean.maxTestResults}">
                            <strong>Test results (${actionBean.testRun.rowCount} found):</strong>
                        </c:if>
                        <c:if test="${actionBean.testRun.foundMissingConcepts}">
                            <a href="#" id="openMissingConceptsPopup" class="important-msg" style="float:right">No metadata exists for these found concepts &#187;</a>
                        </c:if>
                    </p>

                    <display:table name="${actionBean.testRun.testResults}" id="testResultRow" class="datatable" sort="list" pagesize="20" requestURI="${actionBean.urlBinding}" style="width:100%;margin-top:20px">

                        <display:setProperty name="paging.banner.item_name" value="row"/>
                        <display:setProperty name="paging.banner.items_name" value="rows"/>
                        <display:setProperty name="paging.banner.all_items_found" value=""/>
                        <display:setProperty name="paging.banner.one_item_found" value=""/>
                        <display:setProperty name="paging.banner.onepage" value=""/>
                        <display:setProperty name="paging.banner.some_items_found" value='<span class="pagebanner">Rows {2} to {3}.</span>'/>

                        <c:forEach items="${testResultRow}" var="testResultRowEntry">
                            <display:column property="${testResultRowEntry.key}" title="${testResultRowEntry.key}"/>
                        </c:forEach>

                    </display:table>

                    <c:if test="${actionBean.testRun.foundMissingConcepts}">
                        <div id="missingConceptsPopup" title="Concepts with no metadta in the system yet">
                            <table class="datatable" style="width:100%">
                                <tr>
                                    <c:if test="${not empty actionBean.testRun.missingIndicators}">
                                        <th>Indicators</th>
                                    </c:if>
                                    <c:if test="${not empty actionBean.testRun.missingBreakdowns}">
                                        <th>Breakdowns</th>
                                    </c:if>
                                    <c:if test="${not empty actionBean.testRun.missingUnits}">
                                        <th>Units</th>
                                    </c:if>
                                    <c:if test="${not empty actionBean.testRun.missingRefAreas}">
                                        <th>Ref. areas</th>
                                    </c:if>
                                </tr>
                                <tr>
                                    <c:if test="${not empty actionBean.testRun.missingIndicators}">
                                        <td>
                                            <c:forEach items="${actionBean.testRun.missingIndicators}" var="missingIndicator">
                                                <ul style="list-style-type:none">
                                                    <li><c:out value="${missingIndicator}"/></li>
                                                </ul>
                                             </c:forEach>
                                        </td>
                                    </c:if>
                                    <c:if test="${not empty actionBean.testRun.missingBreakdowns}">
                                        <td>
                                            <c:forEach items="${actionBean.testRun.missingBreakdowns}" var="missingBreakdown">
                                                <ul style="list-style-type:none">
                                                    <li><c:out value="${missingBreakdown}"/></li>
                                                </ul>
                                            </c:forEach>
                                        </td>
                                    </c:if>
                                    <c:if test="${not empty actionBean.testRun.missingUnits}">
                                        <td>
                                            <c:forEach items="${actionBean.testRun.missingUnits}" var="missingUnit">
                                                <ul style="list-style-type:none">
                                                    <li><c:out value="${missingUnit}"/></li>
                                                </ul>
                                            </c:forEach>
                                        </td>
                                    </c:if>
                                    <c:if test="${not empty actionBean.testRun.missingRefAreas}">
                                        <td>
                                            <c:forEach items="${actionBean.testRun.missingRefAreas}" var="missingRefArea">
                                                <ul style="list-style-type:none">
                                                    <li><c:out value="${missingRefArea}"/></li>
                                                </ul>
                                            </c:forEach>
                                        </td>
                                    </c:if>
                                </tr>
                            </table>
                        </div>
                    </c:if>

                </div>

            </c:if>
        </div>

        <%-- The "create new dataset" popup. Displayed when user clicks on the relevant popup link. --%>

        <div id="createNewDatasetDialog" title="Create a new dataset">
            <stripes:form beanclass="${actionBean['class'].name}" method="post">

                <p>
                    The following properties are sufficient to create a new dataset. The ones mandatory, are marked with <img src="${pageContext.request.contextPath}/images/mandatory.gif"/>.<br/>
                    More information is displayed when placing the mouse over properties' labels.<br/>
                    Once the dataset is created, you can add more properties on the dataset's detailed view page.
                </p>

                <table>
                    <tr>
                        <td><stripes:label for="txtTitle" class="question required" title="The dataset's unique identifier used by the system to distinguish it from others. Only digits, latin letters, underscores and dashes allowed! Will go into the dataset URI and also into the property identified by http://purl.org/dc/terms/identifier">Identifier:</stripes:label></td>
                        <td><stripes:text name="newDatasetIdentifier" id="txtIdentifier" size="60"/></td>
                    </tr>
                    <tr>
                        <td><stripes:label for="txtTitle" class="question required" title="Friendly name of the dataset. Any free text allowed here. Will go into the property identified by http://purl.org/dc/terms/title">Title:</stripes:label></td>
                        <td><stripes:text name="newDatasetTitle" id="txtTitle" size="80"/></td>
                    </tr>
                    <tr>
	                    <td>
	                        <label for="selDatasetCatalog" class="question required">Catalog:</label>
	                    </td>
	                    <td>
	                        <stripes:select name="datasetCatalogUri" id="selDatasetCatalog">
	                            <c:if test="${empty actionBean.catalogs}">
	                                <stripes:option value="" label=" - none found - "/>
	                            </c:if>
	                            <c:if test="${not empty actionBean.catalogs}">
	                                <stripes:option value="" label=""/>
	                                <c:forEach items="${actionBean.catalogs}" var="catalog">
	                                    <stripes:option value="${catalog.left}" label="${catalog.right}"/>
	                                </c:forEach>
	                            </c:if>
	                        </stripes:select>
	                    </td>
	                </tr>
                    <tr>
                        <td><stripes:label for="txtDescription" class="question" title="Humanly understandable detailed description of the dataset. Any free text allowed here. Will go into the property identified by http://purl.org/dc/terms/description">Description:</stripes:label></td>
                        <td>
                            <stripes:textarea name="newDatasetDescription" id="txtDescription" cols="80" rows="10"/>
                        </td>
                    </tr>
                    <tr>
                        <td>&nbsp;</td>
                        <td style="padding-top:10px">
                            <stripes:submit name="createNewDataset" value="Create"/>
                            <input type="button" id="closeCreateNewDatasetDialog" value="Cancel"/>
                        </td>
                    </tr>
                </table>

            </stripes:form>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
