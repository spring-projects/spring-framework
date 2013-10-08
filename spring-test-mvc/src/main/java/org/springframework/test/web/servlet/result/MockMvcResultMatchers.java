/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.web.servlet.result;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.AntPathMatcher;

/**
 * Static, factory methods for {@link ResultMatcher}-based result actions.
 *
 * <p><strong>Eclipse users:</strong> consider adding this class as a Java editor
 * favorite. To navigate, open the Preferences and type "favorites".
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.2
 */
public abstract class MockMvcResultMatchers {


	private MockMvcResultMatchers() {
	}

	/**
	 * Access to request-related assertions.
	 */
	public static RequestResultMatchers request() {
		return new RequestResultMatchers();
	}

	/**
	 * Access to assertions for the handler that handled the request.
	 */
	public static HandlerResultMatchers handler() {
		return new HandlerResultMatchers();
	}

	/**
	 * Access to model-related assertions.
	 */
	public static ModelResultMatchers model() {
		return new ModelResultMatchers();
	}

	/**
	 * Access to assertions on the selected view.
	 */
	public static ViewResultMatchers view() {
		return new ViewResultMatchers();
	}

	/**
	 * Access to flash attribute assertions.
	 */
	public static FlashAttributeResultMatchers flash() {
		return new FlashAttributeResultMatchers();
	}

	/**
	 * Asserts the request was forwarded to the given URL.
	 * This methods accepts only exact matches.
	 * @param expectedUrl the exact URL expected
	 */
	public static ResultMatcher forwardedUrl(final String expectedUrl) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				assertEquals("Forwarded URL", expectedUrl, result.getResponse().getForwardedUrl());
			}
		};
	}

	/**
	 * Asserts the request was forwarded to the given URL.
	 * This methods accepts {@link org.springframework.util.AntPathMatcher} expressions.
	 *
	 * <p>When trying to match against "?" or "*" exactly, those characters
	 * should be escaped (e.g. "\\?" and "\\*")
	 *
	 * @param expectedUrl an AntPath expression to match against
	 * @see org.springframework.util.AntPathMatcher
	 * @since 4.0
	 */
	public static ResultMatcher forwardedUrlPattern(final String expectedUrl) {
		return new ResultMatcher() {

			private final AntPathMatcher pathMatcher = new AntPathMatcher();

			@Override
			public void match(MvcResult result) {
				assertTrue("AntPath expression",pathMatcher.isPattern(expectedUrl));
				assertTrue("Forwarded URL",
						pathMatcher.match(expectedUrl, result.getResponse().getForwardedUrl()));
			}
		};
	}

	/**
	 * Asserts the request was redirected to the given URL.
	 * This methods accepts only exact matches.
	 * @param expectedUrl the exact URL expected
	 */
	public static ResultMatcher redirectedUrl(final String expectedUrl) {
		return new ResultMatcher() {
			@Override
			public void match(MvcResult result) {
				assertEquals("Redirected URL", expectedUrl, result.getResponse().getRedirectedUrl());
			}
		};
	}

	/**
	 * Asserts the request was redirected to the given URL.
	 * This methods accepts {@link org.springframework.util.AntPathMatcher} expressions.
	 *
	 * <p>When trying to match against "?" or "*" exactly, those characters
	 * should be escaped (e.g. "\\?" and "\\*")
	 *
	 * @param expectedUrl an AntPath expression to match against
	 * @see org.springframework.util.AntPathMatcher
	 * @since 4.0
	 */
	public static ResultMatcher redirectedUrlPattern(final String expectedUrl) {
		return new ResultMatcher() {

			private final AntPathMatcher pathMatcher = new AntPathMatcher();

			@Override
			public void match(MvcResult result) {
				assertTrue("AntPath expression",pathMatcher.isPattern(expectedUrl));
				assertTrue("Redirected URL",
						pathMatcher.match(expectedUrl, result.getResponse().getRedirectedUrl()));
			}
		};
	}

	/**
	 * Access to response status assertions.
	 */
	public static StatusResultMatchers status() {
		return new StatusResultMatchers();
	}

	/**
	 * Access to response header assertions.
	 */
	public static HeaderResultMatchers header() {
		return new HeaderResultMatchers();
	}

	/**
	 * Access to response body assertions.
	 */
	public static ContentResultMatchers content() {
		return new ContentResultMatchers();
	}

	/**
	 * Access to response body assertions using a <a
	 * href="http://goessner.net/articles/JsonPath/">JSONPath</a> expression to
	 * inspect a specific subset of the body. The JSON path expression can be a
	 * parameterized string using formatting specifiers as defined in
	 * {@link String#format(String, Object...)}.
	 *
	 * @param expression the JSON path optionally parameterized with arguments
	 * @param args arguments to parameterize the JSON path expression with
	 */
	public static JsonPathResultMatchers jsonPath(String expression, Object ... args) {
		return new JsonPathResultMatchers(expression, args);
	}

	/**
	 * Access to response body assertions using a <a
	 * href="http://goessner.net/articles/JsonPath/">JSONPath</a> expression to
	 * inspect a specific subset of the body and a Hamcrest match for asserting
	 * the value found at the JSON path.
	 *
	 * @param expression the JSON path expression
	 * @param matcher a matcher for the value expected at the JSON path
	 */
	public static <T> ResultMatcher jsonPath(String expression, Matcher<T> matcher) {
		return new JsonPathResultMatchers(expression).value(matcher);
	}

	/**
	 * Access to response body assertions using an XPath to inspect a specific
	 * subset of the body. The XPath expression can be a parameterized string
	 * using formatting specifiers as defined in
	 * {@link String#format(String, Object...)}.
	 *
	 * @param expression the XPath optionally parameterized with arguments
	 * @param args arguments to parameterize the XPath expression with
	 */
	public static XpathResultMatchers xpath(String expression, Object... args) throws XPathExpressionException {
		return new XpathResultMatchers(expression, null, args);
	}

	/**
	 * Access to response body assertions using an XPath to inspect a specific
	 * subset of the body. The XPath expression can be a parameterized string
	 * using formatting specifiers as defined in
	 * {@link String#format(String, Object...)}.
	 *
	 * @param expression the XPath optionally parameterized with arguments
	 * @param namespaces namespaces referenced in the XPath expression
	 * @param args arguments to parameterize the XPath expression with
	 */
	public static XpathResultMatchers xpath(String expression, Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		return new XpathResultMatchers(expression, namespaces, args);
	}

	/**
	 * Access to response cookie assertions.
	 */
	public static CookieResultMatchers cookie() {
		return new CookieResultMatchers();
	}

}
