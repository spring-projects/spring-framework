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

package org.springframework.web.servlet.mvc.method.annotation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Test fixture with {@link HttpEntityMethodProcessor} delegating to
 * actual {@link HttpMessageConverter} instances.
 *
 * <p>Also see {@link HttpEntityMethodProcessorMockTests}.
 *
 * @author Rossen Stoyanchev
 */
public class HttpEntityMethodProcessorTests {

	private MethodParameter paramList;
	private MethodParameter paramSimpleBean;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MockHttpServletResponse servletResponse;

	private MockHttpServletRequest servletRequest;

	@Before
	public void setUp() throws Exception {

		Method method = getClass().getMethod("handle", HttpEntity.class, HttpEntity.class);
		paramList = new MethodParameter(method, 0);
		paramSimpleBean = new MethodParameter(method, 1);

		mavContainer = new ModelAndViewContainer();

		servletRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(servletRequest, servletResponse);
	}

	@Test
	public void resolveArgument() throws Exception {
		String content = "{\"name\" : \"Jad\"}";
		this.servletRequest.setContent(content.getBytes("UTF-8"));
		this.servletRequest.setContentType("application/json");

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new MappingJackson2HttpMessageConverter());
		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters);

		@SuppressWarnings("unchecked")
		HttpEntity<SimpleBean> result = (HttpEntity<SimpleBean>) processor.resolveArgument(
				paramSimpleBean, mavContainer, webRequest, new ValidatingBinderFactory());

		assertNotNull(result);
		assertEquals("Jad", result.getBody().getName());
	}

	@Test
	public void resolveGenericArgument() throws Exception {
		String content = "[{\"name\" : \"Jad\"}, {\"name\" : \"Robert\"}]";
		this.servletRequest.setContent(content.getBytes("UTF-8"));
		this.servletRequest.setContentType("application/json");

		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(new MappingJackson2HttpMessageConverter());
		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters);

		@SuppressWarnings("unchecked")
		HttpEntity<List<SimpleBean>> result = (HttpEntity<List<SimpleBean>>) processor.resolveArgument(
				paramList, mavContainer, webRequest, new ValidatingBinderFactory());

		assertNotNull(result);
		assertEquals("Jad", result.getBody().get(0).getName());
		assertEquals("Robert", result.getBody().get(1).getName());
	}


	public void handle(HttpEntity<List<SimpleBean>> arg1, HttpEntity<SimpleBean> arg2) {
	}


	private static class SimpleBean {

		private String name;

		public String getName() {
			return name;
		}

		@SuppressWarnings("unused")
		public void setName(String name) {
			this.name = name;
		}
	}

	private final class ValidatingBinderFactory implements WebDataBinderFactory {
		public WebDataBinder createBinder(NativeWebRequest webRequest, Object target, String objectName) throws Exception {
			LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
			validator.afterPropertiesSet();
			WebDataBinder dataBinder = new WebDataBinder(target, objectName);
			dataBinder.setValidator(validator);
			return dataBinder;
		}
	}

}