package eionet.cr.test.helpers;

import java.io.File;
import java.net.URL;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Utility methods for dealing with Jetty's Java API.
 *
 * @author Jaanus
 */
public class JettyUtil {

    /**
     * Creates and starts a Jetty mock server on localhost, listening on the given port and the given web-app context path.
     * The given resource file's folder will be used as the server's resource base. It will be looked up via classpath.
     *
     * @param port Given port.
     * @param contextPath Any imaginary context path, must start with "/". Use simply "/" for root context.
     * @param resourceFileName The name of the resource file whose folder will be used as server's resource base.
     * @return The started server's instance.
     * @throws Exception If any error happens.
     */
    public static Server startResourceServerMock(int port, String contextPath, String resourceFileName) throws Exception {

        // Derive the given resource file's folder name.
        ClassLoader classLoader = JettyUtil.class.getClassLoader();
        URL resourceFileURL = classLoader.getResource(resourceFileName);
        String resourceFolderPath = new File(resourceFileURL.toURI()).getParent();

        // Start Jetty server on given port
        Server server = new Server(port);
        server.setStopAtShutdown(true);

        // Set up the web-app context, using above-derived resource file folder as resource base.
        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath(contextPath);
        webAppContext.setResourceBase(resourceFolderPath);
        webAppContext.setClassLoader(classLoader);
        server.addHandler(webAppContext);
        server.start();

        return server;
    }

    /**
     * Null-safe method for closing the given Jetty server instance.
     *
     * @param server The server instance.
     * @throws Exception If any error happens.
     */
    public static void close(Server server) throws Exception {
        if (server != null) {
            server.stop();
        }
    }
}
