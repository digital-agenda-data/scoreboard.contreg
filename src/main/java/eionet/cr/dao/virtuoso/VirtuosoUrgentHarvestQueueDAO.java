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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;

import eionet.cr.dao.DAOException;
import eionet.cr.dao.UrgentHarvestQueueDAO;
import eionet.cr.dao.readers.HarvestQueueItemDTOReader;
import eionet.cr.dto.UrgentHarvestQueueItemDTO;
import eionet.cr.util.sql.SQLUtil;

/**
 *
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
public class VirtuosoUrgentHarvestQueueDAO extends VirtuosoBaseDAO implements UrgentHarvestQueueDAO {

    /** */
    private static final Logger LOGGER = Logger.getLogger(VirtuosoUrgentHarvestQueueDAO.class);

    /** */
    private static final String ADD_PUSH_HARVEST_SQL = "insert into URGENT_HARVEST_QUEUE (URL,\"TIMESTAMP\",PUSHED_CONTENT) VALUES (?,NOW(),?)";

    /** */
    private static final String GET_URGENT_HARVEST_QUEUE_SQL = "select * from URGENT_HARVEST_QUEUE order by \"TIMESTAMP\" asc";

    /** */
    private static final String PEEK_SQL = "select top 1 * from URGENT_HARVEST_QUEUE order by \"TIMESTAMP\" asc";

    /** */
    private static final String DELETE_QUEUE_ITEM_SQL = "delete from URGENT_HARVEST_QUEUE "
            + "where URL=? and substring(cast(\"TIMESTAMP\" as varchar), 1, 19)=?";

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestQueueDAO#addQueueItem(eionet.cr.dto.HarvestQueueItemDTO)
     */
    @Override
    public void addPullHarvests(List<UrgentHarvestQueueItemDTO> queueItems) throws DAOException {

        String sql = "insert into URGENT_HARVEST_QUEUE (URL,\"TIMESTAMP\") VALUES (?,NOW())";
        PreparedStatement ps = null;
        Connection conn = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < queueItems.size(); i++) {
                UrgentHarvestQueueItemDTO dto = queueItems.get(i);
                String url = dto.getUrl();
                if (url != null) {
                    url = StringUtils.substringBefore(url, "#");
                }
                ps.setString(1, url);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(ps);
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestQueueDAO#addPushHarvest(eionet.cr.dto.HarvestQueueItemDTO)
     */
    @Override
    public void addPushHarvest(UrgentHarvestQueueItemDTO queueItem) throws DAOException {

        List<Object> values = new ArrayList<Object>();

        String url = queueItem.getUrl();
        if (url != null) {
            url = StringUtils.substringBefore(url, "#");
        }
        values.add(url);
        values.add(queueItem.getPushedContent());

        Connection conn = null;
        try {
            conn = getSQLConnection();
            SQLUtil.executeUpdate(ADD_PUSH_HARVEST_SQL, values, conn);
        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.HarvestQueueDAO#getUrgentHarvestQueue()
     */
    @Override
    public List<UrgentHarvestQueueItemDTO> getUrgentHarvestQueue() throws DAOException {
        return executeSQL(GET_URGENT_HARVEST_QUEUE_SQL, new ArrayList<Object>(), new HarvestQueueItemDTOReader());
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.UrgentHarvestQueueDAO#poll()
     */
    @Override
    public UrgentHarvestQueueItemDTO poll() throws DAOException {

        Connection conn = null;
        try {
            conn = getSQLConnection();
            UrgentHarvestQueueItemDTO queueItem = peek(conn);
            if (queueItem != null) {
                int updateCount = deleteQueueItem(queueItem, conn);
                if (updateCount <= 0) {
                    LOGGER.error("Failed to delete urgent harvest queue item (attempting deletion of all queue items with this URL): "
                            + queueItem.toString());
                    updateCount = deleteAllQueueItems(queueItem.getUrl(), conn);
                    if (updateCount <= 0) {
                        throw new DAOException("Failed to delete any queue items with this URL: " + queueItem.getUrl());
                    }
                }
            }
            return queueItem;

        } catch (Exception e) {
            throw new DAOException(e.getMessage(), e);
        } finally {
            SQLUtil.close(conn);
        }
    }

    /**
     *
     * @param conn
     * @return
     * @throws SQLException
     */
    private static UrgentHarvestQueueItemDTO peek(Connection conn) throws Exception {

        List<Object> values = new ArrayList<Object>();

        HarvestQueueItemDTOReader rsReader = new HarvestQueueItemDTOReader();
        SQLUtil.executeQuery(PEEK_SQL, values, rsReader, conn);
        List<UrgentHarvestQueueItemDTO> list = rsReader.getResultList();

        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }

    /**
     *
     * @param item
     * @throws SQLException
     */
    private static int deleteQueueItem(UrgentHarvestQueueItemDTO item, Connection conn) throws SQLException {

        if (item == null || StringUtils.isBlank(item.getUrl()) || item.getTimeAdded() == null) {
            return 0;
        }

        List<Object> values = new ArrayList<Object>();
        values.add(item.getUrl());
        values.add(DateFormatUtils.format(item.getTimeAdded(), "yyyy-MM-dd HH:mm:ss"));

        return SQLUtil.executeUpdate(DELETE_QUEUE_ITEM_SQL, values, conn);
    }

    /**
     *
     * @param url
     * @param conn
     * @return
     * @throws SQLException
     */
    private static int deleteAllQueueItems(String url, Connection conn) throws SQLException {

        List<Object> values = new ArrayList<Object>();
        values.add(url);
        return SQLUtil.executeUpdate("delete from URGENT_HARVEST_QUEUE where URL=?", values, conn);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.dao.UrgentHarvestQueueDAO#isInQueue(java.lang.String)
     */
    @Override
    public boolean isInQueue(String url) {

        boolean ret = false;
        String sql = "select top 1 * from URGENT_HARVEST_QUEUE where URL = ?";
        PreparedStatement ps = null;
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, url);
            rs = ps.executeQuery();
            while (rs.next()) {
                ret = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            SQLUtil.close(ps);
            SQLUtil.close(conn);
        }
        return ret;
    }
}
