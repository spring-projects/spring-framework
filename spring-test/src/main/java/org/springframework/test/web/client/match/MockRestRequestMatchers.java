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

package org.springframework.test.web.client.match;

import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * Static factory methods for {@link RequestMatcher} classes. Typically used to
 * provide input for {@link MockRestServiceServer#expect(RequestMatcher)}.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to
 * this setting, open the Preferences and type "favorites".
 *
 * @author Craig Walls
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public abstract class MockRestRequestMatchers {

	/**
	 * Match to any request.
	 */
	public static RequestMatcher anything() {
		return request -> {};
	}

	/**
	 * Assert the {@link HttpMethod} of the request.
	 * @param method the HTTP method
	 * @return the request matcher
	 */
	public static RequestMatcher method(final HttpMethod method) {
		Assert.notNull(method, "'method' must not be null");
		return request -> assertEquals("Unexpected HttpMethod", method, request.getMethod());
	}

	/**
	 * Assert the request URI string with the given Hamcrest matcher.
	 * @param matcher the String matcher for the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestTo(final Matcher<String> matcher) {
		Assert.notNull(matcher, "'matcher' must not be null");
		return request -> assertThat("Request URI", request.getURI().toString(), matcher);
	}

	/**
	 * Assert the request URI matches the given string.
	 * @param expectedUri the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestTo(final String expectedUri) {
		Assert.notNull(expectedUri, "'uri' must not be null");
		return request -> assertEquals("Request URI", expectedUri, request.getURI().toString());
	}

	/**
	 * Variant of {@link #requestTo(URI)} that prepares the URI from a URI
	 * template plus optional variables via {@link UriComponentsBuilder}
	 * including encoding.
	 * @param expectedUri the expected URI template
	 * @param uriVars zero or more URI variables to populate the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestToUriTemplate(final String expectedUri, final Object... uriVars) {
		Assert.notNull(expectedUri, "'uri' must not be null");
		URI uri = UriComponentsBuilder.fromUriString(expectedUri).buildAndExpand(uriVars).encode().toUri();
		return requestTo(uri);
	}

	/**
	 * Expect a request to the given URI.
	 * @param uri the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestTo(final URI uri) {
		Assert.notNull(uri, "'uri' must not be null");
		return request -> assertEquals("Unexpected request", uri, request.getURI());
	}

	/**
	 * Assert request query parameter values with the given Hamcrest matcher(s).
	 */
	@SafeVarargs
	public static RequestMatcher queryParam(final String name, final Matcher<? super String>... matchers) {
		return request -> {
			MultiValueMap<String, String> params = getQueryParams(request);
			assertValueCount("query param", name, params, matchers.length);
			for (int i = 0 ; i < matchers.length; i++) {
				assertThat("Query param", params.get(name).get(i), matchers[i]);
			}
		};
	}

	/**
	 * Assert request query parameter values.
	 */
	public static RequestMatcher queryParam(final String name, final String... expectedValues) {
		return request -> {
			MultiValueMap<String, String> params = getQueryParams(request);
			assertValueCount("query param", name, params, expectedValues.length);
			for (int i = 0 ; i < expectedValues.length; i++) {
				assertEquals("Query param [" + name + "]", expectedValues[i], params.get(name).get(i));
			}
		};
	}

	private static MultiValueMap<String, String> getQueryParams(ClientHttpRequest request) {
		return UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
	}

	private static void assertValueCount(
			String valueType, final String name, MultiValueMap<String, String> map, int count) {

		List<String> values = map.get(name);
		String message = "Expected " + valueType + " <" + name + ">";
		if (values == null) {
			fail(message + " to exist but was null");
		}
		if (count > values.size()) {
			fail(message + " to have at least <" + count + "> values but found " + values);
		}
	}

	/**
	 * Assert request header values with the given Hamcrest matcher(s).
	 */
	@SafeVarargs
	public static RequestMatcher header(final String name, final Matcher<? super String>... matchers) {
		return request -> {
			assertValueCount("header", name, request.getHeaders(), matchers.length);
			List<String> headerValues = request.getHeaders().get(name);
			Assert.state(headerValues != null, "No header values");
			for (int i = 0; i < matchers.length; i++) {
				assertThat("Request header [" + name + "]", headerValues.get(i), matchers[i]);
			}
		};
	}

	/**
	 * Assert request header values.
	 */
	public static RequestMatcher header(final String name, final String... expectedValues) {
		return request -> {
			assertValueCount("header", name, request.getHeaders(), expectedValues.length);
			List<String> headerValues = request.getHeaders().get(name);
			Assert.state(headerValues != null, "No header values");
			for (int i = 0; i < expectedValues.length; i++) {
				assertEquals("Request header [" + name + "]", expectedValues[i], headerValues.get(i));
			}
		};
	}

	/**
	 * Access to request body matchers.
	 */
	public static ContentRequestMatchers content() {
		return new ContentRequestMatchers();
	}

	/**
	 * Access to request body matchers using a
	 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression to
	 * inspect a specific subset of the body. The JSON path expression can be a
	 * parameterized string using formatting specifiers as defined in
	 * {@link String#format(String, Object...)}.
	 * @param expression the JSON path optionally parameterized with arguments
	 * @param args arguments to parameterize the JSON path expression with
	 */
	public static JsonPathRequestMatchers jsonPath(String expression, Object... args) {
		return new JsonPathRequestMatchers(expression, args);
	}

	/**
	 * Access to request body matchers using a
	 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression to
	 * inspect a specific subset of the body and a Hamcrest match for asserting
	 * the value found at the JSON path.
	 * @param expression the JSON path expression
	 * @param matcher a matcher for the value expected at the JSON path
	 */
	public static <T> RequestMatcher jsonPath(String expression, Matcher<T> matcher) {
		return new JsonPathRequestMatchers(expression).value(matcher);
	}

	/**
	 * Access to request body matchers using an XPath to inspect a specific
	 * subset of the body. The XPath expression can be a parameterized string
	 * using formatting specifiers as defined in
	 * {@link String#format(String, Object...)}.
	 * @param expression the XPath optionally parameterized with arguments
	 * @param args arguments to parameterize the XPath expression with
	 */
	public static XpathRequestMatchers xpath(String expression, Object... args) throws XPathExpressionException {
		return new XpathRequestMatchers(expression, null, args);
	}

	/**
	 * Access to response body matchers using an XPath to inspect a specific
	 * subset of the body. The XPath expression can be a parameterized string
	 * using formatting specifiers as defined in
	 * {@link String#format(String, Object...)}.
	 * @param expression the XPath optionally parameterized with arguments
	 * @param namespaces namespaces referenced in the XPath expression
	 * @param args arguments to parameterize the XPath expression with
	 */
	public static XpathRequestMatchers xpath(String expression, Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		return new XpathRequestMatchers(expression, namespaces, args);
	}

}
