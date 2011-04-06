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

package org.springframework.web.method.annotation.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.annotation.support.ExpressionValueMethodArgumentResolver;

/**
 * Test fixture for {@link ExpressionValueMethodArgumentResolver} unit tests.
 * 
 * @author Rossen Stoyanchev
 */
public class ExpressionValueMethodArgumentResolverTests {

	private ExpressionValueMethodArgumentResolver resolver;

	private MethodParameter systemParameter;

	private MethodParameter requestParameter;

	private MethodParameter unsupported;

	private MockHttpServletRequest servletRequest;

	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.refresh();
		
		resolver = new ExpressionValueMethodArgumentResolver(context.getBeanFactory());
		
		Method method = getClass().getMethod("params", int.class, String.class, String.class);
		systemParameter = new MethodParameter(method, 0);
		requestParameter = new MethodParameter(method, 1);
		unsupported = new MethodParameter(method, 2);

		servletRequest = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(servletRequest, new MockHttpServletResponse());
		
		// Expose request to the current thread (for SpEL expressions)
		RequestContextHolder.setRequestAttributes(webRequest);
	}
	
	@After
	public void teardown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	public void supportsParameter() throws Exception {
		assertTrue(resolver.supportsParameter(systemParameter));
		assertTrue(resolver.supportsParameter(requestParameter));
		
		assertFalse(resolver.supportsParameter(unsupported));
	}

	@Test
	public void resolveSystemProperty() throws Exception {
		System.setProperty("systemIntValue", "22");
		Object value = resolver.resolveArgument(systemParameter, null, webRequest, null);
		
		assertEquals("22", value);
	}

	@Test
	public void resolveRequestProperty() throws Exception {
		servletRequest.setContextPath("/contextPath");
		Object value = resolver.resolveArgument(requestParameter, null, webRequest, null);
		
		assertEquals("/contextPath", value);
	}

	public void params(@Value("#{systemProperties.systemIntValue}") int param1,
					   @Value("#{request.contextPath}") String param2,
					   String unsupported) {
	}

}
