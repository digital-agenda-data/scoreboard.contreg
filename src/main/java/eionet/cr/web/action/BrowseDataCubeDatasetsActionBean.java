package eionet.cr.web.action;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.common.TempFilePathGenerator;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.dto.SearchResultDTO;
import eionet.cr.service.DatasetMetadataService;
import eionet.cr.util.FileDeletionJob;
import eionet.cr.util.Pair;
import eionet.cr.util.SortingRequest;
import eionet.cr.util.pagination.PagingRequest;
import eionet.cr.util.sql.PairReader;
import eionet.cr.web.action.factsheet.FactsheetActionBean;
import eionet.cr.web.util.CustomPaginatedList;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.ValidationMethod;

/**
 * An action bean enabling to browse subjects whose rdf:type is that of {@link Subjects.DATACUBE_DATA_SET}.
 *
 * @author jaanus
 */
@UrlBinding("/dataCubeDatasets")
public class BrowseDataCubeDatasetsActionBean extends DisplaytagSearchActionBean {

    /** */
    private static final Logger LOGGER = Logger.getLogger(BrowseDataCubeDatasetsActionBean.class);

    /** */
    private static final String[] LABEL_PREDICATES =
            {Predicates.DCTERMS_TITLE, Predicates.RDFS_LABEL, Predicates.DC_TITLE, Predicates.FOAF_NAME};

    /** */
    private static final List<HashMap<String, String>> AVAIL_COLUMNS = createAvailColumns();

    /** */
    private static final String JSP = "/pages/browseDataCubeDatasets.jsp";

    /** */
    private CustomPaginatedList<Pair<String, String>> datasets;

    /** Properties submitted from the "create new dataset" form. */
    private String identifier;
    private String dctermsTitle;
    private String dctermsDescription;

    /** File bean for importing datasets metadata. */
    private FileBean importFileBean;

    /**
     *
     * @return
     */
    @DefaultHandler
    public Resolution defaultEvent() {

        String sortColumn = getSortColumnByAlias(sort);
        SortingRequest sortRequest = StringUtils.isBlank(sortColumn) ? null : SortingRequest.create(sortColumn, dir);
        PagingRequest pageRequest = PagingRequest.create(page);

        SearchResultDTO<Pair<String, String>> searchResult = null;
        ScoreboardSparqlDAO dao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
        try {
            searchResult = dao.getDistinctDatasets(isUserLoggedIn(), pageRequest, sortRequest, LABEL_PREDICATES);
        } catch (DAOException e) {
            LOGGER.error("DataCube datasets search error", e);
            addWarningMessage("A technical error occurred when searching for the available datasets" + e.getMessage());
        }

        datasets = new CustomPaginatedList<Pair<String, String>>(this, searchResult, pageRequest.getItemsPerPage());
        return new ForwardResolution(JSP);
    }

    /**
     *
     * @return
     */
    public Resolution createNew() {

        try {
            DatasetMetadataService.newInstance().createDataset(identifier, dctermsTitle, dctermsDescription, null);
            addSystemMessage("A new dataset with identifier \"" + identifier + "\" successfully created!");
        } catch (Exception e) {
            LOGGER.error("Dataset creation failed with technical error", e);
            addWarningMessage("Dataset creation failed with technical error: " + e.getMessage());
        }

        return new RedirectResolution(getClass());
    }

    /**
     * @throws DAOException
     *
     */
    @ValidationMethod(on = {"createNew"})
    public void validateCreateNew() throws DAOException {

        if (getUser() == null || !getUser().isAdministrator()) {
            addGlobalValidationError("You are not authorized for this operation!");
            getContext().setSourcePageResolution(new RedirectResolution(getClass()));
            return;
        }

        if (StringUtils.isBlank(identifier)) {
            addGlobalValidationError("The identifier is mandatory!");
        } else {
            String s = identifier.replaceAll("[^a-zA-Z0-9-._]+", "");
            if (!s.equals(identifier)) {
                addGlobalValidationError("Only digits, latin letters, underscores and dashes allowed in the identifier!");
            } else {
                boolean datasetExists = DAOFactory.get().getDao(ScoreboardSparqlDAO.class).datasetExists(identifier);
                if (datasetExists) {
                    addGlobalValidationError("A dataset already exists by this identifier: " + identifier);
                }
            }
        }

        if (StringUtils.isBlank(dctermsTitle)) {
            addGlobalValidationError("The title is mandatory!");
        }

        getContext().setSourcePageResolution(defaultEvent());
    }

    /**
     *
     * @return
     */
    public Resolution importNew() {

        File tempFile = TempFilePathGenerator.generateWithExtension("xls");
        try {
            importFileBean.save(tempFile);
        } catch (IOException e) {
            addCautionMessage("Failed saving the file to a temporary location!");
            LOGGER.error("Failed saving " + importFileBean.getFileName() + " to " + tempFile);
            return new RedirectResolution(getClass());
        }

        try {
            int nrOfDatasets = DatasetMetadataService.newInstance().importFile(tempFile);
            addSystemMessage(String.format("A total of %d datasets were imported!", nrOfDatasets));
        } catch (Exception e) {
            LOGGER.error("Datasets import failed with technical error", e);
            addWarningMessage("Datasets import failed with technical error: " + e.getMessage());
        } finally {
            FileDeletionJob.register(tempFile);
        }

        return new RedirectResolution(getClass());
    }

    /**
     *
     * @throws DAOException
     */
    @ValidationMethod(on = {"importNew"})
    public void validateImportNew() throws DAOException {

        if (getUser() == null || !getUser().isAdministrator()) {
            addGlobalValidationError("You are not authorized for this operation!");
            getContext().setSourcePageResolution(new RedirectResolution(getClass()));
            return;
        }

        if (importFileBean == null || importFileBean.getSize() == 0) {
            addGlobalValidationError("Uploaded file missing or empty!");
        }

        getContext().setSourcePageResolution(defaultEvent());
    }

    /**
     * @return the datasets
     */
    public CustomPaginatedList<Pair<String, String>> getDatasets() {
        return datasets;
    }

    /**
     *
     * @param alias
     * @return
     */
    private String getSortColumnByAlias(String alias) {

        String predicate = null;
        if (StringUtils.isNotBlank(alias)) {
            for (HashMap<String, String> columnConf : AVAIL_COLUMNS) {
                if (alias.equals(columnConf.get("alias"))) {
                    predicate = columnConf.get("sortColumn");
                    break;
                }
            }
        }

        return predicate;
    }

    /**
     *
     * @return
     */
    public Class getFactsheetActionBeanClass() {
        return FactsheetActionBean.class;
    }

    /**
     *
     * @return
     */
    public List<HashMap<String, String>> getAvailColumns() {
        return AVAIL_COLUMNS;
    }

    /**
     *
     * @return
     */
    private static List<HashMap<String, String>> createAvailColumns() {

        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();

        HashMap<String, String> map = new HashMap<String, String>();
        map.put("alias", "uri");
        map.put("isFactsheetLink", "true");
        map.put("title", "URI");
        map.put("hint", "The URI of the dataset");
        map.put("sortable", Boolean.TRUE.toString());
        map.put("sortColumn", PairReader.LEFTCOL);
        map.put("width", "60%");
        list.add(map);

        map = new HashMap<String, String>();
        map.put("alias", "label");
        map.put("title", "Label");
        map.put("hint", "The label of the dataset");
        map.put("sortable", Boolean.TRUE.toString());
        map.put("sortColumn", PairReader.RIGHTCOL);
        map.put("width", "40%");
        list.add(map);

        return Collections.unmodifiableList(list);
    }

    /**
     * @param dctermsTitle the dctermsTitle to set
     */
    public void setDctermsTitle(String dctermsTitle) {
        this.dctermsTitle = dctermsTitle;
    }

    /**
     * @param dctermsDescription the dctermsDescription to set
     */
    public void setDctermsDescription(String dctermsDescription) {
        this.dctermsDescription = dctermsDescription;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @param importFileBean the importFileBean to set
     */
    public void setImportFileBean(FileBean fileBean) {
        this.importFileBean = fileBean;
    }
}
