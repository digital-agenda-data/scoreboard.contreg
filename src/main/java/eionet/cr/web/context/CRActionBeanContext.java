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
/**
 *
 */
package eionet.cr.web.context;

import static eionet.cr.web.util.WebConstants.LAST_ACTION_URL_SESSION_ATTR;
import static eionet.cr.web.util.WebConstants.USER_SESSION_ATTR;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Resolution;
import eionet.cr.web.security.CRUser;
import eionet.cr.web.security.EionetCASFilter;

/**
 * Extension of stripes ActionBeanContext.
 *
 * @author altnyris
 *
 */
public class CRActionBeanContext extends ActionBeanContext {

    /** */
    private int severity;

    /** */
    private Resolution sourcePageResolution;

    /**
     * Wrapper method for {@link javax.servlet.ServletRequest.ServletRequest#getParameter(String)}.
     * <p>
     * The wrapper allows to avoid direct usage of {@link javax.servlet.http.HttpServletRequest}.
     *
     * @param parameterName parameter name.
     * @return corresponding parameter value from {@link javax.servlet.http.HttpServletRequest}.
     */
    public String getRequestParameter(String parameterName) {
        return getRequest().getParameter(parameterName);
    }

    /**
     * Wrapper method for {@link javax.servlet.http.HttpSession#setAttribute(String, Object).
     * The wrapper allows to avoid direct usage of {@link javax.servlet.http.HttpSession}.
     *
     * @param name session attribute name.
     * @param value session attribute value.
     */
    public void setSessionAttribute(String name, Object value) {
        getRequest().getSession().setAttribute(name, value);
    }

    /**
     * Wrapper method for {@link javax.servlet.http.HttpSession#getAttribute(String).
     * The wrapper allows to avoid direct usage of {@link javax.servlet.http.HttpSession}.
     *
     * @param name session attribute name.
     */
    public Object getSessionAttribute(String name) {
        return getRequest().getSession().getAttribute(name);
    }

    /**
     * Wrapper method for {@link javax.servlet.http.HttpSession#removeAttribute(String).
     * The wrapper allows to avoid direct usage of {@link javax.servlet.http.HttpSession}.
     *
     * @param name session attribute name.
     */
    public void removeSessionAttribute(String name) {
        getRequest().getSession().removeAttribute(name);
    }

    /**
     * Method returns {@link CRUser} from session.
     *
     * @return {@link CRUser} from session or null if user is not logged in.
     */
    public CRUser getCRUser() {

        return (CRUser) getRequest().getSession().getAttribute(USER_SESSION_ATTR);
    }

    /**
     * A wrapper for {@link EionetCASFilter#getCASLoginURL(javax.servlet.http.HttpServletRequest)}.
     *
     * @return central authentication system login URL.
     */
    public String getCASLoginURL() {
        return EionetCASFilter.getCASLoginURL(getRequest());
    }

    /**
     * A wrapper for {@link EionetCASFilter#getCASLogoutURL(javax.servlet.http.HttpServletRequest)}.
     *
     * @return central authentication system logout URL.
     */
    public String getCASLogoutURL() {
        return EionetCASFilter.getCASLogoutURL(getRequest());
    }

    /**
     *
     * @return last action event URL.
     */
    public String getLastActionEventUrl() {
        return (String) getRequest().getSession().getAttribute(LAST_ACTION_URL_SESSION_ATTR);
    }

    /**
     * @return
     */
    public int getSeverity() {
        return severity;
    }

    /**
     * @param severity
     */
    public void setSeverity(int severity) {
        this.severity = severity;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sourceforge.stripes.action.ActionBeanContext#getSourcePageResolution()
     */
    public Resolution getSourcePageResolution() {

        if (this.sourcePageResolution != null) {
            return this.sourcePageResolution;
        } else {
            return super.getSourcePageResolution();
        }
    }

    /**
     *
     * @param resolution
     */
    public void setSourcePageResolution(Resolution resolution) {
        this.sourcePageResolution = resolution;
    }

    /**
     * Gets application init parameter.
     *
     * @param key
     * @return String
     */
    public String getInitParameter(String key) {
        return getServletContext().getInitParameter(key);
    }
}
