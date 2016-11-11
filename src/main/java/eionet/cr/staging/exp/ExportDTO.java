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

package eionet.cr.staging.exp;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * DTO (Data Transfer Object) for records in STAGING_DB table.
 *
 * @author jaanus
 */
public class ExportDTO {

    /** */
    private int exportId;
    private int databaseId;
    private String exportName;
    private String databaseName;
    private String userName;
    private String queryConf;
    private Date started;
    private Date finished;
    private ExportStatus status;
    private String exportLog;
    private int noOfSubjects;
    private int noOfTriples;
    private int rowCount;
    private String missingConcepts;
    private String graphs;

    /**
     * @return the exportId
     */
    public int getExportId() {
        return exportId;
    }

    /**
     * @param exportId the exportId to set
     */
    public void setExportId(int exportId) {
        this.exportId = exportId;
    }

    /**
     * @return the databaseId
     */
    public int getDatabaseId() {
        return databaseId;
    }

    /**
     * @param databaseId the databaseId to set
     */
    public void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the queryConf
     */
    public String getQueryConf() {
        return queryConf;
    }

    /**
     * @param queryConf the queryConf to set
     */
    public void setQueryConf(String queryConf) {
        this.queryConf = queryConf;
    }

    /**
     * @return the started
     */
    public Date getStarted() {
        return started;
    }

    /**
     * @param started the started to set
     */
    public void setStarted(Date started) {
        this.started = started;
    }

    /**
     * @return the finished
     */
    public Date getFinished() {
        return finished;
    }

    /**
     * @param finished the finished to set
     */
    public void setFinished(Date finished) {
        this.finished = finished;
    }

    /**
     * @return the status
     */
    public ExportStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(ExportStatus status) {
        this.status = status;
    }

    /**
     * @return the exportLog
     */
    public String getExportLog() {
        return exportLog;
    }

    /**
     * @param exportLog the exportLog to set
     */
    public void setExportLog(String exportLog) {
        this.exportLog = exportLog;
    }

    /**
     * @return the noOfSubjects
     */
    public int getNoOfSubjects() {
        return noOfSubjects;
    }

    /**
     * @param noOfSubjects the noOfSubjects to set
     */
    public void setNoOfSubjects(int noOfSubjects) {
        this.noOfSubjects = noOfSubjects;
    }

    /**
     * @return the noOfTriples
     */
    public int getNoOfTriples() {
        return noOfTriples;
    }

    /**
     * @param noOfTriples the noOfTriples to set
     */
    public void setNoOfTriples(int noOfTriples) {
        this.noOfTriples = noOfTriples;
    }

    /**
     * @return the exportName
     */
    public String getExportName() {
        return exportName;
    }

    /**
     * @param exportName the exportName to set
     */
    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    /**
     * @return the databaseName
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * @param databaseName the databaseName to set
     */
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * @return the rowCount
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     * @param rowCount the rowCount to set
     */
    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    /**
     * @return the missingConcepts
     */
    public String getMissingConcepts() {
        return missingConcepts;
    }

    /**
     * @param missingConcepts the missingConcepts to set
     */
    public void setMissingConcepts(String missingConcepts) {
        this.missingConcepts = missingConcepts;
    }

    /**
     * @return the graphs
     */
    public String getGraphs() {
        return graphs;
    }

    /**
     * @param graphs the graphs to set
     */
    public void setGraphs(String graphs) {
        this.graphs = graphs;
    }

    /**
     *
     * @return
     */
    public List<String> getGraphsList() {
        if (StringUtils.isNotBlank(graphs)) {
            String[] split = StringUtils.split(graphs, "\n");
            return Arrays.asList(split);
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     *
     * @return
     */
    public Map<String, String> getGraphsToDatasets() {

        Map<String, String> resultMap = new LinkedHashMap<>();
        for (String graphUri : getGraphsList()) {
            resultMap.put(graphUri, graphUri.replace("/data/", "/dataset/"));
        }

        return resultMap;
    }
}
