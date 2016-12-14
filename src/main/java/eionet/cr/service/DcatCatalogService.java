package eionet.cr.service;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;

import eionet.cr.common.Predicates;
import eionet.cr.common.Subjects;
import eionet.cr.dto.DcatCatalogTemplateDTO;
import eionet.cr.freemarker.TemplatesConfiguration;
import eionet.cr.util.Util;
import eionet.cr.util.sesame.SesameUtil;
import freemarker.template.Template;

/**
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DcatCatalogService {

    /** */
    public static final String CATALOG_URI_PREFIX = "http://semantic.digital-agenda-data.eu/catalog/";

    /** */
    private static final String CATALOG_RDF_TEMPLATE_PATH = "freemarker/catalog-rdf-template.ftl";

    /**
     *
     * @return
     */
    public static DcatCatalogService newInstance() {
        return new DcatCatalogService();
    }

    /**
     *
     * @param identifier
     * @param title
     * @param description
     * @return
     * @throws ServiceException
     */
    public int createCatalog(String identifier, String title, String description) throws ServiceException {

        if (StringUtils.isBlank(identifier) || StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("Identifier and title must not be blank!");
        }

        DcatCatalogTemplateDTO dto = new DcatCatalogTemplateDTO();
        dto.setIdentifier(identifier);
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setUri(CATALOG_URI_PREFIX + identifier);

        return createCatalogs(Arrays.asList(dto), false);
    }

    /**
     *
     * @param catalogs
     * @param clear
     * @throws ServiceException
     */
    public int createCatalogs(List<DcatCatalogTemplateDTO> catalogs, boolean clear) throws ServiceException {

        if (CollectionUtils.isEmpty(catalogs)) {
            return 0;
        }

        RepositoryConnection repoConn = null;
        try {

            repoConn = SesameUtil.getRepositoryConnection();
            ValueFactory vf = repoConn.getValueFactory();
            Template template = TemplatesConfiguration.getInstance().getTemplate(CATALOG_RDF_TEMPLATE_PATH);
            String modifiedDateTimeStr = Util.virtuosoDateToString(new Date());

            Set<String> catalogUris = new HashSet<>();
            for (DcatCatalogTemplateDTO catalog : catalogs) {

                Util.trimToNullAllStringProperties(catalog);
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("catalog", catalog);
                catalog.setModifiedDateTimeStr(modifiedDateTimeStr);

                Writer writer = null;
                Reader reader = null;
                try {
                    writer = new StringWriter();
                    template.process(data, writer);
                    String str = writer.toString();
                    reader = new StringReader(str);

                    String catalogUri = catalog.getUri();
                    URI graphURI = vf.createURI(catalogUri);

                    // If clear requested and this catalog's metadata not yet added (because it could be added multiple times,
                    // e.g. for each row in imported spreadsheet file), then clear the graph.
                    if (clear && !catalogUris.contains(catalogUri)) {
                        repoConn.clear(graphURI);
                    }

                    catalogUris.add(catalogUri);
                    repoConn.add(reader, catalogUri, RDFFormat.TURTLE, graphURI);
                } finally {
                    IOUtils.closeQuietly(reader);
                    IOUtils.closeQuietly(writer);
                }
            }

            return catalogUris.size();
        } catch (Exception e) {
            throw new ServiceException(e.getMessage(), e);
        } finally {
            SesameUtil.close(repoConn);
        }

    }

    /**
     *
     * @param catalogIdentifier
     * @return
     * @throws ServiceException
     */
    public boolean isCatalogExisting(String catalogIdentifier) throws ServiceException {

        RepositoryConnection repoConn = null;
        try {
            repoConn = SesameUtil.getRepositoryConnection();

            ValueFactory vf = repoConn.getValueFactory();
            URI identifierURI = vf.createURI(CATALOG_URI_PREFIX + catalogIdentifier);
            URI typeURI = vf.createURI(Predicates.RDF_TYPE);

            return repoConn.hasStatement(identifierURI, typeURI, vf.createURI(Subjects.DCAT_DATASET), false);
        } catch (RepositoryException e) {
            throw new ServiceException(e.getMessage(), e);
        } finally {
            SesameUtil.close(repoConn);
        }
    }
}
