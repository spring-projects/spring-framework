/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Arrays;

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

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for
 * {@link ResponseBodyAdviceChain}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class ResponseBodyAdviceChainTests {

	private String body;

	private MediaType contentType;

	private Class<? extends HttpMessageConverter<?>> converterType;

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
	public void responseBodyAdvice() {

		@SuppressWarnings("unchecked")
		ResponseBodyAdvice<String> advice = Mockito.mock(ResponseBodyAdvice.class);
		ResponseBodyAdviceChain chain = new ResponseBodyAdviceChain(Arrays.asList(advice));

		String expected = "body++";
		given(advice.supports(this.returnType, this.converterType)).willReturn(true);
		given(advice.beforeBodyWrite(eq(this.body), eq(this.returnType), eq(this.contentType),
				eq(this.converterType), same(this.request), same(this.response))).willReturn(expected);

		String actual = chain.invoke(this.body, this.returnType,
				this.contentType, this.converterType, this.request, this.response);

		assertEquals(expected, actual);
	}

	@Test
	public void controllerAdvice() {

		Object adviceBean = new ControllerAdviceBean(new MyControllerAdvice());
		ResponseBodyAdviceChain chain = new ResponseBodyAdviceChain(Arrays.asList(adviceBean));

		String actual = chain.invoke(this.body, this.returnType,
				this.contentType, this.converterType, this.request, this.response);

		assertEquals("body-MyControllerAdvice", actual);
	}

	@Test
	public void controllerAdviceNotApplicable() {

		Object adviceBean = new ControllerAdviceBean(new TargetedControllerAdvice());
		ResponseBodyAdviceChain chain = new ResponseBodyAdviceChain(Arrays.asList(adviceBean));

		String actual = chain.invoke(this.body, this.returnType,
				this.contentType, this.converterType, this.request, this.response);

		assertEquals(this.body, actual);
	}


	@ControllerAdvice
	private static class MyControllerAdvice implements ResponseBodyAdvice<String> {

		@Override
		public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		public String beforeBodyWrite(String body, MethodParameter returnType,
				MediaType contentType, Class<? extends HttpMessageConverter<?>> converterType,
				ServerHttpRequest request, ServerHttpResponse response) {

			return body + "-MyControllerAdvice";
		}
	}

	@ControllerAdvice(annotations = Controller.class)
	private static class TargetedControllerAdvice implements ResponseBodyAdvice<String> {

		@Override
		public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		public String beforeBodyWrite(String body, MethodParameter returnType,
				MediaType contentType, Class<? extends HttpMessageConverter<?>> converterType,
				ServerHttpRequest request, ServerHttpResponse response) {

			return body + "-TargetedControllerAdvice";
		}
	}


	@SuppressWarnings("unused")
	@ResponseBody
	public String handle() {
		return "";
	}
}
