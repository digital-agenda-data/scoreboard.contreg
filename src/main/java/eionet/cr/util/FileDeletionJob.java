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

package eionet.cr.util;

import java.io.File;
import java.io.FileFilter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.StatefulJob;

import eionet.cr.common.JobScheduler;
import eionet.cr.common.TempFilePathGenerator;
import eionet.cr.config.GeneralConfig;

/**
 * Background job that silently deletes CR's temporary files in the background. Files must be registered to this job to get deleted.
 *
 * @author Jaanus Heinlaid
 */
public class FileDeletionJob implements ServletContextListener, StatefulJob {

    /** */
    private static final Logger LOGGER = Logger.getLogger(FileDeletionJob.class);

    /** */
    private static final long RUNNING_INTERVAL_MS = Long.parseLong(GeneralConfig.getProperty(
            GeneralConfig.FILE_DELETION_JOB_INTERVAL, "20000"));

    /** */
    private static final Collection<File> QUEUED_FILES = Collections.synchronizedSet(new HashSet<File>());

    /** */
    private static final TempFileFilter TEMP_FILE_FILTER = new FileDeletionJob.TempFileFilter();

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        Class<FileDeletionJob> clazz = FileDeletionJob.class;
        JobDetail jobDetails = new JobDetail(clazz.getSimpleName(), clazz.getName(), clazz);
        Exception exception = null;
        try {
            LOGGER.debug("Scheduling " + clazz.getSimpleName() + " with interval " + RUNNING_INTERVAL_MS + " ms");
            JobScheduler.scheduleIntervalJob(RUNNING_INTERVAL_MS, jobDetails);
        } catch (SchedulerException e) {
            exception = e;
        } catch (ParseException e) {
            exception = e;
        }

        if (exception != null) {
            LOGGER.error("Failed to schedule " + clazz.getSimpleName(), exception);
        }

        // find and register for deletion the temporary files still left from previous application runs
        queueLeftovers();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    @Override
    public void execute(JobExecutionContext executionContext) throws JobExecutionException {

        ArrayList<File> toBeRemvedFromQueue = new ArrayList<File>();

        for (File file : QUEUED_FILES) {
            if (file != null && file.exists()) {
                try {
                    boolean deleted = file.delete();
                    if (deleted) {
                        toBeRemvedFromQueue.add(file);
                        LOGGER.debug("File successfully deleted: " + file);
                    } else {
                        LOGGER.debug("Deleting this file failed, trying next time: " + file);
                    }
                } catch (SecurityException e) {
                    LOGGER.error("Security exception when trying to delete " + file, e);
                } catch (RuntimeException e) {
                    LOGGER.error("Unexpected RuntimeException when trying to delete " + file, e);
                }
            } else {
                toBeRemvedFromQueue.add(file);
            }
        }

        QUEUED_FILES.removeAll(toBeRemvedFromQueue);
    }

    /**
     *
     * @param files
     */
    public static synchronized void register(File[] files) {

        for (File file : files) {
            FileDeletionJob.register(file);
        }
    }

    /**
     *
     * @param files
     */
    public static synchronized void register(Collection<File> files) {

        for (File file : files) {
            FileDeletionJob.register(file);
        }
    }

    /**
     *
     * @param file
     * @param marker
     */
    public static synchronized void register(File file) {

        if (file != null && file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                QUEUED_FILES.add(file);
            } else {
                LOGGER.debug("File successfully deleted: " + file);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        LOGGER.debug(this.getClass().getName() + " context destroyed");
    }

    /**
     * Finds and register for deletion the temporary files still left from previous application runs.
     */
    private void queueLeftovers() {

        for (File tempFileDir : TempFilePathGenerator.TEMP_FILE_DIRECTORIES) {

            if (tempFileDir.exists() && tempFileDir.isDirectory()) {

                File[] tempFiles = tempFileDir.listFiles(TEMP_FILE_FILTER);
                for (int i = 0; i < tempFiles.length; i++) {

                    LOGGER.debug("Found leftover temporary file from previous runs, re-registering for deletion: " + tempFiles[i]);
                    register(tempFiles[i]);
                }
            }
        }
    }

    /**
     *
     * Implementation of {@link java.io.FilenameFilter} that checks if a given file is a temporary file created by this application.
     *
     * @author Jaanus Heinlaid
     */
    static class TempFileFilter implements FileFilter {

        /*
         * (non-Javadoc)
         *
         * @see java.io.FileFilter#accept(java.io.File)
         */
        @Override
        public boolean accept(File file) {

            return file.isFile() && file.getName().startsWith(TempFilePathGenerator.PREFIX);
        }

    }
}
