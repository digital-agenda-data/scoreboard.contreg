package eionet.cr.util.xlwrap;

import java.util.LinkedHashSet;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

import eionet.cr.common.Predicates;
import eionet.cr.web.action.admin.XLWrapUploadActionBean;

/**
 * An implementation of {@link RDFHandler} that will be used to listen to statements
 * imported by the {@link XLWrapUploadActionBean#upload()}.
 *
 * @author jaanus
 */
public class StatementListener implements RDFHandler {

    /** */
    private String subjectsRdfType;

    /** */
    private Set<String> timePeriodUris = new LinkedHashSet<String>();

    /** */
    private Set<String> subjects = new LinkedHashSet<String>();

    /** */
    private Set<String> datasetUris = new LinkedHashSet<String>();

    /** */
    private Set<String> graphs = new LinkedHashSet<String>();

    /**
     * @param subjectsRdfType
     */
    public StatementListener(String subjectsRdfType) {
        if (subjectsRdfType == null) {
            subjectsRdfType = "";
        }
        this.subjectsRdfType = subjectsRdfType;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.rio.RDFHandler#endRDF()
     */
    @Override
    public void endRDF() throws RDFHandlerException {
        // Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.rio.RDFHandler#handleComment(java.lang.String)
     */
    @Override
    public void handleComment(String arg0) throws RDFHandlerException {
        // Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.rio.RDFHandler#handleNamespace(java.lang.String, java.lang.String)
     */
    @Override
    public void handleNamespace(String arg0, String arg1) throws RDFHandlerException {
        // Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.rio.RDFHandler#handleStatement(org.openrdf.model.Statement)
     */
    @Override
    public void handleStatement(Statement stmt) throws RDFHandlerException {

        URI predicateURI = stmt.getPredicate();
        if (Predicates.DAS_TIMEPERIOD.equals(predicateURI.stringValue())) {
            Value object = stmt.getObject();
            if (object instanceof URI) {
                timePeriodUris.add(object.stringValue());
            }
        }

        if (Predicates.RDF_TYPE.equals(predicateURI.stringValue())) {
            Value object = stmt.getObject();
            if (object != null && subjectsRdfType.equals(object.stringValue())) {
                subjects.add(stmt.getSubject().stringValue());
            }
        }

        if (Predicates.DATACUBE_DATA_SET.equals(predicateURI.stringValue())) {
            datasetUris.add(stmt.getObject().stringValue());
        }

        graphs.add(stmt.getContext().stringValue());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.rio.RDFHandler#startRDF()
     */
    @Override
    public void startRDF() throws RDFHandlerException {
        // Auto-generated method stub
    }

    /**
     * @return
     */
    public Set<String> getSubjects() {
        return subjects;
    }

    /**
     * @return the timePeriodUris
     */
    public Set<String> getTimePeriodUris() {
        return timePeriodUris;
    }

    /**
     * @return the datasetUris
     */
    public Set<String> getDatasetUris() {
        return datasetUris;
    }

    /**
     * @return the graphs
     */
    public Set<String> getGraphs() {
        return graphs;
    }
}
