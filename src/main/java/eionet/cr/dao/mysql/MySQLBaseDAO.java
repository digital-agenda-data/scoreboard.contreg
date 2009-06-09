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
package eionet.cr.dao.mysql;

import java.sql.Connection;
import java.sql.SQLException;

import eionet.cr.dao.DAOException;
import eionet.cr.util.sql.ConnectionUtil;
import eionet.cr.util.sql.MySQLUtil;

/**
 * 
 * @author heinljab, altnyris
 *
 */
public abstract class MySQLBaseDAO {
	
	/**
	 * 
	 * @return
	 */
	protected Connection getConnection() throws SQLException{
		return ConnectionUtil.getConnection();
	}
	
	/**
	 * 
	 * @param conn
	 */
	protected void closeConnection(Connection conn){
		ConnectionUtil.closeConnection(conn);
	}
	
	/**
	 * 
	 * @return
	 * @throws SQLException 
	 * @throws DAOException
	 */
	protected Integer getLastInsertID(Connection conn) throws SQLException{
		return MySQLUtil.getLastInsertID(conn);
	}
}
