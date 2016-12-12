package eionet.cr.util.odp;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class ODPDataset {

    /** */
    private int id;
    private String uri;
    private String identifier;
    private String title;
    private String description;
    private String issued;
    private String modified;
    private String periodStart;
    private List<String> spatialUris;

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }
    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }
    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }
    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }
    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     * @return the modified
     */
    public String getModified() {
        return modified;
    }
    /**
     * @param modified the modified to set
     */
    public void setModified(String modified) {
        this.modified = modified;
    }
    /**
     * @return the issued
     */
    public String getIssued() {
        return issued;
    }
    /**
     * @param issued the issued to set
     */
    public void setIssued(String issued) {
        this.issued = issued;
    }
    /**
     * @return the periodStart
     */
    public String getPeriodStart() {
        return periodStart;
    }
    /**
     * @param periodStart the periodStart to set
     */
    public void setPeriodStart(String periodStart) {
        this.periodStart = periodStart;
    }
    /**
     * @return the spatialUris
     */
    public List<String> getSpatialUris() {
        return spatialUris;
    }
    /**
     * @param spatialUris the spatialUris to set
     */
    public void setSpatialUris(List<String> spatialUris) {
        this.spatialUris = spatialUris;
    }

    public static void main(String[] args) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        ODPDataset ds = new ODPDataset();
        ds.setId(1);
        ds.setIdentifier("");
        System.out.println(ds.getIdentifier());

        PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(ds);
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            String name = propertyDescriptor.getName();
            Class<?> clazz = propertyDescriptor.getPropertyType();
            //System.out.println(name + " = " + clazz);

            if (clazz.equals(String.class)) {
                Object strValue = PropertyUtils.getProperty(ds, name);
                if (strValue != null) {
                    PropertyUtils.setProperty(ds, name, StringUtils.trimToNull(strValue.toString()));
                }
            }
        }

        System.out.println(ds.getIdentifier());

    }
    /**
     * @return the id
     */
    public int getId() {
        return id;
    }
    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }
}
