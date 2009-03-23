package eionet.cr.search.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import eionet.cr.dto.ObjectDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.util.YesNoBoolean;
import eionet.cr.util.sql.ResultSetBaseReader;

/**
 * 
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
public class SubjectDataReader extends ResultSetBaseReader{

	/** */
	private LinkedHashMap<String,SubjectDTO> subjectsMap;
	
	/** */
	private SubjectDTO currentSubject = null;
	private String currentPredicate = null;
	private Collection<ObjectDTO> currentObjects = null;
	private StringBuffer predicateHashesCommaSeparated = new StringBuffer();
	
	/**
	 * 
	 * @param subjectsMap
	 */
	public SubjectDataReader(LinkedHashMap<String,SubjectDTO> subjectsMap){
		this.subjectsMap = subjectsMap;
	}
	
	/*
	 * (non-Javadoc)
	 * @see eionet.cr.util.sql.ResultSetBaseReader#readRow(java.sql.ResultSet)
	 */
	public void readRow(ResultSet rs) throws SQLException {
		
		String subjectUri = rs.getString("SUBJECT_URI");
		boolean newSubject = currentSubject==null || !currentSubject.getUri().equals(subjectUri);
		if (newSubject){
			String subjectHash = rs.getString("SUBJECT");
			currentSubject = new SubjectDTO(subjectUri, YesNoBoolean.parse(rs.getString("ANON_SUBJ")));
			subjectsMap.put(subjectHash, currentSubject);
		}
		
		String predicateUri = rs.getString("PREDICATE_URI");
		boolean newPredicate = newSubject || currentPredicate==null || !currentPredicate.equals(predicateUri);
		if (newPredicate){
			currentPredicate = predicateUri;
			currentObjects = new ArrayList<ObjectDTO>();
			currentSubject.getPredicates().put(predicateUri, currentObjects);
		}
		
		addPredicateHash(rs.getString("PREDICATE_HASH"));
		
		ObjectDTO object = new ObjectDTO(rs.getString("OBJECT"),
											rs.getString("OBJ_LANG"),
											YesNoBoolean.parse(rs.getString("LIT_OBJ")),
											YesNoBoolean.parse(rs.getString("ANON_OBJ")));
		object.setSource(rs.getString("SOURCE_URI"));
		object.setDerivSource(rs.getString("DERIV_SOURCE_URI"));
		
		currentObjects.add(object);
	}
	
	/**
	 * 
	 * @return
	 */
	private void addPredicateHash(String predicateHash){
		
		if (predicateHashesCommaSeparated.length()>0){
			predicateHashesCommaSeparated.append(",");
		}
		predicateHashesCommaSeparated.append(predicateHash);
	}

	/**
	 * 
	 */
	public String getPredicateHashesCommaSeparated() {
		return predicateHashesCommaSeparated.toString();
	}
}