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
package eionet.cr.web.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import eionet.cr.config.GeneralConfig;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.harvest.HarvestException;
import eionet.cr.harvest.scheduled.UrgentHarvestQueue;
import eionet.cr.harvest.util.DedicatedHarvestSourceTypes;

/**
 * @author altnyris
 *
 */
@UrlBinding("/sources.action")
public class HarvestSourcesActionBean extends AbstractActionBean {
	
	/** */
	private static final String UNAVAILABLE_TYPE = "unavail";
	
	/** */
	private List<HarvestSourceDTO> harvestSources;
	
	/** */
	public static List<Map<String,String>> sourceTypes;
	
	/** */
	private String type;

	/** */
	private List<String> sourceUrl;

	/** */
	public HarvestSourcesActionBean(){
		this.type = "data";
	}
	
	/**
	 * 
	 * @return
	 * @throws DAOException 
	 */
	@DefaultHandler
	public Resolution view() throws DAOException{
		
		if (type!=null && type.length()>0){
			if (type.equals(UNAVAILABLE_TYPE))
				harvestSources = DAOFactory.getDAOFactory().getHarvestSourceDAO().getHarvestSourcesUnavailable();
			else
				harvestSources = DAOFactory.getDAOFactory().getHarvestSourceDAO().getHarvestSourcesByType(type);
		}
		
		return new ForwardResolution("/pages/sources.jsp");
	}

	/**
	 * 
	 * @return
	 * @throws DAOException 
	 */
	public Resolution delete() throws DAOException{
		
		if(isUserLoggedIn()){
			if (sourceUrl!=null && !sourceUrl.isEmpty()){
				DAOFactory.getDAOFactory().getHarvestSourceDAO().deleteSourcesByUrl(sourceUrl);
				showMessage("Harvest source(s) deleted!");
			}
		}
		else
			handleCrException(getBundle().getString("not.logged.in"), GeneralConfig.SEVERITY_WARNING);

		return view();
	}
	
	/**
	 * @throws DAOException 
	 * @throws HarvestException 
	 * 
	 */
	public Resolution harvest() throws DAOException, HarvestException{
		
		if(isUserLoggedIn()){
			if (sourceUrl!=null && !sourceUrl.isEmpty()){
				UrgentHarvestQueue.addPullHarvests(sourceUrl);
				if (sourceUrl.size()==1)
					showMessage("The source has been scheduled for urgent harvest!");
				else
					showMessage("The sources have been scheduled for urgent harvest!");
			}
		}
		else
			handleCrException(getBundle().getString("not.logged.in"), GeneralConfig.SEVERITY_WARNING);
		
		return view();
	}

	/**
	 * @return the harvestSources
	 */
	public List<HarvestSourceDTO> getHarvestSources() {
		return harvestSources;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<Map<String, String>> getSourceTypes(){
		
		if (sourceTypes==null){
			
			sourceTypes = new ArrayList<Map<String,String>>();
			
			Map<String,String> typeMap = new HashMap<String,String>();
			typeMap.put("title", "Data");
			typeMap.put("type", "data");
			sourceTypes.add(typeMap);
			
			typeMap = new HashMap<String,String>();
			typeMap.put("title", "Schemas");
			typeMap.put("type", "schema");
			sourceTypes.add(typeMap);
			
			typeMap = new HashMap<String,String>();
			typeMap.put("title", "Delivered files");
			typeMap.put("type", DedicatedHarvestSourceTypes.deliveredFile);
			sourceTypes.add(typeMap);
			
			typeMap = new HashMap<String,String>();
			typeMap.put("title", "QAW sources");
			typeMap.put("type", DedicatedHarvestSourceTypes.qawSource);
			sourceTypes.add(typeMap);

			typeMap = new HashMap<String,String>();
			typeMap.put("title", "Unavailable");
			typeMap.put("type", UNAVAILABLE_TYPE);
			sourceTypes.add(typeMap);
		}
		
		return sourceTypes;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	
	/**
	 * @param sourceUrl the sourceUrl to set
	 */
	public void setSourceUrl(List<String> sourceUrl) {
		this.sourceUrl = sourceUrl;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getPagingUrl(){
		
		String urlBinding = getUrlBinding();
		if (urlBinding.startsWith("/")){
			urlBinding = urlBinding.substring(1);
		}
		
		StringBuffer buf = new StringBuffer(urlBinding);
		return buf.append("?view=").toString();
	}
}
