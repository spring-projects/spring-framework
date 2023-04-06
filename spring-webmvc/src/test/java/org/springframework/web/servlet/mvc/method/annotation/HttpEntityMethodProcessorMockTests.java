/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static java.time.Instant.ofEpochMilli;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN;
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
class HttpEntityMethodProcessorMockTests {

	private static final ZoneId GMT = ZoneId.of("GMT");


	private HttpEntityMethodProcessor processor;

	@SuppressWarnings("unchecked")
	private HttpMessageConverter<String> stringHttpMessageConverter = mock();

	@SuppressWarnings("unchecked")
	private HttpMessageConverter<Resource> resourceMessageConverter = mock();

	@SuppressWarnings("unchecked")
	private HttpMessageConverter<Object> resourceRegionMessageConverter = mock();

	@SuppressWarnings("unchecked")
	private HttpMessageConverter<Object> jsonMessageConverter = mock();

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

	private MethodParameter returnTypeErrorResponse;

	private MethodParameter returnTypeProblemDetail;

	private ModelAndViewContainer mavContainer = new ModelAndViewContainer();

	private MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/foo");

	private MockHttpServletResponse servletResponse = new MockHttpServletResponse();

	private ServletWebRequest webRequest = new ServletWebRequest(servletRequest, servletResponse);


	@BeforeEach
	void setup() throws Exception {
		given(stringHttpMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(TEXT_PLAIN));

		given(resourceMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.ALL));
		given(resourceMessageConverter.getSupportedMediaTypes(any())).willReturn(Collections.singletonList(MediaType.ALL));

		given(resourceRegionMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.ALL));
		given(resourceRegionMessageConverter.getSupportedMediaTypes(any())).willReturn(Collections.singletonList(MediaType.ALL));

		given(jsonMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.APPLICATION_PROBLEM_JSON));
		given(jsonMessageConverter.getSupportedMediaTypes(any())).willReturn(Collections.singletonList(MediaType.APPLICATION_PROBLEM_JSON));

		processor = new HttpEntityMethodProcessor(Arrays.asList(
				stringHttpMessageConverter, resourceMessageConverter, resourceRegionMessageConverter, jsonMessageConverter));

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
		returnTypeErrorResponse = new MethodParameter(getClass().getMethod("handle6"), -1);
		returnTypeProblemDetail = new MethodParameter(getClass().getMethod("handle7"), -1);
	}


	@Test
	void supportsParameter() {
		assertThat(processor.supportsParameter(paramHttpEntity)).as("HttpEntity parameter not supported").isTrue();
		assertThat(processor.supportsParameter(paramRequestEntity)).as("RequestEntity parameter not supported").isTrue();
		assertThat(processor.supportsParameter(paramResponseEntity)).as("ResponseEntity parameter supported").isFalse();
		assertThat(processor.supportsParameter(paramInt)).as("non-entity parameter supported").isFalse();
	}

	@Test
	void supportsReturnType() {
		assertThat(processor.supportsReturnType(returnTypeResponseEntity)).as("ResponseEntity return type not supported").isTrue();
		assertThat(processor.supportsReturnType(returnTypeHttpEntity)).as("HttpEntity return type not supported").isTrue();
		assertThat(processor.supportsReturnType(returnTypeHttpEntitySubclass)).as("Custom HttpEntity subclass not supported").isTrue();
		assertThat(processor.supportsReturnType(returnTypeErrorResponse)).isTrue();
		assertThat(processor.supportsReturnType(returnTypeProblemDetail)).isTrue();
		assertThat(processor.supportsReturnType(paramRequestEntity)).as("RequestEntity parameter supported").isFalse();
		assertThat(processor.supportsReturnType(returnTypeInt)).as("non-ResponseBody return type supported").isFalse();
	}

	@Test
	void shouldResolveHttpEntityArgument() throws Exception {
		String body = "Foo";

		MediaType contentType = TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());
		servletRequest.setContent(body.getBytes(StandardCharsets.UTF_8));

		given(stringHttpMessageConverter.canRead(String.class, contentType)).willReturn(true);
		given(stringHttpMessageConverter.read(eq(String.class), isA(HttpInputMessage.class))).willReturn(body);

		Object result = processor.resolveArgument(paramHttpEntity, mavContainer, webRequest, null);

		assertThat(result).isInstanceOf(HttpEntity.class);
		assertThat(mavContainer.isRequestHandled()).as("The requestHandled flag shouldn't change").isFalse();
		assertThat(((HttpEntity<?>) result).getBody()).as("Invalid argument").isEqualTo(body);
	}

	@Test
	void shouldResolveRequestEntityArgument() throws Exception {
		String body = "Foo";

		MediaType contentType = TEXT_PLAIN;
		servletRequest.addHeader("Content-Type", contentType.toString());
		servletRequest.setMethod("GET");
		servletRequest.setServerName("www.example.com");
		servletRequest.setServerPort(80);
		servletRequest.setRequestURI("/path");
		servletRequest.setContent(body.getBytes(StandardCharsets.UTF_8));

		given(stringHttpMessageConverter.canRead(String.class, contentType)).willReturn(true);
		given(stringHttpMessageConverter.read(eq(String.class), isA(HttpInputMessage.class))).willReturn(body);

		Object result = processor.resolveArgument(paramRequestEntity, mavContainer, webRequest, null);

		assertThat(result).isInstanceOf(RequestEntity.class);
		assertThat(mavContainer.isRequestHandled()).as("The requestHandled flag shouldn't change").isFalse();
		RequestEntity<?> requestEntity = (RequestEntity<?>) result;
		assertThat(requestEntity.getMethod()).as("Invalid method").isEqualTo(HttpMethod.GET);
		// using default port (which is 80), so do not need to append the port (-1 means ignore)
		URI uri = new URI("http", null, "www.example.com", -1, "/path", null, null);
		assertThat(requestEntity.getUrl()).as("Invalid url").isEqualTo(uri);
		assertThat(requestEntity.getBody()).as("Invalid argument").isEqualTo(body);
	}

	@Test
	void shouldFailResolvingWhenConverterCannotRead() throws Exception {
		MediaType contentType = TEXT_PLAIN;
		servletRequest.setMethod("POST");
		servletRequest.addHeader("Content-Type", contentType.toString());

		given(stringHttpMessageConverter.getSupportedMediaTypes(any())).willReturn(Collections.singletonList(contentType));
		given(stringHttpMessageConverter.canRead(String.class, contentType)).willReturn(false);

		assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class).isThrownBy(() ->
				processor.resolveArgument(paramHttpEntity, mavContainer, webRequest, null));
	}

	@Test
	void shouldFailResolvingWhenContentTypeNotSupported() throws Exception {
		servletRequest.setMethod("POST");
		servletRequest.setContent("some content".getBytes(StandardCharsets.UTF_8));
		assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class).isThrownBy(() ->
				processor.resolveArgument(paramHttpEntity, mavContainer, webRequest, null));
	}

	@Test
	void shouldHandleReturnValue() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);
		MediaType accepted = TEXT_PLAIN;
		servletRequest.addHeader("Accept", accepted.toString());
		initStringMessageConversion(accepted);

		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertThat(mavContainer.isRequestHandled()).isTrue();
		verify(stringHttpMessageConverter).write(eq(body), eq(accepted), isA(HttpOutputMessage.class));
	}

	@Test
	void shouldHandleErrorResponse() throws Exception {
		ErrorResponseException ex = new ErrorResponseException(HttpStatus.BAD_REQUEST);
		ex.getHeaders().add("foo", "bar");
		servletRequest.addHeader("Accept", APPLICATION_PROBLEM_JSON_VALUE);
		given(jsonMessageConverter.canWrite(ProblemDetail.class, APPLICATION_PROBLEM_JSON)).willReturn(true);

		processor.handleReturnValue(ex, returnTypeProblemDetail, mavContainer, webRequest);

		assertThat(mavContainer.isRequestHandled()).isTrue();
		assertThat(webRequest.getNativeResponse(HttpServletResponse.class).getStatus()).isEqualTo(400);
		verify(jsonMessageConverter).write(eq(ex.getBody()), eq(APPLICATION_PROBLEM_JSON), isA(HttpOutputMessage.class));

		assertThat(ex.getBody()).isNotNull()
				.extracting(ProblemDetail::getInstance).isNotNull()
				.extracting(URI::toString)
				.as("Instance was not set to the request path")
				.isEqualTo(servletRequest.getRequestURI());

		// But if instance is set, it should be respected
		ex.getBody().setInstance(URI.create("/something/else"));
		processor.handleReturnValue(ex, returnTypeProblemDetail, mavContainer, webRequest);

		assertThat(ex.getBody()).isNotNull()
				.extracting(ProblemDetail::getInstance).isNotNull()
				.extracting(URI::toString)
				.as("Instance was not set to the request path")
				.isEqualTo("/something/else");
	}

	@Test
	void shouldHandleProblemDetail() throws Exception {
		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		servletRequest.addHeader("Accept", APPLICATION_PROBLEM_JSON_VALUE);
		given(jsonMessageConverter.canWrite(ProblemDetail.class, APPLICATION_PROBLEM_JSON)).willReturn(true);

		processor.handleReturnValue(problemDetail, returnTypeProblemDetail, mavContainer, webRequest);

		assertThat(mavContainer.isRequestHandled()).isTrue();
		assertThat(webRequest.getNativeResponse(HttpServletResponse.class).getStatus()).isEqualTo(400);
		verify(jsonMessageConverter).write(eq(problemDetail), eq(APPLICATION_PROBLEM_JSON), isA(HttpOutputMessage.class));

		assertThat(problemDetail)
				.extracting(ProblemDetail::getInstance).isNotNull()
				.extracting(URI::toString)
				.as("Instance was not set to the request path")
				.isEqualTo(servletRequest.getRequestURI());


		// But if instance is set, it should be respected
		problemDetail.setInstance(URI.create("/something/else"));
		processor.handleReturnValue(problemDetail, returnTypeProblemDetail, mavContainer, webRequest);

		assertThat(problemDetail).isNotNull()
				.extracting(ProblemDetail::getInstance).isNotNull()
				.extracting(URI::toString)
				.as("Instance was not set to the request path")
				.isEqualTo("/something/else");
	}

	@Test
	void shouldHandleReturnValueWithProducibleMediaType() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);
		servletRequest.addHeader("Accept", "text/*");
		servletRequest.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(MediaType.TEXT_HTML));
		given(stringHttpMessageConverter.canWrite(String.class, MediaType.TEXT_HTML)).willReturn(true);

		processor.handleReturnValue(returnValue, returnTypeResponseEntityProduces, mavContainer, webRequest);

		assertThat(mavContainer.isRequestHandled()).isTrue();
		verify(stringHttpMessageConverter).write(eq(body), eq(MediaType.TEXT_HTML), isA(HttpOutputMessage.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldHandleReturnValueWithResponseBodyAdvice() throws Exception {
		servletRequest.addHeader("Accept", "text/*");
		servletRequest.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(MediaType.TEXT_HTML));
		ResponseEntity<String> returnValue = new ResponseEntity<>(HttpStatus.OK);
		ResponseBodyAdvice<String> advice = mock();
		given(advice.supports(any(), any())).willReturn(true);
		given(advice.beforeBodyWrite(any(), any(), any(), any(), any(), any())).willReturn("Foo");

		HttpEntityMethodProcessor processor = new HttpEntityMethodProcessor(
				Collections.singletonList(stringHttpMessageConverter), null, Collections.singletonList(advice));

		reset(stringHttpMessageConverter);
		given(stringHttpMessageConverter.canWrite(String.class, MediaType.TEXT_HTML)).willReturn(true);

		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertThat(mavContainer.isRequestHandled()).isTrue();
		verify(stringHttpMessageConverter).write(eq("Foo"), eq(MediaType.TEXT_HTML), isA(HttpOutputMessage.class));
	}

	@Test
	void shouldFailHandlingWhenContentTypeNotSupported() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);
		MediaType accepted = MediaType.APPLICATION_ATOM_XML;
		servletRequest.addHeader("Accept", accepted.toString());

		given(stringHttpMessageConverter.canWrite(String.class, null)).willReturn(true);
		given(stringHttpMessageConverter.getSupportedMediaTypes(any()))
				.willReturn(Collections.singletonList(TEXT_PLAIN));

		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class).isThrownBy(() ->
				processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest));
	}

	@Test // gh-23205
	void shouldFailWithServerErrorIfContentTypeFromResponseEntity() {
		ResponseEntity<String> returnValue = ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_XML)
				.body("<foo/>");

		given(stringHttpMessageConverter.canWrite(String.class, TEXT_PLAIN)).willReturn(true);
		given(stringHttpMessageConverter.getSupportedMediaTypes(any())).willReturn(Collections.singletonList(TEXT_PLAIN));

		assertThatThrownBy(() ->
				processor.handleReturnValue(
						returnValue, returnTypeResponseEntity, mavContainer, webRequest))
				.isInstanceOf(HttpMessageNotWritableException.class)
				.hasMessageContaining("with preset Content-Type");
	}

	@Test // gh-23287
	void shouldFailWithServerErrorIfContentTypeFromProducibleAttribute() {
		Set<MediaType> mediaTypes = Collections.singleton(MediaType.APPLICATION_XML);
		servletRequest.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);

		ResponseEntity<String> returnValue = ResponseEntity.ok().body("<foo/>");

		given(stringHttpMessageConverter.canWrite(String.class, TEXT_PLAIN)).willReturn(true);
		given(stringHttpMessageConverter.getSupportedMediaTypes(any())).willReturn(Collections.singletonList(TEXT_PLAIN));

		assertThatThrownBy(() ->
				processor.handleReturnValue(
						returnValue, returnTypeResponseEntity, mavContainer, webRequest))
				.isInstanceOf(HttpMessageNotWritableException.class)
				.hasMessageContaining("with preset Content-Type");
	}

	@Test
	void shouldFailHandlingWhenConverterCannotWrite() throws Exception {
		String body = "Foo";
		ResponseEntity<String> returnValue = new ResponseEntity<>(body, HttpStatus.OK);
		MediaType accepted = TEXT_PLAIN;
		servletRequest.addHeader("Accept", accepted.toString());

		given(stringHttpMessageConverter.canWrite(String.class, null)).willReturn(true);
		given(stringHttpMessageConverter.getSupportedMediaTypes(any()))
				.willReturn(Collections.singletonList(TEXT_PLAIN));
		given(stringHttpMessageConverter.canWrite(String.class, accepted)).willReturn(false);

		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class).isThrownBy(() ->
				processor.handleReturnValue(returnValue, returnTypeResponseEntityProduces, mavContainer, webRequest));
	}

	@Test  // SPR-9142
	void shouldFailHandlingWhenAcceptHeaderIllegal() throws Exception {
		ResponseEntity<String> returnValue = new ResponseEntity<>("Body", HttpStatus.ACCEPTED);
		servletRequest.addHeader("Accept", "01");

		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class).isThrownBy(() ->
				processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest));
	}

	@Test
	void shouldHandleResponseHeaderNoBody() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set("headerName", "headerValue");
		ResponseEntity<String> returnValue = new ResponseEntity<>(headers, HttpStatus.ACCEPTED);

		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertThat(mavContainer.isRequestHandled()).isTrue();
		assertThat(servletResponse.getHeader("headerName")).isEqualTo("headerValue");
	}

	@Test
	void shouldHandleResponseHeaderAndBody() throws Exception {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.set("header", "headerValue");
		ResponseEntity<String> returnValue = new ResponseEntity<>("body", responseHeaders, HttpStatus.ACCEPTED);

		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		ArgumentCaptor<HttpOutputMessage> outputMessage = ArgumentCaptor.forClass(HttpOutputMessage.class);
		verify(stringHttpMessageConverter).write(eq("body"), eq(TEXT_PLAIN), outputMessage.capture());
		assertThat(mavContainer.isRequestHandled()).isTrue();
		assertThat(outputMessage.getValue().getHeaders().get("header").get(0)).isEqualTo("headerValue");
	}

	@Test
	void shouldHandleLastModifiedWithHttp304() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		ZonedDateTime dateTime = ofEpochMilli(currentTime).atZone(GMT);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, RFC_1123_DATE_TIME.format(dateTime));
		ResponseEntity<String> returnValue = ResponseEntity.ok().lastModified(oneMinuteAgo).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, null, oneMinuteAgo);
	}

	@Test
	void handleEtagWithHttp304() throws Exception {
		String etagValue = "\"deadb33f8badf00d\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, -1);
	}

	@Test
	void handleEtagWithHttp304AndEtagFilterHasNoImpact() throws Exception {
		String eTagValue = "\"deadb33f8badf00d\"";

		FilterChain chain = (req, res) -> {
			servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTagValue);
			ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(eTagValue).body("body");
			initStringMessageConversion(TEXT_PLAIN);
			try {
				processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		};

		new ShallowEtagHeaderFilter().doFilter(this.servletRequest, this.servletResponse, chain);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, eTagValue, -1);
	}

	@Test  // SPR-14559
	void shouldHandleInvalidIfNoneMatchWithHttp200() throws Exception {
		String etagValue = "\"deadb33f8badf00d\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "unquoted");
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.OK, "body", etagValue, -1);
	}

	@Test
	void shouldHandleETagAndLastModifiedWithHttp304() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		String etagValue = "\"deadb33f8badf00d\"";
		ZonedDateTime dateTime = ofEpochMilli(currentTime).atZone(GMT);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, RFC_1123_DATE_TIME.format(dateTime));
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok()
				.eTag(etagValue).lastModified(oneMinuteAgo).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, oneMinuteAgo);
	}

	@Test
	void shouldHandleNotModifiedResponse() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		String etagValue = "\"deadb33f8badf00d\"";
		ResponseEntity<String> returnValue = ResponseEntity.status(HttpStatus.NOT_MODIFIED)
				.eTag(etagValue).lastModified(oneMinuteAgo).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, oneMinuteAgo);
	}

	@Test
	void shouldHandleChangedETagAndLastModified() throws Exception {
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		String etagValue = "\"deadb33f8badf00d\"";
		String changedEtagValue = "\"changed-etag-value\"";
		ZonedDateTime dateTime = ofEpochMilli(currentTime).atZone(GMT);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, RFC_1123_DATE_TIME.format(dateTime));
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok()
				.eTag(changedEtagValue).lastModified(oneMinuteAgo).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.OK, null, changedEtagValue, oneMinuteAgo);
	}

	@Test  // SPR-13496
	void shouldHandleConditionalRequestIfNoneMatchWildcard() throws Exception {
		String wildcardValue = "*";
		String etagValue = "\"some-etag\"";
		servletRequest.setMethod("POST");
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, wildcardValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.OK, "body", etagValue, -1);
	}

	@Test  // SPR-13626
	void shouldHandleGetIfNoneMatchWildcard() throws Exception {
		String wildcardValue = "*";
		String etagValue = "\"some-etag\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, wildcardValue);
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.OK, "body", etagValue, -1);
	}

	@Test  // SPR-13626
	void shouldHandleIfNoneMatchIfMatch() throws Exception {
		String etagValue = "\"some-etag\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		servletRequest.addHeader(HttpHeaders.IF_MATCH, "ifmatch");
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, -1);
	}

	@Test  // SPR-13626
	void shouldHandleIfNoneMatchIfUnmodifiedSince() throws Exception {
		String etagValue = "\"some-etag\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etagValue);
		ZonedDateTime dateTime = ofEpochMilli(new Date().getTime()).atZone(GMT);
		servletRequest.addHeader(HttpHeaders.IF_UNMODIFIED_SINCE, RFC_1123_DATE_TIME.format(dateTime));
		ResponseEntity<String> returnValue = ResponseEntity.ok().eTag(etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.NOT_MODIFIED, null, etagValue, -1);
	}

	@Test
	void shouldHandleResource() throws Exception {
		ResponseEntity<Resource> returnValue = ResponseEntity
				.ok(new ByteArrayResource("Content".getBytes(StandardCharsets.UTF_8)));

		given(resourceMessageConverter.canWrite(ByteArrayResource.class, null)).willReturn(true);
		given(resourceMessageConverter.getSupportedMediaTypes(any())).willReturn(Collections.singletonList(MediaType.ALL));
		given(resourceMessageConverter.canWrite(ByteArrayResource.class, APPLICATION_OCTET_STREAM)).willReturn(true);

		processor.handleReturnValue(returnValue, returnTypeResponseEntityResource, mavContainer, webRequest);

		then(resourceMessageConverter).should(times(1)).write(
				any(ByteArrayResource.class), eq(APPLICATION_OCTET_STREAM), any(HttpOutputMessage.class));
		assertThat(servletResponse.getStatus()).isEqualTo(200);
	}

	@Test
	void shouldHandleResourceByteRange() throws Exception {
		ResponseEntity<Resource> returnValue = ResponseEntity
				.ok(new ByteArrayResource("Content".getBytes(StandardCharsets.UTF_8)));
		servletRequest.addHeader("Range", "bytes=0-5");

		given(resourceRegionMessageConverter.canWrite(any(), eq(null))).willReturn(true);
		given(resourceRegionMessageConverter.canWrite(any(), eq(APPLICATION_OCTET_STREAM))).willReturn(true);

		processor.handleReturnValue(returnValue, returnTypeResponseEntityResource, mavContainer, webRequest);

		then(resourceRegionMessageConverter).should(times(1)).write(
				anyCollection(), eq(APPLICATION_OCTET_STREAM),
				argThat(outputMessage -> "bytes".equals(outputMessage.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES))));
		assertThat(servletResponse.getStatus()).isEqualTo(206);
	}

	@Test
	void handleReturnTypeResourceIllegalByteRange() throws Exception {
		ResponseEntity<Resource> returnValue = ResponseEntity
				.ok(new ByteArrayResource("Content".getBytes(StandardCharsets.UTF_8)));
		servletRequest.addHeader("Range", "illegal");

		given(resourceRegionMessageConverter.canWrite(any(), eq(null))).willReturn(true);
		given(resourceRegionMessageConverter.canWrite(any(), eq(APPLICATION_OCTET_STREAM))).willReturn(true);

		processor.handleReturnValue(returnValue, returnTypeResponseEntityResource, mavContainer, webRequest);

		then(resourceRegionMessageConverter).should(never()).write(
				anyCollection(), eq(APPLICATION_OCTET_STREAM), any(HttpOutputMessage.class));
		assertThat(servletResponse.getStatus()).isEqualTo(416);
	}

	@Test //SPR-16754
	void disableRangeSupportForStreamingResponses() throws Exception {
		InputStream is = new ByteArrayInputStream("Content".getBytes(StandardCharsets.UTF_8));
		InputStreamResource resource = new InputStreamResource(is, "test");
		ResponseEntity<Resource> returnValue = ResponseEntity.ok(resource);
		servletRequest.addHeader("Range", "bytes=0-5");

		given(resourceMessageConverter.canWrite(any(), eq(null))).willReturn(true);
		given(resourceMessageConverter.canWrite(any(), eq(APPLICATION_OCTET_STREAM))).willReturn(true);

		processor.handleReturnValue(returnValue, returnTypeResponseEntityResource, mavContainer, webRequest);
		then(resourceMessageConverter).should(times(1)).write(
				any(InputStreamResource.class), eq(APPLICATION_OCTET_STREAM), any(HttpOutputMessage.class));
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader(HttpHeaders.ACCEPT_RANGES)).isNull();
	}

	@Test //SPR-16921
	void disableRangeSupportIfContentRangePresent() throws Exception {
		ResponseEntity<Resource> returnValue = ResponseEntity
				.status(HttpStatus.PARTIAL_CONTENT)
				.header(HttpHeaders.RANGE, "bytes=0-5")
				.body(new ByteArrayResource("Content".getBytes(StandardCharsets.UTF_8)));

		given(resourceRegionMessageConverter.canWrite(any(), eq(null))).willReturn(true);
		given(resourceRegionMessageConverter.canWrite(any(), eq(APPLICATION_OCTET_STREAM))).willReturn(true);

		processor.handleReturnValue(returnValue, returnTypeResponseEntityResource, mavContainer, webRequest);

		then(resourceRegionMessageConverter).should(never()).write(anyCollection(), any(), any());
		assertThat(servletResponse.getStatus()).isEqualTo(206);
	}

	@Test  //SPR-14767
	void shouldHandleValidatorHeadersInputResponses() throws Exception {
		servletRequest.setMethod("PUT");
		String etagValue = "\"some-etag\"";
		ResponseEntity<String> returnValue = ResponseEntity.ok().header(HttpHeaders.ETAG, etagValue).body("body");

		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.OK, "body", etagValue, -1);
	}

	@Test
	void shouldNotFailPreconditionForPutRequests() throws Exception {
		servletRequest.setMethod("PUT");
		ZonedDateTime dateTime = ofEpochMilli(new Date().getTime()).atZone(GMT);
		servletRequest.addHeader(HttpHeaders.IF_UNMODIFIED_SINCE, RFC_1123_DATE_TIME.format(dateTime));

		long justModified = dateTime.plus(1, ChronoUnit.SECONDS).toEpochSecond() * 1000;
		ResponseEntity<String> returnValue = ResponseEntity.ok()
				.lastModified(justModified).body("body");
		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertConditionalResponse(HttpStatus.OK, null, null, justModified);
	}

	@Test
	void varyHeader() throws Exception {
		String[] entityValues = {"Accept-Language", "User-Agent"};
		String[] existingValues = {};
		String[] expected = {"Accept-Language, User-Agent"};
		testVaryHeader(entityValues, existingValues, expected);
	}

	@Test
	void varyHeaderWithExistingWildcard() throws Exception {
		String[] entityValues = {"Accept-Language"};
		String[] existingValues = {"*"};
		String[] expected = {"*"};
		testVaryHeader(entityValues, existingValues, expected);
	}

	@Test
	void varyHeaderWithExistingCommaValues() throws Exception {
		String[] entityValues = {"Accept-Language", "User-Agent"};
		String[] existingValues = {"Accept-Encoding", "Accept-Language"};
		String[] expected = {"Accept-Encoding", "Accept-Language", "User-Agent"};
		testVaryHeader(entityValues, existingValues, expected);
	}

	@Test
	void varyHeaderWithExistingCommaSeparatedValues() throws Exception {
		String[] entityValues = {"Accept-Language", "User-Agent"};
		String[] existingValues = {"Accept-Encoding, Accept-Language"};
		String[] expected = {"Accept-Encoding, Accept-Language", "User-Agent"};
		testVaryHeader(entityValues, existingValues, expected);
	}

	@Test
	void handleReturnValueVaryHeader() throws Exception {
		String[] entityValues = {"Accept-Language", "User-Agent"};
		String[] existingValues = {"Accept-Encoding, Accept-Language"};
		String[] expected = {"Accept-Encoding, Accept-Language", "User-Agent"};
		testVaryHeader(entityValues, existingValues, expected);
	}


	private void testVaryHeader(String[] entityValues, String[] existingValues, String[] expected) throws Exception {
		ResponseEntity<String> returnValue = ResponseEntity.ok().varyBy(entityValues).body("Foo");
		for (String value : existingValues) {
			servletResponse.addHeader("Vary", value);
		}
		initStringMessageConversion(TEXT_PLAIN);
		processor.handleReturnValue(returnValue, returnTypeResponseEntity, mavContainer, webRequest);

		assertThat(mavContainer.isRequestHandled()).isTrue();
		assertThat(servletResponse.getHeaders("Vary")).isEqualTo(Arrays.asList(expected));
		verify(stringHttpMessageConverter).write(eq("Foo"), eq(TEXT_PLAIN), isA(HttpOutputMessage.class));
	}

	private void initStringMessageConversion(MediaType accepted) {
		given(stringHttpMessageConverter.canWrite(String.class, null)).willReturn(true);
		given(stringHttpMessageConverter.getSupportedMediaTypes()).willReturn(Collections.singletonList(TEXT_PLAIN));
		given(stringHttpMessageConverter.getSupportedMediaTypes(String.class)).willReturn(Collections.singletonList(TEXT_PLAIN));
		given(stringHttpMessageConverter.canWrite(String.class, accepted)).willReturn(true);
	}

	private void assertResponseBody(String body) throws IOException {
		ArgumentCaptor<HttpOutputMessage> outputMessage = ArgumentCaptor.forClass(HttpOutputMessage.class);
		verify(stringHttpMessageConverter).write(eq(body), eq(TEXT_PLAIN), outputMessage.capture());
	}

	private void assertConditionalResponse(HttpStatus status, String body, String etag, long lastModified)
			throws IOException {

		assertThat(servletResponse.getStatus()).isEqualTo(status.value());
		assertThat(mavContainer.isRequestHandled()).isTrue();
		if (body != null) {
			assertResponseBody(body);
		}
		else {
			assertThat(servletResponse.getContentAsByteArray()).isEmpty();
		}
		if (etag != null) {
			assertThat(servletResponse.getHeaderValues(HttpHeaders.ETAG)).hasSize(1);
			assertThat(servletResponse.getHeader(HttpHeaders.ETAG)).isEqualTo(etag);
		}
		if (lastModified != -1) {
			assertThat(servletResponse.getHeaderValues(HttpHeaders.LAST_MODIFIED)).hasSize(1);
			assertThat((servletResponse.getDateHeader(HttpHeaders.LAST_MODIFIED) / 1000)).isEqualTo((lastModified / 1000));
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
	public ErrorResponse handle6() {
		return null;
	}

	@SuppressWarnings("unused")
	public ProblemDetail handle7() {
		return null;
	}

	@SuppressWarnings("unused")
	public static class CustomHttpEntity extends HttpEntity<Object> {
	}

}
