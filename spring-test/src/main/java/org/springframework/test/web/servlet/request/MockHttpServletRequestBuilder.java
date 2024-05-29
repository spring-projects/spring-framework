/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.servlet.request;

import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Assert;

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
	 * @param uriTemplate a URI template; the resulting URI will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	MockHttpServletRequestBuilder(HttpMethod httpMethod, String uriTemplate, Object... uriVariables) {
		super(httpMethod);
		super.uri(uriTemplate, uriVariables);
	}

	/**
	 * Alternative to {@link #MockHttpServletRequestBuilder(HttpMethod, String, Object...)}
	 * with a pre-built URI.
	 * @param httpMethod the HTTP method (GET, POST, etc.)
	 * @param uri the URI
	 * @since 4.0.3
	 */
	MockHttpServletRequestBuilder(HttpMethod httpMethod, URI uri) {
		super(httpMethod);
		Assert.notNull(uri, "'uri' is required");
		super.uri(uri);
	}

}
