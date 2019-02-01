<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@ attribute name="tabs" required="true" type="java.util.ArrayList" %>

<div id="tabbedmenu">
    <ul>
    <c:forEach items="${tabs}" var="tab">
        <c:choose>
            <c:when test="${tab.selected}">
                <li id="currenttab"><span><c:out value="${tab.title}" /></span></li>
            </c:when>
            <c:otherwise>
                <li>
                    <stripes:link href="${tab.href}" prependContext="true"><c:out value="${tab.title}" />
                        <c:if test="${not empty tab.event}">
                            <stripes:param name="${tab.event}" />
                        </c:if>
                        <c:forEach var="item" items="${tab.params}">
                            <stripes:param name="${item.key}" value="${item.value}" />
                        </c:forEach>
                    </stripes:link>
                    <div style="display: none"><c:out value="${tab.href}"/></div>
                </li>
            </c:otherwise>
        </c:choose>
    </c:forEach>
    </ul>
</div>
