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

package eionet.cr.staging.exp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.openrdf.model.vocabulary.XMLSchema;

/**
 *
 * @author jaanus
 */
public class ObjectTypes {

    /**
     *
     */
    private static final LinkedHashMap<DSD, ObjectType> TYPES_BY_DSD = load();

    /**
     * Disable utility class constructor.
     */
    private ObjectTypes() {
        // Empty constructor.
    }

    /**
     * Gets object type by DSD.
     *
     * @param dsd the DSD
     * @return
     */
    public static ObjectType getByDsd(DSD dsd) {
        return TYPES_BY_DSD.get(dsd);
    }

    /**
     * Gets the map.
     *
     * @return the map
     */
    public static Map<DSD, ObjectType> getMap() {
        return TYPES_BY_DSD;
    }

    /**
     * Load.
     *
     * @return the linked hash map
     */
    private static LinkedHashMap<DSD, ObjectType> load() {

        // TODO: load from triplestore.

        LinkedHashMap<DSD, ObjectType> result = new LinkedHashMap<DSD, ObjectType>();

        ObjectType defaultObservation = createScoreboardObservationType();
        result.put(defaultObservation.getDsd(), defaultObservation);

        ObjectType desiObservation = createDesiObservationType();
        result.put(desiObservation.getDsd(), desiObservation);

        return result;
    }

    /**
     * Creates the default observation type.
     *
     * @return the object type
     */
    private static ObjectType createScoreboardObservationType() {

        ObjectType qbObservation =
                new ObjectType("http://purl.org/linked-data/cube#Observation", "Default Scoreboard observation", DSD.SCOREBOARD);
        String objUriTempl = "http://semantic.digital-agenda-data.eu/data/<dataSet>/<breakdown>/<unit>/<refArea>/<timePeriod>";
        qbObservation.setObjectUriTemplate(objUriTempl);
        qbObservation.setDatasetIdentifierColumns(new HashSet<>(Arrays.asList("dataSet", "dataset", "variable", "variableCode")));
        qbObservation.setDatasetUriTemplate("http://semantic.digital-agenda-data.eu/dataset/<identifier>");

        // dataSet.
        ObjectProperty property =
                new ObjectProperty("http://purl.org/linked-data/cube#dataSet", "dataSet", "dataSet", ObjectProperty.Range.RESOURCE);
        property.setValueTemplate("http://semantic.digital-agenda-data.eu/dataset/<value>");
        property.setHint("Expects a Eurostat dataset code. e.g. aact_ali01, edat_aes_l11, fish_ld_nl, etc.");

        // Breakdown.
        property = new ObjectProperty("http://semantic.digital-agenda-data.eu/def/property/breakdown", "breakdown", "breakdown",
                ObjectProperty.Range.RESOURCE);
        property.setValueTemplate("http://semantic.digital-agenda-data.eu/codelist/breakdown/<value>");
        property.setHint("Expects a Eurostat breakdown code. e.g. 10_bb, 10_c10, etc.");
        qbObservation.addProperty(property, true, "breakdown", "brkDown", "brkdwn", "breakdownCode", "brkDownCode", "brkdwnCode");

        // Unit.
        property = new ObjectProperty("http://semantic.digital-agenda-data.eu/def/property/unit-measure", "unit", "unit",
                ObjectProperty.Range.RESOURCE);
        property.setValueTemplate("http://semantic.digital-agenda-data.eu/codelist/unit-measure/<value>");
        property.setHint("Expects a Eurostat measurement unit code. e.g. pc_emp, pc_ent, pc_turn, etc.");
        qbObservation.addProperty(property, true, "unit", "unitMeasure", "unitCode");

        // Reference area.
        property = new ObjectProperty("http://semantic.digital-agenda-data.eu/def/property/ref-area", "refArea", "ref-area",
                ObjectProperty.Range.RESOURCE);
        property.setValueTemplate("http://eurostat.linked-statistics.org/dic/geo#<value>");
        property.setHint("Expects a two letter country code as in ISO 3166-1 alpha-2 standard. e.g. AT, BE, DE, etc.");
        qbObservation.addProperty(property, true, "refArea", "country", "countryCode");

        // Time period.
        property = new ObjectProperty("http://semantic.digital-agenda-data.eu/def/property/time-period", "timePeriod",
                "time-period", ObjectProperty.Range.RESOURCE);
        property.setValueTemplate("http://reference.data.gov.uk/id/gregorian-year/<value>");
        property.setHint("Expects a 4-digit notation of a calendar year. e.g. 1999, 2000, 2001, etc.");
        qbObservation.addProperty(property, true, "timePeriod", "year", "time");

        // Observed value.
        property = new ObjectProperty("http://purl.org/linked-data/sdmx/2009/measure#obsValue", "obsValue", "obsValue",
                ObjectProperty.Range.LITERAL);
        property.setDataType(XMLSchema.DOUBLE.stringValue());
        property.setValueTemplate("<value>");
        property.setHint("Expects an Observation's measured value, as a number. e.g. 0.789, 0.018, 1000, 4.324, etc.");
        qbObservation.addProperty(property, true, "value", "observedValue", "obsValue");

        // Note.
        property = new ObjectProperty("http://semantic.digital-agenda-data.eu/def/property/note", "note", "note",
                ObjectProperty.Range.LITERAL);
        property.setDataType(XMLSchema.STRING.stringValue());
        property.setValueTemplate("<value>");
        property.setHint("Expects any text that servers as a comment/note to the observation.");
        qbObservation.addProperty(property, false, "note", "notes", "comment", "comments");

        // Flag.
        property = new ObjectProperty("http://semantic.digital-agenda-data.eu/def/property/flag", "flag", "flag",
                ObjectProperty.Range.RESOURCE);
        property.setValueTemplate("http://eurostat.linked-statistics.org/dic/flags#<value>");
        property.setHint("Expects a flag indicating the obsevration's status as in "
                + "http://eurostat.linked-statistics.org/dic/flags. e.g. u, n, p. r, etc.");
        qbObservation.addProperty(property, false, "flag", "status", "statusFlag", "flagStatus", "flags");

        return qbObservation;
    }

    /**
     *
     * @return
     */
    private static ObjectType createDesiObservationType() {

        ObjectType qbObservation = new ObjectType("http://purl.org/linked-data/cube#Observation", "DESI observation", DSD.DESI);
        String objUriTempl =
                "http://semantic.digital-agenda-data.eu/data/<dataSet>/<indicator>/<breakdown>/<unit>/<refArea>/<timePeriod>";
        qbObservation.setObjectUriTemplate(objUriTempl);
        qbObservation.setDatasetUriTemplate("http://semantic.digital-agenda-data.eu/dataset/DESI");

        // dataSet.
        ObjectProperty property =
                new ObjectProperty("http://purl.org/linked-data/cube#dataSet", "dataSet", "dataSet", ObjectProperty.Range.RESOURCE);
        property.setValueTemplate("http://semantic.digital-agenda-data.eu/dataset/DESI");
        property.setHint("Expects a Eurostat dataset code. e.g. aact_ali01, edat_aes_l11, fish_ld_nl, etc.");

        // Indicator.
        property = new ObjectProperty("http://semantic.digital-agenda-data.eu/def/property/indicator", "indicator", "indicator",
                ObjectProperty.Range.RESOURCE);
        property.setValueTemplate("http://semantic.digital-agenda-data.eu/codelist/indicator/<value>");
        property.setHint("Expects a Eurostat indicator code. e.g. p_siscall, p_cuse2, etc.");
        qbObservation.addProperty(property, true, "variable", "indicator", "indicatorCode", "indic");

        // Breakdown.
        property = new ObjectProperty("http://semantic.digital-agenda-data.eu/def/property/breakdown", "breakdown", "breakdown",
                ObjectProperty.Range.RESOURCE);
        property.setValueTemplate("http://semantic.digital-agenda-data.eu/codelist/breakdown/<value>");
        property.setHint("Expects a Eurostat breakdown code. e.g. 10_bb, 10_c10, etc.");
        qbObservation.addProperty(property, true, "breakdown", "brkDown", "brkdwn", "breakdownCode", "brkDownCode", "brkdwnCode");

        // Unit.
        property = new ObjectProperty("http://semantic.digital-agenda-data.eu/def/property/unit-measure", "unit", "unit",
                ObjectProperty.Range.RESOURCE);
        property.setValueTemplate("http://semantic.digital-agenda-data.eu/codelist/unit-measure/<value>");
        property.setHint("Expects a Eurostat measurement unit code. e.g. pc_emp, pc_ent, pc_turn, etc.");
        qbObservation.addProperty(property, true, "unit", "unitMeasure", "unitCode");

        // Reference area.
        property = new ObjectProperty("http://semantic.digital-agenda-data.eu/def/property/ref-area", "refArea", "ref-area",
                ObjectProperty.Range.RESOURCE);
        property.setValueTemplate("http://eurostat.linked-statistics.org/dic/geo#<value>");
        property.setHint("Expects a two letter country code as in ISO 3166-1 alpha-2 standard. e.g. AT, BE, DE, etc.");
        qbObservation.addProperty(property, true, "refArea", "country", "countryCode");

        // Time period.
        property = new ObjectProperty("http://semantic.digital-agenda-data.eu/def/property/time-period", "timePeriod",
                "time-period", ObjectProperty.Range.RESOURCE);
        property.setValueTemplate("http://reference.data.gov.uk/id/gregorian-year/<value>");
        property.setHint("Expects a 4-digit notation of a calendar year. e.g. 1999, 2000, 2001, etc.");
        qbObservation.addProperty(property, true, "timePeriod", "year", "time");

        // Observed value.
        property = new ObjectProperty("http://purl.org/linked-data/sdmx/2009/measure#obsValue", "obsValue", "obsValue",
                ObjectProperty.Range.LITERAL);
        property.setDataType(XMLSchema.DOUBLE.stringValue());
        property.setValueTemplate("<value>");
        property.setHint("Expects an Observation's measured value, as a number. e.g. 0.789, 0.018, 1000, 4.324, etc.");
        qbObservation.addProperty(property, true, "value", "observedValue", "obsValue");

        // Note.
        property = new ObjectProperty("http://semantic.digital-agenda-data.eu/def/property/note", "note", "note",
                ObjectProperty.Range.LITERAL);
        property.setDataType(XMLSchema.STRING.stringValue());
        property.setValueTemplate("<value>");
        property.setHint("Expects any text that servers as a comment/note to the observation.");
        qbObservation.addProperty(property, false, "note", "notes", "comment", "comments");

        // Flag.
        property = new ObjectProperty("http://semantic.digital-agenda-data.eu/def/property/flag", "flag", "flag",
                ObjectProperty.Range.RESOURCE);
        property.setValueTemplate("http://eurostat.linked-statistics.org/dic/flags#<value>");
        property.setHint("Expects a flag indicating the obsevration's status as in "
                + "http://eurostat.linked-statistics.org/dic/flags. e.g. u, n, p. r, etc.");
        qbObservation.addProperty(property, false, "flag", "status", "statusFlag", "flagStatus", "flags");

        return qbObservation;
    }

    /**
     *
     * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
     */
    public static enum DSD {

        SCOREBOARD("http://semantic.digital-agenda-data.eu/def/dsd/scoreboard"), DESI("http://semantic.digital-agenda-data.eu/def/dsd/DESI");

        /** **/
        private String uri;

        /**
         * @param uri
         */
        DSD(String uri) {
            this.uri = uri;
        }

        /**
         * @return the uri
         */
        public String getUri() {
            return uri;
        }
    }
}
