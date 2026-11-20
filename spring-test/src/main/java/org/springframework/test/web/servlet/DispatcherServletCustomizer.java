/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet;

import org.springframework.web.servlet.DispatcherServlet;

/**
 * Strategy interface for customizing {@link DispatcherServlet} instances that are
 * managed by {@link MockMvc}.
 *
 * @author Stephane Nicoll
 * @since 4.3.4
 */
@FunctionalInterface
public interface DispatcherServletCustomizer {

	/**
	 * Customize the supplied {@link DispatcherServlet} <em>before</em> it is
	 * initialized.
	 * @param dispatcherServlet the dispatcher servlet to customize
	 */
	void customize(DispatcherServlet dispatcherServlet);

}
