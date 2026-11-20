/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Extended variant of a {@link RequestBuilder} that applies its
 * {@link org.springframework.test.web.servlet.request.RequestPostProcessor org.springframework.test.web.servlet.request.RequestPostProcessors}
 * as a separate step from the {@link #buildRequest} method.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface SmartRequestBuilder extends RequestBuilder {

	/**
	 * Apply request post-processing. Typically, that means invoking one or more
	 * {@link org.springframework.test.web.servlet.request.RequestPostProcessor org.springframework.test.web.servlet.request.RequestPostProcessors}.
	 * @param request the request to initialize
	 * @return the request to use, either the one passed in or a wrapped one
	 */
	MockHttpServletRequest postProcessRequest(MockHttpServletRequest request);

}
