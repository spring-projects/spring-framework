/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.request;

import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Default builder for {@link MockHttpServletRequest} required as input to
 * perform requests in {@link MockMvc}.
 *
 * <p>Application tests will typically access this builder through the static
 * factory methods in {@link MockMvcRequestBuilders}.
 *
 * <p>This class is not open for extension. To apply custom initialization to
 * the created {@code MockHttpServletRequest}, please use the
 * {@link #with(RequestPostProcessor)} extension point.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Kamill Sokol
 * @since 3.2
 */
public class MockHttpServletRequestBuilder
		extends AbstractMockHttpServletRequestBuilder<MockHttpServletRequestBuilder> {

	/**
	 * Package private constructor. To get an instance, use static factory
	 * methods in {@link MockMvcRequestBuilders}.
	 * <p>Although this class cannot be extended, additional ways to initialize
	 * the {@code MockHttpServletRequest} can be plugged in via
	 * {@link #with(RequestPostProcessor)}.
	 * @param httpMethod the HTTP method (GET, POST, etc.)
	 */
	MockHttpServletRequestBuilder(HttpMethod httpMethod) {
		super(httpMethod);
	}

}
