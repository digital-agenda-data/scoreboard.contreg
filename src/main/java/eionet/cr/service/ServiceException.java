package eionet.cr.service;

import eionet.cr.common.CRException;

/**
 *
 * Type definition ...
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class ServiceException extends CRException {

    /**  */
    private static final long serialVersionUID = -7391771317010851296L;

    /**
     *
     */
    public ServiceException() {
        super();
    }

    /**
     * @param msg
     */
    public ServiceException(String msg) {
        super(msg);
    }

    /**
     * @param msg
     * @param cause
     */
    public ServiceException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * @param cause
     */
    public ServiceException(Throwable cause) {
        super(cause);
    }
}
