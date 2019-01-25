package eionet.cr.web.util.tabs;

import org.apache.commons.lang.WordUtils;

public enum TabId {

    RESOURCE_PROPERTIES, RESOURCE_REFERENCES, OBJECTS_IN_SOURCE, SHOW_ON_MAP, BOOKMARKED_SPARQL("Bookmarked SPARQL"),
    COMPILED_DATASET, REVIEW_FOLDER("Reviews"), FOLDER("Contents"), BOOKMARKS, REGISTRATIONS, HISTORY, TABLE_FILE_CONTENTS(
            "CSV/TSV contents"), DATASET_INDICATORS("Dataset indicators");

    /** */
    private String title;

    /**
     *
     * Class constructor.
     */
    TabId() {
        title = WordUtils.capitalize(name().toLowerCase()).replace('_', ' ');
    }

    /**
     * @param title
     */
    TabId(String title) {

        if (title == null || title.trim().length() == 0) {
            throw new IllegalArgumentException("The title must not be blank!");
        }
        this.title = title;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }
}
