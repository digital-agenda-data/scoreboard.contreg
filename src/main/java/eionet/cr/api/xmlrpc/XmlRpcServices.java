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
package eionet.cr.api.xmlrpc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eionet.cr.common.CRException;
import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.SearchDAO;
import eionet.cr.dao.mysql.MySQLDAOFactory;
import eionet.cr.dto.ObjectDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.harvest.scheduled.UrgentHarvestQueue;
import eionet.cr.util.PagingRequest;
import eionet.cr.util.Pair;
import eionet.cr.util.URLUtil;
import eionet.cr.util.Util;
import eionet.qawcommons.DataflowResultDto;

/**
 * 
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
public class XmlRpcServices implements Services{

	/** */
	private static final int MAX_RESULTS = 1000;
	
	/** */
	private static Log logger = LogFactory.getLog(XmlRpcServices.class);

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.api.xmlrpc.Services#getResourcesSinceTimestamp(java.util.Date)
	 */
	public List getResourcesSinceTimestamp(Date timestamp) throws CRException {
		
		List<Map<String,String[]>> result = new ArrayList<Map<String,String[]>>();		
		if (timestamp!=null){
			
			// given timestamp must be less than current time (in seconds)
			long curTimeSeconds = Util.currentTimeSeconds();
			long givenTimeSeconds = Util.getSeconds(timestamp.getTime());
			if (givenTimeSeconds < curTimeSeconds){
				
				try{
					Collection<SubjectDTO> subjects =
						DAOFactory.get().getDao(HelperDAO.class).getSubjectsNewerThan(
							timestamp, MAX_RESULTS);
					
					for (Iterator<SubjectDTO> subjectsIter=subjects.iterator(); subjectsIter.hasNext();){
						
						SubjectDTO subjectDTO = subjectsIter.next();
						HashMap<String,String[]> map = new HashMap<String,String[]>();
						for (Iterator<String> predicatesIter=subjectDTO.getPredicates().keySet().iterator(); predicatesIter.hasNext();){
							
							String predicate = predicatesIter.next();
							map.put(predicate, toStringArray(subjectDTO.getObjects(predicate)));
						}
						
						// if map not empty and the subject has a URL (i.e. getUrl() is not blank)
						// then add the map to result
						if (!map.isEmpty()){
							String url = subjectDTO.getUrl();
							if (!StringUtils.isBlank(url)){
								String[] arr = new String[1];
								arr[0] = url;
								map.put(Predicates.CR_URL, arr); // QAW needs this special reserved predicate
								result.add(map);
							}
						}
					}
				}
				catch (Throwable t){
					t.printStackTrace();
					if (t instanceof CRException)
						throw (CRException)t;
					else
						throw new CRException(t.toString(), t);
				}

			}
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.api.xmlrpc.Services#dataflowSearch(java.util.Map)
	 */
	public List dataflowSearch(Map<String,String> criteria) throws CRException{
		
		if (criteria==null)
			criteria = new HashMap<String,String>();
		
		if (!criteria.containsKey(Predicates.RDF_TYPE))
			criteria.put(Predicates.RDF_TYPE, Subjects.ROD_DELIVERY_CLASS);
		
		List<DataflowResultDto> result = new ArrayList<DataflowResultDto>();
		try{
			Pair<Integer, List<SubjectDTO>> search = MySQLDAOFactory
					.get().getDao(SearchDAO.class)
					.searchByFilters(
							criteria,
							null,
							new PagingRequest(1, MAX_RESULTS),
							null);
			
			String[] strArray = {};
			Collection<SubjectDTO> subjects = search.getRight();
			if (subjects!=null){
				for (Iterator<SubjectDTO> iter=subjects.iterator(); iter.hasNext();){
					
					SubjectDTO subjectDTO = iter.next();
					DataflowResultDto resultDTO = new DataflowResultDto();
					
					resultDTO.setResource(subjectDTO.getUri());
					resultDTO.setTitle(subjectDTO.getObjectValue(Predicates.RDFS_LABEL));
					
					resultDTO.setDate(subjectDTO.getObjectValue(Predicates.DC_DATE));
					resultDTO.setDataflow(getLiteralValues(subjectDTO, Predicates.ROD_OBLIGATION_PROPERTY).toArray(strArray));
					resultDTO.setLocality(getLiteralValues(subjectDTO, Predicates.ROD_LOCALITY_PROPERTY).toArray(strArray));
					resultDTO.setType(getLiteralValues(subjectDTO, Predicates.RDF_TYPE).toArray(strArray));
					
					result.add(resultDTO);
				}
			}
		}
		catch (Throwable t){
			t.printStackTrace();
			if (t instanceof CRException)
				throw (CRException)t;
			else
				throw new CRException(t.toString(), t);
		}
		
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.api.xmlrpc.Services#pushContent(java.lang.String)
	 */
	public String pushContent(String content, String sourceUrl) throws CRException {
		
		if (content!=null && content.trim().length()>0){
			if (StringUtils.isBlank(sourceUrl))
				throw new CRException("Missing source URL!");
			else if (!URLUtil.isURL(sourceUrl))
				throw new CRException("Invalid source URL!");
			else if (sourceUrl.indexOf("#")>=0)
				throw new CRException("Source URL must not contain a fragment part!");
			else
				UrgentHarvestQueue.addPushHarvest(content, sourceUrl);
		}
		
		return OK_RETURN_STRING;
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.api.xmlrpc.Services#getEntries(java.util.Hashtable)
	 * 
	 * This method implements what getEntries did in the old Content Registry.
	 * It is called by ROD, though it can be used by any other application as well.
	 * 
	 * The purpose is to return all metadata of all resources that match the given
	 * criteria. The criteria is given as a <code>java.util.Hashtable</code>, where keys represent metadata
	 * attribute names and values represent their values. Data type of both keys and
	 * values is <code>java.lang.String</code>.
	 * 
	 * The method returns a <code>java.util.Vector</code> of type <code>java.util.Hashtable</code>. Every such hashtable represents one
	 * resource that contains exactly 1 key that is a String that represents the resource's URI. The value is
	 * another <code>java.lang.Hashtable</code> where the data type of keys is <code>java.lang.String</code> and the data type of values
	 * is <code>java.util.Vector</code>. They keys represent URIs of the resource's attributes and the value-vectors represent values
	 * of attributes. These values are of type <code>java.lang.String</code>.
	 * 
	 */
	public Vector getEntries(Hashtable criteria) throws CRException {

		Vector result = new Vector();
		try{
			Pair<Integer,List<SubjectDTO>> search = MySQLDAOFactory.get()
					.getDao(SearchDAO.class)
					.searchByFilters(
							criteria,
							null,
							new PagingRequest(1,MAX_RESULTS),
							null);
			Collection<SubjectDTO> subjects = search.getRight();
			if (subjects!=null){
				for (Iterator<SubjectDTO> iter=subjects.iterator(); iter.hasNext();){
					
					SubjectDTO subjectDTO = iter.next();
					Hashtable<String,Vector<String>> predicatesTable = new Hashtable<String,Vector<String>>();
					for (Iterator<String> predicatesIter=subjectDTO.getPredicates().keySet().iterator(); predicatesIter.hasNext();){
						
						String predicate = predicatesIter.next();
						predicatesTable.put(predicate, new Vector<String>(getLiteralValues(subjectDTO, predicate)));
					}

					if (!predicatesTable.isEmpty()){
						Hashtable<String,Hashtable> subjectTable = new Hashtable<String,Hashtable>();
						subjectTable.put(subjectDTO.getUri(), predicatesTable);
						result.add(subjectTable);
					}
				}
			}
		}
		catch (Throwable t){
			t.printStackTrace();
			if (t instanceof CRException)
				throw (CRException)t;
			else
				throw new CRException(t.toString(), t);
		}
		
		return result;
	}

	/**
	 * 
	 * @param subjectDTO
	 * @param predicateUri
	 * @return
	 */
	private static Collection<String> getLiteralValues(SubjectDTO subjectDTO, String predicateUri){
		
		HashSet<String> result = new HashSet<String>();
		
		Collection<ObjectDTO> objects = subjectDTO.getObjects(predicateUri, ObjectDTO.Type.LITERAL);
		if (objects!=null && !objects.isEmpty()){
			for (Iterator<ObjectDTO> iter = objects.iterator(); iter.hasNext();){
				result.add(iter.next().getValue());
			}
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param objects
	 * @return
	 */
	private static String[] toStringArray(Collection<ObjectDTO> objects){
		
		if (objects==null)
			return new String[0];
		
		int i = 0;
		String[] result = new String[objects.size()];
		for (Iterator<ObjectDTO> iter=objects.iterator(); iter.hasNext();i++){
			result[i] = iter.next().getValue();
		}
		
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.api.xmlrpc.Services#getXmlFilesBySchema(java.lang.String)
	 */
	public Vector getXmlFilesBySchema(String schemaIdentifier) throws CRException {

		Vector result = new Vector();
		try{
			List<SubjectDTO> subjects = null;
			if (!StringUtils.isBlank(schemaIdentifier)){
				
				SearchDAO searchDao = MySQLDAOFactory.get().getDao(SearchDAO.class);
				Map<String, String> criteria = new HashMap<String, String>();
				criteria.put(Predicates.CR_SCHEMA, schemaIdentifier);

				Pair<Integer, List<SubjectDTO>> results = searchDao.searchByFilters(
						criteria,
						null, 
						new PagingRequest(0, 0),
						null);

				int subjectCount = results==null ?
						0 : (results.getRight()==null ? 0 : results.getRight().size());  
				logger.debug(getClass().getSimpleName() + ".getXmlFilesBySchema("
						+ schemaIdentifier + "), " + subjectCount + " subjects found in total");
				subjects = results.getRight();
				
				if (subjects!=null){
					for (SubjectDTO subjectDTO : subjects){
						
						String lastModif = subjectDTO.getObjectValue(Predicates.CR_LAST_MODIFIED);
						Hashtable hashtable = new Hashtable();
						hashtable.put("uri", subjectDTO.getUri());
						hashtable.put("lastModified", lastModif==null ? "" : lastModif.trim());
						result.add(hashtable);
					}
				}
			}
		}
		catch (Throwable t){
			t.printStackTrace();
			if (t instanceof CRException)
				throw (CRException)t;
			else
				throw new CRException(t.toString(), t);
		}
		
		return result;
	}
}
