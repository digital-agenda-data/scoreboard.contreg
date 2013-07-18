<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="SPARQL endpoint harvest query">

    <stripes:layout-component name="head">
    
        <script type="text/javascript" src="<c:url value="/scripts/useful_namespaces.js"/>"></script>
        
        <script type="text/javascript">
        // <![CDATA[
            ( function($) {
                $(document).ready(
                    function(){
                        
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
                        
                        // The handling of useful namespace clicks
                        <c:forEach items="${actionBean.usefulNamespaces}" var="usefulNamespace" varStatus="usefulNamespacesLoop">
                            $("#prefix${usefulNamespacesLoop.index}").click(function() {
                                return handlePrefixClick("PREFIX ${usefulNamespace.key}: <${fn:escapeXml(usefulNamespace.value)}>");
                            });
                        </c:forEach>

                    });
            } ) ( jQuery );
            // ]]>
        </script>
    </stripes:layout-component>

    <stripes:layout-component name="contents">

        <%-- Drop-down operations --%>

        <ul id="dropdown-operations">
            <li><a href="#">Operations</a>
                <ul>
                    <li>
                        <stripes:link beanclass="${actionBean.endpointQueriesActionBeanClass.name}">
                           <c:out value="Back to queries"/>
                           <c:if test="${not empty actionBean.query.endpointUrl}">
                               <stripes:param name="endpointUrl" value="${actionBean.query.endpointUrl}"/>
                           </c:if>
                        </stripes:link>
                    </li>
                    <li>
                        <a href="#" id="prefixesLink">Useful namespaces</a>
                    </li>
                </ul>
            </li>
        </ul>

        <%-- The page's heading and explanation text. --%>

        <h1>SPARQL endpoint harvest query</h1>

        <div style="margin-top:20px">
            <c:if test="${actionBean.query == null || actionBean.query.id <= 0}">
                <p>You are defining a new harvest query for the SPARQL endpoint <stripes:link beanclass="${actionBean.endpointQueriesActionBeanClass.name}"><c:out value="${actionBean.query.endpointUrl}"/><stripes:param name="endpointUrl" value="${actionBean.query.endpointUrl}"/></stripes:link></p>
            </c:if>
            <c:if test="${actionBean.query != null && actionBean.query.id > 0}">
                <p>This is a harvest query for the SPARQL endpoint <stripes:link beanclass="${actionBean.endpointQueriesActionBeanClass.name}"><c:out value="${actionBean.query.endpointUrl}"/><stripes:param name="endpointUrl" value="${actionBean.query.endpointUrl}"/></stripes:link> at position <c:out value="${actionBean.query.position}"/>.</p>
            </c:if>
        </div>

        <%-- The section that displays the query's properties, editable or not, depending on the event. --%>

        <div style="margin-top:20px">
            <crfn:form id="queryForm" beanclass="${actionBean.class.name}" method="post" focus="first">
                <table>
                    <tr>
                        <td>
                            <label for="txtTitle" class="question required">Title:</label>
                        </td>
                        <td>
                            <stripes:text id="txtTitle" size="70" name="query.title"/>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <label for="queryText" class="question required">Query:</label>
                        </td>
                        <td>
                            <div class="expandingArea">
                                <pre><span></span><br /></pre>
                                <stripes:textarea id="queryText" name="query.query" cols="80" rows="8" style="clear:right; display: block; width: 100%"/>
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
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <label for="chkActive" class="question">Active:</label>
                        </td>
                        <td>
                            <stripes:checkbox id="chkActive" name="query.active"/>
                        </td>
                    </tr>
                    <tr>
                        <td>&nbsp;</td>
                        <td>
                            <stripes:submit name="save" value="Save"/>
                            <stripes:submit name="saveAndClose" value="Save & close"/>
                            <stripes:submit name="test" value="Test"/>
                            <stripes:submit name="cancel" value="Cancel"/>
                        </td>
                    </tr>
                </table>

                <fieldset style="display:none">
                    <c:if test="${not empty actionBean.query}">
                        <stripes:hidden name="query.id"/>
                        <stripes:hidden name="query.endpointUrl"/>
                    </c:if>
                </fieldset>
            </crfn:form>

            <c:if test="${actionBean.context.eventName eq 'test'}">
                <c:if test="${not empty actionBean.testResult}">
                    <display:table name="${actionBean.testResult}" id="statement" sort="page" class="datatable" style="width:100%">
                        <display:column title="Subject">
                            <stripes:link beanclass="${actionBean.endpointResourceActionBeanClass.name}">
                                <c:out value="${statement.subject}"/>
                                <stripes:param name="url" value="${statement.subject}"/>
                                <stripes:param name="endpoint" value="${actionBean.query.endpointUrl}"/>
                            </stripes:link>
                        </display:column>
                        <display:column title="Predicate">
                            <stripes:link beanclass="${actionBean.endpointResourceActionBeanClass.name}">
                                <c:out value="${statement.predicate}"/>
                                <stripes:param name="url" value="${statement.predicate}"/>
                                <stripes:param name="endpoint" value="${actionBean.query.endpointUrl}"/>
                            </stripes:link>
                        </display:column>
                        <display:column title="Object">
                            <c:choose>
                                <c:when test="${fn:contains(statement.object.class.name, 'Literal')}">
                                    <c:out value="${statement.object.label}"/>
                                </c:when>
                                <c:otherwise>
                                    <stripes:link beanclass="${actionBean.endpointResourceActionBeanClass.name}">
                                        <c:out value="${statement.object}"/>
                                        <stripes:param name="url" value="${statement.object}"/>
                                        <stripes:param name="endpoint" value="${actionBean.query.endpointUrl}"/>
                                    </stripes:link>
                                </c:otherwise>
                            </c:choose>
                        </display:column>
                    </display:table>
                </c:if>
                <c:if test="${empty actionBean.testResult}">
                    <div class="system-msg">
                        <p>The test returned no results!</p>
                    </div>
                </c:if>
            </c:if>
        </div>
        
        <%-- The "Useful namesoaces" dialog, hidden by default --%>
            
	    <div id="prefixesDialog" title="Useful namespaces">
	        
	        <c:if test="${empty actionBean.usefulNamespaces}">
	            <p>None found!</p>
	        </c:if>
	        <c:if test="${not empty actionBean.usefulNamespaces}">
	            <ul>
	                <c:forEach items="${actionBean.usefulNamespaces}" var="usefulNamespace" varStatus="usefulNamespacesLoop">
	                   <li><span id="prefix${usefulNamespacesLoop.index}" class="shadowHover">PREFIX <c:out value="${usefulNamespace.key}"/>: &lt;<c:out value="${usefulNamespace.value}"/>&gt;</span></li>
	                </c:forEach>
	            </ul>
	        </c:if>
	        <button id="closePrefixesDialog">Close</button>
	        
	    </div>

    </stripes:layout-component>
</stripes:layout-render>
