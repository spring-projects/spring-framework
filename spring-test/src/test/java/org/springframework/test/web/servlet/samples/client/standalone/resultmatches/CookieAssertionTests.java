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

package org.springframework.test.web.servlet.samples.client.standalone.resultmatches;

import java.time.Duration;

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
public class CookieAssertionTests {

	private static final String COOKIE_NAME = CookieLocaleResolver.DEFAULT_COOKIE_NAME;

	private WebTestClient client;


	@BeforeEach
	public void setup() {
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
	public void testExists() {
		client.get().uri("/").exchange().expectCookie().exists(COOKIE_NAME);
	}

	@Test
	public void testNotExists() {
		client.get().uri("/").exchange().expectCookie().doesNotExist("unknownCookie");
	}

	@Test
	public void testEqualTo() {
		client.get().uri("/").exchange().expectCookie().valueEquals(COOKIE_NAME, "en-US");
		client.get().uri("/").exchange().expectCookie().value(COOKIE_NAME, equalTo("en-US"));
	}

	@Test
	public void testMatcher() {
		client.get().uri("/").exchange().expectCookie().value(COOKIE_NAME, startsWith("en-US"));
	}

	@Test
	public void testMaxAge() {
		client.get().uri("/").exchange().expectCookie().maxAge(COOKIE_NAME, Duration.ofSeconds(-1));
	}

	@Test
	public void testDomain() {
		client.get().uri("/").exchange().expectCookie().domain(COOKIE_NAME, "domain");
	}

	@Test
	public void testPath() {
		client.get().uri("/").exchange().expectCookie().path(COOKIE_NAME, "/");
	}

	@Test
	public void testSecured() {
		client.get().uri("/").exchange().expectCookie().secure(COOKIE_NAME, false);
	}

	@Test
	public void testHttpOnly() {
		client.get().uri("/").exchange().expectCookie().httpOnly(COOKIE_NAME, true);
	}

	@Test
	public void testSameSite() {
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
