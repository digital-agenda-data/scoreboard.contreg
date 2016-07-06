<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Dataset migrations">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){
                        
                        var migratablePackagesJSON = ${actionBean.migratablePackagesJSON};

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
                        
                        $("#selSourceCr").change(function() {
                            
                            var $select = $('#selSourcePackage'); 
                            $select.find('option').remove();
                            
                            var packages = migratablePackagesJSON[$(this).val()];
                            if (packages == undefined || packages == null || packages.length == 0) {
                                $select.append('<option value="">-- no packages found in this CR --</option>');
                            } else {
                                var i = 0;
                                $.each(packages, function() {
                                    i++;
                                    if (i == 1) {
                                        $select.append('<option selected="selected" value="' + this['identifier'] + '" title="' + this['datasetUri'] + '">' + this['identifier'] + '</option>');
                                        $select.prop('title', this['datasetUri']);
                                        $('#txtTargetDatasetUri').val(this['datasetUri']);
                                        $('#txtTargetDatasetUri').prop('title', this['datasetUri']);
                                        $('#hiddenTargetDatasetUri').val(this['datasetUri']);
                                    } else {
                                        $select.append('<option value="' + this['identifier'] + '" title="' + this['datasetUri'] + '">' + this['identifier'] + '</option>');
                                    }
                                });
                            }
                            $select.trigger("chosen:updated");
                            $select.removeAttr("disabled");
                        });
                        
                        $("#selSourcePackage").change(function() {
                            $(this).attr("title", $("option:selected",this).attr('title'));
                            $('#txtTargetDatasetUri').val($(this).attr('title'));
                            $('#txtTargetDatasetUri').prop('title', $(this).attr('title'));
                            $('#hiddenTargetDatasetUri').val($(this).attr('title'));
                            return true;
                        });
                        
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
                    This page lists dataset migrations executed on this CR instance. To start a new migration, use the <em>Operations</em> menu.<br/>
                    A question mark in migration status indicates an unfinished migration. Refresh the page to check if it's finished.
                </p>
        </div>

        <%-- The section that displays the migrations list. --%>

        <c:if test="${not empty actionBean.migrations}">

            <div style="width:100%;padding-top:10px">
            
                <stripes:form id="migrationsForm" method="post" beanclass="${actionBean['class'].name}">
                
                    <display:table name="${actionBean.migrations}" class="sortable" id="migration" sort="list" pagesize="9" requestURI="${actionBean.urlBinding}" style="width:100%">
                        <display:column style="width:3%">
                            <stripes:checkbox name="selectedMigrations" value="${migration.id}" />
                        </display:column>
                        <display:column title="Migration package &amp; target dataset" sortProperty="sourcePackageIdentifier" sortable="true" style="width:74%;font-size:0.85em">
                            <ul style="list-style:none; display: inline-block; padding: 0; margin: 0;">
                                <li title="Source: ${migration.sourceCrUrl}">Package: <strong><c:out value="${migration.sourcePackageIdentifier}"/></strong></li>
                                <li>Dataset: <strong><c:out value="${migration.targetDatasetUri}"/></strong></li>
                            </ul>
                        </display:column>
                        <display:column title="Started" sortProperty="startedTime" sortable="true" style="width:15%;font-size:0.85em">
                            <ul style="list-style:none; display: inline-block; padding: 0; margin: 0;">
                                <li><fmt:formatDate value="${migration.startedTime}" pattern="yy-MM-dd HH:mm:ss" /></li>
                                <li>by <i><c:out value="${migration.userName}"/></i></li>
                            </ul>
                        </display:column>
                        <display:column title="Status" sortProperty="status" sortable="true" style="width:8%;font-size:0.85em;text-align:center;">
                            <img src="${pageContext.request.contextPath}/images/status_${fn:toLowerCase(migration.status)}.png" alt="${migration.status}" title="${migration.failed ? fn:escapeXml(migration.messages) : migration.status}"/>
                        </display:column>
                    </display:table>
<%--
                    <display:table name="${actionBean.migrations}" class="sortable" id="migration" sort="list" requestURI="${actionBean.urlBinding}" style="width:100%">
                        <display:column style="width:3%">
                            <stripes:checkbox name="selectedMigrations" value="${migration.id}" />
                        </display:column>
                        <display:column title="Package identifier" property="sourcePackageIdentifier" sortable="true" style="width:30%;font-size:0.8em"/>
                        <display:column title="Target dataset" property="targetDatasetUri" sortable="true" style="width:34%;font-size:0.8em"/>                       
                        <display:column title="User" property="userName" sortable="true" style="width:9%;font-size:0.8em"/>
                        <display:column title="Started" sortable="true" sortProperty="startedTime" style="width:12%;font-size:0.8em">
                            <fmt:formatDate value="${migration.startedTime}" pattern="yy-MM-dd HH:mm:ss" />
                        </display:column>
                        <display:column title="Finished" sortable="true" sortProperty="finishedTime" style="width:12%;font-size:0.8em">
                            <fmt:formatDate value="${migration.finishedTime}" pattern="yy-MM-dd HH:mm:ss" />
                            <c:if test="${migration.failed}">
                                <img src="${pageContext.request.contextPath}/images/exclamation.png" alt="Failure" title="${fn:escapeXml(migration.messages)}"/>
                            </c:if>
                        </display:column>
                    </display:table> --%>

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

                <div style="padding-top:10px;">
                    <stripes:label for="selSourceCr" class="question required">Select source CR to migrate from:</stripes:label><br/>
                    <stripes:select name="newMigration.sourceCrUrl" id="selSourceCr" value="${actionBean.newMigration.sourceCrUrl}">
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
                </div>
                     
                <div style="padding-top:10px;">
                 
                    <stripes:label for="selSourcePackage" class="question required">Select source package to migrate from:</stripes:label><br/>
                    <stripes:select name="newMigration.sourcePackageIdentifier" id="selSourcePackage" value="${actionBean.newMigration.sourcePackageIdentifier}" disabled="${empty actionBean.newMigration.sourceCrUrl}" title="${actionBean.newMigration.targetDatasetUri}">
                      
                        <c:choose>
                            <c:when test="${not empty actionBean.newMigration.sourceCrUrl && not empty actionBean.migratablePackagesMap[actionBean.newMigration.sourceCrUrl]}">
                                <c:forEach items="${actionBean.migratablePackagesMap[actionBean.newMigration.sourceCrUrl]}" var="packageDTO">
                                    <stripes:option value="${packageDTO.identifier}" label="${packageDTO.identifier}" title="${packageDTO.datasetUri}"/>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <stripes:option value="" label=" -- no packages to select from -- " style="font-size:0.8em;"/>
                            </c:otherwise>
                        </c:choose>
                       
                    </stripes:select>
                </div>
                
                <div style="padding-top:10px;">
                
                    <stripes:label for="txtTargetDatasetUri" class="question required">Selected package will be migrated into this dataset:</stripes:label><br/>
                    <input type="text" id="txtTargetDatasetUri" name="dummy" disabled="disabled" size="100" style="font-size:0.75em" value="${actionBean.newMigration.targetDatasetUri}" title="${actionBean.newMigration.targetDatasetUri}"/>
                    <input type="hidden" id="hiddenTargetDatasetUri" name="newMigration.targetDatasetUri" value="${actionBean.newMigration.targetDatasetUri}"/>
                 
                    <br/>
                    <input type="hidden" name="newMigration.prePurge" value="true"/>
                </div>
                                                 
                <div style="padding-top:20px">
                    <stripes:submit id="startNewSubmit" name="startNewMigration" value="Start"/>
                    <input type="button" id="closeStartNewDialog" value="Cancel"/>
                </div>

            </stripes:form>
        </div>
        
    </stripes:layout-component>
</stripes:layout-render>
