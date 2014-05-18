/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.ControllerAdviceBean;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for
 * {@link org.springframework.web.servlet.mvc.method.annotation.ResponseBodyInterceptorChain}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class ResponseBodyInterceptorChainTests {

	private String body;

	private MediaType contentType;

	private Class<? extends HttpMessageConverter<String>> converterType;

	private MethodParameter returnType;

	private ServerHttpRequest request;

	private ServerHttpResponse response;


	@Before
	public void setup() {
		this.body = "body";
		this.contentType = MediaType.TEXT_PLAIN;
		this.converterType = StringHttpMessageConverter.class;
		this.returnType = new MethodParameter(ClassUtils.getMethod(this.getClass(), "handle"), -1);
		this.request = new ServletServerHttpRequest(new MockHttpServletRequest());
		this.response = new ServletServerHttpResponse(new MockHttpServletResponse());
	}

	@Test
	public void responseBodyInterceptor() {

		ResponseBodyInterceptor interceptor = Mockito.mock(ResponseBodyInterceptor.class);
		ResponseBodyInterceptorChain chain = new ResponseBodyInterceptorChain(Arrays.asList(interceptor));

		String expected = "body++";
		when(interceptor.beforeBodyWrite(
				eq(this.body), eq(this.contentType), eq(this.converterType), eq(this.returnType),
				same(this.request), same(this.response))).thenReturn(expected);

		String actual = chain.invoke(this.body, this.contentType,
				this.converterType, this.returnType, this.request, this.response);

		assertEquals(expected, actual);
	}

	@Test
	public void controllerAdvice() {

		Object interceptor = new ControllerAdviceBean(new MyControllerAdvice());
		ResponseBodyInterceptorChain chain = new ResponseBodyInterceptorChain(Arrays.asList(interceptor));

		String actual = chain.invoke(this.body, this.contentType,
				this.converterType, this.returnType, this.request, this.response);

		assertEquals("body-MyControllerAdvice", actual);
	}

	@Test
	public void controllerAdviceNotApplicable() {

		Object interceptor = new ControllerAdviceBean(new TargetedControllerAdvice());
		ResponseBodyInterceptorChain chain = new ResponseBodyInterceptorChain(Arrays.asList(interceptor));

		String actual = chain.invoke(this.body, this.contentType,
				this.converterType, this.returnType, this.request, this.response);

		assertEquals(this.body, actual);
	}


	@ControllerAdvice
	private static class MyControllerAdvice implements ResponseBodyInterceptor {

		@SuppressWarnings("unchecked")
		@Override
		public <T> T beforeBodyWrite(T body, MediaType contentType,
				Class<? extends HttpMessageConverter<T>> converterType,
				MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {

			return (T) (body + "-MyControllerAdvice");
		}
	}

	@ControllerAdvice(annotations = Controller.class)
	private static class TargetedControllerAdvice implements ResponseBodyInterceptor {

		@SuppressWarnings("unchecked")
		@Override
		public <T> T beforeBodyWrite(T body, MediaType contentType,
				Class<? extends HttpMessageConverter<T>> converterType,
				MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {

			return (T) (body + "-TargetedControllerAdvice");
		}
	}


	@SuppressWarnings("unused")
	@ResponseBody
	public String handle() {
		return "";
	}
}
