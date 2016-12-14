<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="SPARQL endpoint" bodyAttribute="onLoad=\"format_select()\"">

    <stripes:layout-component name="head">
        <script type="text/javascript">
        // <![CDATA[
            var last_format = 1;
            function format_select() {
                var query_obg = document.getElementById('queryText');
                var query = query_obg.value;
                var format = query_obg.form.format;

                if ((query.match(/\bconstruct\b/i) || query.match(/\bdescribe\b/i)) && last_format == 1) {
                    for ( var i = format.options.length; i > 0; i--){
                        format.options[i] = null;
                    }
                    format.options[0] = new Option('HTML', 'text/html');
                    format.options[1] = new Option('RDF/XML', 'application/rdf+xml');
                    format.selectedIndex = 1;
                    last_format = 2;
                }

                if (!(query.match(/\bconstruct\b/i) || query.match(/\bdescribe\b/i)) && last_format == 2) {
                    for ( var i = format.options.length; i > 0; i--){
                        format.options[i] = null;
                    }
                    format.options[0] = new Option('HTML', 'text/html');
                    format.options[1] = new Option('XML', 'application/sparql-results+xml');
                    format.options[2] = new Option('JSON', 'application/sparql-results+json');
                    format.selectedIndex = 1;
                    last_format = 1;
                }
            }

            ( function($) {
                $(document).ready(
                    function(){
                        // Open bookmarked queries dialog
                        $("#bookmarksLink").click(function() {
                            $('#bookmarksDialog').dialog('open');
                            return false;
                        });

                        // Bookmarked queries dialog setup
                        $('#bookmarksDialog').dialog({
                            autoOpen: false,
                            width: 600
                        });

                        // Close dialog
                        $("#deleteBookmarked").click(function() {
                            $('#bookmarksDialog').dialog("close");
                            return true;
                        });

                        // Open shared bookmarked queries dialog
                        $("#sharedBookmarksLink").click(function() {
                            $('#sharedBookmarksDialog').dialog('open');
                            return false;
                        });

                        // Shared bookmarked queries dialog setup
                        $('#sharedBookmarksDialog').dialog({
                            autoOpen: false,
                            width: 600
                        });


                        // Open project bookmarked queries dialog
                        $("#projectBookmarksLink").click(function() {
                            $('#projectBookmarksDialog').dialog('open');
                            return false;
                        });

                        // Project bookmarked queries dialog setup
                        $('#projectBookmarksDialog').dialog({
                            autoOpen: false,
                            width: 600
                        });

                        // Close dialog
                        $("#deleteSahredBookmarked").click(function() {
                            $('#sharedBookmarksDialog').dialog("close");
                            return true;
                        });

                        // Open prefixes dialog
                        $("#prefixesLink").click(function() {
                            $('#prefixesDialog').dialog('open');
                            return false;
                        });

                        // Prefixes dialog setup
                        $('#prefixesDialog').dialog({
                            autoOpen: false,
                            width: 600
                        });

                        // Close prefixes dialog
                        $("#closePrefixesDialog").click(function() {
                            $('#prefixesDialog').dialog("close");
                            return true;
                        });

                        // Add query prefix #1
                        $("#prefix1").click(function() {
                            return handlePrefixClick("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
                        });
                        // Add query prefix #2
                        $("#prefix2").click(function() {
                            return handlePrefixClick("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>");
                        });
                        // Add query prefix #3
                        $("#prefix3").click(function() {
                            return handlePrefixClick("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>");
                        });
                        // Add query prefix #4
                        $("#prefix4").click(function() {
                            return handlePrefixClick("PREFIX owl: <http://www.w3.org/2002/07/owl#>");
                        });
                        // Add query prefix #5
                        $("#prefix5").click(function() {
                            return handlePrefixClick("PREFIX dc: <http://purl.org/dc/elements/1.1/>");
                        });
                        // Add query prefix #6
                        $("#prefix6").click(function() {
                            return handlePrefixClick("PREFIX dcterms: <http://purl.org/dc/terms/>");
                        });
                        // Add query prefix #7
                        $("#prefix7").click(function() {
                            return handlePrefixClick("PREFIX foaf: <http://xmlns.com/foaf/0.1/>");
                        });
                        // Add query prefix #8
                        $("#prefix8").click(function() {
                            return handlePrefixClick("PREFIX skos: <http://www.w3.org/2004/02/skos/core#>");
                        });
                        // Add query prefix #9
                        $("#prefix9").click(function() {
                            return handlePrefixClick("PREFIX cube: <http://purl.org/linked-data/cube#>");
                        });
                        $("#prefix9_5").click(function() {
                            return handlePrefixClick("PREFIX dcat: <http://www.w3.org/ns/dcat#>");
                        });
                        // Add query prefix #10
                        $("#prefix10").click(function() {
                            return handlePrefixClick("PREFIX dad-prop: <http://semantic.digital-agenda-data.eu/def/property/>");
                        });
                        // Add query prefix #11
                        $("#prefix11").click(function() {
                            return handlePrefixClick("PREFIX dad-class: <http://semantic.digital-agenda-data.eu/def/class/>");
                        });
                        // Add query prefix #12
                        $("#prefix12").click(function() {
                            return handlePrefixClick("PREFIX dad-codelist: <http://semantic.digital-agenda-data.eu/def/codelist/>");
                        });
                        // Add query prefix #13
                        $("#prefix13").click(function() {
                            return handlePrefixClick("PREFIX rad: <http://www.w3.org/ns/radion#>");
                        });
                        // Add query prefix #14
                        $("#prefix14").click(function() {
                            return handlePrefixClick("PREFIX adms: <http://www.w3.org/ns/adms#>");
                        });
                        // Add query prefix #15
                        $("#prefix15").click(function() {
                            return handlePrefixClick("PREFIX estat-dic: <http://eurostat.linked-statistics.org/dic/>");
                        });
                        // Add query prefix #16
                        $("#prefix16").click(function() {
                            return handlePrefixClick("PREFIX sdmx: <http://purl.org/linked-data/sdmx#>");
                        });
                        // Add query prefix #17
                        $("#prefix17").click(function() {
                            return handlePrefixClick("PREFIX sdmx-attribute: <http://purl.org/linked-data/sdmx/2009/attribute#>");
                        });
                        // Add query prefix #18
                        $("#prefix18").click(function() {
                            return handlePrefixClick("PREFIX sdmx-measure: <http://purl.org/linked-data/sdmx/2009/measure#>");
                        });
                        // Add query prefix #19
                        $("#prefix19").click(function() {
                            return handlePrefixClick("PREFIX sdmx-dimension: <http://purl.org/linked-data/sdmx/2009/dimension#>");
                        });
                        // Add query prefix #20
                        $("#prefix20").click(function() {
                            return handlePrefixClick("PREFIX sdmx-code: <http://purl.org/linked-data/sdmx/2009/code#>");
                        });
                        // Add query prefix #21
                        $("#prefix21").click(function() {
                            return handlePrefixClick("PREFIX sdmx-concept: <http://purl.org/linked-data/sdmx/2009/concept#>");
                        });
                    });
            } ) ( jQuery );

            function handlePrefixClick(prefix) {
                var query = document.getElementById('queryText').value;
                document.getElementById('queryText').value = addOrRemove(query, prefix);

                var areas = document.querySelectorAll('.expandingArea');
                var l = areas.length;
                while (l--) {
                    makeExpandingArea(areas[l]);
                }

                return false;
            }

            function addOrRemove(original, prefix) {
                if (stringExists(original, prefix)) {
                    return removeString(original, prefix);
                } else {
                    return addString(original, prefix);
                }
            }

            function stringExists(string, subString) {
                if (string.length < 1) {
                    return false;
                }
                if (subString.length < 1) {
                    return false;
                }
                if (string.indexOf(subString) == -1) {
                    return false;
                }
                return true;
            }

            function addString(string, addable) {
                return addable + "\n" + string;
            }

            function removeString(string, removable) {
                if (stringExists(string, removable + "\n")) {
                    return string.replace(removable + "\n", "");
                } else {
                    return string.replace(removable, "");
                }
            }
            // ]]>
        </script>
        <c:if test="${actionBean.userLoggedIn}">
            <script type="text/javascript">
                // <![CDATA[
                ( function($) {
                $(document).ready(
                    function(){
                        $("#executeButton").click(function() {
                            var query = document.getElementById('queryText').value;
                            var format = document.getElementById('format').value;

                            if (format == 'application/rdf+xml' && query.match(/\bconstruct\b/i)) {
                                $('#constructDialog').dialog('open');
                                document.getElementById('constructQuery').value = query;
                                document.getElementById('constructFormat').value = format;
                                return false;
                            } else {
                                return true;
                            }
                        });

                        // Construct queriy dialog setup
                        $('#constructDialog').dialog({
                            autoOpen: false,
                            width: 500
                        });

                        //Hide div w/id extra
                        if ($("input[name=exportType]:checked").val() != "HOMESPACE") {
                            $("#homespace_data").css("display","none");
                        }

                        // Add onclick handler to checkbox w/id checkme
                        $('input[name=exportType]').click(function(){
                            $("#homespace_data").toggle();
                        });

                        // Close dialog
                        $("#executeConstruct").click(function() {
                            $('#constructDialog').dialog("close");
                            return true;
                        });


                    });
                } ) ( jQuery );

                // ]]>
            </script>
        </c:if>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <ul id="dropdown-operations">
            <li><a href="#">Operations</a>
                <ul>
                    <li>
                        <a href="#" id="sharedBookmarksLink">Shared bookmarked queries</a>
                    </li>
                    <c:if test="${actionBean.user != null}">
                        <li>
                            <a href="#" id="bookmarksLink">My bookmarked queries</a>
                        </li>
                        <c:if test="${not empty(actionBean.projectBookmarkedQueries)}">
                        <li>
                            <a href="#" id="projectBookmarksLink">Bookmarks in projects</a>
                        </li>
                        </c:if>
                    </c:if>
                    <li>
                        <a href="#" id="prefixesLink">Useful namespaces</a>
                    </li>
                </ul>
            </li>
        </ul>

        <h1>SPARQL endpoint</h1>
        <c:if test="${not empty actionBean.selectedBookmarkName}">
            <h2>Bookmarked query: <c:out value="${actionBean.selectedBookmarkName}" /></h2>
        </c:if>

        <div style="margin-top: 15px">
            <div style="float:right">
                <a href="documentation/sparqlfunctions" title="Press Ctrl and click to open help on SPARQL Functions in a new window">SPARQL Functions</a>
            </div>
            <crfn:form name="mainForm" action="/sparql" method="get">
                <div>
                <stripes:hidden name="selectedBookmarkName" />
                </div>
                <c:if test="${not empty actionBean.defaultGraphUris}">
                    <c:forEach var="defaultGraphUri" items="${actionBean.defaultGraphUris}">
                        <input type="hidden" name="default-graph-uri" value="${defaultGraphUri}" />
                    </c:forEach>
                </c:if>
                <c:if test="${not empty actionBean.namedGraphUris}">
                    <c:forEach var="namedGraphUri" items="${actionBean.namedGraphUris}">
                        <input type="hidden" name="named-graph-uri" value="${namedGraphUri}" />
                    </c:forEach>
                </c:if>
                <div>
                    <label for="queryText" class="question">Query:</label>
                    <div>
                        <c:if test="${not empty actionBean.defaultGraphUris or not empty actionBean.namedGraphUris}">
                        <ul>
                        <c:if test="${not empty actionBean.defaultGraphUris}">
                            <c:forEach var="defaultGraphUri" items="${actionBean.defaultGraphUris}">
                                <li><b>default-graph-uri:</b> <c:out value="${defaultGraphUri}" /></li>
                            </c:forEach>
                        </c:if>
                        <c:if test="${not empty actionBean.namedGraphUris}">
                            <c:forEach var="namedGraphUri" items="${actionBean.namedGraphUris}">
                                <li><b>named-graph-uri:</b> <c:out value="${namedGraphUri}" /></li>
                            </c:forEach>
                        </c:if>
                        </ul>
                        </c:if>
                    </div>
                    <div class="expandingArea">
                    <pre><span></span><br /></pre>
                    <textarea name="query" id="queryText" rows="8" cols="80" style="clear:right; display: block; width: 100%" onchange="format_select()" onkeyup="format_select()"><c:if test="${empty actionBean.query}">
# This is a sample SPARQL query that returns the first 10 DataCube observations
# for the indicator "Households with access to the Internet at home".

PREFIX cube: &lt;http://purl.org/linked-data/cube#&gt;
PREFIX dcat: &lt;http://www.w3.org/ns/dcat#&gt;
PREFIX dad-prop: &lt;http://semantic.digital-agenda-data.eu/def/property/&gt;

SELECT DISTINCT ?observation WHERE {
  ?observation a cube:Observation .
  ?observation dad-prop:indicator &lt;http://semantic.digital-agenda-data.eu/codelist/indicator/h_iacc&gt;
}
ORDER BY ?observation
LIMIT 10</c:if>${crfn:escapeHtml(actionBean.query)}</textarea>
                    </div>
                    <script type="text/javascript">
// <![CDATA[
function makeExpandingArea(container) {
 var area = container.querySelector('textarea');
 var span = container.querySelector('span');
 if (area.addEventListener) {
   area.addEventListener('input', function() {
     span.textContent = area.value;
   }, false);
   span.textContent = area.value;
 } else if (area.attachEvent) {
   // IE8 compatibility
   area.attachEvent('onpropertychange', function() {
     span.innerText = area.value;
   });
   span.innerText = area.value;
 }
 // Enable extra CSS
 container.className += ' active';
}

var areas = document.querySelectorAll('.expandingArea');
var l = areas.length;

while (l--) {
 makeExpandingArea(areas[l]);
}
// ]]>
                    </script>
                </div>
                <div style="position: relative; margin-bottom: 30px">
                    <div style="position: absolute; top: 5px; left: 0px;">
                        <label for="format" class="question" style="width: 200px;">Output format:</label>
                        <stripes:select name="format" id="format">
                            <stripes:option value="text/html" label="HTML" />
                            <stripes:option value="text/html+" label="HTML+" />
                            <stripes:option value="application/sparql-results+json" label="JSON" />
                            <stripes:option value="application/sparql-results+xml" label="XML" />
                            <stripes:option value="application/x-ms-access-export+xml" label="XML with Schema" />
                            <stripes:option value="application/csv" label="CSV" />
                        </stripes:select>
                    </div>
                    <div style="position: absolute; top: 5px; left: 250px;">
                        <stripes:label for="nrOfHits" class="question">Hits per page</stripes:label>
                        <stripes:text name="nrOfHits" size="2" id="nrOfHits" />
                    </div>
                    <div style="position: absolute; top: 5px; left: 410px;">
                        <stripes:label for="inferencing" class="question">Use CR inferencing</stripes:label>
                        <stripes:checkbox name="useInferencing" id="inferencing" />
                    </div>

                    <c:choose>
                        <c:when test="${actionBean.user != null}">
                            <div style="position: absolute; top: 5px; left: 590px;">
                                <stripes:submit name="execute" value="Execute" id="executeButton" />
                                <stripes:submit name="bookmark" value="Bookmark" id="bookmarkButton" />
                                <stripes:hidden name="bookmarkName" value="${actionBean.bookmarkName}"/>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div style="position: absolute; top: 5px; right: 0px;">
                                <stripes:submit name="execute" value="Execute" id="executeButton" />
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div style="clear:both">
                    <c:if test="${not empty actionBean.query && empty param.bookmark && empty param.fillfrom}">
                        <c:if test="${actionBean.result.allRowsReturned == 'false'}">
                            <div class="important-msg">The query result exceeded maximum allowed row count. Displaying only first rows. Please run a more detailed query.</div>
                        </c:if>
                        <c:choose>
                            <c:when test="${not empty actionBean.result && not empty actionBean.result.rows}">
                                <br />
                                <display:table name="${actionBean.result.rows}" class="datatable"
                                    pagesize="${actionBean.nrOfHits}" sort="list" id="listItem"
                                    htmlId="listItem" requestURI="/sparql"
                                    decorator="eionet.cr.web.sparqlClient.helpers.SparqlClientColumnDecorator">
                                    <c:forEach var="cl" items="${actionBean.result.cols}">
                                        <display:column property="map(${cl.property})"
                                            title="${cl.title}" sortable="${cl.sortable}" />
                                    </c:forEach>
                                </display:table>
                                <br/>Done. -- ${actionBean.executionTime} ms.
                            </c:when>
                            <c:when test="${actionBean.askQuery == 'true'}">
                                <br/><c:out value="${actionBean.resultAsk}"/>
                            </c:when>
                            <c:otherwise>
                                <div class="system-msg">The query gave no results!</div>
                            </c:otherwise>
                        </c:choose>
                    </c:if>

                    <c:if test="${empty actionBean.result || empty actionBean.result.rows}">
                        <br/>
                        <div class="advice-msg">
                            On this page you can execute various SPARQL queries against the backend that CR is running on.
                            For a more convenient use, you can insert common namespace prefixs into your query by selecting
                            "Useful namespaces" in the Operations menu and clicking the namespaces in the opening pop-up window.
                            The Operations menu also provides a list of shared bookmarked queries which you can select to
                            pre-fill your query.<br/><br/>
                            The output format of the query depends on the one you select from the above select box. In order
                            to make the query use inference rules, turn on the relevant checkbox. A link to common useful
                            SPARQL functions is also available below the Operations menu.
                        </div>
                    </c:if>
                </div>
            </crfn:form>
        </div>

            <c:if test="${actionBean.userLoggedIn}">
                <div id="constructDialog" title="Save result to ">
                    <crfn:form name="constructForm" action="/sparql" method="get">
                        <div>
                        <stripes:hidden name="query" id="constructQuery"/>
                        <stripes:hidden name="format" id="constructFormat"/>
                        </div>
                        <table class="formtable">
                            <tr>
                                <td style="width:120px"><stripes:label for="toFile">To file</stripes:label></td>
                                <td>
                                    <stripes:radio name="exportType" value="FILE" checked="FILE" title="To file" id="toFile"/>
                                </td>
                            </tr>
                            <tr>
                                <td><stripes:label for="toHomespace">To homespace</stripes:label></td>
                                <td>
                                    <stripes:radio name="exportType" value="HOMESPACE" id="toHomespace"/>
                                </td>
                            </tr>
                        </table>
                        <div id="homespace_data">
                            <table class="formtable">
                                <tr>
                                    <td style="width:120px"><label for="datasetName">Dataset name</label></td>
                                    <td>
                                        <stripes:text name="datasetName" id="datasetName" style="width: 350px;"/>
                                    </td>
                                </tr>
                                <tr>
                                    <td><label for="folder">Folder</label></td>
                                    <td>
                                        <stripes:select name="folder" id="folder" style="width: 355px;">
                                            <c:forEach items="${actionBean.folders}" var="f" varStatus="loop">
                                                <stripes:option value="${f}" label="${crfn:extractFolder(f)}" />
                                            </c:forEach>
                                        </stripes:select>
                                    </td>
                                </tr>
                                <tr>
                                    <td></td>
                                    <td>
                                        <stripes:checkbox name="overwriteDataset" id="overwriteDataset"/>
                                        <stripes:label for="overwriteDataset">Overwrite if file/dataset already exists</stripes:label>
                                    </td>
                                </tr>
                            </table>
                        </div>
                        <table class="formtable">
                            <tr>
                                <td align="right">
                                    <stripes:submit name="execute" value="Execute" id="executeConstruct"/>
                                </td>
                            </tr>
                        </table>
                    </crfn:form>
                </div>
            </c:if>

            <%-- Bookmarked queries dialog --%>
            <div id="bookmarksDialog" title="Bookmarked queries">
            <h1>Bookmarked SPARQL queries</h1>
            <c:choose>
                <c:when test="${not empty actionBean.bookmarkedQueries}">
                    <crfn:form id="bookmarkedQueriesForm" action="/sparql" method="post">
                    <c:if test="${not empty actionBean.defaultGraphUris}">
                        <c:forEach var="defaultGraphUri" items="${actionBean.defaultGraphUris}">
                            <input type="hidden" name="default-graph-uri" value="${defaultGraphUri}" />
                        </c:forEach>
                    </c:if>
                    <c:if test="${not empty actionBean.namedGraphUris}">
                        <c:forEach var="namedGraphUri" items="${actionBean.namedGraphUris}">
                            <input type="hidden" name="named-graph-uri" value="${namedGraphUri}" />
                        </c:forEach>
                    </c:if>
                    <table>
                        <c:forEach items="${actionBean.bookmarkedQueries}" var="bookmarkedQuery">
                        <tr>
                            <td style="width:1%"><stripes:checkbox value="${bookmarkedQuery.subj}" name="deleteQueries"/></td>
                            <td style="width:99%">
                                <stripes:link href="/sparql" title="${bookmarkedQuery.queryString}">
                                    <stripes:param name="fillfrom" value="${bookmarkedQuery.subj}" />
                                    <stripes:param name="selectedBookmarkName" value="${bookmarkedQuery.label}" />
                                    <c:if test="${not empty actionBean.defaultGraphUris}">
                                        <c:forEach var="defaultGraphUri" items="${actionBean.defaultGraphUris}">
                                            <stripes:param name="default-graph-uri" value="${defaultGraphUri}" />
                                        </c:forEach>
                                    </c:if>
                                    <c:if test="${not empty actionBean.namedGraphUris}">
                                        <c:forEach var="namedGraphUri" items="${actionBean.namedGraphUris}">
                                            <stripes:param name="named-graph-uri" value="${namedGraphUri}" />
                                        </c:forEach>
                                    </c:if>
                                    <c:out value="${bookmarkedQuery.label}"/>
                                </stripes:link>
                            </td>
                        </tr>
                        </c:forEach>
                        <tr>
                            <td colspan="2" align="right" style="padding-top: 5px">
                                <stripes:submit name="deletePersonalBookmark" id="deletePersonalBookmark" value="Delete" title="Delete the bookmarked queries that you have selected below"/>
                                <input type="button" name="selectAll" value="Select all" onclick="toggleSelectAll('bookmarkedQueriesForm');return false"/>
                            </td>
                        </tr>
                    </table>
                    </crfn:form>
                </c:when>
                <c:otherwise>
                    No bookmarked queries found.
                </c:otherwise>
            </c:choose>
            </div>

            <%-- Shared bookmarked queries dialog --%>
            <div id="sharedBookmarksDialog" title="Bookmarked queries">
            <h1>Shared bookmarked SPARQL queries</h1>
            <c:choose>
                <c:when test="${not empty actionBean.sharedBookmarkedQueries}">
                    <crfn:form id="sharedBookmarkedQueriesForm" action="/sparql" method="post">
                    <c:if test="${not empty actionBean.defaultGraphUris}">
                        <c:forEach var="defaultGraphUri" items="${actionBean.defaultGraphUris}">
                            <input type="hidden" name="default-graph-uri" value="${defaultGraphUri}" />
                        </c:forEach>
                    </c:if>
                    <c:if test="${not empty actionBean.namedGraphUris}">
                        <c:forEach var="namedGraphUri" items="${actionBean.namedGraphUris}">
                            <input type="hidden" name="named-graph-uri" value="${namedGraphUri}" />
                        </c:forEach>
                    </c:if>
                    <table>
                        <c:forEach items="${actionBean.sharedBookmarkedQueries}" var="bookmarkedQuery">
                        <tr>
                            <c:if test="${actionBean.sharedBookmarkPrivilege}">
                            <td style="width:1%"><stripes:checkbox value="${bookmarkedQuery.subj}" name="deleteQueries"/></td>
                            </c:if>
                            <td style="width:99%">
                                <stripes:link href="/sparql" title="${bookmarkedQuery.queryString}">
                                    <stripes:param name="fillfrom" value="${bookmarkedQuery.subj}" />
                                    <stripes:param name="selectedBookmarkName" value="${bookmarkedQuery.label}" />
                                    <c:if test="${not empty actionBean.defaultGraphUris}">
                                        <c:forEach var="defaultGraphUri" items="${actionBean.defaultGraphUris}">
                                            <stripes:param name="default-graph-uri" value="${defaultGraphUri}" />
                                        </c:forEach>
                                    </c:if>
                                    <c:if test="${not empty actionBean.namedGraphUris}">
                                        <c:forEach var="namedGraphUri" items="${actionBean.namedGraphUris}">
                                            <stripes:param name="named-graph-uri" value="${namedGraphUri}" />
                                        </c:forEach>
                                    </c:if>
                                    <c:out value="${bookmarkedQuery.label}"/>
                                </stripes:link>
                            </td>
                        </tr>
                        </c:forEach>
                        <c:if test="${actionBean.sharedBookmarkPrivilege}">
                        <tr>
                            <td colspan="2" align="right" style="padding-top: 5px">
                                <stripes:submit name="deleteSharedBookmark" id="deleteSharedBookmark" value="Delete" title="Delete the bookmarked queries that you have selected below"/>
                                <input type="button" name="selectAll" value="Select all" onclick="toggleSelectAll('sharedBookmarkedQueriesForm');return false"/>
                            </td>
                        </tr>
                        </c:if>
                    </table>
                    </crfn:form>
                </c:when>
                <c:otherwise>
                    No bookmarked queries found.
                </c:otherwise>
            </c:choose>
            </div>

           <%-- project Bookmarked queries dialog --%>
            <div id="projectBookmarksDialog" title="Project queries">
            <h1>Bookmarked SPARQL queries in project folders</h1>
            <c:choose>
                <c:when test="${not empty actionBean.projectBookmarkedQueries}">
                    <crfn:form id="projectBookmarkedQueriesForm" action="/sparql" method="post">
                    <c:if test="${not empty actionBean.defaultGraphUris}">
                        <c:forEach var="defaultGraphUri" items="${actionBean.defaultGraphUris}">
                            <input type="hidden" name="default-graph-uri" value="${defaultGraphUri}" />
                        </c:forEach>
                    </c:if>
                    <c:if test="${not empty actionBean.namedGraphUris}">
                        <c:forEach var="namedGraphUri" items="${actionBean.namedGraphUris}">
                            <input type="hidden" name="named-graph-uri" value="${namedGraphUri}" />
                        </c:forEach>
                    </c:if>
                    <table>
            <tr>
               <th>Project</th>
               <th>Bookmark</th>
            </tr>
                        <c:forEach items="${actionBean.projectBookmarkedQueries}" var="bookmarkedQuery">
                        <tr>
                            <!-- td width="1%"><stripes:checkbox value="${bookmarkedQuery.subj}" name="deleteQueries"/></td-->
                            <td style="width:40%"><c:out value="${bookmarkedQuery.proj}"/></td>
                            <td style="width:60%">
                                <stripes:link href="/sparql" title="${bookmarkedQuery.queryString}">
                                    <stripes:param name="fillfrom" value="${bookmarkedQuery.subj}" />
                                    <stripes:param name="selectedBookmarkName" value="${bookmarkedQuery.label}" />
                                    <c:if test="${not empty actionBean.defaultGraphUris}">
                                        <c:forEach var="defaultGraphUri" items="${actionBean.defaultGraphUris}">
                                            <stripes:param name="default-graph-uri" value="${defaultGraphUri}" />
                                        </c:forEach>
                                    </c:if>
                                    <c:if test="${not empty actionBean.namedGraphUris}">
                                        <c:forEach var="namedGraphUri" items="${actionBean.namedGraphUris}">
                                            <stripes:param name="named-graph-uri" value="${namedGraphUri}" />
                                        </c:forEach>
                                    </c:if>
                                    <c:out value="${bookmarkedQuery.label}"/>
                                </stripes:link>
                            </td>
                        </tr>
                        </c:forEach>
                        <!-- tr>
                            <td colspan="2" align="right" style="padding-top: 5px">
                                <stripes:submit name="deletePersonalBookmark" id="deletePersonalBookmark" value="Delete" title="Delete the bookmarked queries that you have selected below"/>
                                <input type="button" name="selectAll" value="Select all" onclick="toggleSelectAll('bookmarkedQueriesForm');return false"/>
                            </td>
                        </tr-->
                    </table>
                    </crfn:form>
                </c:when>
                <c:otherwise>
                    No bookmarked queries found.
                </c:otherwise>
            </c:choose>
            </div>


            <div id="prefixesDialog" title="Useful namespaces">
                <ul>
                    <li><span id="prefix1" class="shadowHover">PREFIX rdf: &lt;http://www.w3.org/1999/02/22-rdf-syntax-ns#&gt;</span></li>
                    <li><span id="prefix2" class="shadowHover">PREFIX rdfs: &lt;http://www.w3.org/2000/01/rdf-schema#&gt;</span></li>
                    <li><span id="prefix3" class="shadowHover">PREFIX xsd: &lt;http://www.w3.org/2001/XMLSchema#&gt;</span></li>
                    <li><span id="prefix4" class="shadowHover">PREFIX owl: &lt;http://www.w3.org/2002/07/owl#&gt;</span></li>
                    <li><span id="prefix5" class="shadowHover">PREFIX dc: &lt;http://purl.org/dc/elements/1.1/&gt;</span></li>
                    <li><span id="prefix6" class="shadowHover">PREFIX dcterms: &lt;http://purl.org/dc/terms/&gt;</span></li>
                    <li><span id="prefix7" class="shadowHover">PREFIX foaf: &lt;http://xmlns.com/foaf/0.1/&gt;</span></li>
                    <li><span id="prefix8" class="shadowHover">PREFIX skos: &lt;http://www.w3.org/2004/02/skos/core#&gt;</span></li>
                    <li><span id="prefix9" class="shadowHover">PREFIX cube: &lt;http://purl.org/linked-data/cube#&gt;</span></li>
                    <li><span id="prefix9_5" class="shadowHover">PREFIX dcat: &lt;http://www.w3.org/ns/dcat#&gt;</span></li>
                    <li><span id="prefix10" class="shadowHover">PREFIX dad-prop: &lt;http://semantic.digital-agenda-data.eu/def/property/&gt;</span></li>
                    <li><span id="prefix11" class="shadowHover">PREFIX dad-class: &lt;http://semantic.digital-agenda-data.eu/def/class/&gt;</span></li>
                    <li><span id="prefix12" class="shadowHover">PREFIX dad-codelist: &lt;http://semantic.digital-agenda-data.eu/def/codelist/&gt;</span></li>
                    <li><span id="prefix13" class="shadowHover">PREFIX rad: &lt;http://www.w3.org/ns/radion#&gt;</span></li>
                    <li><span id="prefix14" class="shadowHover">PREFIX adms: &lt;http://www.w3.org/ns/adms#&gt;</span></li>
                    <li><span id="prefix15" class="shadowHover">PREFIX estat-dic: &lt;http://eurostat.linked-statistics.org/dic/&gt;</span></li>
                    <li><span id="prefix16" class="shadowHover">PREFIX smdx: &lt;http://purl.org/linked-data/sdmx#&gt;</span></li>
                    <li><span id="prefix17" class="shadowHover">PREFIX smdx-attribute: &lt;http://purl.org/linked-data/sdmx/2009/attribute#&gt;</span></li>
                    <li><span id="prefix18" class="shadowHover">PREFIX smdx-measure: &lt;http://purl.org/linked-data/sdmx/2009/measure#&gt;</span></li>
                    <li><span id="prefix19" class="shadowHover">PREFIX smdx-dimension: &lt;http://purl.org/linked-data/sdmx/2009/dimension#&gt;</span></li>
                    <li><span id="prefix20" class="shadowHover">PREFIX smdx-code: &lt;http://purl.org/linked-data/sdmx/2009/code#&gt;</span></li>
                    <li><span id="prefix21" class="shadowHover">PREFIX smdx-concept: &lt;http://purl.org/linked-data/sdmx/2009/concept#&gt;</span></li>
                </ul>
                <button id="closePrefixesDialog">Close</button>
            </div>

    </stripes:layout-component>
</stripes:layout-render>
