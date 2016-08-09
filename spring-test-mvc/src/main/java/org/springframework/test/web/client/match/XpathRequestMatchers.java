/*
 * Copyright 2002-2016 the original author or authors.
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

	private final XpathExpectationsHelper xpathHelper;


	/**
	 * Class constructor, not for direct instantiation. Use
	 * {@link MockRestRequestMatchers#xpath(String, Object...)} or
	 * {@link MockRestRequestMatchers#xpath(String, Map, Object...)}.
	 * @param expression the XPath expression
	 * @param namespaces XML namespaces referenced in the XPath expression, or {@code null}
	 * @param args arguments to parameterize the XPath expression with using the
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @throws XPathExpressionException
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
				xpathHelper.assertNode(request.getBodyAsString(), matcher);
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
				xpathHelper.exists(request.getBodyAsString());
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
				xpathHelper.doesNotExist(request.getBodyAsString());
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
				xpathHelper.assertNodeCount(request.getBodyAsString(), matcher);
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
				xpathHelper.assertNodeCount(request.getBodyAsString(), expectedCount);
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
				xpathHelper.assertString(request.getBodyAsString(), matcher);
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
				xpathHelper.assertString(request.getBodyAsString(), value);
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
				xpathHelper.assertNumber(request.getBodyAsString(), matcher);
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
				xpathHelper.assertNumber(request.getBodyAsString(), value);
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
				xpathHelper.assertBoolean(request.getBodyAsString(), value);
			}
		};
	}


	/**
	 * Abstract base class for XPath {@link RequestMatcher}'s.
	 */
	private abstract static class AbstractXpathRequestMatcher implements RequestMatcher {

		public final void match(ClientHttpRequest request) throws IOException, AssertionError {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				matchInternal(mockRequest);
			}
			catch (Exception e) {
				throw new AssertionError("Failed to parse XML request content: " + e.getMessage());
			}
		}

		protected abstract void matchInternal(MockClientHttpRequest request) throws Exception;
	}

}
