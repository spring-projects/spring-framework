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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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


	@BeforeEach
	public void setUp() throws Exception {
		resolver = new TestCookieValueMethodArgumentResolver();

		Method method = getClass().getMethod("params", Cookie.class, String.class, String.class);
		paramNamedCookie = new SynthesizingMethodParameter(method, 0);
		paramNamedDefaultValueString = new SynthesizingMethodParameter(method, 1);
		paramString = new SynthesizingMethodParameter(method, 2);

		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());
	}


	@Test
	public void supportsParameter() {
		assertThat(resolver.supportsParameter(paramNamedCookie)).as("Cookie parameter not supported").isTrue();
		assertThat(resolver.supportsParameter(paramNamedDefaultValueString)).as("Cookie string parameter not supported").isTrue();
		assertThat(resolver.supportsParameter(paramString)).as("non-@CookieValue parameter supported").isFalse();
	}

	@Test
	public void resolveCookieDefaultValue() throws Exception {
		Object result = resolver.resolveArgument(paramNamedDefaultValueString, null, webRequest, null);

		boolean condition = result instanceof String;
		assertThat(condition).isTrue();
		assertThat(result).as("Invalid result").isEqualTo("bar");
	}

	@Test
	public void notFound() throws Exception {
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
			resolver.resolveArgument(paramNamedCookie, null, webRequest, null));
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
			@CookieValue(name = "name", defaultValue = "bar") String param2,
			String param3) {
	}

}
