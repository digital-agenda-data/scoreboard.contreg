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

import eionet.cr.dto.UrgentHarvestQueueItemDTO;

/**
 * 
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
public interface UrgentHarvestQueueDAO {

	/**
	 * 
	 * @throws DAOException
	 */
	public abstract void addPullHarvests(List<UrgentHarvestQueueItemDTO> queueItems) throws DAOException;
	
	/**
	 * 
	 * @param queueItem
	 * @throws DAOException
	 */
	public abstract void addPushHarvest(UrgentHarvestQueueItemDTO queueItem) throws DAOException;
	
	/**
	 * 
	 * @return
	 */
	public abstract List<UrgentHarvestQueueItemDTO> getUrgentHarvestQueue() throws DAOException;
	
	/**
	 * 
	 * @return
	 * @throws DAOException
	 */
	public abstract UrgentHarvestQueueItemDTO poll() throws DAOException;
}
