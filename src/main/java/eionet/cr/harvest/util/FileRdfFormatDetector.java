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

package eionet.cr.harvest.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.nio.channels.FileLockInterruptionException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.IIOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.n3.N3ParserFactory;
import org.openrdf.rio.rdfxml.RDFXMLParserFactory;
import org.openrdf.rio.turtle.TurtleParserFactory;

/**
 * Detects the {@link RDFFormat} of a given file.
 *
 *
 * @author jaanus
 */
public class FileRdfFormatDetector {

    /** */
    private static final Log LOGGER = LogFactory.getLog(FileRdfFormatDetector.class);

    /** RDF format parser factories used in this class. */
    private static final RDFParserFactory[] PARSER_FACTORIES = {new RDFXMLParserFactory(), new TurtleParserFactory(),
            new N3ParserFactory()};

    /** Parsing exceptions for each parsed format. Using linked hash-map, so the iterator will be in parsing formats order. */
    private Map<RDFFormat, Exception> parsingExceptions = new LinkedHashMap<RDFFormat, Exception>();

    /**
     * Detects the {@link RDFFormat} of the given file, in the content of the given base URI. If null returned, no RDF format was
     * detected.
     *
     * For each RDF format that the file was parsed for, the received parsing exceptions shall be available from
     * {@link #getParsingExceptions()}.
     *
     * @param inputStream
     * @param baseUri
     * @return
     * @throws IOException
     */
    public RDFFormat detect(File file, String baseUri) throws IOException {

        RDFFormat result = null;

        for (int i = 0; i < PARSER_FACTORIES.length; i++) {

            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);
                RDFParserFactory parserFactory = PARSER_FACTORIES[i];

                boolean success = parse(inputStream, baseUri, parserFactory);
                if (success) {
                    result = parserFactory.getRDFFormat();
                    break;
                }
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        return result;
    }

    /**
     * Returns true if the given input stream is a valid notation of the RDF format whose parser-factory has been supplied.
     *
     * @param inputStream
     *            - the input stream in question
     * @param baseUri
     *            - the base URI that will supplied to the {@link RDFParser} that will be supplied dby the given factory
     * @param parserFactory
     *            - the parser-factory in question
     * @return
     * @throws IOException
     */
    private boolean parse(InputStream inputStream, String baseUri, RDFParserFactory parserFactory) throws IOException {

        boolean result = false;

        RDFParser parser = parserFactory.getParser();
        parser.setRDFHandler(new Handler());
        try {
            parser.parse(new InputStreamReader(inputStream), baseUri);
            result = true;
        } catch (RDFParseException e) {
            parsingExceptions.put(parserFactory.getRDFFormat(), e);
        } catch (RDFHandlerException e) {
            result = true;
        } catch (IOException e) {

            // Only the below selected sub-classes of IOException will be thrown.
            // All the rest (i.e. the last else block) are considered non-fatal, therefore not thrown.
            if (e instanceof FileNotFoundException) {
                throw e;
            } else if (e instanceof FileLockInterruptionException) {
                throw e;
            } else if (e instanceof IIOException) {
                throw e;
            } else if (e instanceof InterruptedIOException) {
                throw e;
            } else {
                parsingExceptions.put(parserFactory.getRDFFormat(), e);
            }
        }

        return result;
    }

    /**
     * Returns a map of parsing exception that were encountered for each RDF format.
     *
     * @return
     */
    public Map<RDFFormat, Exception> getParsingExceptions() {
        return parsingExceptions;
    }

    /**
     * An extension of {@link RDFHandlerBase} whose only purpose is to help detect whether the parsed content is valid RDF notation.
     * If by 100th statement there have been no {@link RDFParseException} thrown, it throws a dummy {@link RDFHandlerException} to
     * abort any further processing. Callers must interpret this exception as "parsed content is valid RDF notation".
     *
     * @author jaanus
     */
    class Handler extends RDFHandlerBase {

        /** */
        int statementCounter = 0;

        /*
         * (non-Javadoc)
         *
         * @see org.openrdf.rio.helpers.RDFHandlerBase#handleStatement(org.openrdf.model.Statement)
         */
        @Override
        public void handleStatement(Statement statement) throws RDFHandlerException {

            // If we have reached this far by 100th statement, the parsed content probably is a valid RDF notation,
            // so lets abort here by throwing a fake RDFHandlerException.
            if (++statementCounter == 100) {
                throw new RDFHandlerException("");
            }
        }
    }
}
