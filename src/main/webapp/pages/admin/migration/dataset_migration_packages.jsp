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
                    Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.<br/>
                    Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.<br/>
                    Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.<br/>
                    Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
                </p>
        </div>

        <%-- The section that displays the packages list. --%>

        <c:if test="${not empty actionBean.packages}">

            <div style="width:75%;padding-top:10px">
            
                <stripes:form id="packagesForm" method="post" beanclass="${actionBean['class'].name}">

                    <display:table name="${actionBean.packages}" class="sortable" id="pack" sort="list" requestURI="${actionBean.urlBinding}" style="width:100%">
                        <display:column style="width:5%">
                            <stripes:checkbox name="selectedPackages" value="${pack.name}" />
                        </display:column>
                        <display:column title="Package" property="name" sortable="true" style="width:50%"/>
                        <display:column title="Started" sortable="true" sortProperty="started" style="width:25%">
                            <fmt:formatDate value="${pack.started}" pattern="yyyy-MM-dd HH:mm:ss" />
                        </display:column>
                        <display:column title="Finished" sortable="true" sortProperty="finished" style="width:25%">
                            <fmt:formatDate value="${pack.finished}" pattern="yyyy-MM-dd HH:mm:ss" />
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
                Select the dataset to migrate and give a meaningful package name that will help you to distinguish it from other packages.<br/>
                Package name will be used as corresponding directory name in CR file system, so it must only contain Latin characters,<br/>
                digits, underscores, dashes or periods. An example name format: "dataset-user-datetime".
            </p>        
            <stripes:form beanclass="${actionBean['class'].name}" method="post">

                <table>
                    <tr>
                        <td><stripes:label for="txtName" class="question required" title="Meaningful package name to help you distinguish it from others. Use only characters allowed in file names!">Name:</stripes:label></td>
                        <td><stripes:text name="newPackage.name" id="txtName" size="75"/></td>
                    </tr>
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
                            <stripes:submit name="createNewPackage" value="Create"/>
                            <input type="button" id="closeCreateNewDialog" value="Cancel"/>
                        </td>
                    </tr>
                </table>

            </stripes:form>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
