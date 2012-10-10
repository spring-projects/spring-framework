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

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.mock.AssertionErrors;
import org.springframework.test.web.mock.client.MockRestServiceServer;
import org.springframework.test.web.mock.client.RequestMatcher;
import org.springframework.util.Assert;

/**
 * Static, factory methods for {@link RequestMatcher} classes. Typically used to
 * provide input for {@link MockRestServiceServer#expect(RequestMatcher)}.
 *
 * <p><strong>Eclipse users:</strong> consider adding this class as a Java editor
 * favorite. To navigate, open the Preferences and type "favorites".
 *
 * @author Craig Walls
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public abstract class MockRestRequestMatchers {


	/**
	 * Private class constructor.
	 */
	private MockRestRequestMatchers() {
	}

	/**
	 * Match to any request.
	 */
	public static RequestMatcher anything() {
		return new RequestMatcher() {
			public void match(ClientHttpRequest request) throws AssertionError {
			}
		};
	}

	/**
	 * Assert the request URI string with the given matcher.
	 *
	 * @param matcher String matcher for the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestTo(final Matcher<String> matcher) {
		Assert.notNull(matcher, "'matcher' must not be null");
		return new RequestMatcher() {
			public void match(ClientHttpRequest request) throws IOException, AssertionError {
				MatcherAssert.assertThat("Request URI", request.getURI().toString(), matcher);
			}
		};
	}

	/**
	 * Assert the request URI string.
	 *
	 * @param uri the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestTo(String uri) {
		Assert.notNull(uri, "'uri' must not be null");
		return requestTo(Matchers.equalTo(uri));
	}

	/**
	 * Assert the {@link HttpMethod} of the request.
	 *
	 * @param method the HTTP method
	 * @return the request matcher
	 */
	public static RequestMatcher method(final HttpMethod method) {
		Assert.notNull(method, "'method' must not be null");
		return new RequestMatcher() {
			public void match(ClientHttpRequest request) throws AssertionError {
				AssertionErrors.assertEquals("Unexpected HttpMethod", method, request.getMethod());
			}
		};
	}

	/**
	 * Expect a request to the given URI.
	 *
	 * @param uri the expected URI
	 * @return the request matcher
	 */
	public static RequestMatcher requestTo(final URI uri) {
		Assert.notNull(uri, "'uri' must not be null");
		return new RequestMatcher() {
			public void match(ClientHttpRequest request) throws IOException, AssertionError {
				AssertionErrors.assertEquals("Unexpected request", uri, request.getURI());
			}
		};
	}

	/**
	 * Assert request header values with the given Hamcrest matcher.
	 */
	public static RequestMatcher header(final String name, final Matcher<? super String>... matchers) {
		return new RequestMatcher() {
			public void match(ClientHttpRequest request) {
				HttpHeaders headers = request.getHeaders();
				List<String> values = headers.get(name);
				AssertionErrors.assertTrue("Expected header <" + name + ">", values != null);
				AssertionErrors.assertTrue("Expected header <" + name + "> to have at least <" + matchers.length
						+ "> values but it has only <" + values.size() + ">", matchers.length <= values.size());
				for (int i = 0 ; i < matchers.length; i++) {
					MatcherAssert.assertThat("Request header", headers.get(name).get(i), matchers[i]);
				}
			}
		};
	}

	/**
	 * Assert request header values.
	 */
	public static RequestMatcher header(String name, String... values) {
		@SuppressWarnings("unchecked")
		Matcher<? super String>[] matchers = new IsEqual[values.length];
		for (int i = 0; i < values.length; i++) {
			matchers[i] = Matchers.equalTo(values[i]);
		}
		return header(name, matchers);
	}

	/**
	 * Access to request body matchers.
	 */
	public static ContentRequestMatchers content() {
		return new ContentRequestMatchers();
	}

	/**
	 * Access to request body matchers using a <a
	 * href="http://goessner.net/articles/JsonPath/">JSONPath</a> expression to
	 * inspect a specific subset of the body. The JSON path expression can be a
	 * parameterized string using formatting specifiers as defined in
	 * {@link String#format(String, Object...)}.
	 *
	 * @param expression the JSON path optionally parameterized with arguments
	 * @param args arguments to parameterize the JSON path expression with
	 */
	public static JsonPathRequestMatchers jsonPath(String expression, Object ... args) {
		return new JsonPathRequestMatchers(expression, args);
	}

	/**
	 * Access to request body matchers using a <a
	 * href="http://goessner.net/articles/JsonPath/">JSONPath</a> expression to
	 * inspect a specific subset of the body and a Hamcrest match for asserting
	 * the value found at the JSON path.
	 *
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
	 *
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
	 *
	 * @param expression the XPath optionally parameterized with arguments
	 * @param namespaces namespaces referenced in the XPath expression
	 * @param args arguments to parameterize the XPath expression with
	 */
	public static XpathRequestMatchers xpath(String expression, Map<String, String> namespaces, Object... args)
			throws XPathExpressionException {

		return new XpathRequestMatchers(expression, namespaces, args);
	}

}
