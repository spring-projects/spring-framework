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

import java.io.IOException;
import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.XpathExpectationsHelper;
import org.springframework.test.web.client.RequestMatcher;

/**
 * Factory methods for request content {@code RequestMatcher}'s using an XPath
 * expression. An instance of this class is typically accessed via
 * {@code RequestMatchers.xpath(..)}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class XpathRequestMatchers {

	private static final String DEFAULT_ENCODING = "UTF-8";

	private final XpathExpectationsHelper xpathHelper;


	/**
	 * Class constructor, not for direct instantiation. Use
	 * {@link MockRestRequestMatchers#xpath(String, Object...)} or
	 * {@link MockRestRequestMatchers#xpath(String, Map, Object...)}.
	 * @param expression the XPath expression
	 * @param namespaces XML namespaces referenced in the XPath expression, or {@code null}
	 * @param args arguments to parameterize the XPath expression with using the
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @throws XPathExpressionException if expression compilation failed
	 */
	protected XpathRequestMatchers(String expression, Map<String, String> namespaces, Object ... args)
			throws XPathExpressionException {

		this.xpathHelper = new XpathExpectationsHelper(expression, namespaces, args);
	}


	/**
	 * Apply the XPath and assert it with the given {@code Matcher<Node>}.
	 */
	public <T> RequestMatcher node(final Matcher<? super Node> matcher) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertNode(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
			}
		};
	}

	/**
	 * Assert that content exists at the given XPath.
	 */
	public <T> RequestMatcher exists() {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.exists(request.getBodyAsBytes(), DEFAULT_ENCODING);
			}
		};
	}

	/**
	 * Assert that content does not exist at the given XPath.
	 */
	public <T> RequestMatcher doesNotExist() {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.doesNotExist(request.getBodyAsBytes(), DEFAULT_ENCODING);
			}
		};
	}

	/**
	 * Apply the XPath and assert the number of nodes found with the given
	 * {@code Matcher<Integer>}.
	 */
	public <T> RequestMatcher nodeCount(final Matcher<Integer> matcher) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertNodeCount(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
			}
		};
	}

	/**
	 * Apply the XPath and assert the number of nodes found.
	 */
	public <T> RequestMatcher nodeCount(final int expectedCount) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertNodeCount(request.getBodyAsBytes(), DEFAULT_ENCODING, expectedCount);
			}
		};
	}

	/**
	 * Apply the XPath and assert the String content found with the given matcher.
	 */
	public <T> RequestMatcher string(final Matcher<? super String> matcher) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertString(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
			}
		};
	}

	/**
	 * Apply the XPath and assert the String content found.
	 */
	public RequestMatcher string(final String value) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertString(request.getBodyAsBytes(), DEFAULT_ENCODING, value);
			}
		};
	}

	/**
	 * Apply the XPath and assert the number found with the given matcher.
	 */
	public <T> RequestMatcher number(final Matcher<? super Double> matcher) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertNumber(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
			}
		};
	}

	/**
	 * Apply the XPath and assert the number of nodes found.
	 */
	public RequestMatcher number(final Double value) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertNumber(request.getBodyAsBytes(), DEFAULT_ENCODING, value);
			}
		};
	}

	/**
	 * Apply the XPath and assert the boolean value found.
	 */
	public <T> RequestMatcher booleanValue(final Boolean value) {
		return new AbstractXpathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws Exception {
				xpathHelper.assertBoolean(request.getBodyAsBytes(), DEFAULT_ENCODING, value);
			}
		};
	}


	/**
	 * Abstract base class for XPath {@link RequestMatcher}'s.
	 */
	private abstract static class AbstractXpathRequestMatcher implements RequestMatcher {

		@Override
		public final void match(ClientHttpRequest request) throws IOException, AssertionError {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				matchInternal(mockRequest);
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to parse XML request content: " + ex.getMessage());
			}
		}

		protected abstract void matchInternal(MockClientHttpRequest request) throws Exception;
	}

}
