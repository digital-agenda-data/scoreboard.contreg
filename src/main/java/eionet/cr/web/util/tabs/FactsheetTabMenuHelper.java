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
 * The Original Code is Content Registry 3
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency. Portions created by TripleDev or Zero Technologies are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *        Juhan Voolaid
 */

package eionet.cr.web.util.tabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.web.action.BrowseCodelistsActionBean;

/**
 * Helper for creating factsheet tab menu.
 *
 * @author Juhan Voolaid
 */
public final class FactsheetTabMenuHelper {

    /** */
    private static final Map<String, String> LABELLED_TYPES = createLabelledTypes();

    /** The subject data object found by the requestd URI or URI hash. */
    private final SubjectDTO subject;

    private boolean uriIsGraph;
    private boolean uriIsHarvestSource;
    private boolean mapDisplayable;
    private boolean sparqlBookmarkType;
    private boolean compiledDatasetType;
    private boolean folderType;
    private boolean bookmarksFileType;
    private boolean registrationsFileType;
    private boolean historyFileType;
    private boolean reviewType;
    private boolean tableFileType;

    /** */
    private String latitude;
    private String longitude;

    /** */
    private HarvestSourceDTO harvestSourceDTO;

    /** The subject's RDF types. */
    private HashSet<String> rdfTypes = new HashSet<String>();

    /**
     *
     * Class constructor.
     *
     * @param uri
     * @param subject
     * @param harvesterSourceDao
     * @throws DAOException
     */
    public FactsheetTabMenuHelper(String uri, SubjectDTO subject, HarvestSourceDAO harvesterSourceDao) throws DAOException {
        if (subject == null) {
            subject = new SubjectDTO(uri, false);
        }

        this.subject = subject;

        harvestSourceDTO = harvesterSourceDao.getHarvestSourceByUrl(subject.getUri());
        uriIsHarvestSource = harvestSourceDTO != null;
        uriIsGraph = DAOFactory.get().getDao(HelperDAO.class).isGraphExists(uri);

        // TODO: mapDisplayable = Subjects.WGS_SPATIAL_THING.equals(subject.getObject(Predicates.RDF_TYPE).getValue());
        mapDisplayable = subject.getObject(Predicates.WGS_LAT) != null && subject.getObject(Predicates.WGS_LONG) != null;
        if (mapDisplayable) {
            latitude = subject.getObject(Predicates.WGS_LAT).getValue();
            longitude = subject.getObject(Predicates.WGS_LONG).getValue();
        }

        // Populated the map of RDF types of this subject.
        rdfTypes.addAll(subject.getObjectValues(Predicates.RDF_TYPE));

        // Set some type-indicating fields, based on the subject's RDF types.
        sparqlBookmarkType = rdfTypes.contains(Subjects.CR_SPARQL_BOOKMARK);
        compiledDatasetType = rdfTypes.contains(Subjects.CR_COMPILED_DATASET);
        tableFileType = rdfTypes.contains(Subjects.CR_TABLE_FILE);
        reviewType = rdfTypes.contains(Subjects.CR_REVIEW_FOLDER) || rdfTypes.contains(Subjects.CR_FEEDBACK);
        bookmarksFileType = rdfTypes.contains(Subjects.CR_BOOKMARKS_FILE);
        registrationsFileType = rdfTypes.contains(Subjects.CR_REGISTRATIONS_FILE);
        historyFileType = rdfTypes.contains(Subjects.CR_HISTORY_FILE);
        folderType = rdfTypes.contains(Subjects.CR_FOLDER) || rdfTypes.contains(Subjects.CR_USER_FOLDER);
    }

    /**
     *
     * @param selectedTab
     * @return
     */
    public List<TabElement> getTabs(TabId selectedTab) {
        List<TabElement> result = new ArrayList<TabElement>();

        boolean isScoreboardCodelist = isScoreboardCodelist();
        String typeLabel = getTypeLabel();
        if (isScoreboardCodelist) {
            typeLabel = "Codelist";
        }

        String title = typeLabel != null ? typeLabel + " properties" : null;
        TabElement te1 = new TabElement(TabId.RESOURCE_PROPERTIES, title, "/factsheet.action", selectedTab);
        te1.addParam("uri", subject.getUri());
        result.add(te1);

        title = typeLabel != null ? typeLabel + " references" : null;
        TabElement te2 = new TabElement(TabId.RESOURCE_REFERENCES, title, "/references.action", selectedTab);
        te2.setEvent("search");
        te2.addParam("uri", subject.getUri());
        result.add(te2);

        if (displayObjectsInSourceTab()) {

            TabElement te3 = createObjectsInSourceTab(selectedTab);
            result.add(te3);
        }

        result.addAll(getTypeSpecificTabs(selectedTab));
        return result;
    }

    /**
     * Returns the list of tab objects without a selected tab.
     *
     * @return
     */
    public List<TabElement> getTypeSpecificTabs() {
        return getTypeSpecificTabs(null);
    }

    /**
     *
     * @param selectedTab
     * @return
     */
    public List<TabElement> getTypeSpecificTabs(TabId selectedTab) {

        List<TabElement> result = new ArrayList<TabElement>();

        if (mapDisplayable) {
            TabElement t = new TabElement(TabId.SHOW_ON_MAP, "/factsheet.action", selectedTab);
            t.setEvent("showOnMap");
            t.addParam("uri", subject.getUri());
            t.addParam("latitude", latitude);
            t.addParam("longitude", longitude);
            result.add(t);
        }

        if (sparqlBookmarkType) {
            TabElement t = new TabElement(TabId.BOOKMARKED_SPARQL, "/sparqlBookmark.action", selectedTab);
            t.addParam("uri", subject.getUri());
            result.add(t);
        }

        if (compiledDatasetType) {
            TabElement t = new TabElement(TabId.COMPILED_DATASET, "/compiledDataset.action", selectedTab);
            t.addParam("uri", subject.getUri());
            result.add(t);
        }

        if (reviewType) {
            TabElement t = new TabElement(TabId.REVIEW_FOLDER, "/reviews.action", selectedTab);
            t.addParam("uri", subject.getUri());
            result.add(t);
        }

        if (folderType) {
            TabElement t = new TabElement(TabId.FOLDER, "/folder.action", selectedTab);
            t.addParam("uri", subject.getUri());
            result.add(t);
        }

        if (bookmarksFileType) {
            TabElement t = new TabElement(TabId.BOOKMARKS, "/bookmarks.action", selectedTab);
            t.addParam("uri", subject.getUri());
            result.add(t);
        }

        if (registrationsFileType) {
            TabElement t = new TabElement(TabId.REGISTRATIONS, "/registrations.action", selectedTab);
            t.addParam("uri", subject.getUri());
            result.add(t);
        }

        if (historyFileType) {
            TabElement t = new TabElement(TabId.HISTORY, "/history.action", selectedTab);
            t.addParam("uri", subject.getUri());
            result.add(t);
        }

        if (tableFileType) {
            TabElement t = new TabElement(TabId.TABLE_FILE_CONTENTS, "/tableFile.action", selectedTab);
            t.addParam("uri", subject.getUri());
            result.add(t);
        }

        if (isDataCubeDataset()) {
            TabElement te4 = new TabElement(TabId.DATASET_INDICATORS, "/datasetIndicators.action", selectedTab);
            te4.addParam("datasetUri", subject.getUri());
            result.add(te4);
        }

        return result;
    }

    /**
     * @return the uriIsHarvestSource
     */
    public boolean isUriIsHarvestSource() {
        return uriIsHarvestSource;
    }

    /**
     * True if the resource is a local folder.
     */
    public boolean isUriFolder() {
        return folderType;
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
     * @return
     */
    private boolean displayObjectsInSourceTab() {
        return uriIsHarvestSource || uriIsGraph || rdfTypes.contains(Subjects.DATACUBE_DATA_SET) || isScoreboardCodelist();
    }

    /**
     * @param selectedTab
     * @return
     */
    private TabElement createObjectsInSourceTab(TabId selectedTab) {

        String tabTitle = null;
        String factsheetUri = null;
        String graphUri = subject.getUri();

        if (rdfTypes.contains(Subjects.DATACUBE_DATA_SET)) {
            tabTitle = "Dataset contents";
            factsheetUri = graphUri;
            graphUri = StringUtils.replace(graphUri, "/dataset/", "/data/");
        } else if (isScoreboardCodelist()) {
            tabTitle = "Codelist members";
            factsheetUri = graphUri;
            graphUri = graphUri + "/";
        } else {
            tabTitle = uriIsHarvestSource ? TabId.OBJECTS_IN_SOURCE.getTitle() : "Graph contents";
        }

        TabElement tab = new TabElement(TabId.OBJECTS_IN_SOURCE, tabTitle, "/objectsInSource.action", selectedTab);
        tab.setEvent("search");
        tab.addParam("uri", graphUri);
        if (StringUtils.isNotBlank(factsheetUri)) {
            tab.addParam("factsheetUri", factsheetUri);
        }

        return tab;
    }

    /**
     * @return
     */
    private boolean isDataCubeDataset() {
        return rdfTypes.contains(Subjects.DATACUBE_DATA_SET);
    }

    /**
     *
     * @return
     */
    private boolean isScoreboardCodelist() {
        String uri = subject.getUri();
        return rdfTypes.contains(Subjects.SKOS_CONCEPT_SCHEME) && uri.startsWith(BrowseCodelistsActionBean.CODELISTS_PREFIX);
    }

    /**
     *
     * @return
     */
    private String getTypeLabel() {

        for (Entry<String, String> entry : LABELLED_TYPES.entrySet()) {
            if (rdfTypes.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     *
     * @return
     */
    private static Map<String, String> createLabelledTypes() {

        HashMap<String, String> map = new HashMap<String, String>();

        map.put(Subjects.DATACUBE_DATA_SET, "Dataset");
        map.put(Subjects.DATACUBE_OBSERVATION, "Observation");
        map.put(Subjects.DAS_INDICATOR_GROUP, "Indicator group");
        map.put(Subjects.DAS_INDICATOR, "Indicator");
        map.put(Subjects.DAS_BREAKDOWN_GROUP, "Breakdown group");
        map.put(Subjects.DAS_BREAKDOWN, "Breakdown");
        map.put(Subjects.DAS_UNIT, "Unit measure");

        return map;
    }
}
