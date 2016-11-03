<%@ include file="/pages/common/taglibs.jsp"%>

<%@page import="eionet.cr.web.util.BaseUrl"%>

<stripes:layout-definition>
    <%@ page contentType="text/html;charset=UTF-8" language="java"%>

    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">

    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
            <meta name="viewport" content="initial-scale=1.0" />
            <meta name="Publisher" content="DG Connect, European Commission's Directorate General for Communications Networks, Content and Technology" />

            <title>${initParam.appDispName} - ${pageTitle}</title>

            <link rel="stylesheet" type="text/css" href="<c:url value="/css/eionet2007/print.css"/>" media="print" />
            <link rel="stylesheet" type="text/css" href="<c:url value="/css/eionet2007/handheld.css"/>" media="handheld" />
            <link rel="stylesheet" type="text/css" href="<c:url value="/css/eionet2007/screen.css"/>" media="screen" title="Eionet 2007 style"/>
            <link rel="stylesheet" type="text/css" href="<c:url value="/css/eionet2007.css"/>" media="screen"/>
            <link rel="stylesheet" type="text/css" href="<c:url value="/css/application.css"/>" media="screen"/>
            <link rel="stylesheet" type="text/css" href="<c:url value="/css/scoreboard.css"/>" media="screen"/>
            
            <link rel="shortcut icon" href="<c:url value="/favicon.ico"/>" type="image/x-icon" />

            <link type="text/css" href="<c:url value="/css/smoothness/jquery-ui-1.8.16.custom.css" />" rel="stylesheet" />
            <script type="text/javascript" src="<c:url value="/scripts/jquery-1.6.2.min.js" />"></script>
            <script type="text/javascript" src="<c:url value="/scripts/jquery-ui-1.8.16.custom.min.js" />"></script>
            <script type="text/javascript" src="<c:url value="/scripts/jquery-timers.js"/>"></script>
            <script type="text/javascript" src="<c:url value="/scripts/jquery.autocomplete.js"/>"></script>

            <script type="text/javascript" src="<c:url value="/scripts/util.js"/>"></script>
            <script type="text/javascript" src="<c:url value="/scripts/pageops.js"/>"></script>
            <script type="text/javascript" src="<c:url value="/scripts/prototype.js"/>"></script>
            <script type="text/javascript" src="<c:url value="/scripts/map.js"/>"></script>

            <stripes:layout-component name="head"/>
        </head>
        <body ${bodyAttribute}>
        
            <div id="container" style="border:none;">
            
                <div id="scb-header">
                    <img class="scb-logo" src="${pageContext.request.contextPath}/images/ec_logo_en.gif" alt="European Commision logo" />
                    <p id="banner-title-text">Digital Agenda for Europe</p>
                    <p id="banner-title-text2">A Europe 2020 Initiative</p>
                    <ul class="scb-breadcrumbs"><li class="first"><a href="http://ec.europa.eu/index_en.htm">European Commission</a></li>
                        <li><a href="http://ec.europa.eu/digital-agenda/en">Digital Agenda for Europe</a></li>
                        <li><a href="http://ec.europa.eu/digital-agenda/en/scoreboard">Scoreboard</a></li>
                        <li><a href="${pageContext.request.contextPath}/">${initParam.appDispName}</a></li>
                        
                        <c:if test="${not empty pageTitle}">
                            <li><a href="#" onclick="return false;"><c:out value="${pageTitle}"/></a></li>
                        </c:if>
                    
                    </ul>
                </div>
                
                <div id="left-menu-and-workarea-wrapper">
                
                    <stripes:layout-component name="navigation">
                        <jsp:include page="/pages/common/navigation.jsp"/>
                    </stripes:layout-component>

                    <div id="workarea" class="documentContent">

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
                
                <div class="scb-layout-footer">
                    <div class="scb-layout-footer-wrapper">
                        <div class="scb-region scb-region-footer">
                            <div id="block-menu-menu-get-involved" class="block block-menu">
                            
                                <h2>Get Involved</h2>
                                <div class="scb-content">
                                    <ul class="menu clearfix">
                                        <li class="first leaf">
                                        
                                            <c:choose>
                                                <c:when test="${empty crUser}">
                                                    <stripes:link id="personaltools-login" title="Login" href="/login.action" event="login">Log in</stripes:link>
                                                </c:when>
                                                <c:otherwise>
                                                    <stripes:link id="personaltools-login" title="Logout" href="/login.action" event="logout">Log out user: ${crUser.userName}</stripes:link>
                                                </c:otherwise>
                                            </c:choose>
                                            
                                        </li>
                                        <li class="leaf"><a href="https://ec.europa.eu/digital-agenda/en/newsroom" title="" class="">Newsroom</a></li>
                                        <li class="last leaf"><a href="https://ec.europa.eu/digital-agenda/en/blog_home" title="" class="">Blog</a></li>
                                    </ul>
                                </div>
                            </div>

                            <div id="block-boxes-haveyoursaybox" class="block block-boxes block-boxes-simple">
                                <div class="scb-content">
                                    <div id="boxes-box-haveyoursaybox" class="boxes-box">
                                        <div class="boxes-box-content">
                                            <p><a href="http://daa.ec.europa.eu/"><img border="0" src="${pageContext.request.contextPath}/images/dae-button.png" /></a></p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <p id="scb-copyright"> © Copyright 2013 European Commission</p>
                </div>
                
            </div>
        </body>
    </html>
</stripes:layout-definition>
