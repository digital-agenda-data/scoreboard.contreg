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
package eionet.cr.web.util;

/**
 *
 * @author heinljab
 *
 */
public interface WebConstants {

    /** */
    int MAX_OBJECT_LENGTH = 800;

    /** CR user session attribute name. */
    String USER_SESSION_ATTR = "crUser";

    /** Specifies login action constant value. */
    String LOGIN_ACTION = "/login.action";

    String MAIN_PAGE_ACTION = "/";

    /** Specifies login event name. */
    String LOGIN_EVENT = "login";

    /** Specifies logout event name. */
    String LOGOUT_EVENT = "logout";

    /** Specifies presentation value for not logged in user. */
    String ANONYMOUS_USER_NAME = "anononymous";

    /** Specifies session attribute name where last action URL is kept. */
    String LAST_ACTION_URL_SESSION_ATTR = "ActionEventInterceptor#lastActionUrl";

    /** Specifies after login event name. */
    String AFTER_LOGIN_EVENT = "afterLogin";

    /** Delivery filter parameter separator. */
    String FILTER_SEPARATOR = "||";

    /** Delivery filter parameter's label separator. */
    String FILTER_LABEL_SEPARATOR = "|";

    /** Not available. */
    String NOT_AVAILABLE = "N/A";
}
