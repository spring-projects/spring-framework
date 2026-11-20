/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server.assertj;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.Assertions;

import org.springframework.http.ResponseCookie;

/**
 * AssertJ {@linkplain org.assertj.core.api.Assert assertions} that can be applied
 * to {@link ResponseCookie cookies}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class ResponseCookieMapAssert
		extends AbstractMapAssert<ResponseCookieMapAssert, Map<String, ResponseCookie>, String, ResponseCookie> {


	public ResponseCookieMapAssert(ResponseCookie[] actual) {
		super(toMap(actual), ResponseCookieMapAssert.class);
		as("Cookies");
	}

	private static Map<String, ResponseCookie> toMap(ResponseCookie[] cookies) {
		Map<String, ResponseCookie> map = new LinkedHashMap<>();
		for (ResponseCookie cookie : cookies) {
			map.putIfAbsent(cookie.getName(), cookie);
		}
		return map;
	}


	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}.
	 * @param name the name of an expected cookie
	 * @see #containsKey
	 */
	public ResponseCookieMapAssert containsCookie(String name) {
		return containsKey(name);
	}

	/**
	 * Verify that the actual cookies contain cookies with the given {@code names}.
	 * @param names the names of expected cookies
	 * @see #containsKeys
	 */
	public ResponseCookieMapAssert containsCookies(String... names) {
		return containsKeys(names);
	}

	/**
	 * Verify that the actual cookies do not contain a cookie with the given
	 * {@code name}.
	 * @param name the name of a cookie that should not be present
	 * @see #doesNotContainKey
	 */
	public ResponseCookieMapAssert doesNotContainCookie(String name) {
		return doesNotContainKey(name);
	}

	/**
	 * Verify that the actual cookies do not contain any cookies with the given
	 * {@code names}.
	 * @param names the names of cookies that should not be present
	 * @see #doesNotContainKeys
	 */
	public ResponseCookieMapAssert doesNotContainCookies(String... names) {
		return doesNotContainKeys(names);
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * that satisfies the given {@code cookieRequirements}.
	 * @param name the name of an expected cookie
	 * @param cookieRequirements the requirements for the cookie
	 */
	public ResponseCookieMapAssert hasCookieSatisfying(String name, Consumer<ResponseCookie> cookieRequirements) {
		return hasEntrySatisfying(name, cookieRequirements);
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * whose {@linkplain ResponseCookie#getValue() value} is equal to the given one.
	 * @param name the name of the cookie
	 * @param expected the expected value of the cookie
	 */
	public ResponseCookieMapAssert hasValue(String name, String expected) {
		return hasCookieSatisfying(name, cookie -> Assertions.assertThat(cookie.getValue()).isEqualTo(expected));
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * whose {@linkplain ResponseCookie#getMaxAge() max age} is equal to the given one.
	 * @param name the name of the cookie
	 * @param expected the expected max age of the cookie
	 */
	public ResponseCookieMapAssert hasMaxAge(String name, Duration expected) {
		return hasCookieSatisfying(name, cookie -> Assertions.assertThat(cookie.getMaxAge()).isEqualTo(expected));
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * whose {@linkplain ResponseCookie#getPath() path} is equal to the given one.
	 * @param name the name of the cookie
	 * @param expected the expected path of the cookie
	 */
	public ResponseCookieMapAssert hasPath(String name, String expected) {
		return hasCookieSatisfying(name, cookie -> Assertions.assertThat(cookie.getPath()).isEqualTo(expected));
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * whose {@linkplain ResponseCookie#getDomain() domain} is equal to the given one.
	 * @param name the name of the cookie
	 * @param expected the expected domain of the cookie
	 */
	public ResponseCookieMapAssert hasDomain(String name, String expected) {
		return hasCookieSatisfying(name, cookie -> Assertions.assertThat(cookie.getDomain()).isEqualTo(expected));
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * whose {@linkplain ResponseCookie#isSecure() secure flag} is equal to the give one.
	 * @param name the name of the cookie
	 * @param expected whether the cookie is secure
	 */
	public ResponseCookieMapAssert isSecure(String name, boolean expected) {
		return hasCookieSatisfying(name, cookie -> Assertions.assertThat(cookie.isSecure()).isEqualTo(expected));
	}

	/**
	 * Verify that the actual cookies contain a cookie with the given {@code name}
	 * whose {@linkplain ResponseCookie#isHttpOnly() http only flag} is equal to the given
	 * one.
	 * @param name the name of the cookie
	 * @param expected whether the cookie is http only
	 */
	public ResponseCookieMapAssert isHttpOnly(String name, boolean expected) {
		return hasCookieSatisfying(name, cookie -> Assertions.assertThat(cookie.isHttpOnly()).isEqualTo(expected));
	}

}
