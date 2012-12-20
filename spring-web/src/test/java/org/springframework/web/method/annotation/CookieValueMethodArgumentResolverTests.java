/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.AbstractCookieValueMethodArgumentResolver;

/**
 * Test fixture with {@link org.springframework.web.method.annotation.AbstractCookieValueMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class CookieValueMethodArgumentResolverTests {

	private AbstractCookieValueMethodArgumentResolver resolver;

	private MethodParameter paramNamedCookie;

	private MethodParameter paramNamedDefaultValueString;

	private MethodParameter paramString;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest request;

	@Before
	public void setUp() throws Exception {
		resolver = new TestCookieValueMethodArgumentResolver();

		Method method = getClass().getMethod("params", Cookie.class, String.class, String.class);
		paramNamedCookie = new MethodParameter(method, 0);
		paramNamedDefaultValueString = new MethodParameter(method, 1);
		paramString = new MethodParameter(method, 2);

		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());
	}

	@Test
	public void supportsParameter() {
		assertTrue("Cookie parameter not supported", resolver.supportsParameter(paramNamedCookie));
		assertTrue("Cookie string parameter not supported", resolver.supportsParameter(paramNamedDefaultValueString));
		assertFalse("non-@CookieValue parameter supported", resolver.supportsParameter(paramString));
	}

	@Test
	public void resolveCookieDefaultValue() throws Exception {
		Object result = resolver.resolveArgument(paramNamedDefaultValueString, null, webRequest, null);

		assertTrue(result instanceof String);
		assertEquals("Invalid result", "bar", result);
	}

	@Test(expected = ServletRequestBindingException.class)
	public void notFound() throws Exception {
		resolver.resolveArgument(paramNamedCookie, null, webRequest, null);
		fail("Expected exception");
	}

	private static class TestCookieValueMethodArgumentResolver extends AbstractCookieValueMethodArgumentResolver {

		public TestCookieValueMethodArgumentResolver() {
			super(null);
		}

		@Override
		protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request) throws Exception {
			return null;
		}
	}

	public void params(@CookieValue("name") Cookie param1,
					   @CookieValue(value = "name", defaultValue = "bar") String param2,
					   String param3) {
	}

}
