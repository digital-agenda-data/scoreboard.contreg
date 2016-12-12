package eionet.cr.util.odp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import eionet.cr.common.CRRuntimeException;
import eionet.cr.common.Predicates;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.DAOFactory;
import eionet.cr.dao.ScoreboardSparqlDAO;
import eionet.cr.dao.SearchDAO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.freemarker.TemplatesConfiguration;
import eionet.cr.util.URIUtil;
import eionet.cr.util.Util;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * @author Jaanus
 */
public class ODPDatasetsPacker {

    /** */
    private static final String UTF_8 = "UTF-8";

    /** */
    private static final String DATASET_TEMPLATE_PATH = "freemarker/odp-dataset-template.ftl";

    /** */
    private static final String MANIFEST_TEMPLATE_PATH = "freemarker/odp-manifest-template.ftl";

    /** */
    private ODPAction odpAction;

    /** */
    private boolean isPrepareCalled;

    /** */
    private List<String> datasetUris;

    /** */
    private List<ODPDataset> odpDatasets = new ArrayList<>();

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

        List<SubjectDTO> datasetSubjects = DAOFactory.get().getDao(SearchDAO.class).getSubjectsData(datasetUris, null);
        if (CollectionUtils.isEmpty(datasetSubjects)) {
            throw new DAOException("Could not find any metadata about the given datasets!");
        }

        ScoreboardSparqlDAO ssDao = DAOFactory.get().getDao(ScoreboardSparqlDAO.class);
        for (SubjectDTO datasetSubject : datasetSubjects) {
            ODPDataset odpDataset = toOdpDataset(datasetSubject, ssDao);
            odpDatasets.add(odpDataset);
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
            for (ODPDataset odpDataset : odpDatasets) {
                createAndWriteDatasetEntry(zipOutput, odpDataset, i++);
            }

            createAndWriteManifestEntry(zipOutput, odpDatasets);
        } finally {
            IOUtils.closeQuietly(zipOutput);
        }
    }

    /**
     *
     * @param zipOutput
     * @param odpDataset
     * @param datasetIndex
     * @return
     * @throws IOException
     * @throws TemplateException
     * @throws DAOException
     * @throws ReflectiveOperationException
     */
    private ODPDataset createAndWriteDatasetEntry(ZipArchiveOutputStream zipOutput, ODPDataset odpDataset, int datasetIndex)
            throws DAOException {

        try {
            ZipArchiveEntry entry = new ZipArchiveEntry("datasets/" + odpDataset.getIdentifier() + ".rdf");
            zipOutput.putArchiveEntry(entry);
            odpDataset = writeDatasetEntry(zipOutput, odpDataset, datasetIndex);
            zipOutput.closeArchiveEntry();
        } catch (IOException | TemplateException | ReflectiveOperationException e) {
            throw new DAOException(e.getMessage(), e);
        }

        return odpDataset;
    }

    /**
     *
     * @param zipOutput
     * @param datasetSubject
     * @param index
     * @return
     * @throws IOException
     * @throws TemplateException
     * @throws ReflectiveOperationException
     */
    private ODPDataset writeDatasetEntry(ZipArchiveOutputStream zipOutput, ODPDataset odpDataset, int index)
            throws IOException, TemplateException, ReflectiveOperationException {

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("dataset", odpDataset);

        Writer writer = new OutputStreamWriter(zipOutput, Charset.forName(UTF_8));
        Template template = TemplatesConfiguration.getInstance().getTemplate(DATASET_TEMPLATE_PATH);
        template.process(data, writer);

        return odpDataset;
    }

    /**
     *
     * @param zipOutput
     * @param odpDatasets
     * @throws IOException
     * @throws TemplateException
     */
    private void createAndWriteManifestEntry(ZipArchiveOutputStream zipOutput, List<ODPDataset> odpDatasets)
            throws IOException, TemplateException {

        List<ODPManifestEntry> manifestEntries = new ArrayList<>();
        for (ODPDataset odpDataset : odpDatasets) {

            ODPManifestEntry entry = new ODPManifestEntry();
            entry.setDataset(odpDataset);
            entry.setOdpAction(odpAction);
            manifestEntries.add(entry);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("manifestEntries", manifestEntries);

        ZipArchiveEntry entry = new ZipArchiveEntry("manifest.xml");
        zipOutput.putArchiveEntry(entry);

        Template template = TemplatesConfiguration.getInstance().getTemplate(MANIFEST_TEMPLATE_PATH);
        Writer writer = new OutputStreamWriter(zipOutput, Charset.forName(UTF_8));
        template.process(data, writer);

        zipOutput.closeArchiveEntry();
    }

    /**
     *
     * @param datasetSubject
     * @return
     * @throws DAOException
     */
    private ODPDataset toOdpDataset(SubjectDTO datasetSubject, ScoreboardSparqlDAO ssDao) throws DAOException {

        ODPDataset odpDataset = new ODPDataset();

        String datasetUri = datasetSubject.getUri();
        String datasetIdentifier = datasetSubject.getObjectValue(Predicates.DCTERMS_IDENTIFIER);
        if (StringUtils.isBlank(datasetIdentifier) || datasetIdentifier.equals(datasetUri)) {
            datasetIdentifier = URIUtil.extractURILabel(datasetUri);
            if (StringUtils.isBlank(datasetIdentifier)) {
                throw new CRRuntimeException("Could not detect identifier of dataset with URI = " + datasetUri);
            }
        }

        odpDataset.setUri(datasetUri);
        odpDataset.setIdentifier(datasetIdentifier);

        String title = datasetSubject.getObjectValue(Predicates.DCTERMS_TITLE);
        if (StringUtils.isBlank(title)) {
            title = datasetSubject.getObjectValue(Predicates.RDFS_LABEL);
            if (StringUtils.isBlank(title)) {
                title = datasetIdentifier;
            }
        }
        odpDataset.setTitle(title);

        String description = datasetSubject.getObjectValue(Predicates.DCTERMS_DESCRIPTION);
        if (StringUtils.isBlank(title)) {
            description = title;
        }
        odpDataset.setDescription(description);

        String modifiedDate = StringUtils.EMPTY;
        List<String> modifiedDates = datasetSubject.getObjectValues(Predicates.DCTERMS_MODIFIED);
        if (CollectionUtils.isNotEmpty(modifiedDates)) {
            Collections.sort(modifiedDates);
            modifiedDate = modifiedDates.get(modifiedDates.size() - 1).trim();
        }
        if (StringUtils.isBlank(modifiedDate)) {
            modifiedDate = Util.virtuosoDateToString(new Date());
        }
        odpDataset.setModified(modifiedDate);

        String issuedDate = StringUtils.EMPTY;
        List<String> issuedDates = datasetSubject.getObjectValues(Predicates.DCTERMS_ISSUED);
        if (CollectionUtils.isNotEmpty(issuedDates)) {
            Collections.sort(issuedDates);
            issuedDate = modifiedDates.get(modifiedDates.size() - 1).trim();
        }
        if (StringUtils.isBlank(issuedDate)) {
            issuedDate = Util.virtuosoDateToString(new Date());
        }
        odpDataset.setIssued(issuedDate);

        List<String> odpCountryUris = new ArrayList<>();
        List<String> refAreaUris = ssDao.getDistinctUsedRefAreas(datasetUri, null);
        for (String refAreaUri : refAreaUris) {
            String odpCountryUri = ODPCountryMappings.getMappingFor(refAreaUri);
            if (StringUtils.isNotBlank(odpCountryUri)) {
                odpCountryUris.add(odpCountryUri);
            }
        }
        odpDataset.setSpatialUris(odpCountryUris);

        try {
            Util.trimToNullAllStringProperties(odpDataset);
        } catch (ReflectiveOperationException e) {
            throw new CRRuntimeException("Failed to trim all string properties");
        }
        return odpDataset;
    }
}
