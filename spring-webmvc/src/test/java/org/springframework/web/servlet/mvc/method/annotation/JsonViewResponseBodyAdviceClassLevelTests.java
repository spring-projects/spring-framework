/*
 * Copyright 2002-present the original author or authors.
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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for class-level {@code @JsonView} support in {@link JsonViewResponseBodyAdvice}.
 *
 * @author Asif Ebrahim  
 * @since 7.0
 */
class JsonViewResponseBodyAdviceClassLevelTests {

	private JsonViewResponseBodyAdvice advice;

	private ServerHttpRequest request;

	private ServerHttpResponse response;


	@BeforeEach
	void setup() {
		this.advice = new JsonViewResponseBodyAdvice();
		this.request = new ServletServerHttpRequest(new MockHttpServletRequest());
		this.response = new ServletServerHttpResponse(new MockHttpServletResponse());
	}


	@Test
	void supportsWithClassLevelJsonView() throws Exception {
		Method method = ClassLevelJsonViewController.class.getDeclaredMethod("methodWithoutAnnotation");
		MethodParameter returnType = new MethodParameter(method, -1);

		assertThat(this.advice.supports(returnType, MappingJackson2HttpMessageConverter.class)).isTrue();
		assertThat(this.advice.supports(returnType, JacksonJsonHttpMessageConverter.class)).isTrue();
	}

	@Test
	void supportsWithMethodLevelJsonView() throws Exception {
		Method method = RegularController.class.getDeclaredMethod("methodWithJsonView");
		MethodParameter returnType = new MethodParameter(method, -1);

		assertThat(this.advice.supports(returnType, MappingJackson2HttpMessageConverter.class)).isTrue();
		assertThat(this.advice.supports(returnType, JacksonJsonHttpMessageConverter.class)).isTrue();
	}

	@Test
	void doesNotSupportWithoutJsonView() throws Exception {
		Method method = RegularController.class.getDeclaredMethod("methodWithoutAnnotation");
		MethodParameter returnType = new MethodParameter(method, -1);

		assertThat(this.advice.supports(returnType, MappingJackson2HttpMessageConverter.class)).isFalse();
		assertThat(this.advice.supports(returnType, JacksonJsonHttpMessageConverter.class)).isFalse();
	}

	@Test
	void beforeBodyWriteWithClassLevelJsonView() throws Exception {
		Method method = ClassLevelJsonViewController.class.getDeclaredMethod("methodWithoutAnnotation");
		MethodParameter returnType = new MethodParameter(method, -1);

		MappingJacksonValue container = new MappingJacksonValue(new Object());
		this.advice.beforeBodyWriteInternal(container, MediaType.APPLICATION_JSON, returnType, this.request, this.response);

		assertThat(container.getSerializationView()).isEqualTo(MyJsonView.class);
	}

	@Test
	void beforeBodyWriteWithMethodLevelJsonView() throws Exception {
		Method method = RegularController.class.getDeclaredMethod("methodWithJsonView");
		MethodParameter returnType = new MethodParameter(method, -1);

		MappingJacksonValue container = new MappingJacksonValue(new Object());
		this.advice.beforeBodyWriteInternal(container, MediaType.APPLICATION_JSON, returnType, this.request, this.response);

		assertThat(container.getSerializationView()).isEqualTo(MyJsonView.class);
	}

	@Test
	void methodLevelAnnotationTakesPrecedenceOverClassLevel() throws Exception {
		Method method = ClassLevelJsonViewController.class.getDeclaredMethod("methodWithDifferentJsonView");
		MethodParameter returnType = new MethodParameter(method, -1);

		MappingJacksonValue container = new MappingJacksonValue(new Object());
		this.advice.beforeBodyWriteInternal(container, MediaType.APPLICATION_JSON, returnType, this.request, this.response);

		// Method-level annotation should take precedence
		assertThat(container.getSerializationView()).isEqualTo(AnotherJsonView.class);
	}

	@Test
	void determineWriteHintsWithClassLevelJsonView() throws Exception {
		Method method = ClassLevelJsonViewController.class.getDeclaredMethod("methodWithoutAnnotation");
		MethodParameter returnType = new MethodParameter(method, -1);

		var hints = this.advice.determineWriteHints(new Object(), returnType, MediaType.APPLICATION_JSON, MappingJackson2HttpMessageConverter.class);

		assertThat(hints).containsEntry(JsonView.class.getName(), MyJsonView.class);
	}


	// Test interfaces for JsonView
	private interface MyJsonView {}

	private interface AnotherJsonView {}

	// Test controller with class-level @JsonView
	@JsonView(MyJsonView.class)
	private static class ClassLevelJsonViewController {

		@RequestMapping
		@ResponseBody
		public String methodWithoutAnnotation() {
			return "test";
		}

		@RequestMapping
		@ResponseBody
		@JsonView(AnotherJsonView.class)
		public String methodWithDifferentJsonView() {
			return "test";
		}
	}

	// Test controller without class-level @JsonView
	private static class RegularController {

		@RequestMapping
		@ResponseBody
		@JsonView(MyJsonView.class)
		public String methodWithJsonView() {
			return "test";
		}

		@RequestMapping
		@ResponseBody
		public String methodWithoutAnnotation() {
			return "test";
		}
	}

}
