package eionet.cr.web.action.admin;

import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.web.action.AbstractActionBean;
import eionet.cr.web.action.factsheet.FactsheetActionBean;
import net.sourceforge.stripes.action.*;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * An action that enables administrators to see codelist elements that don't have metadata.
 *
 * @author Jaanus
 */
@UrlBinding("/admin/bareCodelistElements.action")
public class BareCodelistElementsActionBean extends AbstractActionBean {

    /** Static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BareCodelistElementsActionBean.class);

    /** The default JSP to forward to. */
    private static final String DEFAULT_JSP = "/pages/admin/bareCodelistElements.jsp";

    /** */
    private List<String> codelistElements;
    private boolean adminLoggedIn = false;

    /**
     * @return
     * @throws DAOException
     */
    @DefaultHandler
    public Resolution view() throws DAOException {

        if (getUser() != null && getUser().isAdministrator()) {

            setAdminLoggedIn(true);
            codelistElements = DAOFactory.get().getDao(ScoreboardSparqlDAO.class).getBareCodelistElements();
        } else {
            setAdminLoggedIn(false);
        }

        return new ForwardResolution(DEFAULT_JSP);
    }

    /**
     *
     * @return
     */
    public List<String> getCodelistElements() {
        return codelistElements;
    }

    /**
     *
     * @return
     */
    public boolean isAdminLoggedIn() {
        return adminLoggedIn;
    }

    /**
     * @param adminLoggedIn
     */
    public void setAdminLoggedIn(boolean adminLoggedIn) {
        this.adminLoggedIn = adminLoggedIn;
    }

    /**
     *
     * @return
     */
    public Class getFactsheetActionBeanClass() {
        return FactsheetActionBean.class;
    }
}
