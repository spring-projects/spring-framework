/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import org.jspecify.annotations.Nullable;

import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Specialization of {@link ConfigurableEnvironment} allowing initialization of
 * servlet-related {@link org.springframework.core.env.PropertySource} objects at the
 * earliest moment that the {@link ServletContext} and (optionally) {@link ServletConfig}
 * become available.
 *
 * @author Chris Beams
 * @since 3.1.2
 * @see ConfigurableWebApplicationContext#getEnvironment()
 */
public interface ConfigurableWebEnvironment extends ConfigurableEnvironment {

	/**
	 * Replace any {@linkplain
	 * org.springframework.core.env.PropertySource.StubPropertySource stub property source}
	 * instances acting as placeholders with real servlet context/config property sources
	 * using the given parameters.
	 * @param servletContext the {@link ServletContext} ({@code null} if not available)
	 * @param servletConfig the {@link ServletConfig} ({@code null} if not available)
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources(
	 * org.springframework.core.env.MutablePropertySources, ServletContext, ServletConfig)
	 */
	void initPropertySources(@Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig);

}
