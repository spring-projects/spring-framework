/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link ExpressionValueMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class ExpressionValueMethodArgumentResolverTests {

	private ExpressionValueMethodArgumentResolver resolver;

	private MethodParameter paramSystemProperty;

	private MethodParameter paramContextPath;

	private MethodParameter paramNotSupported;

	private NativeWebRequest webRequest;

	@Before
	@SuppressWarnings("resource")
	public void setUp() throws Exception {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.refresh();
		resolver = new ExpressionValueMethodArgumentResolver(context.getBeanFactory());

		Method method = getClass().getMethod("params", int.class, String.class, String.class);
		paramSystemProperty = new MethodParameter(method, 0);
		paramContextPath = new MethodParameter(method, 1);
		paramNotSupported = new MethodParameter(method, 2);

		webRequest = new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse());

		// Expose request to the current thread (for SpEL expressions)
		RequestContextHolder.setRequestAttributes(webRequest);
	}

	@After
	public void teardown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	public void supportsParameter() throws Exception {
		assertTrue(resolver.supportsParameter(paramSystemProperty));
		assertTrue(resolver.supportsParameter(paramContextPath));
		assertFalse(resolver.supportsParameter(paramNotSupported));
	}

	@Test
	public void resolveSystemProperty() throws Exception {
		System.setProperty("systemProperty", "22");
		Object value = resolver.resolveArgument(paramSystemProperty, null, webRequest, null);
		System.clearProperty("systemProperty");

		assertEquals("22", value);
	}

	@Test
	public void resolveContextPath() throws Exception {
		webRequest.getNativeRequest(MockHttpServletRequest.class).setContextPath("/contextPath");
		Object value = resolver.resolveArgument(paramContextPath, null, webRequest, null);

		assertEquals("/contextPath", value);
	}

	public void params(@Value("#{systemProperties.systemProperty}") int param1,
			@Value("#{request.contextPath}") String param2, String notSupported) {
	}

}
