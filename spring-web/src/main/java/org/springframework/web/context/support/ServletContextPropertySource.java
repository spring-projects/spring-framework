/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context.support;

import jakarta.servlet.ServletContext;
import org.jspecify.annotations.Nullable;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * {@link PropertySource} that reads init parameters from a {@link ServletContext} object.
 *
 * @author Chris Beams
 * @since 3.1
 * @see ServletConfigPropertySource
 */
public class ServletContextPropertySource extends EnumerablePropertySource<ServletContext> {

	public ServletContextPropertySource(String name, ServletContext servletContext) {
		super(name, servletContext);
	}

	@Override
	public String[] getPropertyNames() {
		return StringUtils.toStringArray(this.source.getInitParameterNames());
	}

	@Override
	public @Nullable String getProperty(String name) {
		return this.source.getInitParameter(name);
	}

}
