package eionet.cr.util.odp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import eionet.cr.common.Predicates;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.SearchDAO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.util.URIUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

/**
 * @author Jaanus
 */
public class ODPDatasetsPacker {

    /** */
    private static final Logger LOGGER = Logger.getLogger(ODPDatasetsPacker.class);

    /** */
    private static final String DATASET_TEMPLATE_PATH = "freemarker/odp-dataset-template.ftl";

    /** */
    private static final Configuration TEMPLATES_CONFIGURATION = createTemplatesConfiguration();

    /** */
    private ODPAction odpAction;

    /** */
    private boolean isPrepareCalled;

    /** */
    private List<String> datasetUris;

    /** */
    private List<SubjectDTO> datasetSubjects;

    /**
     * @param datasetUris
     * @param odpAction
     */
    public ODPDatasetsPacker(List<String> datasetUris, ODPAction odpAction) {

        this.datasetUris = datasetUris;
        this.odpAction = odpAction;
    }

    /**
     * Does preparations for the {@link #execute(OutputStream)} method, so it should be called before the latter, otherwise the
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

        datasetSubjects = DAOFactory.get().getDao(SearchDAO.class).getSubjectsData(datasetUris, null);
        if (CollectionUtils.isEmpty(datasetSubjects)) {
            throw new DAOException("Could not find any metadata about the given datasets!");
        }
    }

    /**
     *
     * @param outputStream
     * @throws IOException
     * @throws TemplateException
     * @throws DAOException
     */
    public void execute(OutputStream outputStream) throws IOException, TemplateException, DAOException {

        if (!isPrepareCalled) {
            throw new IllegalStateException("Prepare has not been called yet!");
        }

        ZipArchiveOutputStream zipOutput = null;
        try {
            zipOutput = new ZipArchiveOutputStream(outputStream);

            int i = 0;
            for (SubjectDTO datasetSubject : datasetSubjects) {
                createAndWriteDatasetEntry(zipOutput, datasetSubject, i++);
            }
            // TODO: create and write manifestEntry
        } finally {
            IOUtils.closeQuietly(zipOutput);
        }
    }

    /**
     *
     * @param zipOutput
     * @param datasetSubject
     * @param datasetIndex
     * @throws IOException
     * @throws TemplateException
     * @throws DAOException
     */
    private void createAndWriteDatasetEntry(ZipArchiveOutputStream zipOutput, SubjectDTO datasetSubject, int datasetIndex)
            throws IOException, TemplateException, DAOException {

        String datasetUri = datasetSubject.getUri();
        String datasetIdentifier = datasetSubject.getObjectValue(Predicates.DCTERMS_IDENTIFIER);
        if (StringUtils.isBlank(datasetIdentifier) || datasetIdentifier.equals(datasetUri)) {
            datasetIdentifier = URIUtil.extractURILabel(datasetUri);
            if (StringUtils.isBlank(datasetIdentifier)) {
                throw new DAOException("Could not detect identifier of dataset with URI = " + datasetUri);
            }
        }

        ZipArchiveEntry entry = new ZipArchiveEntry("datasets/" + datasetIdentifier + ".rdf");
        zipOutput.putArchiveEntry(entry);
        writeDatasetEntry(zipOutput, datasetIdentifier, datasetSubject, datasetIndex);
        zipOutput.closeArchiveEntry();
    }

    /**
     *
     * @param zipOutput
     * @param datasetIdentifier TODO
     * @param datasetSubject
     * @param index
     * @throws IOException
     * @throws TemplateException
     */
    private void writeDatasetEntry(ZipArchiveOutputStream zipOutput, String datasetIdentifier, SubjectDTO datasetSubject, int index) throws IOException, TemplateException {

        Template template = TEMPLATES_CONFIGURATION.getTemplate(DATASET_TEMPLATE_PATH);

        ODPDataset odpDataset = new ODPDataset();
        odpDataset.setUri(datasetSubject.getUri());
        odpDataset.setIdentifier(datasetIdentifier);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("dataset", odpDataset);

        Writer writer = new OutputStreamWriter(zipOutput, Charset.forName("UTF-8"));
        template.process(data, writer);
    }

    /**
     *
     * @return
     */
    private static Configuration createTemplatesConfiguration() {
        Configuration cfg = new Configuration(new Version(2, 3, 25));
        cfg.setClassForTemplateLoading(ODPDatasetsPacker.class, "/");
        return cfg;
    }

    /**
     *
     * @param args
     * @throws IOException
     * @throws TemplateException
     */
    public static void main(String[] args) throws IOException, TemplateException {

//        FreeMarker Template example: ${message}
//    ${messageMore}
//    =======================
//    ===  County List   ====
//    =======================
//    <#list countries as country>
//        ${country_index + 1}. ${country}
//    </#list>


        Template template = TEMPLATES_CONFIGURATION.getTemplate(DATASET_TEMPLATE_PATH);

        System.out.println(template == null);

        // Build the data-model
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("message", "Hello World!");

        // List parsing
        List<String> countries = new ArrayList<String>();
        countries.add("India");
        countries.add("United States");
        countries.add("Germany");
        countries.add("France");

        data.put("countries", countries);

        // Console output
        Writer out = new OutputStreamWriter(System.out);
        template.process(data, out);
        out.flush();
    }
}
