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

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Locale;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestMethodArgumentResolver;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link ServletRequestMethodArgumentResolver}.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class ServletRequestMethodArgumentResolverTests {

	private final ServletRequestMethodArgumentResolver resolver = new ServletRequestMethodArgumentResolver();

	private Method method;

	private ModelAndViewContainer mavContainer;
	
	private ServletWebRequest webRequest;

	private MockHttpServletRequest servletRequest;

	@Before
	public void setUp() throws Exception {
		method = getClass().getMethod("supportedParams", ServletRequest.class, MultipartRequest.class,
				HttpSession.class, Principal.class, Locale.class, InputStream.class, Reader.class, WebRequest.class);
		mavContainer = new ModelAndViewContainer();
		servletRequest = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(servletRequest, new MockHttpServletResponse());
	}

	@Test
	public void servletRequest() throws Exception {
		MethodParameter servletRequestParameter = new MethodParameter(method, 0);
		
		boolean isSupported = resolver.supportsParameter(servletRequestParameter);
		Object result = resolver.resolveArgument(servletRequestParameter, mavContainer, webRequest, null);
		
		assertTrue("ServletRequest not supported", isSupported);
		assertSame("Invalid result", servletRequest, result);
		assertFalse("The requestHandled flag shouldn't change", mavContainer.isRequestHandled());
	}

	@Test
	public void session() throws Exception {
		MockHttpSession session = new MockHttpSession();
		servletRequest.setSession(session);
		MethodParameter sessionParameter = new MethodParameter(method, 2);

		boolean isSupported = resolver.supportsParameter(sessionParameter);
		Object result = resolver.resolveArgument(sessionParameter, mavContainer, webRequest, null);

		assertTrue("Session not supported", isSupported);
		assertSame("Invalid result", session, result);
		assertFalse("The requestHandled flag shouldn't change", mavContainer.isRequestHandled());
	}

	@Test
	public void principal() throws Exception {
		Principal principal = new Principal() {
			public String getName() {
				return "Foo";
			}
		};
		servletRequest.setUserPrincipal(principal);
		MethodParameter principalParameter = new MethodParameter(method, 3);

		assertTrue("Principal not supported", resolver.supportsParameter(principalParameter));

		Object result = resolver.resolveArgument(principalParameter, null, webRequest, null);
		assertSame("Invalid result", principal, result);
	}

	@Test
	public void locale() throws Exception {
		Locale locale = Locale.ENGLISH;
		servletRequest.addPreferredLocale(locale);
		MethodParameter localeParameter = new MethodParameter(method, 4);

		assertTrue("Locale not supported", resolver.supportsParameter(localeParameter));

		Object result = resolver.resolveArgument(localeParameter, null, webRequest, null);
		assertSame("Invalid result", locale, result);
	}

	@Test
	public void inputStream() throws Exception {
		MethodParameter inputStreamParameter = new MethodParameter(method, 5);

		assertTrue("InputStream not supported", resolver.supportsParameter(inputStreamParameter));

		Object result = resolver.resolveArgument(inputStreamParameter, null, webRequest, null);
		assertSame("Invalid result", webRequest.getRequest().getInputStream(), result);
	}

	@Test
	public void reader() throws Exception {
		MethodParameter readerParameter = new MethodParameter(method, 6);

		assertTrue("Reader not supported", resolver.supportsParameter(readerParameter));

		Object result = resolver.resolveArgument(readerParameter, null, webRequest, null);
		assertSame("Invalid result", webRequest.getRequest().getReader(), result);
	}

	@Test
	public void webRequest() throws Exception {
		MethodParameter webRequestParameter = new MethodParameter(method, 7);

		assertTrue("WebRequest not supported", resolver.supportsParameter(webRequestParameter));

		Object result = resolver.resolveArgument(webRequestParameter, null, webRequest, null);
		assertSame("Invalid result", webRequest, result);
	}

	public void supportedParams(ServletRequest p0,
								MultipartRequest p1,
								HttpSession p2,
								Principal p3,
								Locale p4,
								InputStream p5,
								Reader p6,
								WebRequest p7) {
	}

}