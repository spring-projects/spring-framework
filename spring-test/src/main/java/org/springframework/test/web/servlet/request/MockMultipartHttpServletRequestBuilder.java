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

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

/**
 * Default builder for {@link MockMultipartHttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Stephane Nicoll
 * @since 3.2
 */
public class MockMultipartHttpServletRequestBuilder
		extends AbstractMockMultipartHttpServletRequestBuilder<MockMultipartHttpServletRequestBuilder> {

	/**
	 * Package-private constructor. Use static factory methods in
	 * {@link MockMvcRequestBuilders}.
	 * <p>For other ways to initialize a {@code MockMultipartHttpServletRequest},
	 * see {@link #with(RequestPostProcessor)} and the
	 * {@link RequestPostProcessor} extension point.
	 * @param httpMethod the HTTP method (GET, POST, etc.)
	 */
	MockMultipartHttpServletRequestBuilder(HttpMethod httpMethod) {
		super(httpMethod);
		super.contentType(MediaType.MULTIPART_FORM_DATA);
	}

	/**
	 * Variant of {@link #MockMultipartHttpServletRequestBuilder(HttpMethod)}
	 * that defaults to {@link HttpMethod#POST}.
	 */
	MockMultipartHttpServletRequestBuilder() {
		this(HttpMethod.POST);
	}


}
