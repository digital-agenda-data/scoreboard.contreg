/*
 * The contents of this file are subject to the Mozilla Public
 *
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
 * Agency. Portions created by Tieto Eesti are Copyright
 * (C) European Environment Agency. All Rights Reserved.
 *
 * Contributor(s):
 * Jaanus Heinlaid, Tieto Eesti*/
package eionet.cr.web.action;

import org.apache.commons.lang.StringUtils;

import eionet.cr.dao.DAOException;
import eionet.cr.harvest.HarvestException;
import eionet.cr.util.URLUtil;
import eionet.cr.web.action.factsheet.FactsheetActionBean;
import eionet.cr.web.util.RegisterUrl;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationMethod;

/**
 *
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
@UrlBinding("/admin/registerUrl.action")
public class RegisterURLActionBean extends AbstractActionBean {

    private String url;
    private String label;
    private boolean bookmark = false;

    /**
     *
     * @return
     * @throws DAOException
     */
    @DefaultHandler
    public Resolution unspecified() {

        return new ForwardResolution("/pages/registerUrl.jsp");
    }

    /**
     *
     * @return
     * @throws DAOException
     * @throws HarvestException
     */
    public Resolution save() throws DAOException, HarvestException {

        // register URL
        url = URLUtil.escapeIRI(url);
        RegisterUrl.register(url, getUser(), bookmark, label);
        // go to factsheet in edit mode
        return new RedirectResolution(FactsheetActionBean.class, "edit").addParameter("uri", url);
    }

    /**
     *
     */
    @ValidationMethod(on = "save")
    public void validateSave() {

        if (StringUtils.isBlank(url) || !URLUtil.isURL(url)) {
            addGlobalValidationError(new SimpleError("Not a valid URL!"));
        }
        if (getUser() == null) {
            addGlobalValidationError(new SimpleError("You are not logged in!"));
        }
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @param bookmark the bookmark to set
     */
    public void setBookmark(boolean bookmark) {
        this.bookmark = bookmark;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
