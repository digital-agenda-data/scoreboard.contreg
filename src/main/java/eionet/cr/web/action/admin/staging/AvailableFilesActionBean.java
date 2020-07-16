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
 * Agency. Portions created by TripleDev or Zero Technologies are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 *        jaanus
 */

package eionet.cr.web.action.admin.staging;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.FilterConfig;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.validation.ValidationMethod;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import eionet.cr.staging.AvailableFile;
import eionet.cr.staging.FileDownloader;
import eionet.cr.staging.imp.msaccess.MSAccessImporter;
import eionet.cr.util.CompressUtil;
import eionet.cr.util.FileDeletionJob;
import eionet.cr.util.URLUtil;
import eionet.cr.web.action.AbstractActionBean;
import eionet.cr.web.action.admin.AdminWelcomeActionBean;

/**
 * Action bean that lists files available for the creation of staging databases, and provides other actions on these files as well.
 *
 * @author jaanus
 */
@UrlBinding("/admin/availFiles.action")
public class AvailableFilesActionBean extends AbstractActionBean {

    /** */
    private static final String LIST_JSP = "/pages/admin/staging/availableFiles.jsp";

    /** */
    private ArrayList<AvailableFile> availableFiles = new ArrayList<AvailableFile>();

    /** */
    private FileBean uploadFile;

    /** */
    private String downloadUrl;

    /** */
    private String newFileName;

    /** */
    private int maxFilePostSize;

    /** */
    private List<String> fileNames;

    /** */
    private String selectedFileName;

    /** */
    private String uncompressedFileName;

    /** */
    private List<ArchiveEntry> archiveEntries;

    /** */
    private List<String> extractEntries;

    /**
     * List all available files.
     *
     * @return the resolution
     */
    @DefaultHandler
    public Resolution list() {

        File[] files = FileDownloader.FILES_DIR.listFiles();
        Arrays.sort(files);
        for (int i = 0; i < files.length; i++) {
            availableFiles.add(AvailableFile.create(files[i]));
        }

        return new ForwardResolution(LIST_JSP);
    }

    /**
     * Handles the "upload" event.
     *
     * @return the resolution
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public Resolution upload() throws IOException {

        String renameTo = StringUtils.isBlank(newFileName) ? uploadFile.getFileName() : newFileName;
        uploadFile.save(new File(FileDownloader.FILES_DIR, FileDownloader.ensureUniqueFileName(renameTo)));

        addSystemMessage("File successfully uploaded!");
        return new RedirectResolution(this.getClass());
    }

    /**
     * Handles the the "download" event.
     *
     * @return the resolution
     */
    public Resolution download() {

        FileDownloader downloader = new FileDownloader(downloadUrl, newFileName);
        downloader.start();

        int i = 1;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                addWarningMessage("Technical error when attempting to detect the download status!"
                        + "Please refresh the page within the next minute to see if the download started.");
                return new RedirectResolution(this.getClass());
            }
        } while (!downloader.isDownloadStarted() && i++ <= 15);

        if (downloader.isDownloadStarted()) {
            addSystemMessage("File download started! Refresh the page to see progress.");
        } else {
            addCautionMessage("Still establishing the connection!"
                    + "Please refresh the page within the next minute to see if the download started.");
        }
        return new RedirectResolution(this.getClass());
    }

    /**
     * Handles the "delete" event.
     *
     * @return the resolution
     */
    public Resolution delete() {

        if (fileNames != null && !fileNames.isEmpty()) {

            int noOfFilesBefore = FileDownloader.FILES_DIR.listFiles().length;
            for (String fileName : fileNames) {
                File file = new File(FileDownloader.FILES_DIR, fileName);
                boolean success = file.delete();
                if (!success) {
                    FileDeletionJob.register(file);
                }
            }

            if (FileDownloader.FILES_DIR.listFiles().length != noOfFilesBefore - fileNames.size()) {
                addWarningMessage("Failed to delete some of the files! This might be a temporary resource locking problem."
                        + "Please refresh in a dozen of seconds, and if the file is still present, try its deletion again.");
            } else {
                addSystemMessage("Files successfully deleted!");
            }
        } else {
            addCautionMessage("No files selected!");
        }

        return new RedirectResolution(this.getClass());
    }

    /**
     * Event that is triggered when the user has clicked a file name in the list of available files.
     *
     * @return the resulting resolution
     */
    public Resolution select() {

        this.selectedFileName = getContext().getRequestParameter("select");
        if (StringUtils.isBlank(selectedFileName)) {
            addGlobalValidationError("No file selected!");
            return list();
        }

        File selectedFile = new File(FileDownloader.FILES_DIR, selectedFileName);
        if (!selectedFile.exists() || !selectedFile.isFile()) {
            addGlobalValidationError("No such file existing!");
            return new RedirectResolution(getClass());
        }

        uncompressedFileName = CompressUtil.getUncompressedFileName(selectedFile);
        archiveEntries = CompressUtil.getArchiveEntries(selectedFile);

        if (isCompressedFile() || isArchiveFile()) {
            return list();
        } else if (!MSAccessImporter.isMSAccessFile(selectedFile)) {
            addWarningMessage("The selected file is not compressed or archive, and it is not a MicrosoftAccess file either!");
            return new RedirectResolution(getClass());
        } else {
            return new RedirectResolution(StagingDatabaseActionBean.class, "add").addParameter("fileName", selectedFileName);
        }
    }

    /**
     * Event that is triggered when the user has chosen to uncompress a particular compressed file.
     *
     * @return the resulting resolution
     * @throws IOException
     */
    public Resolution uncompress() throws IOException {

        String fileName = getContext().getRequestParameter("uncompress");
        if (StringUtils.isBlank(fileName)) {
            addGlobalValidationError("No file selected!");
        }

        File file = new File(FileDownloader.FILES_DIR, fileName);
        if (!file.exists() || !file.isFile()) {
            addGlobalValidationError("Found no such file: " + fileName);
        }

        File uncompressedFile = new File(FileDownloader.FILES_DIR, CompressUtil.getUncompressedFileName(file));
        if (uncompressedFile.exists()) {
            addWarningMessage("Uncompression failed! A file by this name already exists: " + uncompressedFile.getName());
        } else {
            CompressUtil.uncompress(file, uncompressedFile);
            addSystemMessage("File successfully uncompressed to thyis file: " + uncompressedFile.getName());
        }

        return new RedirectResolution(getClass());
    }

    /**
     * Event that is triggered when the user has chosen to extract a particular archive file.
     *
     * @return the resulting resolution
     * @throws IOException
     */
    public Resolution extract() throws IOException {

        RedirectResolution resolution = new RedirectResolution(getClass());

        String fileName = getContext().getRequestParameter("extract");
        if (StringUtils.isBlank(fileName)) {
            addGlobalValidationError("No file selected!");
            return resolution;
        }

        File file = new File(FileDownloader.FILES_DIR, fileName);
        if (!file.exists() || !file.isFile()) {
            addGlobalValidationError("Found no such file to extract from: " + fileName);
            return resolution;
        }

        if (extractEntries == null || extractEntries.isEmpty()) {
            addSystemMessage("No entries were selected for extraction!");
        } else {
            LinkedHashMap<String, String> entryToNewName = new LinkedHashMap<String, String>();
            for (String entry : extractEntries) {
                String newName = getContext().getRequestParameter(DigestUtils.md5Hex(entry) + "_newName");
                if (StringUtils.isNotBlank(newName)) {
                    if (!isValidNewFileName(newName)) {
                        addWarningMessage("Renamed file name must contain only spaces, digits, latin letters, underscores, dots and minuses!");
                        return resolution;
                    } else {
                        entryToNewName.put(entry, newName);
                    }
                } else {
                    entryToNewName.put(entry, entry);
                }
            }
            CompressUtil.extract(file, entryToNewName);

            String msg = "Selected entries were extracted to the following files:";
            for (String newName : entryToNewName.values()) {
                msg += "\n" + newName;
            }
            addSystemMessage(msg);
        }

        return resolution;
    }

    /**
     * Validate "download" event.
     */
    @ValidationMethod(on = {"download"})
    public void validateDownload() {

        // Check the URL is valid.
        if (!URLUtil.isURL(downloadUrl)) {
            addGlobalValidationError("You supplied an invalid URL!");
        }

        // Check the user-supplied new file name is valid.
        if (StringUtils.isNotBlank(newFileName)) {
            if (!isValidNewFileName(newFileName)) {
                addGlobalValidationError("Only spaces, digits, latin letters, underscores, dots and minuses allowed in file name!");
            }
        }

        getContext().setSourcePageResolution(list());
    }

    /**
     * Returns java.io.File referring to the staging files directory in file system.
     *
     * @return As indicated above.
     */
    public File getStagingFilesDir() {
        return FileDownloader.FILES_DIR;
    }

    /**
     * Suffix to display in UI at file names that are currently being downloaded.
     *
     * @return As indicated above.
     */
    public String getDownloadingSuffix() {
        return FileDownloader.FILE_SUFFIX;
    }

    /**
     * Gets the staging database action bean class.
     *
     * @return the staging database action bean class
     */
    public Class getStagingDatabaseActionBeanClass() {
        return StagingDatabaseActionBean.class;
    }

    /**
     * Gets the staging databases action bean class.
     *
     * @return the staging databases action bean class
     */
    public Class getStagingDatabasesActionBeanClass() {
        return StagingDatabasesActionBean.class;
    }

    /**
     * Sets the upload file.
     *
     * @param uploadFile
     *            the uploadFile to set
     */
    public void setUploadFile(FileBean uploadFile) {
        this.uploadFile = uploadFile;
    }

    /**
     * Sets the download url.
     *
     * @param downloadUrl
     *            the downloadUrl to set
     */
    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
     * Sets the new file name.
     *
     * @param newFileName
     *            the newFileName to set
     */
    public void setNewFileName(String newFileName) {
        this.newFileName = newFileName;
    }

    /**
     * Gets the available files.
     *
     * @return the availableFiles
     */
    public ArrayList<AvailableFile> getAvailableFiles() {
        return availableFiles;
    }

    /**
     * Validates the the user is authorised for any operations on this action bean. If user not authorised, redirects to the
     * {@link AdminWelcomeActionBean} which displays a proper error message. Will be run on any events, since no specific events
     * specified in the {@link ValidationMethod} annotation.
     */
    @ValidationMethod(priority = 1)
    public void validateUserAuthorised() {

        if (getUser() == null || !getUser().isAdministrator()) {
            addGlobalValidationError("You are not authorized for this operation!");
            getContext().setSourcePageResolution(new RedirectResolution(AdminWelcomeActionBean.class));
        }
    }

    /**
     * Initializations and other pre-actions that should be invoked prior to {@link LifecycleStage.BindingAndValidation}.
     */
    @Before(stages = {LifecycleStage.BindingAndValidation})
    public void beforeBindingAndValidation() {

        FilterConfig stripesFilterConfig = StripesFilter.getConfiguration().getBootstrapPropertyResolver().getFilterConfig();
        maxFilePostSize = NumberUtils.toInt(stripesFilterConfig.getInitParameter("FileUpload.MaximumPostSize"));
    }

    /**
     * Gets the max file post size.
     *
     * @return the maxFilePostSize
     */
    public int getMaxFilePostSize() {
        return maxFilePostSize;
    }

    /**
     * Sets the file names.
     *
     * @param fileNames
     *            the fileNames to set
     */
    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    /**
     *
     * @return
     */
    public String getSelectedFileName() {
        return selectedFileName;
    }

    /**
     *
     * @return
     */
    public String getUncompressedFileName() {
        return uncompressedFileName;
    }

    /**
     *
     * @return
     */
    public List<ArchiveEntry> getArchiveEntries() {
        return archiveEntries;
    }

    /**
     *
     * @return
     */
    public boolean isCompressedFile() {
        return StringUtils.isNotBlank(uncompressedFileName);
    }

    /**
     *
     * @return
     */
    public boolean isArchiveFile() {
        return archiveEntries != null && !archiveEntries.isEmpty();
    }

    /**
     *
     * @param extractEntries
     */
    public void setExtractEntries(List<String> extractEntries) {
        this.extractEntries = extractEntries;
    }

    /**
     *
     * @param fileName
     * @return
     */
    private boolean isValidNewFileName(String fileName) {

        if (StringUtils.isBlank(fileName)) {
            return false;
        }

        String s = fileName.replaceAll("[^a-zA-Z0-9-._ ]+", "");
        return s.equals(fileName);
    }
}
