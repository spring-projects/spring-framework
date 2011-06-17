/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * Test fixture with {@link RequestMappingHandlerMapping}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestMappingInfoHandlerMappingTests {

	private TestRequestMappingInfoHandlerMapping mapping;

	private Handler handler;

	private HandlerMethod fooMethod;

	private HandlerMethod fooParamMethod;

	private HandlerMethod barMethod;

	private HandlerMethod emptyMethod;

	@Before
	public void setUp() throws Exception {
		handler = new Handler();
		fooMethod = new HandlerMethod(handler, "foo");
		fooParamMethod = new HandlerMethod(handler, "fooParam");
		barMethod = new HandlerMethod(handler, "bar");
		emptyMethod = new HandlerMethod(handler, "empty");

		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("handler", handler.getClass());

		mapping = new TestRequestMappingInfoHandlerMapping();
		mapping.setApplicationContext(context);
	}

	@Test
	public void directMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		HandlerMethod hm = (HandlerMethod) mapping.getHandler(request).getHandler();
		assertEquals(fooMethod.getMethod(), hm.getMethod());
	}

	@Test
	public void globMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bar");
		HandlerMethod hm = (HandlerMethod) mapping.getHandler(request).getHandler();
		assertEquals(barMethod.getMethod(), hm.getMethod());
	}

	@Test
	public void emptyPathMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
		HandlerMethod hm = (HandlerMethod) mapping.getHandler(request).getHandler();
		assertEquals(emptyMethod.getMethod(), hm.getMethod());

		request = new MockHttpServletRequest("GET", "/");
		hm = (HandlerMethod) mapping.getHandler(request).getHandler();
		assertEquals(emptyMethod.getMethod(), hm.getMethod());
	}

	@Test
	public void bestMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("p", "anything");
		HandlerMethod hm = (HandlerMethod) mapping.getHandler(request).getHandler();
		assertEquals(fooParamMethod.getMethod(), hm.getMethod());
	}
	
	@Test
	public void methodNotAllowed() throws Exception {
		try {
			MockHttpServletRequest request = new MockHttpServletRequest("POST", "/bar");
			mapping.getHandler(request);
			fail("HttpRequestMethodNotSupportedException expected");
		}
		catch (HttpRequestMethodNotSupportedException ex) {
			assertArrayEquals("Invalid supported methods", new String[]{"GET", "HEAD"}, ex.getSupportedMethods());
		}
	}

	@Test
	public void uriTemplateVariables() {
		RequestMappingInfo key = new RequestMappingInfo(new String[] {"/{path1}/{path2}"});
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/1/2");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);

		mapping.handleMatch(key, lookupPath, request);
		
		@SuppressWarnings("unchecked")
		Map<String, String> actual = (Map<String, String>) request.getAttribute(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		
		assertNotNull(actual);
		assertEquals("1", actual.get("path1"));
		assertEquals("2", actual.get("path2"));
	}
	
	@Test
	public void mappedInterceptors() throws Exception {
		String path = "/foo";
		HandlerInterceptor interceptor = new HandlerInterceptorAdapter() {};
		MappedInterceptor mappedInterceptor = new MappedInterceptor(new String[] {path}, interceptor);

		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("handler", handler.getClass());

		mapping = new TestRequestMappingInfoHandlerMapping();
		mapping.setInterceptors(new Object[] { mappedInterceptor });
		mapping.setApplicationContext(context);

		HandlerExecutionChain chain = mapping.getHandler(new MockHttpServletRequest("GET", path));
		assertNotNull(chain);
		assertNotNull(chain.getInterceptors());
		assertSame(interceptor, chain.getInterceptors()[0]);

		chain = mapping.getHandler(new MockHttpServletRequest("GET", "/invalid"));
		assertNull(chain);
	}

	@SuppressWarnings("unused")
	@Controller
	private static class Handler {

		@RequestMapping(value = "/foo", method = RequestMethod.GET)
		public void foo() {
		}
		
		@RequestMapping(value = "/foo", method = RequestMethod.GET, params="p")
		public void fooParam() {
		}

		@RequestMapping(value = "/ba*", method = { RequestMethod.GET, RequestMethod.HEAD })
		public void bar() {
		}

		@RequestMapping(value = "")
		public void empty() {
		}
	}

	private static class TestRequestMappingInfoHandlerMapping extends RequestMappingInfoHandlerMapping {

		@Override
		protected boolean isHandler(Class<?> beanType) {
			return AnnotationUtils.findAnnotation(beanType, Controller.class) != null;
		}

		@Override
		protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
			RequestMapping annotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
			return new RequestMappingInfo(
					new PatternsRequestCondition(annotation.value(), getUrlPathHelper(), getPathMatcher()),
					new RequestMethodsRequestCondition(annotation.method()),
					new ParamsRequestCondition(annotation.params()),
					new HeadersRequestCondition(annotation.headers()),
					new ConsumesRequestCondition(annotation.consumes(), annotation.headers()),
					new ProducesRequestCondition(annotation.produces(), annotation.headers()));
		}
	}
	
}