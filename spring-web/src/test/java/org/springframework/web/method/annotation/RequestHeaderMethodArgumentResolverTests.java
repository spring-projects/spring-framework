/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;

import static org.junit.Assert.*;

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
	private MethodParameter paramResolvedNameWithExpression;
	private MethodParameter paramResolvedNameWithPlaceholder;
	private MethodParameter paramNamedValueMap;
	private MethodParameter paramDate;
	private MethodParameter paramInstant;

	private MockHttpServletRequest servletRequest;

	private NativeWebRequest webRequest;


	@Before
	@SuppressWarnings("resource")
	public void setUp() throws Exception {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.refresh();
		resolver = new RequestHeaderMethodArgumentResolver(context.getBeanFactory());

		Method method = ReflectionUtils.findMethod(getClass(), "params", (Class<?>[]) null);
		paramNamedDefaultValueStringHeader = new SynthesizingMethodParameter(method, 0);
		paramNamedValueStringArray = new SynthesizingMethodParameter(method, 1);
		paramSystemProperty = new SynthesizingMethodParameter(method, 2);
		paramContextPath = new SynthesizingMethodParameter(method, 3);
		paramResolvedNameWithExpression = new SynthesizingMethodParameter(method, 4);
		paramResolvedNameWithPlaceholder = new SynthesizingMethodParameter(method, 5);
		paramNamedValueMap = new SynthesizingMethodParameter(method, 6);
		paramDate = new SynthesizingMethodParameter(method, 7);
		paramInstant = new SynthesizingMethodParameter(method, 8);

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
		assertEquals(expected, result);
	}

	@Test
	public void resolveStringArrayArgument() throws Exception {
		String[] expected = new String[] {"foo", "bar"};
		servletRequest.addHeader("name", expected);

		Object result = resolver.resolveArgument(paramNamedValueStringArray, null, webRequest, null);
		assertTrue(result instanceof String[]);
		assertArrayEquals(expected, (String[]) result);
	}

	@Test
	public void resolveDefaultValue() throws Exception {
		Object result = resolver.resolveArgument(paramNamedDefaultValueStringHeader, null, webRequest, null);
		assertTrue(result instanceof String);
		assertEquals("bar", result);
	}

	@Test
	public void resolveDefaultValueFromSystemProperty() throws Exception {
		System.setProperty("systemProperty", "bar");
		try {
			Object result = resolver.resolveArgument(paramSystemProperty, null, webRequest, null);
			assertTrue(result instanceof String);
			assertEquals("bar", result);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	public void resolveNameFromSystemPropertyThroughExpression() throws Exception {
		String expected = "foo";
		servletRequest.addHeader("bar", expected);

		System.setProperty("systemProperty", "bar");
		try {
			Object result = resolver.resolveArgument(paramResolvedNameWithExpression, null, webRequest, null);
			assertTrue(result instanceof String);
			assertEquals(expected, result);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	public void resolveNameFromSystemPropertyThroughPlaceholder() throws Exception {
		String expected = "foo";
		servletRequest.addHeader("bar", expected);

		System.setProperty("systemProperty", "bar");
		try {
			Object result = resolver.resolveArgument(paramResolvedNameWithPlaceholder, null, webRequest, null);
			assertTrue(result instanceof String);
			assertEquals(expected, result);
		}
		finally {
			System.clearProperty("systemProperty");
		}
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
	}

	@Test
	@SuppressWarnings("deprecation")
	public void dateConversion() throws Exception {
		String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
		servletRequest.addHeader("name", rfc1123val);

		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setConversionService(new DefaultFormattingConversionService());
		Object result = resolver.resolveArgument(paramDate, null, webRequest,
				new DefaultDataBinderFactory(bindingInitializer));

		assertTrue(result instanceof Date);
		assertEquals(new Date(rfc1123val), result);
	}

	@Test
	public void instantConversion() throws Exception {
		String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
		servletRequest.addHeader("name", rfc1123val);

		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setConversionService(new DefaultFormattingConversionService());
		Object result = resolver.resolveArgument(paramInstant, null, webRequest,
				new DefaultDataBinderFactory(bindingInitializer));

		assertTrue(result instanceof Instant);
		assertEquals(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(rfc1123val)), result);
	}


	public void params(
			@RequestHeader(name = "name", defaultValue = "bar") String param1,
			@RequestHeader("name") String[] param2,
			@RequestHeader(name = "name", defaultValue="#{systemProperties.systemProperty}") String param3,
			@RequestHeader(name = "name", defaultValue="#{request.contextPath}") String param4,
			@RequestHeader("#{systemProperties.systemProperty}") String param5,
			@RequestHeader("${systemProperty}") String param6,
			@RequestHeader("name") Map<?, ?> unsupported,
			@RequestHeader("name") Date dateParam,
			@RequestHeader("name") Instant instantParam) {
	}

}
