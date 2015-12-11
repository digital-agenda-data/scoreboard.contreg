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

package eionet.cr.harvest;

import static eionet.cr.harvest.ResponseCodeUtil.isError;
import static eionet.cr.harvest.ResponseCodeUtil.isNotModified;
import static eionet.cr.harvest.ResponseCodeUtil.isPermanentError;
import static eionet.cr.harvest.ResponseCodeUtil.isRedirect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sparql.SPARQLConnection;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.openrdf.repository.sparql.query.SPARQLGraphQuery;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.xml.sax.SAXException;

import eionet.cr.common.Predicates;
import eionet.cr.common.TempFilePathGenerator;
import eionet.cr.config.GeneralConfig;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.EndpointHarvestQueryDAO;
import eionet.cr.dao.HarvestSourceDAO;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.PostHarvestScriptDAO;
import eionet.cr.dto.EndpointHarvestQueryDTO;
import eionet.cr.dto.HarvestMessageDTO;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.dto.ObjectDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.filestore.FileStore;
import eionet.cr.harvest.load.ContentLoader;
import eionet.cr.harvest.load.FeedFormatLoader;
import eionet.cr.harvest.load.RDFFormatLoader;
import eionet.cr.harvest.util.EndpointHttpClient;
import eionet.cr.harvest.util.HarvestMessageType;
import eionet.cr.harvest.util.MediaTypeToDcmiTypeConverter;
import eionet.cr.harvest.util.RDFMediaTypes;
import eionet.cr.util.FileDeletionJob;
import eionet.cr.util.Hashes;
import eionet.cr.util.URLUtil;
import eionet.cr.util.Util;
import eionet.cr.util.sesame.SesameUtil;
import eionet.cr.util.xml.ConversionsParser;

/**
 *
 * @author Jaanus Heinlaid
 */
public class PullHarvest extends BaseHarvest {

    /** */
    private static final int NO_RESPONSE = -1;

    /** */
    private static final Logger LOGGER = Logger.getLogger(PullHarvest.class);

    /** */
    private static final int MAX_REDIRECTIONS = 4;

    /** */
    private static final int MAX_CONTENTLENGTH = 5;

    /** */
    private static final String ACCEPT_HEADER = StringUtils.join(RDFMediaTypes.collection(), ',') + ",text/xml,*/*;q=0.6";

    /** */
    private boolean isSourceAvailable;

    /** */
    private final List<String> redirectedUrls = new ArrayList<String>();

    /**
     * @param contextUrl
     * @throws HarvestException
     */
    public PullHarvest(String contextUrl) throws HarvestException {
        super(contextUrl);
    }

    /**
     *
     * @param contextSourceDTO
     * @throws DAOException
     */
    public PullHarvest(HarvestSourceDTO contextSourceDTO) throws DAOException {
        super(contextSourceDTO);
    }

    /**
     * Harvests file already uploaded to a CR folder and residing in the filestore.
     *
     * @throws HarvestException
     *             if harvest fails
     */
    private void doLocalFileHarvest() throws HarvestException {
        String initialContextUrl = getContextUrl();

        httpResponseCode = NO_RESPONSE;
        File file = null;
        String responseMessage = null;

        try {

            String message = "Opening connection to local file";
            LOGGER.debug(loggerMsg(message));

            file = FileStore.getByUri(initialContextUrl);

            if (file == null) {
                finishWithError(NO_RESPONSE, "The file does not exist", new HarvestException("The file does not exist"));
                return;
            }

            isSourceAvailable = true;

            // if URL connection returned no errors and its content has been modified since last harvest,
            // proceed to downloading

            // get content type and title from previously saved triples
            SubjectDTO subject = DAOFactory.get().getDao(HelperDAO.class).getFactsheet(initialContextUrl, null, null);
            String contentType = (subject != null ? subject.getObjectValue(Predicates.CR_MEDIA_TYPE) : null);
            String fileTitle = (subject != null ? subject.getObjectValue(Predicates.DC_TITLE) : null);

            int noOfTriples = processLocalContent(file, contentType);

            // for local files store N/A as http response code

            setStoredTriplesCount(noOfTriples);
            LOGGER.debug(loggerMsg(noOfTriples + " triples loaded"));

            if (!StringUtils.isBlank(fileTitle)) {
                addSourceMetadata(Predicates.DC_TITLE, ObjectDTO.createLiteral(fileTitle));
            }
            if (!StringUtils.isBlank(contentType)) {
                addSourceMetadata(Predicates.CR_MEDIA_TYPE, ObjectDTO.createLiteral(contentType));
            }

            addSourceMetadata(Predicates.CR_BYTE_SIZE, ObjectDTO.createLiteral(String.valueOf(file.length())));
            addSourceMetadata(Predicates.CR_LAST_MODIFIED, ObjectDTO.createLiteral(formatDate(new Date()), XMLSchema.DATETIME));

            httpResponseCode = 0;
            finishWithOK(null, noOfTriples);

        } catch (Exception e) {

            LOGGER.debug(loggerMsg("Exception occurred (will be further logged by caller below): " + e.toString()));

            // check what caused the DAOException - fatal flag is set to true
            checkAndSetFatalExceptionFlag(e.getCause());

            try {
                finishWithError(httpResponseCode, responseMessage, e);
            } catch (RuntimeException finishingException) {
                LOGGER.error("Error when finishing up: ", finishingException);
            }
            if (e instanceof HarvestException) {
                throw (HarvestException) e;
            } else {
                throw new HarvestException(e.getMessage(), e);
            }

        }

    }

    /**
     *
     * @throws HarvestException
     */
    private void doEndpointHarvest() throws HarvestException {

        int numberOfTriples = 0;
        EndpointHttpClient httpClient = prepareEndpointHttpClient();

        GraphQueryResult queryResult = null;
        RepositoryConnection localConn = null;
        RepositoryConnection endpointConn = null;
        try {
            // First see if this particular endpoint has any active harvest queries mapped to it at all.
            String endpointUrl = getContextUrl();
            List<EndpointHarvestQueryDTO> queries =
                    DAOFactory.get().getDao(EndpointHarvestQueryDAO.class).listByEndpointUrl(endpointUrl, true);
            if (queries == null || queries.isEmpty()) {
                LOGGER.warn(loggerMsg("Found no active harvest queries for this endpoint"));
                return;
            }

            // Prepare remote repository connection
            SPARQLRepository sparqlRepository = new SPARQLRepository(endpointUrl);
            endpointConn = sparqlRepository.getConnection();

            // Prepare local repository connection
            localConn = SesameUtil.getRepositoryConnection();
            localConn.setAutoCommit(false);

            // Prepare local repository value factory
            ValueFactory vf = localConn.getValueFactory();
            org.openrdf.model.URI graphURI = vf.createURI(endpointUrl);

            // Loop through the harvest queries, execute each one of them on the remote repository,
            // write the returned statements straight into local repository.
            for (EndpointHarvestQueryDTO queryDTO : queries) {

                LOGGER.debug(loggerMsg("Executing endpoint harvest query with id = " + queryDTO.getId()));
                GraphQuery graphQuery = new SPARQLGraphQuery(httpClient, endpointUrl, endpointUrl, queryDTO.getQuery());

                // Note that the returned GraphQueryResult is always a org.openrdf.repository.sparql.query.BackgroundGraphResult
                // for remote endpoints, and it's a result that returns its statements (i.e. triples) AS THEY ARE PARSED.
                // i.e. the statements ARE NOT written to the memory first and then passed back. So no memory problems here, and
                // safe to use it this way.
                queryResult = graphQuery.evaluate();
                if (queryResult != null) {
                    while (queryResult.hasNext()) {

                        // Clear the graph just before first insert.
                        if (numberOfTriples == 0) {
                            localConn.clear(graphURI);
                        }
                        localConn.add(queryResult.next(), graphURI);
                        numberOfTriples++;
                    }
                }
            }

            localConn.commit();

            setStoredTriplesCount(numberOfTriples);
            LOGGER.debug(loggerMsg("All queries executed, total of " + numberOfTriples + " triples loaded"));
            finishWithOK(null, numberOfTriples);

        } catch (Exception e) {

            SesameUtil.rollback(localConn);
            LOGGER.debug(loggerMsg("Exception occurred (will be further logged by caller below): " + e.toString()));

            checkAndSetFatalExceptionFlag(e.getCause());
            try {
                finishWithError(httpClient.getLastExecutionResponseCode(), httpClient.getLastExecutionResponseText(), e);
            } catch (RuntimeException finishingException) {
                LOGGER.error("Error when finishing up: ", finishingException);
            }

            if (e instanceof HarvestException) {
                throw (HarvestException) e;
            } else {
                throw new HarvestException(e.getMessage(), e);
            }
        } finally {
            SesameUtil.close(queryResult);
            SesameUtil.close(localConn);
            SesameUtil.close(endpointConn);
        }
    }

    /**
     *
     * @return
     */
    private EndpointHttpClient prepareEndpointHttpClient() {

        HttpConnectionManagerParams managerParams = new HttpConnectionManagerParams();
        managerParams.setDefaultMaxConnectionsPerHost(20);
        managerParams.setStaleCheckingEnabled(false);

        int httpTimeout = GeneralConfig.getIntProperty(GeneralConfig.HARVESTER_HTTP_TIMEOUT, getTimeout());
        managerParams.setConnectionTimeout(httpTimeout);
        managerParams.setSoTimeout(httpTimeout);

        HttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
        manager.setParams(managerParams);

        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setParameter("http.useragent", URLUtil.userAgentHeader());
        clientParams.setParameter("http.protocol.max-redirects", MAX_REDIRECTIONS);

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("Connection", "close");
        clientParams.setParameter(SPARQLConnection.ADDITIONAL_HEADER_NAME, headers);

        return new EndpointHttpClient(clientParams, manager);
    }

    /**
     * Harvests external source.
     *
     * @throws HarvestException
     *             if harvest fails
     */
    private void doUrlHarvest() throws HarvestException {

        String initialContextUrl = getContextUrl();
        HttpURLConnection urlConn = null;
        httpResponseCode = NO_RESPONSE;
        String responseMessage = null;
        int noOfRedirections = 0;

        try {
            String connectUrl = getContextUrl();
            do {
                String message = "Opening URL connection";
                if (!connectUrl.equals(getContextUrl())) {
                    message = message + " to " + connectUrl;
                }
                LOGGER.debug(loggerMsg(message));

                urlConn = openUrlConnection(connectUrl);

                try {
                    httpResponseCode = urlConn.getResponseCode();
                    responseMessage = urlConn.getResponseMessage();
                } catch (IOException ioe) {
                    // an error when connecting to server is considered a temporary error-
                    // don't throw it, but log in the database and exit
                    LOGGER.debug("Error when connecting to server: " + ioe);
                    finishWithError(NO_RESPONSE, null, ioe);
                    return;
                }

                // Throws exception when the content-length indicated in HTTP response is more than the maximum allowed.
                validateContentLength(urlConn);

                // Handle redirection.
                if (isRedirect(httpResponseCode)) {

                    noOfRedirections++;

                    // if number of redirections more than maximum allowed, throw exception
                    if (noOfRedirections > MAX_REDIRECTIONS) {
                        throw new TooManyRedirectionsException("Too many redirections, originally started from "
                                + initialContextUrl);
                    }

                    // get redirected-to-url, throw exception if it's missing
                    String redirectedToUrl = getRedirectUrl(urlConn);
                    redirectedUrls.add(connectUrl);
                    redirectedHarvestSources.add(getContextSourceDTO());

                    if (StringUtils.isBlank(redirectedToUrl)) {
                        throw new NoRedirectLocationException("Redirection response code wihtout \"Location\" header!");
                    }
                    LOGGER.debug(loggerMsg(connectUrl + " redirects to " + redirectedToUrl));

                    // treat this as a redirection only if the context URL and the redirected-to-URL
                    // are not essentially the same
                    if (!URLUtil.equalUrls(getContextUrl(), redirectedToUrl)) {

                        finishRedirectedHarvest(redirectedToUrl, httpResponseCode);

                        LOGGER.debug(loggerMsg("Redirection details saved"));
                        startWithNewContext(redirectedToUrl);
                    } else {
                        LOGGER.debug(loggerMsg("Ignoring this redirection, as it is essentially to the same URL"));
                    }

                    connectUrl = redirectedToUrl;
                    // Close redirected URL connection
                    URLUtil.disconnect(urlConn);
                }
            } while (isRedirect(httpResponseCode));

            // if URL connection returned no errors and its content has been modified since last harvest,
            // proceed to downloading
            if (!isError(httpResponseCode) && !isNotModified(httpResponseCode)) {

                int noOfTriples = downloadAndProcessContent(urlConn);
                setStoredTriplesCount(noOfTriples);
                LOGGER.debug(loggerMsg(noOfTriples + " triples loaded"));
                finishWithOK(urlConn, noOfTriples);

            } else if (isNotModified(httpResponseCode)) {
                LOGGER.debug(loggerMsg("Source not modified since last harvest"));
                finishWithNotModified(urlConn, 0);

            } else if (isError(httpResponseCode)) {
                LOGGER.debug(loggerMsg("Server returned error code " + httpResponseCode));
                finishWithError(httpResponseCode, responseMessage, null);
            }
        } catch (Exception e) {

            LOGGER.debug(loggerMsg("Exception occurred (will be further logged by caller below): " + e.toString()));

            // check what caused the DAOException - fatal flag is set to true
            checkAndSetFatalExceptionFlag(e.getCause());

            try {
                finishWithError(httpResponseCode, responseMessage, e);
            } catch (RuntimeException finishingException) {
                LOGGER.error("Error when finishing up: ", finishingException);
            }
            if (e instanceof HarvestException) {
                throw (HarvestException) e;
            } else {
                throw new HarvestException(e.getMessage(), e);
            }
        } finally {
            URLUtil.disconnect(urlConn);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.harvest.BaseHarvest#doHarvest()
     */
    @Override
    protected void doHarvest() throws HarvestException {

        // check if the file is on the local filestore folder
        boolean isLocalFile = FileStore.isFileStoreUri(getContextUrl());

        if (isLocalFile) {
            doLocalFileHarvest();
        } else if (getContextSourceDTO().isSparqlEndpoint()) {
            doEndpointHarvest();
        } else {
            doUrlHarvest();
        }
    }

    /**
     *
     * @param urlConn
     * @param noOfTriples
     */
    private void finishWithOK(HttpURLConnection urlConn, int noOfTriples) {

        // update context source DTO with the results of this harvest
        getContextSourceDTO().setStatements(noOfTriples);
        getContextSourceDTO().setLastHarvest(new Date());
        getContextSourceDTO().setLastHarvestFailed(false);
        getContextSourceDTO().setPermanentError(false);
        getContextSourceDTO().setCountUnavail(0);

        // add source metadata resulting from this harvest
        addSourceMetadata(urlConn, 0, null, null);

        // since the harvest went OK, clean previously harvested metadata of this source
        setCleanAllPreviousSourceMetadata(true);
    }

    /**
     *
     * @param urlConn
     * @param noOfTriples
     */
    private void finishWithNotModified(HttpURLConnection urlConn, int noOfTriples) {

        addHarvestMessage("Source not modified since last harvest", HarvestMessageType.INFO);
        isSourceAvailable = true;

        // update context source DTO (since the server returned source-not-modified,
        // the number of harvested statements stays as it already is, i.e. we're not setting it)
        getContextSourceDTO().setLastHarvest(new Date());
        getContextSourceDTO().setLastHarvestFailed(false);
        getContextSourceDTO().setPermanentError(false);
        getContextSourceDTO().setCountUnavail(0);

        // since the server returned source-not-modified, we're keeping the old metadata,
        // but still updating the cr:lastRefreshed
        setCleanAllPreviousSourceMetadata(false);
        addSourceMetadata(Predicates.CR_LAST_REFRESHED, ObjectDTO.createLiteral(formatDate(new Date()), XMLSchema.DATETIME));
    }

    /**
     * @param responseCode
     * @param exception
     */
    private void finishWithError(int responseCode, String responseMessage, Exception exception) {

        // source is unavailable if there was no response, or it was an error code, or the exception cause is RDFParseException
        boolean isRDFParseException = exception != null && (exception.getCause() instanceof RDFParseException);
        boolean sourceNotAvailable = responseCode == NO_RESPONSE || isError(responseCode) || isRDFParseException;

        // if source was not available, the new unavailability-count is increased by one, otherwise reset
        int countUnavail = sourceNotAvailable ? getContextSourceDTO().getCountUnavail() + 1 : 0;

        // if permanent error, the last harvest date will be set to now, otherwise special logic used
        Date now = new Date();
        Date lastHarvest = isPermanentError(responseCode) ? now : temporaryErrorLastHarvest(now);

        // if permanent error, clean previously harvested metadata of this source,
        // and if not a priority source, clean all previously harvested content of this source too
        int noOfStatements = getContextSourceDTO().getStatements();
        if (isPermanentError(responseCode)) {

            setCleanAllPreviousSourceMetadata(true);
            if (!getContextSourceDTO().isPrioritySource()) {
                try {
                    getHarvestSourceDAO().clearGraph(getContextUrl());
                    noOfStatements = 0;
                } catch (DAOException e) {
                    LOGGER.error("Failed to delete previous content after permanent error", e);
                }
            }
        }

        // update context source DTO with the results of this harvest
        getContextSourceDTO().setStatements(noOfStatements);
        getContextSourceDTO().setLastHarvest(lastHarvest);
        getContextSourceDTO().setLastHarvestFailed(true);
        getContextSourceDTO().setPermanentError(isPermanentError(responseCode));
        getContextSourceDTO().setCountUnavail(countUnavail);

        // save same error parameters to parent sources where this source was redirected from
        handleRedirectedHarvestDTOs(lastHarvest, responseCode, sourceNotAvailable);

        // add harvest message about the given exception if it's not null
        if (exception != null) {
            String message = exception.getMessage() == null ? exception.toString() : exception.getMessage();
            String stackTrace = Util.getStackTrace(exception);
            stackTrace = StringUtils.replace(stackTrace, "\r", "");
            addHarvestMessage(message, HarvestMessageType.ERROR, stackTrace);
        }

        // add harvest message about the given response code, if it's an error code (because it could also be
        // a "no response" code, meaning an exception was raised before the response code could be obtained)
        if (isError(responseCode)) {
            if (responseMessage == null) {
                responseMessage = "";
            }
            addHarvestMessage("Server returned error: " + responseMessage + " (HTTP response code: " + responseCode + ")",
                    HarvestMessageType.ERROR);
        }

        // add source metadata resulting from this harvest
        addSourceMetadata(null, responseCode, responseMessage, exception);
    }

    /**
     * Marks redirected sources with error markers.
     *
     * @param lastHarvest
     *            last harvest time
     * @param responseCode
     *            http response code
     * @param sourceNotAvailable
     *            shows if source was available
     */
    private void handleRedirectedHarvestDTOs(Date lastHarvest, int responseCode, boolean sourceNotAvailable) {
        for (HarvestSourceDTO harvestSourceDTO : redirectedHarvestSources) {
            setErrorsToRedirectedHarvestDTO(harvestSourceDTO, lastHarvest, responseCode, sourceNotAvailable);
        }
    }

    /**
     * Stores error in HarvestSource DTO.
     *
     * @param harvestSourceDTO
     *            / source DTO object
     */
    private void setErrorsToRedirectedHarvestDTO(HarvestSourceDTO harvestSourceDTO, Date lastHarvest, int responseCode,
            boolean sourceNotAvailable) {

        // if source was not available, the new unavailability-count is increased by one, otherwise reset
        int countUnavail = sourceNotAvailable ? harvestSourceDTO.getCountUnavail() + 1 : 0;

        harvestSourceDTO.setStatements(0);
        harvestSourceDTO.setLastHarvest(lastHarvest);
        harvestSourceDTO.setLastHarvestFailed(true);
        harvestSourceDTO.setPermanentError(isPermanentError(responseCode));
        harvestSourceDTO.setCountUnavail(countUnavail);

    }

    /**
     * Returns the {@link Date} to which the source's last harvest time should be set in case of temporary harvest error. It should
     * be set to "now - harvest_interval + max(harvest_interval*0,1, 120 min)". The "now" is given as method input.
     *
     * @param now As indicated above.
     * @return The calculated last harvest date as indicated above.
     */
    private Date temporaryErrorLastHarvest(Date now) {

        // The source's harvesting interval in minutes.
        int intervalMinutes = getContextSourceDTO().getIntervalMinutes();

        // The new last harvest will be "now - interval + interval*0,1", but at least two hours (i.e. 120 minutes).
        // So here we calculate the value that we shall add to the "now - interval".
        int increaseMinutes = Math.max((intervalMinutes * 10) / 100, 120);

        // Get calendar instance, set it to now.
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);

        // Subtract interval and add the above-calculated increase.
        cal.add(Calendar.MINUTE, -1 * intervalMinutes);
        cal.add(Calendar.MINUTE, increaseMinutes);

        // Just make it 100% sure that the calculated time will not be after now, though the business logic should exclude it.
        Date resultingTime = cal.getTime();
        if (resultingTime.after(now)) {
            resultingTime = now;
        }

        return resultingTime;
    }

    /**
     *
     * @param urlConn
     * @param responseCode
     * @param exception
     * @throws DAOException
     */
    private void addSourceMetadata(HttpURLConnection urlConn, int responseCode, String responseMessage, Exception exception) {

        String firstSeen = formatDate(getContextSourceDTO().getTimeCreated());
        String lastRefreshed = formatDate(new Date());

        addSourceMetadata(Predicates.CR_FIRST_SEEN, ObjectDTO.createLiteral(firstSeen, XMLSchema.DATETIME));
        addSourceMetadata(Predicates.CR_LAST_REFRESHED, ObjectDTO.createLiteral(lastRefreshed, XMLSchema.DATETIME));

        if (isError(responseCode)) {
            if (responseMessage == null) {
                responseMessage = "";
            }
            addSourceMetadata(
                    Predicates.CR_ERROR_MESSAGE,
                    ObjectDTO.createLiteral("Server returned error: " + responseMessage + " (HTTP response code: " + responseCode
                            + ")"));
        } else if (exception != null) {
            addSourceMetadata(Predicates.CR_ERROR_MESSAGE, ObjectDTO.createLiteral(exception.toString()));
        }

        if (urlConn != null) {

            // content type
            String contentType = getSourceContentType(urlConn);
            if (!StringUtils.isBlank(contentType)) {

                addSourceMetadata(Predicates.CR_MEDIA_TYPE, ObjectDTO.createLiteral(contentType));

                // if content type is not "application/rdf+xml", generate rdf:type from the
                // DublinCore type mappings
                if (!contentType.toLowerCase().startsWith("application/rdf+xml")) {

                    String rdfType = MediaTypeToDcmiTypeConverter.getDcmiTypeFor(contentType);
                    if (rdfType != null) {
                        addSourceMetadata(Predicates.RDF_TYPE, ObjectDTO.createResource(rdfType));
                    }
                }
            }

            // content's last modification
            long contentLastModified = urlConn.getLastModified();
            if (contentLastModified > 0) {
                String lastModifString = formatDate(new Date(contentLastModified));
                addSourceMetadata(Predicates.CR_LAST_MODIFIED, ObjectDTO.createLiteral(lastModifString, XMLSchema.DATETIME));
            }

            // content size
            int contentLength = urlConn.getContentLength();
            if (contentLength >= 0) {
                addSourceMetadata(Predicates.CR_BYTE_SIZE, ObjectDTO.createLiteral(String.valueOf(contentLength)));
            }
        }

    }

    /**
     * Download and process content. If response content type is one of RDF, then proceed straight to loading. Otherwise process the
     * file to see if it's zipped, it's an XML with RDF conversion, or actually an RDF file.
     *
     * @param urlConn
     *            - connection to the remote source.
     * @return number of triples harvested.
     *
     * @throws IOException
     * @throws DAOException
     * @throws SAXException
     * @throws RDFParseException
     *             if RDF parsing fails while analyzing file with unknown format
     * @throws RDFHandlerException
     *             if RDF parsing fails while analyzing file with unknown format
     */
    private int downloadAndProcessContent(HttpURLConnection urlConn) throws IOException, DAOException, SAXException,
            RDFHandlerException, RDFParseException {

        File downloadedFile = null;
        try {
            downloadedFile = downloadFile(urlConn);

            // If the downloaded file can be loaded straight away as it is, then proceed to loading straight away.
            // Otherwise try to process the file into RDF format and *then* proceed to loading.

            ContentLoader contentLoader = createContentLoader(urlConn);

            if (contentLoader != null) {
                contentLoader.setTimeout(getTimeout());
                LOGGER.debug(loggerMsg("Downloaded file is in RDF or web feed format"));
                return loadFile(downloadedFile, contentLoader);
            } else {
                LOGGER.debug(loggerMsg("Downloaded file is not in RDF or web feed format, processing the file further"));
                File processedFile = null;
                try {
                    // The file could be a zipped RDF, an XML with an RDF conversion, N3, or actually a completely valid RDF
                    // that simply wasn't declared in the server-returned content type.
                    FileToRdfProcessor fileProcessor = new FileToRdfProcessor(downloadedFile, getContextUrl());
                    processedFile = fileProcessor.process();
                    if (processedFile != null && fileProcessor.getRdfFormat() != null) {
                        LOGGER.debug(loggerMsg("File processed into RDF format"));
                        ContentLoader rdfLoader = new RDFFormatLoader(fileProcessor.getRdfFormat());
                        rdfLoader.setTimeout(getTimeout());
                        return loadFile(processedFile, rdfLoader);
                    } else {
                        LOGGER.debug(loggerMsg("File couldn't be processed into RDF format"));
                        return 0;
                    }
                } finally {
                    FileDeletionJob.register(processedFile);
                }
            }
        } finally {
            FileDeletionJob.register(downloadedFile);
        }
    }

    /**
     *
     * @param redirectedToUrl
     * @param responseCode
     *            HTTP Code from the redirected URL
     * @throws DAOException
     */
    private void finishRedirectedHarvest(String redirectedToUrl, int responseCode) throws DAOException {

        Date redirectionSeen = new Date();

        // update the context source's last-harvest and number of statements
        getContextSourceDTO().setLastHarvest(redirectionSeen);
        getContextSourceDTO().setLastHarvestFailed(false);
        getContextSourceDTO().setStatements(0);
        getContextSourceDTO().setLastHarvestId(getHarvestId());
        getHarvestSourceDAO().updateSourceHarvestFinished(getContextSourceDTO());

        // update current harvest to finished, set its count of harvested triples to 0
        getHarvestDAO().updateFinishedHarvest(getHarvestId(), 0, responseCode);

        // insert redirection message to the current harvest
        String message = getContextUrl() + "  redirects to  " + redirectedToUrl;
        HarvestMessageDTO messageDTO = HarvestMessageDTO.create(message, HarvestMessageType.INFO, null);
        messageDTO.setHarvestId(getHarvestId());
        getHarvestMessageDAO().insertHarvestMessage(messageDTO);

        // clear context source's metadata, save new metadata about redirection
        getHarvestSourceDAO().deleteSubjectTriplesInSource(getContextUrl(), GeneralConfig.HARVESTER_URI);
        SubjectDTO subjectDTO = createRedirectionMetadata(getContextSourceDTO(), redirectionSeen, redirectedToUrl);
        getHelperDAO().addTriples(subjectDTO);

        // if redirected-to source not existing, create it by copying the context source
        HarvestSourceDTO redirectedToSourceDTO = getHarvestSource(redirectedToUrl);
        if (redirectedToSourceDTO == null) {

            LOGGER.debug(loggerMsg("Creating harvest source for " + redirectedToUrl));

            // clone the redirected-to source from the context source
            // (no null-checking, i.e. assuming the context source already exists)
            redirectedToSourceDTO = getContextSourceDTO().clone();

            // set the redirected-to source's url, creation time and last harvest time
            redirectedToSourceDTO.setUrl(redirectedToUrl);
            redirectedToSourceDTO.setUrlHash(Long.valueOf(Hashes.spoHash(redirectedToUrl)));
            redirectedToSourceDTO.setTimeCreated(redirectionSeen);

            // persist the redirected-to source
            getHarvestSourceDAO().addSource(redirectedToSourceDTO);
        }

        // delete old harvests history
        LOGGER.debug(loggerMsg("Deleting old redirected harvests history"));
        getHarvestDAO().deleteOldHarvests(getContextSourceDTO().getSourceId());
    }

    /**
     *
     * @param sourceDTO
     * @param redirectionSeen
     * @param redirectedToUrl
     * @return
     */
    private SubjectDTO createRedirectionMetadata(HarvestSourceDTO sourceDTO, Date redirectionSeen, String redirectedToUrl) {

        String firstSeen = formatDate(sourceDTO.getTimeCreated());
        String lastRefreshed = formatDate(redirectionSeen);

        SubjectDTO subjectDTO = new SubjectDTO(sourceDTO.getUrl(), false);

        String harvesterContextUri = eionet.cr.config.GeneralConfig.HARVESTER_URI;
        ObjectDTO object = ObjectDTO.createLiteral(firstSeen, XMLSchema.DATETIME);
        object.setSourceUri(harvesterContextUri);
        subjectDTO.addObject(Predicates.CR_FIRST_SEEN, object);

        object = ObjectDTO.createLiteral(lastRefreshed, XMLSchema.DATETIME);
        object.setSourceUri(harvesterContextUri);
        subjectDTO.addObject(Predicates.CR_LAST_REFRESHED, object);

        object = ObjectDTO.createResource(redirectedToUrl);
        object.setSourceUri(harvesterContextUri);
        subjectDTO.addObject(Predicates.CR_REDIRECTED_TO, object);

        return subjectDTO;
    }

    /**
     * Download file from remote source to a temporary file locally. Side effect: adds the file size to the metadata to save in the
     * harvester context.
     *
     * @param urlConn
     *            - connection to the remote source.
     * @return object representing the temporary file.
     * @throws IOException
     *             if the file is not downloadable.
     */
    private File downloadFile(HttpURLConnection urlConn) throws IOException {

        LOGGER.debug(loggerMsg("Downloading file"));

        InputStream inputStream = null;
        OutputStream outputStream = null;
        File file = TempFilePathGenerator.generate();
        try {
            outputStream = new FileOutputStream(file);
            inputStream = urlConn.getInputStream();
            isSourceAvailable = true;
            int bytesCopied = IOUtils.copy(inputStream, outputStream);

            // add number of bytes to source metadata, unless it's already there
            addSourceMetadata(Predicates.CR_BYTE_SIZE, ObjectDTO.createLiteral(String.valueOf(bytesCopied)));

        } catch (IOException e) {
            FileDeletionJob.register(file);
            throw e;
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
            URLUtil.disconnect(urlConn);
        }

        return file;
    }

    /**
     *
     * @param connectUrl
     * @return
     * @throws IOException
     * @throws DAOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private HttpURLConnection openUrlConnection(String connectUrl) throws IOException, DAOException, SAXException,
            ParserConfigurationException {

        String sanitizedUrl = StringUtils.substringBefore(connectUrl, "#");
        sanitizedUrl = StringUtils.replace(sanitizedUrl, " ", "%20");

        HttpURLConnection connection = (HttpURLConnection) new URL(sanitizedUrl).openConnection();
        connection.setRequestProperty("Accept", ACCEPT_HEADER);
        connection.setRequestProperty("User-Agent", URLUtil.userAgentHeader());
        connection.setRequestProperty("Connection", "close");
        connection.setInstanceFollowRedirects(false);

        // Set the timeout both for establishing the connection, and reading from it once established.
        int httpTimeout = GeneralConfig.getIntProperty(GeneralConfig.HARVESTER_HTTP_TIMEOUT, getTimeout());
        connection.setConnectTimeout(httpTimeout);
        connection.setReadTimeout(httpTimeout);

        // Use "If-Modified-Since" header, if this is not an on-demand harvest
        if (!isOnDemandHarvest) {

            // "If-Modified-Since" will be compared to this URL's last harvest
            Date lastHarvestDate = getContextSourceDTO().getLastHarvest();
            long lastHarvest = lastHarvestDate == null ? 0L : lastHarvestDate.getTime();
            if (lastHarvest > 0) {

                // Check if this URL has a conversion stylesheet, and if the latter has been modified since last harvest.
                String conversionStylesheetUrl = getConversionStylesheetUrl(sanitizedUrl);
                boolean hasConversion = !StringUtils.isBlank(conversionStylesheetUrl);
                boolean hasModifiedConversion = hasConversion && URLUtil.isModifiedSince(conversionStylesheetUrl, lastHarvest);

                // Check if post-harvest scripts are updated
                boolean scriptsModified =
                        DAOFactory.get().getDao(PostHarvestScriptDAO.class)
                                .isScriptsModified(lastHarvestDate, getContextSourceDTO().getUrl());

                // "If-Modified-Since" should only be set if there is no modified conversion or post-harvest scripts for this URL.
                // Because if there is a conversion stylesheet or post-harvest scripts, and any of them has been modified since last
                // harvest, we surely want to get the content again and run the conversion or script on the content, regardless of
                // when the content itself was last modified.
                if (!hasModifiedConversion && !scriptsModified) {
                    LOGGER.debug(loggerMsg("Using if-modified-since, compared to last harvest " + formatDate(lastHarvestDate)));
                    connection.setIfModifiedSince(lastHarvest);
                }
            }
        }

        return connection;
    }

    /**
     *
     * @param connection
     * @return
     * @throws MalformedURLException
     */
    private String getRedirectUrl(HttpURLConnection connection) throws MalformedURLException {

        String location = connection.getHeaderField("Location");
        if (location != null) {
            try {
                // If location does not seem to be an absolute URI, consider it relative to the
                // URL of this URL connection.
                if (!(new URI(location).isAbsolute())) {
                    location = new URL(connection.getURL(), location).toString();
                }
            } catch (URISyntaxException e) {
                // Ignoring on purpose.
            }

            // we want to avoid fragment parts in CR harvest source URLs
            location = StringUtils.substringBefore(location, "#");
        }

        return location;
    }

    /**
     *
     * @param harvestSourceUrl
     * @return
     * @throws DAOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private String getConversionStylesheetUrl(String harvestSourceUrl) throws DAOException, IOException, SAXException,
            ParserConfigurationException {

        String result = null;

        String schemaUri = getHelperDAO().getSubjectSchemaUri(harvestSourceUrl);
        if (!StringUtils.isBlank(schemaUri)) {

            // see if schema has RDF conversion
            ConversionsParser convParser = ConversionsParser.parseForSchema(schemaUri);
            if (!StringUtils.isBlank(convParser.getRdfConversionId())) {

                result = convParser.getRdfConversionXslFileName();
            }
        }

        return result;
    }

    /**
     * Returns RDF format from url connection.
     *
     * @param contentType
     * @return
     */
    private RDFFormat getRdfFormat(HttpURLConnection urlConn) {
        String contentType = getSourceContentType(urlConn);

        if (contentType == null) {
            return null;
        }

        if (contentType.equals(CONTENT_TYPE_TEXT)) {
            String path = urlConn.getURL().getPath();
            String[] arr = path.split("\\.");
            if (arr.length > 0) {
                String ext = arr[arr.length - 1];
                if (StringUtils.isNotEmpty(ext)) {
                    if (ext.equalsIgnoreCase(EXT_TTL)) {
                        return RDFFormat.TURTLE;
                    }
                    if (ext.equalsIgnoreCase(EXT_N3)) {
                        return RDFFormat.N3;
                    }
                }
            }
        }

        return RDFMediaTypes.toRdfFormat(contentType);
    }

    /**
     *
     * @param urlConn
     * @return
     */
    private String getSourceContentType(HttpURLConnection urlConn) {

        // prefer content type from context source DTO over the one from URL connection
        String contentType = getContextSourceDTO().getMediaType();
        if (StringUtils.isBlank(contentType)) {
            contentType = urlConn.getContentType();
        }
        return contentType;
    }

    /**
     *
     * @param urlConn
     * @return
     */
    private ContentLoader createContentLoader(HttpURLConnection urlConn) {

        RDFFormat rdfFormat = getRdfFormat(urlConn);
        if (rdfFormat != null) {
            return new RDFFormatLoader(rdfFormat);
        }

        String contentType = getSourceContentType(urlConn);
        if (StringUtils.isBlank(contentType)) {
            return null;
        } else if (contentType.startsWith("application/rss+xml") || contentType.startsWith("application/atom+xml")) {
            return new FeedFormatLoader();
        } else {
            return null;
        }
    }

    /**
     *
     * @param urlConn
     * @throws ContentTooLongException
     */
    private void validateContentLength(HttpURLConnection urlConn) throws ContentTooLongException {

        int maxLengthAllowed = NumberUtils.toInt(GeneralConfig.getProperty(GeneralConfig.HARVESTER_MAX_CONTENT_LENGTH));
        if (maxLengthAllowed > 0) {
            int contentLength = NumberUtils.toInt(urlConn.getHeaderField("Content-Length"));
            if (contentLength > maxLengthAllowed) {
                throw new ContentTooLongException(contentLength + " is more than the allowed maximum " + maxLengthAllowed);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.harvest.BaseHarvest#getHarvestType()
     */
    @Override
    protected String getHarvestType() {

        return HarvestConstants.TYPE_PULL;
    }

    /**
     * @return the isSourceAvailable
     */
    protected boolean isSourceAvailable() {
        return isSourceAvailable;
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.harvest.BaseHarvest#isSendNotifications()
     */
    @Override
    protected boolean isSendNotifications() {

        // Send notification only when this is not an on-demand harvest and the source is priority or a fatal error occurred.
        // P.S. The harvest's timeout is also considered a fatal error.
        return !isOnDemandHarvest && (getContextSourceDTO().isPrioritySource() || isFatalErrorOccured);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.harvest.BaseHarvest#isBeingHarvested(java.lang.String)
     */
    @Override
    public boolean isBeingHarvested(String url) {

        boolean isBeingHarvested = super.isBeingHarvested(url);
        return isBeingHarvested ? isBeingHarvested : redirectedUrls.contains(url);
    }

    /*
     * (non-Javadoc)
     *
     * @see eionet.cr.harvest.BaseHarvest#afterFinish()
     */
    @Override
    protected void afterFinish() {

        // Execute a background thread that will attempt to clear the graph of all redirected sources.
        (new Thread() {
            @Override
            public void run() {
                for (String redirectedUrl : PullHarvest.this.redirectedUrls) {

                    try {
                        PullHarvest.LOGGER.debug("Clearing the graph of redirected source " + redirectedUrl);
                        DAOFactory.get().getDao(HarvestSourceDAO.class).clearGraph(redirectedUrl);
                    } catch (DAOException e) {
                        PullHarvest.LOGGER.error("Failed to clear the graph of redirected source " + redirectedUrl, e);
                    }
                }
            }
        }).start();
    }
}
