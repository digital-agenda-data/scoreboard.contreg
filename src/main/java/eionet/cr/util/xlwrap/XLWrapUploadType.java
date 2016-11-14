package eionet.cr.util.xlwrap;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.lang.WordUtils;

import eionet.cr.common.CRRuntimeException;
import eionet.cr.common.Subjects;

public enum XLWrapUploadType {

    // @formatter:on
    INDICATOR("Indicators metadata", "File containing metadata of the Digital Agenda Scoreboard indicators"),
    INDICATOR_GROUP("Indicator groups metadata", "File containing metadata of the Digital Agenda Scoreboard indicator groups"),
    BREAKDOWN("Breakdowns metadata", "File containing metadata of the Digital Agenda Scoreboard breakdowns"),
    BREAKDOWN_GROUP("Breakdown groups metadata", "File containing metadata of the Digital Agenda Scoreboard breakdown groups"),
    UNIT_MEASURE("Units metadata", "File containing metadata of the Digital Agenda Scoreboard units"),
    SOURCE("Data sources metadata", "File containing metadata of the Digital Agenda Scoreboard data sources"),
    OBSERVATION_SCOREBOARD("Observations: Scoreboard", "File containing Digital Agenda Scoreboard observations", "", Subjects.DATACUBE_OBSERVATION),
    OBSERVATION_DESI("Observations: DESI", "File containing DESI observations", "", Subjects.DATACUBE_OBSERVATION);

    // @formatter:off
    /** */
    private static final String GRAPH_TEMPLATE = "http://semantic.digital-agenda-data.eu/codelist/@type@/";
    private static final String SUBJECTS_TYPE_TEMPLATE = "http://semantic.digital-agenda-data.eu/def/class/@type@";

    /** */
    public static final String MAPPING_FILE_EXTENSION = "trig";
    public static final String SPREADSHEET_FILE_EXTENSION = "xls";
    public static final String SPREADSHEETS_PATH = "/spreadsheets/";
    public static final String TRIG_FILES__PATH = "/trig/";

    /** */
    private String title;
    private String hint;

    /** */
    private String graphUri;
    private String subjectsTypeUri;

    /** */
    private File mappingTemplate;
    private File spreadsheetTemplate;

    /**
     * Basic constructor.
     *
     * @param title
     * @param hint
     */
    private XLWrapUploadType(String title, String hint) {

        // Prepare title and hint.

        this.title = title;
        this.hint = hint;

        // Prepare target graph URI.

        String normalizedName = name().toLowerCase().replace('_', '-');
        this.graphUri = GRAPH_TEMPLATE.replace("@type@", normalizedName);

        // Prepare subjects type URI.

        char[] delims = {'_'};
        String camelCaseName = WordUtils.capitalizeFully(name().toLowerCase(), delims).replace("_", "");
        this.subjectsTypeUri = SUBJECTS_TYPE_TEMPLATE.replace("@type@", camelCaseName);

        // Prepare Trig mapping template file reference.

        String mappingTemplateFileName = TRIG_FILES__PATH + normalizedName + "." + MAPPING_FILE_EXTENSION;
        URL mappingTemplateURL = getClass().getClassLoader().getResource(mappingTemplateFileName);
        if (mappingTemplateURL == null) {
            throw new CRRuntimeException("Could not locate mapping template by the name of " + mappingTemplateFileName);
        }
        try {
            this.mappingTemplate = new File(mappingTemplateURL.toURI());
        } catch (URISyntaxException e) {
            throw new CRRuntimeException("Invalid mapping template URI: " + mappingTemplateURL, e);
        }

        // Prepare download spreadsheet template file reference.

        String spreadsheetTemplateFileName = SPREADSHEETS_PATH + normalizedName + "." + SPREADSHEET_FILE_EXTENSION;
        URL spreadsheetTemplateURL = getClass().getClassLoader().getResource(spreadsheetTemplateFileName);
        if (spreadsheetTemplateURL == null) {
            throw new CRRuntimeException("Could not locate spreadsheet template by the name of " + spreadsheetTemplateFileName);
        }
        try {
            this.spreadsheetTemplate = new File(spreadsheetTemplateURL.toURI());
        } catch (URISyntaxException e) {
            throw new CRRuntimeException("Invalid spreadsheet template URI: " + spreadsheetTemplateURL, e);
        }
    }

    /**
     * Calls basic constructor and sets the graph URI and the subjects type URI to the ones given in this constructor input.
     *
     * @param title
     * @param hint
     * @param graphUri
     * @param subjectsTypeUri
     */
    private XLWrapUploadType(String title, String hint, String graphUri, String subjectsTypeUri) {

        this(title, hint);
        this.graphUri = graphUri;
        this.subjectsTypeUri = subjectsTypeUri;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the hint
     */
    public String getHint() {
        return hint;
    }

    /**
     * @return the graphUri
     */
    public String getGraphUri() {
        return graphUri;
    }

    /**
     *
     * @return
     */
    public String getName() {
        return name();
    }

    /**
     * @return the mappingTemplate
     */
    public File getMappingTemplate() {
        return mappingTemplate;
    }

    /**
     * @return the subjectsTypeUri
     */
    public String getSubjectsTypeUri() {
        return subjectsTypeUri;
    }

    /**
     * @return the spreadsheetTemplate
     */
    public File getSpreadsheetTemplate() {
        return spreadsheetTemplate;
    }

    /**
     *
     * @param graphUri
     * @return
     */
    public static XLWrapUploadType getByGraphUri(String graphUri) {

        XLWrapUploadType[] values = XLWrapUploadType.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getGraphUri().equals(graphUri)) {
                return values[i];
            }
        }

        return null;
    }
}
