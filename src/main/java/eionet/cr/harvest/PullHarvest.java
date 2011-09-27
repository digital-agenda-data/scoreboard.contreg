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
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.xml.sax.SAXException;

import eionet.cr.common.Predicates;
import eionet.cr.common.TempFilePathGenerator;
import eionet.cr.config.GeneralConfig;
import eionet.cr.dao.DAOException;
import eionet.cr.dto.HarvestMessageDTO;
import eionet.cr.dto.HarvestSourceDTO;
import eionet.cr.dto.ObjectDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.harvest.util.HarvestMessageType;
import eionet.cr.harvest.util.MediaTypeToDcmiTypeConverter;
import eionet.cr.harvest.util.RDFMediaTypes;
import eionet.cr.util.FileDeletionJob;
import eionet.cr.util.Hashes;
import eionet.cr.util.URLUtil;
import eionet.cr.util.Util;
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
    private static final String ACCEPT_HEADER = StringUtils.join(RDFMediaTypes.collection(), ',') + ",text/xml,*/*;q=0.6";

    /** */
    private boolean isSourceAvailable;

    /** */
    private boolean isOnDemandHarvest;

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
     * @see eionet.cr.harvest.temp.BaseHarvest#doHarvest()
     */
    @Override
    protected void doHarvest() throws HarvestException {

        String initialContextUrl = getContextUrl();
        HttpURLConnection urlConn = null;
        int responseCode = NO_RESPONSE;
        String responseMessage = null;
        int noOfRedirections = 0;

        try {
            String connectUrl = getContextUrl();
            do {
                String message = "Opening URL connection";
                if (!connectUrl.equals(getContextUrl())){
                    message = message + " to " + connectUrl;
                }
                LOGGER.debug(loggerMsg(message));

                urlConn = openUrlConnection(connectUrl);
                try {
                    responseCode = urlConn.getResponseCode();
                    responseMessage = urlConn.getResponseMessage();
                } catch (IOException ioe) {
                    // an error when connecting to server is considered a temporary error-
                    // don't throw it, but log in the database and exit
                    LOGGER.debug("Error when connecting to server: " + ioe);
                    finishWithError(NO_RESPONSE, null, ioe);
                    return;
                }

                if (isRedirect(responseCode)) {

                    noOfRedirections++;

                    // if number of redirections more than maximum allowed, throw exception
                    if (noOfRedirections > MAX_REDIRECTIONS) {
                        throw new TooManyRedirectionsException("Too many redirections, originally started from "
                                + initialContextUrl);
                    }

                    // get redirected-to-url, throw exception if it's missing
                    String redirectedToUrl = getRedirectUrl(urlConn);
                    addRedirectionUrl(connectUrl);
                    if (StringUtils.isBlank(redirectedToUrl)) {
                        throw new NoRedirectLocationException("Redirection response code wihtout \"Location\" header!");
                    }
                    LOGGER.debug(loggerMsg(connectUrl + " redirects to " + redirectedToUrl));

                    // treat this as a redirection only if the context URL and the redirected-to-URL
                    // are not essentially the same
                    if (URLUtil.equalUrls(getContextUrl(), redirectedToUrl)==false){

                        finishRedirectedHarvest(redirectedToUrl);
                        LOGGER.debug(loggerMsg("Redirection details saved"));
                        startWithNewContext(redirectedToUrl);
                    }
                    else{
                        LOGGER.debug(loggerMsg("Ignoring this redirection, as it is essentially to the same URL"));
                    }

                    connectUrl = redirectedToUrl;
                }
            } while (isRedirect(responseCode));

            // if URL connection returned no errors and its content has been modified since last harvest,
            // proceed to downloading
            if (!isError(responseCode) && !isNotModified(responseCode)) {

                int noOfTriples = downloadAndProcessContent(urlConn);
                setStoredTriplesCount(noOfTriples);
                LOGGER.debug(loggerMsg(noOfTriples + " triples loaded"));
                finishWithOK(urlConn, noOfTriples);

            } else if (isNotModified(responseCode)) {
                LOGGER.debug(loggerMsg("Source not modified since last harvest"));
                finishWithNotModified(urlConn, 0);

            } else if (isError(responseCode)) {
                LOGGER.debug(loggerMsg("Server returned error code " + responseCode));
                finishWithError(responseCode, responseMessage, null);
            }
        } catch (Exception e) {

            LOGGER.debug(loggerMsg("Exception occurred (will be further logged by caller below): " + e.toString()));
            try {
                finishWithError(responseCode, responseMessage, e);
            } catch (RuntimeException finishingException) {
                LOGGER.error("Error when finishing up: ", finishingException);
            }
            if (e instanceof HarvestException) {
                throw (HarvestException) e;
            } else {
                throw new HarvestException(e.getMessage(), e);
            }
        }
        finally{
            URLUtil.disconnect(urlConn);
        }
        clearRedirections();
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
        finishWithOK(urlConn, 0);
    }

    /**
     * @param responseCode
     * @param exception
     */
    private void finishWithError(int responseCode, String responseMessage, Exception exception) {

        // source is unavailable if there was no response, or it was an error code, or the exception cause is RDFParseException
        boolean isRDFParseException = exception!=null && (exception.getCause() instanceof  RDFParseException);
        boolean sourceNotAvailable = responseCode == NO_RESPONSE || isError(responseCode) || isRDFParseException;

        // if source was not available, the new unavailability-count is increased by one, otherwise reset
        int countUnavail = sourceNotAvailable ? getContextSourceDTO().getCountUnavail() + 1 : 0;

        // if permanent error, the last harvest date will be set to now, otherwise special logic used
        Date lastHarvest = isPermanentError(responseCode) ? new Date() : temporaryErrorLastHarvest(getContextSourceDTO());

        // update context source DTO with the results of this harvest
        getContextSourceDTO().setStatements(0);
        getContextSourceDTO().setLastHarvest(lastHarvest);
        getContextSourceDTO().setLastHarvestFailed(true);
        getContextSourceDTO().setPermanentError(isPermanentError(responseCode));
        getContextSourceDTO().setCountUnavail(countUnavail);

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

        // if permanent error, clean previously harvested metadata of this source,
        // and also clean all previously harvested content of this source
        if (isPermanentError(responseCode)) {
            setCleanAllPreviousSourceMetadata(true);
            try {
                getHarvestSourceDAO().clearGraph(getContextUrl());
            } catch (DAOException e) {
                LOGGER.error("Failed to delete previous content after permanent error", e);
            }
        }
    }

    /**
     * @param contextSourceDTO
     * @return
     */
    private Date temporaryErrorLastHarvest(HarvestSourceDTO contextSourceDTO) {

        Date lastHarvest = getContextSourceDTO().getLastHarvest();
        if (lastHarvest == null) {
            return new Date();
        }

        int intervalMinutes = getContextSourceDTO().getIntervalMinutes();
        int increase = Math.max((intervalMinutes * 10) / 100, 120) * 60 * 1000;

        return new Date(lastHarvest.getTime() + increase);
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
            addSourceMetadata(Predicates.CR_ERROR_MESSAGE,
                    ObjectDTO.createLiteral("Server returned error: " + responseMessage + " (HTTP response code: "
                            + responseCode + ")"));
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
     * @param urlConn
     * @return
     * @throws IOException
     * @throws OpenRDFException
     * @throws SAXException
     */
    private int downloadAndProcessContent(HttpURLConnection urlConn) throws IOException, OpenRDFException, SAXException {

        File downloadedFile = null;
        try {
            downloadedFile = downloadFile(urlConn);

            // if response content type is one of RDF, then proceed straight to loading,
            // otherwise process the file to see if it's zipped, or it's an XML with RDF conversion,
            // or actually an RDF file

            RDFFormat rdfFormat = contentTypeToRdfFormat(getSourceContentType(urlConn));
            if (rdfFormat!=null) {
                return loadFile(downloadedFile, rdfFormat);
            } else {
                File processedFile = null;
                try {
                    LOGGER.debug(loggerMsg("Server-returned content type is not RDF, processing file further"));
                    processedFile = new FileProcessor(downloadedFile, getContextUrl()).process();
                    if (processedFile != null) {
                        LOGGER.debug(loggerMsg("File processed into loadable format"));
                        return loadFile(processedFile, null);
                    } else {
                        LOGGER.debug(loggerMsg("File couldn't be processed into loadable format"));
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
     * @throws DAOException
     */
    private void finishRedirectedHarvest(String redirectedToUrl) throws DAOException {

        Date redirectionSeen = new Date();

        // update the context source's last-harvest and number of statements
        getContextSourceDTO().setLastHarvest(redirectionSeen);
        getContextSourceDTO().setStatements(0);
        getHarvestSourceDAO().updateSourceHarvestFinished(getContextSourceDTO());

        // update current harvest to finished, set its count of harvested triples to 0
        getHarvestDAO().updateFinishedHarvest(getHarvestId(), 0);

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
        getHarvestDAO().deleteOldHarvests(getHarvestId(), PRESERVED_HARVEST_COUNT);
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
     * @param file
     * @param rdfFormat
     * @return
     * @throws OpenRDFException
     * @throws IOException
     */
    private int loadFile(File file, RDFFormat rdfFormat) throws IOException, OpenRDFException {

        LOGGER.debug(loggerMsg("Loading file into triple store"));

        int tripleCount = getHarvestSourceDAO().loadIntoRepository(file, rdfFormat, getContextUrl(), true);
        return tripleCount;
    }

    /**
     * @param urlConn
     * @return
     * @throws IOException
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
     * @param connectUrl TODO
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws DAOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private HttpURLConnection openUrlConnection(String connectUrl) throws MalformedURLException, IOException, DAOException, SAXException,
    ParserConfigurationException {

        String sanitizedUrl = StringUtils.substringBefore(connectUrl, "#");
        sanitizedUrl = StringUtils.replace(sanitizedUrl, " ", "%20");

        HttpURLConnection connection = (HttpURLConnection) new URL(sanitizedUrl).openConnection();
        connection.setRequestProperty("Accept", ACCEPT_HEADER);
        connection.setRequestProperty("User-Agent", URLUtil.userAgentHeader());
        connection.setRequestProperty("Connection", "close");
        connection.setInstanceFollowRedirects(false);

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

                // "If-Modified-Since" should only be set, if there is no modified conversion for this URL.
                // Because if there is a conversion stylesheet, and it has been modified since last harvest,
                // we surely want to get the content again, regardless of when the content itself was last modified.
                if (!hasModifiedConversion) {
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
                // if location does not seem to be an absolute URI, consider it relative to the
                // URL of this URL connection
                if (new URI(location).isAbsolute() == false) {
                    location = new URL(connection.getURL(), location).toString();
                }
            } catch (URISyntaxException e) {
                // ignoring on purpose
            }

            // we want to avoid fragment parts in CR harvest source URLs
            location = StringUtils.substringBefore(location, "#");
        }

        return location;
    }

    /**
     *
     * @param url
     * @return long
     * @throws DAOException
     */
    private long getLastHarvestTimestamp(String url) throws DAOException {

        long result = 0;

        HarvestSourceDTO dto = getHarvestSource(url);
        if (dto != null) {
            Date lastHarvestDate = dto.getLastHarvest();
            if (lastHarvestDate != null) {
                result = lastHarvestDate.getTime();
            }
        }

        return result;
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
     *
     * @param contentType
     * @return
     */
    private RDFFormat contentTypeToRdfFormat(String contentType) {

        return contentType!=null ? RDFMediaTypes.toRdfFormat(contentType) : null;
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
     * @see eionet.cr.harvest.BaseHarvest#getHarvestType()
     */
    protected String getHarvestType() {

        return HarvestConstants.TYPE_PULL;
    }

    /**
     * @return the isSourceAvailable
     */
    protected boolean isSourceAvailable() {
        return isSourceAvailable;
    }

    /**
     * @see eionet.cr.harvest.BaseHarvest#isSendNotifications()
     */
    @Override
    protected boolean isSendNotifications() {

        // notifications sent only when this is not an on-demand harvest
        return !isOnDemandHarvest;
    }

    /**
     * @param isOnDemandHarvest
     *            the isOnDemandHarvest to set
     */
    public void setOnDemandHarvest(boolean isOnDemandHarvest) {
        this.isOnDemandHarvest = isOnDemandHarvest;
    }

    /**
     * @see eionet.cr.harvest.BaseHarvest#isBeingHarvested(java.lang.String)
     */
    public boolean isBeingHarvested(String url) {
        boolean ret = false;
        if (url != null) {
            if (url.equals(getContextUrl()) || (getRedirectedHarvests() != null && getRedirectedHarvests().contains(url))) {
                ret = true;
            }
        }
        return ret;
    }
}
