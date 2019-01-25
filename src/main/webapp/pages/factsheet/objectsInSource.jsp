<%@page contentType="text/html;charset=UTF-8"%>

<%@ include file="/pages/common/taglibs.jsp"%>

<stripes:layout-render name="/pages/common/template.jsp" pageTitle="Source contents">

    <stripes:layout-component name="contents">

        <c:choose>
            <c:when test="${!actionBean.noCriteria}">

                <cr:tabMenu tabs="${actionBean.tabs}" />

                <br style="clear:left" />
                <ul id="dropdown-operations">
                <li><a href="#">Operations</a>
                    <ul>
                        <li>
                            <stripes:link class="link-plain" href="/source.action?export=&harvestSource.url=${actionBean.uri}">Export triples</stripes:link>
                        </li>
                        <li>
                            <stripes:link class="link-plain" href="/sparql">
                                <stripes:param name="default-graph-uri" value="${actionBean.uri}" />
                                SPARQL endpoint
                            </stripes:link>
                        </li>
                    </ul>
                 </li>
             </ul>

                <c:if test="${param.search!=null}">
                    <stripes:layout-render name="/pages/common/subjectsResultList.jsp" tableClass="sortable"/>
                </c:if>

            </c:when>
            <c:otherwise>
                <div>&nbsp;</div>
            </c:otherwise>
        </c:choose>

    </stripes:layout-component>
</stripes:layout-render>
