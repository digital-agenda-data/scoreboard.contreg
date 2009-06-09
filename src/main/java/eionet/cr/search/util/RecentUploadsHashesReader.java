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
package eionet.cr.search.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

public class RecentUploadsHashesReader extends SubjectHashesReader {

	/** */
	private Map<String,Date> firstSeenTimes;

	/**
	 * 
	 * @param firstSeenTimes
	 */
	public RecentUploadsHashesReader(Map<String,Date> firstSeenTimes){
		
		super();
		
		if (firstSeenTimes==null)
			throw new IllegalArgumentException();
		this.firstSeenTimes = firstSeenTimes;
	}
	
	/*
	 * (non-Javadoc)
	 * @see eionet.cr.search.util.SubjectHashesReader#readRow(java.sql.ResultSet)
	 */
	public void readRow(ResultSet rs) throws SQLException{
		super.readRow(rs);
		firstSeenTimes.put(rs.getString("SUBJECT_HASH"), new Date(rs.getLong("FIRSTSEEN_TIME")));
	}
}
