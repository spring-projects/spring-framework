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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.RequestHeader;
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
@RunWith(Parameterized.class)
public class RequestHeaderMethodArgumentResolverTests {

	@Parameters(name = "{0}")
	public static List<Object[]> createParameters() {
		List<Object[]> parameters = new ArrayList<Object[]>();
		parameters.add(new Object[] { Foo.class });
		parameters.add(new Object[] { Bar.class });
		parameters.add(new Object[] { Baz.class });
		return parameters;
	}

	@Parameter(0)
	public Class<?> target;
	private RequestHeaderMethodArgumentResolver resolver;

	private MethodParameter paramNamedDefaultValueStringHeader;
	private MethodParameter paramNamedValueStringArray;
	private MethodParameter paramSystemProperty;
	private MethodParameter paramContextPath;
	private MethodParameter paramResolvedName;
	private MethodParameter paramNamedValueMap;

	private MockHttpServletRequest servletRequest;

	private NativeWebRequest webRequest;


	@Before
	@SuppressWarnings("resource")
	public void setUp() throws Exception {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.refresh();
		resolver = new RequestHeaderMethodArgumentResolver(context.getBeanFactory());

		Method method = target.getMethod("params", String.class, String[].class,
				String.class, String.class, String.class, Map.class);
		paramNamedDefaultValueStringHeader = new SynthesizingMethodParameter(method, 0);
		paramNamedValueStringArray = new SynthesizingMethodParameter(method, 1);
		paramSystemProperty = new SynthesizingMethodParameter(method, 2);
		paramContextPath = new SynthesizingMethodParameter(method, 3);
		paramResolvedName = new SynthesizingMethodParameter(method, 4);
		paramNamedValueMap = new SynthesizingMethodParameter(method, 5);

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
		String[] expected = new String[] {"foo", "bar"};
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
	public void resolveNameFromSystemProperty() throws Exception {
		String expected = "foo";
		servletRequest.addHeader("bar", expected);

		System.setProperty("systemProperty", "bar");
		try {
			Object result = resolver.resolveArgument(paramResolvedName, null, webRequest, null);
			assertTrue(result instanceof String);
			assertEquals("Invalid result", expected, result);
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


	public interface Foo {
		void params(
				@RequestHeader(name = "name", defaultValue = "bar") String param1,
				@RequestHeader("name") String[] param2,
				@RequestHeader(name = "name", defaultValue="#{systemProperties.systemProperty}") String param3,
				@RequestHeader(name = "name", defaultValue="#{request.contextPath}") String param4,
				@RequestHeader("#{systemProperties.systemProperty}") String param5,
				@RequestHeader("name") Map<?, ?> unsupported);
	}

	public static abstract class Bar implements Foo {
	}

	public static class Baz extends Bar {

		@Override
		public void params(String param1, String[] param2, String param3, String param4,
				String param5, Map<?, ?> unsupported) {
		}

	}
}
