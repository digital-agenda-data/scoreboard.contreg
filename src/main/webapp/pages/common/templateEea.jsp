<%@ include file="/pages/common/taglibs.jsp"%>

<%@page import="eionet.cr.web.util.BaseUrl"%>

<stripes:layout-definition>
<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="viewport" content="initial-scale=1.0" />
        <meta name="Publisher" content="EEA, The European Environment Agency" />
        <base href="<%= BaseUrl.getBaseUrl(request) %>"/>

        <title>${initParam.appDispName} - ${pageTitle}</title>

        <link rel="stylesheet" type="text/css" href="<c:url value="/css/eionet2007.css"/>" media="screen"/>
        <link rel="stylesheet" type="text/css" href="<c:url value="/css/application.css"/>" media="screen"/>
        <link rel="shortcut icon" href="<c:url value="/favicon.ico"/>" type="image/x-icon" />

        <link type="text/css" href="<c:url value="/css/smoothness/jquery-ui-1.8.16.custom.css" />" rel="stylesheet" />
        <script type="text/javascript" src="<c:url value="/scripts/jquery-1.6.2.min.js" />"></script>
        <script type="text/javascript" src="<c:url value="/scripts/jquery-ui-1.8.16.custom.min.js" />"></script>
        <script type="text/javascript" src="<c:url value="/scripts/jquery-timers.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/scripts/jquery.autocomplete.js"/>"></script>
        <%--
        //remove conflicting javascript library
        //TODO check if jquery-ui-min.js is needed on maps.js, when the functionality uses sparql
        <script type="text/javascript" src="<c:url value="/scripts/jquery-ui.min.js"/>"></script>
        --%>
        <script type="text/javascript" src="<c:url value="/scripts/util.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/scripts/pageops.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/scripts/prototype.js"/>"></script>
        <script type="text/javascript" src="<c:url value="/scripts/map.js"/>"></script>
        <stripes:layout-component name="head"/>
        <crfn:template file="required_head.html"/>
    </head>
    <body>
        <div id="visual-portal-wrapper">

        <crfn:template file="header.html"/>

        <!-- The wrapper div. It contains the three columns. -->
        <div id="portal-columns" class="visualColumnHideTwo">
            <!-- start of the main and left columns -->
            <div id="visual-column-wrapper">
                <!-- start of main content block -->
                <div id="portal-column-content">
                    <div id="content">
                        <div class="documentContent" id="region-content">
                            <a name="documentContent"></a>
                            <div class="documentActions">
                                <h5 class="hiddenStructure">Document Actions</h5>
                                   <ul>
                                    <li>
                                          <a href="javascript:this.print();">
                                              <img src="http://webservices.eea.europa.eu/templates/print_icon.gif"
                                              alt="Print this page" title="Print this page" />
                                          </a>
                                     </li>
                                    <li>
                                          <a href="javascript:toggleFullScreenMode();">
                                              <img src="http://webservices.eea.europa.eu/templates/fullscreenexpand_icon.gif"
                                            alt="Toggle full screen mode" title="Toggle full screen mode" />
                                          </a>
                                    </li>
                                  </ul>
                               </div>
                           <!--  validation errors -->
                            <stripes:errors/>

                            <!--  messages -->
                            <stripes:layout-component name="messages">
                                <c:if test="${not empty systemMessages}">
                                    <div class="system-msg">
                                        <stripes:messages key="systemMessages"/>
                                    </div>
                                </c:if>
                                <c:if test="${not empty cautionMessages}">
                                    <div class="caution-msg">
                                        <strong>Caution ...</strong>
                                        <stripes:messages key="cautionMessages"/>
                                    </div>
                                </c:if>
                                <c:if test="${not empty warningMessages}">
                                    <div class="warning-msg">
                                        <strong>Warning ...</strong>
                                        <stripes:messages key="warningMessages"/>
                                    </div>
                                </c:if>
                            </stripes:layout-component>

                            <!--  Home headers, content or default content -->
                            <c:choose>
                                <c:when test="${actionBean.homeContext}">
                                    <c:choose>
                                        <c:when test="${actionBean.userAuthorized || actionBean.showPublic}" >
                                            <div id="tabbedmenu">
                                                <ul>
                                                    <c:forEach items="${actionBean.tabs}" var="tab">
                                                        <c:if test="${actionBean.userAuthorized || tab.showPublic == actionBean.showpublicYes }" >
                                                            <c:choose>
                                                                  <c:when test="${actionBean.section == tab.tabType}" >
                                                                    <li id="currenttab"><span><c:out value="${tab.title}"/></span></li>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <li>
                                                                        <stripes:link href="${actionBean.baseHomeUrl}${actionBean.attemptedUserName}/${tab.tabType}">
                                                                            <c:out value="${tab.title}"/>
                                                                        </stripes:link>
                                                                    </li>
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </c:if>
                                                    </c:forEach>
                                                </ul>
                                            </div>
                                            <br style="clear:left" />
                                            <div style="margin-top:10px">
                                                <stripes:layout-component name="contents"/>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                                <div class="error-msg">
                                                ${actionBean.authenticationMessage}
                                                </div>
                                        </c:otherwise>
                                    </c:choose>
                                </c:when>
                                <c:otherwise>
                                    <stripes:layout-component name="contents"/>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
                <!-- end of main content block -->

                <!-- start of the left (by default at least) column -->
                <div id="portal-column-one">
                    <div class="visualPadding">
<dl class="portlet" id="portlet-navigation-tree">
  <dt class="portletHeader">
    <span class="portletTopLeft"></span>
    <a href="/" class="tile">Semantic Data Service</a>
    <span class="portletTopRight"></span>
  </dt>

  <dd class="portletItem lastItem">
                        <jsp:include page="/pages/common/navigation.jsp"/>
                        <ul>
                            <li>
                                       <c:choose>
                                            <c:when test="${empty crUser}">
                                                <stripes:link id="loginlink" title="Login" href="/login.action" event="login">Login</stripes:link>
                                            </c:when>
                                            <c:otherwise>
                                                <stripes:link id="logoutlink" title="Logout" href="/login.action" event="logout">Logout ${crUser.userName}</stripes:link>
                                            </c:otherwise>
                                        </c:choose>
                            </li>
                        </ul>
</dd>
</dl>
                    </div>
                </div>
                <!-- end of the left (by default at least) column -->
            </div>
            <!-- end of the main and left columns -->
            <div class="visualClear"><!-- --></div>
        </div>
        <!-- end column wrapper -->
        <crfn:template file="footer.html"/>
        </div>
    </body>
</html>
</stripes:layout-definition>
