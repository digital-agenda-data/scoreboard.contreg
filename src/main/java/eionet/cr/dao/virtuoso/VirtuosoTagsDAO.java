package eionet.cr.dao.virtuoso;

import java.util.List;

import eionet.cr.common.Predicates;
import eionet.cr.config.GeneralConfig;
import eionet.cr.dao.DAOException;
import eionet.cr.dao.TagsDAO;
import eionet.cr.dao.readers.TagCloudReader;
import eionet.cr.dto.TagDTO;

/**
 * Queries for handling tags.
 * @author jaanus
 */
public class VirtuosoTagsDAO extends VirtuosoBaseDAO implements TagsDAO {
    /**
     * SPARQL returning distinct values of tags with corresponding tag counts.
     */
    public static final String GET_TAGS_WITH_FREQUENCIES_SPARQL = "define input:inference '"
            + GeneralConfig.getProperty(GeneralConfig.VIRTUOSO_CR_RULESET_NAME)
            + "' SELECT ?o (count(?o) as ?c) WHERE { ?s <" + Predicates.CR_TAG + "> ?o "
            + "} ORDER BY DESC(?c)";

    /**
     * Returns tag cloud.
     * @see eionet.cr.dao.TagsDAO#getTagCloud()
     * @return List<TagDTO>
     * @throws DAOException if query fails
     */
    @Override
    public List<TagDTO> getTagCloud() throws DAOException {

        TagCloudReader reader = new TagCloudReader();
        executeSPARQL(GET_TAGS_WITH_FREQUENCIES_SPARQL, reader);

        return reader.getResultList();

    }
}
