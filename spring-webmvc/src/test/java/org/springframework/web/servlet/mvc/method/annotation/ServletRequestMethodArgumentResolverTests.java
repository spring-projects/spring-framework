/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.security.Principal;
import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.PushBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockHttpSession;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartRequest;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Nicholas Williams
 */
public class ServletRequestMethodArgumentResolverTests {

	private ServletRequestMethodArgumentResolver resolver;

	private ModelAndViewContainer mavContainer;

	private MockHttpServletRequest servletRequest;

	private ServletWebRequest webRequest;

	private Method method;


	@BeforeEach
	public void setup() throws Exception {
		resolver = new ServletRequestMethodArgumentResolver();
		mavContainer = new ModelAndViewContainer();
		servletRequest = new MockHttpServletRequest("GET", "");
		webRequest = new ServletWebRequest(servletRequest, new MockHttpServletResponse());

		method = getClass().getMethod("supportedParams", ServletRequest.class, MultipartRequest.class,
				HttpSession.class, Principal.class, Locale.class, InputStream.class, Reader.class,
				WebRequest.class, TimeZone.class, ZoneId.class, HttpMethod.class, PushBuilder.class);
	}


	@Test
	public void servletRequest() throws Exception {
		MethodParameter servletRequestParameter = new MethodParameter(method, 0);
		assertThat(resolver.supportsParameter(servletRequestParameter)).as("ServletRequest not supported").isTrue();

		Object result = resolver.resolveArgument(servletRequestParameter, mavContainer, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(servletRequest);
		assertThat(mavContainer.isRequestHandled()).as("The requestHandled flag shouldn't change").isFalse();
	}

	@Test
	public void session() throws Exception {
		MockHttpSession session = new MockHttpSession();
		servletRequest.setSession(session);

		MethodParameter sessionParameter = new MethodParameter(method, 2);
		assertThat(resolver.supportsParameter(sessionParameter)).as("Session not supported").isTrue();

		Object result = resolver.resolveArgument(sessionParameter, mavContainer, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(session);
		assertThat(mavContainer.isRequestHandled()).as("The requestHandled flag shouldn't change").isFalse();
	}

	@Test
	public void principal() throws Exception {
		Principal principal = () -> "Foo";
		servletRequest.setUserPrincipal(principal);

		MethodParameter principalParameter = new MethodParameter(method, 3);
		assertThat(resolver.supportsParameter(principalParameter)).as("Principal not supported").isTrue();

		Object result = resolver.resolveArgument(principalParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(principal);
	}

	@Test
	public void principalAsNull() throws Exception {
		MethodParameter principalParameter = new MethodParameter(method, 3);
		assertThat(resolver.supportsParameter(principalParameter)).as("Principal not supported").isTrue();

		Object result = resolver.resolveArgument(principalParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isNull();
	}

	@Test
	public void locale() throws Exception {
		Locale locale = Locale.ENGLISH;
		servletRequest.addPreferredLocale(locale);

		MethodParameter localeParameter = new MethodParameter(method, 4);
		assertThat(resolver.supportsParameter(localeParameter)).as("Locale not supported").isTrue();

		Object result = resolver.resolveArgument(localeParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(locale);
	}

	@Test
	public void localeFromResolver() throws Exception {
		Locale locale = Locale.ENGLISH;
		servletRequest.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,
				new FixedLocaleResolver(locale));

		MethodParameter localeParameter = new MethodParameter(method, 4);
		assertThat(resolver.supportsParameter(localeParameter)).as("Locale not supported").isTrue();

		Object result = resolver.resolveArgument(localeParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(locale);
	}

	@Test
	public void timeZone() throws Exception {
		MethodParameter timeZoneParameter = new MethodParameter(method, 8);
		assertThat(resolver.supportsParameter(timeZoneParameter)).as("TimeZone not supported").isTrue();

		Object result = resolver.resolveArgument(timeZoneParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isEqualTo(TimeZone.getDefault());
	}

	@Test
	public void timeZoneFromResolver() throws Exception {
		TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
		servletRequest.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,
				new FixedLocaleResolver(Locale.US, timeZone));

		MethodParameter timeZoneParameter = new MethodParameter(method, 8);
		assertThat(resolver.supportsParameter(timeZoneParameter)).as("TimeZone not supported").isTrue();

		Object result = resolver.resolveArgument(timeZoneParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isEqualTo(timeZone);
	}

	@Test
	public void zoneId() throws Exception {
		MethodParameter zoneIdParameter = new MethodParameter(method, 9);
		assertThat(resolver.supportsParameter(zoneIdParameter)).as("ZoneId not supported").isTrue();

		Object result = resolver.resolveArgument(zoneIdParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isEqualTo(ZoneId.systemDefault());
	}

	@Test
	public void zoneIdFromResolver() throws Exception {
		TimeZone timeZone = TimeZone.getTimeZone("America/New_York");
		servletRequest.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE,
				new FixedLocaleResolver(Locale.US, timeZone));
		MethodParameter zoneIdParameter = new MethodParameter(method, 9);

		assertThat(resolver.supportsParameter(zoneIdParameter)).as("ZoneId not supported").isTrue();

		Object result = resolver.resolveArgument(zoneIdParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isEqualTo(timeZone.toZoneId());
	}

	@Test
	public void inputStream() throws Exception {
		MethodParameter inputStreamParameter = new MethodParameter(method, 5);
		assertThat(resolver.supportsParameter(inputStreamParameter)).as("InputStream not supported").isTrue();

		Object result = resolver.resolveArgument(inputStreamParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(webRequest.getRequest().getInputStream());
	}

	@Test
	public void reader() throws Exception {
		MethodParameter readerParameter = new MethodParameter(method, 6);
		assertThat(resolver.supportsParameter(readerParameter)).as("Reader not supported").isTrue();

		Object result = resolver.resolveArgument(readerParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(webRequest.getRequest().getReader());
	}

	@Test
	public void webRequest() throws Exception {
		MethodParameter webRequestParameter = new MethodParameter(method, 7);
		assertThat(resolver.supportsParameter(webRequestParameter)).as("WebRequest not supported").isTrue();

		Object result = resolver.resolveArgument(webRequestParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(webRequest);
	}

	@Test
	public void httpMethod() throws Exception {
		MethodParameter httpMethodParameter = new MethodParameter(method, 10);
		assertThat(resolver.supportsParameter(httpMethodParameter)).as("HttpMethod not supported").isTrue();

		Object result = resolver.resolveArgument(httpMethodParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(HttpMethod.valueOf(webRequest.getRequest().getMethod()));
	}

	@Test
	public void pushBuilder() throws Exception {
		final PushBuilder pushBuilder = Mockito.mock(PushBuilder.class);
		servletRequest = new MockHttpServletRequest("GET", "") {
			@Override
			public PushBuilder newPushBuilder() {
				return pushBuilder;
			}
		};
		ServletWebRequest webRequest = new ServletWebRequest(servletRequest, new MockHttpServletResponse());

		MethodParameter pushBuilderParameter = new MethodParameter(method, 11);
		assertThat(resolver.supportsParameter(pushBuilderParameter)).as("PushBuilder not supported").isTrue();

		Object result = resolver.resolveArgument(pushBuilderParameter, null, webRequest, null);
		assertThat(result).as("Invalid result").isSameAs(pushBuilder);
	}


	@SuppressWarnings("unused")
	public void supportedParams(ServletRequest p0,
								MultipartRequest p1,
								HttpSession p2,
								Principal p3,
								Locale p4,
								InputStream p5,
								Reader p6,
								WebRequest p7,
								TimeZone p8,
								ZoneId p9,
								HttpMethod p10,
								PushBuilder p11) {
	}

}
