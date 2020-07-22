package eionet.cr.util.export;

import eionet.cr.common.CRRuntimeException;
import org.apache.commons.lang.math.NumberUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public enum CodelistExportType {

    GEO("http://eurostat.linked-statistics.org/dic/geo"),
    FLAGS("http://eurostat.linked-statistics.org/dic/flags");

    public static final String MAPPING_FILE_EXTENSION = "mapping";
    public static final String SPREADSHEET_FILE_EXTENSION = "xls";

    private static final String mappingFileNameFormat = "codelist-{0}-export-mappings.properties";
    private static final String spreadsheetTemplateFileNameFormat = "codelist-{0}-export-template.xls";

    private String codelistUri;

    CodelistExportType(String codelistUri) {
        this.codelistUri = codelistUri;
    }

    public String getCodelistUri() {
        return codelistUri;
    }

    public Map<String, Integer> getPropertiesToColumnsMapping() {

        String fileName = MessageFormat.format(mappingFileNameFormat, name().toLowerCase());

        try {
            Properties props = new Properties();
            props.loadFromXML(getClass().getClassLoader().getResourceAsStream(fileName));

            Map<String, Integer> map = new HashMap<>();
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                int columnPosition = NumberUtils.toInt(entry.getValue().toString(), -1);
                if (columnPosition != -1) {
                    map.put(entry.getKey().toString(), columnPosition);
                }
            }

            return map;
        } catch (IOException e) {
            throw new CRRuntimeException("Problem loading export mapping from : " + fileName, e);
        }
    }

    public File getSpreadsheetTemplateFile() {

        String fileName = MessageFormat.format(spreadsheetTemplateFileNameFormat, name().toLowerCase());
        URL mappingTemplateURL = getClass().getClassLoader().getResource(fileName);
        if (mappingTemplateURL == null) {
            throw new CRRuntimeException("Could not locate codelist export template file by the name of " + fileName);
        }

        try {
            return new File(mappingTemplateURL.toURI());
        } catch (URISyntaxException e) {
            throw new CRRuntimeException("Invalid template URI: " + mappingTemplateURL, e);
        }
    }

    public static CodelistExportType getByCodelistUri(String codelistUri) {

        CodelistExportType[] values = CodelistExportType.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getCodelistUri().equals(codelistUri)) {
                return values[i];
            }
        }

        return null;
    }
}
