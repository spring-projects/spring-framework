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

package org.springframework.web.servlet.mvc.method.annotation.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.support.CookieValueMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletCookieValueMethodArgumentResolver;

/**
 * @author Arjen Poutsma
 */
public class ServletCookieValueMethodArgumentResolverTests {

	private CookieValueMethodArgumentResolver resolver;

	private MethodParameter cookieParameter;

	private MethodParameter cookieStringParameter;

	private MethodParameter otherParameter;

	private MockHttpServletRequest servletRequest;

	private ServletWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		resolver = new ServletCookieValueMethodArgumentResolver(null);
		Method method = getClass().getMethod("params", Cookie.class, String.class, String.class);
		cookieParameter = new MethodParameter(method, 0);
		cookieStringParameter = new MethodParameter(method, 1);
		otherParameter = new MethodParameter(method, 2);

		servletRequest = new MockHttpServletRequest();
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(servletRequest, servletResponse);

	}

	@Test
	public void usesResponseArgument() throws NoSuchMethodException {
		assertFalse("resolver uses response argument", resolver.usesResponseArgument(null));
	}

	@Test
	public void supportsParameter() {
		assertTrue("Cookie parameter not supported", resolver.supportsParameter(cookieParameter));
		assertTrue("Cookie string parameter not supported", resolver.supportsParameter(cookieStringParameter));
		assertFalse("non-@CookieValue parameter supported", resolver.supportsParameter(otherParameter));
	}

	@Test
	public void resolveCookieArgument() throws Exception {
		Cookie expected = new Cookie("name", "foo");
		servletRequest.setCookies(expected);

		Cookie result = (Cookie) resolver.resolveArgument(cookieParameter, null, webRequest, null);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveCookieStringArgument() throws Exception {
		Cookie cookie = new Cookie("name", "foo");
		servletRequest.setCookies(cookie);

		String result = (String) resolver.resolveArgument(cookieStringParameter, null, webRequest, null);
		assertEquals("Invalid result", cookie.getValue(), result);
	}

	@Test
	public void resolveCookieDefaultValue() throws Exception {
		String result = (String) resolver.resolveArgument(cookieStringParameter, null, webRequest, null);
		assertEquals("Invalid result", "bar", result);
	}

	@Test(expected = IllegalStateException.class)
	public void notFound() throws Exception {
		String result = (String) resolver.resolveArgument(cookieParameter, null, webRequest, null);
		assertEquals("Invalid result", "bar", result);
	}

	public void params(@CookieValue("name") Cookie cookie,
								@CookieValue(value = "name", defaultValue = "bar") String cookieString,
								String unsupported) {

	}


}
