package eionet.cr.web.sparqlClient.helpers;

import info.aduna.io.IndentingWriter;
import info.aduna.text.StringUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
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
 * JSON writer.
 *
 * @author altnyris
 *
 */
public class CRJsonWriter implements TupleQueryResultWriter {

    /** */
    private IndentingWriter writer;

    /** */
    private boolean firstTupleWritten;

    /**
     *
     * Class constructor.
     *
     * @param out
     */
    public CRJsonWriter(OutputStream out) {
        Writer w = new OutputStreamWriter(out, Charset.forName("UTF-8"));
        w = new BufferedWriter(w, 1024);
        writer = new IndentingWriter(w);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.query.resultio.TupleQueryResultWriter#getTupleQueryResultFormat()
     */
    @Override
    public final TupleQueryResultFormat getTupleQueryResultFormat() {
        return TupleQueryResultFormat.JSON;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.query.QueryResultHandler#startQueryResult(java.util.List)
     */
    @Override
    public void startQueryResult(List<String> columnHeaders) throws TupleQueryResultHandlerException {
        try {
            openBraces();

            // Write header
            writeKey("head");
            openBraces();
            writeKeyValue("vars", columnHeaders);
            closeBraces();

            writeComma();

            // Write results
            writeKey("results");
            openBraces();

            writeKey("bindings");
            openArray();

            firstTupleWritten = false;
        } catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.query.QueryResultHandler#endQueryResult()
     */
    @Override
    public void endQueryResult() throws TupleQueryResultHandlerException {
        try {
            closeArray(); // bindings array
            closeBraces(); // results braces
            closeBraces(); // root braces
            writer.flush();
        } catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openrdf.query.QueryResultHandler#handleSolution(org.openrdf.query.BindingSet)
     */
    @Override
    public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
        try {
            if (firstTupleWritten) {
                writeComma();
            } else {
                firstTupleWritten = true;
            }

            openBraces(); // start of new solution

            Iterator<Binding> bindingIter = bindingSet.iterator();
            while (bindingIter.hasNext()) {
                Binding binding = bindingIter.next();

                writeKeyValue(binding.getName(), binding.getValue());

                if (bindingIter.hasNext()) {
                    writeComma();
                }
            }

            closeBraces(); // end solution

            writer.flush();
        } catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    /**
     *
     * @param key
     * @param value
     * @throws IOException
     */
    private void writeKeyValue(String key, String value) throws IOException {
        writeKey(key);
        writeString(value);
    }

    /**
     *
     * @param key
     * @param value
     * @throws IOException
     * @throws TupleQueryResultHandlerException
     */
    private void writeKeyValue(String key, Value value) throws IOException, TupleQueryResultHandlerException {
        writeKey(key);
        writeValue(value);
    }

    /**
     *
     * @param key
     * @param array
     * @throws IOException
     */
    private void writeKeyValue(String key, Iterable<String> array) throws IOException {
        writeKey(key);
        writeArray(array);
    }

    /**
     *
     * @param key
     * @throws IOException
     */
    private void writeKey(String key) throws IOException {
        writeString(key);
        writer.write(": ");
    }

    /**
     *
     * @param value
     * @throws IOException
     * @throws TupleQueryResultHandlerException
     */
    private void writeValue(Value value) throws IOException, TupleQueryResultHandlerException {
        writer.write("{ ");
        if (value != null) {
            if (value instanceof URI) {
                writeKeyValue("type", "uri");
                writer.write(", ");
                writeKeyValue("value", ((URI) value).toString());
            } else if (value instanceof BNode) {
                writeKeyValue("type", "bnode");
                writer.write(", ");
                writeKeyValue("value", ((BNode) value).getID());
            } else if (value instanceof Literal) {
                Literal lit = (Literal) value;

                if (lit.getDatatype() != null) {
                    writeKeyValue("type", "typed-literal");
                    writer.write(", ");
                    writeKeyValue("datatype", lit.getDatatype().toString());
                } else {
                    writeKeyValue("type", "literal");
                    if (lit.getLanguage() != null) {
                        writer.write(", ");
                        writeKeyValue("xml:lang", lit.getLanguage());
                    }
                }

                writer.write(", ");
                writeKeyValue("value", lit.getLabel());
            } else {
                throw new TupleQueryResultHandlerException("Unknown Value object type: " + value.getClass());
            }
        }

        writer.write(" }");
    }

    /**
     *
     * @param value
     * @throws IOException
     */
    private void writeString(String value) throws IOException {
        // Escape special characters
        value = StringUtil.gsub("\\", "\\\\", value);
        value = StringUtil.gsub("\"", "\\\"", value);
        value = StringUtil.gsub("/", "\\/", value);
        value = StringUtil.gsub("\b", "\\b", value);
        value = StringUtil.gsub("\f", "\\f", value);
        value = StringUtil.gsub("\n", "\\n", value);
        value = StringUtil.gsub("\r", "\\r", value);
        value = StringUtil.gsub("\t", "\\t", value);

        writer.write("\"");
        writer.write(value);
        writer.write("\"");
    }

    /**
     *
     * @param array
     * @throws IOException
     */
    private void writeArray(Iterable<String> array) throws IOException {
        writer.write("[ ");

        Iterator<String> iter = array.iterator();
        while (iter.hasNext()) {
            String value = iter.next();

            writeString(value);

            if (iter.hasNext()) {
                writer.write(", ");
            }
        }

        writer.write(" ]");
    }

    /**
     * @throws IOException
     */
    private void openArray() throws IOException {
        writer.write("[");
        writer.writeEOL();
        writer.increaseIndentation();
    }

    /**
     * @throws IOException
     */
    private void closeArray() throws IOException {
        writer.writeEOL();
        writer.decreaseIndentation();
        writer.write("]");
    }

    /**
     * @throws IOException
     */
    private void openBraces() throws IOException {
        writer.write("{");
        writer.writeEOL();
        writer.increaseIndentation();
    }

    /**
     * @throws IOException
     */
    private void closeBraces() throws IOException {
        writer.writeEOL();
        writer.decreaseIndentation();
        writer.write("}");
    }

    /**
     * @throws IOException
     */
    private void writeComma() throws IOException {
        writer.write(", ");
        writer.writeEOL();
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
