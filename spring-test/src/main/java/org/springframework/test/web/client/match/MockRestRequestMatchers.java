/*
 * Copyright 2002-2023 the original author or authors.
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
 * @author Simon Baslé
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
	public static RequestMatcher method(HttpMethod method) {
		Assert.notNull(method, "'method' must not be null");
		return request -> assertEquals("Unexpected HttpMethod", method, request.getMethod());
	}

	/**
	 * Assert the request URI string with the given Hamcrest matcher.
	 * @param matcher the String matcher for the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestTo(Matcher<? super String> matcher) {
		Assert.notNull(matcher, "'matcher' must not be null");
		return request -> assertThat("Request URI", request.getURI().toString(), matcher);
	}

	/**
	 * Assert the request URI matches the given string.
	 * @param expectedUri the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestTo(String expectedUri) {
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
	public static RequestMatcher requestToUriTemplate(String expectedUri, Object... uriVars) {
		Assert.notNull(expectedUri, "'uri' must not be null");
		URI uri = UriComponentsBuilder.fromUriString(expectedUri).buildAndExpand(uriVars).encode().toUri();
		return requestTo(uri);
	}

	/**
	 * Expect a request to the given URI.
	 * @param uri the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestTo(URI uri) {
		Assert.notNull(uri, "'uri' must not be null");
		return request -> assertEquals("Unexpected request", uri, request.getURI());
	}

	/**
	 * Assert request query parameter values with the given Hamcrest matcher,
	 * matching on the entire {@link List} of values.
	 * <p>For example, this can be used to check that the list of query parameter
	 * values has at least one value matching a given Hamcrest matcher (such as
	 * {@link org.hamcrest.Matchers#hasItem(Matcher)}), that every value in the list
	 * matches common criteria (such as {@link org.hamcrest.Matchers#everyItem(Matcher)}),
	 * that each value in the list matches corresponding dedicated criteria
	 * (such as {@link org.hamcrest.Matchers#contains(Matcher[])}), etc.
	 * @param name the name of the query parameter whose value(s) will be asserted
	 * @param matcher the Hamcrest matcher to apply to the entire list of values
	 * for the given query parameter
	 * @since 5.3.26
	 * @see #queryParam(String, Matcher...)
	 * @see #queryParam(String, String...)
	 */
	public static RequestMatcher queryParam(String name, Matcher<? super List<String>> matcher) {
		return request -> {
			MultiValueMap<String, String> params = getQueryParams(request);
			List<String> paramValues = params.get(name);
			if (paramValues == null) {
				fail("Expected query param <" + name + "> to exist but was null");
			}
			assertThat("Query param [" + name + "] values", paramValues, matcher);
		};
	}

	/**
	 * Assert request query parameter values with the given Hamcrest matcher(s).
	 * <p>If the query parameter value list is larger than the number of provided
	 * {@code matchers}, no matchers will be applied to the extra query parameter
	 * values, effectively ignoring the additional parameter values. If the number
	 * of provided {@code matchers} exceeds the number of query parameter values,
	 * an {@link AssertionError} will be thrown to signal the mismatch.
	 * <p>See {@link #queryParam(String, Matcher)} for a variant which accepts a
	 * {@code Matcher} that applies to the entire list of values as opposed to
	 * applying only to individual values.
	 * @param name the name of the query parameter whose value(s) will be asserted
	 * @param matchers the Hamcrest matchers to apply to individual query parameter
	 * values; the n<sup>th</sup> matcher is applied to the n<sup>th</sup> query
	 * parameter value
	 * @see #queryParam(String, Matcher)
	 * @see #queryParam(String, String...)
	 */
	@SafeVarargs
	public static RequestMatcher queryParam(String name, Matcher<? super String>... matchers) {
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
	 * <p>If the query parameter value list is larger than the number of
	 * {@code expectedValues}, no assertions will be applied to the extra query
	 * parameter values, effectively ignoring the additional parameter values. If
	 * the number of {@code expectedValues} exceeds the number of query parameter
	 * values, an {@link AssertionError} will be thrown to signal the mismatch.
	 * <p>See {@link #queryParam(String, Matcher)} for a variant which accepts a
	 * Hamcrest {@code Matcher} that applies to the entire list of values as opposed
	 * to asserting only individual values.
	 * @param name the name of the query parameter whose value(s) will be asserted
	 * @param expectedValues the expected values of individual query parameter values;
	 * the n<sup>th</sup> expected value is compared to the n<sup>th</sup> query
	 * parameter value
	 * @see #queryParam(String, Matcher)
	 * @see #queryParam(String, Matcher...)
	 */
	public static RequestMatcher queryParam(String name, String... expectedValues) {
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

	/**
	 * Assert request header values with the given Hamcrest matcher, matching on
	 * the entire {@link List} of values.
	 * <p>For example, this can be used to check that the list of header values
	 * has at least one value matching a given Hamcrest matcher (such as
	 * {@link org.hamcrest.Matchers#hasItem(Matcher)}), that every value in the list
	 * matches common criteria (such as {@link org.hamcrest.Matchers#everyItem(Matcher)}),
	 * that each value in the list matches corresponding dedicated criteria
	 * (such as {@link org.hamcrest.Matchers#contains(Matcher[])}), etc.
	 * @param name the name of the header whose value(s) will be asserted
	 * @param matcher the Hamcrest matcher to apply to the entire list of values
	 * for the given header
	 * @since 5.3.26
	 * @see #header(String, Matcher...)
	 * @see #header(String, String...)
	 */
	public static RequestMatcher header(String name, Matcher<? super List<String>> matcher) {
		return request -> {
			List<String> headerValues = request.getHeaders().get(name);
			if (headerValues == null) {
				fail("Expected header <" + name + "> to exist but was null");
			}
			assertThat("Request header [" + name + "] values", headerValues, matcher);
		};
	}

	/**
	 * Assert request header values with the given Hamcrest matcher(s).
	 * <p>If the header value list is larger than the number of provided
	 * {@code matchers}, no matchers will be applied to the extra header values,
	 * effectively ignoring the additional header values. If the number of
	 * provided {@code matchers} exceeds the number of header values, an
	 * {@link AssertionError} will be thrown to signal the mismatch.
	 * <p>See {@link #header(String, Matcher)} for a variant which accepts a
	 * Hamcrest {@code Matcher} that applies to the entire list of values as
	 * opposed to applying only to individual values.
	 * @param name the name of the header whose value(s) will be asserted
	 * @param matchers the Hamcrest matchers to apply to individual header values;
	 * the n<sup>th</sup> matcher is applied to the n<sup>th</sup> header value
	 * @see #header(String, Matcher)
	 * @see #header(String, String...)
	 */
	@SafeVarargs
	public static RequestMatcher header(String name, Matcher<? super String>... matchers) {
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
	 * <p>If the header value list is larger than the number of {@code expectedValues},
	 * no matchers will be applied to the extra header values, effectively ignoring the
	 * additional header values. If the number of {@code expectedValues} exceeds the
	 * number of header values, an {@link AssertionError} will be thrown to signal the
	 * mismatch.
	 * <p>See {@link #header(String, Matcher)} for a variant which accepts a
	 * Hamcrest {@code Matcher} that applies to the entire list of values as
	 * opposed to applying only to individual values.
	 * @param name the name of the header whose value(s) will be asserted
	 * @param expectedValues the expected values of individual header values; the
	 * n<sup>th</sup> expected value is compared to the n<sup>th</sup> header value
	 * @see #header(String, Matcher)
	 * @see #header(String, Matcher...)
	 */
	public static RequestMatcher header(String name, String... expectedValues) {
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
	 * Assert that the given request header does not exist.
	 * @since 5.2
	 */
	public static RequestMatcher headerDoesNotExist(String name) {
		return request -> {
			List<String> headerValues = request.getHeaders().get(name);
			if (headerValues != null) {
				fail("Expected header <" + name + "> not to exist, but it exists with values: " +
						headerValues);
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
	public static <T> RequestMatcher jsonPath(String expression, Matcher<? super T> matcher) {
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
	 * @param namespaces the namespaces referenced in the XPath expression
	 * @param args arguments to parameterize the XPath expression with
	 */
	public static XpathRequestMatchers xpath(String expression, Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		return new XpathRequestMatchers(expression, namespaces, args);
	}


	private static void assertValueCount(
			String valueType, String name, MultiValueMap<String, String> map, int count) {

		List<String> values = map.get(name);
		String message = "Expected " + valueType + " <" + name + ">";
		if (values == null) {
			fail(message + " to exist but was null");
		}
		if (count > values.size()) {
			fail(message + " to have at least <" + count + "> values but found " + values);
		}
	}

}
