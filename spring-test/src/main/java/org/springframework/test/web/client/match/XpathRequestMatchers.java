/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;
import org.w3c.dom.Node;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.XpathExpectationsHelper;
import org.springframework.test.web.client.RequestMatcher;

/**
 * Factory methods for request content {@code RequestMatcher} implementations
 * that use an XPath expression.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockRestRequestMatchers#xpath(String, Object...)} or
 * {@link MockRestRequestMatchers#xpath(String, Map, Object...)}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class XpathRequestMatchers {

	private static final String DEFAULT_ENCODING = "UTF-8";

	private final XpathExpectationsHelper xpathHelper;


	/**
	 * Class constructor, not for direct instantiation.
	 * <p>Use {@link MockRestRequestMatchers#xpath(String, Object...)} or
	 * {@link MockRestRequestMatchers#xpath(String, Map, Object...)}.
	 * @param expression the XPath expression
	 * @param namespaces the XML namespaces referenced in the XPath expression, or {@code null}
	 * @param args arguments to parameterize the XPath expression with, using the
	 * formatting specifiers defined in {@link String#format(String, Object...)}
	 * @throws XPathExpressionException if expression compilation failed
	 */
	protected XpathRequestMatchers(String expression, @Nullable Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		this.xpathHelper = new XpathExpectationsHelper(expression, namespaces, args);
	}


	/**
	 * Apply the XPath and assert it with the given {@code Matcher<Node>}.
	 */
	public RequestMatcher node(Matcher<? super Node> matcher) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertNode(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
	}

	/**
	 * Assert that content exists at the given XPath.
	 */
	public RequestMatcher exists() {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.exists(request.getBodyAsBytes(), DEFAULT_ENCODING);
	}

	/**
	 * Assert that content does not exist at the given XPath.
	 */
	public RequestMatcher doesNotExist() {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.doesNotExist(request.getBodyAsBytes(), DEFAULT_ENCODING);
	}

	/**
	 * Apply the XPath and assert the number of nodes found with the given
	 * {@code Matcher<Integer>}.
	 */
	public RequestMatcher nodeCount(Matcher<Integer> matcher) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertNodeCount(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
	}

	/**
	 * Apply the XPath and assert the number of nodes found.
	 */
	public RequestMatcher nodeCount(int expectedCount) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertNodeCount(request.getBodyAsBytes(), DEFAULT_ENCODING, expectedCount);
	}

	/**
	 * Apply the XPath and assert the String content found with the given matcher.
	 */
	public RequestMatcher string(Matcher<? super String> matcher) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertString(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
	}

	/**
	 * Apply the XPath and assert the String content found.
	 */
	public RequestMatcher string(String content) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertString(request.getBodyAsBytes(), DEFAULT_ENCODING, content);
	}

	/**
	 * Apply the XPath and assert the number found with the given matcher.
	 */
	public RequestMatcher number(Matcher<? super Double> matcher) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertNumber(request.getBodyAsBytes(), DEFAULT_ENCODING, matcher);
	}

	/**
	 * Apply the XPath and assert the number value found.
	 */
	public RequestMatcher number(Double value) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertNumber(request.getBodyAsBytes(), DEFAULT_ENCODING, value);
	}

	/**
	 * Apply the XPath and assert the boolean value found.
	 */
	public RequestMatcher booleanValue(Boolean value) {
		return (XpathRequestMatcher) request ->
				this.xpathHelper.assertBoolean(request.getBodyAsBytes(), DEFAULT_ENCODING, value);
	}


	/**
	 * Functional interface for XPath {@link RequestMatcher} implementations.
	 */
	@FunctionalInterface
	private interface XpathRequestMatcher extends RequestMatcher {

		@Override
		default void match(ClientHttpRequest request) {
			try {
				matchInternal((MockClientHttpRequest) request);
			}
			catch (Exception ex) {
				throw new AssertionError("Failed to parse XML request content", ex);
			}
		}

		void matchInternal(MockClientHttpRequest request) throws Exception;
	}

}
