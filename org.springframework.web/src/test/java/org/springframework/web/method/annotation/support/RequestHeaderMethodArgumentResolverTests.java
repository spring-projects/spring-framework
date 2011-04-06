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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.annotation.support.RequestHeaderMethodArgumentResolver;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestHeaderMethodArgumentResolverTests {

	private RequestHeaderMethodArgumentResolver resolver;

	private MethodParameter stringParameter;

	private MethodParameter stringArrayParameter;

	private MethodParameter systemPropertyParameter;

	private MethodParameter contextPathParameter;

	private MethodParameter otherParameter;

	private MockHttpServletRequest servletRequest;

	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.refresh();
		
		resolver = new RequestHeaderMethodArgumentResolver(context.getBeanFactory());

		Method method = getClass().getMethod("params", String.class, String[].class, String.class, String.class, Map.class);
		stringParameter = new MethodParameter(method, 0);
		stringArrayParameter = new MethodParameter(method, 1);
		systemPropertyParameter = new MethodParameter(method, 2);
		contextPathParameter = new MethodParameter(method, 3);
		otherParameter = new MethodParameter(method, 4);

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
	public void usesResponseArgument() throws NoSuchMethodException {
		assertFalse("resolver uses response argument", resolver.usesResponseArgument(null));
	}

	@Test
	public void supportsParameter() {
		assertTrue("String parameter not supported", resolver.supportsParameter(stringParameter));
		assertTrue("String array parameter not supported", resolver.supportsParameter(stringArrayParameter));
		assertFalse("non-@RequestParam parameter supported", resolver.supportsParameter(otherParameter));
	}

	@Test
	public void resolveStringArgument() throws Exception {
		String expected = "foo";
		servletRequest.addHeader("name", expected);

		String result = (String) resolver.resolveArgument(stringParameter, null, webRequest, null);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveStringArrayArgument() throws Exception {
		String[] expected = new String[]{"foo", "bar"};
		servletRequest.addHeader("name", expected);

		String[] result = (String[]) resolver.resolveArgument(stringArrayParameter, null, webRequest, null);
		assertArrayEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveDefaultValue() throws Exception {
		String result = (String) resolver.resolveArgument(stringParameter, null, webRequest, null);
		assertEquals("Invalid result", "bar", result);
	}

	@Test
	public void resolveDefaultValueFromSystemProperty() throws Exception {
		System.setProperty("header", "bar");
		String result = (String) resolver.resolveArgument(systemPropertyParameter, null, webRequest, null);
		assertEquals("bar", result);
	}

	@Test
	public void resolveDefaultValueFromRequest() throws Exception {
		servletRequest.setContextPath("/bar");
		String result = (String) resolver.resolveArgument(contextPathParameter, null, webRequest, null);
		assertEquals("/bar", result);
	}

	@Test(expected = IllegalStateException.class)
	public void notFound() throws Exception {
		String result = (String) resolver.resolveArgument(stringArrayParameter, null, webRequest, null);
		assertEquals("Invalid result", "bar", result);
	}

	public void params(@RequestHeader(value = "name", defaultValue = "bar") String param1,
					   @RequestHeader("name") String[] param2,
					   @RequestHeader(value = "name", defaultValue="#{systemProperties.header}") String param3,
					   @RequestHeader(value = "name", defaultValue="#{request.contextPath}") String param4,
					   @RequestHeader("name") Map<?, ?> unsupported) {
	}

}
