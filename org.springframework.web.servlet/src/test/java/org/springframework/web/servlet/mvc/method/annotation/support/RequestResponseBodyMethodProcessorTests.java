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
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.support.RequestResponseBodyMethodProcessor;

/**
 * @author Arjen Poutsma
 */
public class RequestResponseBodyMethodProcessorTests {

	private RequestResponseBodyMethodProcessor processor;

	private HttpMessageConverter<String> messageConverter;

	private MethodParameter stringParameter;

	private MethodParameter stringReturnValue;

	private MethodParameter intParameter;

	private MethodParameter intReturnValue;

	private ModelAndViewContainer mavContainer;
	
	private NativeWebRequest webRequest;

	private MockHttpServletRequest servletRequest;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		messageConverter = createMock(HttpMessageConverter.class);
		processor = new RequestResponseBodyMethodProcessor(messageConverter);
		Method handle = getClass().getMethod("handle", String.class, Integer.TYPE);
		stringParameter = new MethodParameter(handle, 0);
		intParameter = new MethodParameter(handle, 1);
		stringReturnValue = new MethodParameter(handle, -1);
		Method other = getClass().getMethod("otherMethod");
		intReturnValue = new MethodParameter(other, -1);

		mavContainer = new ModelAndViewContainer();
		
		servletRequest = new MockHttpServletRequest();
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(servletRequest, servletResponse);
	}

	@Test
	public void supportsParameter() {
		assertTrue("RequestBody parameter not supported", processor.supportsParameter(stringParameter));
		assertFalse("non-RequestBody parameter supported", processor.supportsParameter(intParameter));
	}

	@Test
	public void supportsReturnType() {
		assertTrue("ResponseBody return type not supported", processor.supportsReturnType(stringReturnValue));
		assertFalse("non-ResponseBody return type supported", processor.supportsReturnType(intReturnValue));
	}

	@Test
	public void usesResponseArgument() {
		assertFalse("RequestBody parameter uses response argument", processor.usesResponseArgument(stringParameter));
		assertTrue("ResponseBody return type does not use response argument",
				processor.usesResponseArgument(stringReturnValue));
	}

	@Test
	public void resolveArgument() throws Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;
		String expected = "Foo";

		servletRequest.addHeader("Content-Type", contentType.toString());

		expect(messageConverter.getSupportedMediaTypes()).andReturn(Arrays.asList(contentType));
		expect(messageConverter.canRead(String.class, contentType)).andReturn(true);
		expect(messageConverter.read(eq(String.class), isA(HttpInputMessage.class))).andReturn(expected);

		replay(messageConverter);

		Object result = processor.resolveArgument(stringParameter, mavContainer, webRequest, null);
		assertEquals("Invalid argument", expected, result);

		verify(messageConverter);

	}

	@Test(expected = HttpMediaTypeNotSupportedException.class)
	public void resolveArgumentNotReadable() throws Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;

		servletRequest.addHeader("Content-Type", contentType.toString());

		expect(messageConverter.getSupportedMediaTypes()).andReturn(Arrays.asList(contentType));
		expect(messageConverter.canRead(String.class, contentType)).andReturn(false);

		replay(messageConverter);

		processor.resolveArgument(stringParameter, mavContainer, webRequest, null);

		verify(messageConverter);
	}

	@Test(expected = HttpMediaTypeNotSupportedException.class)
	public void resolveArgumentNoContentType() throws Exception {
		processor.resolveArgument(stringParameter, mavContainer, webRequest, null);
	}

	@Test
	public void handleReturnValue() throws Exception {
		MediaType accepted = MediaType.TEXT_PLAIN;
		String returnValue = "Foo";

		servletRequest.addHeader("Accept", accepted.toString());

		expect(messageConverter.canWrite(String.class, accepted)).andReturn(true);
		messageConverter.write(eq(returnValue), eq(accepted), isA(HttpOutputMessage.class));

		replay(messageConverter);

		processor.handleReturnValue(returnValue, stringReturnValue, mavContainer, webRequest);

		assertFalse(mavContainer.isResolveView());
		verify(messageConverter);
	}

	@Test(expected = HttpMediaTypeNotAcceptableException.class)
	public void handleReturnValueNotAcceptable() throws Exception {
		MediaType accepted = MediaType.TEXT_PLAIN;
		String returnValue = "Foo";

		servletRequest.addHeader("Accept", accepted.toString());

		expect(messageConverter.canWrite(String.class, accepted)).andReturn(false);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));

		replay(messageConverter);

		processor.handleReturnValue(returnValue, stringReturnValue, mavContainer, webRequest);

		assertFalse(mavContainer.isResolveView());
		verify(messageConverter);
	}

	@ResponseBody
	public String handle(@RequestBody String s, int i) {
		return s;
	}

	public int otherMethod() {
		return 42;
	}
	
}
