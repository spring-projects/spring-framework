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

package org.springframework.util;

/**
 * Helper class for resolving placeholders in texts. Usually applied to file paths.
 *
 * <p>A text may contain <code>${...}</code> placeholders, to be resolved as
 * system properties: e.g. <code>${user.dir}</code>.
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 * @see #PLACEHOLDER_PREFIX
 * @see #PLACEHOLDER_SUFFIX
 * @see System#getProperty(String)
 */
public abstract class SystemPropertyUtils {

	/** Prefix for system property placeholders: "${" */
	public static final String PLACEHOLDER_PREFIX = "${";

	/** Suffix for system property placeholders: "}" */
	public static final String PLACEHOLDER_SUFFIX = "}";


	/**
	 * Resolve ${...} placeholders in the given text,
	 * replacing them with corresponding system property values.
	 * @param text the String to resolve
	 * @return the resolved String
	 * @see #PLACEHOLDER_PREFIX
	 * @see #PLACEHOLDER_SUFFIX
	 */
	public static String resolvePlaceholders(String text) {
		StringBuffer buf = new StringBuffer(text);

		int startIndex = buf.indexOf(PLACEHOLDER_PREFIX);
		while (startIndex != -1) {
			int endIndex = buf.indexOf(PLACEHOLDER_SUFFIX, startIndex + PLACEHOLDER_PREFIX.length());
			if (endIndex != -1) {
				String placeholder = buf.substring(startIndex + PLACEHOLDER_PREFIX.length(), endIndex);
				int nextIndex = endIndex + PLACEHOLDER_SUFFIX.length();
				try {
					String propVal = System.getProperty(placeholder);
					if (propVal == null) {
						// Fall back to searching the system environment.
						propVal = System.getenv(placeholder);
					}
					if (propVal != null) {
						buf.replace(startIndex, endIndex + PLACEHOLDER_SUFFIX.length(), propVal);
						nextIndex = startIndex + propVal.length();
					}
					else {
						System.err.println("Could not resolve placeholder '" + placeholder + "' in [" + text +
								"] as system property: neither system property nor environment variable found");
					}
				}
				catch (Throwable ex) {
					System.err.println("Could not resolve placeholder '" + placeholder + "' in [" + text +
							"] as system property: " + ex);
				}
				startIndex = buf.indexOf(PLACEHOLDER_PREFIX, nextIndex);
			}
			else {
				startIndex = -1;
			}
		}

		return buf.toString();
	}

}
