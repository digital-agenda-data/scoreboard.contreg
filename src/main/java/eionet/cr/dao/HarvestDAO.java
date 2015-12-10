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
package eionet.cr.dao;

import java.util.List;

import eionet.cr.dto.HarvestDTO;
import eionet.cr.dto.HarvestStatDTO;

/**
 *
 * @author heinljab
 *
 */
public interface HarvestDAO extends DAO {

    /**
     *
     * @param harvestSourceId
     * @param harvestType
     * @param user
     * @return int
     * @throws DAOException
     */
    int insertStartedHarvest(int harvestSourceId, String harvestType, String user) throws DAOException;

    /**
     *
     * @param harvestId
     * @param noOfTriples
     * @param httpCode HTTP Code the source returns
     * @throws DAOException
     */
    void updateFinishedHarvest(int harvestId, int noOfTriples, int httpCode) throws DAOException;

    /**
     *
     * @param harvestSourceId
     * @return List<HarvestDTO>
     * @throws DAOException
     */
    List<HarvestDTO> getHarvestsBySourceId(Integer harvestSourceId) throws DAOException;

    /**
     * Returns list of the last harvests statistics.
     *
     * @param limit how many harvests
     * @return List<HarvestStatDTO>
     * @throws DAOException
     */
    List<HarvestStatDTO> getLastHarvestStats(Integer limit) throws DAOException;

    /**
     *
     * @param harvestId
     * @return HarvestDTO
     * @throws DAOException
     */
    HarvestDTO getHarvestById(Integer harvestId) throws DAOException;

    /**
     *
     * @param harvestSourceId
     * @return HarvestDTO
     * @throws DAOException
     */
    HarvestDTO getLastHarvestBySourceId(Integer harvestSourceId) throws DAOException;

    /**
     * Deletes all the old harvest of the current harvest's harvest source, except the most recent.
     *
     * @param harvestId current harvest's id
     * @param preserveRecent nr of most recent messages to preserve
     * @throws DAOException
     */
    void deleteOldHarvests(int harvestId, int preserveRecent) throws DAOException;

    /**
     * Returns last harvest that has really happened.
     * Harvests that have returned http codes like 304 : source not modified are not queried.
     *
     * @param harvestSourceId Harvest PK
     * @return HarvestDTO Harvest data object
     * @throws DAOException if query fails
     */
    HarvestDTO getLastRealHarvestBySourceId(Integer harvestSourceId) throws DAOException;

}
