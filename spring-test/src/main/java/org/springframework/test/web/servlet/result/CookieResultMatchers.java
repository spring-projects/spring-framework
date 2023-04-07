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

package org.springframework.test.web.servlet.result;

import jakarta.servlet.http.Cookie;
import org.hamcrest.Matcher;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.util.AssertionErrors.assertNull;

/**
 * Factory for response cookie assertions.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#cookie}.
 *
 * @author Rossen Stoyanchev
 * @author Thomas Bruyelle
 * @since 3.2
 */
public class CookieResultMatchers {

	/**
	 * Protected constructor.
	 * Use {@link MockMvcResultMatchers#cookie()}.
	 */
	protected CookieResultMatchers() {
	}


	/**
	 * Assert a cookie value with the given Hamcrest {@link Matcher}.
	 */
	public ResultMatcher value(String name, Matcher<? super String> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "'", cookie.getValue(), matcher);
		};
	}

	/**
	 * Assert a cookie value.
	 */
	public ResultMatcher value(String name, String expectedValue) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie", expectedValue, cookie.getValue());
		};
	}

	/**
	 * Assert a cookie exists. The existence check is irrespective of whether
	 * max age is 0 (i.e. expired).
	 */
	public ResultMatcher exists(String name) {
		return result -> getCookie(result, name);
	}

	/**
	 * Assert a cookie does not exist. Note that the existence check is
	 * irrespective of whether max age is 0, i.e. expired.
	 */
	public ResultMatcher doesNotExist(String name) {
		return result -> {
			Cookie cookie = result.getResponse().getCookie(name);
			assertNull("Unexpected cookie with name '" + name + "'", cookie);
		};
	}

	/**
	 * Assert a cookie's maxAge with a Hamcrest {@link Matcher}.
	 */
	public ResultMatcher maxAge(String name, Matcher<? super Integer> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' maxAge", cookie.getMaxAge(), matcher);
		};
	}

	/**
	 * Assert a cookie's maxAge.
	 */
	public ResultMatcher maxAge(String name, int maxAge) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' maxAge", maxAge, cookie.getMaxAge());
		};
	}

	/**
	 * Assert a cookie's path with a Hamcrest {@link Matcher}.
	 */
	public ResultMatcher path(String name, Matcher<? super String> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' path", cookie.getPath(), matcher);
		};
	}

	/**
	 * Assert a cookie's path.
	 */
	public ResultMatcher path(String name, String path) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' path", path, cookie.getPath());
		};
	}

	/**
	 * Assert a cookie's domain with a Hamcrest {@link Matcher}.
	 */
	public ResultMatcher domain(String name, Matcher<? super String> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' domain", cookie.getDomain(), matcher);
		};
	}

	/**
	 * Assert a cookie's domain.
	 */
	public ResultMatcher domain(String name, String domain) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' domain", domain, cookie.getDomain());
		};
	}

	/**
	 * Assert a cookie's SameSite attribute with a Hamcrest {@link Matcher}.
	 * @since 6.0.8
	 * @see #attribute(String, String, Matcher)
	 */
	public ResultMatcher sameSite(String name, Matcher<? super String> matcher) {
		return attribute(name, "SameSite", matcher);
	}

	/**
	 * Assert a cookie's SameSite attribute.
	 * @since 6.0.8
	 * @see #attribute(String, String, String)
	 */
	public ResultMatcher sameSite(String name, String sameSite) {
		return attribute(name, "SameSite", sameSite);
	}

	/**
	 * Assert a cookie's comment with a Hamcrest {@link Matcher}.
	 */
	@SuppressWarnings("removal")
	public ResultMatcher comment(String name, Matcher<? super String> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' comment", cookie.getComment(), matcher);
		};
	}

	/**
	 * Assert a cookie's comment.
	 */
	@SuppressWarnings("removal")
	public ResultMatcher comment(String name, String comment) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' comment", comment, cookie.getComment());
		};
	}

	/**
	 * Assert a cookie's version with a Hamcrest {@link Matcher}.
	 */
	@SuppressWarnings("removal")
	public ResultMatcher version(String name, Matcher<? super Integer> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' version", cookie.getVersion(), matcher);
		};
	}

	/**
	 * Assert a cookie's version.
	 */
	@SuppressWarnings("removal")
	public ResultMatcher version(String name, int version) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' version", version, cookie.getVersion());
		};
	}

	/**
	 * Assert whether the cookie must be sent over a secure protocol or not.
	 */
	public ResultMatcher secure(String name, boolean secure) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' secure", secure, cookie.getSecure());
		};
	}

	/**
	 * Assert whether the cookie must be HTTP only.
	 * @since 4.3.9
	 */
	public ResultMatcher httpOnly(String name, boolean httpOnly) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' httpOnly", httpOnly, cookie.isHttpOnly());
		};
	}

	/**
	 * Assert a cookie's specified attribute with a Hamcrest {@link Matcher}.
	 * @param cookieAttribute the name of the Cookie attribute (case-insensitive)
	 * @since 6.0.8
	 */
	public ResultMatcher attribute(String cookieName, String cookieAttribute, Matcher<? super String> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, cookieName);
			String attribute = cookie.getAttribute(cookieAttribute);
			assertNotNull("Response cookie '" + cookieName + "' doesn't have attribute '" + cookieAttribute + "'", attribute);
			assertThat("Response cookie '" + cookieName + "' attribute '" + cookieAttribute + "'",
					attribute, matcher);
		};
	}

	/**
	 * Assert a cookie's specified attribute.
	 * @param cookieAttribute the name of the Cookie attribute (case-insensitive)
	 * @since 6.0.8
	 */
	public ResultMatcher attribute(String cookieName, String cookieAttribute, String attributeValue) {
		return result -> {
			Cookie cookie = getCookie(result, cookieName);
			assertEquals("Response cookie '" + cookieName + "' attribute '" + cookieAttribute + "'",
					attributeValue, cookie.getAttribute(cookieAttribute));
		};
	}


	private static Cookie getCookie(MvcResult result, String name) {
		Cookie cookie = result.getResponse().getCookie(name);
		assertNotNull("No cookie with name '" + name + "'", cookie);
		return cookie;
	}

}
