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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

/**
 * Test fixture for {@link CrossOrigin @CrossOrigin} annotated methods.
 *
 * @author Sebastien Deleuze
 */
@SuppressWarnings("unchecked")
public class CrossOriginTests {

	private TestRequestMappingInfoHandlerMapping handlerMapping;
	private MockHttpServletRequest request;

	@Before
	public void setUp() {
		this.handlerMapping = new TestRequestMappingInfoHandlerMapping();
		this.handlerMapping.setRemoveSemicolonContent(false);
		this.handlerMapping.setApplicationContext(new StaticWebApplicationContext());
		this.handlerMapping.afterPropertiesSet();
		this.request = new MockHttpServletRequest();
		this.request.setMethod("GET");
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain.com/");
	}

	@Test
	public void noAnnotationWithoutOrigin() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/no");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNull(config);
	}

	@Test  // SPR-12931
	public void noAnnotationWithOrigin() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setRequestURI("/no");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNull(config);
	}

	@Test  // SPR-12931
	public void noAnnotationPostWithOrigin() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setMethod("POST");
		this.request.setRequestURI("/no");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNull(config);
	}

	@Test
	public void defaultAnnotation() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setRequestURI("/default");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(new String[]{"GET"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[]{"*"}, config.getAllowedOrigins().toArray());
		assertTrue(config.isAllowCredentials());
		assertArrayEquals(new String[]{"*"}, config.getAllowedHeaders().toArray());
		assertNull(config.getExposedHeaders());
		assertEquals(new Long(1800), config.getMaxAge());
	}

	@Test
	public void customized() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setRequestURI("/customized");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(new String[]{"DELETE"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[]{"http://site1.com", "http://site2.com"}, config.getAllowedOrigins().toArray());
		assertArrayEquals(new String[]{"header1", "header2"}, config.getAllowedHeaders().toArray());
		assertArrayEquals(new String[]{"header3", "header4"}, config.getExposedHeaders().toArray());
		assertEquals(new Long(123), config.getMaxAge());
		assertEquals(false, config.isAllowCredentials());
	}

	@Test
	public void classLevel() throws Exception {
		this.handlerMapping.registerHandler(new ClassLevelController());
		this.request.setRequestURI("/foo");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(new String[]{"GET"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[]{"*"}, config.getAllowedOrigins().toArray());
	}

	@Test
	public void preFlightRequest() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setMethod("OPTIONS");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.setRequestURI("/default");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, true);
		assertNotNull(config);
		assertArrayEquals(new String[]{"GET"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[]{"*"}, config.getAllowedOrigins().toArray());
		assertTrue(config.isAllowCredentials());
		assertArrayEquals(new String[]{"*"}, config.getAllowedHeaders().toArray());
		assertNull(config.getExposedHeaders());
		assertEquals(new Long(1800), config.getMaxAge());
	}

	@Test
	public void ambiguousHeaderPreFlightRequest() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setMethod("OPTIONS");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_HEADERS, "header1");
		this.request.setRequestURI("/ambiguous-header");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, true);
		assertNotNull(config);
		assertArrayEquals(new String[]{"*"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[]{"*"}, config.getAllowedOrigins().toArray());
		assertArrayEquals(new String[]{"*"}, config.getAllowedHeaders().toArray());
		assertTrue(config.isAllowCredentials());
		assertNull(config.getExposedHeaders());
		assertNull(config.getMaxAge());
	}

	@Test
	public void ambiguousProducesPreFlightRequest() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setMethod("OPTIONS");
		this.request.addHeader(CorsUtils.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.setRequestURI("/ambiguous-produces");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, true);
		assertNotNull(config);
		assertArrayEquals(new String[]{"*"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[]{"*"}, config.getAllowedOrigins().toArray());
		assertArrayEquals(new String[]{"*"}, config.getAllowedHeaders().toArray());
		assertTrue(config.isAllowCredentials());
		assertNull(config.getExposedHeaders());
		assertNull(config.getMaxAge());
	}

	@Test
	public void preFlightRequestWithoutRequestMethodHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/default");
		request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		assertNull(this.handlerMapping.getHandler(request));
	}

	private CorsConfiguration getCorsConfiguration(HandlerExecutionChain chain, boolean isPreFlightRequest) {
		if (isPreFlightRequest) {
			Object handler = chain.getHandler();
			assertTrue(handler.getClass().getSimpleName().equals("PreFlightHandler"));
			DirectFieldAccessor accessor = new DirectFieldAccessor(handler);
			return (CorsConfiguration)accessor.getPropertyValue("config");
		}
		else {
			HandlerInterceptor[] interceptors = chain.getInterceptors();
			if (interceptors != null) {
				for (HandlerInterceptor interceptor : interceptors) {
					if (interceptor.getClass().getSimpleName().equals("CorsInterceptor")) {
						DirectFieldAccessor accessor = new DirectFieldAccessor(interceptor);
						return (CorsConfiguration) accessor.getPropertyValue("config");
					}
				}
			}
		}
		return null;
	}


	@Controller
	private static class MethodLevelController {

		@RequestMapping(value = "/no", method = RequestMethod.GET)
		public void noAnnotation() {
		}

		@RequestMapping(value = "/no", method = RequestMethod.POST)
		public void noAnnotationPost() {
		}

		@CrossOrigin
		@RequestMapping(value = "/default", method = RequestMethod.GET)
		public void defaultAnnotation() {
		}

		@CrossOrigin
		@RequestMapping(value = "/default", method = RequestMethod.GET, params = "q")
		public void defaultAnnotationWithParams() {
		}

		@CrossOrigin
		@RequestMapping(value = "/ambiguous-header", method = RequestMethod.GET, headers = "header1=a")
		public void ambigousHeader1a() {
		}

		@CrossOrigin
		@RequestMapping(value = "/ambiguous-header", method = RequestMethod.GET, headers = "header1=b")
		public void ambigousHeader1b() {
		}

		@CrossOrigin
		@RequestMapping(value = "/ambiguous-produces", method = RequestMethod.GET, produces = "application/xml")
		public String ambigousProducesXml() {
			return "<a></a>";
		}

		@CrossOrigin
		@RequestMapping(value = "/ambiguous-produces", method = RequestMethod.GET, produces = "application/json")
		public String ambigousProducesJson() {
			return "{}";
		}

		@CrossOrigin(origin = { "http://site1.com", "http://site2.com" }, allowedHeaders = { "header1", "header2" },
				exposedHeaders = { "header3", "header4" }, method = RequestMethod.DELETE, maxAge = 123, allowCredentials = "false")
		@RequestMapping(value = "/customized", method = { RequestMethod.GET, RequestMethod.POST } )
		public void customized() {
		}

	}

	@Controller
	@CrossOrigin
	private static class ClassLevelController {

		@RequestMapping(value = "/foo", method = RequestMethod.GET)
		public void foo() {
		}
	}

	private static class TestRequestMappingInfoHandlerMapping extends RequestMappingHandlerMapping {

		public void registerHandler(Object handler) {
			super.detectHandlerMethods(handler);
		}

		@Override
		protected boolean isHandler(Class<?> beanType) {
			return AnnotationUtils.findAnnotation(beanType, Controller.class) != null;
		}

		@Override
		protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
			RequestMapping annotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
			if (annotation != null) {
				return new RequestMappingInfo(
						new PatternsRequestCondition(annotation.value(), getUrlPathHelper(), getPathMatcher(), true, true),
						new RequestMethodsRequestCondition(annotation.method()),
						new ParamsRequestCondition(annotation.params()),
						new HeadersRequestCondition(annotation.headers()),
						new ConsumesRequestCondition(annotation.consumes(), annotation.headers()),
						new ProducesRequestCondition(annotation.produces(), annotation.headers()), null);
			}
			else {
				return null;
			}
		}
	}

}
