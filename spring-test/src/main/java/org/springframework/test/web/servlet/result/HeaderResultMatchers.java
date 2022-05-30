/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.web.servlet.result;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matcher;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Factory for response header assertions.
 *
 * <p>An instance of this class is available via
 * {@link MockMvcResultMatchers#header}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 3.2
 */
public class HeaderResultMatchers {

	/**
	 * Protected constructor.
	 * See {@link MockMvcResultMatchers#header()}.
	 */
	protected HeaderResultMatchers() {
	}


	/**
	 * Assert the primary value of the response header with the given Hamcrest
	 * String {@code Matcher}.
	 */
	public ResultMatcher string(String name, Matcher<? super String> matcher) {
		return result -> assertThat("Response header '" + name + "'", result.getResponse().getHeader(name), matcher);
	}

	/**
	 * Assert the values of the response header with the given Hamcrest
	 * Iterable {@link Matcher}.
	 * @since 4.3
	 */
	public ResultMatcher stringValues(String name, Matcher<? super Iterable<String>> matcher) {
		return result -> {
			List<String> values = result.getResponse().getHeaders(name);
			assertThat("Response header '" + name + "'", values, matcher);
		};
	}

	/**
	 * Assert the primary value of the response header as a String value.
	 */
	public ResultMatcher string(String name, String value) {
		return result -> assertEquals("Response header '" + name + "'", value, result.getResponse().getHeader(name));
	}

	/**
	 * Assert the values of the response header as String values.
	 * @since 4.3
	 */
	public ResultMatcher stringValues(String name, String... values) {
		return result -> {
			List<Object> actual = result.getResponse().getHeaderValues(name);
			assertEquals("Response header '" + name + "'", Arrays.asList(values), actual);
		};
	}

	/**
	 * Assert that the named response header exists.
	 * @since 5.0.3
	 */
	public ResultMatcher exists(String name) {
		return result -> assertTrue("Response should contain header '" + name + "'",
				result.getResponse().containsHeader(name));
	}

	/**
	 * Assert that the named response header does not exist.
	 * @since 4.0
	 */
	public ResultMatcher doesNotExist(String name) {
		return result -> assertFalse("Response should not contain header '" + name + "'",
				result.getResponse().containsHeader(name));
	}

	/**
	 * Assert the primary value of the named response header as a {@code long}.
	 * <p>The {@link ResultMatcher} returned by this method throws an
	 * {@link AssertionError} if the response does not contain the specified
	 * header, or if the supplied {@code value} does not match the primary value.
	 */
	public ResultMatcher longValue(String name, long value) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			assertTrue("Response does not contain header '" + name + "'", response.containsHeader(name));
			String headerValue = response.getHeader(name);
			if (headerValue != null) {
				assertEquals("Response header '" + name + "'", value, Long.parseLong(headerValue));
			}
		};
	}

	/**
	 * Assert the primary value of the named response header parsed into a date
	 * using the preferred date format described in RFC 7231.
	 * <p>The {@link ResultMatcher} returned by this method throws an
	 * {@link AssertionError} if the response does not contain the specified
	 * header, or if the supplied {@code value} does not match the primary value.
	 * @since 4.2
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
	 */
	public ResultMatcher dateValue(String name, long value) {
		return result -> {
			MockHttpServletResponse response = result.getResponse();
			String headerValue = response.getHeader(name);
			assertNotNull("Response does not contain header '" + name + "'", headerValue);

			HttpHeaders headers = new HttpHeaders();
			headers.setDate("expected", value);
			headers.set("actual", headerValue);

			assertEquals("Response header '" + name + "'='" + headerValue + "' " +
							"does not match expected value '" + headers.getFirst("expected") + "'",
					headers.getFirstDate("expected"), headers.getFirstDate("actual"));
		};
	}

}
