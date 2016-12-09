package eionet.cr.util.odp;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

/**
 *
 * Type definition ...
 *
 * @author Jaanus
 */
public enum ODPAction {

    // @formatter:off

    ADD_DRAFT("Add/replace with status Draft", "add-replace", "draft"),
    ADD_PUBLISHED("Add/replace with status Published", "add-replace", "published"),
    SET_DRAFT("Set status to Draft", "change-status", "draft"),
    SET_PUBLISHED("Set status to Published", "change-status", "published"),
    REMOVE("Remove", "remove", null);

    // @formatter:on

    /** */
    private static final char[] WORD_SEPARATOR = {'_'};

    /** */
    private String label;
    private String nameCamelCase;
    private String tagLocalName;
    private String objectStatus;

    /**
     * @param label
     */
    ODPAction(String label, String tagLocalName, String objectStatus) {

        this.label = label;
        this.tagLocalName = tagLocalName;
        this.objectStatus = objectStatus;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     *
     * @return
     */
    public String getIdPrefix() {
        return getNameCamelCase();
    }

    /**
     * Returns the enum's name in camel case, with lower-case first letter.
     *
     * @return The result.
     */
    public String getNameCamelCase() {

        if (nameCamelCase == null) {
            String fullyCapitalized = WordUtils.capitalizeFully(name(), WORD_SEPARATOR);
            String separatorsRemoved = StringUtils.remove(fullyCapitalized, WORD_SEPARATOR[0]);
            nameCamelCase = StringUtils.uncapitalize(separatorsRemoved);
        }
        return nameCamelCase;
    }

    /**
     * @return the tagLocalName
     */
    public String getTagLocalName() {
        return tagLocalName;
    }

    /**
     * @return the objectStatus
     */
    public String getObjectStatus() {
        return objectStatus;
    }
}
