<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Upload a spreadsheet">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[

            ( function($) {
                $(document).ready(
                    function(){

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
                        
                        $("#createNewDatasetSubmit").click(function() {
                            $('#hiddenUploadType').val($('#selContentType').val());
                            return true;
                        });
                    });
            } ) ( jQuery );

            function typeChanged(selectObj){
                var value = selectObj.options[selectObj.selectedIndex].value;
                if (value!= null && value.startsWith('OBSERVATION_')) {
                    document.getElementById("graphRow").style.display = 'none';
                    document.getElementById("datasetRow").style.display = '';
                }
                else {
                    document.getElementById("graphRow").style.display = '';
                    document.getElementById("datasetRow").style.display = 'none';
                }
            }
        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <%-- The page's heading --%>

        <h1>Upload a spreadsheet</h1>

        <div style="margin-top:20px">
            This page enables you to upload an MS Excel or OpenDocument spreadsheet into CR's triple store.<br/>
            Only files of certain type of content are supported, meaning that CR knows how to map these into the triple store.<br/>
            You must specify one of these types below and upload a spreadsheet file from your computer.<br/>
            If the file is not a supported spreadsheet file or there is a problem with mapping into the triple store,<br/>
            the system returns a relevant error message and rolls back any changes made!
        </div>

        <%-- The form --%>

        <div style="padding-top:20px">
            <crfn:form id="uploadForm" beanclass="${actionBean['class'].name}" method="post">

                <table>
                    <tr>
                        <td style="text-align:right">
                            <label for="selContentType" class="question required">Content type:</label>
                        </td>
                        <td>
                            <stripes:select id="selContentType" name="uploadType" title="${actionBean.uploadType.hint}" onchange="typeChanged(this)">
                                <c:forEach items="${actionBean.uploadTypes}" var="uploadType">
                                    <stripes:option value="${uploadType.name}" label="${uploadType.title}" title="${uploadType.hint}"/>
                                </c:forEach>
                            </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td style="text-align:right">
                            <label for="fileInput" class="question required">Spreadsheet file:</label>
                        </td>
                        <td>
                           <stripes:file name="fileBean" id="fileInput" size="120"/>
                        </td>
                    </tr>
                    <tr id="graphRow" ${fn:startsWith(actionBean.uploadType, 'OBSERVATION_') ? 'style="display:none"' : ''}>
                        <td>
                            &nbsp;
                        </td>
                        <td>
                            <stripes:checkbox name="clearGraph" id="chkClearGraph"/>&nbsp;<label for="chkClearGraph">Clear all previous content of selected type</label>
                        </td>
                    </tr>
                    <tr id="datasetRow" ${fn:startsWith(actionBean.uploadType, 'OBSERVATION_') ? '' : 'style="display:none"'}>
                        <td>
                            <label for="selDataset" class="question required">Target dataset:</label>
                        </td>
                        <td>
                            <stripes:select name="targetDataset" id="selDataset">
                                <c:if test="${empty actionBean.datasets}">
                                    <stripes:option value="" label=" - none found - "/>
                                </c:if>
                                <c:if test="${not empty actionBean.datasets}">
                                    <stripes:option value="" label=""/>
                                    <c:forEach items="${actionBean.datasets}" var="dataset">
                                        <stripes:option value="${dataset.left}" label="${dataset.right}"/>
                                    </c:forEach>
                                </c:if>
                            </stripes:select>&nbsp;&nbsp;<a href="#" id="createNewDatasetLink" title="Opens a pop-up where you can start a brand new dataset.">Create new &#187;</a><br/>
                            <stripes:checkbox name="clearDataset" id="chkClearDataset"/>&nbsp;<label for="chkClearDataset">Clear dataset before upload</label>
                        </td>
                    </tr>
                    <tr>
                        <td>
                           &nbsp;
                        </td>
                        <td style="padding-top:10px">
                            <stripes:submit name="upload" value="Upload"/>
                            <stripes:submit name="cancel" value="Cancel"/>
                        </td>
                    </tr>
                </table>

                <c:if test="${not empty actionBean.touchedGraphs}">
                    <div class="tip-msg">
                        <strong>Tip</strong>
                        <p>
                            All extracted content was imported into the following graph(s). Please click on the links below to explore further:
                        </p>
                        <ul style="list-style:none;margin:0;padding:0;">
                         <c:forEach items="${actionBean.touchedGraphs}" var="touchedGraphUri">
                            <li>
                                <stripes:link beanclass="${actionBean.objectsInSourceActionBeanClass.name}">
                                    <stripes:param name="search" value=""/>
                                    <stripes:param name="uri" value="${touchedGraphUri}"/>
                                    <c:if test="${fn:contains(touchedGraphUri, '/data/')}">
                                        <stripes:param name="factsheetUri" value="${fn:replace(touchedGraphUri, '/data/','/dataset/')}"/>
                                    </c:if>
                                    <c:out value="${touchedGraphUri}"/>
                           </stripes:link>
                            </li>
                         </c:forEach>
                        </ul>
                    </div>
                </c:if>

            </crfn:form>
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
                            <stripes:submit id="createNewDatasetSubmit" name="createNewDataset" value="Create"/>
                            <input type="button" id="closeCreateNewDatasetDialog" value="Cancel"/>
                        </td>
                    </tr>
                </table>
                
                <div style="display:none;">
                    <stripes:hidden id="hiddenUploadType" name="uploadType"/>
                </div>

            </stripes:form>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
