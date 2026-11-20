/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.datetime.standard;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

import org.springframework.util.StringUtils;

/**
 * Internal {@link DateTimeFormatter} utilities.
 *
 * @author Juergen Hoeller
 * @since 5.3.5
 */
abstract class DateTimeFormatterUtils {

	/**
	 * Create a {@link DateTimeFormatter} for the supplied pattern, configured with
	 * {@linkplain ResolverStyle#STRICT strict} resolution.
	 * <p>Note that the strict resolution does not affect the parsing.
	 * @param pattern the pattern to use
	 * @return a new {@code DateTimeFormatter}
	 * @see ResolverStyle#STRICT
	 */
	static DateTimeFormatter createStrictDateTimeFormatter(String pattern) {
		// Using strict resolution to align with standard DateFormat behavior:
		// otherwise, an overflow like, for example, Feb 29 for a non-leap-year wouldn't get rejected.
		// However, with strict resolution, a year digit needs to be specified as 'u'...
		String patternToUse = StringUtils.replace(pattern, "yy", "uu");
		return DateTimeFormatter.ofPattern(patternToUse).withResolverStyle(ResolverStyle.STRICT);
	}

}
