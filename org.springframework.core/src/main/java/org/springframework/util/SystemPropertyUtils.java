/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

/**
 * Helper class for resolving placeholders in texts. Usually applied to file paths.
 *
 * <p>A text may contain <code>${...}</code> placeholders, to be resolved as system properties: e.g.
 * <code>${user.dir}</code>.
 *
 * @author Juergen Hoeller
 * @see #PLACEHOLDER_PREFIX
 * @see #PLACEHOLDER_SUFFIX
 * @see System#getProperty(String)
 * @since 1.2.5
 */
public abstract class SystemPropertyUtils {

	/** Prefix for system property placeholders: "${" */
	public static final String PLACEHOLDER_PREFIX = "${";

	/** Suffix for system property placeholders: "}" */
	public static final String PLACEHOLDER_SUFFIX = "}";

	/** Value separator for system property placeholders: "}" */
	public static final String VALUE_SEPARATOR = ":";

	private static final PropertyPlaceholderHelper helper =
			new PropertyPlaceholderHelper(PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, false);


	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding system property values.
	 * @param text the String to resolve
	 * @return the resolved String
	 * @see #PLACEHOLDER_PREFIX
	 * @see #PLACEHOLDER_SUFFIX
	 */
	public static String resolvePlaceholders(final String text) {
		return helper.replacePlaceholders(text, new PlaceholderResolver() {
			public String resolvePlaceholder(String placeholderName) {
				String propVal = null;
				try {
					propVal = System.getProperty(placeholderName);
					if (propVal == null) {
						// Fall back to searching the system environment.
						propVal = System.getenv(placeholderName);
					}

					if (propVal == null) {
						System.err.println("Could not resolve placeholder '" + placeholderName + "' in [" + text +
								"] as system property: neither system property nor environment variable found");
					}
				}
				catch (Throwable ex) {
					System.err.println("Could not resolve placeholder '" + placeholderName + "' in [" + text +
							"] as system property: " + ex);

				}
				return propVal;
			}
		});
	}

}
