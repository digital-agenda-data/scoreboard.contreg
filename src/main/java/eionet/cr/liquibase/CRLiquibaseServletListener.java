package eionet.cr.liquibase;

import javax.servlet.ServletContextEvent;

import liquibase.database.DatabaseFactory;
import liquibase.integration.servlet.LiquibaseServletListener;

import org.apache.log4j.Logger;

import eionet.liquibase.VirtuosoDatabase;

/**
 * An extension of {@link LiquibaseServletListener} to be instantiated from <listener> tag of web.xml.
 *
 * The idea is to register {@link VirtuosoDatabase} in Liquibase's {@link DatabaseFactory} before proceeding to
 * {@link LiquibaseServletListener#contextInitialized(ServletContextEvent)}.
 *
 * @author Jaanus Heinlaid <jaanus.heinlaid@gmail.com>
 */
public class CRLiquibaseServletListener extends LiquibaseServletListener {

    /** Static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(CRLiquibaseServletListener.class);

    /*
     * (non-Javadoc)
     *
     * @see liquibase.integration.servlet.LiquibaseServletListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        LOGGER.info("Initializing " + getClass().getSimpleName());

        DatabaseFactory.getInstance().register(new eionet.liquibase.VirtuosoDatabase());
        super.contextInitialized(servletContextEvent);
    }
}
