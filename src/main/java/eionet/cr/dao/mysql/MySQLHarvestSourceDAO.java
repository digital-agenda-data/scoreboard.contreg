package eionet.cr.dao.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dto.HarvestScheduleDTO;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.dto.readers.HarvestSourceDTOReader;
import eionet.cr.util.sql.ConnectionUtil;
import eionet.cr.util.sql.SQLUtil;

/**
 * @author altnyris
 *
 */
public class MySQLHarvestSourceDAO extends MySQLBaseDAO implements HarvestSourceDAO {
	
	public MySQLHarvestSourceDAO() {
	}
	
	/** */
	private static final String getSourcesSQL = "select * from HARVEST_SOURCE";
		
	/*
     * (non-Javadoc)
     * 
     * @see eionet.cr.dao.HarvestSourceDao#getHarvestSources()
     */
    public List<HarvestSourceDTO> getHarvestSources() throws DAOException {
    	List<Object> values = new ArrayList<Object>();
				
		Connection conn = null;
		HarvestSourceDTOReader rsReader = new HarvestSourceDTOReader();
		try{
			conn = getConnection();
			SQLUtil.executeQuery(getSourcesSQL, values, rsReader, conn);
			List<HarvestSourceDTO>  list = rsReader.getResultList();
			return list;
		}
		catch (Exception e){
			throw new DAOException(e.getMessage(), e);
		}
		finally{
			try{
				if (conn!=null) conn.close();
			}
			catch (SQLException e){}
		}
    }
    
    /** */
	private static final String getSourcesByIdSQL = "select * from HARVEST_SOURCE where HARVEST_SOURCE_ID=?";
	
	/*
     * (non-Javadoc)
     * 
     * @see eionet.cr.dao.HarvestSourceDao#getHarvestSourceById()
     */
    public HarvestSourceDTO getHarvestSourceById(int harvestSourceID) throws DAOException {
    	List<Object> values = new ArrayList<Object>();
    	values.add(new Integer(harvestSourceID));
				
		Connection conn = null;
		HarvestSourceDTO source = null;
		HarvestSourceDTOReader rsReader = new HarvestSourceDTOReader();
		try{
			conn = getConnection();
			SQLUtil.executeQuery(getSourcesByIdSQL, values, rsReader, conn);
			List<HarvestSourceDTO>  list = rsReader.getResultList();
			if(list!=null && list.size()>0){
				source = list.get(0);
				
				HarvestScheduleDTO schedule = DAOFactory.getDAOFactory().getHarvestScheduleDAO().getHarvestScheduleBySourceId(harvestSourceID);
				if(schedule!=null){
					source.setHarvestSchedule(schedule);
				}
				return source;
			}
			return null;
		}
		catch (Exception e){
			throw new DAOException(e.getMessage(), e);
		}
		finally{
			try{
				if (conn!=null) conn.close();
			}
			catch (SQLException e){}
		}
    }
    
    /** */
	private static final String addSourceSQL = "insert into HARVEST_SOURCE (IDENTIFIER,PULL_URL,TYPE,EMAILS,DATE_CREATED,CREATOR) VALUES (?,?,?,?,NOW(),?)";
		
	/*
     * (non-Javadoc)
     * 
     * @see eionet.cr.dao.HarvestSourceDao#addSource()
     */
    public void addSource(HarvestSourceDTO source, String user) throws DAOException {
    	    	
    	List<Object> values = new ArrayList<Object>();
		values.add(source.getIdentifier());
		values.add(source.getPullUrl());
		values.add(source.getType());
		values.add(source.getEmails());
		values.add(user);
		
		Connection conn = null;
		try{
			conn = getConnection();
			SQLUtil.executeUpdate(addSourceSQL, values, conn);
			
			if(source.getHarvestSchedule() != null){
				Integer source_id = getLastInsertID(conn);
				source.getHarvestSchedule().setHarvestSourceId(source_id);
				DAOFactory.getDAOFactory().getHarvestScheduleDAO().addSchedule(source.getHarvestSchedule());
			}
		}
		catch (Exception e){
			throw new DAOException(e.getMessage(), e);
		}
		finally{
			ConnectionUtil.closeConnection(conn);
		}
    }
    
    /** */
	private static final String editSourceSQL = "update HARVEST_SOURCE set IDENTIFIER=?,PULL_URL=?,TYPE=?,EMAILS=? where HARVEST_SOURCE_ID=?";
	
	/*
     * (non-Javadoc)
     * 
     * @see eionet.cr.dao.HarvestSourceDao#editSource()
     */
    public void editSource(HarvestSourceDTO source) throws DAOException {
    	    	
    	List<Object> values = new ArrayList<Object>();
		values.add(source.getIdentifier());
		values.add(source.getPullUrl());
		values.add(source.getType());
		values.add(source.getEmails());
		values.add(source.getSourceId());
		
		Connection conn = null;
		try{
			conn = getConnection();
			SQLUtil.executeUpdate(editSourceSQL, values, conn);
			
			if(source.getHarvestSchedule() != null){
				DAOFactory.getDAOFactory().getHarvestScheduleDAO().editSchedule(source.getHarvestSchedule());
			}
		}
		catch (Exception e){
			throw new DAOException(e.getMessage(), e);
		}
		finally{
			ConnectionUtil.closeConnection(conn);
		}
    }
    
    /** */
	private static final String deleteSourceSQL = "delete from HARVEST_SOURCE where HARVEST_SOURCE_ID=?";
	
	/*
     * (non-Javadoc)
     * 
     * @see eionet.cr.dao.HarvestSourceDao#deleteSource()
     */
    public void deleteSource(HarvestSourceDTO source) throws DAOException {
    	    	
    	List<Object> values = new ArrayList<Object>();
		values.add(source.getSourceId());
		
		Connection conn = null;
		try{
			conn = getConnection();
			SQLUtil.executeUpdate(deleteSourceSQL, values, conn);
			
			DAOFactory.getDAOFactory().getHarvestScheduleDAO().deleteSchedule(source.getSourceId());
		}
		catch (Exception e){
			throw new DAOException(e.getMessage(), e);
		}
		finally{
			ConnectionUtil.closeConnection(conn);
		}
    }
}
