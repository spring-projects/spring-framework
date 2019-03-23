/*
 * Copyright 2002-2017 the original author or authors.
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

import javax.servlet.http.Cookie;

import org.hamcrest.Matcher;

import org.springframework.test.util.AssertionErrors;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.MatcherAssert.*;
import static org.springframework.test.util.AssertionErrors.*;

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
	public ResultMatcher value(final String name, final Matcher<? super String> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "'", cookie.getValue(), matcher);
		};
	}

	/**
	 * Assert a cookie value.
	 */
	public ResultMatcher value(final String name, final String expectedValue) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie", expectedValue, cookie.getValue());
		};
	}

	/**
	 * Assert a cookie exists. The existence check is irrespective of whether
	 * max age is 0 (i.e. expired).
	 */
	public ResultMatcher exists(final String name) {
		return result -> getCookie(result, name);
	}

	/**
	 * Assert a cookie does not exist. Note that the existence check is
	 * irrespective of whether max age is 0, i.e. expired.
	 */
	public ResultMatcher doesNotExist(final String name) {
		return result -> {
			Cookie cookie = result.getResponse().getCookie(name);
			assertTrue("Unexpected cookie with name '" + name + "'", cookie == null);
		};
	}

	/**
	 * Assert a cookie's maxAge with a Hamcrest {@link Matcher}.
	 */
	public ResultMatcher maxAge(final String name, final Matcher<? super Integer> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' maxAge", cookie.getMaxAge(), matcher);
		};
	}

	/**
	 * Assert a cookie's maxAge value.
	 */
	public ResultMatcher maxAge(final String name, final int maxAge) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' maxAge", maxAge, cookie.getMaxAge());
		};
	}

	/**
	 * Assert a cookie path with a Hamcrest {@link Matcher}.
	 */
	public ResultMatcher path(final String name, final Matcher<? super String> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' path", cookie.getPath(), matcher);
		};
	}

	public ResultMatcher path(final String name, final String path) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' path", path, cookie.getPath());
		};
	}

	/**
	 * Assert a cookie's domain with a Hamcrest {@link Matcher}.
	 */
	public ResultMatcher domain(final String name, final Matcher<? super String> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' domain", cookie.getDomain(), matcher);
		};
	}

	/**
	 * Assert a cookie's domain value.
	 */
	public ResultMatcher domain(final String name, final String domain) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' domain", domain, cookie.getDomain());
		};
	}

	/**
	 * Assert a cookie's comment with a Hamcrest {@link Matcher}.
	 */
	public ResultMatcher comment(final String name, final Matcher<? super String> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' comment", cookie.getComment(), matcher);
		};
	}

	/**
	 * Assert a cookie's comment value.
	 */
	public ResultMatcher comment(final String name, final String comment) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' comment", comment, cookie.getComment());
		};
	}

	/**
	 * Assert a cookie's version with a Hamcrest {@link Matcher}
	 */
	public ResultMatcher version(final String name, final Matcher<? super Integer> matcher) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertThat("Response cookie '" + name + "' version", cookie.getVersion(), matcher);
		};
	}

	/**
	 * Assert a cookie's version value.
	 */
	public ResultMatcher version(final String name, final int version) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' version", version, cookie.getVersion());
		};
	}

	/**
	 * Assert whether the cookie must be sent over a secure protocol or not.
	 */
	public ResultMatcher secure(final String name, final boolean secure) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' secure", secure, cookie.getSecure());
		};
	}

	/**
	 * Assert whether the cookie must be HTTP only.
	 * @since 4.3.9
	 */
	public ResultMatcher httpOnly(final String name, final boolean httpOnly) {
		return result -> {
			Cookie cookie = getCookie(result, name);
			assertEquals("Response cookie '" + name + "' httpOnly", httpOnly, cookie.isHttpOnly());
		};
	}


	private static Cookie getCookie(MvcResult result, String name) {
		Cookie cookie = result.getResponse().getCookie(name);
		if (cookie == null) {
			AssertionErrors.fail("No cookie with name '" + name + "'");
		}
		return cookie;
	}

}
