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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;

/**
 * Fine-grained {@link RequestMappingHandlerAdapter} unit tests.
 *
 * <p>For higher-level adapter tests see:
 * <ul>
 * <li>{@link ServletHandlerMethodTests}
 * <li>{@link RequestMappingHandlerAdapterIntegrationTests}
 * <li>{@link HandlerMethodAdapterAnnotationDetectionTests}
 * </ul>
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingHandlerAdapterTests {

	private RequestMappingHandlerAdapter handlerAdapter;
	
	private MockHttpServletRequest request;
	
	private MockHttpServletResponse response;

	@Before
	public void setup() throws Exception {
		this.handlerAdapter = new RequestMappingHandlerAdapter();
		this.handlerAdapter.setApplicationContext(new GenericWebApplicationContext());
		this.handlerAdapter.afterPropertiesSet();

		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
	}

	@Test
	public void cacheControlWithoutSessionAttributes() throws Exception {
		handlerAdapter.setCacheSeconds(100);
		handlerAdapter.handle(request, response, handlerMethod(new SimpleHandler(), "handle"));

		assertTrue(response.getHeader("Cache-Control").toString().contains("max-age"));
	}

	@Test
	public void cacheControlWithSessionAttributes() throws Exception {
		handlerAdapter.setCacheSeconds(100);
		handlerAdapter.handle(request, response, handlerMethod(new SessionAttributeHandler(), "handle"));

		assertEquals("no-cache", response.getHeader("Cache-Control"));
	}

	private HandlerMethod handlerMethod(Object handler, String methodName, Class<?>... paramTypes) throws Exception {
		Method method = handler.getClass().getDeclaredMethod(methodName, paramTypes);
		return new InvocableHandlerMethod(handler, method);
	}

	private static class SimpleHandler {

		@SuppressWarnings("unused")
		public void handle() {
		}
	}

	@SessionAttributes("attr1")
	private static class SessionAttributeHandler {

		@SuppressWarnings("unused")
		public void handle() {
		}
	}

}