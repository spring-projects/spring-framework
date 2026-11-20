/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context;

import jakarta.servlet.ServletConfig;

/**
 * @author Juergen Hoeller
 */
public class ServletConfigAwareBean implements ServletConfigAware {

	private ServletConfig servletConfig;

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
	}

	public ServletConfig getServletConfig() {
		return servletConfig;
	}
}
