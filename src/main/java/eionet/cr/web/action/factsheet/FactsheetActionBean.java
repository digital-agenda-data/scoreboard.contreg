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
package eionet.cr.web.action.factsheet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import eionet.cr.util.URIUtil;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationMethod;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;

import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.config.GeneralConfig;
import eionet.cr.dao.CompiledDatasetDAO;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.dao.SpoBinaryDAO;
import eionet.cr.dao.virtuoso.PredicateObjectsReader;
import eionet.cr.dataset.CurrentLoadedDatasets;
import eionet.cr.dto.DatasetDTO;
import eionet.cr.dto.FactsheetDTO;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.dto.ObjectDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.dto.TripleDTO;
import eionet.cr.harvest.CurrentHarvests;
import eionet.cr.harvest.HarvestException;
import eionet.cr.harvest.OnDemandHarvester;
import eionet.cr.harvest.scheduled.UrgentHarvestQueue;
import eionet.cr.harvest.util.CsvImportUtil;
import eionet.cr.util.Pair;
import eionet.cr.util.URLUtil;
import eionet.cr.util.Util;
import eionet.cr.web.action.AbstractActionBean;
import eionet.cr.web.action.BrowseCodelistsActionBean;
import eionet.cr.web.action.source.ViewSourceActionBean;
import eionet.cr.web.util.ApplicationCache;
import eionet.cr.web.util.HTMLSelectOption;
import eionet.cr.web.util.tabs.FactsheetTabMenuHelper;
import eionet.cr.web.util.tabs.TabElement;
import eionet.cr.web.util.tabs.TabId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Factsheet.
 *
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
@UrlBinding("/factsheet.action")
public class FactsheetActionBean extends AbstractActionBean {

    /** Substring by which URIs of codelists and their members are checked. */
    private static final String CODELIST_SUBSTRING = "/codelist/";

    /** Name of the resource file containing the list of fully editable types. */
    private static final String FULLY_EDITABLE_TYPES_FILE_NAME = "fully-editable-types.txt";

    /** */
    private static final String DATASET_EDITABLE_PROPERTIES_FILE_NAME = "dataset-editable-properties.json";

    /** */
    private static final Logger LOGGER = Logger.getLogger(FactsheetActionBean.class);

    /**  */
    public static final String PAGE_PARAM_PREFIX = "page";

    /** */
    private static final String ADDBL_PROPS_SESSION_ATTR_PREFIX = "addibleProperties_";

    /** */
    private static final List<HTMLSelectOption> DATACUBE_DATASET_ADDBL_PROPS = createDataCubeDatasetAddibleProperties();

    /** */
    private static final List<HTMLSelectOption> COMMON_ADDBL_PROPS = createCommonAddibleProperties();

    /** */
    private static final Set<String> EDITABLE_TYPES = createEditableTypes();

    /** URI by which the factsheet has been requested. */
    private String uri;

    /** URI hash by which the factsheet has been requested. Ignored when factsheet requested by URI. */
    private long uriHash;

    /** The subject data object found by the requestd URI or URI hash. */
    private FactsheetDTO subject;

    /** Used in factsheet edit mode only, where it indicates if the subject is anonymous. */
    private boolean anonymous;

    /** */
    private String propertyUri;
    /** */
    private String propertyValue;

    /** */
    private String propertyValueMd5;

    /** List of identifiers of property-value rows submitted from factsheet edit form. */
    private List<String> rowId;

    /** True if the session bears a user and it happens to be an administrator. Otherwise false. */
    private boolean adminLoggedIn;

    /** True if the found subject is a bookmark of the logged-in user. In all other cases false. */
    private Boolean subjectIsUserBookmark;

    /** True if the found subject has downloadable content in filestore. */
    private Boolean subjectDownloadable;

    /** True, if URI is harvest source. */
    private boolean uriIsHarvestSource;

    /** True, if URI is a graph. */
    private boolean uriIsGraph;

    /** True, if URI is local folder. */
    private boolean uriIsFolder;

    /** */
    private String bookmarkLabel;

    /** */
    private Map<String, Integer> predicatePageNumbers;
    private Map<String, Integer> predicatePageCounts;

    /** */
    private List<TabElement> tabs;

    /** */
    private Boolean subjectIsType = null;

    /** */
    private String predicateUri;
    private String objectMD5;
    private String graphUri;

    /** */
    private List<DatasetDTO> userCompiledDatasets;

    /** */
    private HarvestSourceDTO harvestSourceDTO;

    /** */
    private SubjectDTO fullSubjectDTO;

    /** */
    private Boolean isAllEditable = null;

    /** */
    private String sourceUri;

    /**
     *
     * @return Resolution
     * @throws DAOException
     *             if query fails
     */
    @DefaultHandler
    public Resolution view() throws DAOException {

        if (isNoCriteria()) {
            addCautionMessage("No request criteria specified!");
        } else {
            HelperDAO helperDAO = DAOFactory.get().getDao(HelperDAO.class);

            setAdminLoggedIn(getUser() != null && getUser().isAdministrator());

            subject = helperDAO.getFactsheet(uri, null, getPredicatePageNumbers());
            if (subject != null) {
                fullSubjectDTO = helperDAO.getSubject(uri);
            }

            FactsheetTabMenuHelper tabsHelper =
                    new FactsheetTabMenuHelper(uri, fullSubjectDTO, factory.getDao(HarvestSourceDAO.class));

            tabs = tabsHelper.getTabs(TabId.RESOURCE_PROPERTIES);
            uriIsHarvestSource = tabsHelper.isUriIsHarvestSource();
            uriIsGraph = tabsHelper.isUriIsGraph();
            uriIsFolder = tabsHelper.isUriFolder();
            harvestSourceDTO = tabsHelper.getHarvestSourceDTO();
        }

        return new ForwardResolution("/pages/factsheet/factsheet.jsp");
    }

    /**
     * Handle for ajax harvesting.
     *
     * @return Resolution
     */
    public Resolution harvestAjax() {
        String message;
        try {
            message = harvestNow().getRight();
        } catch (Exception ignored) {
            logger.error("error while scheduling ajax harvest", ignored);
            message = "Error occured, more info can be obtained in application logs";
        }
        return new StreamingResolution("text/html", message);
    }

    /**
     * Schedules a harvest for resource.
     *
     * @return view resolution
     * @throws HarvestException
     *             if harvesting fails
     * @throws DAOException
     *             if query fails
     */
    public Resolution harvest() throws HarvestException, DAOException {
        HelperDAO helperDAO = DAOFactory.get().getDao(HelperDAO.class);
        SubjectDTO subjectDto = helperDAO.getSubject(uri);

        if (subjectDto != null && CsvImportUtil.isSourceTableFile(subjectDto)) {
            // Harvest table file
            try {
                // harvestTableFile();
                List<String> warnings = CsvImportUtil.harvestTableFile(subjectDto, uri, getUserName());
                for (String msg : warnings) {
                    addWarningMessage(msg);
                }
                addSystemMessage("Source successfully harvested");
            } catch (Exception e) {
                logger.error("Failed to harvest table file", e);
                addWarningMessage("Failed to harvest table file: " + e.getMessage());
            }
        } else {
            // Harvest other source
            Pair<Boolean, String> message = harvestNow();
            if (message.getLeft()) {
                addWarningMessage(message.getRight());
            } else {
                addSystemMessage(message.getRight());
            }
        }
        return new RedirectResolution(this.getClass(), "view").addParameter("uri", uri);
    }

    /**
     * helper method to eliminate code duplication.
     *
     * @return Pair<Boolean, String> feedback messages
     * @throws HarvestException
     *             if harvesting fails
     * @throws DAOException
     *             if query fails
     */
    private Pair<Boolean, String> harvestNow() throws HarvestException, DAOException {

        String message = null;
        if (isUserLoggedIn()) {
            if (!StringUtils.isBlank(uri) && URLUtil.isURL(uri)) {

                /* add this url into HARVEST_SOURCE table */

                HarvestSourceDAO dao = factory.getDao(HarvestSourceDAO.class);
                HarvestSourceDTO dto = new HarvestSourceDTO();
                dto.setUrl(StringUtils.substringBefore(uri, "#"));
                dto.setEmails("");
                dto.setIntervalMinutes(Integer.valueOf(GeneralConfig.getProperty(GeneralConfig.HARVESTER_REFERRALS_INTERVAL,
                        String.valueOf(HarvestSourceDTO.DEFAULT_REFERRALS_INTERVAL))));
                dto.setPrioritySource(false);
                dto.setOwner(null);
                dao.addSourceIgnoreDuplicate(dto);

                /* issue an instant harvest of this url */

                OnDemandHarvester.Resolution resolution = OnDemandHarvester.harvest(dto.getUrl(), getUserName());

                /* give feedback to the user */

                if (resolution.equals(OnDemandHarvester.Resolution.ALREADY_HARVESTING)) {
                    message = "The resource is currently being harvested by another user or background harvester!";
                } else if (resolution.equals(OnDemandHarvester.Resolution.UNCOMPLETE)) {
                    message = "The harvest hasn't finished yet, but continues in the background!";
                } else if (resolution.equals(OnDemandHarvester.Resolution.COMPLETE)) {
                    message = "The harvest has been completed!";
                } else if (resolution.equals(OnDemandHarvester.Resolution.SOURCE_UNAVAILABLE)) {
                    message = "The resource was not available!";
                } else if (resolution.equals(OnDemandHarvester.Resolution.NO_STRUCTURED_DATA)) {
                    message = "The resource contained no RDF data!";
                    // else if (resolution.equals(InstantHarvester.Resolution.RECENTLY_HARVESTED))
                    // message = "Source redirects to another source that has recently been harvested! Will not harvest.";
                } else {
                    message = "No feedback given from harvest!";
                }
            }
            return new Pair<Boolean, String>(false, message);
        } else {
            return new Pair<Boolean, String>(true, getBundle().getString("not.logged.in"));
        }
    }

    /**
     *
     * @return Resolution
     * @throws DAOException
     *             if query fails if query fails
     */
    public Resolution edit() throws DAOException {

        return view();
    }

    /**
     *
     * @return Resolution
     * @throws DAOException
     *             if query fails if query fails
     */
    public Resolution addbookmark() throws DAOException {
        if (isUserLoggedIn()) {
            DAOFactory.get().getDao(HelperDAO.class).addUserBookmark(getUser(), getUrl(), bookmarkLabel);
            addSystemMessage("Succesfully bookmarked this source.");
        } else {
            addSystemMessage("Only logged in users can bookmark sources.");
        }
        return view();
    }

    /**
     *
     * @return Resolution
     * @throws DAOException
     *             if query fails
     */
    public Resolution removebookmark() throws DAOException {
        if (isUserLoggedIn()) {
            DAOFactory.get().getDao(HelperDAO.class).deleteUserBookmark(getUser(), getUrl());
            addSystemMessage("Succesfully removed this source from bookmarks.");
        } else {
            addSystemMessage("Only logged in users can remove bookmarks.");
        }
        return view();
    }

    /**
     * Adds a user-submitted property to the underlying subject.
     *
     * @return Resolution
     * @throws DAOException
     *             if query fails if query fails
     */
    public Resolution add() throws DAOException {

        RedirectResolution resolution = new RedirectResolution(this.getClass(), "edit").addParameter("uri", uri);

        if (StringUtils.isBlank(propertyUri) || StringUtils.isBlank(propertyValue)) {
            addWarningMessage("Property URI or value empty!");
            return resolution;
        }

        String trimmedPropertyUri = propertyUri.trim();
        if (!URIUtil.isAbsoluteURI(trimmedPropertyUri)) {
            addWarningMessage("Not a valid absolute URI: " + trimmedPropertyUri);
            return resolution;
        }

        TripleDTO triple = new TripleDTO(uri, trimmedPropertyUri, propertyValue, sourceUri);
        triple.setAnonymousSubject(anonymous);
        DAOFactory.get().getDao(ScoreboardSparqlDAO.class).addUserTriple(triple, getUser());

        addSystemMessage("Property successfully saved!");
        return resolution;
    }

    /**
     *
     * @param subjectUri
     * @param anonymousSubject
     * @param propertyUri
     * @param propertyValue
     * @param sourceUri
     * @throws DAOException
     */
    private void addTripleByUser(String subjectUri, boolean anonymousSubject, String propertyUri, String propertyValue, String sourceUri) throws DAOException {

        // Prepare blank subject DTO for collecting the triples about to be saved.
        SubjectDTO subjectDTO = new SubjectDTO(subjectUri, anonymousSubject);

        // URI of property (i.e. predicate) that is saved. Special case if it's cr:tag.
        if (propertyUri.equals(Predicates.CR_TAG)) {

            List<String> tags = Util.splitStringBySpacesExpectBetweenQuotes(propertyValue);
            for (String tag : tags) {
                ObjectDTO objectDTO = new ObjectDTO(tag, true);
                objectDTO.setSourceUri(sourceUri);
                subjectDTO.addObject(propertyUri, objectDTO);
            }
        } else {
            // If saved property is not cr:tag.
            // Add saved predicate-object pair into subject DTO.
            boolean isLiteral = !URLUtil.isURL(propertyValue);
            ObjectDTO objectDTO = new ObjectDTO(propertyValue, isLiteral);
            objectDTO.setSourceUri(sourceUri);
            subjectDTO.addObject(propertyUri, objectDTO);
        }

        // Add saved triples into repository.
        HelperDAO helperDao = factory.getDao(HelperDAO.class);
        helperDao.addTriples(subjectDTO);
        helperDao.updateUserHistory(getUser(), subjectUri);

        // Since user registrations URI was used as triple source, add it to HARVEST_SOURCE too
        // (but set interval minutes to 0, to avoid it being background-harvested)
        if (sourceUri != null && sourceUri.equals(getUser().getRegistrationsUri())) {
            DAOFactory
                    .get()
                    .getDao(HarvestSourceDAO.class)
                    .addSourceIgnoreDuplicate(
                            HarvestSourceDTO.create(sourceUri, true, 0, getUser().getUserName()));
        }
    }

    /**
     *
     * @return
     * @throws DAOException
     */
    public Resolution delete() throws DAOException {

        RedirectResolution resolution = new RedirectResolution(this.getClass(), "edit").addParameter("uri", uri);
        if (StringUtils.isBlank(propertyUri) || StringUtils.isBlank(propertyValueMd5)) {
            addWarningMessage("Property URI or value MD5 empty!");
            return resolution;
        }

        String trimmedPropertyUri = propertyUri.trim();
        if (!URIUtil.isAbsoluteURI(trimmedPropertyUri)) {
            addWarningMessage("Not a valid absolute URI: " + trimmedPropertyUri);
            return resolution;
        }

        TripleDTO triple = new TripleDTO(uri, trimmedPropertyUri, null, sourceUri);
        triple.setObjectMd5(propertyValueMd5);
        triple.setAnonymousSubject(anonymous);

        DAOFactory.get().getDao(ScoreboardSparqlDAO.class).deleteUserTriple(triple, getUser());
        addSystemMessage("Property successfully deleted!");
        return resolution;
    }

    /**
     *
     * @return
     * @throws DAOException
     */
    public Resolution save() throws DAOException {

        RedirectResolution resolution = new RedirectResolution(this.getClass(), "edit").addParameter("uri", uri);

        if (StringUtils.isBlank(propertyUri) || StringUtils.isBlank(propertyValue)) {
            addWarningMessage("Property URI or value empty!");
            return resolution;
        }

        String trimmedPropertyUri = propertyUri.trim();
        if (!URIUtil.isAbsoluteURI(trimmedPropertyUri)) {
            addWarningMessage("Not a valid absolute URI: " + trimmedPropertyUri);
            return resolution;
        }

        TripleDTO triple = new TripleDTO(uri, trimmedPropertyUri, propertyValue, sourceUri);
        triple.setAnonymousSubject(anonymous);
        String oldValue = getContext().getRequestParameter("oldPropertyValueMd5");

        DAOFactory.get().getDao(ScoreboardSparqlDAO.class).updateUserTriple(triple, oldValue, getUser());
        addSystemMessage("Property successfully saved!");
        return resolution;
    }

    /**
     * Validates if user is logged on and if event property is not empty.
     */
    @ValidationMethod(on = {"save", "delete", "edit", "harvest"})
    public void validateUserKnown() {

        if (getUser() == null) {
            addWarningMessage("Operation not allowed for anonymous users");
        } else if (getContext().getEventName().equals("save") && StringUtils.isBlank(propertyValue)) {
            addGlobalValidationError(new SimpleError("Property value must not be blank"));
        }
    }

    /**
     * @return the resourceUri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param resourceUri
     *            the resourceUri to set
     */
    public void setUri(final String resourceUri) {
        this.uri = resourceUri;
    }

    /**
     * @return the resource
     */
    public FactsheetDTO getSubject() {
        return subject;
    }

    /**
     *
     * @return
     * @throws DAOException
     */
    @SuppressWarnings("unchecked")
    public List<HTMLSelectOption> getAddibleProperties() throws DAOException {

        String sessionAttrName = ADDBL_PROPS_SESSION_ATTR_PREFIX + uri;
        List<HTMLSelectOption> result = (List<HTMLSelectOption>) getContext().getSessionAttribute(sessionAttrName);

        if (CollectionUtils.isEmpty(result)) {

            // Get the subject's RDF types.
            Collection<String> rdfTypes = fullSubjectDTO == null ? null : fullSubjectDTO.getObjectValues(Predicates.RDF_TYPE);

            // Special case for DataCube datasets.
            if (rdfTypes.contains(Subjects.DATACUBE_DATA_SET)) {
                getContext().setSessionAttribute(sessionAttrName, DATACUBE_DATASET_ADDBL_PROPS);
                return DATACUBE_DATASET_ADDBL_PROPS;
            }

            // Get addible properties from the repository.
            HelperDAO dao = DAOFactory.get().getDao(HelperDAO.class);
            Map<String, HTMLSelectOption> optionMap = dao.getAddibleProperties(uri, rdfTypes);

            // Add some hard-coded addable properties.
            for (HTMLSelectOption htmlSelectOption : COMMON_ADDBL_PROPS) {
                optionMap.put(htmlSelectOption.getValue(), htmlSelectOption);
            }

            result = new ArrayList<HTMLSelectOption>();
            result.addAll(optionMap.values());
            Collections.sort(result, new BeanComparator("label"));

            getContext().setSessionAttribute(sessionAttrName, result);
        }

        LinkedHashSet<HTMLSelectOption> resultSet = new LinkedHashSet<>(result);
        if (fullSubjectDTO != null && fullSubjectDTO.getPredicateCount() > 0) {
            Set<String> predicateUris = fullSubjectDTO.getPredicateUris();
            for (String predUri : predicateUris) {
                resultSet.add(HTMLSelectOption.createFromUri(predUri, StringUtils.EMPTY));
            }
        }

        result = new ArrayList<>(resultSet);
        Collections.sort(result, new BeanComparator("labelUppercase"));

        return result;
    }

    /**
     * @param anonymous
     *            the anonymous to set
     */
    public void setAnonymous(final boolean anonymous) {
        this.anonymous = anonymous;
    }

    /**
     * @param propertyUri
     *            the propertyUri to set
     */
    public void setPropertyUri(final String propertyUri) {
        this.propertyUri = propertyUri;
    }

    /**
     * @param propertyValue
     *            the propertyValue to set
     */
    public void setPropertyValue(final String propertyValue) {
        this.propertyValue = propertyValue;
    }

    /**
     * @param rowId
     *            the rowId to set
     */
    public void setRowId(final List<String> rowId) {
        this.rowId = rowId;
    }

    /**
     * @return the noCriteria
     */
    public boolean isNoCriteria() {
        return StringUtils.isBlank(uri);
    }

    /**
     * @return the uriHash
     */
    public long getUriHash() {
        return uriHash;
    }

    /**
     * @param uriHash
     *            the uriHash to set
     */
    public void setUriHash(final long uriHash) {
        this.uriHash = uriHash;
    }

    /**
     *
     * @return String
     */
    public String getUrl() {
        return uri != null && URLUtil.isURL(uri) ? uri : null;
    }

    /**
     * True if admin is logged in.
     *
     * @return boolean
     */
    public boolean isAdminLoggedIn() {
        return adminLoggedIn;
    }

    /**
     * Setter of admin logged in property.
     *
     * @param adminLoggedIn
     *            boolean
     */
    public void setAdminLoggedIn(final boolean adminLoggedIn) {
        this.adminLoggedIn = adminLoggedIn;
    }

    /**
     *
     * @return boolean
     * @throws DAOException
     *             if query fails if query fails
     */
    public boolean getSubjectIsUserBookmark() throws DAOException {

        if (!isUserLoggedIn()) {
            return false;
        }

        if (subjectIsUserBookmark == null) {
            subjectIsUserBookmark = Boolean.valueOf(factory.getDao(HelperDAO.class).isSubjectUserBookmark(getUser(), uri));
        }

        return subjectIsUserBookmark.booleanValue();
    }

    /**
     * @return the subjectDownloadable
     * @throws DAOException
     */
    public boolean isSubjectDownloadable() throws DAOException {

        if (subjectDownloadable == null) {
            subjectDownloadable = Boolean.valueOf(DAOFactory.get().getDao(SpoBinaryDAO.class).exists(uri));
        }
        return subjectDownloadable.booleanValue();
    }

    /**
     *
     * @return boolean
     */
    public boolean isCurrentlyHarvested() {

        return uri == null ? false : (CurrentHarvests.contains(uri) || UrgentHarvestQueue.isInQueue(uri) || CurrentLoadedDatasets
                .contains(uri));
    }

    /**
     *
     * @return boolean
     */
    public boolean isCompiledDataset() {

        boolean ret = false;

        if (subject.getObject(Predicates.RDF_TYPE) != null) {
            ret = Subjects.CR_COMPILED_DATASET.equals(subject.getObject(Predicates.RDF_TYPE).getValue());
        }

        return ret;
    }

    /**
     *
     * @return Resolution
     * @throws DAOException
     */
    public Resolution showOnMap() throws DAOException {
        HelperDAO helperDAO = DAOFactory.get().getDao(HelperDAO.class);
        subject = helperDAO.getFactsheet(uri, null, null);
        if (subject != null) {
            fullSubjectDTO = helperDAO.getSubject(uri);
        }

        FactsheetTabMenuHelper helper = new FactsheetTabMenuHelper(uri, fullSubjectDTO, factory.getDao(HarvestSourceDAO.class));
        tabs = helper.getTabs(TabId.SHOW_ON_MAP);
        return new ForwardResolution("/pages/factsheet/map.jsp");
    }

    public boolean isUriIsHarvestSource() {
        return uriIsHarvestSource;
    }

    /**
     *
     * @return
     */
    public String getBookmarkLabel() {
        return bookmarkLabel;
    }

    /**
     *
     * @param bookmarkLabel
     */
    public void setBookmarkLabel(String bookmarkLabel) {
        this.bookmarkLabel = bookmarkLabel;
    }

    /**
     * @return the predicatePages
     */
    public Map<String, Integer> getPredicatePageNumbers() {

        if (predicatePageNumbers == null) {

            predicatePageNumbers = new HashMap<String, Integer>();
            HttpServletRequest request = getContext().getRequest();
            Map<String, String[]> paramsMap = request.getParameterMap();

            if (paramsMap != null && !paramsMap.isEmpty()) {

                for (Map.Entry<String, String[]> entry : paramsMap.entrySet()) {

                    String paramName = entry.getKey();
                    if (isPredicatePageParam(paramName)) {

                        int pageNumber = NumberUtils.toInt(paramName.substring(PAGE_PARAM_PREFIX.length()));
                        if (pageNumber > 0) {

                            String[] predicateUris = entry.getValue();
                            if (predicateUris != null) {
                                for (String predUri : predicateUris) {
                                    predicatePageNumbers.put(predUri, pageNumber);
                                }
                            }
                        }
                    }
                }
            }
        }

        return predicatePageNumbers;
    }

    /**
     *
     * @param paramName
     * @return
     */
    public boolean isPredicatePageParam(String paramName) {

        if (paramName.startsWith(PAGE_PARAM_PREFIX) && paramName.length() > PAGE_PARAM_PREFIX.length()) {
            return StringUtils.isNumeric(paramName.substring(PAGE_PARAM_PREFIX.length()));
        } else {
            return false;
        }
    }

    /**
     *
     * @return
     */
    public int getPredicatePageSize() {

        return PredicateObjectsReader.PREDICATE_PAGE_SIZE;
    }

    /**
     *
     * @return
     */
    public List<TabElement> getTabs() {
        return tabs;
    }

    /**
     *
     * @return
     */
    public boolean getSubjectIsType() {

        if (subjectIsType == null) {

            List<String> typeUris = ApplicationCache.getTypeUris();
            subjectIsType = Boolean.valueOf(typeUris.contains(this.uri));
        }

        return subjectIsType;
    }

    /**
     *
     * @return
     */
    @HandlesEvent("openPredObjValue")
    public Resolution openPredObjValue() {

        logger.trace("Retrieving object value for MD5 " + objectMD5 + " of predicate " + predicateUri);
        String value = DAOFactory.get().getDao(HelperDAO.class).getLiteralObjectValue(uri, predicateUri, objectMD5, graphUri);
        if (StringUtils.isBlank(value)) {
            value = "Found no value!";
        } else {
            value = StringEscapeUtils.escapeXml(value);
        }
        return new StreamingResolution("text/html", value);
    }

    /**
     * @param predicateUri
     *            the predicateUri to set
     */
    public void setPredicateUri(String predicateUri) {
        this.predicateUri = predicateUri;
    }

    /**
     * @param objectMD5
     *            the objectMD5 to set
     */
    public void setObjectMD5(String objectMD5) {
        this.objectMD5 = objectMD5;
    }

    /**
     * @param graphUri
     *            the graphUri to set
     */
    public void setGraphUri(String graphUri) {
        this.graphUri = graphUri;
    }

    /**
     *
     * @return
     */
    public List<DatasetDTO> getUserCompiledDatasets() {
        if (userCompiledDatasets == null && !StringUtils.isBlank(uri)) {
            try {
                CompiledDatasetDAO dao = DAOFactory.get().getDao(CompiledDatasetDAO.class);
                userCompiledDatasets = dao.getCompiledDatasets(getUser().getHomeUri(), uri);
            } catch (DAOException e) {
                e.printStackTrace();
            }
        }
        return userCompiledDatasets;
    }

    /**
     *
     * @param userCompiledDatasets
     */
    public void setUserCompiledDatasets(List<DatasetDTO> userCompiledDatasets) {
        this.userCompiledDatasets = userCompiledDatasets;
    }

    /**
     *
     * @return
     */
    public Class getViewSourceActionBeanClass() {
        return ViewSourceActionBean.class;
    }

    /**
     *
     * @return
     */
    public boolean isUriIsFolder() {
        return uriIsFolder;
    }

    /**
     *
     * @param uriIsFolder
     */
    public void setUriIsFolder(boolean uriIsFolder) {
        this.uriIsFolder = uriIsFolder;

    }

    /**
     * @return the harvestSourceDTO
     */
    public HarvestSourceDTO getHarvestSourceDTO() {
        return harvestSourceDTO;
    }

    /**
     * @return the uriIsGraph
     */
    public boolean isUriIsGraph() {
        return uriIsGraph;
    }

    /**
     *
     * @return
     */
    public boolean isDataCubeDataset() {

        if (fullSubjectDTO == null) {
            return false;
        }

        Collection<String> types = fullSubjectDTO.getObjectValues(Predicates.RDF_TYPE);
        return types != null && types.contains(Subjects.DATACUBE_DATA_SET);
    }

    /**
     *
     * @return
     */
    public boolean isScoreboardCodelist() {

        if (fullSubjectDTO == null) {
            return false;
        }

        Collection<String> types = fullSubjectDTO.getObjectValues(Predicates.RDF_TYPE);
        if (types != null && types.contains(Subjects.SKOS_CONCEPT_SCHEME)) {
            return fullSubjectDTO.getUri().startsWith(BrowseCodelistsActionBean.CODELISTS_PREFIX);
        } else {
            return false;
        }
    }

    /**
     * @return
     */
    public boolean isAllEditable() {

        if (isAllEditable == null) {

            Collection<String> types = fullSubjectDTO == null ? null : fullSubjectDTO.getObjectValues(Predicates.RDF_TYPE);
            isAllEditable = types != null && EDITABLE_TYPES != null && CollectionUtils.containsAny(EDITABLE_TYPES, types);
        }

        return isAllEditable.booleanValue();
    }

    /**
     * Handler for the "Change dataset status" event. Relevant only if the underlying resource is a DataCube dataset.
     *
     * @return
     */
    public Resolution changeDatasetStatus() {

        String newStatus = getContext().getRequestParameter("datasetStatus");
        if (StringUtils.isBlank(newStatus)) {
            addWarningMessage("The new dataset status must be specified!");
        } else {
            List<String> allowableValues =
                    Arrays.asList("http://purl.org/adms/status/Completed", "http://purl.org/adms/status/UnderDevelopment",
                            "http://purl.org/adms/status/Deprecated", "http://purl.org/adms/status/Withdrawn");
            if (!allowableValues.contains(newStatus)) {
                addWarningMessage("Unrecognized dataset status value: " + newStatus);
            } else {
                try {
                    DAOFactory.get().getDao(ScoreboardSparqlDAO.class).changeDatasetStatus(uri, newStatus);
                    addSystemMessage("Datatset status successfully updated!");
                } catch (DAOException e) {
                    LOGGER.error("Technical error when attemptring to change the status of " + uri, e);
                    addWarningMessage("A technical error occurred when trying to change the dataset status: " + e.getMessage());
                }
            }
        }

        return new RedirectResolution(getClass()).addParameter("uri", uri);
    }

    /**
     *
     * @return
     */
    private static List<HTMLSelectOption> createDataCubeDatasetAddibleProperties() {

        ArrayList<HTMLSelectOption> resultList = new ArrayList<>();

        InputStream inputStream = null;
        JSONParser jsonParser = new JSONParser();
        try {
            inputStream = FactsheetActionBean.class.getClassLoader().getResourceAsStream(DATASET_EDITABLE_PROPERTIES_FILE_NAME);
            JSONArray objects = (JSONArray) jsonParser.parse(new InputStreamReader(inputStream));

            if (objects != null) {
                for (Object object : objects) {
                    JSONObject jsonObject = (JSONObject) object;
                    resultList.add(new HTMLSelectOption((String) jsonObject.get("uri"),
                            (String) jsonObject.get("label"), (String) jsonObject.get("hint")));
                }
            }

        } catch (IOException e) {
            LOGGER.error("IOException when finding/reading this resource file: " + DATASET_EDITABLE_PROPERTIES_FILE_NAME, e);
        } catch (ParseException e) {
            LOGGER.error("Failed parsing JSON from this resource file: " + DATASET_EDITABLE_PROPERTIES_FILE_NAME, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        Collections.sort(resultList, new Comparator<HTMLSelectOption>() {
            @Override
            public int compare(HTMLSelectOption o1, HTMLSelectOption o2) {
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });

        return resultList;
    }

    /**
     * @return
     */
    private static List<HTMLSelectOption> createCommonAddibleProperties() {

        ArrayList<HTMLSelectOption> result = new ArrayList<HTMLSelectOption>();

        HTMLSelectOption option = new HTMLSelectOption(Predicates.RDFS_LABEL, "Title");
        option.setTitle("RDF Schema label, i.e. a human-readable name that applications "
                + "can use for displaying in user interfaces. May be any free text.");
        result.add(option);

        option = new HTMLSelectOption(Predicates.CR_TAG, "Tag");
        result.add(option);

        option = new HTMLSelectOption(Predicates.RDFS_COMMENT, "Comment");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_TITLE, "Title");
        option.setTitle("DublinCore title. May be any free text.");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_ABSTRACT, "Abstract");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_CONTRIBUTOR, "Contributor");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_CREATOR, "Creator");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_DATE, "Date");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_DESCRIPTION, "Description");
        option.setTitle("DublinCore description, i.e. a human-readable description of the resource. May be any free text.");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_FORMAT, "Format");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_IDENTIFIER, "Identifier");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_LANGUAGE, "Language");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_LICENSE, "Licenese");
        option.setTitle("A legal document giving official permission to do something with the resource. "
                + "Usually a URL pointing to the document.");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_MODIFIED, "Modified");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_PUBLISHER, "Publisher");
        option.setTitle("DublinCore publisher, i.e. a person, an organization, or a service that is the resource's publisher. "
                + "May be any free text, but it is advised to use URLs.");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_RIGHTS, "Rights");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_SOURCE, "Source");
        result.add(option);

        option = new HTMLSelectOption(Predicates.DCTERMS_SUBJECT, "Subject");
        result.add(option);

        option = new HTMLSelectOption(Predicates.RDFS_DOMAIN, "rdfs:domain");
        result.add(option);

        result.add(HTMLSelectOption.createFromUri(Predicates.SKOS_ALT_LABEL, "altLabel"));
        result.add(HTMLSelectOption.createFromUri(Predicates.SKOS_PREF_LABEL, "prefLabel"));
        result.add(HTMLSelectOption.createFromUri(Predicates.SKOS_NOTATION, "notation"));
        result.add(HTMLSelectOption.createFromUri(Predicates.SKOS_NOTE, "notes"));
        result.add(HTMLSelectOption.createFromUri(Predicates.SKOS_DEFINITION, "definition"));
        result.add(HTMLSelectOption.createFromUri(Predicates.SKOS_HAS_TOP_CONCEPT, "hasTopConcept"));

        return result;
    }

    /**
     *
     * @return
     */
    private static Set<String> createEditableTypes() {

        LinkedHashSet<String> result = new LinkedHashSet<String>();

        InputStream inputStream = null;
        try {
            inputStream = FactsheetActionBean.class.getClassLoader().getResourceAsStream(FULLY_EDITABLE_TYPES_FILE_NAME);
            List<String> lines = IOUtils.readLines(inputStream, "UTF-8");
            for (String line : lines) {
                result.add(line.trim());
            }
        } catch (IOException e) {
            LOGGER.error("Failed reading lines from " + FULLY_EDITABLE_TYPES_FILE_NAME, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        LOGGER.debug("Found these fully editable types: " + result);

        return result;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public void setPropertyValueMd5(String propertyValueMd5) {
        this.propertyValueMd5 = propertyValueMd5;
    }
}
