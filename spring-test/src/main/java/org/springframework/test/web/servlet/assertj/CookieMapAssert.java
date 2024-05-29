/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.servlet.assertj;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.servlet.http.Cookie;
import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.Assertions;

/**
 * AssertJ {@linkplain org.assertj.core.api.Assert assertions} that can be applied
 * to {@link Cookie cookies}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 6.2
 */
public class CookieMapAssert extends AbstractMapAssert<CookieMapAssert, Map<String, Cookie>, String, Cookie> {

	public CookieMapAssert(Cookie[] actual) {
		super(mapCookies(actual), CookieMapAssert.class);
		as("Cookies");
	}

	private static Map<String, Cookie> mapCookies(Cookie[] cookies) {
		Map<String, Cookie> map = new LinkedHashMap<>();
		for (Cookie cookie : cookies) {
			map.putIfAbsent(cookie.getName(), cookie);
		}
		return map;
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}.
	 * @param name the name of an expected cookie
	 * @see #containsKey
	 */
	public CookieMapAssert containsCookie(String name) {
		return containsKey(name);
	}

	/**
	 * Verify that the actual cookies contain cookies with the given {@code names}.
	 * @param names the names of expected cookies
	 * @see #containsKeys
	 */
	public CookieMapAssert containsCookies(String... names) {
		return containsKeys(names);
	}

	/**
	 * Verify that the actual cookies do not contain a cookie with the given
	 * {@code name}.
	 * @param name the name of a cookie that should not be present
	 * @see #doesNotContainKey
	 */
	public CookieMapAssert doesNotContainCookie(String name) {
		return doesNotContainKey(name);
	}

	/**
	 * Verify that the actual cookies do not contain any cookies with the given
	 * {@code names}.
	 * @param names the names of cookies that should not be present
	 * @see #doesNotContainKeys
	 */
	public CookieMapAssert doesNotContainCookies(String... names) {
		return doesNotContainKeys(names);
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * that satisfies the given {@code cookieRequirements}.
	 * @param name the name of an expected cookie
	 * @param cookieRequirements the requirements for the cookie
	 */
	public CookieMapAssert hasCookieSatisfying(String name, Consumer<Cookie> cookieRequirements) {
		return hasEntrySatisfying(name, cookieRequirements);
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * whose {@linkplain Cookie#getValue() value} is equal to the given one.
	 * @param name the name of the cookie
	 * @param expected the expected value of the cookie
	 */
	public CookieMapAssert hasValue(String name, String expected) {
		return hasCookieSatisfying(name, cookie ->
				Assertions.assertThat(cookie.getValue()).isEqualTo(expected));
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * whose {@linkplain Cookie#getMaxAge() max age} is equal to the given one.
	 * @param name the name of the cookie
	 * @param expected the expected max age of the cookie
	 */
	public CookieMapAssert hasMaxAge(String name, Duration expected) {
		return hasCookieSatisfying(name, cookie ->
				Assertions.assertThat(Duration.ofSeconds(cookie.getMaxAge())).isEqualTo(expected));
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * whose {@linkplain Cookie#getPath() path} is equal to the given one.
	 * @param name the name of the cookie
	 * @param expected the expected path of the cookie
	 */
	public CookieMapAssert hasPath(String name, String expected) {
		return hasCookieSatisfying(name, cookie ->
				Assertions.assertThat(cookie.getPath()).isEqualTo(expected));
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * whose {@linkplain Cookie#getDomain() domain} is equal to the given one.
	 * @param name the name of the cookie
	 * @param expected the expected domain of the cookie
	 */
	public CookieMapAssert hasDomain(String name, String expected) {
		return hasCookieSatisfying(name, cookie ->
				Assertions.assertThat(cookie.getDomain()).isEqualTo(expected));
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * whose {@linkplain Cookie#getSecure() secure flag} is equal to the give one.
	 * @param name the name of the cookie
	 * @param expected whether the cookie is secure
	 */
	public CookieMapAssert isSecure(String name, boolean expected) {
		return hasCookieSatisfying(name, cookie ->
				Assertions.assertThat(cookie.getSecure()).isEqualTo(expected));
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * whose {@linkplain Cookie#isHttpOnly() http only flag} is equal to the given
	 * one.
	 * @param name the name of the cookie
	 * @param expected whether the cookie is http only
	 */
	public CookieMapAssert isHttpOnly(String name, boolean expected) {
		return hasCookieSatisfying(name, cookie ->
				Assertions.assertThat(cookie.isHttpOnly()).isEqualTo(expected));
	}

}
