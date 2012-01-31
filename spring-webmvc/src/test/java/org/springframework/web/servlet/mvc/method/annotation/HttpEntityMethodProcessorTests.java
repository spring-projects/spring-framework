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

package org.springframework.web.servlet.mvc.method.annotation;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.web.servlet.HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import org.easymock.Capture;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.HttpEntityMethodProcessor;

/**
 * Test fixture with {@link HttpEntityMethodProcessor} and mock {@link HttpMessageConverter}.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class HttpEntityMethodProcessorTests {

	private HttpEntityMethodProcessor processor;

	private HttpMessageConverter<String> messageConverter;

	private MethodParameter paramHttpEntity;
	private MethodParameter paramResponseEntity;
	private MethodParameter paramInt;
	private MethodParameter returnTypeResponseEntity;
	private MethodParameter returnTypeHttpEntity;
	private MethodParameter returnTypeInt;
	private MethodParameter returnTypeResponseEntityProduces;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MockHttpServletResponse servletResponse;

	private MockHttpServletRequest servletRequest;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		messageConverter = createMock(HttpMessageConverter.class);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		replay(messageConverter);

		processor = new HttpEntityMethodProcessor(Collections.<HttpMessageConverter<?>>singletonList(messageConverter));
		reset(messageConverter);


		Method handle1 = getClass().getMethod("handle1", HttpEntity.class, ResponseEntity.class, Integer.TYPE);
		paramHttpEntity = new MethodParameter(handle1, 0);
		paramResponseEntity = new MethodParameter(handle1, 1);
		paramInt = new MethodParameter(handle1, 2);
		returnTypeResponseEntity = new MethodParameter(handle1, -1);

		returnTypeHttpEntity = new MethodParameter(getClass().getMethod("handle2", HttpEntity.class), -1);

		returnTypeInt = new MethodParameter(getClass().getMethod("handle3"), -1);

		returnTypeResponseEntityProduces = new MethodParameter(getClass().getMethod("handle4"), -1);

		mavContainer = new ModelAndViewContainer();
		
		servletRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(servletRequest, servletResponse);
	}

	@Test
	public void supportsParameter() {
		assertTrue("HttpEntity parameter not supported", processor.supportsParameter(paramHttpEntity));
		assertFalse("ResponseEntity parameter supported", processor.supportsParameter(paramResponseEntity));
		assertFalse("non-entity parameter supported", processor.supportsParameter(paramInt));
	}

	@Test
	public void supportsReturnType() {
		assertTrue("ResponseEntity return type not supported", processor.supportsReturnType(returnTypeResponseEntity));
		assertTrue("HttpEntity return type not supported", processor.supportsReturnType(returnTypeHttpEntity));
		assertFalse("non-ResponseBody return type supported", processor.supportsReturnType(returnTypeInt));
	}

	@Test
	public void resolveArgument() throws Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());

		String body = "Foo";
		expect(messageConverter.canRead(String.class, contentType)).andReturn(true);
		expect(messageConverter.read(eq(String.class), isA(HttpInputMessage.class))).andReturn(body);
		replay(messageConverter);

		Object result = processor.resolveArgument(paramHttpEntity, mavContainer, webRequest, null);

		assertTrue(result instanceof HttpEntity);
		assertFalse("The requestHandled flag shouldn't change", mavContainer.isRequestHandled());
		assertEquals("Invalid argument", body, ((HttpEntity<?>) result).getBody());
		verify(messageConverter);
	}

	@Test(expected = HttpMediaTypeNotSupportedException.class)
	public void resolveArgumentNotReadable() throws Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());

		expect(messageConverter.getSupportedMediaTypes()).andReturn(Arrays.asList(contentType));
		expect(messageConverter.canRead(String.class, contentType)).andReturn(false);
		replay(messageConverter);

		processor.resolveArgument(paramHttpEntity, mavContainer, webRequest, null);

		fail("Expected exception");
	}

	@Test(expected = HttpMediaTypeNotSupportedException.class)
	public void resolveArgumentNoContentType() throws Exception {
		processor.resolveArgument(paramHttpEntity, mavContainer, webRequest, null);
		fail("Expected exception");
	}

	@Test
	public void handleReturnValue() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<String>(body, HttpStatus.OK);

		MediaType accepted = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Accept", accepted.toString());

		expect(messageConverter.canWrite(String.class, null)).andReturn(true);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		expect(messageConverter.canWrite(String.class, accepted)).andReturn(true);
		messageConverter.write(eq(body), eq(accepted), isA(HttpOutputMessage.class));
		replay(messageConverter);

		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertTrue(mavContainer.isRequestHandled());
		verify(messageConverter);
	}

	@Test
	public void handleReturnValueProduces() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<String>(body, HttpStatus.OK);

		servletRequest.addHeader("Accept", "text/*");
		servletRequest.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(MediaType.TEXT_HTML));

		expect(messageConverter.canWrite(String.class, MediaType.TEXT_HTML)).andReturn(true);
		messageConverter.write(eq(body), eq(MediaType.TEXT_HTML), isA(HttpOutputMessage.class));
		replay(messageConverter);

		processor.handleReturnValue(returnValue, returnTypeResponseEntityProduces, mavContainer, webRequest);

		assertTrue(mavContainer.isRequestHandled());
		verify(messageConverter);
	}

	@Test(expected = HttpMediaTypeNotAcceptableException.class)
	public void handleReturnValueNotAcceptable() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<String>(body, HttpStatus.OK);

		MediaType accepted = MediaType.APPLICATION_ATOM_XML;
		servletRequest.addHeader("Accept", accepted.toString());

		expect(messageConverter.canWrite(String.class, null)).andReturn(true);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Arrays.asList(MediaType.TEXT_PLAIN));
		expect(messageConverter.canWrite(String.class, accepted)).andReturn(false);
		replay(messageConverter);

		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		fail("Expected exception");
	}
	
	@Test(expected = HttpMediaTypeNotAcceptableException.class)
	public void handleReturnValueNotAcceptableProduces() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<String>(body, HttpStatus.OK);

		MediaType accepted = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Accept", accepted.toString());

		expect(messageConverter.canWrite(String.class, null)).andReturn(true);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		expect(messageConverter.canWrite(String.class, accepted)).andReturn(false);
		replay(messageConverter);

		processor.handleReturnValue(returnValue, returnTypeResponseEntityProduces, mavContainer, webRequest);

		fail("Expected exception");
	}

	@Test
	public void responseHeaderNoBody() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set("headerName", "headerValue");
		ResponseEntity<String> returnValue = new ResponseEntity<String>(headers, HttpStatus.ACCEPTED);

		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertTrue(mavContainer.isRequestHandled());
		assertEquals("headerValue", servletResponse.getHeader("headerName"));
	}

	@Test
	public void responseHeaderAndBody() throws Exception {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("header", "headerValue");
		ResponseEntity<String> returnValue = new ResponseEntity<String>("body", responseHeaders, HttpStatus.ACCEPTED);

		Capture<HttpOutputMessage> outputMessage = new Capture<HttpOutputMessage>();
		expect(messageConverter.canWrite(String.class, null)).andReturn(true);
		expect(messageConverter.getSupportedMediaTypes()).andReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		expect(messageConverter.canWrite(String.class, MediaType.TEXT_PLAIN)).andReturn(true);
		messageConverter.write(eq("body"), eq(MediaType.TEXT_PLAIN),  capture(outputMessage));
		replay(messageConverter);

		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertTrue(mavContainer.isRequestHandled());
		assertEquals("headerValue", outputMessage.getValue().getHeaders().get("header").get(0));
		verify(messageConverter);
	}
	
	public ResponseEntity<String> handle1(HttpEntity<String> httpEntity, ResponseEntity<String> responseEntity, int i) {
		return responseEntity;
	}

	public HttpEntity<?> handle2(HttpEntity<?> entity) {
		return entity;
	}

	public int handle3() {
		return 42;
	}

	@RequestMapping(produces = {"text/html", "application/xhtml+xml"})
	public ResponseEntity<String> handle4() {
		return null;
	}


}