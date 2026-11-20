/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.request;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Extension point for applications or 3rd party libraries that wish to further
 * initialize a {@link MockHttpServletRequest} instance after it has been built
 * by {@link MockHttpServletRequestBuilder} or its subclass
 * {@link MockMultipartHttpServletRequestBuilder}.
 *
 * <p>Implementations of this interface can be provided to
 * {@link MockHttpServletRequestBuilder#with(RequestPostProcessor)} at the time
 * when a request is about to be constructed.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 */
@FunctionalInterface
public interface RequestPostProcessor {

	/**
	 * Post-process the given {@code MockHttpServletRequest} after its creation
	 * and initialization through a {@code MockHttpServletRequestBuilder}.
	 * @param request the request to initialize
	 * @return the request to use, either the one passed in or a wrapped one
	 */
	MockHttpServletRequest postProcessRequest(MockHttpServletRequest request);

}
