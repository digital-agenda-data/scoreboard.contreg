package eionet.cr.dao.readers;

import eionet.cr.common.CRException;

/**
 *
 * @author jaanus
 *
 */
public class ResultSetReaderException extends CRException {

    /**  */
    private static final long serialVersionUID = -4972271259022517448L;

    /**
     *
     */
    public ResultSetReaderException() {
        super();
    }

    /**
     *
     * @param message
     */
    public ResultSetReaderException(String message) {
        super(message);
    }

    /**
     *
     * @param message
     * @param cause
     */
    public ResultSetReaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
