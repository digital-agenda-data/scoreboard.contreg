package eionet.cr.util.odp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javanet.staxutils.IndentingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import eionet.cr.common.Namespace;
import eionet.cr.common.Predicates;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.HelperDAO;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.dao.SearchDAO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.util.URIUtil;

/**
 * Generates ODP (Open Data Portal, http://open-data.europa.eu) datasets' metadata packages from the metadata of
 * a selected set of indicators. The output generated into a given stream, and is a ZIP file consisting of one RDF/XML formatted
 * metadata file per indicator.
 *
 * @author Jaanus
 */
public class ODPDatasetsPacker {

    /** URI of the "main" dataset, as opposed to the "virtual" datasets we generate for eacg indicator. */
    private static final String MAIN_DATASET_URI =
            "http://semantic.digital-agenda-data.eu/dataset/digital-agenda-scoreboard-key-indicators";

    /** Expected charset encoding of the generated output. */
    private static final String ENCODING = "UTF-8";

    /** Default namespace of the generated RDF/XML files that will be zipped. */
    private static final Namespace DEFAULT_NAMESPACE = Namespace.ECODP;

    /** Namespaces used in the generated RDF/XML files. */
    private static final List<Namespace> NAMESPACES = buildNamespacesList();

    /** URIs of indicators for which the RDF/XML formatted metadata shall be generated. */
    private List<String> indicatorUris;

    /** List of {@link SubjectDTO} where each member represents an indicator from {@link #indicatorUris}. */
    List<SubjectDTO> indicatorSubjects;

    /** A {@link SubjectDTO} representing the "main" dataset identified by {@link #MAIN_DATASET_URI}. */
    private SubjectDTO mainDstSubject;

    /** List of URIs of all distinct reference areas used by observations in the triplestore. */
    private List<String> refAreas;

    /** A boolean indicating if {@link #prepare()} has already been called. */
    private boolean isPrepareCalled;

    /**
     * Main constructor for generating ODP dataset metadata package for the given indicators.
     *
     * @param indicatorUris The URIs of the indicators whose metadata is to be packaged.
     */
    public ODPDatasetsPacker(List<String> indicatorUris) {

        if (CollectionUtils.isEmpty(indicatorUris)) {
            throw new IllegalArgumentException("The given list of indicatior URIs must not be empty!");
        }

        this.indicatorUris = indicatorUris;
    }

    /**
     * Does preparations for the {@link #execute(OutputStream)} method, so it should be called before tha latter, otherwise the
     * latter will throw {@link IllegalStateException}.
     *
     * The reason for this method is that we can do preparations (e.g. get various stuff from database and triplestore) before
     * we start streaming the output. This is convenient for exception handling in Stripes action bean events that return a
     * streaming resolution.
     *
     * @throws DAOException If data access error occurs.
     */
    public void prepare() throws DAOException {

        isPrepareCalled = true;

        indicatorSubjects = DAOFactory.get().getDao(SearchDAO.class).getSubjectsData(indicatorUris, null);
        if (CollectionUtils.isEmpty(indicatorSubjects)) {
            throw new DAOException("Could not find any metadata about the given indicators!");
        }

        mainDstSubject = DAOFactory.get().getDao(HelperDAO.class).getSubject(MAIN_DATASET_URI);
        if (mainDstSubject == null || mainDstSubject.getPredicateCount() == 0) {
            throw new DAOException("Could not find any metadata about the main (i.e. parent) dataset!");
        }

        refAreas = DAOFactory.get().getDao(ScoreboardSparqlDAO.class).getDistinctUsedRefAreas();
    }

    /**
     * The main execution method.
     *
     * @param outputStream Output stream where the zipped file should be written into.
     *
     * @throws IOException If any sort of output stream writing error occurs.
     * @throws XMLStreamException Thrown by methods from the {@link XMLStreamWriter} that is used by called methods.
     */
    public void execute(OutputStream outputStream) throws IOException, XMLStreamException {

        if (!isPrepareCalled) {
            throw new IllegalStateException("Prepare has not been called yet!");
        }

        int i = 0;
        ZipArchiveOutputStream zipOutput = null;
        try {
            zipOutput = new ZipArchiveOutputStream(outputStream);
            for (SubjectDTO indicatorSubject : indicatorSubjects) {
                createAndWriteEntry(zipOutput, indicatorSubject, i++);
            }
        } finally {
            IOUtils.closeQuietly(zipOutput);
        }
    }

    /**
     * Creates and writes a ZIP archive entry file for the given indicator.
     *
     * @param zipOutput ZIP output where the entry goes into.
     * @param indSubject The indicator whose for whom the entry is written.
     * @param index 0-based index of the indicator (in the indicator list received from dataabse) that is being written.
     *
     * @throws IOException If any sort of output stream writing error occurs.
     * @throws XMLStreamException Thrown by methods from the {@link XMLStreamWriter} that is used by called methods.
     */
    private void createAndWriteEntry(ZipArchiveOutputStream zipOutput, SubjectDTO indSubject, int index) throws IOException,
            XMLStreamException {

        String id = indSubject.getObjectValue(Predicates.SKOS_NOTATION);
        if (StringUtils.isEmpty(id)) {
            id = URIUtil.extractURILabel(indSubject.getUri(), String.valueOf(index));
        }

        ZipArchiveEntry entry = new ZipArchiveEntry(id + ".rdf");
        zipOutput.putArchiveEntry(entry);
        writeEntry(zipOutput, indSubject, index);
        zipOutput.closeArchiveEntry();
    }

    /**
     * Writes a ZIP archive entry file for the given indicator.
     *
     * @param zipOutput ZIP output where the entry goes into.
     * @param indSubject The indicator whose for whom the entry is written.
     * @param index 0-based index of the indicator (in the indicator list received from dataabse) that is being written.
     *
     * @throws XMLStreamException Thrown by methods from the {@link XMLStreamWriter} that is used by called methods.
     */
    private void writeEntry(ZipArchiveOutputStream zipOutput, SubjectDTO indSubject, int index) throws XMLStreamException {

        // Prepare STAX indenting writer based on a Java XMLStreamWriter that is based on the given zipped output.
        XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(zipOutput, ENCODING);
        IndentingXMLStreamWriter writer = new IndentingXMLStreamWriter(xmlWriter);

        // Start the XML document
        writer.writeStartDocument(ENCODING, "1.0");

        // Register all relevant namespaces.
        registerNamespaces(writer);

        // Write root element start tag + default namespace
        writer.writeStartElement(Namespace.RDF.getUri(), "RDF");
        writer.writeDefaultNamespace(DEFAULT_NAMESPACE.getUri());

        // Write all other namespace prefixes.
        for (Namespace namespace : NAMESPACES) {
            writer.writeNamespace(namespace.getPrefix(), namespace.getUri());
        }

        // Prepare some metadata values.
        String uri = indSubject.getUri();
        String skosNotation = indSubject.getObjectValue(Predicates.SKOS_NOTATION);
        String skosPrefLabel = indSubject.getObjectValue(Predicates.SKOS_PREF_LABEL);
        String skosAltLabel = indSubject.getObjectValue(Predicates.SKOS_ALT_LABEL);

        String skosDefinition = indSubject.getObjectValue(Predicates.SKOS_DEFINITION);
        String skosNotes = indSubject.getObjectValue(Predicates.SKOS_NOTES);
        String dctDescription = skosDefinition == null ? StringUtils.EMPTY : skosDefinition;
        dctDescription = (dctDescription + " " + (skosNotes == null ? StringUtils.EMPTY : skosNotes)).trim();

        String dctIssued = mainDstSubject.getObjectValue(Predicates.DCTERMS_ISSUED);

        List<String> modifiedDates = mainDstSubject.getObjectValues(Predicates.DCTERMS_MODIFIED);
        String dctModified = StringUtils.EMPTY;
        if (CollectionUtils.isNotEmpty(modifiedDates)) {
            Collections.sort(modifiedDates);
            dctModified = modifiedDates.get(modifiedDates.size() - 1).trim();
        }

        // Start the dataset tag.
        writer.writeStartElement(Namespace.DCAT.getUri(), "Dataset");
        writer.writeAttribute(Namespace.RDF.getUri(), "about", uri);

        // Write dct:title
        writer.writeStartElement(Namespace.DCT.getUri(), "title");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters(skosPrefLabel);
        writer.writeEndElement();

        // Write dct:alternative
        writer.writeStartElement(Namespace.DCT.getUri(), "alternative");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters(skosAltLabel);
        writer.writeEndElement();

        // Write dct:description
        writer.writeStartElement(Namespace.DCT.getUri(), "description");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters(dctDescription);
        writer.writeEndElement();

        // Write dct:identifier
        writer.writeStartElement(Namespace.DCT.getUri(), "identifier");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters(skosNotation);
        writer.writeEndElement();

        // Write ecodp:interoperabilityLevel
        writer.writeStartElement(Namespace.ECODP.getUri(), "interoperabilityLevel");
        writer.writeEmptyElement(Namespace.SKOS.getUri(), "Concept");
        writer.writeAttribute(Namespace.RDF.getUri(), "about", "http://open-data.europa.eu/kos/interoperability-level/Legal");
        writer.writeEndElement();

        // Write ecodp:datasetType
        writer.writeStartElement(Namespace.ECODP.getUri(), "datasetType");
        writer.writeEmptyElement(Namespace.SKOS.getUri(), "Concept");
        writer.writeAttribute(Namespace.RDF.getUri(), "about", "http://open-data.europa.eu/kos/dataset-type/Statistical");
        writer.writeEndElement();

        // Write ecodp:isDocumentedBy for the main home page about the main dataset
        writer.writeStartElement(Namespace.ECODP.getUri(), "isDocumentedBy");
        writer.writeAttribute(Namespace.RDF.getUri(), "parseType", "Resource");
        writer.writeStartElement(Namespace.ECODP.getUri(), "documentationType");
        writer.writeEmptyElement(Namespace.SKOS.getUri(), "Concept");
        writer.writeAttribute(Namespace.RDF.getUri(), "about",
                "http://open-data.europa.eu/kos/documentation-type/MainDocumentation");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.ECODP.getUri(), "accessURL");
        writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "datatype",
                "http://www.w3.org/2001/XMLSchema#anyURI");
        writer.writeCharacters("http://digital-agenda-data.eu/datasets/digital_agenda_scoreboard_key_indicators");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "title");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("The dataset homepage");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "description");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("The main information about the dataset, with its key metadata and further links to downloads.");
        writer.writeEndElement();
        writer.writeEndElement();

        // Write ecodp:isDocumentedBy for the Scoreboard documentation page
        writer.writeStartElement(Namespace.ECODP.getUri(), "isDocumentedBy");
        writer.writeAttribute(Namespace.RDF.getUri(), "parseType", "Resource");
        writer.writeStartElement(Namespace.ECODP.getUri(), "documentationType");
        writer.writeEmptyElement(Namespace.SKOS.getUri(), "Concept");
        writer.writeAttribute(Namespace.RDF.getUri(), "about",
                "http://open-data.europa.eu/kos/documentation-type/RelatedDocumentation");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.ECODP.getUri(), "accessURL");
        writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "datatype",
                "http://www.w3.org/2001/XMLSchema#anyURI");
        writer.writeCharacters("http://digital-agenda-data.eu/documentation");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "title");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("Reports and notes about the technical characteristics.");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "description");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("This page presents some reports and notes about the technical characteristics of the project"
                + "developing the dataset's present visualisation tool and semantic repository.");
        writer.writeEndElement();
        writer.writeEndElement();

        // Write ecodp:isDocumentedBy for the main dataset's visualisation page
        writer.writeStartElement(Namespace.ECODP.getUri(), "isDocumentedBy");
        writer.writeAttribute(Namespace.RDF.getUri(), "parseType", "Resource");
        writer.writeStartElement(Namespace.ECODP.getUri(), "documentationType");
        writer.writeEmptyElement(Namespace.SKOS.getUri(), "Concept");
        writer.writeAttribute(Namespace.RDF.getUri(), "about",
                "http://open-data.europa.eu/kos/documentation-type/RelatedDocumentation");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.ECODP.getUri(), "accessURL");
        writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "datatype",
                "http://www.w3.org/2001/XMLSchema#anyURI");
        writer.writeCharacters("http://digital-agenda-data.eu/datasets/digital_agenda_scoreboard_key_indicators/visualizations");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "title");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("The dataset's visualisations");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "description");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("Various dynamically generated visualisations (i.e. charts, diagrams) of the dataset contents.");
        writer.writeEndElement();
        writer.writeEndElement();

        // Write ecodp:isDocumentedBy for the list of main dataset's indicators
        writer.writeStartElement(Namespace.ECODP.getUri(), "isDocumentedBy");
        writer.writeAttribute(Namespace.RDF.getUri(), "parseType", "Resource");
        writer.writeStartElement(Namespace.ECODP.getUri(), "documentationType");
        writer.writeEmptyElement(Namespace.SKOS.getUri(), "Concept");
        writer.writeAttribute(Namespace.RDF.getUri(), "about",
                "http://open-data.europa.eu/kos/documentation-type/RelatedDocumentation");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.ECODP.getUri(), "accessURL");
        writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "datatype",
                "http://www.w3.org/2001/XMLSchema#anyURI");
        writer.writeCharacters("http://digital-agenda-data.eu/datasets/digital_agenda_scoreboard_key_indicators/indicators");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "title");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("List and facts about all key indicators related.");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "description");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("A page that lists metadata about all key indicators related to this dataset.");
        writer.writeEndElement();
        writer.writeEndElement();

        // Write ecodp:isDocumentedBy for the Digital Agenda Scoreboard home page
        writer.writeStartElement(Namespace.ECODP.getUri(), "isDocumentedBy");
        writer.writeAttribute(Namespace.RDF.getUri(), "parseType", "Resource");
        writer.writeStartElement(Namespace.ECODP.getUri(), "documentationType");
        writer.writeEmptyElement(Namespace.SKOS.getUri(), "Concept");
        writer.writeAttribute(Namespace.RDF.getUri(), "about", "http://open-data.europa.eu/kos/documentation-type/RelatedWebPage");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.ECODP.getUri(), "accessURL");
        writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "datatype",
                "http://www.w3.org/2001/XMLSchema#anyURI");
        writer.writeCharacters("http://ec.europa.eu/digital-agenda/en/scoreboard");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "title");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("Home page of the Digital Agenda Scoreboard");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "description");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("Digital Agenda Scoreboard home page to which the visualisation page subordinates.");
        writer.writeEndElement();
        writer.writeEndElement();

        // Write ecodp:isDocumentedBy for the Digital Agenda's home page
        writer.writeStartElement(Namespace.ECODP.getUri(), "isDocumentedBy");
        writer.writeAttribute(Namespace.RDF.getUri(), "parseType", "Resource");
        writer.writeStartElement(Namespace.ECODP.getUri(), "documentationType");
        writer.writeEmptyElement(Namespace.SKOS.getUri(), "Concept");
        writer.writeAttribute(Namespace.RDF.getUri(), "about", "http://open-data.europa.eu/kos/documentation-type/RelatedWebPage");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.ECODP.getUri(), "accessURL");
        writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "datatype",
                "http://www.w3.org/2001/XMLSchema#anyURI");
        writer.writeCharacters("http://ec.europa.eu/digital-agenda");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "title");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("Home page of the Digital Agenda for Europe");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "description");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("Home page of the EU initiative in the context of which the dataset has been produced.");
        writer.writeEndElement();
        writer.writeEndElement();

        // Write dcat:distribution for the download link
        writer.writeStartElement(Namespace.DCAT.getUri(), "distribution");
        writer.writeAttribute(Namespace.RDF.getUri(), "parseType", "Resource");
        writer.writeStartElement(Namespace.DCT.getUri(), "title");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("Download options for the entire dataset.");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.ECODP.getUri(), "accessURL");
        writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "datatype",
                "http://www.w3.org/2001/XMLSchema#anyURI");
        writer.writeCharacters("http://digital-agenda-data.eu/datasets/digital_agenda_scoreboard_key_indicators");
        writer.writeEndElement();
        writer.writeEmptyElement(Namespace.RDF.getUri(), "type");
        writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "resource",
                "http://www.w3.org/TR/vocab-dcat#Download");
        writer.writeStartElement(Namespace.ECODP.getUri(), "distributionFormat");
        writer.writeCharacters("text/csv");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.ECODP.getUri(), "distributionFormat");
        writer.writeCharacters("text/n3");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.ECODP.getUri(), "distributionFormat");
        writer.writeCharacters("application/rdf+xml");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "description");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("The database with the selected indicators of the Digital Agenda Scoreboard is made of three tables: "
                + "a) a data table, with codes for the indicators, countries, years and values; "
                + "b) an indicators table with labels for indicators' codes, definition and scope, and a source code; "
                + "c) a sources table, with details about sources and links to more methodological information. "
                + "The codes allow the creation of relations between the 3 tables. ");
        writer.writeEndElement();
        if (StringUtils.isNotEmpty(dctModified)) {
            writer.writeStartElement(Namespace.DCT.getUri(), "modified");
            writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "datatype",
                    "http://www.w3.org/2001/XMLSchema#dateTime");
            writer.writeCharacters(dctModified);
            writer.writeEndElement();
        }
        if (StringUtils.isNotEmpty(dctIssued)) {
            writer.writeStartElement(Namespace.DCT.getUri(), "issued");
            writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "datatype",
                    "http://www.w3.org/2001/XMLSchema#dateTime");
            writer.writeCharacters(dctIssued);
            writer.writeEndElement();
        }
        writer.writeEndElement();

        // Write dcat:distribution for the SPARQL endpoint
        writer.writeStartElement(Namespace.DCAT.getUri(), "distribution");
        writer.writeAttribute(Namespace.RDF.getUri(), "parseType", "Resource");
        writer.writeStartElement(Namespace.DCT.getUri(), "title");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("SPARQL endpoint of the dataset.");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.ECODP.getUri(), "accessURL");
        writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "datatype",
                "http://www.w3.org/2001/XMLSchema#anyURI");
        writer.writeCharacters("http://digital-agenda-data.eu/data/sparql");
        writer.writeEndElement();
        writer.writeEmptyElement(Namespace.RDF.getUri(), "type");
        writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "resource",
                "http://www.w3.org/TR/vocab-dcat#WebService");
        writer.writeStartElement(Namespace.ECODP.getUri(), "distributionFormat");
        writer.writeCharacters("webservice/sparql");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.DCT.getUri(), "description");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("This SPARQL endpoint (for machines regarding the SPARQL protocol for RDF) offers a public "
                + "service to the statistical data allowing anyone to build applications based on the most recent data.");
        writer.writeEndElement();
        writer.writeEndElement();

        // Write reference areas.
        if (CollectionUtils.isNotEmpty(refAreas)) {
            for (String refArea : refAreas) {

                writer.writeStartElement(Namespace.DCT.getUri(), "spatial");
                writer.writeEmptyElement(Namespace.SKOS.getUri(), "Concept");
                writer.writeAttribute(Namespace.RDF.getUri(), "about", refArea);
                writer.writeEndElement();
            }
        }

        // Write dct:publisher
        writer.writeStartElement(Namespace.DCT.getUri(), "publisher");
        writer.writeEmptyElement(Namespace.SKOS.getUri(), "Concept");
        writer.writeAttribute(Namespace.RDF.getUri(), "about",
                "http://publications.europa.eu/resource/authority/corporate-body/CNECT");
        writer.writeEndElement();

        // Write ecodp:contactPoint
        writer.writeStartElement(Namespace.ECODP.getUri(), "contactPoint");
        writer.writeStartElement(Namespace.FOAF.getUri(), "agent");
        writer.writeAttribute(Namespace.RDF.getUri(), "about",
                "http://publications.europa.eu/resource/authority/corporate-body/CNECT/C4");
        writer.writeEmptyElement(Namespace.FOAF.getUri(), "mbox");
        writer.writeAttribute(Namespace.RDF.getUri(), "resource", "mailto:CNECT-F4@ec.europa.eu");
        writer.writeEmptyElement(Namespace.FOAF.getUri(), "workplaceHomepage");
        writer.writeAttribute(Namespace.RDF.getUri(), "resource", "http://digital-agenda-data.eu/");
        writer.writeStartElement(Namespace.FOAF.getUri(), "name");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("DG CONNECT Unit F4 Knowledge Base");
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndElement();

        // Write dct:issued
        if (StringUtils.isNotEmpty(dctIssued)) {
            writer.writeStartElement(Namespace.DCT.getUri(), "issued");
            writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "datatype",
                    "http://www.w3.org/2001/XMLSchema#dateTime");
            writer.writeCharacters(dctIssued);
            writer.writeEndElement();
        }

        // Write dct:modified (mandatory, so don't even check if empty)
        writer.writeStartElement(Namespace.DCT.getUri(), "modified");
        writer.writeAttribute(Namespace.RDF.getPrefix(), Namespace.RDF.getUri(), "datatype",
                "http://www.w3.org/2001/XMLSchema#dateTime");
        writer.writeCharacters(dctModified);
        writer.writeEndElement();

        // Write dct:license
        writer.writeStartElement(Namespace.DCT.getUri(), "license");
        writer.writeEmptyElement(Namespace.SKOS.getUri(), "Concept");
        writer.writeAttribute(Namespace.RDF.getUri(), "about", "http://open-data.europa.eu/kos/licence/EuropeanCommission");
        writer.writeEndElement();

        // Write ecodp:datasetStatus
        // TODO: get from the main dataset object actually
        writer.writeStartElement(Namespace.ECODP.getUri(), "datasetStatus");
        writer.writeEmptyElement(Namespace.SKOS.getUri(), "Concept");
        writer.writeAttribute(Namespace.RDF.getUri(), "about", "http://open-data.europa.eu/kos/dataset-status/Completed");
        writer.writeEndElement();

        // Write dct:language
        writer.writeStartElement(Namespace.DCT.getUri(), "language");
        writer.writeEmptyElement(Namespace.SKOS.getUri(), "Concept");
        writer.writeAttribute(Namespace.RDF.getUri(), "about", "http://publications.europa.eu/resource/authority/language/ENG");
        writer.writeEndElement();

        // Write ecodp:accrualPeriodicity
        // TODO: Clarify should come from the main dataset object? Because here we need literal, but in dataste object it's URI.
        writer.writeStartElement(Namespace.ECODP.getUri(), "accrualPeriodicity");
        writer.writeAttribute(Namespace.XML.getPrefix(), Namespace.XML.getUri(), "lang", "en");
        writer.writeCharacters("semiannual");
        writer.writeEndElement();

        // Write dct:temporal
        // TODO: Currenlty not in main dataset object, but should it be there?
        writer.writeStartElement(Namespace.DCT.getUri(), "temporal");
        writer.writeAttribute(Namespace.RDF.getUri(), "parseType", "Resource");
        writer.writeStartElement(Namespace.ECODP.getUri(), "periodStart");
        writer.writeCharacters("2001-01-01");
        writer.writeEndElement();
        writer.writeStartElement(Namespace.ECODP.getUri(), "periodEnd");
        writer.writeCharacters("2013-12-31");
        writer.writeEndElement();
        writer.writeEndElement();

        // End the dataset tag.
        writer.writeEndElement();

        // End the root tag.
        writer.writeEndElement();

        // End the document
        writer.writeEndDocument();
    }

    /**
     * Registers {@link #NAMESPACES} in the given {@link XMLStreamWriter}, by calling setPrefix(...) of the latter for each.
     *
     * @param xmlWriter The writer to register in.
     * @throws XMLStreamException In case the write throws exception.
     */
    private void registerNamespaces(XMLStreamWriter xmlWriter) throws XMLStreamException {

        for (Namespace namespace : NAMESPACES) {
            xmlWriter.setPrefix(namespace.getPrefix(), namespace.getUri());
        }
    }

    /**
     * Build a list of namespaces used in the generated RDF/XML files.
     *
     * @return The list.
     */
    private static List<Namespace> buildNamespacesList() {

        ArrayList<Namespace> list = new ArrayList<Namespace>();
        list.add(Namespace.RDF);
        list.add(Namespace.RDFS);
        list.add(Namespace.OWL);
        list.add(Namespace.XSD);
        list.add(Namespace.DC);
        list.add(Namespace.DCT);
        list.add(Namespace.DCAM);
        list.add(Namespace.DCAT);
        list.add(Namespace.ECODP);
        list.add(Namespace.FOAF);
        list.add(Namespace.SKOS);
        list.add(Namespace.SKOS_XL);
        return list;
    }
}
