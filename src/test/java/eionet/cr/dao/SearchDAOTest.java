/*
 * The contents of this file are subject to the Mozilla Public
 *
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Content Registry 2.0.
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency. Portions created by Tieto Eesti are Copyright
 * (C) European Environment Agency. All Rights Reserved.
 *
 * Contributor(s):
 * Jaanus Heinlaid, Tieto Eesti*/
package eionet.cr.dao;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import eionet.cr.common.Predicates;
import eionet.cr.config.GeneralConfig;
import eionet.cr.dao.helpers.FreeTextSearchHelper;
import eionet.cr.dao.util.SearchExpression;
import eionet.cr.dto.SearchResultDTO;
import eionet.cr.dto.SubjectDTO;
import eionet.cr.test.helpers.RdfLoader;
import eionet.cr.util.pagination.PagingRequest;

/**
 *
 * @author Risto Alt
 *
 */
public class SearchDAOTest {

    private static final String seedFile = "obligations.rdf";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new RdfLoader(seedFile);
    }

    @Test
    public void testFreeTextSearchCountResults() throws Exception {

        // Should not test full-text search if there is no real-time full-text indexing activated in the underlying repository.
        // By "real-time" we mean that the index is updated instantly after loading a triple.
        if (isRealTimeFullTextIndexingActivated() == false) {
            System.out.println("Skipping full-text search test, as no real-time full-text indexing has been activated!");
            return;
        }

        PagingRequest pagingRequest = PagingRequest.create(1);
        SearchResultDTO<SubjectDTO> result =
                DAOFactory
                        .get()
                        .getDao(SearchDAO.class)
                        .searchByFreeText(new SearchExpression("Questionnaire"), FreeTextSearchHelper.FilterType.ANY_OBJECT,
                                false, pagingRequest, null);

        assertEquals(2, result.getMatchCount());
    }

    /**
     * @throws Exception
     *
     */
    @Test
    public void testSearchByTypeAndFilters() throws Exception {
        PagingRequest pagingRequest = PagingRequest.create(1);

        Map<String, String> filters = new LinkedHashMap<String, String>();
        filters.put(Predicates.RDF_TYPE, "http://rod.eionet.europa.eu/schema.rdf#Obligation");
        List<String> selectedPredicates = new ArrayList<String>();

        SearchResultDTO<SubjectDTO> result =
                DAOFactory.get().getDao(SearchDAO.class)
                        .searchByTypeAndFilters(filters, false, pagingRequest, null, selectedPredicates);

        assertEquals(3, result.getMatchCount());
    }

    /**
     * Returns true if the configuration says that the underlying triple-store has real-time full-text indexing activated.
     *
     * @return True/false.
     */
    private boolean isRealTimeFullTextIndexingActivated() {
        String value = GeneralConfig.getProperty(GeneralConfig.VIRTUOSO_REAL_TIME_FT_INDEXING);
        return BooleanUtils.toBoolean(value);
    }
}
