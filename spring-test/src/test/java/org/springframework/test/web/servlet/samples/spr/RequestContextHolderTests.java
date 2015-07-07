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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
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
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Tests for SPR-10025 (access to request attributes via RequestContextHolder)
 * and SPR-13211 (re-use of mock request from the TestContext framework).
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration
@DirtiesContext
public class RequestContextHolderTests {

	private static final String FROM_TCF_MOCK = "fromTestContextFrameworkMock";
	private static final String FROM_MVC_TEST_DEFAULT = "fromSpringMvcTestDefault";
	private static final String FROM_MVC_TEST_MOCK = "fromSpringMvcTestMock";
	private static final String FROM_FILTER = "fromFilter";

	private static final String ENIGMA = "puzzle";

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private MockHttpServletRequest mockRequest;

	@Autowired
	private RequestScopedController requestScopedController;

	@Autowired
	private RequestScopedService requestScopedService;

	@Autowired
	private SessionScopedService sessionScopedService;

	private MockMvc mockMvc;


	@Before
	public void setup() {
		this.mockRequest.setAttribute(FROM_TCF_MOCK, ENIGMA);

		this.mockMvc = webAppContextSetup(this.wac)
				.addFilter(new AbcFilter())
				.defaultRequest(get("/").requestAttr(FROM_MVC_TEST_DEFAULT, ENIGMA))
				.alwaysExpect(status().isOk())
				.build();
	}

	@Test
	public void singletonController() throws Exception {
		this.mockMvc.perform(get("/singletonController").requestAttr(FROM_MVC_TEST_MOCK, ENIGMA));
	}

	@Test
	public void requestScopedController() throws Exception {
		assertTrue("request-scoped controller must be a CGLIB proxy", AopUtils.isCglibProxy(this.requestScopedController));
		this.mockMvc.perform(get("/requestScopedController").requestAttr(FROM_MVC_TEST_MOCK, ENIGMA));
	}

	@Test
	public void requestScopedService() throws Exception {
		assertTrue("request-scoped service must be a CGLIB proxy", AopUtils.isCglibProxy(this.requestScopedService));
		this.mockMvc.perform(get("/requestScopedService").requestAttr(FROM_MVC_TEST_MOCK, ENIGMA));
	}

	@Test
	public void sessionScopedService() throws Exception {
		assertTrue("session-scoped service must be a CGLIB proxy", AopUtils.isCglibProxy(this.sessionScopedService));
		this.mockMvc.perform(get("/sessionScopedService").requestAttr(FROM_MVC_TEST_MOCK, ENIGMA));
	}


	// -------------------------------------------------------------------

	@Configuration
	@EnableWebMvc
	static class WebConfig extends WebMvcConfigurerAdapter {

		@Bean
		public SingletonController singletonController() {
			return new SingletonController();
		}

		@Bean
		@Scope(name = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
		public RequestScopedController requestScopedController() {
			return new RequestScopedController();
		}

		@Bean
		@Scope(name = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
		public RequestScopedService requestScopedService() {
			return new RequestScopedService();
		}

		@Bean
		public ControllerWithRequestScopedService controllerWithRequestScopedService() {
			return new ControllerWithRequestScopedService();
		}

		@Bean
		@Scope(name = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
		public SessionScopedService sessionScopedService() {
			return new SessionScopedService();
		}

		@Bean
		public ControllerWithSessionScopedService controllerWithSessionScopedService() {
			return new ControllerWithSessionScopedService();
		}
	}

	@RestController
	private static class SingletonController {

		@RequestMapping("/singletonController")
		public void handle() {
			assertRequestAttributes();
		}
	}

	@RestController
	private static class RequestScopedController {

		@Autowired
		private HttpServletRequest request;


		@RequestMapping("/requestScopedController")
		public void handle() {
			assertRequestAttributes(request);
			assertRequestAttributes();
		}
	}

	private static class RequestScopedService {

		@Autowired
		private HttpServletRequest request;


		void process() {
			assertRequestAttributes(request);
		}
	}

	private static class SessionScopedService {

		@Autowired
		private HttpServletRequest request;


		void process() {
			assertRequestAttributes(this.request);
		}
	}

	@RestController
	private static class ControllerWithRequestScopedService {

		@Autowired
		private RequestScopedService service;


		@RequestMapping("/requestScopedService")
		public void handle() {
			this.service.process();
			assertRequestAttributes();
		}
	}

	@RestController
	private static class ControllerWithSessionScopedService {

		@Autowired
		private SessionScopedService service;


		@RequestMapping("/sessionScopedService")
		public void handle() {
			this.service.process();
			assertRequestAttributes();
		}
	}

	private static class AbcFilter extends GenericFilterBean {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
			request.setAttribute(FROM_FILTER, ENIGMA);
			chain.doFilter(request, response);
		}
	}


	private static void assertRequestAttributes() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		assertThat(requestAttributes, instanceOf(ServletRequestAttributes.class));
		assertRequestAttributes(((ServletRequestAttributes) requestAttributes).getRequest());
	}

	private static void assertRequestAttributes(HttpServletRequest request) {
		// TODO [SPR-13211] Assert that FOO is ENIGMA, instead of NULL.
		// assertThat(this.request.getAttribute(FOO), is(ENIGMA));
		assertThat(request.getAttribute(FROM_TCF_MOCK), is(nullValue()));
		assertThat(request.getAttribute(FROM_MVC_TEST_DEFAULT), is(ENIGMA));
		assertThat(request.getAttribute(FROM_MVC_TEST_MOCK), is(ENIGMA));
		assertThat(request.getAttribute(FROM_FILTER), is(ENIGMA));
	}

}
