/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mock.web;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;


/**
 * Utility class to generate HTTP dates.
 * <p>
 * This class is based on code in Apache Tomcat.
 *
 * @author Remy Maucherat
 * @author Andrey Grebnev
 */
public class FastHttpDateFormat {
    //~ Static fields/initializers =====================================================================================

    /** HTTP date format. */
    protected static final SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

    /** The set of SimpleDateFormat formats to use in <code>getDateHeader()</code>. */
    protected static final SimpleDateFormat[] formats = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US)
        };

    /** GMT time zone - all HTTP dates are on GMT */
    protected static final TimeZone gmtZone = TimeZone.getTimeZone("GMT");

    static {
        format.setTimeZone(gmtZone);

        formats[0].setTimeZone(gmtZone);
        formats[1].setTimeZone(gmtZone);
        formats[2].setTimeZone(gmtZone);
    }

    /** Instant on which the currentDate object was generated. */
    protected static long currentDateGenerated = 0L;

    /** Current formatted date. */
    protected static String currentDate = null;

    /** Formatter cache. */
    protected static final HashMap<Long,String> formatCache = new HashMap<Long,String>();

    /** Parser cache. */
    protected static final HashMap<String,Long> parseCache = new HashMap<String,Long>();

    //~ Methods ========================================================================================================

    /**
     * Formats a specified date to HTTP format. If local format is not <code>null</code>, it's used instead.
     *
     * @param value Date value to format
     * @param threadLocalformat The format to use (or <code>null</code> -- then HTTP format will be used)
     *
     * @return Formatted date
     */
    public static String formatDate(long value, DateFormat threadLocalformat) {
        String cachedDate = null;
        Long longValue = Long.valueOf(value);

        try {
            cachedDate = formatCache.get(longValue);
        } catch (Exception ignored) {}

        if (cachedDate != null) {
            return cachedDate;
        }

        String newDate;
        Date dateValue = new Date(value);

        if (threadLocalformat != null) {
            newDate = threadLocalformat.format(dateValue);

            synchronized (formatCache) {
                updateCache(formatCache, longValue, newDate);
            }
        } else {
            synchronized (formatCache) {
                newDate = format.format(dateValue);
                updateCache(formatCache, longValue, newDate);
            }
        }

        return newDate;
    }

    /**
     * Gets the current date in HTTP format.
     *
     * @return Current date in HTTP format
     */
    public static String getCurrentDate() {
        long now = System.currentTimeMillis();

        if ((now - currentDateGenerated) > 1000) {
            synchronized (format) {
                if ((now - currentDateGenerated) > 1000) {
                    currentDateGenerated = now;
                    currentDate = format.format(new Date(now));
                }
            }
        }

        return currentDate;
    }

    /**
     * Parses date with given formatters.
     *
     * @param value The string to parse
     * @param formats Array of formats to use
     *
     * @return Parsed date (or <code>null</code> if no formatter mached)
     */
    private static Long internalParseDate(String value, DateFormat[] formats) {
        Date date = null;

        for (int i = 0; (date == null) && (i < formats.length); i++) {
            try {
                date = formats[i].parse(value);
            } catch (ParseException ignored) {
            }
        }

        if (date == null) {
            return null;
        }

        return new Long(date.getTime());
    }

    /**
     * Tries to parse the given date as an HTTP date. If local format list is not <code>null</code>, it's used
     * instead.
     *
     * @param value The string to parse
     * @param threadLocalformats Array of formats to use for parsing. If <code>null</code>, HTTP formats are used.
     *
     * @return Parsed date (or -1 if error occurred)
     */
    public static long parseDate(String value, DateFormat[] threadLocalformats) {
        Long cachedDate = null;

        try {
            cachedDate = (Long) parseCache.get(value);
        } catch (Exception ignored) {}

        if (cachedDate != null) {
            return cachedDate.longValue();
        }

        Long date;

        if (threadLocalformats != null) {
            date = internalParseDate(value, threadLocalformats);

            synchronized (parseCache) {
                updateCache(parseCache, value, date);
            }
        } else {
            synchronized (parseCache) {
                date = internalParseDate(value, formats);
                updateCache(parseCache, value, date);
            }
        }

        if (date == null) {
            return (-1L);
        } else {
            return date.longValue();
        }
    }

    /**
     * Updates cache.
     *
     * @param cache Cache to be updated
     * @param key Key to be updated
     * @param value New value
     */
    @SuppressWarnings("unchecked")
    private static void updateCache(HashMap cache, Object key, Object value) {
        if (value == null) {
            return;
        }

        if (cache.size() > 1000) {
            cache.clear();
        }

        cache.put(key, value);
    }
}
