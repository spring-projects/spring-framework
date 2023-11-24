/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.log;

import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility methods for formatting and logging messages.
 *
 * <p>Mainly for internal use within the framework with Apache Commons Logging,
 * typically in the form of the {@code spring-jcl} bridge but also compatible
 * with other Commons Logging bridges.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.1
 */
public abstract class LogFormatUtils {

	private static final Pattern NEWLINE_PATTERN = Pattern.compile("[\n\r]");

	private static final Pattern CONTROL_CHARACTER_PATTERN = Pattern.compile("\\p{Cc}");


	/**
	 * Convenience variant of {@link #formatValue(Object, int, boolean)} that
	 * limits the length of a log message to 100 characters and also replaces
	 * newline and control characters if {@code limitLength} is set to "true".
	 * @param value the value to format
	 * @param limitLength whether to truncate the value at a length of 100
	 * @return the formatted value
	 */
	public static String formatValue(@Nullable Object value, boolean limitLength) {
		return formatValue(value, (limitLength ? 100 : -1), limitLength);
	}

	/**
	 * Format the given value via {@code toString()}, quoting it if it is a
	 * {@link CharSequence}, truncating at the specified {@code maxLength}, and
	 * compacting it into a single line when {@code replaceNewLines} is set.
	 * @param value the value to be formatted
	 * @param maxLength the max length, after which to truncate, or -1 for unlimited
	 * @param replaceNewlinesAndControlCharacters whether to replace newline and
	 * control characters with placeholders
	 * @return the formatted value
	 */
	public static String formatValue(
			@Nullable Object value, int maxLength, boolean replaceNewlinesAndControlCharacters) {

		if (value == null) {
			return "";
		}
		String result;
		try {
			result = ObjectUtils.nullSafeToString(value);
		}
		catch (Throwable ex) {
			result = ObjectUtils.nullSafeToString(ex);
		}
		if (maxLength != -1) {
			result = StringUtils.truncate(result, maxLength);
		}
		if (replaceNewlinesAndControlCharacters) {
			result = NEWLINE_PATTERN.matcher(result).replaceAll("<EOL>");
			result = CONTROL_CHARACTER_PATTERN.matcher(result).replaceAll("?");
		}
		if (value instanceof CharSequence) {
			result = "\"" + result + "\"";
		}
		return result;
	}

	/**
	 * Use this to log a message with different levels of detail (or different
	 * messages) at TRACE vs DEBUG log levels. Effectively, a substitute for:
	 * <pre class="code">
	 * if (logger.isDebugEnabled()) {
	 *   String str = logger.isTraceEnabled() ? "..." : "...";
	 *   if (logger.isTraceEnabled()) {
	 *     logger.trace(str);
	 *   }
	 *   else {
	 *     logger.debug(str);
	 *   }
	 * }
	 * </pre>
	 * @param logger the logger to use to log the message
	 * @param messageFactory function that accepts a boolean set to the value
	 * of {@link Log#isTraceEnabled()}
	 */
	public static void traceDebug(Log logger, Function<Boolean, String> messageFactory) {
		if (logger.isDebugEnabled()) {
			boolean traceEnabled = logger.isTraceEnabled();
			String logMessage = messageFactory.apply(traceEnabled);
			if (traceEnabled) {
				logger.trace(logMessage);
			}
			else {
				logger.debug(logMessage);
			}
		}
	}

}
