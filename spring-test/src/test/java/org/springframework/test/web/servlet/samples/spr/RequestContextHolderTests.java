/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.web.servlet.samples.spr;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;
import static org.springframework.web.context.request.RequestAttributes.*;

/**
 * Test for SPR-10025 (access to request attributes via RequestContextHolder).
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration
@DirtiesContext
public class RequestContextHolderTests {

	private static final String FOO = "foo";
	private static final String BAR = "bar";
	private static final String BAZ = "baz";
	private static final String QUUX = "quux";
	private static final String ENIGMA = "enigma";
	private static final String PUZZLE = "puzzle";

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private MockHttpServletRequest mockRequest;

	@Autowired
	private MyScopedController myScopedController;

	private MockMvc mockMvc;


	@Before
	public void setup() {
		this.mockRequest.setAttribute(FOO, BAR);

		this.mockMvc = webAppContextSetup(this.wac)
				.defaultRequest(get("/").requestAttr(ENIGMA, PUZZLE))
				.alwaysExpect(status().isOk())
				.build();
	}

	@Test
	public void singletonController() throws Exception {
		this.mockMvc.perform(get("/singleton").requestAttr(BAZ, QUUX));
	}

	@Test
	public void requestScopedController() throws Exception {
		assertTrue("request-scoped controller must be a CGLIB proxy", AopUtils.isCglibProxy(this.myScopedController));
		this.mockMvc.perform(get("/requestScoped").requestAttr(BAZ, QUUX));
	}


	@Configuration
	@EnableWebMvc
	static class WebConfig extends WebMvcConfigurerAdapter {

		@Bean
		public MyController myController() {
			return new MyController();
		}

		@Bean
		@Scope(name = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
		public MyScopedController myScopedController() {
			return new MyScopedController();
		}
	}


	private static void assertRequestAttributes() {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		// TODO [SPR-13211] Assert that FOO is BAR, instead of NULL.
		// assertThat(attributes.getAttribute(FOO, SCOPE_REQUEST), is(BAR));
		assertThat(attributes.getAttribute(FOO, SCOPE_REQUEST), is(nullValue()));
		assertThat(attributes.getAttribute(ENIGMA, SCOPE_REQUEST), is(PUZZLE));
		assertThat(attributes.getAttribute(BAZ, SCOPE_REQUEST), is(QUUX));
	}


	@RestController
	private static class MyController {

		@RequestMapping("/singleton")
		public void handle() {
			assertRequestAttributes();
		}
	}

	@RestController
	private static class MyScopedController {

		@Autowired
		private HttpServletRequest request;


		@RequestMapping("/requestScoped")
		public void handle() {
			// TODO [SPR-13211] Assert that FOO is BAR, instead of NULL.
			// assertThat(this.request.getAttribute(FOO), is(BAR));
			assertThat(this.request.getAttribute(FOO), is(nullValue()));
			assertRequestAttributes();
		}
	}

}
