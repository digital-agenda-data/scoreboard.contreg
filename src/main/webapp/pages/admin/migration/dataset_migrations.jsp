<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Dataset migrations">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){

                        $("#startNewLink").click(function() {
                            $('#startNewDialog').dialog('option','width', 800);
                            $('#startNewDialog').dialog('open');
                            return false;
                        });

                        $('#startNewDialog').dialog({
                            autoOpen: ${param.startNewValidationErrors ? 'true' : 'false'},
                            width: 800
                        });

                        $("#closeStartNewDialog").click(function() {
                            $('#startNewDialog').dialog("close");
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
                    <li><a href="#" id="startNewLink" title="Start new migration">Start new migration</a></li>
                </ul>
            </li>
        </ul>

        <%-- The page's heading --%>

        <h1>Dataset migrations</h1>

        <div style="margin-top:20px">
                <p>
                    Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.<br/>
                    Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.<br/>
                    Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.<br/>
                    Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
                </p>
        </div>

        <%-- The section that displays the migrations list. --%>

        <c:if test="${not empty actionBean.migrations}">

            <div style="width:100%;padding-top:10px">
            
                <stripes:form id="migrationsForm" method="post" beanclass="${actionBean['class'].name}">

                    <display:table name="${actionBean.migrations}" class="sortable" id="migration" sort="list" requestURI="${actionBean.urlBinding}" style="width:100%">
                        <display:column style="width:5%">
                            <stripes:checkbox name="selectedPackages" value="${migration.id}" />
                        </display:column>
                        <display:column title="User" property="userName" sortable="true" style="width:40%"/>
                        <display:column title="Started" sortable="true" sortProperty="startedTime" style="width:27%">
                            <fmt:formatDate value="${migration.startedTime}" pattern="yy-MM-dd HH:mm:ss" />
                        </display:column>
                        <display:column title="Finished" sortable="true" sortProperty="finishedTime" style="width:18%">
                            <fmt:formatDate value="${migration.finishedTime}" pattern="yy-MM-dd HH:mm:ss" />
                            <c:if test="${migration.failed}">
                                <img src="${pageContext.request.contextPath}/images/exclamation.png" alt="Failure" title="${fn:escapeXml(migration.messages)}"/>
                            </c:if>
                        </display:column>
                    </display:table>

                    <stripes:submit name="delete" value="Delete" />
                    <input type="button" onclick="toggleSelectAll('migrationsForm');return false" value="Select all" name="selectAll">

                </stripes:form>
                
            </div>
            
        </c:if>

        <%-- Message if no migrations found. --%>

        <c:if test="${empty actionBean.migrations}">
            <div class="system-msg">No performed migrations found in the database yet! Please use the dropdown menu to start new.</div>
        </c:if>
        
        <%-- --%>
        
        <div id="startNewDialog" title="Start new dataset migration">

            <stripes:form id="startNewForm" beanclass="${actionBean['class'].name}" method="post">

                <table>
                    <tr>
                        <td>
	                        <stripes:label for="selSourceCr" class="question required">Select source CR to migrate from:</stripes:label><br/>
	                        <stripes:select name="newPackage.sourceCrUrl" id="selSourceCr" value="${actionBean.newMigration.sourceCrUrl}">
	                            <c:if test="${empty actionBean.sourceCrs}">
	                                <stripes:option value="" label=" - none found - "/>
	                            </c:if>
	                            <c:if test="${not empty actionBean.sourceCrs}">
	                                <stripes:option value="" label=""/>
	                                <c:forEach items="${actionBean.sourceCrs}" var="sourceCr">
	                                    <stripes:option value="${sourceCr.url}" label="${sourceCr.name} (${sourceCr.url})" title="${sourceCr.url}"/>
	                                </c:forEach>
	                            </c:if>
	                        </stripes:select>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding-top:10px">
                            <stripes:submit id="startNewSubmit" name="startNewPackage" value="Create"/>
                            <input type="button" id="closeStartNewDialog" value="Cancel"/>
                        </td>
                    </tr>
                </table>

            </stripes:form>
        </div>
        
    </stripes:layout-component>
</stripes:layout-render>
