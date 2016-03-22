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
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpRangeResource;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.*;
import static org.springframework.web.servlet.HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;

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
	public void resolveArgument() throws Exception {
		String body = "Foo";

		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());
		servletRequest.setContent(body.getBytes(Charset.forName("UTF-8")));

		given(stringHttpMessageConverter.canRead(String.class, contentType)).willReturn(true);
		given(stringHttpMessageConverter.read(eq(String.class), isA(HttpInputMessage.class))).willReturn(body);

		Object result = processor.resolveArgument(paramHttpEntity, mavContainer, webRequest, null);

		assertTrue(result instanceof HttpEntity);
		assertFalse("The requestHandled flag shouldn't change", mavContainer.isRequestHandled());
		assertEquals("Invalid argument", body, ((HttpEntity<?>) result).getBody());
	}

	@Test
	public void resolveArgumentRequestEntity() throws Exception {
		String body = "Foo";

		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());
		servletRequest.setMethod("GET");
		servletRequest.setServerName("www.example.com");
		servletRequest.setServerPort(80);
		servletRequest.setRequestURI("/path");
		servletRequest.setContent(body.getBytes(Charset.forName("UTF-8")));

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

	@Test(expected = HttpMediaTypeNotSupportedException.class)
	public void resolveArgumentNotReadable() throws Exception {
		MediaType contentType = MediaType.TEXT_PLAIN;
		servletRequest.setMethod("POST");
		servletRequest.addHeader("Content-Type", contentType.toString());

		given(stringHttpMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(contentType));
		given(stringHttpMessageConverter.canRead(String.class, contentType)).willReturn(false);

		processor.resolveArgument(paramHttpEntity, mavContainer, webRequest, null);

		fail("Expected exception");
	}

	@Test(expected = HttpMediaTypeNotSupportedException.class)
	public void resolveArgumentNoContentType() throws Exception {
		servletRequest.setMethod("POST");
		servletRequest.setContent("some content".getBytes(Charset.forName("UTF-8")));
		processor.resolveArgument(paramHttpEntity, mavContainer, webRequest, null);
		fail("Expected exception");
	}

	@Test
	public void handleReturnValue() throws Exception {
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
	public void handleReturnValueProduces() throws Exception {
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
	public void handleReturnValueWithResponseBodyAdvice() throws Exception {
		ResponseEntity<String> returnValue = new ResponseEntity<>(HttpStatus.OK);

		servletRequest.addHeader("Accept", "text/*");
		servletRequest.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(MediaType.TEXT_HTML));

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

	@Test(expected = HttpMediaTypeNotAcceptableException.class)
	public void handleReturnValueNotAcceptable() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);

		MediaType accepted = MediaType.APPLICATION_ATOM_XML;
		servletRequest.addHeader("Accept", accepted.toString());

		given(stringHttpMessageConverter.canWrite(String.class, null)).willReturn(true);
		given(stringHttpMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		given(stringHttpMessageConverter.canWrite(String.class, accepted)).willReturn(false);

		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		fail("Expected exception");
	}

	@Test(expected = HttpMediaTypeNotAcceptableException.class)
	public void handleReturnValueNotAcceptableProduces() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);

		MediaType accepted = MediaType.TEXT_PLAIN;
		servletRequest.addHeader("Accept", accepted.toString());

		given(stringHttpMessageConverter.canWrite(String.class, null)).willReturn(true);
		given(stringHttpMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		given(stringHttpMessageConverter.canWrite(String.class, accepted)).willReturn(false);

		processor.handleReturnValue(returnValue, returnTypeResponseEntityProduces, mavContainer, webRequest);

		fail("Expected exception");
	}

	// SPR-9142

	@Test(expected=HttpMediaTypeNotAcceptableException.class)
	public void handleReturnValueNotAcceptableParseError() throws Exception {
		ResponseEntity<String> returnValue = new ResponseEntity<>("Body", HttpStatus.ACCEPTED);
		servletRequest.addHeader("Accept", "01");

		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);
		fail("Expected exception");
	}

	@Test
	public void handleReturnValueResponseHeaderNoBody() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set("headerName", "headerValue");
		ResponseEntity<String> returnValue = new ResponseEntity<>(headers, HttpStatus.ACCEPTED);

		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertTrue(mavContainer.isRequestHandled());
		assertEquals("headerValue", servletResponse.getHeader("headerName"));
	}

	@Test
	public void handleReturnValueResponseHeaderAndBody() throws Exception {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("header", "headerValue");
		ResponseEntity<String> returnValue = new ResponseEntity<>("body", responseHeaders, HttpStatus.ACCEPTED);

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		ArgumentCaptor<HttpOutputMessage> outputMessage = ArgumentCaptor.forClass(HttpOutputMessage.class);
		verify(stringHttpMessageConverter).write(eq("body"), eq(MediaType.TEXT_PLAIN),  outputMessage.capture());
		assertTrue(mavContainer.isRequestHandled());
		assertEquals("headerValue", outputMessage.getValue().getHeaders().get("header").get(0));
	}

	@Test
	public void handleReturnValueLastModified() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo  = currentTime - (1000 * 60);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, dateFormat.format(currentTime));
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setDate(HttpHeaders.LAST_MODIFIED, oneMinuteAgo);
		ResponseEntity<String> returnValue = new ResponseEntity<>("body", responseHeaders, HttpStatus.OK);

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertResponseNotModified();
		assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.LAST_MODIFIED).size());
		assertEquals(dateFormat.format(oneMinuteAgo), servletResponse.getHeader(HttpHeaders.LAST_MODIFIED));
	}

	@Test
	public void handleReturnValueEtag() throws Exception {
		String etagValue = "\"deadb33f8badf00d\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HttpHeaders.ETAG, etagValue);
		ResponseEntity<String> returnValue = new ResponseEntity<>("body", responseHeaders, HttpStatus.OK);

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertResponseNotModified();
		assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.ETAG).size());
		assertEquals(etagValue, servletResponse.getHeader(HttpHeaders.ETAG));
	}

	@Test
	public void handleReturnValueETagAndLastModified() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo  = currentTime - (1000 * 60);
		String etagValue = "\"deadb33f8badf00d\"";
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, dateFormat.format(currentTime));
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setDate(HttpHeaders.LAST_MODIFIED, oneMinuteAgo);
		responseHeaders.set(HttpHeaders.ETAG, etagValue);
		ResponseEntity<String> returnValue = new ResponseEntity<>("body", responseHeaders, HttpStatus.OK);

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertResponseNotModified();
		assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.LAST_MODIFIED).size());
		assertEquals(dateFormat.format(oneMinuteAgo), servletResponse.getHeader(HttpHeaders.LAST_MODIFIED));
		assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.ETAG).size());
		assertEquals(etagValue, servletResponse.getHeader(HttpHeaders.ETAG));
	}

	@Test
	public void handleReturnValueNotModified() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo  = currentTime - (1000 * 60);
		String etagValue = "\"deadb33f8badf00d\"";
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setDate(HttpHeaders.LAST_MODIFIED, oneMinuteAgo);
		responseHeaders.set(HttpHeaders.ETAG, etagValue);
		ResponseEntity<String> returnValue = new ResponseEntity<>("body", responseHeaders, HttpStatus.NOT_MODIFIED);

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertResponseNotModified();
		assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.LAST_MODIFIED).size());
		assertEquals(dateFormat.format(oneMinuteAgo), servletResponse.getHeader(HttpHeaders.LAST_MODIFIED));
		assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.ETAG).size());
		assertEquals(etagValue, servletResponse.getHeader(HttpHeaders.ETAG));
	}

	@Test
	public void handleReturnValueChangedETagAndLastModified() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo  = currentTime - (1000 * 60);
		String etagValue = "\"deadb33f8badf00d\"";
		String changedEtagValue = "\"changed-etag-value\"";
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, dateFormat.format(currentTime));
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setDate(HttpHeaders.LAST_MODIFIED, oneMinuteAgo);
		responseHeaders.set(HttpHeaders.ETAG, changedEtagValue);
		ResponseEntity<String> returnValue = new ResponseEntity<>("body", responseHeaders, HttpStatus.OK);

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertTrue(mavContainer.isRequestHandled());
		assertEquals(HttpStatus.OK.value(), servletResponse.getStatus());
		assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.LAST_MODIFIED).size());
		assertEquals(dateFormat.format(oneMinuteAgo), servletResponse.getHeader(HttpHeaders.LAST_MODIFIED));
		assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.ETAG).size());
		assertEquals(changedEtagValue, servletResponse.getHeader(HttpHeaders.ETAG));
		assertEquals(0, servletResponse.getContentAsByteArray().length);
	}

	// SPR-13496
	@Test
	public void handleReturnValuePostRequestWithIfNotModified() throws Exception {
		String wildcardValue = "*";
		String etagValue = "\"some-etag\"";
		servletRequest.setMethod("POST");
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, wildcardValue);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HttpHeaders.ETAG, etagValue);
		ResponseEntity<String> returnValue = new ResponseEntity<>("body", responseHeaders, HttpStatus.OK);

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertResponseOkWithBody("body");
		assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.ETAG).size());
		assertEquals(etagValue, servletResponse.getHeader(HttpHeaders.ETAG));
	}

	// SPR-13626
	@Test
	public void handleReturnValueGetIfNoneMatchWildcard() throws Exception {
		String wildcardValue = "*";
		String etagValue = "\"some-etag\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, wildcardValue);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HttpHeaders.ETAG, etagValue);
		ResponseEntity<String> returnValue = new ResponseEntity<>("body", responseHeaders, HttpStatus.OK);

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertResponseOkWithBody("body");
		assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.ETAG).size());
		assertEquals(etagValue, servletResponse.getHeader(HttpHeaders.ETAG));
	}

	// SPR-13626
	@Test
	public void handleReturnValueIfNoneMatchIfMatch() throws Exception {
		String etagValue = "\"some-etag\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		servletRequest.addHeader(HttpHeaders.IF_MATCH, "ifmatch");
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HttpHeaders.ETAG, etagValue);
		ResponseEntity<String> returnValue = new ResponseEntity<>("body", responseHeaders, HttpStatus.OK);

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertResponseOkWithBody("body");
		assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.ETAG).size());
		assertEquals(etagValue, servletResponse.getHeader(HttpHeaders.ETAG));
	}

	// SPR-13626
	@Test
	public void handleReturnValueIfNoneMatchIfUnmodifiedSince() throws Exception {
		String etagValue = "\"some-etag\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		servletRequest.addHeader(HttpHeaders.IF_UNMODIFIED_SINCE, dateFormat.format(new Date().getTime()));
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set(HttpHeaders.ETAG, etagValue);
		ResponseEntity<String> returnValue = new ResponseEntity<>("body", responseHeaders, HttpStatus.OK);

		initStringMessageConversion(MediaType.TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertResponseOkWithBody("body");
		assertEquals(1, servletResponse.getHeaderValues(HttpHeaders.ETAG).size());
		assertEquals(etagValue, servletResponse.getHeader(HttpHeaders.ETAG));
	}

	@Test
	public void handleReturnTypeResource() throws Exception {
		ResponseEntity<Resource> returnValue = ResponseEntity
				.ok(new ByteArrayResource("Content".getBytes(Charset.forName("UTF-8"))));

		given(resourceMessageConverter.canWrite(ByteArrayResource.class, null)).willReturn(true);
		given(resourceMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.ALL));
		given(resourceMessageConverter.canWrite(ByteArrayResource.class, MediaType.APPLICATION_OCTET_STREAM)).willReturn(true);

		processor.handleReturnValue(returnValue, returnTypeResponseEntityResource, mavContainer, webRequest);

		then(resourceMessageConverter).should(times(1)).write(any(ByteArrayResource.class),
				eq(MediaType.APPLICATION_OCTET_STREAM), any(HttpOutputMessage.class));
		assertEquals(200, servletResponse.getStatus());
	}

	@Test
	public void handleReturnTypeResourceByteRange() throws Exception {
		Resource resource = new ByteArrayResource("Content".getBytes(Charset.forName("UTF-8")));
		ResponseEntity<Resource> returnValue = ResponseEntity.ok(resource);
		servletRequest.addHeader("Range", "bytes=0-5");

		given(resourceMessageConverter.canWrite(HttpRangeResource.class, null)).willReturn(true);
		given(resourceMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.ALL));
		given(resourceMessageConverter.canWrite(HttpRangeResource.class, MediaType.APPLICATION_OCTET_STREAM)).willReturn(true);

		processor.handleReturnValue(returnValue, returnTypeResponseEntityResource, mavContainer, webRequest);

		then(resourceMessageConverter).should(times(1)).write(any(ByteArrayResource.class),
				eq(MediaType.APPLICATION_OCTET_STREAM), any(HttpOutputMessage.class));
		assertEquals(206, servletResponse.getStatus());
	}

	@Test
	public void handleReturnTypeResourceIllegalByteRange() throws Exception {
		Resource resource = new ByteArrayResource("Content".getBytes(Charset.forName("UTF-8")));
		ResponseEntity<Resource> returnValue = ResponseEntity.ok(resource);
		servletRequest.addHeader("Range", "illegal");

		given(resourceMessageConverter.canWrite(ByteArrayResource.class, null)).willReturn(true);
		given(resourceMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.ALL));

		processor.handleReturnValue(returnValue, returnTypeResponseEntityResource, mavContainer, webRequest);

		then(resourceMessageConverter).should(never()).write(any(ByteArrayResource.class),
				eq(MediaType.APPLICATION_OCTET_STREAM), any(HttpOutputMessage.class));
		assertEquals(416, servletResponse.getStatus());
	}

	private void initStringMessageConversion(MediaType accepted) {
		given(stringHttpMessageConverter.canWrite(String.class, null)).willReturn(true);
		given(stringHttpMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		given(stringHttpMessageConverter.canWrite(String.class, accepted)).willReturn(true);
	}

	private void assertResponseNotModified() {
		assertTrue(mavContainer.isRequestHandled());
		assertEquals(HttpStatus.NOT_MODIFIED.value(), servletResponse.getStatus());
		assertEquals(0, servletResponse.getContentAsByteArray().length);
	}

	private void assertResponseOkWithBody(String body) throws Exception {
		assertTrue(mavContainer.isRequestHandled());
		assertEquals(HttpStatus.OK.value(), servletResponse.getStatus());
		ArgumentCaptor<HttpOutputMessage> outputMessage = ArgumentCaptor.forClass(HttpOutputMessage.class);
		verify(stringHttpMessageConverter).write(eq(body), eq(MediaType.TEXT_PLAIN), outputMessage.capture());
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
	public ResponseEntity<Resource> handle5() {return null;}

	@SuppressWarnings("unused")
	public static class CustomHttpEntity extends HttpEntity<Object> {
	}

}