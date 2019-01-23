/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Content Registry 2.0.
 *
 * The Initial Owner of the Original Code is European Environment
 * Agency.  Portions created by Tieto Eesti are Copyright
 * (C) European Environment Agency.  All Rights Reserved.
 *
 * Contributor(s):
 * Jaanus Heinlaid, Tieto Eesti
 */
package eionet.cr.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

// TODO: Auto-generated Javadoc
/**
 *
 * @author <a href="mailto:jaanus.heinlaid@tietoenator.com">Jaanus Heinlaid</a>
 *
 */
public final class URIUtil {

    /** */
    private static HashSet<String> schemes;

    /**
     * Hide utility class constructor.
     */
    private URIUtil() {
        // Just an empty private constructor to avoid instantiating this utility class.
    }

    /**
     * Returns true if the following conditions are all met: - the given string is not null nor empty - the given string passes
     * <code>new java.net.URI(str)</code> without the <code>java.net.URISyntaxException</code> being thrown - the given string is an
     * <strong>absolute</strong> URI, meaning it has a scheme part specified - URI scheme in the given string matches one of those
     * specified by [RFC4395] (see http://www.iana.org/assignments/uri-schemes.html)
     *
     * Otherwise returns false.
     *
     * @param str
     * @return boolean
     */
    public static boolean isSchemedURI(String str) {

        if (Util.isNullOrEmpty(str)) {
            return false;
        }

        try {
            URI uri = new URI(str);
            if (!uri.isAbsolute()) {
                return false;
            }

            if (schemes == null) {
                initSchemes();
            }

            return schemes.contains(uri.getScheme());
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     *
     * @param str
     * @return
     */
    public static boolean isAbsoluteURI(String str) {

        if (Util.isNullOrEmpty(str)) {
            return false;
        }

        try {
            URI uri = new URI(str);
            return uri.isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     *
     * @param str
     * @return
     */
    public static boolean isURI(String str) {

        if (Util.isNullOrEmpty(str)) {
            return false;
        }

        try {
            URI uri = new URI(str);
            return uri != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     *
     * @param uri
     * @return String
     */
    public static String extractURILabel(String uri) {

        String result = null;
        if (URLUtil.isURL(uri)) {

            int i = Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/'));
            if (i >= 0) {
                result = uri.substring(i + 1);
                if (result.trim().length() == 0) {
                    result = null;
                }
            }
        }

        return result;
    }

    /**
     *
     * @param uri
     * @param dflt
     * @return String
     */
    public static String extractURILabel(String uri, String dflt) {

        String result = null;
        if (URLUtil.isURL(uri)) {

            int i = Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/'));
            if (i >= 0) {
                result = uri.substring(i + 1);
                if (result.trim().length() == 0) {
                    result = null;
                }
            }
        } else if (URIUtil.isAbsoluteURI(uri)) {
            int i = uri.lastIndexOf(':');
            if (i >= 0) {
                result = uri.substring(i + 1);
                if (result.trim().length() == 0) {
                    result = null;
                }
            }
        }

        return result == null ? dflt : result;
    }

    /**
     *
     */
    private static synchronized void initSchemes() {

        schemes = new HashSet<String>();

        // schemes as specified by [RFC4395] (see http://www.iana.org/assignments/uri-schemes.html)
        schemes.add("aaa");
        schemes.add("aaas");
        schemes.add("acap");
        schemes.add("afs");
        schemes.add("cap");
        schemes.add("cid");
        schemes.add("crid");
        schemes.add("data");
        schemes.add("dav");
        schemes.add("dict");
        schemes.add("dns");
        schemes.add("dtn");
        schemes.add("fax");
        schemes.add("file");
        schemes.add("ftp");
        schemes.add("go");
        schemes.add("gopher");
        schemes.add("h323");
        schemes.add("http");
        schemes.add("https");
        schemes.add("iax");
        schemes.add("icap");
        schemes.add("im");
        schemes.add("imap");
        schemes.add("info");
        schemes.add("ipp");
        schemes.add("iris");
        schemes.add("iris.beep");
        schemes.add("iris.lwz");
        schemes.add("iris.xpc");
        schemes.add("iris.xpcs");
        schemes.add("ldap");
        schemes.add("mailserver");
        schemes.add("mailto");
        schemes.add("mid");
        schemes.add("modem");
        schemes.add("msrp");
        schemes.add("msrps");
        schemes.add("mtqp");
        schemes.add("mupdate");
        schemes.add("news");
        schemes.add("nfs");
        schemes.add("nntp");
        schemes.add("opaquelocktoken");
        schemes.add("pack");
        schemes.add("pop");
        schemes.add("pres");
        schemes.add("prospero");
        schemes.add("rtsp");
        schemes.add("service");
        schemes.add("shttp");
        schemes.add("sip");
        schemes.add("sips");
        schemes.add("snmp");
        schemes.add("soap.beep");
        schemes.add("soap.beeps");
        schemes.add("z39.50r");
        schemes.add("z39.50s");
        schemes.add("tag");
        schemes.add("tel");
        schemes.add("telnet");
        schemes.add("tftp");
        schemes.add("thismessage");
        schemes.add("tip");
        schemes.add("tn3270");
        schemes.add("tv");
        schemes.add("urn");
        schemes.add("wais");
        schemes.add("vemmi");
        schemes.add("xmlrpc.beep");
        schemes.add("xmlrpc.beeps");
        schemes.add("xmpp");
    }
}
