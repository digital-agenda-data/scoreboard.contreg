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
package eionet.cr.harvest;

import java.util.Date;

public class InstantHarvest extends PullHarvest{

	/** */
	private String userName;

	/**
	 * 
	 * @param sourceUrlString
	 * @param lastHarvest
	 */
	public InstantHarvest(String sourceUrlString, Date lastHarvest, String userName){
		super(sourceUrlString, lastHarvest);
		this.userName = userName;
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.harvest.Harvest#createRDFHandler()
	 */
	protected RDFHandler createRDFHandler(){
		RDFHandler handler = super.createRDFHandler();
		handler.setInstantHarvestUser(userName);
		return handler;
	}

	/*
	 * (non-Javadoc)
	 * @see eionet.cr.harvest.Harvest#doHarvestStartedActions()
	 */
	protected void doHarvestStartedActions() throws HarvestException{
		
		logger.debug("Instant harvest started");
		super.doHarvestStartedActions();
	}
}
