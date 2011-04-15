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

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Test fixture with {@link InvocableHandlerMethod}.
 * 
 * @author Rossen Stoyanchev
 */
public class InvocableHandlerMethodTests {

	private HandlerMethodArgumentResolverComposite argumentResolvers;

	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		argumentResolvers = new HandlerMethodArgumentResolverComposite();
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse());
	}

	@Test
	public void resolveArgument() throws Exception {
		StubArgumentResolver intResolver = addResolver(Integer.class, 99);
		StubArgumentResolver strResolver = addResolver(String.class, "value");
		InvocableHandlerMethod method = invocableHandlerMethod("handle", Integer.class, String.class);
		Object returnValue = method.invokeForRequest(webRequest, null);
		
		assertEquals("Integer resolver not invoked", 1, intResolver.getResolvedParameters().size());
		assertEquals("String resolver not invoked", 1, strResolver.getResolvedParameters().size());
		assertEquals("Invalid return value", "99-value", returnValue);
	}
	
	@Test
	public void resolveProvidedArgument() throws Exception {
		InvocableHandlerMethod method = invocableHandlerMethod("handle", Integer.class, String.class);
		Object returnValue = method.invokeForRequest(webRequest, null, 99, "value");

		assertEquals("Expected raw return value with no handlers registered", String.class, returnValue.getClass());
		assertEquals("Provided argument values were not resolved", "99-value", returnValue);
	}

	@Test
	public void discoverParameterName() throws Exception {
		StubArgumentResolver resolver = addResolver(Integer.class, 99);
		InvocableHandlerMethod method = invocableHandlerMethod("parameterNameDiscovery", Integer.class);
		method.invokeForRequest(webRequest, null);
		
		assertEquals("intArg", resolver.getResolvedParameters().get(0).getParameterName());
	}

	private StubArgumentResolver addResolver(Class<?> parameterType, Object stubValue) {
		StubArgumentResolver resolver = new StubArgumentResolver(parameterType, stubValue);
		argumentResolvers.addResolver(resolver);
		return resolver;
	}
	
	private InvocableHandlerMethod invocableHandlerMethod(String methodName, Class<?>... paramTypes)
			throws Exception {
		Method method = Handler.class.getDeclaredMethod(methodName, paramTypes);
		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(new Handler(), method);
		handlerMethod.setHandlerMethodArgumentResolvers(argumentResolvers);
		return handlerMethod;
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
	}

}
