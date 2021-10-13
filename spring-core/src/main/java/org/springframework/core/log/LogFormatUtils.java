/*
 * Copyright 2002-2021 the original author or authors.
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

import org.apache.commons.logging.Log;

import org.springframework.lang.Nullable;

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

	/**
	 * Variant of {@link #formatValue(Object, int, boolean)} and a convenience
	 * method that truncates at 100 characters when {@code limitLength} is set.
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
	 * @param replaceNewlines whether to replace newline characters with placeholders
	 * @return the formatted value
	 */
	public static String formatValue(@Nullable Object value, int maxLength, boolean replaceNewlines) {
		if (value == null) {
			return "";
		}
		String result;
		try {
			result = value.toString();
		}
		catch (Throwable ex) {
			result = ex.toString();
		}
		if (maxLength != -1) {
			result = (result.length() > maxLength ? result.substring(0, maxLength) + " (truncated)..." : result);
		}
		if (replaceNewlines) {
			result = result.replace("\n", "<LF>").replace("\r", "<CR>");
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
