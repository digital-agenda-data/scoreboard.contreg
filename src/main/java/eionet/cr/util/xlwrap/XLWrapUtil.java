package eionet.cr.util.xlwrap;

import at.jku.xlwrap.common.XLWrapException;
import at.jku.xlwrap.exec.XLWrapMaterializer;
import at.jku.xlwrap.map.MapTemplate;
import at.jku.xlwrap.map.MappingParser;
import at.jku.xlwrap.map.XLWrapMapping;
import at.jku.xlwrap.spreadsheet.Sheet;
import at.jku.xlwrap.spreadsheet.Workbook;
import at.jku.xlwrap.spreadsheet.WorkbookFactory;
import at.jku.xlwrap.spreadsheet.XLWrapEOFException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import eionet.cr.common.CRRuntimeException;
import eionet.cr.common.TempFilePathGenerator;
import eionet.cr.util.FileDeletionJob;
import eionet.cr.util.Pair;
import eionet.cr.util.jena.JenaUtil;
import jxl.JXLException;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.openrdf.OpenRDFException;
import org.openrdf.rio.RDFHandler;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

/**
 * Utility class for importing an MS Excel or OpenDocument spreadsheet into the RDF model and triple store, using a given
 * Spreadsheet-to-RDF mapping. The library used is XLWrap (http://xlwrap.sourceforge.net/) and the mapping must be in TriG syntax:
 * http://wifo5-03.informatik.uni-mannheim.de/bizer/trig/.
 *
 * @author jaanus
 */
public class XLWrapUtil {

    /** */
    private static final String XLWRAP_NAMESPACE = "http://purl.org/NET/xlwrap#";
    private static final String[] SPREADSHEET_COLS = {"A2", "B2", "C2", "D2", "E2", "F2", "G2", "H2", "I2", "J2", "K2", "L2",
            "M2", "N2", "O2", "P2", "Q2", "R2", "S2", "T2", "U2", "V2", "W2", "X2", "Y2", "Z2"};

    /** */
    private static final String FILE_URL_PLACEHOLDER = "@FILE_URL@";
    private static final String DATASET_IDENTIFIER_PLACEHOLDER = "@DATASET_IDENTIFIER@";

    /**
     * Disable utility class constructor.
     */
    private XLWrapUtil() {
        // Empty constructor.
    }

    /**
     *
     * @param uploadType
     * @param spreadsheetFile
     * @param targetDataset
     * @param clear
     * @return
     * @throws IOException
     * @throws XLWrapException
     * @throws OpenRDFException
     */
    public static int importMapping(XLWrapUploadType uploadType, File spreadsheetFile, String targetDataset, boolean clear)
            throws IOException, XLWrapException, OpenRDFException {
        return importMapping(uploadType, spreadsheetFile, targetDataset, clear, null);
    }

    /**
     *
     * @param uploadType
     * @param spreadsheetFile
     * @param targetDataset
     * @param clear
     * @param stmtListener
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws XLWrapException
     * @throws OpenRDFException
     */
    public static int importMapping(XLWrapUploadType uploadType, File spreadsheetFile, String targetDataset, boolean clear,
            RDFHandler stmtListener) throws IOException, XLWrapException, OpenRDFException {

        File template = uploadType.getMappingTemplate();
        File target = TempFilePathGenerator.generate(XLWrapUploadType.MAPPING_FILE_EXTENSION);

        try {
            Properties properties = new Properties();
            properties.setProperty(FILE_URL_PLACEHOLDER, spreadsheetFile.toURI().toURL().toString());
            if (StringUtils.isNotBlank(targetDataset)) {
                String datasetIdentifier = StringUtils.substringAfterLast(targetDataset, "/");
                if (StringUtils.isBlank(datasetIdentifier)) {
                    datasetIdentifier = "unknown_dataset";
                }
                properties.setProperty(DATASET_IDENTIFIER_PLACEHOLDER, datasetIdentifier);
            }

            createMappingFile(template, target, properties);
            String graphUri = StringUtils.isBlank(targetDataset) ? uploadType.getGraphUri() : targetDataset;
            return importMapping(target, graphUri, clear, stmtListener);
        } finally {
            FileDeletionJob.register(target);
        }
    }

    /**
     *
     * @param mappingFile
     * @param graphUri
     * @param clearGraph
     * @param stmtListener
     * @return
     * @throws IOException
     * @throws IOException
     * @throws XLWrapException
     * @throws OpenRDFException
     */
    public static int importMapping(File mappingFile, String graphUri, boolean clearGraph, RDFHandler stmtListener)
            throws IOException, XLWrapException, OpenRDFException {

        return importMapping(mappingFile.toURI().toURL(), graphUri, clearGraph, stmtListener);
    }

    /**
     *
     * @param mappingFileURL
     * @param graphUri
     * @param clearGraph
     * @param stmtListener
     * @return
     * @throws IOException
     * @throws XLWrapException
     * @throws OpenRDFException
     */
    public static int importMapping(URL mappingFileURL, String graphUri, boolean clearGraph, RDFHandler stmtListener)
            throws IOException, XLWrapException, OpenRDFException {

        Model model = null;
        try {
            XLWrapMapping mapping = MappingParser.parse(mappingFileURL.toString());
            XLWrapMaterializer materializer = new XLWrapMaterializer();
            model = materializer.generateModel(mapping);
            Pair<Integer, Integer> saveResult = JenaUtil.saveModel(model, graphUri, clearGraph, stmtListener);
            Integer resourceCount = saveResult.getRight();
            return resourceCount;
        } finally {
            JenaUtil.close(model);
        }
    }

    /**
     *
     * @param template
     * @param target
     * @param replacements
     * @return
     * @throws IOException
     */
    private static File createMappingFile(File template, File target, Properties replacements) throws IOException {

        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(template));
            writer = new BufferedWriter(new FileWriter(target));

            String line = null;
            boolean replaceTokens = replacements != null && !replacements.isEmpty();
            while ((line = reader.readLine()) != null) {
                if (replaceTokens) {
                    for (Entry<Object, Object> entry : replacements.entrySet()) {
                        line = line.replace(entry.getKey().toString(), entry.getValue().toString());
                    }
                }

                writer.write(line);
                writer.write(IOUtils.LINE_SEPARATOR);
            }
        } finally {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(reader);
        }

        return target;
    }

    /**
     * Parses the given Trig (http://wifo5-03.informatik.uni-mannheim.de/bizer/trig/) file, gets the first template mapping (see
     * http://xlwrap.sourceforge.net/ for more background), and from that extracts a mapping of RDF properties to spreadsheet
     * columns. The extracted mapping is returned as a map where keys represent the RDF properties and teh values represent the
     * 0-based indexes of corresponding spreadsheet columns.
     *
     * @param trigFile The Trig file to parse.
     * @return The map as described above.
     * @throws IOException When any sort of I/O error occurs
     * @throws XLWrapException
     */
    public static Map<String, Integer> getPropsToSpreadsheetCols(File trigFile) throws IOException, XLWrapException {

        if (trigFile == null || !trigFile.exists() || !trigFile.isFile()) {
            throw new IllegalArgumentException("The given Trig file must not be null and it must exits!");
        }

        HashMap<String, Integer> map = new HashMap<String, Integer>();
        String trigFileUrl = trigFile.toURI().toURL().toString();
        XLWrapMapping mapping = MappingParser.parse(trigFileUrl);
        Iterator<MapTemplate> mapTemplates = mapping.getMapTemplatesIterator();
        while (mapTemplates.hasNext()) {
            Model templateModel = mapTemplates.next().getTemplateModel();
            if (templateModel != null && !templateModel.isEmpty()) {

                StmtIterator statements = templateModel.listStatements();
                while (statements.hasNext()) {

                    Statement statement = statements.next();

                    String predicateUri = statement.getPredicate().getURI();
                    if (!predicateUri.startsWith(XLWRAP_NAMESPACE)) {

                        RDFNode object = statement.getObject();
                        String objectStr =
                                object.isLiteral() ? "\"" + object.asLiteral().getString() + "\"" : object.asResource().getURI();

                        if (StringUtils.isNotBlank(objectStr)) {
                            for (int i = 0; i < SPREADSHEET_COLS.length; i++) {
                                if (objectStr.contains(SPREADSHEET_COLS[i])) {
                                    map.put(predicateUri, i);
                                    break;
                                }
                            }
                        }
                    }
                }
                break;
            }
        }

        return map;
    }

    /**
     *
     * @param xlWrapUploadType
     * @return
     * @throws XLWrapException
     * @throws XLWrapEOFException
     */
    public static List<String> getTemplateColumnNames(XLWrapUploadType xlWrapUploadType) throws XLWrapException, XLWrapEOFException {

        File spreadsheetTemplate = xlWrapUploadType.getSpreadsheetTemplate();
        Workbook workbook = WorkbookFactory.getWorkbook(spreadsheetTemplate.toURI().toString());
        if (workbook == null) {
            throw new CRRuntimeException("Unable to find such a workbook: " + spreadsheetTemplate);
        }

        try {
            List<String> resultList = new ArrayList<>();
            Sheet sheet = workbook.getSheet(0);
            int colCount = sheet.getColumns();
            for (int i = 0; i < colCount; i++) {
                resultList.add(sheet.getCell(i, 0).getText());
            }

            return resultList;
        } finally {
            workbook.close();
        }
    }

    /**
     *
     * @param itemType
     * @param itemColumnValues
     * @return
     * @throws IOException
     * @throws XLWrapException
     * @throws OpenRDFException
     */
    public static int importCodelistItem(XLWrapUploadType itemType, List<String> itemColumnValues)
            throws IOException, XLWrapException, OpenRDFException, JXLException {

        if (CollectionUtils.isEmpty(itemColumnValues)) {
            return 0;
        }

        jxl.Workbook templateWorkbook = jxl.Workbook.getWorkbook(itemType.getSpreadsheetTemplate());

        File targetFile = TempFilePathGenerator.generate(XLWrapUploadType.SPREADSHEET_FILE_EXTENSION);
        WritableWorkbook targetWorkbook = null;
        try {
            targetWorkbook = jxl.Workbook.createWorkbook(targetFile, templateWorkbook);
            WritableSheet sheet = targetWorkbook.getSheet(0);
            for (int col = 0; col < itemColumnValues.size(); col++) {

                String colValue = itemColumnValues.get(col);
                Label label = new Label(col, 1, StringUtils.trimToEmpty(colValue));
                sheet.addCell(label);
            }

            targetWorkbook.write();
            targetWorkbook.close();
            return importMapping(itemType, targetFile, null, false);
        } finally {
            if (targetWorkbook != null) {
                try { targetWorkbook.close(); } catch (Exception e) {}
            }
            FileDeletionJob.register(targetFile);
        }
    }
}
