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

package org.springframework.web.method.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Test fixture for {@link InvocableHandlerMethod} unit tests.
 * 
 * @author Rossen Stoyanchev
 */
public class InvocableHandlerMethodTests {

	private HandlerMethodArgumentResolverComposite argResolvers;

	private NativeWebRequest webRequest;

	private MockHttpServletResponse response;

	@Before
	public void setUp() throws Exception {
		argResolvers = new HandlerMethodArgumentResolverComposite();

		response = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest(), response);
	}

	@Test
	public void argResolutionAndReturnValueHandling() throws Exception {
		StubArgumentResolver resolver0 = registerResolver(Integer.class, 99, false);
		StubArgumentResolver resolver1 = registerResolver(String.class, "value", false);

		InvocableHandlerMethod method = handlerMethod(new Handler(), "handle", Integer.class, String.class);
		Object returnValue = method.invokeForRequest(webRequest, null);
		
		assertEquals("Integer resolver not invoked", 1, resolver0.getResolvedParameterNames().size());
		assertEquals("String resolver not invoked", 1, resolver1.getResolvedParameterNames().size());
		assertEquals("Invalid return value", "99-value", returnValue);
	}
	
	@Test
	public void providedArgResolution() throws Exception {
		InvocableHandlerMethod method = handlerMethod(new Handler(), "handle", Integer.class, String.class);
		Object returnValue = method.invokeForRequest(webRequest, null, 99, "value");

		assertEquals("Expected raw return value with no handlers registered", String.class, returnValue.getClass());
		assertEquals("Provided argument values were not resolved", "99-value", returnValue);
	}

	@Test
	public void parameterNameDiscovery() throws Exception {
		StubArgumentResolver resolver = registerResolver(Integer.class, 99, false);

		InvocableHandlerMethod method = handlerMethod(new Handler(), "parameterNameDiscovery", Integer.class);
		method.invokeForRequest(webRequest, null);
		
		assertEquals("intArg", resolver.getResolvedParameterNames().get(0).getParameterName());
	}

	@Test
	public void usesResponseArgResolver() throws Exception {
		InvocableHandlerMethod requestMethod = handlerMethod(new Handler(), "usesResponse", int.class);
		registerResolver(int.class, 99, true);

		assertTrue(requestMethod.usesResponseArgument());
	}

	private InvocableHandlerMethod handlerMethod(Object handler, String methodName, Class<?>... paramTypes)
			throws Exception {
		Method method = handler.getClass().getDeclaredMethod(methodName, paramTypes);
		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(handler, method);
		handlerMethod.setHandlerMethodArgumentResolvers(argResolvers);
		return handlerMethod;
	}
	
	private StubArgumentResolver registerResolver(Class<?> supportedType, Object stubValue, boolean usesResponse) {
		StubArgumentResolver resolver = new StubArgumentResolver(supportedType, stubValue, usesResponse);
		argResolvers.registerArgumentResolver(resolver);
		return resolver;
	}

	private static class Handler {
		
		@SuppressWarnings("unused")
		public String handle(Integer intArg, String stringArg) {
			return intArg + "-" + stringArg;
		}
	
		@SuppressWarnings("unused")
		@RequestMapping
		public void parameterNameDiscovery(Integer intArg) {
		}
	
		@SuppressWarnings("unused")
		@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "400 Bad Request")
		public void responseStatus() {
		}

		@SuppressWarnings("unused")
		public String usesResponse(int arg) {
			return "";
		}
	}
}
