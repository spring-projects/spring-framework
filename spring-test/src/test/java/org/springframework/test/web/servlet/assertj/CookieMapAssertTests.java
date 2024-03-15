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
import java.util.List;

import jakarta.servlet.http.Cookie;
import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link CookieMapAssert}.
 *
 * @author Brian Clozel
 */
class CookieMapAssertTests {

	static Cookie[] cookies;

	@BeforeAll
	static void setup() {
		Cookie framework = new Cookie("framework", "spring");
		framework.setSecure(true);
		framework.setHttpOnly(true);
		Cookie age = new Cookie("age", "value");
		age.setMaxAge(1200);
		Cookie domain = new Cookie("domain", "value");
		domain.setDomain("spring.io");
		Cookie path = new Cookie("path", "value");
		path.setPath("/spring");
		cookies = List.of(framework, age, domain, path).toArray(new Cookie[0]);
	}

	@Test
	void containsCookieWhenCookieExistsShouldPass() {
		assertThat(forCookies()).containsCookie("framework");
	}

	@Test
	void containsCookieWhenCookieMissingShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertThat(forCookies()).containsCookie("missing"));
	}

	@Test
	void containsCookiesWhenCookiesExistShouldPass() {
		assertThat(forCookies()).containsCookies("framework", "age");
	}

	@Test
	void containsCookiesWhenCookieMissingShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertThat(forCookies()).containsCookies("framework", "missing"));
	}

	@Test
	void doesNotContainCookieWhenCookieMissingShouldPass() {
		assertThat(forCookies()).doesNotContainCookie("missing");
	}

	@Test
	void doesNotContainCookieWhenCookieExistsShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertThat(forCookies()).doesNotContainCookie("framework"));
	}

	@Test
	void doesNotContainCookiesWhenCookiesMissingShouldPass() {
		assertThat(forCookies()).doesNotContainCookies("missing", "missing2");
	}

	@Test
	void doesNotContainCookiesWhenAtLeastOneCookieExistShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertThat(forCookies()).doesNotContainCookies("missing", "framework"));
	}

	@Test
	void hasValueEqualsWhenCookieValueMatchesShouldPass() {
		assertThat(forCookies()).hasValue("framework", "spring");
	}

	@Test
	void hasValueEqualsWhenCookieValueDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertThat(forCookies()).hasValue("framework", "other"));
	}

	@Test
	void hasCookieSatisfyingWhenCookieValueMatchesShouldPass() {
		assertThat(forCookies()).hasCookieSatisfying("framework", cookie ->
				assertThat(cookie.getValue()).startsWith("spr"));
	}

	@Test
	void hasCookieSatisfyingWhenCookieValueDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertThat(forCookies()).hasCookieSatisfying("framework", cookie ->
						assertThat(cookie.getValue()).startsWith("not")));
	}

	@Test
	void hasMaxAgeWhenCookieAgeMatchesShouldPass() {
		assertThat(forCookies()).hasMaxAge("age", Duration.ofMinutes(20));
	}

	@Test
	void hasMaxAgeWhenCookieAgeDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertThat(forCookies()).hasMaxAge("age", Duration.ofMinutes(30)));
	}

	@Test
	void pathWhenCookiePathMatchesShouldPass() {
		assertThat(forCookies()).hasPath("path", "/spring");
	}

	@Test
	void pathWhenCookiePathDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertThat(forCookies()).hasPath("path", "/other"));
	}

	@Test
	void hasDomainWhenCookieDomainMatchesShouldPass() {
		assertThat(forCookies()).hasDomain("domain", "spring.io");
	}

	@Test
	void hasDomainWhenCookieDomainDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertThat(forCookies()).hasDomain("domain", "example.org"));
	}

	@Test
	void isSecureWhenCookieSecureMatchesShouldPass() {
		assertThat(forCookies()).isSecure("framework", true);
	}

	@Test
	void isSecureWhenCookieSecureDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertThat(forCookies()).isSecure("domain", true));
	}

	@Test
	void isHttpOnlyWhenCookieHttpOnlyMatchesShouldPass() {
		assertThat(forCookies()).isHttpOnly("framework", true);
	}

	@Test
	void isHttpOnlyWhenCookieHttpOnlyDiffersShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertThat(forCookies()).isHttpOnly("domain", true));
	}


	private AssertProvider<CookieMapAssert> forCookies() {
		return () -> new CookieMapAssert(cookies);
	}

}
