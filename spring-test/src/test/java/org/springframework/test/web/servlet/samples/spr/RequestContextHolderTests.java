/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.servlet.samples.spr;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

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
@ExtendWith(SpringExtension.class)
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


	@BeforeEach
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
		assertThat(AopUtils.isCglibProxy(this.requestScopedController)).as("request-scoped controller must be a CGLIB proxy").isTrue();
		this.mockMvc.perform(get("/requestScopedController").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
	}

	@Test
	public void requestScopedService() throws Exception {
		assertThat(AopUtils.isCglibProxy(this.requestScopedService)).as("request-scoped service must be a CGLIB proxy").isTrue();
		this.mockMvc.perform(get("/requestScopedService").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
	}

	@Test
	public void sessionScopedService() throws Exception {
		assertThat(AopUtils.isCglibProxy(this.sessionScopedService)).as("session-scoped service must be a CGLIB proxy").isTrue();
		this.mockMvc.perform(get("/sessionScopedService").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
	}

	@AfterEach
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
		@RequestScope
		public RequestScopedService requestScopedService() {
			return new RequestScopedService();
		}

		@Bean
		public ControllerWithRequestScopedService controllerWithRequestScopedService() {
			return new ControllerWithRequestScopedService();
		}

		@Bean
		@SessionScope
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
	static class SingletonController {

		@RequestMapping("/singletonController")
		public void handle() {
			assertRequestAttributes();
		}
	}

	@RestController
	static class RequestScopedController {

		@Autowired
		private ServletRequest request;


		@RequestMapping("/requestScopedController")
		public void handle() {
			assertRequestAttributes(request);
			assertRequestAttributes();
		}
	}

	static class RequestScopedService {

		@Autowired
		private ServletRequest request;


		void process() {
			assertRequestAttributes(request);
		}
	}

	static class SessionScopedService {

		@Autowired
		private ServletRequest request;


		void process() {
			assertRequestAttributes(this.request);
		}
	}

	@RestController
	static class ControllerWithRequestScopedService {

		@Autowired
		private RequestScopedService service;


		@RequestMapping("/requestScopedService")
		public void handle() {
			this.service.process();
			assertRequestAttributes();
		}
	}

	@RestController
	static class ControllerWithSessionScopedService {

		@Autowired
		private SessionScopedService service;


		@RequestMapping("/sessionScopedService")
		public void handle() {
			this.service.process();
			assertRequestAttributes();
		}
	}

	static class FilterWithSessionScopedService extends GenericFilterBean {

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

	static class RequestFilter extends GenericFilterBean {

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
			request.setAttribute(FROM_REQUEST_FILTER, FROM_REQUEST_FILTER);
			chain.doFilter(request, response);
		}
	}

	static class RequestAttributesFilter extends GenericFilterBean {

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
		assertThat(requestAttributes).isInstanceOf(ServletRequestAttributes.class);
		assertRequestAttributes(((ServletRequestAttributes) requestAttributes).getRequest(), withinMockMvc);
	}

	private static void assertRequestAttributes(ServletRequest request) {
		assertRequestAttributes(request, true);
	}

	private static void assertRequestAttributes(ServletRequest request, boolean withinMockMvc) {
		if (withinMockMvc) {
			assertThat(request.getAttribute(FROM_TCF_MOCK)).isNull();
			assertThat(request.getAttribute(FROM_MVC_TEST_DEFAULT)).isEqualTo(FROM_MVC_TEST_DEFAULT);
			assertThat(request.getAttribute(FROM_MVC_TEST_MOCK)).isEqualTo(FROM_MVC_TEST_MOCK);
			assertThat(request.getAttribute(FROM_REQUEST_FILTER)).isEqualTo(FROM_REQUEST_FILTER);
			assertThat(request.getAttribute(FROM_REQUEST_ATTRIBUTES_FILTER)).isEqualTo(FROM_REQUEST_ATTRIBUTES_FILTER);
		}
		else {
			assertThat(request.getAttribute(FROM_TCF_MOCK)).isEqualTo(FROM_TCF_MOCK);
			assertThat(request.getAttribute(FROM_MVC_TEST_DEFAULT)).isNull();
			assertThat(request.getAttribute(FROM_MVC_TEST_MOCK)).isNull();
			assertThat(request.getAttribute(FROM_REQUEST_FILTER)).isNull();
			assertThat(request.getAttribute(FROM_REQUEST_ATTRIBUTES_FILTER)).isNull();
		}
	}

}
