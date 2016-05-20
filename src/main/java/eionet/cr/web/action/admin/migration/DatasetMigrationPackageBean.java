package eionet.cr.web.action.admin.migration;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dto.DatasetMigrationPackageDTO;
import eionet.cr.dto.SearchResultDTO;
import eionet.cr.service.DatasetMigrationPackageService;
import eionet.cr.service.ServiceException;
import eionet.cr.util.Pair;
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
@UrlBinding("/admin/migrationPackages.action")
public class DatasetMigrationPackageBean extends AbstractActionBean {

    /** */
    private static final Logger LOGGER = Logger.getLogger(DatasetMigrationPackageBean.class);

    /** */
    private static final String DATASET_MIGRATION_PACKAGES_JSP = "/pages/admin/migration/dataset_migration_packages.jsp";

    /** */
    private List<DatasetMigrationPackageDTO> packages;

    /** */
    private List<Pair<String, String>> datasets;

    /** */
    private DatasetMigrationPackageDTO newPackage;

    /**
     *
     * @return
     * @throws DAOException
     */
    @DefaultHandler
    public Resolution defaultHandler() {
        return new ForwardResolution(DATASET_MIGRATION_PACKAGES_JSP);
    }

    /**
     *
     * @return
     * @throws ServiceException
     */
    public Resolution createNewPackage() throws ServiceException {

        LOGGER.debug("Creating new package: " + newPackage + " ...");

        newPackage.setIdentifier(DatasetMigrationPackageDTO.buildPackageIdentifier(newPackage.getDatasetUri(), getUserName(), new Date()));
        DatasetMigrationPackageService.newInstance().createNew(newPackage);
        addSystemMessage("New package being created! Refresh this page to check if it's finished.");
        return new RedirectResolution(getClass());
    }

    /**
     *
     * @return
     * @throws ServiceException
     */
    public Resolution delete() throws ServiceException {
        addCautionMessage("Deletion not implemented yet!");
        return new RedirectResolution(getClass());
    }

    /**
     *
     */
    @ValidationMethod(on = "createNewPackage")
    public void validateCreateNewPackage() {

        if (newPackage == null) {
            newPackage = new DatasetMigrationPackageDTO();
        }

        try {
            newPackage.validateForNew();
        } catch (IllegalArgumentException e) {
            addGlobalValidationError(e.getMessage());
            getContext().setSourcePageResolution(
                    new ForwardResolution(DATASET_MIGRATION_PACKAGES_JSP).addParameter("createNewValidationErrors", Boolean.TRUE));
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
     * @return the packages
     * @throws ServiceException
     */
    public List<DatasetMigrationPackageDTO> getPackages() throws ServiceException {

        if (packages == null) {
            packages = DatasetMigrationPackageService.newInstance().listAll();
        }
        return packages;
    }

    /**
     *
     * @return
     * @throws DAOException
     */
    public List<Pair<String, String>> getDatasets() throws DAOException {

        if (datasets == null) {
            String[] labels = {Predicates.DCTERMS_TITLE, Predicates.RDFS_LABEL, Predicates.FOAF_NAME};
            HelperDAO dao = DAOFactory.get().getDao(HelperDAO.class);
            SearchResultDTO<Pair<String, String>> searchResult = dao.getUriLabels(Subjects.DATACUBE_DATA_SET, null, null, labels);
            if (searchResult != null) {
                datasets = searchResult.getItems();
            }
        }
        return datasets;
    }

    /**
     * @param newPackage the newPackage to set
     */
    public void setNewPackage(DatasetMigrationPackageDTO newPackage) {
        this.newPackage = newPackage;
    }

    /**
     * @return the newPackage
     */
    public DatasetMigrationPackageDTO getNewPackage() {
        return newPackage;
    }
}
