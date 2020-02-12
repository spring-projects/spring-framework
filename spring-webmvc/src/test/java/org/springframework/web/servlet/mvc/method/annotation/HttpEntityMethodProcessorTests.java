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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link HttpEntityMethodProcessor} delegating to
 * actual {@link HttpMessageConverter} instances.
 *
 * <p>Also see {@link HttpEntityMethodProcessorMockTests}.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("unused")
public class HttpEntityMethodProcessorTests {

	private MethodParameter paramList;

	private MethodParameter paramSimpleBean;

	private ModelAndViewContainer mavContainer;

	private WebDataBinderFactory binderFactory;

	private MockHttpServletRequest servletRequest;

	private ServletWebRequest webRequest;

	private MockHttpServletResponse servletResponse;


	@BeforeEach
	public void setup() throws Exception {
		Method method = getClass().getDeclaredMethod("handle", HttpEntity.class, HttpEntity.class);
		paramList = new MethodParameter(method, 0);
		paramSimpleBean = new MethodParameter(method, 1);

		mavContainer = new ModelAndViewContainer();
		binderFactory = new ValidatingBinderFactory();
		servletRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		servletRequest.setMethod("POST");
		webRequest = new ServletWebRequest(servletRequest, servletResponse);
	}


	@Test
	public void resolveArgument() throws Exception {
		String content = "{\"name\" : \"Jad\"}";
		this.servletRequest.setContent(content.getBytes("UTF-8"));
		this.servletRequest.setContentType("application/json");

		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new MappingJackson2HttpMessageConverter());
		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters);

		@SuppressWarnings("unchecked")
		HttpEntity<SimpleBean> result = (HttpEntity<SimpleBean>) processor.resolveArgument(
				paramSimpleBean, mavContainer, webRequest, binderFactory);

		assertThat(result).isNotNull();
		assertThat(result.getBody().getName()).isEqualTo("Jad");
	}

	@Test  // SPR-12861
	public void resolveArgumentWithEmptyBody() throws Exception {
		this.servletRequest.setContent(new byte[0]);
		this.servletRequest.setContentType("application/json");

		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new MappingJackson2HttpMessageConverter());
		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters);

		HttpEntity<?> result = (HttpEntity<?>) processor.resolveArgument(this.paramSimpleBean,
				this.mavContainer, this.webRequest, this.binderFactory);

		assertThat(result).isNotNull();
		assertThat(result.getBody()).isNull();
	}

	@Test
	public void resolveGenericArgument() throws Exception {
		String content = "[{\"name\" : \"Jad\"}, {\"name\" : \"Robert\"}]";
		this.servletRequest.setContent(content.getBytes("UTF-8"));
		this.servletRequest.setContentType("application/json");

		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new MappingJackson2HttpMessageConverter());
		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters);

		@SuppressWarnings("unchecked")
		HttpEntity<List<SimpleBean>> result = (HttpEntity<List<SimpleBean>>) processor.resolveArgument(
				paramList, mavContainer, webRequest, binderFactory);

		assertThat(result).isNotNull();
		assertThat(result.getBody().get(0).getName()).isEqualTo("Jad");
		assertThat(result.getBody().get(1).getName()).isEqualTo("Robert");
	}

	@Test
	public void resolveArgumentTypeVariable() throws Exception {
		Method method = MySimpleParameterizedController.class.getMethod("handleDto", HttpEntity.class);
		HandlerMethod handlerMethod = new HandlerMethod(new MySimpleParameterizedController(), method);
		MethodParameter methodParam = handlerMethod.getMethodParameters()[0];

		String content = "{\"name\" : \"Jad\"}";
		this.servletRequest.setContent(content.getBytes("UTF-8"));
		this.servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);

		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new MappingJackson2HttpMessageConverter());
		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters);

		@SuppressWarnings("unchecked")
		HttpEntity<SimpleBean> result = (HttpEntity<SimpleBean>)
				processor.resolveArgument(methodParam, mavContainer, webRequest, binderFactory);

		assertThat(result).isNotNull();
		assertThat(result.getBody().getName()).isEqualTo("Jad");
	}

	@Test  // SPR-12811
	public void jacksonTypeInfoList() throws Exception {
		Method method = JacksonController.class.getMethod("handleList");
		HandlerMethod handlerMethod = new HandlerMethod(new JacksonController(), method);
		MethodParameter methodReturnType = handlerMethod.getReturnType();

		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(new MappingJackson2HttpMessageConverter());
		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters);

		Object returnValue = new JacksonController().handleList();
		processor.handleReturnValue(returnValue, methodReturnType, this.mavContainer, this.webRequest);

		String content = this.servletResponse.getContentAsString();
		assertThat(content.contains("\"type\":\"foo\"")).isTrue();
		assertThat(content.contains("\"type\":\"bar\"")).isTrue();
	}

	@Test  // SPR-13423
	public void handleReturnValueCharSequence() throws Exception {
		List<HttpMessageConverter<?>>converters = new ArrayList<>();
		converters.add(new ByteArrayHttpMessageConverter());
		converters.add(new StringHttpMessageConverter());

		Method method = getClass().getDeclaredMethod("handle");
		MethodParameter returnType = new MethodParameter(method, -1);
		ResponseEntity<StringBuilder> returnValue = ResponseEntity.ok(new StringBuilder("Foo"));

		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(converters);
		processor.handleReturnValue(returnValue, returnType, mavContainer, webRequest);

		assertThat(servletResponse.getHeader("Content-Type")).isEqualTo("text/plain;charset=ISO-8859-1");
		assertThat(servletResponse.getContentAsString()).isEqualTo("Foo");
	}


	@SuppressWarnings("unused")
	private void handle(HttpEntity<List<SimpleBean>> arg1, HttpEntity<SimpleBean> arg2) {
	}

	private ResponseEntity<CharSequence> handle() {
		return null;
	}


	@SuppressWarnings("unused")
	private static abstract class MyParameterizedController<DTO extends Identifiable> {

		public void handleDto(HttpEntity<DTO> dto) {
		}
	}


	@SuppressWarnings("unused")
	private static class MySimpleParameterizedController extends MyParameterizedController<SimpleBean> {
	}


	private interface Identifiable extends Serializable {

		Long getId();

		void setId(Long id);
	}


	@SuppressWarnings({ "serial" })
	private static class SimpleBean implements Identifiable {

		private Long id;

		private String name;

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		@SuppressWarnings("unused")
		public void setName(String name) {
			this.name = name;
		}
	}


	private final class ValidatingBinderFactory implements WebDataBinderFactory {

		@Override
		public WebDataBinder createBinder(NativeWebRequest webRequest, @Nullable Object target, String objectName) {
			LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
			validator.afterPropertiesSet();
			WebDataBinder dataBinder = new WebDataBinder(target, objectName);
			dataBinder.setValidator(validator);
			return dataBinder;
		}
	}


	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	private static class ParentClass {

		private String parentProperty;

		public ParentClass() {
		}

		public ParentClass(String parentProperty) {
			this.parentProperty = parentProperty;
		}

		public String getParentProperty() {
			return parentProperty;
		}

		public void setParentProperty(String parentProperty) {
			this.parentProperty = parentProperty;
		}
	}


	@JsonTypeName("foo")
	private static class Foo extends ParentClass {

		public Foo() {
		}

		public Foo(String parentProperty) {
			super(parentProperty);
		}
	}


	@JsonTypeName("bar")
	private static class Bar extends ParentClass {

		public Bar() {
		}

		public Bar(String parentProperty) {
			super(parentProperty);
		}
	}


	private static class JacksonController {

		@RequestMapping
		@ResponseBody
		public HttpEntity<List<ParentClass>> handleList() {
			List<ParentClass> list = new ArrayList<>();
			list.add(new Foo("foo"));
			list.add(new Bar("bar"));
			return new HttpEntity<>(list);
		}
	}

}
