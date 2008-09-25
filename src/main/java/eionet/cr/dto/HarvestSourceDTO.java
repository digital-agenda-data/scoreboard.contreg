/**
 *
 */
package eionet.cr.dto;

import java.io.Serializable;
import java.util.Date;

import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;

/**
 * @author altnyris
 *
 */
public class HarvestSourceDTO implements Serializable {
	
	/** */
	public static final int COUNT_UNAVAIL_THRESHOLD = 5;
	public static final String DEDICATED_HARVEST_SOURCE_DEFAULT_CRON = "0 0 2 ? * 6L"; // 02:00am on the last Friday of every month
	
	/** */
	private Integer sourceId;
	private String name;
	private String url;
	private String type;
	private String emails;
	private Date dateCreated;
	private String creator;
	private Integer statements;
	private Integer resources;
	private Integer countUnavail;
	private String scheduleCron;
	
	/**
	 * 
	 */
	public HarvestSourceDTO(){
	}

	/**
	 * @return the sourceId
	 */
	public Integer getSourceId() {
		return sourceId;
	}

	/**
	 * @param sourceId the sourceId to set
	 */
	public void setSourceId(Integer sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
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
	 * @return the emails
	 */
	public String getEmails() {
		return emails;
	}

	/**
	 * @param emails the emails to set
	 */
	public void setEmails(String emails) {
		this.emails = emails;
	}

	/**
	 * @return the dateCreated
	 */
	public Date getDateCreated() {
		return dateCreated;
	}

	/**
	 * @param dateCreated the dateCreated to set
	 */
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}

	/**
	 * @return the creator
	 */
	public String getCreator() {
		return creator;
	}

	/**
	 * @param creator the creator to set
	 */
	public void setCreator(String creator) {
		this.creator = creator;
	}

	/**
	 * @return the statements
	 */
	public Integer getStatements() {
		return statements;
	}

	/**
	 * @param statements the statements to set
	 */
	public void setStatements(Integer statements) {
		this.statements = statements;
	}

	/**
	 * @return the resources
	 */
	public Integer getResources() {
		return resources;
	}

	/**
	 * @param resources the resources to set
	 */
	public void setResources(Integer resources) {
		this.resources = resources;
	}

	/**
	 * @return the countUnavail
	 */
	public Integer getCountUnavail() {
		return countUnavail;
	}

	/**
	 * @param countUnavail the countUnavail to set
	 */
	public void setCountUnavail(Integer countUnavail) {
		this.countUnavail = countUnavail;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isUnavailable(){
		
		return countUnavail!=null && countUnavail.intValue()>=COUNT_UNAVAIL_THRESHOLD;
	}

	/**
	 * @return the scheduleCron
	 */
	public String getScheduleCron() {
		return scheduleCron;
	}

	/**
	 * @param scheduleCron the scheduleCron to set
	 */
	public void setScheduleCron(String scheduleCron) {
		this.scheduleCron = scheduleCron;
	}
}
