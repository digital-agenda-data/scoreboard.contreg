<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Resource properties">

    <stripes:layout-component name="contents">
        <script language="javascript">
// <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){

                        // The edit-property-value dialog setup.
                        $(".edit-property-opener").click(function() {

                            var propertyUri = $(this).data('property-uri');
                            var propertyLabel = $(this).data('property-label');
                            var propertyValue = $(this).data('property-value');
                            var propertyValueMd5 = $(this).data('property-value-md5');
                            var sourceUri = $(this).data('source-uri');

                            $('#editDialog').find('#property-label').text(propertyLabel);
                            $('#editDialog').find('#property-value').val(propertyValue);
                            $('#editDialog').find('#input-predicate-uri').val(propertyUri);
                            $('#editDialog').find('#input-source-uri').val(sourceUri);
                            $('#editDialog').find('#input-old-property-value-md5').val(propertyValueMd5);

                            $('#editDialog').dialog('option','width', 700);
                            $('#editDialog').dialog('open');

                            return false;
                        });
                        $('#editDialog').dialog({
                            autoOpen: false,
                            width: 700
                        });
                        $("#closeEditDialog").click(function() {
                            $('#editDialog').dialog("close");
                            return true;
                        });

                        // onClick handler for add-bookmark link (open add-bookmark dialog)
                        $("#add_bookmark").click(function() {
                            $('#bookmark_dialog').dialog('open');
                            return false;
                        });

                        // The add-bookmark dialog setup
                        $('#bookmark_dialog').dialog({
                            autoOpen: false,
                            width: 500
                        });

                        // onClick handler for submit button in add-bookmark dialog that closes the dialog
                        $("#bookmark_form_submit").click(function() {
                            $('#bookmark_dialog').dialog("close");
                            return true;
                        });

                         // onClick handler for add-to_dataset link (open add_dataset dialog)
                        $("#add_to_dataset").click(function() {
                            $('#dataset_dialog').dialog('open');
                            return false;
                        });

                        // The dataset dialog setup
                        $('#dataset_dialog').dialog({
                            autoOpen: false,
                            width: 400
                        });

                        // onClick handler for submit button in dataset dialog that closes the dialog
                        $("#dataset_form_submit").click(function() {
                            $('#dataset_dialog').dialog("close");
                            return true;
                        });

                        // onClick handler for links that open full values of predicate objects
                        // in a dialog window (used when the full value is too long to display on factsheet)
                        $("[id^=predObjValueLink]").click(function() {
                            $('#predObjValueDialog').dialog('open');
                            $('#predObjValueDialog').append('<img src="${pageContext.request.contextPath}/images/wait.gif" alt="Wait clock"/>');
                            $('#predObjValueDialog').load($(this).attr("href"));
                            return false;
                        });

                        // Setup of the modal dialog that displays full value of objects that are too long
                        // to display on factsheet.
                        $('#predObjValueDialog').dialog({
                            autoOpen: false,
                            height: 500,
                            width: 700,
                            maxHeight: 800,
                            maxWidth: 800,
                            modal: true,
                            closeOnEscape: true
                        });

                        // Actions for the display/closing of the "Change dataset status" popup
                        $("#changeDatasetStatusLink").click(function() {
                            $('#changeDatasetStatusDialog').dialog('option','width', 800);
                            $('#changeDatasetStatusDialog').dialog('open');
                            return false;
                        });

                        $('#changeDatasetStatusDialog').dialog({
                            autoOpen: false,
                            width: 800
                        });

                        $("#closeChangeDatasetStatusDialog").click(function() {
                            $('#changeDatasetStatusDialog').dialog("close");
                            return true;
                        });
                    });
            } ) ( jQuery );
// ]]>
        </script>
        <c:choose>
            <c:when test="${!actionBean.noCriteria}">

                <cr:tabMenu tabs="${actionBean.tabs}" />

                <br style="clear:left" />

                <c:choose>
                    <c:when test="${actionBean.subject!=null}">

                        <c:set var="subjectUrl" value="${actionBean.subject.url}"/>
                        <c:set var="subjectUri" value="${actionBean.subject.uri}"/>

                        <c:set var="registrationsAllowed" value='${crfn:userHasPermission(pageContext.session, "/registrations", "u")}'/>

                        <c:set var="editAllowed" value="${registrationsAllowed && !subject.anonymous}"/>
                        <c:set var="harvestAllowed" value="${editAllowed && subjectUrl!=null && !actionBean.currentlyHarvested && !actionBean.uriIsFolder}"/>
                        <c:set var="sourceReadActionsAllowed" value="${actionBean.uriIsHarvestSource}"/>
                        <c:set var="uriIsGraph" value="${actionBean.uriIsGraph}"/>
                        <c:set var="uriIsDataCubeDataset" value="${actionBean.dataCubeDataset}"/>
                        <c:set var="uriIsScoreboardCodelist" value="${actionBean.scoreboardCodelist}"/>
                        <c:set var="downloadAllowed" value="${actionBean.subjectDownloadable}"/>
                        <c:set var="addBookmarkAllowed" value="${registrationsAllowed && !actionBean.subjectIsUserBookmark}"/>
                        <c:set var="removeBookmarkAllowed" value="${actionBean.subjectIsUserBookmark}"/>
                        <c:set var="addReviewAllowed" value="${registrationsAllowed && subjectUrl!=null}"/>
                        <c:set var="addToCompiledDataset" value="${sourceReadActionsAllowed && actionBean.adminLoggedIn && !actionBean.compiledDataset && not empty actionBean.userCompiledDatasets}"/>

                        <c:set var="displayOperations" value="${editAllowed || harvestAllowed || sourceReadActionsAllowed || uriIsGraph || uriIsDataCubeDataset || uriIsScoreboardCodelist || downloadAllowed || addBookmarkAllowed || removeBookmarkAllowed || addReviewAllowed}"/>

                        <c:if test="${displayOperations}">
                            <ul id="dropdown-operations">
                                <li><a href="#">Operations</a>
                                    <ul>
                                        <c:if test="${editAllowed}">
                                            <li>
                                                <stripes:link class="link-plain" href="/factsheet.action" event="${actionBean.context.eventName=='edit' ? 'view' : 'edit'}">${actionBean.context.eventName=='edit' ? 'View' : 'Edit'}
                                                    <stripes:param name="uri" value="${subjectUri}"/>
                                                </stripes:link>
                                            </li>
                                        </c:if>
                                        <c:if test="${harvestAllowed && !actionBean.compiledDataset}">
                                            <li>
                                                <stripes:url value="${actionBean.urlBinding}" event="harvestAjax" var="url">
                                                        <stripes:param name="uri" value="${actionBean.uri}"/>
                                                </stripes:url>
                                                <stripes:url value="${actionBean.urlBinding}" event="harvest" var="oldUrl">
                                                        <stripes:param name="uri" value="${actionBean.uri}"/>
                                                </stripes:url>
                                                <a id="wait_link" href="${oldUrl}" onclick="javascript:loadAndWait('The resource is being harvested. Please wait ...', '${url}', '${pageContext.request.contextPath}'); return false;">Harvest</a>
                                            </li>
                                        </c:if>
                                        <c:if test="${editAllowed && uriIsDataCubeDataset}">
                                            <li>
                                                <a href="#" id="changeDatasetStatusLink" title="Change the dataset status (i.e. Completed, Under development, etc)">Change status</a>
                                            </li>
                                            <li>
                                                <stripes:link class="link-plain" beanclass="${actionBean.browseObservationsActionBeanClass.name}" title="Browse observations in this DataCube dataset">
                                                    <c:out value="Browse observations"/>
                                                    <stripes:param name="dataset" value="${subjectUri}"/>
                                                </stripes:link>
                                            </li>
                                        </c:if>
                                        <c:if test="${uriIsScoreboardCodelist}">
                                            <li>
                                                <stripes:link class="link-plain" beanclass="${actionBean.browseCodelistsActionBeanClass.name}" title="List members of this codelist">
                                                    <c:out value="Codelist members"/>
                                                    <stripes:param name="codelistUri" value="${subjectUri}"/>
                                                </stripes:link>
                                            </li>
                                        </c:if>
                                        <c:if test="${sourceReadActionsAllowed}">
                                            <li>
                                                <stripes:link class="link-plain" beanclass="${actionBean.viewSourceActionBeanClass.name}">Source details
                                                    <stripes:param name="uri" value="${subjectUrl}"/>
                                                </stripes:link>
                                            </li>
                                        </c:if>
                                        <c:if test="${uriIsGraph || sourceReadActionsAllowed}">
                                            <li>
                                                <stripes:link class="link-plain" href="/source.action?export=&harvestSource.url=${subjectUrl}">Export triples</stripes:link>
                                            </li>
                                        </c:if>
                                        <c:if test="${actionBean.subjectIsType || (sourceReadActionsAllowed && actionBean.adminLoggedIn)}">
                                            <li>
                                                <stripes:link class="link-plain" href="/admin/postHarvestScripts?targetType=${actionBean.subjectIsType ? 'TYPE' : 'SOURCE'}&targetUrl=${subjectUrl}">Post-harvest scripts</stripes:link>
                                            </li>
                                        </c:if>
                                        <c:if test="${sourceReadActionsAllowed && actionBean.adminLoggedIn && actionBean.harvestSourceDTO != null && actionBean.harvestSourceDTO.sparqlEndpoint == true}">
                                            <li>
                                                <stripes:link class="link-plain" href="/admin/endpointQueries.action">
                                                    <c:out value="Endpoint harvest scripts"/>
                                                    <stripes:param name="endpointUrl" value="${actionBean.harvestSourceDTO.url}"/>
                                                </stripes:link>
                                            </li>
                                        </c:if>
                                        <c:if test="${downloadAllowed}">
                                            <li>
                                                <stripes:link class="link-plain" href="/download?uri=${subjectUri}">Download</stripes:link>
                                            </li>
                                        </c:if>
                                        <c:if test="${addBookmarkAllowed}">
                                            <li>
                                                <a href="#" id="add_bookmark">Add bookmark</a>
                                            </li>
                                        </c:if>
                                        <c:if test="${removeBookmarkAllowed}">
                                            <li>
                                                <stripes:link class="link-plain" href="/factsheet.action">Remove bookmark
                                                    <stripes:param name="removebookmark" value="" />
                                                    <stripes:param name="uri" value="${subjectUrl}"/>
                                                </stripes:link>
                                            </li>
                                        </c:if>
                                        <c:if test="${addReviewAllowed}">
                                            <li>
                                                <stripes:link class="link-plain" href="/reviews.action">
                                                    Add review
                                                    <stripes:param name="addReview" value="Add"/>
                                                    <stripes:param name="uri" value="${actionBean.user.reviewsUri}"/>
                                                    <stripes:param name="addUrl" value="${subjectUrl}"/>
                                                </stripes:link>
                                            </li>
                                        </c:if>
                                        <c:if test="${addToCompiledDataset}">
                                            <li>
                                                <a href="#" id="add_to_dataset">Add to compiled dataset</a>
                                            </li>
                                        </c:if>
                                    </ul>
                                </li>
                            </ul>
                        </c:if>
                        <div style="margin-top:20px" id="wait_container">

                            <c:choose>
                                <c:when test="${actionBean.subject.anonymous}">
                                    <div class="advice-msg">This is an anonymous resource!</div>
                                </c:when>
                                <c:when test="${subjectUrl!=null}">
                                    <p>Resource URL: <a class="link-external" href="${fn:escapeXml(subjectUrl)}"><c:out value="${subjectUrl}"/></a>
                                        <c:choose>
                                            <c:when test ="${actionBean.subjectIsUserBookmark}">(Bookmarked)</c:when>
                                        </c:choose>
                                    </p>
                                    <c:if test="${actionBean.currentlyHarvested}">
                                        <div class="advice-msg">The resource is currently being harvested!</div>
                                    </c:if>
                                </c:when>
                                <c:when test="${subjectUri!=null}">
                                    <p>Resource URI: <c:out value="${subjectUri}"/>
                                </c:when>
                                <c:otherwise>
                                    <div class="advice-msg" title="${fn:escapeXml(subjectUri)}">This is an unresolvable resource!</div>
                                </c:otherwise>
                            </c:choose>

                            <c:if test="${actionBean.context.eventName=='edit' && editAllowed}">
                                <crfn:form id="addForm" action="/factsheet.action" method="post">

                                    <table style="margin-top: 30px;">
                                        <tr>
                                            <td><stripes:label for="propertySelect">Property:</stripes:label></td>
                                            <td>
                                                <div class="select-editable">
                                                    <select id="propertySelect" onchange="this.nextElementSibling.value=this.value">
                                                        <option value=""></option>
                                                        <c:forEach var="prop" items="${actionBean.addibleProperties}">
                                                            <option value="${prop.value}" title="${empty prop.title ? prop.label : prop.title}">
                                                                <c:out value="${prop.label} (${prop.value})"/>
                                                            </option>
                                                        </c:forEach>
                                                    </select>
                                                    <input type="text" name="propertyUri" value=""/>
                                                </div>
                                            </td>
                                            <td></td>
                                        </tr>
                                        <tr>
                                            <td><stripes:label for="propertyText">Value:</stripes:label></td>
                                            <td><stripes:textarea name="propertyValue" id="propertyText" cols="80" rows="2"/></td>
                                            <td style="vertical-align: bottom;padding-bottom: 4px;"><stripes:submit name="add" value="Add" id="addButton" title="Add a new property-value pair."/></td>
                                        </tr>
                                    </table>

                                    <fieldset style="display: none;">
                                        <stripes:hidden name="uri" value="${subjectUri}"/>
                                        <stripes:hidden name="anonymous" value="${actionBean.subject.anonymous}"/>
                                    </fieldset>
                                </crfn:form>
                            </c:if>

                            <stripes:layout-render name="/pages/common/factsheet_table.jsp"
                                        subjectUrl="${subjectUrl}" subjectUri="${subjectUri}"
                                        enableEdit="${actionBean.context.eventName=='edit' && editAllowed}"/>

                        </div>
                        <c:if test="${actionBean.userLoggedIn}">
                            <div id="bookmark_dialog" title="Add bookmark">
                                <crfn:form action="/factsheet.action" method="post">
                                    <stripes:hidden name="uri" value="${subjectUrl}"/>
                                    <fieldset style="border: 0px;">
                                        <label for="label" style="width: 200px; float: left;">Label</label>
                                        <stripes:text name="bookmarkLabel" id="label" size="40"/>
                                    </fieldset>
                                    <stripes:submit name="addbookmark" value="Add bookmark" id="bookmark_form_submit" style="float: right;"/>
                                </crfn:form>
                            </div>
                            <div id="dataset_dialog" title="Add to compiled dataset">
                                <crfn:form action="/saveFiles.action" method="post">
                                    <stripes:hidden name="selectedFiles" value="${subjectUri}"/>
                                    <fieldset style="border: 0px;">
                                        <label for="compiledDataset" style="width: 200px; float: left;">Select compiled dataset:</label>
                                        <stripes:select name="dataset" id="compiledDataset" style="width: 350px;">
                                            <c:forEach items="${actionBean.userCompiledDatasets}" var="ds" varStatus="loop">
                                                <stripes:option value="${ds.uri}" label="${crfn:removeHomeUri(ds.uri)} (${ds.label})" />
                                            </c:forEach>
                                        </stripes:select>
                                    </fieldset>
                                    <stripes:submit name="save" value="Add" id="dataset_form_submit" style="float: right;"/>
                                </crfn:form>
                            </div>
                        </c:if>
                        <div id="predObjValueDialog" title="Property value"></div>
                    </c:when>

                    <%-- The section that is displayed if the subject does not yet exist in the database, --%>
                    <%-- that is: actionBean.subject==null --%>

                    <c:otherwise>
                        <c:choose>
                            <c:when test="${not empty actionBean.uri}">
                                <c:if test='${actionBean.uriIsGraph || crfn:userHasPermission(pageContext.session, "/registrations", "u")}'>
                                    <c:if test="${not empty actionBean.url}">
                                        <ul id="dropdown-operations">
                                            <li>
                                                <a href="#">Operations</a>
                                                <ul>
                                                    <c:if test="${actionBean.uriIsGraph}">
                                                        <li>
                                                            <stripes:link class="link-plain" href="/source.action?export=&harvestSource.url=${actionBean.uri}">Export triples</stripes:link>
                                                        </li>
                                                    </c:if>
                                                    <c:if test="${actionBean.currentlyHarvested==false}">
                                                        <li>
                                                            <stripes:url value="${actionBean.urlBinding}" event="harvestAjax"  var="url">
                                                                    <stripes:param name="uri" value="${actionBean.uri}"/>
                                                            </stripes:url>
                                                            <stripes:url value="${actionBean.urlBinding}" event="harvest"  var="oldUrl">
                                                                    <stripes:param name="uri" value="${actionBean.uri}"/>
                                                            </stripes:url>
                                                            <a id="wait_link" href="${oldUrl}" onclick="javascript:loadAndWait('The resource is being harvested. Please wait ...', '${url}', '${pageContext.request.contextPath}'); return false;">Harvest</a>
                                                        </li>
                                                    </c:if>
                                                </ul>
                                            </li>
                                        </ul>
                                    </c:if>
                                </c:if>
                                <div style="margin-top:20px" class="note-msg" id="wait_container">
                                    <strong>Unknown</strong>
                                    <p>The application has no information about
                                        <c:choose>
                                            <c:when test="${not empty actionBean.url}">
                                                <a class="link-external" href="${actionBean.url}">${actionBean.url}</a>
                                            </c:when>
                                            <c:otherwise>
                                                ${actionBean.uri}
                                            </c:otherwise>
                                        </c:choose>
                                    </p>
                                </div>
                            </c:when>
                            <c:when test="${actionBean.uriHash!=0}">
                                <div style="margin-top:20px" class="note-msg">
                                <strong>Unknown</strong>
                                    <p>The application has no information about this anonymous resource</p>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="error-msg">
                                    Resource identifier not specified!
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </c:otherwise>
                </c:choose>
            </c:when>
            <c:otherwise>
                <div>&nbsp;</div>
            </c:otherwise>
        </c:choose>

        <%-- The "Change dataset status" popup, relevant for the factsheets of DataCube datasets only. Displayed when user clicks on the relevant popup link. --%>

        <div id="changeDatasetStatusDialog" title="Change dataset status">

            <c:set var="currentDatasetStatus" value="${actionBean.subject.predicates['http://www.w3.org/ns/adms#status'][0].value}"/>

            <stripes:form beanclass="${actionBean['class'].name}" method="post">

                <table>
                    <tr>
                        <td>
                            <span class="question">Current status:</span>
                        </td>
                        <td>
                            <c:out value="${empty currentDatasetStatus ? 'none' : currentDatasetStatus}"/>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <stripes:label for="selStatus" class="question required" title="New status for the dataset">New status:</stripes:label>
                        </td>
                        <td>
                            <select id="selStatus" name="datasetStatus">
                                <option value="http://purl.org/adms/status/Completed">Completed (http://purl.org/adms/status/Completed)</option>
                                <option value="http://purl.org/adms/status/UnderDevelopment">Under development (http://purl.org/adms/status/UnderDevelopment)</option>
                                <option value="http://purl.org/adms/status/Deprecated">Deprecated (http://purl.org/adms/status/Deprecated)</option>
                                <option value="http://purl.org/adms/status/Withdrawn">Withdrawn (http://purl.org/adms/status/Withdrawn)</option>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>&nbsp;</td>
                        <td style="padding-top:10px">
                            <stripes:submit name="changeDatasetStatus" value="Submit"/>
                            <input type="button" id="closeChangeDatasetStatusDialog" value="Cancel"/>
                        </td>
                    </tr>
                </table>

                <fieldset style="display:none">
                    <stripes:hidden name="uri"/>
                </fieldset>

            </stripes:form>
        </div>

    </stripes:layout-component>
</stripes:layout-render>
