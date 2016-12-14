package eionet.cr.web.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.common.TempFilePathGenerator;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.dto.CubeDatasetTemplateDTO;
import eionet.cr.dto.SearchResultDTO;
import eionet.cr.service.CubeDatasetMetadataService;
import eionet.cr.service.DcatCatalogService;
import eionet.cr.service.ServiceException;
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
import net.sourceforge.stripes.action.StreamingResolution;
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
    private String datasetCatalogUri;

    /** File bean for importing datasets metadata. */
    private FileBean importFileBean;

    /** */
    private boolean clearExisting;

    /** */
    private List<Pair<String, String>> catalogs;

    /** */
    private String targetCatalogUri;

    /** */
    private String catalogIdentifier;
    private String catalogTitle;
    private String catalogDescription;

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
            CubeDatasetTemplateDTO dto = new CubeDatasetTemplateDTO(identifier, dctermsTitle, dctermsDescription, null);
            CubeDatasetMetadataService.newInstance().createDataset(dto, datasetCatalogUri);
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

        if (StringUtils.isBlank(datasetCatalogUri)) {
            addGlobalValidationError("Dataset catalog is mandatory!");
        }

        getContext().setSourcePageResolution(defaultEvent());
    }

    /**
     *
     * @return
     */
    public Resolution createNewCatalog() {

        try {
            DcatCatalogService.newInstance().createCatalog(catalogIdentifier, catalogTitle, catalogDescription);
            addSystemMessage("A new catalog with identifier \"" + catalogIdentifier + "\" successfully created!");
        } catch (Exception e) {
            LOGGER.error("Catalog creation failed with technical error", e);
            addWarningMessage("Catalog creation failed with technical error: " + e.getMessage());
        }

        return new RedirectResolution(getClass());
    }

    /**
     *
     * @throws DAOException
     */
    @ValidationMethod(on = {"createNewCatalog"})
    public void validateCreateNewCatalog() throws ServiceException {

        if (getUser() == null || !getUser().isAdministrator()) {
            addGlobalValidationError("You are not authorized for this operation!");
            getContext().setSourcePageResolution(new RedirectResolution(getClass()));
            return;
        }

        if (StringUtils.isBlank(catalogIdentifier)) {
            addGlobalValidationError("The identifier is mandatory!");
        } else {
            String s = catalogIdentifier.replaceAll("[^a-zA-Z0-9-._]+", "");
            if (!s.equals(catalogIdentifier)) {
                addGlobalValidationError("Only digits, latin letters, underscores and dashes allowed in the identifier!");
            } else {
                boolean exists = DcatCatalogService.newInstance().isCatalogExisting(catalogIdentifier);
                if (exists) {
                    addGlobalValidationError("A catalog already exists with this identifier: " + catalogIdentifier);
                }
            }
        }

        if (StringUtils.isBlank(catalogTitle)) {
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
            int nrOfDatasets = CubeDatasetMetadataService.newInstance().importDatasetsSpreadsheet(tempFile, targetCatalogUri, clearExisting);
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
     * @return
     * @throws ServiceException
     */
    public Resolution export() throws ServiceException {

        Pair<Integer, File> pair = CubeDatasetMetadataService.newInstance().exportDatasetsMetadata();
        int exportedCount = pair.getLeft();
        LOGGER.debug("Number of exported datasets: " + exportedCount);
        File targetFile = pair.getRight();
        return streamToResponse(targetFile, CubeDatasetMetadataService.DATASETS_SPREADSHEET_TEMPLATE_FILE_NAME);
    }

    /**
     *
     * @param file
     * @param targetName
     * @return
     */
    private StreamingResolution streamToResponse(final File file, String targetName) {

        return new StreamingResolution("application/vnd.ms-excel") {

            /*
             * (non-Javadoc)
             *
             * @see net.sourceforge.stripes.action.StreamingResolution#stream(javax.servlet.http.HttpServletResponse)
             */
            @Override
            public void stream(HttpServletResponse response) throws Exception {

                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    inputStream = new FileInputStream(file);
                    outputStream = response.getOutputStream();
                    IOUtils.copy(inputStream, outputStream);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                    IOUtils.closeQuietly(outputStream);
                    FileDeletionJob.register(file);
                }
            }
        }.setFilename(StringUtils.isBlank(targetName) ? file.getName() : targetName);
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
     * @return
     * @throws DAOException
     */
    public List<Pair<String, String>> getCatalogs() throws DAOException {

        if (catalogs == null) {
            String[] labels = {Predicates.DCTERMS_TITLE, Predicates.RDFS_LABEL, Predicates.FOAF_NAME};
            HelperDAO dao = DAOFactory.get().getDao(HelperDAO.class);
            SearchResultDTO<Pair<String, String>> searchResult = dao.getUriLabels(Subjects.DCAT_CATALOG, null, null, labels);
            if (searchResult != null) {
                catalogs = searchResult.getItems();
            }
        }
        return catalogs;
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

    /**
     * @param clearExisting the clearExisting to set
     */
    public void setClearExisting(boolean clearExisting) {
        this.clearExisting = clearExisting;
    }

    /**
     * @return the targetCatalogUri
     */
    public String getTargetCatalogUri() {
        return targetCatalogUri;
    }

    /**
     * @param targetCatalogUri the targetCatalogUri to set
     */
    public void setTargetCatalogUri(String targetCatalogUri) {
        this.targetCatalogUri = targetCatalogUri;
    }

    /**
     * @return the catalogIdentifier
     */
    public String getCatalogIdentifier() {
        return catalogIdentifier;
    }

    /**
     * @param catalogIdentifier the catalogIdentifier to set
     */
    public void setCatalogIdentifier(String catalogIdentifier) {
        this.catalogIdentifier = catalogIdentifier;
    }

    /**
     * @return the catalogTitle
     */
    public String getCatalogTitle() {
        return catalogTitle;
    }

    /**
     * @param catalogTitle the catalogTitle to set
     */
    public void setCatalogTitle(String catalogTitle) {
        this.catalogTitle = catalogTitle;
    }

    /**
     * @return the catalogDescription
     */
    public String getCatalogDescription() {
        return catalogDescription;
    }

    /**
     * @param catalogDescription the catalogDescription to set
     */
    public void setCatalogDescription(String catalogDescription) {
        this.catalogDescription = catalogDescription;
    }

    /**
     * @return the datasetCatalogUri
     */
    public String getDatasetCatalogUri() {
        return datasetCatalogUri;
    }

    /**
     * @param datasetCatalogUri the datasetCatalogUri to set
     */
    public void setDatasetCatalogUri(String datasetCatalogUri) {
        this.datasetCatalogUri = datasetCatalogUri;
    }
}
