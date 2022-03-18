/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.nativex;

/**
 * Utility class for JSON.
 *
 * @author Sebastien Deleuze
 */
abstract class JsonUtils {

	/**
	 * Escape a JSON String.
	 */
	static String escape(String input) {
		StringBuilder builder = new StringBuilder();
		input.chars().forEach(c -> {
			switch (c) {
				case '"':
					builder.append("\\\"");
					break;
				case '\\':
					builder.append("\\\\");
					break;
				case '/':
					builder.append("\\/");
					break;
				case '\b':
					builder.append("\\b");
					break;
				case '\f':
					builder.append("\\f");
					break;
				case '\n':
					builder.append("\\n");
					break;
				case '\r':
					builder.append("\\r");
					break;
				case '\t':
					builder.append("\\t");
					break;
				default:
					if (c <= 0x1F) {
						builder.append(String.format("\\u%04x", c));
					}
					else {
						builder.append((char) c);
					}
					break;
			}
		});
		return builder.toString();
	}
}
