/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.web.client.match;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.JsonExpectationsHelper;
import org.springframework.test.util.XmlExpectationsHelper;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Factory for request content {@code RequestMatcher}'s. An instance of this
 * class is typically accessed via {@link MockRestRequestMatchers#content()}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ContentRequestMatchers {

	private final XmlExpectationsHelper xmlHelper;

	private final JsonExpectationsHelper jsonHelper;


	/**
	 * Class constructor, not for direct instantiation.
	 * Use {@link MockRestRequestMatchers#content()}.
	 */
	protected ContentRequestMatchers() {
		this.xmlHelper = new XmlExpectationsHelper();
		this.jsonHelper = new JsonExpectationsHelper();
	}


	/**
	 * Assert the request content type as a String.
	 */
	public RequestMatcher contentType(String expectedContentType) {
		return contentType(MediaType.parseMediaType(expectedContentType));
	}

	/**
	 * Assert the request content type as a {@link MediaType}.
	 */
	public RequestMatcher contentType(final MediaType expectedContentType) {
		return request -> {
			MediaType actualContentType = request.getHeaders().getContentType();
			assertTrue("Content type not set", actualContentType != null);
			assertEquals("Content type", expectedContentType, actualContentType);
		};
	}

	/**
	 * Assert the request content type is compatible with the given
	 * content type as defined by {@link MediaType#isCompatibleWith(MediaType)}.
	 */
	public RequestMatcher contentTypeCompatibleWith(String contentType) {
		return contentTypeCompatibleWith(MediaType.parseMediaType(contentType));
	}

	/**
	 * Assert the request content type is compatible with the given
	 * content type as defined by {@link MediaType#isCompatibleWith(MediaType)}.
	 */
	public RequestMatcher contentTypeCompatibleWith(final MediaType contentType) {
		return request -> {
			MediaType actualContentType = request.getHeaders().getContentType();
			assertTrue("Content type not set", actualContentType != null);
			if (actualContentType != null) {
				assertTrue("Content type [" + actualContentType + "] is not compatible with [" + contentType + "]",
						actualContentType.isCompatibleWith(contentType));
			}
		};
	}

	/**
	 * Get the body of the request as a UTF-8 string and apply the given {@link Matcher}.
	 */
	public RequestMatcher string(final Matcher<? super String> matcher) {
		return request -> {
			MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
			assertThat("Request content", mockRequest.getBodyAsString(), matcher);
		};
	}

	/**
	 * Get the body of the request as a UTF-8 string and compare it to the given String.
	 */
	public RequestMatcher string(final String expectedContent) {
		return request -> {
			MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
			assertEquals("Request content", expectedContent, mockRequest.getBodyAsString());
		};
	}

	/**
	 * Compare the body of the request to the given byte array.
	 */
	public RequestMatcher bytes(final byte[] expectedContent) {
		return request -> {
			MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
			assertEquals("Request content", expectedContent, mockRequest.getBodyAsBytes());
		};
	}

	/**
	 * Parse the body as form data and compare to the given {@code MultiValueMap}.
	 * @since 4.3
	 */
	public RequestMatcher formData(final MultiValueMap<String, String> expectedContent) {
		return request -> {
			HttpInputMessage inputMessage = new HttpInputMessage() {
				@Override
				public InputStream getBody() throws IOException {
					MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
					return new ByteArrayInputStream(mockRequest.getBodyAsBytes());
				}
				@Override
				public HttpHeaders getHeaders() {
					return request.getHeaders();
				}
			};
			FormHttpMessageConverter converter = new FormHttpMessageConverter();
			assertEquals("Request content", expectedContent, converter.read(null, inputMessage));
		};
	}

	/**
	 * Parse the request body and the given String as XML and assert that the
	 * two are "similar" - i.e. they contain the same elements and attributes
	 * regardless of order.
	 * <p>Use of this matcher assumes the
	 * <a href="http://xmlunit.sourceforge.net/">XMLUnit</a> library is available.
	 * @param expectedXmlContent the expected XML content
	 */
	public RequestMatcher xml(final String expectedXmlContent) {
		return new AbstractXmlRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xmlHelper.assertXmlEqual(expectedXmlContent, request.getBodyAsString());
			}
		};
	}

	/**
	 * Parse the request content as {@link Node} and apply the given {@link Matcher}.
	 */
	public RequestMatcher node(final Matcher<? super Node> matcher) {
		return new AbstractXmlRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xmlHelper.assertNode(request.getBodyAsString(), matcher);
			}
		};
	}

	/**
	 * Parse the request content as {@link DOMSource} and apply the given {@link Matcher}.
	 * @see <a href="https://code.google.com/p/xml-matchers/">https://code.google.com/p/xml-matchers/</a>
	 */
	public RequestMatcher source(final Matcher<? super Source> matcher) {
		return new AbstractXmlRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xmlHelper.assertSource(request.getBodyAsString(), matcher);
			}
		};
	}

	/**
	 * Parse the expected and actual strings as JSON and assert the two
	 * are "similar" - i.e. they contain the same attribute-value pairs
	 * regardless of formatting with a lenient checking (extensible, and non-strict array
	 * ordering).
	 * <p>Use of this matcher requires the <a
	 * href="https://jsonassert.skyscreamer.org/">JSONassert</a> library.
	 * @param expectedJsonContent the expected JSON content
	 * @since 5.0.5
	 */
	public RequestMatcher json(final String expectedJsonContent) {
		return json(expectedJsonContent, false);
	}

	/**
	 * Parse the request body and the given string as JSON and assert the two
	 * are "similar" - i.e. they contain the same attribute-value pairs
	 * regardless of formatting.
	 * <p>Can compare in two modes, depending on {@code strict} parameter value:
	 * <ul>
	 * <li>{@code true}: strict checking. Not extensible, and strict array ordering.</li>
	 * <li>{@code false}: lenient checking. Extensible, and non-strict array ordering.</li>
	 * </ul>
	 * <p>Use of this matcher requires the <a
	 * href="https://jsonassert.skyscreamer.org/">JSONassert</a> library.
	 * @param expectedJsonContent the expected JSON content
	 * @param strict enables strict checking
	 * @since 5.0.5
	 */
	public RequestMatcher json(final String expectedJsonContent, final boolean strict) {
		return request -> {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				this.jsonHelper.assertJsonEqual(expectedJsonContent, mockRequest.getBodyAsString(), strict);
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to parse expected or actual JSON request content", ex);
			}
		};
	}


	/**
	 * Abstract base class for XML {@link RequestMatcher}'s.
	 */
	private abstract static class AbstractXmlRequestMatcher implements RequestMatcher {

		@Override
		public final void match(ClientHttpRequest request) throws IOException, AssertionError {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				matchInternal(mockRequest);
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to parse expected or actual XML request content", ex);
			}
		}

		protected abstract void matchInternal(MockClientHttpRequest request) throws Exception;
	}

}
