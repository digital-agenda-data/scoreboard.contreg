package eionet.cr.migration;

/**
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class DatasetMigrationException extends Exception {

    /**  */
    private static final long serialVersionUID = 882984811482400909L;

    /**
     */
    public DatasetMigrationException() {
        super();
    }

    /**
     * @param message
     */
    public DatasetMigrationException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public DatasetMigrationException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public DatasetMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
