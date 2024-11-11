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

package org.springframework.test.web.servlet.samples.standalone.resultmatchers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Examples of expectations on response cookies values.
 *
 * @author Rossen Stoyanchev
 * @author Nikola Yovchev
 */
public class CookieAssertionTests {

	private static final String COOKIE_NAME = CookieLocaleResolver.DEFAULT_COOKIE_NAME;
	private static final String COOKIE_WITH_ATTRIBUTES_NAME = "SecondCookie";
	private static final String SECOND_COOKIE_ATTRIBUTE = "COOKIE_ATTRIBUTE";

	private MockMvc mockMvc;


	@BeforeEach
	public void setup() {
		CookieLocaleResolver localeResolver = new CookieLocaleResolver();
		localeResolver.setCookieDomain("domain");
		localeResolver.setCookieHttpOnly(true);
		localeResolver.setCookieSameSite("foo");

		Cookie cookie = new Cookie(COOKIE_WITH_ATTRIBUTES_NAME, "value");
		cookie.setAttribute("sameSite", "Strict"); //intentionally camelCase
		cookie.setAttribute(SECOND_COOKIE_ATTRIBUTE, "there");

		this.mockMvc = standaloneSetup(new SimpleController())
				.addInterceptors(new LocaleChangeInterceptor())
				.addInterceptors(new HandlerInterceptor() {
					@Override
					public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
						response.addCookie(cookie);
						return true;
					}
				})
				.setLocaleResolver(localeResolver)
				.defaultRequest(get("/").param("locale", "en_US"))
				.alwaysExpect(status().isOk())
				.build();
	}


	@Test
	public void testExists() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie().exists(COOKIE_NAME));
	}

	@Test
	public void testNotExists() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie().doesNotExist("unknownCookie"));
	}

	@Test
	public void testEqualTo() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie().value(COOKIE_NAME, "en-US"));
		this.mockMvc.perform(get("/")).andExpect(cookie().value(COOKIE_NAME, equalTo("en-US")));
	}

	@Test
	public void testMatcher() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie().value(COOKIE_NAME, startsWith("en")));
	}

	@Test
	public void testMaxAge() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie().maxAge(COOKIE_NAME, -1));
	}

	@Test
	public void testDomain() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie().domain(COOKIE_NAME, "domain"));
	}

	@Test
	void testSameSite() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie()
				.sameSite(COOKIE_NAME, "foo"));
	}

	@Test
	void testSameSiteMatcher() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie()
				.sameSite(COOKIE_WITH_ATTRIBUTES_NAME, startsWith("Str")));
	}

	@Test
	void testSameSiteNotEquals() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
						this.mockMvc.perform(get("/")).andExpect(cookie()
								.sameSite(COOKIE_WITH_ATTRIBUTES_NAME, "Str")))
				.withMessage("Response cookie 'SecondCookie' attribute 'SameSite' expected:<Str> but was:<Strict>");
	}

	@Test
	public void testVersion() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie().version(COOKIE_NAME, 0));
	}

	@Test
	public void testPath() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie().path(COOKIE_NAME, "/"));
	}

	@Test
	public void testSecured() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie().secure(COOKIE_NAME, false));
	}

	@Test
	public void testHttpOnly() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie().httpOnly(COOKIE_NAME, true));
	}

	@Test
	void testAttribute() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie()
				.attribute(COOKIE_WITH_ATTRIBUTES_NAME, SECOND_COOKIE_ATTRIBUTE, "there"));
	}

	@Test
	void testAttributeMatcher() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie()
				.attribute(COOKIE_WITH_ATTRIBUTES_NAME, SECOND_COOKIE_ATTRIBUTE, is("there")));
	}

	@Test
	void testAttributeNotPresent() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> this.mockMvc.perform(get("/"))
						.andExpect(cookie().attribute(COOKIE_WITH_ATTRIBUTES_NAME, "randomAttribute", anything())))
				.withMessage("Response cookie 'SecondCookie' doesn't have attribute 'randomAttribute'");
	}

	@Test
	void testAttributeNotEquals() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> this.mockMvc.perform(get("/"))
						.andExpect(cookie().attribute(COOKIE_WITH_ATTRIBUTES_NAME, SECOND_COOKIE_ATTRIBUTE, "foo")))
				.withMessage("Response cookie 'SecondCookie' attribute 'COOKIE_ATTRIBUTE' expected:<foo> but was:<there>");
	}


	@Controller
	private static class SimpleController {

		@RequestMapping("/")
		public String home() {
			return "home";
		}
	}

}
