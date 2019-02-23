/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;

import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Static factory methods for {@link ResultMatcher}-based result actions.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to
 * this setting, open the Preferences and type "favorites".
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 3.2
 */
public abstract class MockMvcResultMatchers {

	private static final AntPathMatcher pathMatcher = new AntPathMatcher();


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
	 * <p>This method accepts only exact matches.
	 * @param expectedUrl the exact URL expected
	 */
	public static ResultMatcher forwardedUrl(@Nullable String expectedUrl) {
		return result -> assertEquals("Forwarded URL", expectedUrl, result.getResponse().getForwardedUrl());
	}

	/**
	 * Asserts the request was forwarded to the given URL template.
	 * <p>This method accepts exact matches against the expanded and encoded URL template.
	 * @param urlTemplate a URL template; the expanded URL will be encoded
	 * @param uriVars zero or more URI variables to populate the template
	 * @see UriComponentsBuilder#fromUriString(String)
	 */
	public static ResultMatcher forwardedUrlTemplate(String urlTemplate, Object... uriVars) {
		String uri = UriComponentsBuilder.fromUriString(urlTemplate).buildAndExpand(uriVars).encode().toUriString();
		return forwardedUrl(uri);
	}

	/**
	 * Asserts the request was forwarded to the given URL.
	 * <p>This method accepts {@link org.springframework.util.AntPathMatcher}
	 * patterns.
	 * @param urlPattern an AntPath pattern to match against
	 * @since 4.0
	 * @see org.springframework.util.AntPathMatcher
	 */
	public static ResultMatcher forwardedUrlPattern(String urlPattern) {
		return result -> {
			assertTrue("AntPath pattern", pathMatcher.isPattern(urlPattern));
			String url = result.getResponse().getForwardedUrl();
			assertTrue("Forwarded URL does not match the expected URL pattern",
					(url != null && pathMatcher.match(urlPattern, url)));
		};
	}

	/**
	 * Asserts the request was redirected to the given URL.
	 * <p>This method accepts only exact matches.
	 * @param expectedUrl the exact URL expected
	 */
	public static ResultMatcher redirectedUrl(String expectedUrl) {
		return result -> assertEquals("Redirected URL", expectedUrl, result.getResponse().getRedirectedUrl());
	}

	/**
	 * Asserts the request was redirected to the given URL template.
	 * <p>This method accepts exact matches against the expanded and encoded URL template.
	 * @param urlTemplate a URL template; the expanded URL will be encoded
	 * @param uriVars zero or more URI variables to populate the template
	 * @see UriComponentsBuilder#fromUriString(String)
	 */
	public static ResultMatcher redirectedUrlTemplate(String urlTemplate, Object... uriVars) {
		String uri = UriComponentsBuilder.fromUriString(urlTemplate).buildAndExpand(uriVars).encode().toUriString();
		return redirectedUrl(uri);
	}

	/**
	 * Asserts the request was redirected to the given URL.
	 * <p>This method accepts {@link org.springframework.util.AntPathMatcher}
	 * patterns.
	 * @param urlPattern an AntPath pattern to match against
	 * @since 4.0
	 * @see org.springframework.util.AntPathMatcher
	 */
	public static ResultMatcher redirectedUrlPattern(String urlPattern) {
		return result -> {
			assertTrue("No Ant-style path pattern", pathMatcher.isPattern(urlPattern));
			String url = result.getResponse().getRedirectedUrl();
			assertTrue("Redirected URL does not match the expected URL pattern",
					(url != null && pathMatcher.match(urlPattern, url)));
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
	 * Access to response body assertions using a
	 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression
	 * to inspect a specific subset of the body.
	 * <p>The JSON path expression can be a parameterized string using
	 * formatting specifiers as defined in
	 * {@link String#format(String, Object...)}.
	 * @param expression the JSON path expression, optionally parameterized with arguments
	 * @param args arguments to parameterize the JSON path expression with
	 */
	public static JsonPathResultMatchers jsonPath(String expression, Object... args) {
		return new JsonPathResultMatchers(expression, args);
	}

	/**
	 * Access to response body assertions using a
	 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression
	 * to inspect a specific subset of the body and a Hamcrest matcher for
	 * asserting the value found at the JSON path.
	 * @param expression the JSON path expression
	 * @param matcher a matcher for the value expected at the JSON path
	 */
	public static <T> ResultMatcher jsonPath(String expression, Matcher<T> matcher) {
		return new JsonPathResultMatchers(expression).value(matcher);
	}

	/**
	 * Access to response body assertions using an XPath expression to
	 * inspect a specific subset of the body.
	 * <p>The XPath expression can be a parameterized string using formatting
	 * specifiers as defined in {@link String#format(String, Object...)}.
	 * @param expression the XPath expression, optionally parameterized with arguments
	 * @param args arguments to parameterize the XPath expression with
	 */
	public static XpathResultMatchers xpath(String expression, Object... args) throws XPathExpressionException {
		return new XpathResultMatchers(expression, null, args);
	}

	/**
	 * Access to response body assertions using an XPath expression to
	 * inspect a specific subset of the body.
	 * <p>The XPath expression can be a parameterized string using formatting
	 * specifiers as defined in {@link String#format(String, Object...)}.
	 * @param expression the XPath expression, optionally parameterized with arguments
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
