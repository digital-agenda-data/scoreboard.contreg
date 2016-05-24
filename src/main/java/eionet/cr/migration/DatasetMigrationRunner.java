package eionet.cr.migration;

import org.apache.log4j.Logger;

import eionet.cr.dto.DatasetMigrationDTO;
import eionet.cr.service.DatasetMigrationsService;
import eionet.cr.service.ServiceException;

/**
 * Thread that runs a given dataset migration.
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMigrationRunner extends Thread {

    /** */
    private static final Logger LOGGER = Logger.getLogger(DatasetMigrationRunner.class);

    /** */
    private int migrationId;
    private DatasetMigrationDTO migrationDTO;

    /**
     * @param migrationId
     * @throws ServiceException
     */
    public DatasetMigrationRunner(int migrationId) throws ServiceException {

        super();

        this.migrationDTO = DatasetMigrationsService.newInstance().findById(migrationId);
        if (this.migrationDTO == null) {
            throw new IllegalArgumentException("Failed to find a dataset migration object by this id: " + migrationId);
        }
        this.migrationId = migrationId;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {

        try {
            runInternal();
        } catch (Exception e) {
            LOGGER.error("Error when running dataset migration by this id: " + migrationId);
        }
    }

    /**
     * @throws InterruptedException
     *
     */
    private void runInternal() throws InterruptedException {

        LOGGER.debug("Started dataset migration by this id: " + migrationId);
        Thread.sleep(5000);
    }
}
