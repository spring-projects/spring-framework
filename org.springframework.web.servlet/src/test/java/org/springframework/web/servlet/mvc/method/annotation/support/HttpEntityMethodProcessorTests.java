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

package org.springframework.web.servlet.mvc.method.annotation.support;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.support.HttpEntityMethodProcessor;

/**
 * @author Arjen Poutsma
 */
public class HttpEntityMethodProcessorTests {

	private HttpEntityMethodProcessor processor;

	private HttpMessageConverter<String> messageConverter;

	private MethodParameter httpEntityParam;

	private MethodParameter responseEntityReturnValue;

	private MethodParameter responseEntityParameter;

	private MethodParameter intReturnValue;

	private MethodParameter httpEntityReturnValue;

	private MethodParameter intParameter;

	private ModelAndViewContainer mavContainer;
	
	private ServletWebRequest request;

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		messageConverter = createMock(HttpMessageConverter.class);
		processor = new HttpEntityMethodProcessor(messageConverter);

		Method handle1 = getClass().getMethod("handle1", HttpEntity.class, ResponseEntity.class, Integer.TYPE);
		httpEntityParam = new MethodParameter(handle1, 0);
		responseEntityParameter = new MethodParameter(handle1, 1);
		intParameter = new MethodParameter(handle1, 2);
		responseEntityReturnValue = new MethodParameter(handle1, -1);

		Method handle2 = getClass().getMethod("handle2", HttpEntity.class);
		httpEntityReturnValue = new MethodParameter(handle2, -1);

		Method other = getClass().getMethod("otherMethod");
		intReturnValue = new MethodParameter(other, -1);

		mavContainer = new ModelAndViewContainer();
		
		servletRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		request = new ServletWebRequest(servletRequest, servletResponse);
	}

	@Test
	public void supportsParameter() {
		assertTrue("HttpEntity parameter not supported", processor.supportsParameter(httpEntityParam));
		assertFalse("ResponseEntity parameter supported", processor.supportsParameter(responseEntityParameter));
		assertFalse("non-entity parameter supported", processor.supportsParameter(intParameter));
	}

	@Test
	public void supportsReturnType() {
		assertTrue("ResponseEntity return type not supported", processor.supportsReturnType(responseEntityReturnValue));
		assertTrue("HttpEntity return type not supported", processor.supportsReturnType(httpEntityReturnValue));
		assertFalse("non-ResponseBody return type supported", processor.supportsReturnType(intReturnValue));
	}

	@Test
	public void usesResponseArgument() {
		assertFalse("HttpEntity parameter uses response argument", processor.usesResponseArgument(httpEntityParam));
		assertTrue("ResponseBody return type does not use response argument",
				processor.usesResponseArgument(responseEntityReturnValue));
		assertTrue("HttpEntity return type does not use response argument",
				processor.usesResponseArgument(httpEntityReturnValue));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveArgument() throws Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;
		String expected = "Foo";

		servletRequest.addHeader("Content-Type", contentType.toString());

		expect(messageConverter.getSupportedMediaTypes()).andReturn(Arrays.asList(contentType));
		expect(messageConverter.canRead(String.class, contentType)).andReturn(true);
		expect(messageConverter.read(eq(String.class), isA(HttpInputMessage.class))).andReturn(expected);

		replay(messageConverter);

		HttpEntity<?> result = (HttpEntity<String>) processor.resolveArgument(httpEntityParam, mavContainer, request, null);
		assertEquals("Invalid argument", expected, result.getBody());

		verify(messageConverter);
	}

	@Test(expected = HttpMediaTypeNotSupportedException.class)
	public void resolveArgumentNotReadable() throws Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;

		servletRequest.addHeader("Content-Type", contentType.toString());

		expect(messageConverter.getSupportedMediaTypes()).andReturn(Arrays.asList(contentType));
		expect(messageConverter.canRead(String.class, contentType)).andReturn(false);

		replay(messageConverter);

		processor.resolveArgument(httpEntityParam, mavContainer, request, null);

		verify(messageConverter);
	}

	@Test(expected = HttpMediaTypeNotSupportedException.class)
	public void resolveArgumentNoContentType() throws Exception {
		processor.resolveArgument(httpEntityParam, mavContainer, request, null);
	}

	@Test
	public void handleReturnValue() throws Exception {
		MediaType accepted = MediaType.TEXT_PLAIN;
		String s = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<String>(s, HttpStatus.OK);

		servletRequest.addHeader("Accept", accepted.toString());

		expect(messageConverter.canWrite(String.class, accepted)).andReturn(true);
		messageConverter.write(eq(s), eq(accepted), isA(HttpOutputMessage.class));

		replay(messageConverter);

		processor.handleReturnValue(returnValue, responseEntityReturnValue, mavContainer, request);

		assertFalse(mavContainer.isResolveView());
		verify(messageConverter);
	}

	@Test(expected = HttpMediaTypeNotAcceptableException.class)
	public void handleReturnValueNotAcceptable() throws Exception {
		MediaType accepted = MediaType.TEXT_PLAIN;
		String s = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<String>(s, HttpStatus.OK);

		servletRequest.addHeader("Accept", accepted.toString());

		expect(messageConverter.canWrite(String.class, accepted)).andReturn(false);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));

		replay(messageConverter);

		processor.handleReturnValue(returnValue, responseEntityReturnValue, mavContainer, request);

		assertFalse(mavContainer.isResolveView());
		verify(messageConverter);
	}

	@Test
	public void responseHeaderNoBody() throws Exception {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("header", "headerValue");
		ResponseEntity<String> returnValue = new ResponseEntity<String>(responseHeaders, HttpStatus.ACCEPTED);

		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(new StringHttpMessageConverter());
		processor.handleReturnValue(returnValue, responseEntityReturnValue, mavContainer, request);

		assertFalse(mavContainer.isResolveView());
		assertEquals("headerValue", servletResponse.getHeader("header"));
	}

	@Test
	public void responseHeaderAndBody() throws Exception {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("header", "headerValue");
		ResponseEntity<String> returnValue = new ResponseEntity<String>("body", responseHeaders, HttpStatus.ACCEPTED);

		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(new StringHttpMessageConverter());
		processor.handleReturnValue(returnValue, responseEntityReturnValue, mavContainer, request);

		assertFalse(mavContainer.isResolveView());
		assertEquals("headerValue", servletResponse.getHeader("header"));
	}
	
	public ResponseEntity<String> handle1(HttpEntity<String> httpEntity, ResponseEntity<String> responseEntity, int i) {
		return responseEntity;
	}

	public HttpEntity<?> handle2(HttpEntity<?> entity) {
		return entity;
	}

	public HttpEntity<?> handle3() {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("header", "headerValue");
		return new ResponseEntity<String>(responseHeaders, HttpStatus.OK);
	}
	
	public int otherMethod() {
		return 42;
	}

}
