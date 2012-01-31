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

package org.springframework.web.servlet.mvc.method.annotation;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ServletCookieValueMethodArgumentResolver;

/**
 * Test fixture with {@link ServletCookieValueMethodArgumentResolver}.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class ServletCookieValueMethodArgumentResolverTests {

	private ServletCookieValueMethodArgumentResolver resolver;

	private MethodParameter cookieParameter;

	private MethodParameter cookieStringParameter;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest request;

	@Before
	public void setUp() throws Exception {
		resolver = new ServletCookieValueMethodArgumentResolver(null);
		
		Method method = getClass().getMethod("params", Cookie.class, String.class);
		cookieParameter = new MethodParameter(method, 0);
		cookieStringParameter = new MethodParameter(method, 1);

		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());
	}

	@Test
	public void resolveCookieArgument() throws Exception {
		Cookie expected = new Cookie("name", "foo");
		request.setCookies(expected);

		Cookie result = (Cookie) resolver.resolveArgument(cookieParameter, null, webRequest, null);
		assertEquals("Invalid result", expected, result);
	}

	@Test
	public void resolveCookieStringArgument() throws Exception {
		Cookie cookie = new Cookie("name", "foo");
		request.setCookies(cookie);

		String result = (String) resolver.resolveArgument(cookieStringParameter, null, webRequest, null);
		assertEquals("Invalid result", cookie.getValue(), result);
	}

	public void params(@CookieValue("name") Cookie cookie,
					   @CookieValue(value = "name", defaultValue = "bar") String cookieString) {
	}

}