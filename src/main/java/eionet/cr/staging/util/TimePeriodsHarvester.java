package eionet.cr.staging.util;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.apache.commons.collections.SetUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eionet.cr.common.Predicates;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.dto.ObjectDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.harvest.OnDemandHarvester;
import eionet.cr.util.LogUtil;
import eionet.cr.web.security.CRUser;

/**
 * Utility class that harvests all given Scoreboard time-periods recorded during an import from a staging DB or from an XLS file.
 * And applies the necessary business logic that goes with it.
 *
 * @author jaanus
 */
public class TimePeriodsHarvester {

    /** */
    private static final Logger LOGGER = Logger.getLogger(TimePeriodsHarvester.class);

    /** */
    public static final String TIME_PERIOD_CODELIST_URI = "http://semantic.digital-agenda-data.eu/codelist/time-period";

    /** */
    private Set<String> timePeriodUris;

    /** */
    private Logger exportLogger;

    /** */
    private int harvestedCount;

    /** */
    private int noOfNewPeriods;

    /**
     * Constructor a harvester for the given time-periods.
     *
     * @param timePeriodUris
     *            Given time-periods, can be null or empty in which case no harvest will be executed.
     */
    public TimePeriodsHarvester(Set<String> timePeriodUris) {
        this.timePeriodUris = timePeriodUris;
    }

    /**
     * Executes the harvest of the time periods for which this instance was constructed.
     */
    public void execute() {

        if (timePeriodUris == null || timePeriodUris.isEmpty()) {
            return;
        }

        // Get the time periods that have skos:notation before the harvest.

        DAOFactory daoFactory = DAOFactory.get();
        ScoreboardSparqlDAO ssDAO = daoFactory.getDao(ScoreboardSparqlDAO.class);
        Set<String> boundBefore = null;
        try {
            LOGGER.debug("Checking time periods BEFORE harvest");
            boundBefore = ssDAO.getSubjectsWithBoundProperty(Predicates.SKOS_NOTATION, timePeriodUris);
            noOfNewPeriods = timePeriodUris.size() - boundBefore.size();
        } catch (DAOException e) {
            LOGGER.warn("Failed to check if time periods have skos:noation BEFORE harvest", e);
        }

        // Do the harvest.

        HarvestSourceDAO harvestSourceDAO = daoFactory.getDao(HarvestSourceDAO.class);
        for (String timePeriodUri : timePeriodUris) {
            LOGGER.debug("Going to harvest time period: " + timePeriodUri);
            harvestTimePeriod(timePeriodUri, harvestSourceDAO);
            harvestedCount++;
        }

        // Get the time periods that have skos:notation AFTER the harvest.
        Set<String> boundAfter = null;
        try {
            LOGGER.debug("Checking time periods AFTER harvest");
            boundAfter = ssDAO.getSubjectsWithBoundProperty(Predicates.SKOS_NOTATION, timePeriodUris);
        } catch (DAOException e) {
            LOGGER.warn("Failed to check if time periods have skos:noation AFTER harvest", e);
        }

        String codelistUri = TIME_PERIOD_CODELIST_URI;
        String codelistGraphUri = codelistUri + "/";

        // Compare the BEFORE and AFTER sets: if they're unequal, it means there was a change, so lets update
        // dcterms:modified of time-period codelist.

        if (!SetUtils.isEqualSet(boundBefore, boundAfter)) {
            try {
                Date date = new Date();
                LOGGER.debug("Setting dcterms:modified of " + codelistUri + " to " + date);
                ssDAO.updateSubjectModificationDate(codelistUri, date, codelistGraphUri);
            } catch (DAOException e) {
                LOGGER.warn("Failed to update dcterms:modified of " + codelistUri, e);
            }
        } else {
            LOGGER.debug("The BEFORE and AFTER sets of time periods were equal!");
        }

        // Ensure that time-period codelist indeed has a dcterms:modified after all the above.

        try {
            Set<String> set =
                    ssDAO.getSubjectsWithBoundProperty(Predicates.DCTERMS_MODIFIED,
                            Collections.singleton(TIME_PERIOD_CODELIST_URI));
            if (set.isEmpty()) {
                try {
                    Date date = new Date();
                    LOGGER.debug("Setting dcterms:modified of " + codelistUri + " to " + date);
                    ssDAO.updateSubjectModificationDate(codelistUri, date, codelistGraphUri);
                } catch (DAOException e) {
                    LOGGER.warn("Failed to update dcterms:modified of " + codelistUri, e);
                }
            }
        } catch (DAOException e) {
            LOGGER.warn("Failed to check if dcterms:modified exists for " + codelistUri, e);
        }

        // Ensure the harvested time-periods are associated with the time periods codelist

        SubjectDTO subjectDTO = new SubjectDTO(codelistUri, false);
        for (String timePeriodUri : timePeriodUris) {
            ObjectDTO objectDTO = ObjectDTO.createResource(timePeriodUri);
            objectDTO.setSourceUri(codelistGraphUri);
            subjectDTO.addObject(Predicates.SKOS_HAS_TOP_CONCEPT, objectDTO);
        }
        try {
            LOGGER.debug("Associating harvested time periods with codelist " + codelistUri);
            daoFactory.getDao(HelperDAO.class).addTriples(subjectDTO);
        } catch (DAOException e) {
            LOGGER.warn("Failed associating harvested time periods with codelist " + codelistUri);
        }
    }

    /**
     *
     * @param timePeriodUri
     * @param dao
     */
    private void harvestTimePeriod(String timePeriodUri, HarvestSourceDAO dao) {

        HarvestSourceDTO harvestSourceDTO = new HarvestSourceDTO();
        harvestSourceDTO.setUrl(StringUtils.substringBefore(timePeriodUri, "#"));
        harvestSourceDTO.setEmails("");
        harvestSourceDTO.setIntervalMinutes(0);
        harvestSourceDTO.setPrioritySource(false);
        harvestSourceDTO.setOwner(null);

        try {
            dao.addSourceIgnoreDuplicate(harvestSourceDTO);
            OnDemandHarvester.harvest(harvestSourceDTO.getUrl(), CRUser.APPLICATION.getUserName());
        } catch (Exception e) {
            LOGGER.error("Failed to harvest time period: " + timePeriodUri, e);
            LogUtil.warn("Failed to harvest time period: " + timePeriodUri, exportLogger);
        }
    }

    /**
     * @return the harvestedCount
     */
    public int getHarvestedCount() {
        return harvestedCount;
    }

    /**
     * @return the noOfNewPeriods
     */
    public int getNoOfNewPeriods() {
        return noOfNewPeriods;
    }
}
