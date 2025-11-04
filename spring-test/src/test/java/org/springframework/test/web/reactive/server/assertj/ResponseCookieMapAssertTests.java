/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.reactive.server.assertj;

import java.time.Duration;
import java.util.List;

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.assertj.CookieMapAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link CookieMapAssert}.
 *
 * @author Rossen Stoyanchev
 */
class ResponseCookieMapAssertTests {

	static ResponseCookie[] cookies;

	@BeforeAll
	static void setup() {
		ResponseCookie framework = ResponseCookie.from("framework", "spring").secure(true).httpOnly(true).build();
		ResponseCookie age = ResponseCookie.from("age", "value").maxAge(1200).build();
		ResponseCookie domain = ResponseCookie.from("domain", "value").domain("spring.io").build();
		ResponseCookie path = ResponseCookie.from("path", "value").path("/spring").build();
		cookies = List.of(framework, age, domain, path).toArray(new ResponseCookie[0]);
	}

	@Test
	void containsCookieWhenCookieExistsShouldPass() {
		assertThat(cookies()).containsCookie("framework");
	}

	@Test
	void containsCookieWhenCookieMissingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(cookies()).containsCookie("missing"));
	}

	@Test
	void containsCookiesWhenCookiesExistShouldPass() {
		assertThat(cookies()).containsCookies("framework", "age");
	}

	@Test
	void containsCookiesWhenCookieMissingShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(cookies()).containsCookies("framework", "missing"));
	}

	@Test
	void doesNotContainCookieWhenCookieMissingShouldPass() {
		assertThat(cookies()).doesNotContainCookie("missing");
	}

	@Test
	void doesNotContainCookieWhenCookieExistsShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(cookies()).doesNotContainCookie("framework"));
	}

	@Test
	void doesNotContainCookiesWhenCookiesMissingShouldPass() {
		assertThat(cookies()).doesNotContainCookies("missing", "missing2");
	}

	@Test
	void doesNotContainCookiesWhenAtLeastOneCookieExistShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(cookies()).doesNotContainCookies("missing", "framework"));
	}

	@Test
	void hasValueEqualsWhenCookieValueMatchesShouldPass() {
		assertThat(cookies()).hasValue("framework", "spring");
	}

	@Test
	void hasValueEqualsWhenCookieValueDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(cookies()).hasValue("framework", "other"));
	}

	@Test
	void hasCookieSatisfyingWhenCookieValueMatchesShouldPass() {
		assertThat(cookies()).hasCookieSatisfying("framework", cookie -> assertThat(cookie.getValue()).startsWith("spr"));
	}

	@Test
	void hasCookieSatisfyingWhenCookieValueDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(cookies()).hasCookieSatisfying(
						"framework", cookie -> assertThat(cookie.getValue()).startsWith("not")));
	}

	@Test
	void hasMaxAgeWhenCookieAgeMatchesShouldPass() {
		assertThat(cookies()).hasMaxAge("age", Duration.ofMinutes(20));
	}

	@Test
	void hasMaxAgeWhenCookieAgeDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(cookies()).hasMaxAge("age", Duration.ofMinutes(30)));
	}

	@Test
	void pathWhenCookiePathMatchesShouldPass() {
		assertThat(cookies()).hasPath("path", "/spring");
	}

	@Test
	void pathWhenCookiePathDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(cookies()).hasPath("path", "/other"));
	}

	@Test
	void hasDomainWhenCookieDomainMatchesShouldPass() {
		assertThat(cookies()).hasDomain("domain", "spring.io");
	}

	@Test
	void hasDomainWhenCookieDomainDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(cookies()).hasDomain("domain", "example.org"));
	}

	@Test
	void isSecureWhenCookieSecureMatchesShouldPass() {
		assertThat(cookies()).isSecure("framework", true);
	}

	@Test
	void isSecureWhenCookieSecureDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(cookies()).isSecure("domain", true));
	}

	@Test
	void isHttpOnlyWhenCookieHttpOnlyMatchesShouldPass() {
		assertThat(cookies()).isHttpOnly("framework", true);
	}

	@Test
	void isHttpOnlyWhenCookieHttpOnlyDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(cookies()).isHttpOnly("domain", true));
	}

	private static AssertProvider<ResponseCookieMapAssert> cookies() {
		return () -> new ResponseCookieMapAssert(cookies);
	}

}
