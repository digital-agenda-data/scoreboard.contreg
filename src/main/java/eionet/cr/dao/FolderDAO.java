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
 * The Original Code is Content Registry 3
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency. Portions created by Zero Technologies are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *        Jaanus Heinlaid
 */

package eionet.cr.dao;

/**
 *
 * @author Jaanus Heinlaid
 */
public interface FolderDAO extends DAO{

    /**
     * Creates home folder for the given user name. The latter must not be null or blank!
     * Creates also all reserved folders under the newly created home folder.
     *
     * @param userName Given user name
     * @throws DAOException Thrown when a database-access error occurs.
     */
    void createUserHomeFolder(String userName) throws DAOException;

    /**
     * Creates a new folder in the given parent folder. Both given parameters must not be null
     * or blank.
     *
     * @param parentFolderUri URI of the new foler's parent folder.
     * @param folderName The new folder's name.
     * @throws DAOException Thrown when a database-access error occurs.
     */
    void createFolder(String parentFolderUri, String folderName) throws DAOException;
}