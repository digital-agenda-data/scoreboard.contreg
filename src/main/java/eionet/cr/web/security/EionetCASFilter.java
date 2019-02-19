/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Content Registry 2.0.
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency.  Portions created by Tieto Eesti are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 * Jaanus Heinlaid, Tieto Eesti
 */
package eionet.cr.web.security;

import edu.yale.its.tp.cas.client.filter.CASFilter;
import eionet.cr.web.util.CrCasFilterConfig;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import static eionet.cr.web.util.WebConstants.*;

/**
 *
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
public class EionetCASFilter extends CASFilter {

    /** */
    private static final Logger LOGGER = Logger.getLogger(EionetCASFilter.class);

    /** */
    private static String casLoginUrl = null;
    private static String serverName = null;

    /*
     * (non-Javadoc)
     *
     * @see edu.yale.its.tp.cas.client.filter.CASFilter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig config) throws ServletException {

        LOGGER.debug(getClass().getSimpleName() + " initializing");

        CrCasFilterConfig filterConfig = CrCasFilterConfig.getInstance(config);

        casLoginUrl = filterConfig.getInitParameter(CASFilter.LOGIN_INIT_PARAM);
        serverName = filterConfig.getInitParameter(CASFilter.SERVERNAME_INIT_PARAM);

        super.init(filterConfig);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.yale.its.tp.cas.client.filter.CASFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
     * javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc) throws ServletException, IOException {

        if (request.getParameter("action") != null && !request.getParameter("action").equalsIgnoreCase("list_services")) {
            fc.doFilter(request, response);
            return;
        }

        CASFilterChain chain = new CASFilterChain();
        super.doFilter(request, response, chain);
        if (chain.isDoNext()) {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpSession session = httpRequest.getSession();
            if (session != null) {

                String userName = ((String) session.getAttribute(CAS_FILTER_USER)).toLowerCase();
                session.setAttribute(USER_SESSION_ATTR, new CRUser(userName));

                String requestURI = httpRequest.getRequestURI();
                if (requestURI.endsWith("/login")) {

                    String redirectUrl = httpRequest.getContextPath() + LOGIN_ACTION + "?" + AFTER_LOGIN_EVENT;
                    ((HttpServletResponse) response).sendRedirect(redirectUrl);
                    return;
                }
            }

            fc.doFilter(request, response);
            return;
        }
    }

    /**
     *
     * @param request
     * @return
     */
    public static String getCASLoginURL(HttpServletRequest request) {
        return casLoginUrl + "?service=" + request.getScheme() + "://" + serverName + request.getContextPath() + "/login";
    }

    /**
     *
     * @param req
     * @param forSubscription
     * @return
     */
    public static String getCASLoginURL(HttpServletRequest req, boolean forSubscription) {

        StringBuffer sb = new StringBuffer(casLoginUrl);
        sb.append("?service=");
        sb.append(req.getScheme());
        sb.append("://");
        sb.append(serverName);
        if (!req.getContextPath().equals("")) {
            sb.append(req.getContextPath());
        }
        sb.append("/login");
        if (forSubscription) {
            sb.append("?rd=subscribe");
        }
        return sb.toString();
    }

    /**
     *
     * @param request
     * @return
     */
    public static String getCASLogoutURL(HttpServletRequest request) {
        return casLoginUrl.replaceFirst("/login", "/logout") + "?url=" + request.getScheme() + "://" + serverName
                + request.getContextPath();
    }

    /**
     *
     * @param request
     * @return
     */
    public static boolean isCasLoggedUser(HttpServletRequest request) {
        return (request.getSession() != null && request.getSession().getAttribute(CAS_FILTER_USER) != null);
    }

    /**
     *
     * @param aRegexFragment
     * @return
     */
    public static String forRegex(String aRegexFragment) {
        final StringBuffer result = new StringBuffer();

        final StringCharacterIterator iterator = new StringCharacterIterator(aRegexFragment);
        char character = iterator.current();
        while (character != CharacterIterator.DONE) {
            /*
             * All literals need to have backslashes doubled.
             */
            if (character == '.') {
                result.append("\\.");
            } else if (character == '\\') {
                result.append("\\\\");
            } else if (character == '?') {
                result.append("\\?");
            } else if (character == '*') {
                result.append("\\*");
            } else if (character == '+') {
                result.append("\\+");
            } else if (character == '&') {
                result.append("\\&");
            } else if (character == ':') {
                result.append("\\:");
            } else if (character == '{') {
                result.append("\\{");
            } else if (character == '}') {
                result.append("\\}");
            } else if (character == '[') {
                result.append("\\[");
            } else if (character == ']') {
                result.append("\\]");
            } else if (character == '(') {
                result.append("\\(");
            } else if (character == ')') {
                result.append("\\)");
            } else if (character == '^') {
                result.append("\\^");
            } else if (character == '$') {
                result.append("\\$");
            } else {
                // the char is not a special one
                // add it to the result as is
                result.append(character);
            }
            character = iterator.next();
        }
        return result.toString();
    }

}

/**
 *
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
class CASFilterChain implements FilterChain {

    private boolean doNext = false;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) {
        doNext = true;
    }

    public boolean isDoNext() {
        return doNext;
    }
}
