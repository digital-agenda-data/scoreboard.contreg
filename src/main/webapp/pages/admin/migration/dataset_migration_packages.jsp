<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Dataset migration packages">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){

                        $("#createNewLink").click(function() {
                            $('#createNewDialog').dialog('option','width', 800);
                            $('#createNewDialog').dialog('open');
                            return false;
                        });

                        $('#createNewDialog').dialog({
                            autoOpen: ${param.createNewValidationErrors ? 'true' : 'false'},
                            width: 800
                        });

                        $("#closeCreateNewDialog").click(function() {
                            $('#createNewDialog').dialog("close");
                            return true;
                        });

                        ////////////////////////////////////////////
                        
                    });
            }) ( jQuery );
        // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <%-- Drop-down operations --%>

        <ul id="dropdown-operations">
            <li><a href="#">Operations</a>
                <ul>
                    <li><a href="#" id="createNewLink" title="Create new migration package">New package</a></li>
                </ul>
            </li>
        </ul>

        <%-- The page's heading --%>

        <h1>Dataset migration packages prepared in this CR</h1>

        <div style="margin-top:20px">
                <p>
                    This page lists already created dataset migration packages and enables to create new ones from the Operations menu.<br/>
                    If a migration package does not have a Finished date, but you think it should be finished, it was probably interrupted<br/>
                    in an unrecoverable way or it has hanged for some reason. In such cases you can delete the package by the help of checkboxes<br/>
                    and the "Delete"" button.
                </p>
        </div>

        <%-- The section that displays the packages list. --%>

        <c:if test="${not empty actionBean.packages}">

            <div style="width:100%;padding-top:10px">
            
                <stripes:form id="packagesForm" method="post" beanclass="${actionBean['class'].name}">

                    <display:table name="${actionBean.packages}" class="sortable" id="pack" sort="list" requestURI="${actionBean.urlBinding}" style="width:100%">
                        <display:column style="width:4%">
                            <stripes:checkbox name="selectedPackages" value="${pack.identifier}" />
                        </display:column>
                        <display:column title="Package" property="identifier" sortable="true" style="width:60%"/>
                        <display:column title="Started" sortable="true" sortProperty="started" style="width:18%">
                            <fmt:formatDate value="${pack.started}" pattern="yy-MM-dd HH:mm:ss" />
                        </display:column>
                        <display:column title="Finished" sortable="true" sortProperty="finished" style="width:18%">
                            <fmt:formatDate value="${pack.finished}" pattern="yy-MM-dd HH:mm:ss" />
                            <c:if test="${not empty pack.finishedErrorMessage}">
                                <img src="${pageContext.request.contextPath}/images/exclamation.png" alt="Errors" title="${fn:escapeXml(pack.finishedErrorMessage)}"/>
                            </c:if>
                        </display:column>
                    </display:table>

                    <stripes:submit name="delete" value="Delete" />
                    <input type="button" onclick="toggleSelectAll('packagesForm');return false" value="Select all" name="selectAll">

                </stripes:form>
                
            </div>
            
        </c:if>

        <%-- Message if no packages found. --%>

        <c:if test="${empty actionBean.packages}">
            <div class="system-msg">No migration packages found! Please use the dropdown menu to create new.</div>
        </c:if>
        
        <%-- --%>
        
        <div id="createNewDialog" title="Create new dataset migration package">

            <p>
                Select the dataset to migrate.<br/>
                Migration package identifier will be generated from dataset identifier, username and current datetime.
            </p>        
            <stripes:form id="createNewForm" beanclass="${actionBean['class'].name}" method="post">

                <table>
                    <tr>
                        <td><stripes:label for="selDataset" class="question required">Dataset:</stripes:label></td>
                        <td>
                            <stripes:select name="newPackage.datasetUri" id="selDataset" value="${actionBean.newPackage.datasetUri}">
                                <c:if test="${empty actionBean.datasets}">
                                    <stripes:option value="" label=" - none found - "/>
                                </c:if>
                                <c:if test="${not empty actionBean.datasets}">
                                    <stripes:option value="" label=""/>
                                    <c:forEach items="${actionBean.datasets}" var="dataset">
                                        <stripes:option value="${dataset.left}" label="${dataset.right}" title="${dataset.left}"/>
                                    </c:forEach>
                                </c:if>
                            </stripes:select><br/>
                            <img src="${pageContext.request.contextPath}/images/info_icon.gif" alt="Tip"/><span style="font-size:0.75em;color:#8C8C8C;">Hold mouse over dropdown options to see dataset URIs!</span>
                        </td>
                    </tr>
                    <tr>
                        <td>&nbsp;</td>
                        <td style="padding-top:10px">
                            <stripes:submit id="createNewSubmit" name="createNewPackage" value="Create"/>
                            <input type="button" id="closeCreateNewDialog" value="Cancel"/>
                        </td>
                    </tr>
                </table>

            </stripes:form>
        </div>
        
    </stripes:layout-component>
</stripes:layout-render>
