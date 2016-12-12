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

import java.beans.PropertyDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.quartz.CronExpression;

import eionet.cr.common.CRRuntimeException;
import eionet.cr.util.export.XmlUtil;
import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.UrlBinding;
import nl.bitwalker.useragentutils.Browser;
import nl.bitwalker.useragentutils.BrowserType;

/**
 * Utility methods.
 *
 * @author heinljab
 *
 */
public final class Util {

    /**
     * Constant equals to 1000.
     */
    private static final int MILLISECONDS_COUNT_IN_SECOND = 1000;

    /**
     * Private constructor to prevent public initiation.
     */
    private Util() {
        // Private constructor to prevent public initiation.
    }

    /**
     *
     * @param t
     * @return
     */
    public static String getStackTrace(Throwable t) {

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    /**
     *
     * @param array
     * @param separator
     * @return
     */
    public static String arrayToString(Object[] array, String separator) {

        if (array == null) {
            return null;
        }

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                buf.append(separator);
            }
            buf.append(array[i].toString());
        }
        return buf.toString();
    }

    /**
     *
     * @param date
     * @param datePattern
     * @return
     */
    public static String virtuosoDateToString(java.util.Date date) {

        if (date == null) {
            return null;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return dateFormat.format(date);
    }

    /**
     *
     * @param str
     * @return java.util.Date
     */
    public static java.util.Date virtuosoStringToDate(String str) {

        if (StringUtils.isBlank(str)) {
            return null;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        try {
            return dateFormat.parse(str);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     *
     * @param date
     * @param datePattern
     * @return
     */
    public static String dateToString(java.util.Date date, String datePattern) {

        if (date == null) {
            return null;
        }

        SimpleDateFormat formatter = datePattern == null ? new SimpleDateFormat() : new SimpleDateFormat(datePattern);
        return formatter.format(date);
    }

    /**
     *
     * @param str
     * @param datePattern
     * @return
     */
    public static java.util.Date stringToDate(String str, String datePattern) {

        if (str == null || str.trim().length() == 0) {
            return null;
        }

        SimpleDateFormat formatter = new SimpleDateFormat(datePattern);
        try {
            return formatter.parse(str);
        } catch (ParseException e) {
            throw new CRRuntimeException("Failed to convert the given string to java.util.Date: " + e.toString(), e);
        }
    }

    /**
     * Current time in seconds.
     *
     * @return current time in seconds.
     */
    public static long currentTimeSeconds() {
        return (System.currentTimeMillis() / MILLISECONDS_COUNT_IN_SECOND);
    }

    /**
     * Converts milliseconds to seconds.
     *
     * @param milliSeconds
     *            milliseconds
     * @return seconds
     */
    public static long getSeconds(final long milliSeconds) {
        return (milliSeconds / MILLISECONDS_COUNT_IN_SECOND);
    }

    /**
     * Returns true if given String is null or empty.
     *
     * @param str
     *            String
     * @return boolean if String is empty or null.
     */
    public static boolean isNullOrEmpty(final String str) {
        return str == null || str.length() == 0 || str.trim().length() == 0;
    }

    /**
     * Returns true if the given string has any whitespace in it, including the leading and trailing whitespace.
     *
     * @param s
     * @return
     */
    public static boolean hasWhiteSpace(String s) {

        if (s == null || s.length() != s.trim().length()) {
            return true;
        } else {
            StringTokenizer st = new StringTokenizer(s);
            int count = 0;
            for (; st.hasMoreTokens() && count < 2; count++) {
                st.nextToken();
            }
            return count > 1;
        }
    }

    /**
     *
     * @param array
     * @return
     */
    public static Object getFirst(Object[] array) {
        return array != null && array.length > 0 ? array[0] : null;
    }

    /**
     *
     * @param array
     * @return
     */
    public static String getFirst(String[] array) {
        return array != null && array.length > 0 ? array[0] : null;
    }

    /**
     *
     * @param array
     * @return
     */
    public static String[] pruneUrls(String[] array) {

        if (array == null || array.length == 0) {
            return array;
        }

        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < array.length; i++) {
            if (!URLUtil.isURL(array[i])) {
                list.add(array[i]);
            }
        }

        if (list.isEmpty()) {
            return array;
        } else {
            String[] result = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = list.get(i);
            }
            return result;
        }
    }

    /**
     *
     * @param pageContext
     * @param objectClass
     * @return
     */
    public static Object findInAnyScope(PageContext pageContext, Class objectClass) {

        if (pageContext == null || objectClass == null) {
            return null;
        }

        int[] scopes =
                {PageContext.APPLICATION_SCOPE, PageContext.PAGE_SCOPE, PageContext.REQUEST_SCOPE, PageContext.SESSION_SCOPE};
        for (int i = 0; i < scopes.length; i++) {
            Enumeration attrs = pageContext.getAttributeNamesInScope(scopes[i]);
            while (attrs != null && attrs.hasMoreElements()) {
                String name = (String) attrs.nextElement();
                Object o = pageContext.getAttribute(name, scopes[i]);
                if (o != null && objectClass.isInstance(o)) {
                    return o;
                }
            }
        }

        return null;
    }

    /**
     * Convenience method for URL-encoding the given string.
     *
     * @param s
     * @return
     */
    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new CRRuntimeException(e.toString(), e);
        }
    }

    /**
     * Convenience method for URL-decoding the given string.
     *
     * @param s
     * @return
     */
    public static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new CRRuntimeException(e.toString(), e);
        }
    }

    /**
     *
     * @param o
     * @return
     */
    public static Object[] toArray(Object o) {
        if (o == null) {
            return null;
        } else {
            Object[] oo = new Object[1];
            oo[0] = o;
            return oo;
        }
    }

    /**
     *
     * @param expression
     */
    public static boolean isValidQuartzCronExpression(String expression) {

        if (Util.isNullOrEmpty(expression)) {
            return false;
        } else {
            return CronExpression.isValidExpression(expression);
        }
    }

    /**
     *
     * @param coll
     * @return
     */
    public static String toCSV(Collection coll) {

        StringBuffer buf = new StringBuffer();
        if (coll != null) {
            for (Iterator it = coll.iterator(); it.hasNext();) {

                if (buf.length() > 0) {
                    buf.append(",");
                }
                buf.append(it.next());
            }
        }
        return buf.toString();
    }

    /**
     *
     * @param coll
     * @param surroundWith
     * @return
     */
    public static String toCSV(Collection coll, String surroundWith) {

        StringBuffer buf = new StringBuffer();
        if (coll != null) {
            for (Iterator it = coll.iterator(); it.hasNext();) {

                if (buf.length() > 0) {
                    buf.append(",");
                }
                buf.append(surroundWith).append(it.next()).append(surroundWith);
            }
        }
        return buf.toString();
    }

    /**
     *
     * @param coll
     * @return
     */
    public static String toCSVHashes(Collection coll) {

        StringBuffer buf = new StringBuffer();
        if (coll != null) {
            for (Iterator it = coll.iterator(); it.hasNext();) {

                if (buf.length() > 0) {
                    buf.append(",");
                }
                buf.append(Hashes.spoHash(it.next().toString()));
            }
        }
        return buf.toString();
    }

    /**
     * Create a comma-separated list of tag enclosed URIs. The result can be used in sparql queries. eg.: [uri1,uri2,uri3] is
     * transformed to <uri1>,<uri2>,<uri3>.
     *
     * @param uriList
     *            list of URIs
     * @return comma separated list of tag enclosed URIs
     */
    public static String sparqlUrisToCsv(final Collection<String> uriList) {

        StringBuilder strBuilder = new StringBuilder();
        if (uriList != null) {
            for (String uri : uriList) {

                if (strBuilder.length() > 0) {
                    strBuilder.append(",");
                }
                strBuilder.append("<");
                strBuilder.append(uri);
                strBuilder.append(">");
            }
        }
        return strBuilder.toString();
    }

    /**
     * Converts string to double.
     *
     * @param s
     *            String to convert.
     * @return Double
     */
    public static Double toDouble(final String s) {

        if (s == null || s.trim().length() == 0) {
            return null;
        } else {
            try {
                return Double.valueOf(s);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }

    /**
     *
     * @param language
     */
    public static String normalizeHTTPAcceptedLanguage(String httpAcceptedLanguage) {

        if (httpAcceptedLanguage == null) {
            return httpAcceptedLanguage;
        } else {
            httpAcceptedLanguage = httpAcceptedLanguage.trim();
        }

        String result = new String(httpAcceptedLanguage);

        /* ignore quality value which is separated by ';' */

        int j = result.indexOf(";");
        if (j != -1) {
            result = result.substring(0, j);
        }

        /*
         * ignore language refinement (e.g. en-US, en_UK) which is separated either by '-' or '_'
         */

        j = result.indexOf("-");
        if (j < 0) {
            j = result.indexOf("_");
        }
        if (j >= 0) {
            result = result.substring(0, j);
        }

        return result.toLowerCase();
    }

    /**
     *
     * @param acceptLanguageHeader
     * @return
     */
    public static List<String> getAcceptedLanguages(String acceptLanguageHeader) {

        final HashMap<String, Double> languageMap = new LinkedHashMap<String, Double>();
        if (!StringUtils.isBlank(acceptLanguageHeader)) {

            String[] languageStrings = StringUtils.split(acceptLanguageHeader, ',');
            for (int i = 0; i < languageStrings.length; i++) {

                String languageString = languageStrings[i].trim();
                String languageCode = StringUtils.substringBefore(languageString, ";").trim();
                if (!StringUtils.isEmpty(languageCode)) {

                    String languageCodeUnrefined = StringUtils.split(languageCode, "-_")[0];
                    String qualityString = StringUtils.substringAfter(languageString, ";").trim();
                    double qualityValue = NumberUtils.toDouble(StringUtils.substringAfter(qualityString, "="), 1.0d);

                    Double existingQualityValue = languageMap.get(languageCodeUnrefined);
                    if (existingQualityValue == null || qualityValue > existingQualityValue) {
                        languageMap.put(languageCodeUnrefined, Double.valueOf(qualityValue));
                    }
                }
            }
        }

        ArrayList<String> result = new ArrayList<String>(languageMap.keySet());
        Collections.sort(result, new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                return (-1) * languageMap.get(o1).compareTo(languageMap.get(o2));
            }
        });

        if (!result.contains("en")) {
            result.add("en");
        }
        result.add("");
        return result;
    }

    /**
     *
     * @param startTime
     * @return
     */
    public static String durationSince(long startTime) {

        return duration(Math.max(0, System.currentTimeMillis() - startTime));
    }

    /**
     *
     * @param duration
     * @return
     */
    protected static String duration(long duration) {

        int minutes = (int) ((duration / 1000) / 60);
        int seconds = (int) ((duration / 1000) % 60);
        int milliseconds = (int) (duration % 1000);
        StringBuffer buf = new StringBuffer();
        if (minutes > 0) {
            buf.append(minutes).append(" min ");
        }
        buf.append(seconds).append(".");
        if (milliseconds < 10) {
            buf.append("00");
        } else if (milliseconds < 100) {
            buf.append("0");
        }

        return buf.append(milliseconds).append(" sec").toString();
    }

    /**
     * Algorithm calculates the estimated number of hashes.
     *
     * @param minHash
     * @param maxHash
     * @return
     */
    public static int calculateHashesCount(long minHash, long maxHash) {

        BigDecimal minValue = new BigDecimal(Long.MIN_VALUE);
        BigDecimal maxValue = new BigDecimal(Long.MAX_VALUE);
        BigDecimal lowKey = new BigDecimal(minHash);
        BigDecimal highKey = new BigDecimal(maxHash);
        BigDecimal distance = maxValue.subtract(highKey).add(lowKey).subtract(minValue);
        BigDecimal hitCount = new BigDecimal(2).pow(64).divide(distance, 0, BigDecimal.ROUND_HALF_UP);

        return hitCount.intValue();
    }

    /**
     *
     * @param subjectString
     * @return
     */
    public static List<String> splitStringBySpacesExpectBetweenQuotes(String subjectString) {

        List<String> matchList = new ArrayList<String>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(subjectString.trim());
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }
        return matchList;
    }

    /**
     *
     * @param <K>
     * @param <V>
     * @param map
     * @return
     */
    public static <K, V> Set<K> getNullValueKeys(Map<K, V> map) {

        HashSet<K> result = new HashSet<K>();
        if (map != null && !map.isEmpty()) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                if (entry.getValue() == null) {
                    result.add(entry.getKey());
                }
            }
        }

        return result;
    }

    /**
     *
     * @param l
     * @return
     */
    public static int safeLongToInt(long l) {

        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value");
        }
        return (int) l;
    }

    /**
     *
     * @param s
     * @return
     */
    public static boolean isSurroundedWithQuotes(String s) {
        return s.startsWith("\"") && s.endsWith("\"") && s.length() > 1;
    }

    /**
     * Escape HTML characters and replace new lines with HTML brake tag.
     *
     * @param s
     * @return escaped string
     */
    public static String escapeHtml(String s) {

        if (!StringUtils.isBlank(s)) {
            s = StringEscapeUtils.escapeHtml(s);
            s = s.replaceAll("\n", "<br/>");
        }
        return s;
    }

    /**
     * Converts given string into boolean. The following inputs are covered (case-insensitively): - null string returns false -
     * "true" returns true - "false" return false - "yes" and "y" return true - "no" and "n" return false - "0", "-1", "-2", etc
     * return false - "1", "2", "3", etc return true - any other string, including an empty one, returns false
     *
     * @param s
     *            String to represent boolean.
     * @return boolean
     */
    public static boolean toBooolean(final String s) {

        if (s == null) {
            return false;
        } else if (s.equalsIgnoreCase("true")) {
            return true;
        } else if (s.equalsIgnoreCase("false")) {
            return false;
        } else if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("y")) {
            return true;
        } else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("n")) {
            return false;
        } else {
            try {
                return Util.toBooolean(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    /**
     * Returns true if the given integer is >0, otherwise returns false.
     *
     * @param i
     *            int
     * @return boolean
     */
    public static boolean toBooolean(final int i) {

        return i > 0;
    }

    /**
     * Removes surrounding quotes of the given String.
     *
     * @param s
     *            String to remove quotes from
     * @return Given String without surrounding quotes. If the method parameter is not surrounded with quotes the original String is
     *         returned.
     */
    public static String removeSurroundingQuotes(final String s) {
        if (!isSurroundedWithQuotes(s)) {
            return s;
        }
        return s.substring(1, s.indexOf("\"", 1));
    }

    /**
     * If the given element name already exists (case insensitive) in the list of element names, then append a trailing unique
     * number.
     *
     * @param element
     * @param elements
     *            elements
     * @return
     */
    public static String getUniqueElementName(String element, Collection<String> elements) {

        if (element == null || element.length() == 0) {
            element = XmlUtil.INVALID_ELEMENT_NAME;
        }

        if (elements != null) {
            while (elements.contains(element)) {
                int dashPos = element.lastIndexOf("_");
                if (dashPos > 1 && dashPos < element.length() - 1) {
                    String snum = element.substring(dashPos + 1);
                    try {
                        int inum = Integer.parseInt(snum);
                        element = element.substring(0, dashPos) + "_" + (inum + 1);
                    } catch (Exception e) {
                        element = element + "_1";
                    }
                } else {
                    element = element + "_1";
                }
            }
        }
        return element;
    }

    /**
     *
     * @param str
     * @param token
     * @return
     */
    public static boolean containsToken(String str, String token) {
        return containsToken(str, token, false);
    }

    /**
     *
     * @param str
     * @param token
     * @return
     */
    public static boolean containsTokenIgnoreCase(String str, String token) {
        return containsToken(str, token, true);
    }

    /**
     *
     * @param str
     * @param token
     * @param ignoreCase
     * @return
     */
    private static boolean containsToken(String str, String token, boolean ignoreCase) {

        if (str == null || str.trim().length() == 0) {
            return false;
        }

        StringTokenizer st = new StringTokenizer(str);
        while (st.hasMoreTokens()) {

            String nextToken = st.nextToken();
            if (ignoreCase ? nextToken.equalsIgnoreCase(token) : nextToken.equals(token)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param request
     * @return
     */
    public static boolean isWebBrowser(HttpServletRequest request) {

        // Lazy-loading.
        boolean result = false;
        String userAgentString = request.getHeader("User-Agent");
        if (userAgentString != null && userAgentString.trim().length() > 0) {

            Browser browser = Browser.parseUserAgentString(userAgentString);
            if (browser != null) {

                BrowserType browserType = browser.getBrowserType();
                if (browserType != null) {

                    if (browserType.equals(BrowserType.WEB_BROWSER) || browserType.equals(BrowserType.MOBILE_BROWSER)) {
                        result = true;
                    }
                }
            }
        }

        return result;
    }

    /**
     *
     * @param actionBeanClass
     * @return
     */
    public static String getUrlBinding(Class<? extends ActionBean> actionBeanClass) {

        if (actionBeanClass == null) {
            return null;
        } else {
            return actionBeanClass.getAnnotation(UrlBinding.class).value();
        }
    }

    /**
     * Returns true if the given text matches the given wildcard (i.e. '*') pattern.
     * Code borrowed from http://www.adarshr.com/papers/wildcard.
     *
     * @param text as indicated above
     * @param pattern as indicated above
     * @return as indicated above
     */
    public static boolean wildCardMatch(String text, String pattern) {

        // Create the cards by splitting using a RegEx. If more speed
        // is desired, a simpler character based splitting can be done.
        String[] cards = pattern.split("\\*");

        // Iterate over the cards.
        for (String card : cards) {
            int idx = text.indexOf(card);

            // Card not detected in the text.
            if (idx == -1) {
                return false;
            }

            // Move ahead, towards the right of the text.
            text = text.substring(idx + card.length());
        }

        return true;
    }

    /**
     *
     * @param coll
     * @return
     */
    public static boolean isAllNull(Collection<?> coll) {

        for (Object object : coll) {
            if (object != null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Return a comma-separated string of "times" instances of given object.
     *
     * @param obj
     * @param times
     * @return
     */
    public static String csv(Object obj, int times) {

        if (times <= 0) {
            return StringUtils.EMPTY;
        }

        Object[] array = new Object[times];
        Arrays.fill(array, obj);
        return StringUtils.join(array, ',');
    }

    /**
     *
     * @param bean
     * @throws ReflectiveOperationException
     */
    public static void trimToNullAllStringProperties(Object bean) throws ReflectiveOperationException {

        if (bean == null) {
            return;
        }

        PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(bean);
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            String name = propertyDescriptor.getName();
            Class<?> clazz = propertyDescriptor.getPropertyType();
            if (clazz.equals(String.class)) {
                Object strValue = PropertyUtils.getProperty(bean, name);
                if (strValue != null) {
                    PropertyUtils.setProperty(bean, name, StringUtils.trimToNull(strValue.toString()));
                }
            }
        }
    }
}
