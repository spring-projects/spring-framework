/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.util;

/**
 * Utility class for JavaScript escaping.
 * Escapes based on the JavaScript 1.5 recommendation.
 *
 * <p>Reference:
 * <a href="http://developer.mozilla.org/en/docs/Core_JavaScript_1.5_Guide:Literals#String_Literals">
 * Core JavaScript 1.5 Guide
 * </a>
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 1.1.1
 */
public class JavaScriptUtils {

	/**
	 * Turn special characters into escaped characters conforming to JavaScript.
	 * Handles complete character set defined in HTML 4.01 recommendation.
	 * @param input the input string
	 * @return the escaped string
	 */
	public static String javaScriptEscape(String input) {
		if (input == null) {
			return input;
		}

		StringBuilder filtered = new StringBuilder(input.length());
		char prevChar = '\u0000';
		char c;
		for (int i = 0; i < input.length(); i++) {
			c = input.charAt(i);
			if (c == '"') {
				filtered.append("\\\"");
			}
			else if (c == '\'') {
				filtered.append("\\'");
			}
			else if (c == '\\') {
				filtered.append("\\\\");
			}
			else if (c == '/') {
				filtered.append("\\/");
			}
			else if (c == '\t') {
				filtered.append("\\t");
			}
			else if (c == '\n') {
				if (prevChar != '\r') {
					filtered.append("\\n");
				}
			}
			else if (c == '\r') {
				filtered.append("\\n");
			}
			else if (c == '\f') {
				filtered.append("\\f");
			}
			else {
				filtered.append(c);
			}
			prevChar = c;

		}
		return filtered.toString();
	}

}
