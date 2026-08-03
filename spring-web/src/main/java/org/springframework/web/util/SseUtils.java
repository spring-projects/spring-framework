/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.util;

import org.springframework.util.Assert;

/**
 * Utility methods for writing content as
 * <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">Server-Sent Events</a>,
 * shared by the Servlet and Reactive SSE support.
 *
 * @author Brian Clozel
 * @since 7.0.9
 */
public abstract class SseUtils {

	/**
	 * Append {@code value} to {@code output}, replacing each line separator
	 * ({@code "\n"}, {@code "\r"}, or {@code "\r\n"}) it contains with a new
	 * {@code field} line (that is, {@code "\n" + field + ":"}). This keeps a
	 * multi-line field value from breaking out of the current SSE field when
	 * written on the wire.
	 * @param field the name of the SSE field that {@code value} belongs to
	 * (for example, {@code "data"}), or an empty string for a comment
	 * @param value the field value to escape and append
	 * @param output the {@code StringBuilder} to append the escaped value to
	 */
	public static void appendFieldValue(String field, String value, StringBuilder output) {
		if (value.indexOf('\n') == -1 && value.indexOf('\r') == -1) {
			output.append(value);
			return;
		}
		String lineSeparatorReplacement = "\n" + field + ":";
		int length = value.length();
		for (int i = 0; i < length; i++) {
			char c = value.charAt(i);
			if (c == '\r') {
				if (i + 1 < length && value.charAt(i + 1) == '\n') {
					i++;
				}
				output.append(lineSeparatorReplacement);
			}
			else if (c == '\n') {
				output.append(lineSeparatorReplacement);
			}
			else {
				output.append(c);
			}
		}
	}

	/**
	 * Assert that the given single-line SSE field value, such as an
	 * {@code id} or {@code event} name, does not contain a line separator.
	 * @param content the field value to check
	 * @throws IllegalArgumentException if {@code content} contains {@code "\n"} or {@code "\r"}
	 */
	public static void assertNoLineSeparator(String content) {
		Assert.isTrue(content.indexOf('\n') == -1 && content.indexOf('\r') == -1,
				"illegal character '\\n' or '\\r' in event content");
	}

}
