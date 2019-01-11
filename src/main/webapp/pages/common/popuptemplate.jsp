<%@ include file="/pages/common/taglibs.jsp"%>

<%@page import="eionet.cr.web.util.BaseUrl"%>

<stripes:layout-definition>
    <%@ page contentType="text/html;charset=UTF-8" language="java"%>

    <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
        <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
            <meta name="Publisher" content="EEA, The European Environment Agency" />
            <meta name="Rights" content="Copyright EEA Copenhagen 2003-2008" />
            <base href="<%= BaseUrl.getBaseUrl(request) %>"/>

            <title>${initParam.appDispName} - ${pageTitle}</title>

            <link rel="stylesheet" type="text/css" href="<c:url value="/css/eionet2007/print.css"/>" media="print" />
            <link rel="stylesheet" type="text/css" href="<c:url value="/css/eionet2007/handheld.css"/>" media="handheld" />
            <link rel="stylesheet" type="text/css" href="<c:url value="/css/eionet2007/screen.css"/>" media="screen"/>
            <link rel="stylesheet" type="text/css" href="<c:url value="/css/eionet2007.css"/>" media="screen"/>
            <link rel="stylesheet" type="text/css" href="<c:url value="/css/application.css"/>" media="screen"/>
            <link rel="shortcut icon" href="<c:url value="/favicon.ico"/>" type="image/x-icon" />

            <script type="text/javascript" src="<c:url value="/scripts/jquery-1.6.2.min.js"/>"></script>
            <script type="text/javascript" src="<c:url value="/scripts/jquery-timers.js"/>"></script>
            <script type="text/javascript" src="<c:url value="/scripts/jquery.autocomplete.js"/>"></script>
            <script type="text/javascript" src="<c:url value="/scripts/util.js"/>"></script>
            <script type="text/javascript" src="<c:url value="/scripts/pageops.js"/>"></script>
        </head>
        <body class="popup">

            <div id="popuphead">

            <div id="sitetitle">${siteTitle}</div>
              </div>
              <div id="popupbody">
                <stripes:layout-component name="contents"/>
            </div>
        </body>
    </html>
</stripes:layout-definition>
