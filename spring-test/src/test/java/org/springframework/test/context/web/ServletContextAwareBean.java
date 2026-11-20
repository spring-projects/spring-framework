/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.web;

import jakarta.servlet.ServletContext;

import org.springframework.web.context.ServletContextAware;

/**
 * Introduced to investigate claims in SPR-11145.
 *
 * @author Sam Brannen
 * @since 4.0.2
 */
public class ServletContextAwareBean implements ServletContextAware {

	private ServletContext servletContext;

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public ServletContext getServletContext() {
		return this.servletContext;
	}

}
