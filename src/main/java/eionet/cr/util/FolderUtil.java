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
package eionet.cr.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import eionet.cr.config.GeneralConfig;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.FolderDAO;
import eionet.cr.web.security.CRUser;

/**
 *
 * @author altnyris
 *
 */
public final class FolderUtil {

    /**
     * Hide utility class constructor.
     */
    private FolderUtil() {
        // Just an empty private constructor to avoid instantiating this utility class.
    }

    /**
     * Extract ACL path for special folders: projects and home. Until DDC not done, main project/home folder ACL is used
     *
     * @param uri uri of the folder
     * @param specialFolderName - special folder prefix in the name
     * @return String acl path of th given folder
     */
    public static String extractSpecialAclPath(String uri, String specialFolderName) {
        String appHome = GeneralConfig.getProperty(GeneralConfig.APPLICATION_HOME_URL);
        String aclPath = StringUtils.substringAfter(uri, appHome);

        if (aclPath.startsWith("/" + specialFolderName)) {
            String path = extractPathInSpecialFolder(uri, specialFolderName);
            if (!StringUtils.isBlank(path)) {
                String[] tokens = path.split("/");
                if (tokens != null && tokens.length > 0) {
                    aclPath = "/" + specialFolderName + "/" + tokens[0];
                }
            }
        }

        return aclPath;
    }

    /**
     *
     * @return String
     */
    public static String getProjectsFolder() {

        String appHome = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        return appHome + "/project";
    }

    /**
     * Detects if the given URI starts with a CR user home.
     *
     * @param uri The given URI.
     * @return See description above..
     */
    public static boolean startsWithUserHome(String uri) {

        if (StringUtils.isBlank(uri)) {
            return false;
        }

        String appHomeUrl = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        if (!uri.startsWith(appHomeUrl) || uri.equals(appHomeUrl)) {
            return false;
        }

        String afterAppHomeUrl = StringUtils.substringAfter(uri, appHomeUrl);
        String homeString = "/home/";
        return afterAppHomeUrl.startsWith(homeString) && afterAppHomeUrl.length() > homeString.length();
    }

    /**
     * Returns true if {@link URIUtil#startsWithUserHome(String)} true and it is a reserved URI. For resreved URIs, see
     * {@link CRUser#getReservedFolderAndFileUris(String)}. Otherwise returns false.
     *
     * @param uri The given URI.
     * @return See method description.
     */
    public static boolean isUserReservedUri(String uri) {

        String userName = extractUserName(uri);
        return !StringUtils.isBlank(userName) && CRUser.getReservedFolderAndFileUris(userName).contains(uri);
    }

    /**
     * Extracts user name from the given URI. A user name is returned only if the given URI returns true for
     * {@link URIUtil#startsWithUserHome(String)}. Otherwise null is returned.
     *
     * @param uri The given URI.
     * @return See method description.
     */
    public static String extractUserName(String uri) {

        if (!startsWithUserHome(uri)) {
            return null;
        }

        String str =
                StringUtils.substringAfter(uri, GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL) + "/home/");
        return StringUtils.substringBefore(str, "/");
    }

    /**
     * Returns the path after the user home folder. For example if uri is
     * "http://127.0.0.1:8080/cr/home/heinlja/newFolder/newFile.txt" the result is "newFolder/newFile.txt".
     *
     * @param uri
     * @return
     */
    public static String extractPathInUserHome(String uri) {
        if (!startsWithUserHome(uri)) {
            if (isProjectFolder(uri)) {
                return StringUtils.substringAfter(uri, GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL)
                        + "/project/");
            }
            return null;
        }

        String userName = extractUserName(uri);

        String result =
                StringUtils.substringAfter(uri, GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL) + "/home/"
                        + userName + "/");
        return result;
    }

    /**
     * Returns the path after the special folder. For example if uri is "http://127.0.0.1:8080/cr/project/newFolder/newFile.txt" the
     * result is "newFolder/newFile.txt".
     *
     * @param uri
     * @param mainFolder special main folder name
     * @return String
     */
    public static String extractPathInSpecialFolder(String uri, String mainFolder) {

        String result = null;
        String appHome = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        if (uri.startsWith(appHome + "/" + mainFolder + "/")) {
            result = StringUtils.substringAfter(uri, appHome + "/" + mainFolder + "/");
        }
        return result;
    }

    /**
     * Extracts path in the uri.
     *
     * @param uri Full uri
     * @return Path after the special folder path (project, user etc)
     */
    public static String extractPathInFolder(String uri) {

        if (uri == null) {
            return null;
        }

        String appHome = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        if (uri.startsWith(appHome + "/project/")) {
            return extractPathInSpecialFolder(uri, "project");
        } else if (startsWithUserHome(uri)) {
            return extractPathInUserHome(uri);
        } else {
            return null;
        }
    }

    /**
     * Return all folders where user can store data.
     *
     * @param user current user
     * @return List of folder names
     * @throws DAOException if query does not succees
     */
    public static List<String> getUserAccessibleFolders(CRUser user) throws DAOException {

        List<String> folders = null;
        if (user != null) {
            folders = DAOFactory.get().getDao(FolderDAO.class).getSubFolders(user.getHomeUri());

            // Get project folders where user can insert content
            if (CRUser.hasPermission(user.getUserName(), "/project", "i")) {
                List<String> projectFolders =
                        DAOFactory.get().getDao(FolderDAO.class).getSubFolders(FolderUtil.getProjectsFolder());
                if (projectFolders != null && projectFolders.size() > 0) {
                    for (String furi : projectFolders) {
                        String aclPath = FolderUtil.extractPathInSpecialFolder(furi, "project");
                        if (!StringUtils.isBlank(aclPath) && CRUser.hasPermission(user.getUserName(), aclPath, "i")) {
                            folders.add(furi);
                        }
                    }
                }
            }
        }
        return folders;
    }

    /**
     * Returns list of project folders where the user can insert items.
     *
     * @param user current session user
     * @param permission - permission to check
     * @return list of folder names
     * @throws DAOException if query fails
     */
    public static List<String> getUserAccessibleProjectFolderNames(CRUser user, String permission) throws DAOException {
        List<String> folders = null;
        if (user != null) {
            // Get project folders where user can insert content
            if (CRUser.hasPermission(user.getUserName(), "/project", permission)) {
                List<String> projectFolders =
                        DAOFactory.get().getDao(FolderDAO.class).getSubFolders(FolderUtil.getProjectsFolder());
                if (projectFolders != null && projectFolders.size() > 0) {
                    folders = new ArrayList<String>();
                    for (String furi : projectFolders) {
                        String projectName = FolderUtil.extractPathInSpecialFolder(furi, "project");
                        if (!StringUtils.isBlank(projectName)
                                && CRUser.hasPermission(user.getUserName(), "/project/" + projectName, permission)) {
                            folders.add(projectName);
                        }
                    }
                }
            }
        }
        return folders;
    }

    /**
     * Returns the path after the app home URL. For example if uri is "http://127.0.0.1:8080/cr/abc" the result is "abc".
     *
     * @param uri
     * @return String
     */
    public static String extractAclPath(String uri) {

        String appHome = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        return StringUtils.substringAfter(uri, appHome);
    }

    /**
     * True, if the folder uri is user's home folder.
     *
     * @return
     */
    public static boolean isHomeFolder(String uri) {
        String appHome = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        if (!isProjectFolder(uri) && uri.startsWith(appHome + "/home")
                && StringUtils.isEmpty(FolderUtil.extractPathInUserHome(uri))) {
            return true;
        }
        return false;
    }

    /**
     * True, if the folder uri is a project folder.
     *
     * @return boolean
     */
    public static boolean isProjectFolder(String uri) {
        String appHome = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        if (uri.startsWith(appHome + "/project")) {
            return true;
        }
        return false;
    }

    /**
     * True, if the folder uri is project root folder.
     *
     * @return boolean
     */
    public static boolean isProjectRootFolder(String uri) {
        String appHome = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        if (uri.equals(appHome + "/project")) {
            return true;
        }
        return false;
    }

    /**
     * Returns full url of the project.
     *
     * @param projectName project name
     * @return full url
     */
    public static String getProjectFolder(String projectName) {
        String appHome = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);
        return appHome + "/project/" + projectName;
    }

    /**
     * Returns FileStore userDir where the files will be stored. Current userdir is returned as other users files can be managed as
     * well
     *
     * @param uri
     * @param userName
     * @return String
     */
    public static String getUserDir(String uri, String userName) {
        String folder = userName;
        if (FolderUtil.isProjectFolder(uri)) {
            folder = "project";
        } else if (startsWithUserHome(uri)) {
            folder = extractUserName(uri);
        }
        return folder;
    }

    /**
     * Calculates folder context (graph) of the triples of file/folder object.
     *
     * @param uri URI of the file
     * @return folder context
     */
    public static String folderContext(String uri) {
        String context = uri;
        String appHome = GeneralConfig.getRequiredProperty(GeneralConfig.APPLICATION_HOME_URL);

        if (FolderUtil.startsWithUserHome(uri)) {
            context = appHome + FolderUtil.extractSpecialAclPath(uri, "home");
        } else if (uri.startsWith(appHome + "/project")) {
            context = appHome + "/project";
        }

        return context;
    }

    /**
     * returns parent folder URI.
     * @param uri this URI
     * @return parent uri
     */
    public static String extractParentFolderUri(String uri) {
        return StringUtils.substringBeforeLast(uri, "/");
    }
}
