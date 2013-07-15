/*
 * Copyright 2002-2013 the original author or authors.
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

import javax.servlet.ServletContext;

import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

/**
 * Helper class for resolving placeholders in texts. Usually applied to file paths.
 *
 * <p>A text may contain {@code ${...}} placeholders, to be resolved as servlet context
 * init parameters or system properties: e.g. {@code ${user.dir}}. Default values can
 * be supplied using the ":" separator between key and value.
 *
 * @author Juergen Hoeller
 * @author Marten Deinum
 * @since 3.2.2
 * @see SystemPropertyUtils
 * @see ServletContext#getInitParameter(String)
 */
public abstract class ServletContextPropertyUtils {

    private static final PropertyPlaceholderHelper strictHelper =
            new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX,
					SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, false);

    private static final PropertyPlaceholderHelper nonStrictHelper =
            new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX,
					SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, true);


	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * servlet context init parameter or system property values.
     * @param text the String to resolve
     * @param servletContext the servletContext to use for lookups.
	 * @return the resolved String
	 * @see SystemPropertyUtils#PLACEHOLDER_PREFIX
	 * @see SystemPropertyUtils#PLACEHOLDER_SUFFIX
     * @see SystemPropertyUtils#resolvePlaceholders(String, boolean)
	 * @throws IllegalArgumentException if there is an unresolvable placeholder
	 */
	public static String resolvePlaceholders(String text, ServletContext servletContext) {
		return resolvePlaceholders(text, servletContext, false);
	}

	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * servlet context init parameter or system property values. Unresolvable placeholders
	 * with no default value are ignored and passed through unchanged if the flag is set to true.
	 * @param text the String to resolve
     * @param servletContext the servletContext to use for lookups.
	 * @param ignoreUnresolvablePlaceholders flag to determine is unresolved placeholders are ignored
	 * @return the resolved String
	 * @see SystemPropertyUtils#PLACEHOLDER_PREFIX
	 * @see SystemPropertyUtils#PLACEHOLDER_SUFFIX
     * @see SystemPropertyUtils#resolvePlaceholders(String, boolean)
	 * @throws IllegalArgumentException if there is an unresolvable placeholder and the flag is false
	 */
	public static String resolvePlaceholders(String text, ServletContext servletContext, boolean ignoreUnresolvablePlaceholders) {
		PropertyPlaceholderHelper helper = (ignoreUnresolvablePlaceholders ? nonStrictHelper : strictHelper);
		return helper.replacePlaceholders(text, new ServletContextPlaceholderResolver(text, servletContext));
	}


	private static class ServletContextPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

        private final String text;

        private final ServletContext servletContext;

        public ServletContextPlaceholderResolver(String text, ServletContext servletContext) {
            this.text = text;
            this.servletContext = servletContext;
        }

        @Override
		public String resolvePlaceholder(String placeholderName) {
            try {
                String propVal = this.servletContext.getInitParameter(placeholderName);
				if (propVal == null) {
					// Fall back to system properties.
					propVal = System.getProperty(placeholderName);
					if (propVal == null) {
						// Fall back to searching the system environment.
						propVal = System.getenv(placeholderName);
					}
				}
				return propVal;
			}
            catch (Throwable ex) {
                System.err.println("Could not resolve placeholder '" + placeholderName + "' in [" +
                        this.text + "] as ServletContext init-parameter or system property: " + ex);
                return null;
            }
        }
    }

}
