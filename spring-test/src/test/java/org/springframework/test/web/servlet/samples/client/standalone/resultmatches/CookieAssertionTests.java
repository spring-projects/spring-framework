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

package org.springframework.test.web.servlet.samples.client.standalone.resultmatches;

import java.time.Duration;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resultmatchers.CookieAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
class CookieAssertionTests {

	private static final String COOKIE_NAME = CookieLocaleResolver.DEFAULT_COOKIE_NAME;

	private WebTestClient client;


	@BeforeEach
	void setup() {
		CookieLocaleResolver localeResolver = new CookieLocaleResolver();
		localeResolver.setCookieDomain("domain");
		localeResolver.setCookieHttpOnly(true);
		localeResolver.setCookieSameSite("Strict");

		client = MockMvcWebTestClient.bindToController(new SimpleController())
				.interceptors(new LocaleChangeInterceptor())
				.localeResolver(localeResolver)
				.alwaysExpect(status().isOk())
				.configureClient()
				.baseUrl("/?locale=en_US")
				.build();
	}


	@Test
	void exists() {
		client.get().uri("/").exchange().expectCookie().exists(COOKIE_NAME);
	}

	@Test
	void notExists() {
		client.get().uri("/").exchange().expectCookie().doesNotExist("unknownCookie");
	}

	@Test
	void hamcrestEqualTo() {
		client.get().uri("/").exchange().expectCookie().valueEquals(COOKIE_NAME, "en-US");
		client.get().uri("/").exchange().expectCookie()
				.value(COOKIE_NAME, v -> MatcherAssert.assertThat(v, equalTo("en-US")));
	}

	@Test
	void matcher() {
		client.get().uri("/").exchange().expectCookie()
				.value(COOKIE_NAME, v -> MatcherAssert.assertThat(v, startsWith("en-US")));
	}

	@Test
	void maxAge() {
		client.get().uri("/").exchange().expectCookie().maxAge(COOKIE_NAME, Duration.ofSeconds(-1));
	}

	@Test
	void domain() {
		client.get().uri("/").exchange().expectCookie().domain(COOKIE_NAME, "domain");
	}

	@Test
	void path() {
		client.get().uri("/").exchange().expectCookie().path(COOKIE_NAME, "/");
	}

	@Test
	void secured() {
		client.get().uri("/").exchange().expectCookie().secure(COOKIE_NAME, false);
	}

	@Test
	void httpOnly() {
		client.get().uri("/").exchange().expectCookie().httpOnly(COOKIE_NAME, true);
	}

	@Test
	void sameSite() {
		client.get().uri("/").exchange().expectCookie().sameSite(COOKIE_NAME, "Strict");
	}

	@Controller
	private static class SimpleController {

		@RequestMapping("/")
		public String home() {
			return "home";
		}
	}

}
