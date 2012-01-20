/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.web.context.support;

import java.util.Properties;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.web.context.ServletContextAware;


/**
 * Subclass of {@link PropertyPlaceholderConfigurer} that resolves placeholders as
 * ServletContext init parameters (that is, <code>web.xml</code> context-param
 * entries).
 *
 * <p>Can be combined with "locations" and/or "properties" values in addition
 * to web.xml context-params. Alternatively, can be defined without local
 * properties, to resolve all placeholders as <code>web.xml</code> context-params
 * (or JVM system properties).
 *
 * <p>If a placeholder could not be resolved against the provided local
 * properties within the application, this configurer will fall back to
 * ServletContext parameters. Can also be configured to let ServletContext
 * init parameters override local properties (contextOverride=true).
 *
 * <p>Optionally supports searching for ServletContext <i>attributes</i>: If turned
 * on, an otherwise unresolvable placeholder will matched against the corresponding
 * ServletContext attribute, using its stringified value if found. This can be
 * used to feed dynamic values into Spring's placeholder resolution.
 *
 * <p>If not running within a WebApplicationContext (or any other context that
 * is able to satisfy the ServletContextAware callback), this class will behave
 * like the default PropertyPlaceholderConfigurer. This allows for keeping
 * ServletContextPropertyPlaceholderConfigurer definitions in test suites.
 *
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see #setLocations
 * @see #setProperties
 * @see #setSystemPropertiesModeName
 * @see #setContextOverride
 * @see #setSearchContextAttributes
 * @see javax.servlet.ServletContext#getInitParameter(String)
 * @see javax.servlet.ServletContext#getAttribute(String)
 * @deprecated in Spring 3.1 in favor of {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer}
 * in conjunction with {@link org.springframework.web.context.support.StandardServletEnvironment}.
 */
@Deprecated
public class ServletContextPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer
		implements ServletContextAware {

	private boolean contextOverride = false;

	private boolean searchContextAttributes = false;

	private ServletContext servletContext;


	/**
	 * Set whether ServletContext init parameters (and optionally also ServletContext
	 * attributes) should override local properties within the application.
	 * Default is "false": ServletContext settings serve as fallback.
	 * <p>Note that system properties will still override ServletContext settings,
	 * if the system properties mode is set to "SYSTEM_PROPERTIES_MODE_OVERRIDE".
	 * @see #setSearchContextAttributes
	 * @see #setSystemPropertiesModeName
	 * @see #SYSTEM_PROPERTIES_MODE_OVERRIDE
	 */
	public void setContextOverride(boolean contextOverride) {
		this.contextOverride = contextOverride;
	}

	/**
	 * Set whether to search for matching a ServletContext attribute before
	 * checking a ServletContext init parameter. Default is "false": only
	 * checking init parameters.
	 * <p>If turned on, the configurer will look for a ServletContext attribute with
	 * the same name as the placeholder, and use its stringified value if found.
	 * Exposure of such ServletContext attributes can be used to dynamically override
	 * init parameters defined in <code>web.xml</code>, for example in a custom
	 * context listener.
	 * @see javax.servlet.ServletContext#getInitParameter(String)
	 * @see javax.servlet.ServletContext#getAttribute(String)
	 */
	public void setSearchContextAttributes(boolean searchContextAttributes) {
		this.searchContextAttributes = searchContextAttributes;
	}

	/**
	 * Set the ServletContext to resolve placeholders against.
	 * Will be auto-populated when running in a WebApplicationContext.
	 * <p>If not set, this configurer will simply not resolve placeholders
	 * against the ServletContext: It will effectively behave like a plain
	 * PropertyPlaceholderConfigurer in such a scenario.
	 */
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}


	@Override
	protected String resolvePlaceholder(String placeholder, Properties props) {
		String value = null;
		if (this.contextOverride && this.servletContext != null) {
			value = resolvePlaceholder(placeholder, this.servletContext, this.searchContextAttributes);
		}
		if (value == null) {
			value = super.resolvePlaceholder(placeholder, props);
		}
		if (value == null && this.servletContext != null) {
			value = resolvePlaceholder(placeholder, this.servletContext, this.searchContextAttributes);
		}
		return value;
	}

	/**
	 * Resolves the given placeholder using the init parameters
	 * and optionally also the attributes of the given ServletContext.
	 * <p>Default implementation checks ServletContext attributes before
	 * init parameters. Can be overridden to customize this behavior,
	 * potentially also applying specific naming patterns for parameters
	 * and/or attributes (instead of using the exact placeholder name).
	 * @param placeholder the placeholder to resolve
	 * @param servletContext the ServletContext to check
	 * @param searchContextAttributes whether to search for a matching
	 * ServletContext attribute
	 * @return the resolved value, of null if none
	 * @see javax.servlet.ServletContext#getInitParameter(String)
	 * @see javax.servlet.ServletContext#getAttribute(String)
	 */
	protected String resolvePlaceholder(
			String placeholder, ServletContext servletContext, boolean searchContextAttributes) {

		String value = null;
		if (searchContextAttributes) {
			Object attrValue = servletContext.getAttribute(placeholder);
			if (attrValue != null) {
				value = attrValue.toString();
			}
		}
		if (value == null) {
			value = servletContext.getInitParameter(placeholder);
		}
		return value;
	}

}
