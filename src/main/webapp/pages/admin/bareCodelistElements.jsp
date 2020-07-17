<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>


<%@page import="net.sourceforge.stripes.action.ActionBean"%><stripes:layout-render name="/pages/common/template.jsp" pageTitle="Codelist elements without metadata">

    <stripes:layout-component name="contents">

    <c:choose>
        <c:when test="${actionBean.adminLoggedIn}">

            <div id="operations">
	            <ul>
	                <li><stripes:link href="/admin">Back to admin page</stripes:link></li>
	            </ul>
            </div>

            <h1>Codelist elements without metadata</h1>

            <display:table name="${actionBean.codelistElements}" class="datatable" id="codelistElement" requestURI="${actionBean.urlBinding}" style="width:100%">

                <display:column title='<span title="URI">URI</span>'>
                    <stripes:link beanclass="${actionBean.factsheetActionBeanClass.name}">
                        <c:out value="${codelistElement}"/>
                        <stripes:param name="uri" value="${codelistElement}"/>
                    </stripes:link>
                </display:column>

                <display:caption style="text-align:left;font-weight:normal;">${fn:length(actionBean.codelistElements)} elements found</display:caption>
            </display:table>

        </c:when>
        <c:otherwise>
            <div class="error-msg">Access not allowed!</div>
        </c:otherwise>
    </c:choose>

    </stripes:layout-component>

</stripes:layout-render>
