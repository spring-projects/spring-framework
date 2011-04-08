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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Arrays;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.StaticApplicationContext;
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

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class RequestMappingHandlerMethodMappingTests {

	private MyRequestMappingHandlerMethodMapping mapping;

	private MyHandler handler;

	private HandlerMethod fooMethod;

	private HandlerMethod barMethod;

	@Before
	public void setUp() throws Exception {
		handler = new MyHandler();
		fooMethod = new HandlerMethod(handler, "foo");
		barMethod = new HandlerMethod(handler, "bar");

		StaticApplicationContext context = new StaticApplicationContext();
		context.registerSingleton("handler", handler.getClass());

		mapping = new MyRequestMappingHandlerMethodMapping();
		mapping.setApplicationContext(context);
	}
	
	@Test
	public void directMatch() throws Exception {
		HandlerMethod result = mapping.getHandlerInternal(new MockHttpServletRequest("GET", "/foo"));
		assertEquals(fooMethod.getMethod(), result.getMethod());
	}

	@Test
	public void globMatch() throws Exception {
		HandlerMethod result = mapping.getHandlerInternal(new MockHttpServletRequest("GET", "/bar"));
		assertEquals(barMethod.getMethod(), result.getMethod());
	}

	@Test
	public void methodNotAllowed() throws Exception {
		try {
			mapping.getHandlerInternal(new MockHttpServletRequest("POST", "/bar"));
			fail("HttpRequestMethodNotSupportedException expected");
		}
		catch (HttpRequestMethodNotSupportedException ex) {
			assertArrayEquals("Invalid supported methods", new String[]{"GET", "HEAD"}, ex.getSupportedMethods());
		}
	}

	@Test
	public void uriTemplateVariables() {
		RequestKey key = new RequestKey(Arrays.asList("/{path1}/{path2}"), null);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/1/2");

		mapping.handleMatch(key, request);
		
		@SuppressWarnings("unchecked")
		Map<String, String> actual = (Map<String, String>) request.getAttribute(
				HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		
		assertNotNull(actual);
		assertEquals("1", actual.get("path1"));
		assertEquals("2", actual.get("path2"));
	}
	
	@Test
	public void mappedInterceptors() {
		String path = "/handle";
		HandlerInterceptor interceptor = new HandlerInterceptorAdapter() {};
		MappedInterceptor mappedInterceptor = new MappedInterceptor(new String[] {path}, interceptor);

		MyRequestMappingHandlerMethodMapping mapping = new MyRequestMappingHandlerMethodMapping();
		mapping.setMappedInterceptors(new MappedInterceptor[] { mappedInterceptor });

		HandlerExecutionChain chain = mapping.getHandlerExecutionChain(handler, new MockHttpServletRequest("GET", path));
		assertNotNull(chain.getInterceptors());
		assertSame(interceptor, chain.getInterceptors()[0]);

		chain = mapping.getHandlerExecutionChain(handler, new MockHttpServletRequest("GET", "/invalid"));
		assertNull(chain.getInterceptors());
	}

	private static class MyRequestMappingHandlerMethodMapping extends RequestMappingHandlerMethodMapping {

		@Override
		public HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
			return super.getHandlerInternal(request);
		}
	}

	@Controller
	private static class MyHandler {

		@SuppressWarnings("unused")
		@RequestMapping(value = "/foo", method = RequestMethod.GET)
		public void foo() {
		}

		@SuppressWarnings("unused")
		@RequestMapping(value = "/ba*", method = { RequestMethod.GET, RequestMethod.HEAD })
		public void bar() {
		}

	}

}
