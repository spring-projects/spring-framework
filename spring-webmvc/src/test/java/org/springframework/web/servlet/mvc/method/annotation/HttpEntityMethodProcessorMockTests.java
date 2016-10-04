/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.web.servlet.HandlerMapping.*;

/**
 * Test fixture for {@link HttpEntityMethodProcessor} delegating to a mock
 * {@link HttpMessageConverter}.
 *
 * <p>Also see {@link HttpEntityMethodProcessorTests}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class HttpEntityMethodProcessorMockTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private SimpleDateFormat dateFormat;

	private HttpEntityMethodProcessor processor;

	private HttpMessageConverter<String> stringHttpMessageConverter;

	private HttpMessageConverter<Resource> resourceMessageConverter;

	private MethodParameter paramHttpEntity;

	private MethodParameter paramRequestEntity;

	private MethodParameter paramResponseEntity;

	private MethodParameter paramInt;

	private MethodParameter returnTypeResponseEntity;

	private MethodParameter returnTypeResponseEntityProduces;

	private MethodParameter returnTypeResponseEntityResource;

	private MethodParameter returnTypeHttpEntity;

	private MethodParameter returnTypeHttpEntitySubclass;

	private MethodParameter returnTypeInt;

	private ModelAndViewContainer mavContainer;

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ServletWebRequest webRequest;


	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		stringHttpMessageConverter = mock(HttpMessageConverter.class);
		given(stringHttpMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		resourceMessageConverter = mock(HttpMessageConverter.class);
		given(resourceMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.ALL));
		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		converters.add(stringHttpMessageConverter);
		converters.add(resourceMessageConverter);
		processor = new HttpEntityMethodProcessor(converters);
		reset(stringHttpMessageConverter);
		reset(resourceMessageConverter);

		Method handle1 = getClass().getMethod("handle1", HttpEntity.class, ResponseEntity.class,
				Integer.TYPE, RequestEntity.class);

		paramHttpEntity = new MethodParameter(handle1, 0);
		paramRequestEntity = new MethodParameter(handle1, 3);
		paramResponseEntity = new MethodParameter(handle1, 1);
		paramInt = new MethodParameter(handle1, 2);
		returnTypeResponseEntity = new MethodParameter(handle1, -1);
		returnTypeResponseEntityProduces = new MethodParameter(getClass().getMethod("handle4"), -1);
		returnTypeHttpEntity = new MethodParameter(getClass().getMethod("handle2", HttpEntity.class), -1);
		returnTypeHttpEntitySubclass = new MethodParameter(getClass().getMethod("handle2x", HttpEntity.class), -1);
		returnTypeInt = new MethodParameter(getClass().getMethod("handle3"), -1);
		returnTypeResponseEntityResource = new MethodParameter(getClass().getMethod("handle5"), -1);

		mavContainer = new ModelAndViewContainer();
		servletRequest = new MockHttpServletRequest("GET", "/foo");
		servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(servletRequest, servletResponse);
	}


	@Test
	public void supportsParameter() {
		assertTrue("HttpEntity parameter not supported", processor.supportsParameter(paramHttpEntity));
		assertTrue("RequestEntity parameter not supported", processor.supportsParameter(paramRequestEntity));
		assertFalse("ResponseEntity parameter supported", processor.supportsParameter(paramResponseEntity));
		assertFalse("non-entity parameter supported", processor.supportsParameter(paramInt));
	}

	@Test
	public void supportsReturnType() {
		assertTrue("ResponseEntity return type not supported", processor.supportsReturnType(returnTypeResponseEntity));
		assertTrue("HttpEntity return type not supported", processor.supportsReturnType(returnTypeHttpEntity));
		assertTrue("Custom HttpEntity subclass not supported", processor.supportsReturnType(returnTypeHttpEntitySubclass));
		assertFalse("RequestEntity parameter supported",
				processor.supportsReturnType(paramRequestEntity));
		assertFalse("non-ResponseBody return type supported", processor.supportsReturnType(returnTypeInt));
	}

	@Test
	public void shouldResolveHttpEntityArgument() throws Exception {
		String body = "Foo";

		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());
		servletRequest.setContent(body.getBytes(StandardCharsets.UTF_8));

		given(stringHttpMessageConverter.canRead(String.class, contentType)).willReturn(true);
		given(stringHttpMessageConverter.read(eq(String.class), isA(HttpInputMessage.class))).willReturn(body);

		Object result = processor.resolveArgument(paramHttpEntity, mavContainer, webRequest, null);

		assertTrue(result instanceof HttpEntity);
		assertFalse("The requestHandled flag shouldn't change", mavContainer.isRequestHandled());
		assertEquals("Invalid argument", body, ((HttpEntity<?>) result).getBody());
	}

	@Test
	public void shouldResolveRequestEntityArgument() throws Exception {
		String body = "Foo";

		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());
		servletRequest.setMethod("GET");
		servletRequest.setServerName("www.example.com");
		servletRequest.setServerPort(80);
		servletRequest.setRequestURI("/path");
		servletRequest.setContent(body.getBytes(StandardCharsets.UTF_8));

		given(stringHttpMessageConverter.canRead(String.class, contentType)).willReturn(true);
		given(stringHttpMessageConverter.read(eq(String.class), isA(HttpInputMessage.class))).willReturn(body);

		Object result = processor.resolveArgument(paramRequestEntity, mavContainer, webRequest, null);

		assertTrue(result instanceof RequestEntity);
		assertFalse("The requestHandled flag shouldn't change", mavContainer.isRequestHandled());
		RequestEntity<?> requestEntity = (RequestEntity<?>) result;
		assertEquals("Invalid method", HttpMethod.GET, requestEntity.getMethod());
		// using default port (which is 80), so do not need to append the port (-1 means ignore)
		assertEquals("Invalid url", new URI("http", null, "www.example.com", -1, "/path", null, null), requestEntity.getUrl());
		assertEquals("Invalid argument", body, requestEntity.getBody());
	}

	@Test
	public void shouldFailResolvingWhenConverterCannotRead() throws Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.setMethod("POST");
		servletRequest.addHeader("Content-Type", contentType.toString());

		given(stringHttpMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(contentType));
		given(stringHttpMessageConverter.canRead(String.class, contentType)).willReturn(false);

		this.thrown.expect(HttpMediaTypeNotSupportedException.class);
		processor.resolveArgument(paramHttpEntity, mavContainer, webRequest, null);
	}

	@Test
	public void shouldFailResolvingWhenContentTypeNotSupported() throws Exception {
		servletRequest.setMethod("POST");
		servletRequest.setContent("some content".getBytes(StandardCharsets.UTF_8));
		this.thrown.expect(HttpMediaTypeNotSupportedException.class);
		processor.resolveArgument(paramHttpEntity, mavContainer, webRequest, null);
	}

	@Test
	public void shouldHandleReturnValue() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);
		MediaType accepted = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Accept", accepted.toString());
		initStringMessageConversion(accepted);

		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertTrue(mavContainer.isRequestHandled());
		verify(stringHttpMessageConverter).write(eq(body), eq(accepted), isA(HttpOutputMessage.class));
	}

	@Test
	public void shouldHandleReturnValueWithProducibleMediaType() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);
		servletRequest.addHeader("Accept", "text/*");
		servletRequest.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(MediaType.TEXT_HTML));
		given(stringHttpMessageConverter.canWrite(String.class, MediaType.TEXT_HTML)).willReturn(true);

		processor.handleReturnValue(returnValue, returnTypeResponseEntityProduces, mavContainer, webRequest);

		assertTrue(mavContainer.isRequestHandled());
		verify(stringHttpMessageConverter).write(eq(body), eq(MediaType.TEXT_HTML), isA(HttpOutputMessage.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldHandleReturnValueWithResponseBodyAdvice() throws Exception {
		servletRequest.addHeader("Accept", "text/*");
		servletRequest.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(MediaType.TEXT_HTML));
		ResponseEntity<String> returnValue = new ResponseEntity<>(HttpStatus.OK);
		ResponseBodyAdvice<String> advice = mock(ResponseBodyAdvice.class);
		given(advice.supports(any(), any())).willReturn(true);
		given(advice.beforeBodyWrite(any(), any(), any(), any(), any(), any())).willReturn("Foo");

		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(
				Collections.singletonList(stringHttpMessageConverter), null, Collections.singletonList(advice));

		reset(stringHttpMessageConverter);
		given(stringHttpMessageConverter.canWrite(String.class, MediaType.TEXT_HTML)).willReturn(true);

		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertTrue(mavContainer.isRequestHandled());
		verify(stringHttpMessageConverter).write(eq("Foo"), eq(MediaType.TEXT_HTML), isA(HttpOutputMessage.class));
	}

	@Test
	public void shouldFailHandlingWhenContentTypeNotSupported() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);
		MediaType accepted = MediaType.APPLICATION_ATOM_XML;
		servletRequest.addHeader("Accept", accepted.toString());

		given(stringHttpMessageConverter.canWrite(String.class, null)).willReturn(true);
		given(stringHttpMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));

		this.thrown.expect(HttpMediaTypeNotAcceptableException.class);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);
	}

	@Test
	public void shouldFailHandlingWhenConverterCannotWrite() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);
		MediaType accepted = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Accept", accepted.toString());

		given(stringHttpMessageConverter.canWrite(String.class, null)).willReturn(true);
		given(stringHttpMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		given(stringHttpMessageConverter.canWrite(String.class, accepted)).willReturn(false);

		this.thrown.expect(HttpMediaTypeNotAcceptableException.class);
		processor.handleReturnValue(returnValue, returnTypeResponseEntityProduces, mavContainer, webRequest);
	}

	@Test // SPR-9142
	public void shouldFailHandlingWhenAcceptHeaderIllegal() throws Exception {
		ResponseEntity<String> returnValue = new ResponseEntity<>("Body", HttpStatus.ACCEPTED);
		servletRequest.addHeader("Accept", "01");

		this.thrown.expect(HttpMediaTypeNotAcceptableException.class);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);
	}

	@Test
	public void shouldHandleResponseHeaderNoBody() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set("headerName", "headerValue");
		ResponseEntity<String> returnValue = new ResponseEntity<>(headers, HttpStatus.ACCEPTED);

		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertTrue(mavContainer.isRequestHandled());
		assertEquals("headerValue", servletResponse.getHeader("headerName"));
	}

	@Test
	public void shouldHandleResponseHeaderAndBody() throws Exception {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("header", "headerValue");
		ResponseEntity<String> returnValue = new ResponseEntity<>("body", responseHeaders, HttpStatus.ACCEPTED);

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		ArgumentCaptor<HttpOutputMessage> outputMessage = ArgumentCaptor.forClass(HttpOutputMessage.class);
		verify(stringHttpMessageConverter).write(eq("body"), eq(MediaType.TEXT_PLAIN), outputMessage.capture());
		assertTrue(mavContainer.isRequestHandled());
		assertEquals("headerValue", outputMessage.getValue().getHeaders().get("header").get(0));
	}

	@Test
	public void shouldHandleLastModifiedWithHttp304() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, dateFormat.format(currentTime));
		ResponseEntity<String> returnValue = ResponseEntity.ok().lastModified(oneMinuteAgo).body("body");

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, null, oneMinuteAgo);
	}

	@Test
	public void handleEtagWithHttp304() throws Exception {
		String etagValue = "\"deadb33f8badf00d\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, -1);
	}

	@Test // SPR-14559
	public void shouldHandleInvalidIfNoneMatchWithHttp200() throws Exception {
		String etagValue = "\"deadb33f8badf00d\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "unquoted");
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.OK, "body", etagValue, -1);
	}

	@Test
	public void shouldHandleETagAndLastModifiedWithHttp304() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		String etagValue = "\"deadb33f8badf00d\"";
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, dateFormat.format(currentTime));
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).lastModified(oneMinuteAgo).body("body");

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, oneMinuteAgo);
	}

	@Test
	public void shouldHandleNotModifiedResponse() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		String etagValue = "\"deadb33f8badf00d\"";
		ResponseEntity<String> returnValue = ResponseEntity.status(HttpStatus.NOT_MODIFIED)
				.eTag(etagValue).lastModified(oneMinuteAgo).body("body");

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, oneMinuteAgo);
	}

	@Test
	public void shouldHandleChangedETagAndLastModified() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		String etagValue = "\"deadb33f8badf00d\"";
		String changedEtagValue = "\"changed-etag-value\"";
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, dateFormat.format(currentTime));
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok()
				.eTag(changedEtagValue).lastModified(oneMinuteAgo).body("body");

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.OK, null, changedEtagValue, oneMinuteAgo);
	}

	@Test // SPR-13496
	public void shouldHandleConditionalRequestIfNoneMatchWildcard() throws Exception {
		String wildcardValue = "*";
		String etagValue = "\"some-etag\"";
		servletRequest.setMethod("POST");
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, wildcardValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.OK, "body", etagValue, -1);
	}

	@Test // SPR-13626
	public void shouldHandleGetIfNoneMatchWildcard() throws Exception {
		String wildcardValue = "*";
		String etagValue = "\"some-etag\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, wildcardValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.OK, "body", etagValue, -1);
	}

	@Test // SPR-13626
	public void shouldHandleIfNoneMatchIfMatch() throws Exception {
		String etagValue = "\"some-etag\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		servletRequest.addHeader(HttpHeaders.IF_MATCH, "ifmatch");
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, -1);
	}

	@Test // SPR-13626
	public void shouldHandleIfNoneMatchIfUnmodifiedSince() throws Exception {
		String etagValue = "\"some-etag\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		servletRequest.addHeader(HttpHeaders.IF_UNMODIFIED_SINCE, dateFormat.format(new Date().getTime()));
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, -1);
	}

	@Test
	public void shouldHandleResource() throws Exception {
		ResponseEntity<Resource> returnValue = ResponseEntity
				.ok(new ByteArrayResource("Content".getBytes(StandardCharsets.UTF_8)));

		given(resourceMessageConverter.canWrite(ByteArrayResource.class, null)).willReturn(true);
		given(resourceMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.ALL));
		given(resourceMessageConverter.canWrite(ByteArrayResource.class, MediaType.APPLICATION_OCTET_STREAM)).willReturn(true);

		processor.handleReturnValue(returnValue, returnTypeResponseEntityResource, mavContainer, webRequest);

		then(resourceMessageConverter).should(times(1)).write(any(ByteArrayResource.class),
				eq(MediaType.APPLICATION_OCTET_STREAM), any(HttpOutputMessage.class));
		assertEquals(200, servletResponse.getStatus());
	}

	@Test //SPR-14767
	public void shouldHandleValidatorHeadersInPutResponses() throws Exception {
		servletRequest.setMethod("PUT");
		String etagValue = "\"some-etag\"";
		ResponseEntity<String> returnValue = ResponseEntity.ok().header(HttpHeaders.ETAG, etagValue).body("body");

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.OK, "body", etagValue, -1);
	}

	private void initStringMessageConversion(MediaType accepted) {
		given(stringHttpMessageConverter.canWrite(String.class, null)).willReturn(true);
		given(stringHttpMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		given(stringHttpMessageConverter.canWrite(String.class, accepted)).willReturn(true);
	}

	private void assertResponseBody(String body) throws Exception {
		ArgumentCaptor<HttpOutputMessage> outputMessage = ArgumentCaptor.forClass(HttpOutputMessage.class);
		verify(stringHttpMessageConverter).write(eq(body), eq(MediaType.TEXT_PLAIN), outputMessage.capture());
	}

	private void assertConditionalResponse(HttpStatus status, String body,
			String etag, long lastModified) throws Exception {
		assertEquals(status.value(), servletResponse.getStatus());
		assertTrue(mavContainer.isRequestHandled());
		if (body != null) {
			assertResponseBody(body);
		}
		else {
			assertEquals(0, servletResponse.getContentAsByteArray().length);
		}
		if (etag != null) {
			assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.ETAG).size());
			assertEquals(etag, servletResponse.getHeader(HttpHeaders.ETAG));
		}
		if (lastModified != -1) {
			assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.LAST_MODIFIED).size());
			assertEquals(dateFormat.format(lastModified), servletResponse.getHeader(HttpHeaders.LAST_MODIFIED));
		}
	}

	@SuppressWarnings("unused")
	public ResponseEntity<String> handle1(HttpEntity<String> httpEntity, ResponseEntity<String> entity,
			int i, RequestEntity<String> requestEntity) {

		return entity;
	}

	@SuppressWarnings("unused")
	public HttpEntity<?> handle2(HttpEntity<?> entity) {
		return entity;
	}

	@SuppressWarnings("unused")
	public CustomHttpEntity handle2x(HttpEntity<?> entity) {
		return new CustomHttpEntity();
	}

	@SuppressWarnings("unused")
	public int handle3() {
		return 42;
	}

	@SuppressWarnings("unused")
	@RequestMapping(produces = {"text/html", "application/xhtml+xml"})
	public ResponseEntity<String> handle4() {
		return null;
	}

	@SuppressWarnings("unused")
	public ResponseEntity<Resource> handle5() {
		return null;
	}

	@SuppressWarnings("unused")
	public static class CustomHttpEntity extends HttpEntity<Object> {
	}

}