package eionet.cr.util.odp;

/**
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class ODPManifestEntry {

    /** */
    private ODPAction odpAction;

    /** */
    private ODPDataset dataset;

    /**
     * @return the odpAction
     */
    public ODPAction getOdpAction() {
        return odpAction;
    }

    /**
     * @param odpAction the odpAction to set
     */
    public void setOdpAction(ODPAction odpAction) {
        this.odpAction = odpAction;
    }

    /**
     * @return the dataset
     */
    public ODPDataset getDataset() {
        return dataset;
    }

    /**
     * @param dataset the dataset to set
     */
    public void setDataset(ODPDataset dataset) {
        this.dataset = dataset;
    }
}
