/*
 * Copyright 2002-2018 the original author or authors.
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

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link UriComponentsBuilderMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class UriComponentsBuilderMethodArgumentResolverTests {

	private UriComponentsBuilderMethodArgumentResolver resolver;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest servletRequest;

	private MethodParameter builderParam;
	private MethodParameter servletBuilderParam;
	private MethodParameter intParam;


	@Before
	public void setup() throws Exception {
		this.resolver = new UriComponentsBuilderMethodArgumentResolver();
		this.servletRequest = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(this.servletRequest);

		Method method = this.getClass().getDeclaredMethod(
				"handle", UriComponentsBuilder.class, ServletUriComponentsBuilder.class, int.class);
		this.builderParam = new MethodParameter(method, 0);
		this.servletBuilderParam = new MethodParameter(method, 1);
		this.intParam = new MethodParameter(method, 2);
	}


	@Test
	public void supportsParameter() throws Exception {
		assertTrue(this.resolver.supportsParameter(this.builderParam));
		assertTrue(this.resolver.supportsParameter(this.servletBuilderParam));
		assertFalse(this.resolver.supportsParameter(this.intParam));
	}

	@Test
	public void resolveArgument() throws Exception {
		this.servletRequest.setContextPath("/myapp");
		this.servletRequest.setServletPath("/main");
		this.servletRequest.setPathInfo("/accounts");

		Object actual = this.resolver.resolveArgument(this.builderParam, new ModelAndViewContainer(), this.webRequest, null);

		assertNotNull(actual);
		assertEquals(ServletUriComponentsBuilder.class, actual.getClass());
		assertEquals("http://localhost/myapp/main", ((ServletUriComponentsBuilder) actual).build().toUriString());
	}


	void handle(UriComponentsBuilder builder, ServletUriComponentsBuilder servletBuilder, int value) {
	}

}
