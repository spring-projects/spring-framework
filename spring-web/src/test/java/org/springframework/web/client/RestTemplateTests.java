/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.http.MediaType.*;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("unchecked")
public class RestTemplateTests {

	private RestTemplate template;

	private ClientHttpRequestFactory requestFactory;

	private ClientHttpRequest request;

	private ClientHttpResponse response;

	private ResponseErrorHandler errorHandler;

	@SuppressWarnings("rawtypes")
	private HttpMessageConverter converter;


	@Before
	public void setup() {
		requestFactory = mock(ClientHttpRequestFactory.class);
		request = mock(ClientHttpRequest.class);
		response = mock(ClientHttpResponse.class);
		errorHandler = mock(ResponseErrorHandler.class);
		converter = mock(HttpMessageConverter.class);
		template = new RestTemplate(Collections.singletonList(converter));
		template.setRequestFactory(requestFactory);
		template.setErrorHandler(errorHandler);
	}


	@Test
	public void varArgsTemplateVariables() throws Exception {
		given(requestFactory.createRequest(new URI("http://example.com/hotels/42/bookings/21"), GET))
				.willReturn(request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		template.execute("http://example.com/hotels/{hotel}/bookings/{booking}", GET, null, null, "42",
				"21");

		verify(response).close();
	}

	@Test
	public void varArgsNullTemplateVariable() throws Exception {
		given(requestFactory.createRequest(new URI("http://example.com/-foo"), GET))
				.willReturn(request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		template.execute("http://example.com/{first}-{last}", GET, null, null, null, "foo");

		verify(response).close();
	}

	@Test
	public void mapTemplateVariables() throws Exception {
		given(requestFactory.createRequest(new URI("http://example.com/hotels/42/bookings/42"), GET))
				.willReturn(request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		Map<String, String> vars = Collections.singletonMap("hotel", "42");
		template.execute("http://example.com/hotels/{hotel}/bookings/{hotel}", GET, null, null, vars);

		verify(response).close();
	}

	@Test
	public void mapNullTemplateVariable() throws Exception {
		given(requestFactory.createRequest(new URI("http://example.com/-foo"), GET))
				.willReturn(request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		Map<String, String> vars = new HashMap<>(2);
		vars.put("first", null);
		vars.put("last", "foo");
		template.execute("http://example.com/{first}-{last}", GET, null, null, vars);

		verify(response).close();
	}

	@Test  // SPR-15201
	public void uriTemplateWithTrailingSlash() throws Exception {
		String url = "http://example.com/spring/";
		given(requestFactory.createRequest(new URI(url), GET)).willReturn(request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		template.execute(url, GET, null, null);

		verify(response).close();
	}

	@Test
	public void errorHandling() throws Exception {
		URI uri = new URI("http://example.com");
		given(requestFactory.createRequest(uri, GET)).willReturn(request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(true);
		given(response.getStatusCode()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR);
		given(response.getStatusText()).willReturn("Internal Server Error");
		willThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
				.given(errorHandler).handleError(uri, GET, response);

		try {
			template.execute("http://example.com", GET, null, null);
			fail("HttpServerErrorException expected");
		}
		catch (HttpServerErrorException ex) {
			// expected
		}

		verify(response).close();
	}

	@Test
	public void getForObject() throws Exception {
		given(converter.canRead(String.class, null)).willReturn(true);
		MediaType textPlain = new MediaType("text", "plain");
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(textPlain));
		given(requestFactory.createRequest(new URI("http://example.com"), GET)).willReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(request.getHeaders()).willReturn(requestHeaders);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		String expected = "Hello World";
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(expected.getBytes()));
		given(converter.canRead(String.class, textPlain)).willReturn(true);
		given(converter.read(eq(String.class), any(HttpInputMessage.class))).willReturn(expected);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		String result = template.getForObject("http://example.com", String.class);
		assertEquals("Invalid GET result", expected, result);
		assertEquals("Invalid Accept header", textPlain.toString(), requestHeaders.getFirst("Accept"));

		verify(response).close();
	}

	@Test
	public void getUnsupportedMediaType() throws Exception {
		given(converter.canRead(String.class, null)).willReturn(true);
		MediaType supportedMediaType = new MediaType("foo", "bar");
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(supportedMediaType));
		given(requestFactory.createRequest(new URI("http://example.com/resource"), GET)).willReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(request.getHeaders()).willReturn(requestHeaders);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType contentType = new MediaType("bar", "baz");
		responseHeaders.setContentType(contentType);
		responseHeaders.setContentLength(10);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream("Foo".getBytes()));
		given(converter.canRead(String.class, contentType)).willReturn(false);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		try {
			template.getForObject("http://example.com/{p}", String.class, "resource");
			fail("UnsupportedMediaTypeException expected");
		}
		catch (RestClientException ex) {
			// expected
		}

		verify(response).close();
	}


	@Test
	public void getForEntity() throws Exception {
		given(converter.canRead(String.class, null)).willReturn(true);
		MediaType textPlain = new MediaType("text", "plain");
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(textPlain));
		given(requestFactory.createRequest(new URI("http://example.com"), GET)).willReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(request.getHeaders()).willReturn(requestHeaders);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		String expected = "Hello World";
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getStatusText()).willReturn(HttpStatus.OK.getReasonPhrase());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(expected.getBytes()));
		given(converter.canRead(String.class, textPlain)).willReturn(true);
		given(converter.read(eq(String.class), any(HttpInputMessage.class))).willReturn(expected);

		ResponseEntity<String> result = template.getForEntity("http://example.com", String.class);
		assertEquals("Invalid GET result", expected, result.getBody());
		assertEquals("Invalid Accept header", textPlain.toString(), requestHeaders.getFirst("Accept"));
		assertEquals("Invalid Content-Type header", textPlain, result.getHeaders().getContentType());
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verify(response).close();
	}

	@Test
	public void getForObjectWithCustomUriTemplateHandler() throws Exception {
		DefaultUriBuilderFactory uriTemplateHandler = new DefaultUriBuilderFactory();
		template.setUriTemplateHandler(uriTemplateHandler);

		URI expectedUri = new URI("http://example.com/hotels/1/pic/pics%2Flogo.png/size/150x150");
		given(requestFactory.createRequest(expectedUri, GET)).willReturn(request);

		given(request.getHeaders()).willReturn(new HttpHeaders());
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);

		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(new HttpHeaders());
		given(response.getBody()).willReturn(StreamUtils.emptyInput());

		Map<String, String> uriVariables = new HashMap<>(2);
		uriVariables.put("hotel", "1");
		uriVariables.put("publicpath", "pics/logo.png");
		uriVariables.put("scale", "150x150");

		String url = "http://example.com/hotels/{hotel}/pic/{publicpath}/size/{scale}";
		template.getForObject(url, String.class, uriVariables);

		verify(response).close();
	}

	@Test
	public void headForHeaders() throws Exception {
		given(requestFactory.createRequest(new URI("http://example.com"), HEAD)).willReturn(request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		given(response.getHeaders()).willReturn(responseHeaders);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		HttpHeaders result = template.headForHeaders("http://example.com");

		assertSame("Invalid headers returned", responseHeaders, result);

		verify(response).close();
	}

	@Test
	public void postForLocation() throws Exception {
		given(requestFactory.createRequest(new URI("http://example.com"), POST)).willReturn(request);
		String helloWorld = "Hello World";
		given(converter.canWrite(String.class, null)).willReturn(true);
		converter.write(helloWorld, null, request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		URI expected = new URI("http://example.com/hotels");
		responseHeaders.setLocation(expected);
		given(response.getHeaders()).willReturn(responseHeaders);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		URI result = template.postForLocation("http://example.com", helloWorld);
		assertEquals("Invalid POST result", expected, result);

		verify(response).close();
	}

	@Test
	public void postForLocationEntityContentType() throws Exception {
		given(requestFactory.createRequest(new URI("http://example.com"), POST)).willReturn(request);
		String helloWorld = "Hello World";
		MediaType contentType = MediaType.TEXT_PLAIN;
		given(converter.canWrite(String.class, contentType)).willReturn(true);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(request.getHeaders()).willReturn(requestHeaders);
		converter.write(helloWorld, contentType, request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		URI expected = new URI("http://example.com/hotels");
		responseHeaders.setLocation(expected);
		given(response.getHeaders()).willReturn(responseHeaders);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(contentType);
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);

		URI result = template.postForLocation("http://example.com", entity);
		assertEquals("Invalid POST result", expected, result);

		verify(response).close();
	}

	@Test
	public void postForLocationEntityCustomHeader() throws Exception {
		given(requestFactory.createRequest(new URI("http://example.com"), POST)).willReturn(request);
		String helloWorld = "Hello World";
		given(converter.canWrite(String.class, null)).willReturn(true);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(request.getHeaders()).willReturn(requestHeaders);
		converter.write(helloWorld, null, request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		URI expected = new URI("http://example.com/hotels");
		responseHeaders.setLocation(expected);
		given(response.getHeaders()).willReturn(responseHeaders);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);

		URI result = template.postForLocation("http://example.com", entity);
		assertEquals("Invalid POST result", expected, result);
		assertEquals("No custom header set", "MyValue", requestHeaders.getFirst("MyHeader"));

		verify(response).close();
	}

	@Test
	public void postForLocationNoLocation() throws Exception {
		given(requestFactory.createRequest(new URI("http://example.com"), POST)).willReturn(request);
		String helloWorld = "Hello World";
		given(converter.canWrite(String.class, null)).willReturn(true);
		converter.write(helloWorld, null, request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		given(response.getHeaders()).willReturn(responseHeaders);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		URI result = template.postForLocation("http://example.com", helloWorld);
		assertNull("Invalid POST result", result);

		verify(response).close();
	}

	@Test
	public void postForLocationNull() throws Exception {
		given(requestFactory.createRequest(new URI("http://example.com"), POST)).willReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(request.getHeaders()).willReturn(requestHeaders);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		given(response.getHeaders()).willReturn(responseHeaders);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		template.postForLocation("http://example.com", null);
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());

		verify(response).close();
	}

	@Test
	public void postForObject() throws Exception {
		MediaType textPlain = new MediaType("text", "plain");
		given(converter.canRead(Integer.class, null)).willReturn(true);
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(textPlain));
		given(requestFactory.createRequest(new URI("http://example.com"), POST)).willReturn(this.request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(this.request.getHeaders()).willReturn(requestHeaders);
		String request = "Hello World";
		given(converter.canWrite(String.class, null)).willReturn(true);
		converter.write(request, null, this.request);
		given(this.request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		Integer expected = 42;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(expected.toString().getBytes()));
		given(converter.canRead(Integer.class, textPlain)).willReturn(true);
		given(converter.read(eq(Integer.class), any(HttpInputMessage.class))).willReturn(expected);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		Integer result = template.postForObject("http://example.com", request, Integer.class);
		assertEquals("Invalid POST result", expected, result);
		assertEquals("Invalid Accept header", textPlain.toString(), requestHeaders.getFirst("Accept"));

		verify(response).close();
	}

	@Test
	public void postForEntity() throws Exception {
		MediaType textPlain = new MediaType("text", "plain");
		given(converter.canRead(Integer.class, null)).willReturn(true);
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(textPlain));
		given(requestFactory.createRequest(new URI("http://example.com"), POST)).willReturn(this.request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(this.request.getHeaders()).willReturn(requestHeaders);
		String request = "Hello World";
		given(converter.canWrite(String.class, null)).willReturn(true);
		converter.write(request, null, this.request);
		given(this.request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		Integer expected = 42;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getStatusText()).willReturn(HttpStatus.OK.getReasonPhrase());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(expected.toString().getBytes()));
		given(converter.canRead(Integer.class, textPlain)).willReturn(true);
		given(converter.read(eq(Integer.class), any(HttpInputMessage.class))).willReturn(expected);

		ResponseEntity<Integer> result = template.postForEntity("http://example.com", request, Integer.class);
		assertEquals("Invalid POST result", expected, result.getBody());
		assertEquals("Invalid Content-Type", textPlain, result.getHeaders().getContentType());
		assertEquals("Invalid Accept header", textPlain.toString(), requestHeaders.getFirst("Accept"));
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verify(response).close();
	}

	@Test
	public void postForObjectNull() throws Exception {
		MediaType textPlain = new MediaType("text", "plain");
		given(converter.canRead(Integer.class, null)).willReturn(true);
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(textPlain));
		given(requestFactory.createRequest(new URI("http://example.com"), POST)).willReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(request.getHeaders()).willReturn(requestHeaders);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getBody()).willReturn(StreamUtils.emptyInput());
		given(converter.canRead(Integer.class, textPlain)).willReturn(true);
		given(converter.read(Integer.class, response)).willReturn(null);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		Integer result = template.postForObject("http://example.com", null, Integer.class);
		assertNull("Invalid POST result", result);
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());

		verify(response).close();
	}

	@Test
	public void postForEntityNull() throws Exception {
		MediaType textPlain = new MediaType("text", "plain");
		given(converter.canRead(Integer.class, null)).willReturn(true);
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(textPlain));
		given(requestFactory.createRequest(new URI("http://example.com"), POST)).willReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(request.getHeaders()).willReturn(requestHeaders);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getStatusText()).willReturn(HttpStatus.OK.getReasonPhrase());
		given(response.getBody()).willReturn(StreamUtils.emptyInput());
		given(converter.canRead(Integer.class, textPlain)).willReturn(true);
		given(converter.read(Integer.class, response)).willReturn(null);

		ResponseEntity<Integer> result = template.postForEntity("http://example.com", null, Integer.class);
		assertFalse("Invalid POST result", result.hasBody());
		assertEquals("Invalid Content-Type", textPlain, result.getHeaders().getContentType());
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verify(response).close();
	}

	@Test
	public void put() throws Exception {
		given(converter.canWrite(String.class, null)).willReturn(true);
		given(requestFactory.createRequest(new URI("http://example.com"), PUT)).willReturn(request);
		String helloWorld = "Hello World";
		converter.write(helloWorld, null, request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		template.put("http://example.com", helloWorld);

		verify(response).close();
	}

	@Test
	public void putNull() throws Exception {
		given(requestFactory.createRequest(new URI("http://example.com"), PUT)).willReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(request.getHeaders()).willReturn(requestHeaders);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		template.put("http://example.com", null);
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());

		verify(response).close();
	}

	@Test
	public void patchForObject() throws Exception {
		MediaType textPlain = new MediaType("text", "plain");
		given(converter.canRead(Integer.class, null)).willReturn(true);
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(textPlain));
		given(requestFactory.createRequest(new URI("http://example.com"), PATCH)).willReturn(this.request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(this.request.getHeaders()).willReturn(requestHeaders);
		String request = "Hello World";
		given(converter.canWrite(String.class, null)).willReturn(true);
		converter.write(request, null, this.request);
		given(this.request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		Integer expected = 42;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(expected.toString().getBytes()));
		given(converter.canRead(Integer.class, textPlain)).willReturn(true);
		given(converter.read(eq(Integer.class), any(HttpInputMessage.class))).willReturn(expected);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		Integer result = template.patchForObject("http://example.com", request, Integer.class);
		assertEquals("Invalid POST result", expected, result);
		assertEquals("Invalid Accept header", textPlain.toString(), requestHeaders.getFirst("Accept"));

		verify(response).close();
	}

	@Test
	public void patchForObjectNull() throws Exception {
		MediaType textPlain = new MediaType("text", "plain");
		given(converter.canRead(Integer.class, null)).willReturn(true);
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(textPlain));
		given(requestFactory.createRequest(new URI("http://example.com"), PATCH)).willReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(request.getHeaders()).willReturn(requestHeaders);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getBody()).willReturn(StreamUtils.emptyInput());
		given(converter.canRead(Integer.class, textPlain)).willReturn(true);
		given(converter.read(Integer.class, response)).willReturn(null);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		Integer result = template.patchForObject("http://example.com", null, Integer.class);
		assertNull("Invalid POST result", result);
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());

		verify(response).close();
	}



	@Test
	public void delete() throws Exception {
		given(requestFactory.createRequest(new URI("http://example.com"), DELETE)).willReturn(request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		template.delete("http://example.com");

		verify(response).close();
	}

	@Test
	public void optionsForAllow() throws Exception {
		given(requestFactory.createRequest(new URI("http://example.com"), OPTIONS)).willReturn(request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		EnumSet<HttpMethod> expected = EnumSet.of(GET, POST);
		responseHeaders.setAllow(expected);
		given(response.getHeaders()).willReturn(responseHeaders);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		Set<HttpMethod> result = template.optionsForAllow("http://example.com");
		assertEquals("Invalid OPTIONS result", expected, result);

		verify(response).close();
	}

	@Test  // SPR-9325, SPR-13860
	public void ioException() throws Exception {
		String url = "http://example.com/resource?access_token=123";

		given(converter.canRead(String.class, null)).willReturn(true);
		MediaType mediaType = new MediaType("foo", "bar");
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(mediaType));
		given(requestFactory.createRequest(new URI(url), GET)).willReturn(request);
		given(request.getHeaders()).willReturn(new HttpHeaders());
		given(request.execute()).willThrow(new IOException("Socket failure"));

		try {
			template.getForObject(url, String.class);
			fail("RestClientException expected");
		}
		catch (ResourceAccessException ex) {
			assertEquals("I/O error on GET request for \"http://example.com/resource\": " +
					"Socket failure; nested exception is java.io.IOException: Socket failure",
					ex.getMessage());
		}
	}

	@Test  // SPR-15900
	public void ioExceptionWithEmptyQueryString() throws Exception {

		// http://example.com/resource?
		URI uri = new URI("http", "example.com", "/resource", "", null);

		given(converter.canRead(String.class, null)).willReturn(true);
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(parseMediaType("foo/bar")));
		given(requestFactory.createRequest(uri, GET)).willReturn(request);
		given(request.getHeaders()).willReturn(new HttpHeaders());
		given(request.execute()).willThrow(new IOException("Socket failure"));

		try {
			template.getForObject(uri, String.class);
			fail("RestClientException expected");
		}
		catch (ResourceAccessException ex) {
			assertEquals("I/O error on GET request for \"http://example.com/resource\": " +
					"Socket failure; nested exception is java.io.IOException: Socket failure",
					ex.getMessage());
		}
	}

	@Test
	public void exchange() throws Exception {
		given(converter.canRead(Integer.class, null)).willReturn(true);
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		given(requestFactory.createRequest(new URI("http://example.com"), POST)).willReturn(this.request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(this.request.getHeaders()).willReturn(requestHeaders);
		given(converter.canWrite(String.class, null)).willReturn(true);
		String body = "Hello World";
		converter.write(body, null, this.request);
		given(this.request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		Integer expected = 42;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		responseHeaders.setContentLength(10);
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getStatusText()).willReturn(HttpStatus.OK.getReasonPhrase());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(expected.toString().getBytes()));
		given(converter.canRead(Integer.class, MediaType.TEXT_PLAIN)).willReturn(true);
		given(converter.read(Integer.class, response)).willReturn(expected);
		given(converter.read(eq(Integer.class), any(HttpInputMessage.class))).willReturn(expected);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> entity = new HttpEntity<>(body, entityHeaders);
		ResponseEntity<Integer> result = template.exchange("http://example.com", POST, entity, Integer.class);
		assertEquals("Invalid POST result", expected, result.getBody());
		assertEquals("Invalid Content-Type", MediaType.TEXT_PLAIN, result.getHeaders().getContentType());
		assertEquals("Invalid Accept header", MediaType.TEXT_PLAIN_VALUE, requestHeaders.getFirst("Accept"));
		assertEquals("Invalid custom header", "MyValue", requestHeaders.getFirst("MyHeader"));
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verify(response).close();
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void exchangeParameterizedType() throws Exception {
		GenericHttpMessageConverter converter = mock(GenericHttpMessageConverter.class);
		template.setMessageConverters(Collections.<HttpMessageConverter<?>>singletonList(converter));

		ParameterizedTypeReference<List<Integer>> intList = new ParameterizedTypeReference<List<Integer>>() {};
		given(converter.canRead(intList.getType(), null, null)).willReturn(true);
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		given(requestFactory.createRequest(new URI("http://example.com"), POST)).willReturn(this.request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(this.request.getHeaders()).willReturn(requestHeaders);
		given(converter.canWrite(String.class, String.class, null)).willReturn(true);
		String requestBody = "Hello World";
		converter.write(requestBody, String.class, null, this.request);
		given(this.request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		List<Integer> expected = Collections.singletonList(42);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		responseHeaders.setContentLength(10);
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		given(response.getStatusText()).willReturn(HttpStatus.OK.getReasonPhrase());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(Integer.toString(42).getBytes()));
		given(converter.canRead(intList.getType(), null, MediaType.TEXT_PLAIN)).willReturn(true);
		given(converter.read(eq(intList.getType()), eq(null), any(HttpInputMessage.class))).willReturn(expected);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, entityHeaders);
		ResponseEntity<List<Integer>> result = template.exchange("http://example.com", POST, requestEntity, intList);
		assertEquals("Invalid POST result", expected, result.getBody());
		assertEquals("Invalid Content-Type", MediaType.TEXT_PLAIN, result.getHeaders().getContentType());
		assertEquals("Invalid Accept header", MediaType.TEXT_PLAIN_VALUE, requestHeaders.getFirst("Accept"));
		assertEquals("Invalid custom header", "MyValue", requestHeaders.getFirst("MyHeader"));
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verify(response).close();
	}

	@Test  // SPR-15066
	public void requestInterceptorCanAddExistingHeaderValueWithoutBody() throws Exception {
		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
			request.getHeaders().add("MyHeader", "MyInterceptorValue");
			return execution.execute(request, body);
		};
		template.setInterceptors(Collections.singletonList(interceptor));

		given(requestFactory.createRequest(new URI("http://example.com"), POST)).willReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		given(request.getHeaders()).willReturn(requestHeaders);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.add("MyHeader", "MyEntityValue");
		HttpEntity<Void> entity = new HttpEntity<>(null, entityHeaders);
		template.exchange("http://example.com", POST, entity, Void.class);
		assertThat(requestHeaders.get("MyHeader"), contains("MyEntityValue", "MyInterceptorValue"));

		verify(response).close();
	}

	@Test  // SPR-15066
	public void requestInterceptorCanAddExistingHeaderValueWithBody() throws Exception {
		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
			request.getHeaders().add("MyHeader", "MyInterceptorValue");
			return execution.execute(request, body);
		};
		template.setInterceptors(Collections.singletonList(interceptor));

		MediaType contentType = MediaType.TEXT_PLAIN;
		given(converter.canWrite(String.class, contentType)).willReturn(true);
		given(requestFactory.createRequest(new URI("http://example.com"), POST)).willReturn(request);
		String helloWorld = "Hello World";
		HttpHeaders requestHeaders = new HttpHeaders();
		given(request.getHeaders()).willReturn(requestHeaders);
		converter.write(helloWorld, contentType, request);
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(false);
		HttpStatus status = HttpStatus.OK;
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(contentType);
		entityHeaders.add("MyHeader", "MyEntityValue");
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);
		template.exchange("http://example.com", POST, entity, Void.class);
		assertThat(requestHeaders.get("MyHeader"), contains("MyEntityValue", "MyInterceptorValue"));

		verify(response).close();
	}

}
