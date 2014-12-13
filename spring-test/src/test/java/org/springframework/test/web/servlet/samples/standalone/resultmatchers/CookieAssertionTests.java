/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.servlet.samples.standalone.resultmatchers;

import org.junit.Before;
import org.junit.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Examples of expectations on response cookies values.
 *
 * @author Rossen Stoyanchev
 */
public class CookieAssertionTests {

	private static final String COOKIE_NAME = CookieLocaleResolver.DEFAULT_COOKIE_NAME;

	private MockMvc mockMvc;


	@Before
	public void setup() {
		CookieLocaleResolver localeResolver = new CookieLocaleResolver();
		localeResolver.setCookieDomain("domain");

		this.mockMvc = standaloneSetup(new SimpleController())
				.addInterceptors(new LocaleChangeInterceptor())
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
		this.mockMvc.perform(get("/")).andExpect(cookie().doesNotExist("unknowCookie"));
	}

	@Test
	public void testEqualTo() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(cookie().value(COOKIE_NAME, "en_US"));
		this.mockMvc.perform(get("/")).andExpect(cookie().value(COOKIE_NAME, equalTo("en_US")));
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


	@Controller
	private static class SimpleController {

		@RequestMapping("/")
		public String home() {
			return "home";
		}
	}
}
