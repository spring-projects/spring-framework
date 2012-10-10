/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.test.web.mock.client.match;

import static org.springframework.test.web.mock.AssertionErrors.assertEquals;
import static org.springframework.test.web.mock.AssertionErrors.assertTrue;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.mock.client.RequestMatcher;
import org.springframework.test.web.mock.support.XmlExpectationsHelper;
import org.w3c.dom.Node;

/**
 * Factory for request content {@code RequestMatcher}'s. An instance of this
 * class is typically accessed via {@link MockRestRequestMatchers#content()}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ContentRequestMatchers {

	private final XmlExpectationsHelper xmlHelper;


	/**
	 * Class constructor, not for direct instantiation.
	 * Use {@link MockRestRequestMatchers#content()}.
	 */
	protected ContentRequestMatchers() {
		this.xmlHelper = new XmlExpectationsHelper();
	}

	/**
	 * Assert the request content type as a String.
	 */
	public RequestMatcher mimeType(String expectedContentType) {
		return mimeType(MediaType.parseMediaType(expectedContentType));
	}

	/**
	 * Assert the request content type as a {@link MediaType}.
	 */
	public RequestMatcher mimeType(final MediaType expectedContentType) {
		return new RequestMatcher() {
			public void match(ClientHttpRequest request) throws IOException, AssertionError {
				MediaType actualContentType = request.getHeaders().getContentType();
				assertTrue("Content type not set", actualContentType != null);
				assertEquals("Content type", expectedContentType, actualContentType);
			}
		};
	}

	/**
	 * Get the body of the request as a UTF-8 string and appply the given {@link Matcher}.
	 */
	public RequestMatcher string(final Matcher<? super String> matcher) {
		return new RequestMatcher() {
			public void match(ClientHttpRequest request) throws IOException, AssertionError {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				MatcherAssert.assertThat("Request content", mockRequest.getBodyAsString(), matcher);
			}
		};
	}

	/**
	 * Get the body of the request as a UTF-8 string and compare it to the given String.
	 */
	public RequestMatcher string(String expectedContent) {
		return string(Matchers.equalTo(expectedContent));
	}

	/**
	 * Compare the body of the request to the given byte array.
	 */
	public RequestMatcher bytes(final byte[] expectedContent) {
		return new RequestMatcher() {
			public void match(ClientHttpRequest request) throws IOException, AssertionError {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				byte[] content = mockRequest.getBodyAsBytes();
				MatcherAssert.assertThat("Request content", content, Matchers.equalTo(expectedContent));
			}
		};
	}

	/**
	 * Parse the request body and the given String as XML and assert that the
	 * two are "similar" - i.e. they contain the same elements and attributes
	 * regardless of order.
	 *
	 * <p>Use of this matcher assumes the
	 * <a href="http://xmlunit.sourceforge.net/">XMLUnit<a/> library is available.
	 *
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
	 * @see <a href="http://code.google.com/p/xml-matchers/">http://code.google.com/p/xml-matchers/</a>
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
	 * Abstract base class for XML {@link RequestMatcher}'s.
	 */
	private abstract static class AbstractXmlRequestMatcher implements RequestMatcher {

		public final void match(ClientHttpRequest request) throws IOException, AssertionError {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				matchInternal(mockRequest);
			}
			catch (Exception e) {
				throw new AssertionError("Failed to parse expected or actual XML request content: " + e.getMessage());
			}
		}

		protected abstract void matchInternal(MockClientHttpRequest request) throws Exception;

	}
}
