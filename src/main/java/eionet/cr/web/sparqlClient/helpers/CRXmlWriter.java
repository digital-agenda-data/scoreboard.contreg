package eionet.cr.web.sparqlClient.helpers;

import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.BINDING_NAME_ATT;
import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.BINDING_TAG;
import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.BNODE_TAG;
import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.HEAD_TAG;
import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.LITERAL_DATATYPE_ATT;
import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.LITERAL_LANG_ATT;
import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.LITERAL_TAG;
import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.NAMESPACE;
import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.RESULT_SET_TAG;
import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.RESULT_TAG;
import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.ROOT_TAG;
import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.URI_TAG;
import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.VAR_NAME_ATT;
import static eionet.cr.web.sparqlClient.helpers.SPARQLResultsXMLConstants.VAR_TAG;
import info.aduna.xml.XMLWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.QueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;
import org.openrdf.rio.RioSetting;
import org.openrdf.rio.WriterConfig;

/**
 * A {@link TupleQueryResultWriter} that writes tuple query results in the <a href="http://www.w3.org/TR/rdf-sparql-XMLres/">SPARQL
 * Query Results XML Format</a>.
 */
public class CRXmlWriter implements TupleQueryResultWriter {

    /*-----------*
     * Variables *
     *-----------*/

    /**
     * XMLWriter to write XML to.
     */
    private XMLWriter xmlWriter;

    /*--------------*
     * Constructors *
     *--------------*/

    public CRXmlWriter(OutputStream out) {
        this(new XMLWriter(out));
    }

    public CRXmlWriter(XMLWriter xmlWriter) {
        this.xmlWriter = xmlWriter;
        this.xmlWriter.setPrettyPrint(true);
    }

    /*---------*
     * Methods *
     *---------*/

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.query.resultio.TupleQueryResultWriter#getTupleQueryResultFormat()
     */
    @Override
    public final TupleQueryResultFormat getTupleQueryResultFormat() {
        return TupleQueryResultFormat.SPARQL;
    }

    /**
     * Enables/disables addition of indentation characters and newlines in the XML document. By default, pretty-printing is set to
     * <tt>true</tt>. If set to <tt>false</tt>, no indentation and newlines are added to the XML document. This method has to be
     * used before writing starts (that is, before {@link #startQueryResult(List)} is called).
     */
    public void setPrettyPrint(boolean prettyPrint) {
        xmlWriter.setPrettyPrint(prettyPrint);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.query.TupleQueryResultHandler#startQueryResult(java.util.List)
     */
    @Override
    public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
        try {
            xmlWriter.startDocument();

            xmlWriter.setAttribute("xmlns", NAMESPACE);
            xmlWriter.startTag(ROOT_TAG);

            // Write header
            xmlWriter.startTag(HEAD_TAG);
            for (String name : bindingNames) {
                xmlWriter.setAttribute(VAR_NAME_ATT, name);
                xmlWriter.emptyElement(VAR_TAG);
            }
            xmlWriter.endTag(HEAD_TAG);

            // Write start of results
            xmlWriter.startTag(RESULT_SET_TAG);
        } catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.query.TupleQueryResultHandler#endQueryResult()
     */
    @Override
    public void endQueryResult() throws TupleQueryResultHandlerException {
        try {
            xmlWriter.endTag(RESULT_SET_TAG);
            xmlWriter.endTag(ROOT_TAG);

            xmlWriter.endDocument();
        } catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.query.TupleQueryResultHandler#handleSolution(org.openrdf.query.BindingSet)
     */
    @Override
    public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
        try {
            xmlWriter.startTag(RESULT_TAG);

            for (Binding binding : bindingSet) {
                if (binding.getValue() != null) {
                    xmlWriter.setAttribute(BINDING_NAME_ATT, binding.getName());
                    xmlWriter.startTag(BINDING_TAG);

                    writeValue(binding.getValue());

                    xmlWriter.endTag(BINDING_TAG);
                }
            }

            xmlWriter.endTag(RESULT_TAG);
        } catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    /**
     * Utility method for writing the given {@link Value}.
     *
     * @param value The given {@link Value}.
     * @throws IOException
     */
    private void writeValue(Value value) throws IOException {
        if (value instanceof URI) {
            writeURI((URI) value);
        } else if (value instanceof BNode) {
            writeBNode((BNode) value);
        } else if (value instanceof Literal) {
            writeLiteral((Literal) value);
        }
    }

    /**
     * Utility method for writing the given {@link URI}.
     *
     * @param value The given {@link URI}.
     * @throws IOException
     */
    private void writeURI(URI uri) throws IOException {
        xmlWriter.textElement(URI_TAG, uri.toString());
    }

    /**
     * Utility method for writing the given {@link BNode}.
     *
     * @param bNode The given {@link BNode}.
     * @throws IOException
     */
    private void writeBNode(BNode bNode) throws IOException {
        xmlWriter.textElement(BNODE_TAG, bNode.getID());
    }

    /**
     * Utility method for writing the given {@link Literal}.
     *
     * @param bNode The given {@link Literal}.
     * @throws IOException
     */
    private void writeLiteral(Literal literal) throws IOException {
        if (literal.getLanguage() != null) {
            xmlWriter.setAttribute(LITERAL_LANG_ATT, literal.getLanguage());
        } else if (literal.getDatatype() != null) {
            URI datatype = literal.getDatatype();
            xmlWriter.setAttribute(LITERAL_DATATYPE_ATT, datatype.toString());
        }

        xmlWriter.textElement(LITERAL_TAG, literal.getLabel());
    }

    @Override
    public void handleBoolean(boolean paramBoolean) throws QueryResultHandlerException {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleLinks(List<String> paramList) throws QueryResultHandlerException {
        // TODO Auto-generated method stub

    }

    @Override
    public QueryResultFormat getQueryResultFormat() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void handleNamespace(String paramString1, String paramString2) throws QueryResultHandlerException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startDocument() throws QueryResultHandlerException {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleStylesheet(String paramString) throws QueryResultHandlerException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startHeader() throws QueryResultHandlerException {
        // TODO Auto-generated method stub

    }

    @Override
    public void endHeader() throws QueryResultHandlerException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setWriterConfig(WriterConfig paramWriterConfig) {
        // TODO Auto-generated method stub

    }

    @Override
    public WriterConfig getWriterConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<RioSetting<?>> getSupportedSettings() {
        // TODO Auto-generated method stub
        return null;
    }
}
