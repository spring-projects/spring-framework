/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
