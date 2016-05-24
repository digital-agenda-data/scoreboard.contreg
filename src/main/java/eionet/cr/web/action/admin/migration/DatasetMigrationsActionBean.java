package eionet.cr.web.action.admin.migration;

import java.util.List;

import org.apache.log4j.Logger;

import eionet.cr.config.MigratableCR;
import eionet.cr.dao.DAOException;
import eionet.cr.dto.DatasetMigrationDTO;
import eionet.cr.service.DatasetMigrationsService;
import eionet.cr.service.ServiceException;
import eionet.cr.web.action.AbstractActionBean;
import eionet.cr.web.action.admin.AdminWelcomeActionBean;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationMethod;

/**
 * ActionBean for creating/listing/deleting dataset migration packages in UI admin section.
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
@UrlBinding("/admin/migrations.action")
public class DatasetMigrationsActionBean extends AbstractActionBean {

    /** */
    private static final Logger LOGGER = Logger.getLogger(DatasetMigrationsActionBean.class);

    /** */
    private static final String DATASET_MIGRATIONS_JSP = "/pages/admin/migration/dataset_migrations.jsp";

    /** */
    private List<DatasetMigrationDTO> migrations;

    /** */
    private DatasetMigrationDTO newMigration;

    /**
     *
     * @return
     * @throws DAOException
     */
    @DefaultHandler
    public Resolution defaultHandler() {
        return new ForwardResolution(DATASET_MIGRATIONS_JSP);
    }

    /**
     *
     * @return
     * @throws ServiceException
     */
    public Resolution startNewMigration() throws ServiceException {

        LOGGER.debug("Starting new migration: " + newMigration);

        DatasetMigrationsService.newInstance().startNewMigration(newMigration.getSourceCrUrl(), newMigration.getSourcePackageName(),
                newMigration.getTargetDatasetUri(), newMigration.isPrePurge(), newMigration.getUserName());

        addSystemMessage("Migration started, refresh this page to monitor progress!");
        return new RedirectResolution(getClass());
    }

    /**
    *
    */
    @ValidationMethod(on = "startNewMigration")
    public void validateStartNewMigration() {

        if (newMigration == null) {
            newMigration = new DatasetMigrationDTO();
        }

        try {
            newMigration.validateForStart();
        } catch (IllegalArgumentException e) {
            addGlobalValidationError(e.getMessage());
            getContext()
                    .setSourcePageResolution(new ForwardResolution(DATASET_MIGRATIONS_JSP).addParameter("startNewValidationErrors", Boolean.TRUE));
        }
    }

    /**
    *
    */
    @ValidationMethod(priority = 0)
    public void validateUserAuthorised() {

        if (getUser() == null || !getUser().isAdministrator()) {
            addGlobalValidationError("You are not authorized for this operation!");
            getContext().setSourcePageResolution(new RedirectResolution(AdminWelcomeActionBean.class));
        }
    }

    /**
     *
     * @return
     */
    public List<MigratableCR> getSourceCrs() {
        return DatasetMigrationsService.newInstance().listMigratableCRInstances();
    }

    /**
     * @return the migrations
     */
    public List<DatasetMigrationDTO> getMigrations() {
        return migrations;
    }

    /**
     * @return the newMigration
     */
    public DatasetMigrationDTO getNewMigration() {
        return newMigration;
    }

    /**
     * @param newMigration the newMigration to set
     */
    public void setNewMigration(DatasetMigrationDTO newMigration) {
        this.newMigration = newMigration;
    }
}
