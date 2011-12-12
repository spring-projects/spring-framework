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

package org.springframework.web.method.annotation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver;

/**
 * Test fixture with {@link org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver}.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestHeaderMethodArgumentResolverTests {

	private RequestHeaderMethodArgumentResolver resolver;

	private MethodParameter paramNamedDefaultValueStringHeader;
	private MethodParameter paramNamedValueStringArray;
	private MethodParameter paramSystemProperty;
	private MethodParameter paramContextPath;
	private MethodParameter paramNamedValueMap;

	private MockHttpServletRequest servletRequest;

	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.refresh();
		resolver = new RequestHeaderMethodArgumentResolver(context.getBeanFactory());

		Method method = getClass().getMethod("params", String.class, String[].class, String.class, String.class, Map.class);
		paramNamedDefaultValueStringHeader = new MethodParameter(method, 0);
		paramNamedValueStringArray = new MethodParameter(method, 1);
		paramSystemProperty = new MethodParameter(method, 2);
		paramContextPath = new MethodParameter(method, 3);
		paramNamedValueMap = new MethodParameter(method, 4);

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
	public void supportsParameter() {
		assertTrue("String parameter not supported", resolver.supportsParameter(paramNamedDefaultValueStringHeader));
		assertTrue("String array parameter not supported", resolver.supportsParameter(paramNamedValueStringArray));
		assertFalse("non-@RequestParam parameter supported", resolver.supportsParameter(paramNamedValueMap));
	}

	@Test
	public void resolveStringArgument() throws Exception {
		String expected = "foo";
		servletRequest.addHeader("name", expected);

		Object result = resolver.resolveArgument(paramNamedDefaultValueStringHeader, null, webRequest, null);

		assertTrue(result instanceof String);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveStringArrayArgument() throws Exception {
		String[] expected = new String[]{"foo", "bar"};
		servletRequest.addHeader("name", expected);

		Object result = resolver.resolveArgument(paramNamedValueStringArray, null, webRequest, null);

		assertTrue(result instanceof String[]);
		assertArrayEquals("Invalid result", expected, (String[]) result);
	}

	@Test
	public void resolveDefaultValue() throws Exception {
		Object result = resolver.resolveArgument(paramNamedDefaultValueStringHeader, null, webRequest, null);

		assertTrue(result instanceof String);
		assertEquals("Invalid result", "bar", result);
	}

	@Test
	public void resolveDefaultValueFromSystemProperty() throws Exception {
		System.setProperty("systemProperty", "bar");
		Object result = resolver.resolveArgument(paramSystemProperty, null, webRequest, null);
		System.clearProperty("systemProperty");

		assertTrue(result instanceof String);
		assertEquals("bar", result);
	}

	@Test
	public void resolveDefaultValueFromRequest() throws Exception {
		servletRequest.setContextPath("/bar");
		Object result = resolver.resolveArgument(paramContextPath, null, webRequest, null);

		assertTrue(result instanceof String);
		assertEquals("/bar", result);
	}

	@Test(expected = ServletRequestBindingException.class)
	public void notFound() throws Exception {
		resolver.resolveArgument(paramNamedValueStringArray, null, webRequest, null);
		fail("Expected exception");
	}

	public void params(@RequestHeader(value = "name", defaultValue = "bar") String param1,
					   @RequestHeader("name") String[] param2,
					   @RequestHeader(value = "name", defaultValue="#{systemProperties.systemProperty}") String param3,
					   @RequestHeader(value = "name", defaultValue="#{request.contextPath}") String param4,
					   @RequestHeader("name") Map<?, ?> unsupported) {
	}

}