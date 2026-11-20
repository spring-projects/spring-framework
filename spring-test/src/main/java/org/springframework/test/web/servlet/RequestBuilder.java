/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet;

import jakarta.servlet.ServletContext;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Builds a {@link MockHttpServletRequest}.
 *
 * <p>See static factory methods in
 * {@link org.springframework.test.web.servlet.request.MockMvcRequestBuilders MockMvcRequestBuilders}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface RequestBuilder {

	/**
	 * Build the request.
	 * @param servletContext the {@link ServletContext} to use to create the request
	 * @return the request
	 */
	MockHttpServletRequest buildRequest(ServletContext servletContext);

}
