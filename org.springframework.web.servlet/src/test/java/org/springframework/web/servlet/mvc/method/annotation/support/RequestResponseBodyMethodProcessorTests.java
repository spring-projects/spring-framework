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
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

/**
 * Test fixture with {@link RequestResponseBodyMethodProcessor} and mock {@link HttpMessageConverter}.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestResponseBodyMethodProcessorTests {

	private RequestResponseBodyMethodProcessor processor;

	private HttpMessageConverter<String> messageConverter;

	private MethodParameter paramRequestBodyString;
	private MethodParameter paramInt;
	private MethodParameter returnTypeString;
	private MethodParameter returnTypeInt;

	private ModelAndViewContainer mavContainer;
	
	private NativeWebRequest webRequest;

	private MockHttpServletRequest servletRequest;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		messageConverter = createMock(HttpMessageConverter.class);
		
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.add(messageConverter);
		processor = new RequestResponseBodyMethodProcessor(messageConverters);
		
		Method handle = getClass().getMethod("handle1", String.class, Integer.TYPE);
		paramRequestBodyString = new MethodParameter(handle, 0);
		paramInt = new MethodParameter(handle, 1);
		returnTypeString = new MethodParameter(handle, -1);
		returnTypeInt = new MethodParameter(getClass().getMethod("handle2"), -1);

		mavContainer = new ModelAndViewContainer();
		
		servletRequest = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(servletRequest, new MockHttpServletResponse());
	}

	@Test
	public void supportsParameter() {
		assertTrue("RequestBody parameter not supported", processor.supportsParameter(paramRequestBodyString));
		assertFalse("non-RequestBody parameter supported", processor.supportsParameter(paramInt));
	}

	@Test
	public void supportsReturnType() {
		assertTrue("ResponseBody return type not supported", processor.supportsReturnType(returnTypeString));
		assertFalse("non-ResponseBody return type supported", processor.supportsReturnType(returnTypeInt));
	}

	@Test
	public void resolveArgument() throws Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());

		String body = "Foo";
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Arrays.asList(contentType));
		expect(messageConverter.canRead(String.class, contentType)).andReturn(true);
		expect(messageConverter.read(eq(String.class), isA(HttpInputMessage.class))).andReturn(body);

		replay(messageConverter);

		Object result = processor.resolveArgument(paramRequestBodyString, mavContainer, webRequest, null);

		assertEquals("Invalid argument", body, result);
		assertTrue("The ResolveView flag shouldn't change", mavContainer.isResolveView());
		verify(messageConverter);
	}

	@Test(expected = HttpMediaTypeNotSupportedException.class)
	public void resolveArgumentNotReadable() throws Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());

		expect(messageConverter.getSupportedMediaTypes()).andReturn(Arrays.asList(contentType));
		expect(messageConverter.canRead(String.class, contentType)).andReturn(false);
		replay(messageConverter);

		processor.resolveArgument(paramRequestBodyString, mavContainer, webRequest, null);

		fail("Expected exception");
	}

	@Test(expected = HttpMediaTypeNotSupportedException.class)
	public void resolveArgumentNoContentType() throws Exception {
		processor.resolveArgument(paramRequestBodyString, mavContainer, webRequest, null);
		fail("Expected exception");
	}

	@Test
	public void handleReturnValue() throws Exception {
		MediaType accepted = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Accept", accepted.toString());

		String body = "Foo";
		expect(messageConverter.canWrite(String.class, accepted)).andReturn(true);
		messageConverter.write(eq(body), eq(accepted), isA(HttpOutputMessage.class));
		replay(messageConverter);

		processor.handleReturnValue(body, returnTypeString, mavContainer, webRequest);

		assertFalse("The ResolveView flag wasn't turned off", mavContainer.isResolveView());
		verify(messageConverter);
	}

	@Test(expected = HttpMediaTypeNotAcceptableException.class)
	public void handleReturnValueNotAcceptable() throws Exception {
		MediaType accepted = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Accept", accepted.toString());

		expect(messageConverter.canWrite(String.class, accepted)).andReturn(false);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
		replay(messageConverter);

		processor.handleReturnValue("Foo", returnTypeString, mavContainer, webRequest);

		fail("Expected exception");
	}

	@ResponseBody
	public String handle1(@RequestBody String s, int i) {
		return s;
	}

	public int handle2() {
		return 42;
	}
	
}