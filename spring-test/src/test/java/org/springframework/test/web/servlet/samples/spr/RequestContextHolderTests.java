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

import org.junit.After;
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
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Integration tests for the following use cases.
 * <ul>
 * <li>SPR-10025: Access to request attributes via RequestContextHolder</li>
 * <li>SPR-13217: Populate RequestAttributes before invoking Filters in MockMvc</li>
 * <li>SPR-13260: No reuse of mock requests</li>
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see CustomRequestAttributesRequestContextHolderTests
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration
@DirtiesContext
public class RequestContextHolderTests {

	private static final String FROM_TCF_MOCK = "fromTestContextFrameworkMock";
	private static final String FROM_MVC_TEST_DEFAULT = "fromSpringMvcTestDefault";
	private static final String FROM_MVC_TEST_MOCK = "fromSpringMvcTestMock";
	private static final String FROM_REQUEST_FILTER = "fromRequestFilter";
	private static final String FROM_REQUEST_ATTRIBUTES_FILTER = "fromRequestAttributesFilter";

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

	@Autowired
	private FilterWithSessionScopedService filterWithSessionScopedService;

	private MockMvc mockMvc;


	@Before
	public void setup() {
		this.mockRequest.setAttribute(FROM_TCF_MOCK, FROM_TCF_MOCK);

		this.mockMvc = webAppContextSetup(this.wac)
				.addFilters(new RequestFilter(), new RequestAttributesFilter(), this.filterWithSessionScopedService)
				.defaultRequest(get("/").requestAttr(FROM_MVC_TEST_DEFAULT, FROM_MVC_TEST_DEFAULT))
				.alwaysExpect(status().isOk())
				.build();
	}

	@Test
	public void singletonController() throws Exception {
		this.mockMvc.perform(get("/singletonController").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
	}

	@Test
	public void requestScopedController() throws Exception {
		assertTrue("request-scoped controller must be a CGLIB proxy", AopUtils.isCglibProxy(this.requestScopedController));
		this.mockMvc.perform(get("/requestScopedController").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
	}

	@Test
	public void requestScopedService() throws Exception {
		assertTrue("request-scoped service must be a CGLIB proxy", AopUtils.isCglibProxy(this.requestScopedService));
		this.mockMvc.perform(get("/requestScopedService").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
	}

	@Test
	public void sessionScopedService() throws Exception {
		assertTrue("session-scoped service must be a CGLIB proxy", AopUtils.isCglibProxy(this.sessionScopedService));
		this.mockMvc.perform(get("/sessionScopedService").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
	}

	@After
	public void verifyRestoredRequestAttributes() {
		assertRequestAttributes(false);
	}


	// -------------------------------------------------------------------

	@Configuration
	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Bean
		public SingletonController singletonController() {
			return new SingletonController();
		}

		@Bean
		@Scope(scopeName = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
		public RequestScopedController requestScopedController() {
			return new RequestScopedController();
		}

		@Bean
		@Scope(scopeName = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
		public RequestScopedService requestScopedService() {
			return new RequestScopedService();
		}

		@Bean
		public ControllerWithRequestScopedService controllerWithRequestScopedService() {
			return new ControllerWithRequestScopedService();
		}

		@Bean
		@Scope(scopeName = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
		public SessionScopedService sessionScopedService() {
			return new SessionScopedService();
		}

		@Bean
		public ControllerWithSessionScopedService controllerWithSessionScopedService() {
			return new ControllerWithSessionScopedService();
		}

		@Bean
		public FilterWithSessionScopedService filterWithSessionScopedService() {
			return new FilterWithSessionScopedService();
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
		private ServletRequest request;


		@RequestMapping("/requestScopedController")
		public void handle() {
			assertRequestAttributes(request);
			assertRequestAttributes();
		}
	}

	private static class RequestScopedService {

		@Autowired
		private ServletRequest request;


		void process() {
			assertRequestAttributes(request);
		}
	}

	private static class SessionScopedService {

		@Autowired
		private ServletRequest request;


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

	private static class FilterWithSessionScopedService extends GenericFilterBean {

		@Autowired
		private SessionScopedService service;


		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
			this.service.process();
			assertRequestAttributes(request);
			assertRequestAttributes();
			chain.doFilter(request, response);
		}
	}

	private static class RequestFilter extends GenericFilterBean {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
			request.setAttribute(FROM_REQUEST_FILTER, FROM_REQUEST_FILTER);
			chain.doFilter(request, response);
		}
	}

	private static class RequestAttributesFilter extends GenericFilterBean {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
			RequestContextHolder.getRequestAttributes().setAttribute(FROM_REQUEST_ATTRIBUTES_FILTER, FROM_REQUEST_ATTRIBUTES_FILTER, RequestAttributes.SCOPE_REQUEST);
			chain.doFilter(request, response);
		}
	}


	private static void assertRequestAttributes() {
		assertRequestAttributes(true);
	}

	private static void assertRequestAttributes(boolean withinMockMvc) {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		assertThat(requestAttributes, instanceOf(ServletRequestAttributes.class));
		assertRequestAttributes(((ServletRequestAttributes) requestAttributes).getRequest(), withinMockMvc);
	}

	private static void assertRequestAttributes(ServletRequest request) {
		assertRequestAttributes(request, true);
	}

	private static void assertRequestAttributes(ServletRequest request, boolean withinMockMvc) {
		if (withinMockMvc) {
			assertThat(request.getAttribute(FROM_TCF_MOCK), is(nullValue()));
			assertThat(request.getAttribute(FROM_MVC_TEST_DEFAULT), is(FROM_MVC_TEST_DEFAULT));
			assertThat(request.getAttribute(FROM_MVC_TEST_MOCK), is(FROM_MVC_TEST_MOCK));
			assertThat(request.getAttribute(FROM_REQUEST_FILTER), is(FROM_REQUEST_FILTER));
			assertThat(request.getAttribute(FROM_REQUEST_ATTRIBUTES_FILTER), is(FROM_REQUEST_ATTRIBUTES_FILTER));
		}
		else {
			assertThat(request.getAttribute(FROM_TCF_MOCK), is(FROM_TCF_MOCK));
			assertThat(request.getAttribute(FROM_MVC_TEST_DEFAULT), is(nullValue()));
			assertThat(request.getAttribute(FROM_MVC_TEST_MOCK), is(nullValue()));
			assertThat(request.getAttribute(FROM_REQUEST_FILTER), is(nullValue()));
			assertThat(request.getAttribute(FROM_REQUEST_ATTRIBUTES_FILTER), is(nullValue()));
		}
	}

}
