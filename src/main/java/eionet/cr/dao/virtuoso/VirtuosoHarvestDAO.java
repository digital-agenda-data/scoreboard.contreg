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
package eionet.cr.dao.virtuoso;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eionet.cr.dao.DAOException;
import eionet.cr.dao.HarvestDAO;
import eionet.cr.dao.readers.HarvestDTOReader;
import eionet.cr.dao.readers.HarvestStatReader;
import eionet.cr.dao.readers.HarvestWithMessageTypesReader;
import eionet.cr.dto.HarvestDTO;
import eionet.cr.dto.HarvestStatDTO;
import eionet.cr.harvest.HarvestConstants;
import eionet.cr.harvest.util.HarvestMessageType;
import eionet.cr.util.sql.SQLUtil;

/**
 *
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
public class VirtuosoHarvestDAO extends VirtuosoBaseDAO implements HarvestDAO {

    /** */
    private static final String GET_HARVEST_BY_ID_SQL = "select *, USERNAME as \"USER\" from HARVEST where HARVEST_ID=?";

    /** */
    private static final String GET_LAST_HARVEST_BY_SOURCE_ID_SQL = "select top 1 *, USERNAME as \"USER\""
            + " from HARVEST where HARVEST_SOURCE_ID=? order by HARVEST.STARTED desc";

    /**
     * SQL for last harvest record while harvest has not returned Source not modified http code.
     */
    private static final String GET_LAST_REAL_HARVEST_BY_SOURCE_ID_SQL = "select top 1 *, USERNAME as \"USER\""
            + " from HARVEST where HARVEST_SOURCE_ID=? AND http_code = 200 order by HARVEST.STARTED desc";

    /** */
    private static final String INSERT_STARTED_HARVEST_SQL =
            "insert into HARVEST (HARVEST_SOURCE_ID, TYPE, USERNAME, STATUS, STARTED) values (?, ?, ?, ?, NOW())";

    /** */
    private static final String UPDATE_FINISHED_HARVEST_SQL =
            "update HARVEST set STATUS=?, FINISHED=NOW(), TOT_STATEMENTS=?, HTTP_CODE=? where HARVEST_ID=?";

    /** */
    private static final String DELETE_HARVESTS_OLDER_THAN_LAST_10_SQL = "DELETE FROM harvest WHERE started < (SELECT TOP 1 started FROM ("
            + "SELECT TOP 10 harvest_id, started FROM harvest WHERE harvest_source_id=? ORDER BY started desc) as A ORDER BY STARTED asc)";

    /**
     * {@inheritDoc}
     */
    @Override
    public int insertStartedHarvest(int harvestSourceId, String harvestType, String user) throws DAOException {

        List<Object> values = new ArrayList<Object>();
        values.add(new Integer(harvestSourceId));
        values.add(harvestType);
        values.add(user);
        values.add(HarvestConstants.STATUS_STARTED);

        Connection conn = null;
        try {
            conn = getSQLConnection();
            return SQLUtil.executeUpdateReturnAutoID(INSERT_STARTED_HARVEST_SQL, values, conn);
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFinishedHarvest(int harvestId, int noOfTriples, int httpCode) throws DAOException {

        List<Object> values = new ArrayList<Object>();
        values.add(HarvestConstants.STATUS_FINISHED);
        values.add(new Integer(noOfTriples));
        values.add(new Integer(httpCode));
        values.add(new Integer(harvestId));

        Connection conn = null;
        try {
            conn = getSQLConnection();
            SQLUtil.executeUpdate(UPDATE_FINISHED_HARVEST_SQL, values, conn);
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HarvestDTO getHarvestById(Integer harvestId) throws DAOException {
        List<Object> values = new ArrayList<Object>();
        values.add(harvestId);
        List<HarvestDTO> list = executeSQL(GET_HARVEST_BY_ID_SQL, values, new HarvestDTOReader());
        return list != null && list.size() > 0 ? list.get(0) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<HarvestDTO> getHarvestsBySourceId(Integer harvestSourceId) throws DAOException {

        int maxDistinctHarvests = 10;

        String getHarvestsBySourceIdSQL =
                "select distinct top " + HarvestMessageType.values().length * maxDistinctHarvests + " H.HARVEST_ID as HARVEST_ID,"
                        + " H.HARVEST_SOURCE_ID as SOURCE_ID, H.TYPE as HARVEST_TYPE, H.USERNAME as HARVEST_USER,"
                        + " H.STATUS as STATUS, H.STARTED as STARTED, H.FINISHED as FINISHED,"
                        + " H.ENC_SCHEMES as ENC_SCHEMES, H.TOT_STATEMENTS as TOT_STATEMENTS," + " H.HTTP_CODE as HTTP_CODE,"
                        + " H.LIT_STATEMENTS as LIT_STATEMENTS, M.TYPE as MESSAGE_TYPE"
                        + " from HARVEST AS H left join HARVEST_MESSAGE AS M on H.HARVEST_ID=M.HARVEST_ID"
                        + " where H.HARVEST_SOURCE_ID=? order by H.STARTED desc";

        List<Object> values = new ArrayList<Object>();
        values.add(harvestSourceId);
        return executeSQL(getHarvestsBySourceIdSQL, values, new HarvestWithMessageTypesReader(maxDistinctHarvests));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<HarvestStatDTO> getLastHarvestStats(Integer limit) throws DAOException {

        String getHarvestStatsSQL =
                "SELECT TOP " + limit + " h.harvest_id, h.started, h.finished, h.tot_statements, hs.url"
                        + " FROM HARVEST AS h LEFT JOIN HARVEST_SOURCE AS hs ON h.harvest_source_id = hs.harvest_source_id"
                        + " WHERE h.status = ? ORDER BY h.finished DESC";

        List<Object> values = new ArrayList<Object>();
        values.add(HarvestConstants.STATUS_FINISHED);
        return executeSQL(getHarvestStatsSQL, values, new HarvestStatReader());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HarvestDTO getLastHarvestBySourceId(Integer harvestSourceId) throws DAOException {

        List<Object> values = new ArrayList<Object>();
        values.add(harvestSourceId);
        List<HarvestDTO> list = executeSQL(GET_LAST_HARVEST_BY_SOURCE_ID_SQL, values, new HarvestDTOReader());
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteOldHarvests(int harvestSourceId) throws DAOException {
        executeSQL(DELETE_HARVESTS_OLDER_THAN_LAST_10_SQL, Arrays.asList(harvestSourceId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HarvestDTO getLastRealHarvestBySourceId(Integer harvestSourceId) throws DAOException {
        List<Object> values = new ArrayList<Object>();
        values.add(harvestSourceId);
        List<HarvestDTO> list = executeSQL(GET_LAST_REAL_HARVEST_BY_SOURCE_ID_SQL, values, new HarvestDTOReader());
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

}
