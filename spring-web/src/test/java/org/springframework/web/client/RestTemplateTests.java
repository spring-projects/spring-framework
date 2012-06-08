/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.client;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;

/** @author Arjen Poutsma */
@SuppressWarnings("unchecked")
public class RestTemplateTests {

	private RestTemplate template;

	private ClientHttpRequestFactory requestFactory;

	private ClientHttpRequest request;

	private ClientHttpResponse response;

	private ResponseErrorHandler errorHandler;

	private HttpMessageConverter converter;

	@Before
	public void setUp() {
		requestFactory = createMock(ClientHttpRequestFactory.class);
		request = createMock(ClientHttpRequest.class);
		response = createMock(ClientHttpResponse.class);
		errorHandler = createMock(ResponseErrorHandler.class);
		converter = createMock(HttpMessageConverter.class);
		template = new RestTemplate(requestFactory);
		template.setErrorHandler(errorHandler);
		template.setMessageConverters(Collections.<HttpMessageConverter<?>>singletonList(converter));
	}

	@Test
	public void varArgsTemplateVariables() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com/hotels/42/bookings/21"), HttpMethod.GET))
				.andReturn(request);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		response.close();

		replayMocks();

		template.execute("http://example.com/hotels/{hotel}/bookings/{booking}", HttpMethod.GET, null, null, "42",
				"21");

		verifyMocks();
	}
	
	@Test
	public void varArgsNullTemplateVariable() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com/-foo"), HttpMethod.GET))
				.andReturn(request);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		response.close();

		replayMocks();

		template.execute("http://example.com/{first}-{last}", HttpMethod.GET, null, null, null, "foo");

		verifyMocks();
	}

	@Test
	public void mapTemplateVariables() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com/hotels/42/bookings/42"), HttpMethod.GET))
				.andReturn(request);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		response.close();

		replayMocks();

		Map<String, String> vars = Collections.singletonMap("hotel", "42");
		template.execute("http://example.com/hotels/{hotel}/bookings/{hotel}", HttpMethod.GET, null, null, vars);

		verifyMocks();
	}

	@Test
	public void mapNullTemplateVariable() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com/-foo"), HttpMethod.GET))
				.andReturn(request);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		response.close();

		replayMocks();

		Map<String, String> vars = new HashMap<String, String>(2);
		vars.put("first", null);
		vars.put("last", "foo");
		template.execute("http://example.com/{first}-{last}", HttpMethod.GET, null, null, vars);

		verifyMocks();
	}

	@Test
	public void errorHandling() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET)).andReturn(request);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(true);
		expect(response.getStatusCode()).andReturn(HttpStatus.INTERNAL_SERVER_ERROR);
		expect(response.getStatusText()).andReturn("Internal Server Error");
		errorHandler.handleError(response);
		expectLastCall().andThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
		response.close();

		replayMocks();

		try {
			template.execute("http://example.com", HttpMethod.GET, null, null);
			fail("HttpServerErrorException expected");
		}
		catch (HttpServerErrorException ex) {
			// expected
		}
		verifyMocks();
	}

	@Test
	public void getForObject() throws Exception {
		expect(converter.canRead(String.class, null)).andReturn(true);
		MediaType textPlain = new MediaType("text", "plain");
		expect(converter.getSupportedMediaTypes()).andReturn(Collections.singletonList(textPlain));
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET)).andReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		expect(request.getHeaders()).andReturn(requestHeaders);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		expect(response.getHeaders()).andReturn(responseHeaders).times(2);
		expect(converter.canRead(String.class, textPlain)).andReturn(true);
		String expected = "Hello World";
		expect(converter.read(String.class, response)).andReturn(expected);
		response.close();

		replayMocks();

		String result = template.getForObject("http://example.com", String.class);
		assertEquals("Invalid GET result", expected, result);
		assertEquals("Invalid Accept header", textPlain.toString(), requestHeaders.getFirst("Accept"));

		verifyMocks();
	}

	@Test
	public void getUnsupportedMediaType() throws Exception {
		expect(converter.canRead(String.class, null)).andReturn(true);
		MediaType supportedMediaType = new MediaType("foo", "bar");
		expect(converter.getSupportedMediaTypes()).andReturn(Collections.singletonList(supportedMediaType));
		expect(requestFactory.createRequest(new URI("http://example.com/resource"), HttpMethod.GET)).andReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		expect(request.getHeaders()).andReturn(requestHeaders);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType contentType = new MediaType("bar", "baz");
		responseHeaders.setContentType(contentType);
		responseHeaders.setContentLength(10);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		expect(response.getHeaders()).andReturn(responseHeaders).times(2);
		expect(converter.canRead(String.class, contentType)).andReturn(false);
		response.close();

		replayMocks();

		try {
			template.getForObject("http://example.com/{p}", String.class, "resource");
			fail("UnsupportedMediaTypeException expected");
		}
		catch (RestClientException ex) {
			// expected
		}
		verifyMocks();
	}


	@Test
	public void getForEntity() throws Exception {
		expect(converter.canRead(String.class, null)).andReturn(true);
		MediaType textPlain = new MediaType("text", "plain");
		expect(converter.getSupportedMediaTypes()).andReturn(Collections.singletonList(textPlain));
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.GET)).andReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		expect(request.getHeaders()).andReturn(requestHeaders);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		expect(response.getHeaders()).andReturn(responseHeaders).times(3);
		expect(converter.canRead(String.class, textPlain)).andReturn(true);
		String expected = "Hello World";
		expect(converter.read(String.class, response)).andReturn(expected);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		response.close();

		replayMocks();

		ResponseEntity<String> result = template.getForEntity("http://example.com", String.class);
		assertEquals("Invalid GET result", expected, result.getBody());
		assertEquals("Invalid Accept header", textPlain.toString(), requestHeaders.getFirst("Accept"));
		assertEquals("Invalid Content-Type header", textPlain, result.getHeaders().getContentType());
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verifyMocks();
	}


	@Test
	public void headForHeaders() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.HEAD)).andReturn(request);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		expect(response.getHeaders()).andReturn(responseHeaders);
		response.close();

		replayMocks();
		HttpHeaders result = template.headForHeaders("http://example.com");

		assertSame("Invalid headers returned", responseHeaders, result);

		verifyMocks();
	}

	@Test
	public void postForLocation() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.POST)).andReturn(request);
		String helloWorld = "Hello World";
		expect(converter.canWrite(String.class, null)).andReturn(true);
		converter.write(helloWorld, null, request);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		URI expected = new URI("http://example.com/hotels");
		responseHeaders.setLocation(expected);
		expect(response.getHeaders()).andReturn(responseHeaders);
		response.close();

		replayMocks();

		URI result = template.postForLocation("http://example.com", helloWorld);
		assertEquals("Invalid POST result", expected, result);

		verifyMocks();
	}

	@Test
	public void postForLocationEntityContentType() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.POST)).andReturn(request);
		String helloWorld = "Hello World";
		MediaType contentType = MediaType.TEXT_PLAIN;
		expect(converter.canWrite(String.class, contentType)).andReturn(true);
		HttpHeaders requestHeaders = new HttpHeaders();
		expect(request.getHeaders()).andReturn(requestHeaders);
		converter.write(helloWorld, contentType, request);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		URI expected = new URI("http://example.com/hotels");
		responseHeaders.setLocation(expected);
		expect(response.getHeaders()).andReturn(responseHeaders);
		response.close();

		replayMocks();

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(contentType);
		HttpEntity<String> entity = new HttpEntity<String>(helloWorld, entityHeaders);

		URI result = template.postForLocation("http://example.com", entity);
		assertEquals("Invalid POST result", expected, result);

		verifyMocks();
	}

	@Test
	public void postForLocationEntityCustomHeader() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.POST)).andReturn(request);
		String helloWorld = "Hello World";
		expect(converter.canWrite(String.class, null)).andReturn(true);
		HttpHeaders requestHeaders = new HttpHeaders();
		expect(request.getHeaders()).andReturn(requestHeaders);
		converter.write(helloWorld, null, request);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		URI expected = new URI("http://example.com/hotels");
		responseHeaders.setLocation(expected);
		expect(response.getHeaders()).andReturn(responseHeaders);
		response.close();

		replayMocks();

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> entity = new HttpEntity<String>(helloWorld, entityHeaders);

		URI result = template.postForLocation("http://example.com", entity);
		assertEquals("Invalid POST result", expected, result);
		assertEquals("No custom header set", "MyValue", requestHeaders.getFirst("MyHeader"));

		verifyMocks();
	}

	@Test
	public void postForLocationNoLocation() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.POST)).andReturn(request);
		String helloWorld = "Hello World";
		expect(converter.canWrite(String.class, null)).andReturn(true);
		converter.write(helloWorld, null, request);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		expect(response.getHeaders()).andReturn(responseHeaders);
		response.close();

		replayMocks();

		URI result = template.postForLocation("http://example.com", helloWorld);
		assertNull("Invalid POST result", result);

		verifyMocks();
	}

	@Test
	public void postForLocationNull() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.POST)).andReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		expect(request.getHeaders()).andReturn(requestHeaders);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		expect(response.getHeaders()).andReturn(responseHeaders);
		response.close();

		replayMocks();
		template.postForLocation("http://example.com", null);
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());

		verifyMocks();
	}

	@Test
	public void postForObject() throws Exception {
		MediaType textPlain = new MediaType("text", "plain");
		expect(converter.canRead(Integer.class, null)).andReturn(true);
		expect(converter.getSupportedMediaTypes()).andReturn(Collections.singletonList(textPlain));
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.POST)).andReturn(this.request);
		HttpHeaders requestHeaders = new HttpHeaders();
		expect(this.request.getHeaders()).andReturn(requestHeaders);
		String request = "Hello World";
		expect(converter.canWrite(String.class, null)).andReturn(true);
		converter.write(request, null, this.request);
		expect(this.request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		expect(response.getHeaders()).andReturn(responseHeaders).times(2);
		Integer expected = 42;
		expect(converter.canRead(Integer.class, textPlain)).andReturn(true);
		expect(converter.read(Integer.class, response)).andReturn(expected);
		response.close();

		replayMocks();

		Integer result = template.postForObject("http://example.com", request, Integer.class);
		assertEquals("Invalid POST result", expected, result);
		assertEquals("Invalid Accept header", textPlain.toString(), requestHeaders.getFirst("Accept"));

		verifyMocks();
	}

	@Test
	public void postForEntity() throws Exception {
		MediaType textPlain = new MediaType("text", "plain");
		expect(converter.canRead(Integer.class, null)).andReturn(true);
		expect(converter.getSupportedMediaTypes()).andReturn(Collections.singletonList(textPlain));
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.POST)).andReturn(this.request);
		HttpHeaders requestHeaders = new HttpHeaders();
		expect(this.request.getHeaders()).andReturn(requestHeaders);
		String request = "Hello World";
		expect(converter.canWrite(String.class, null)).andReturn(true);
		converter.write(request, null, this.request);
		expect(this.request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		expect(response.getHeaders()).andReturn(responseHeaders).times(3);
		Integer expected = 42;
		expect(converter.canRead(Integer.class, textPlain)).andReturn(true);
		expect(converter.read(Integer.class, response)).andReturn(expected);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		response.close();

		replayMocks();

		ResponseEntity<Integer> result = template.postForEntity("http://example.com", request, Integer.class);
		assertEquals("Invalid POST result", expected, result.getBody());
		assertEquals("Invalid Content-Type", textPlain, result.getHeaders().getContentType());
		assertEquals("Invalid Accept header", textPlain.toString(), requestHeaders.getFirst("Accept"));
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verifyMocks();
	}

	@Test
	public void postForObjectNull() throws Exception {
		MediaType textPlain = new MediaType("text", "plain");
		expect(converter.canRead(Integer.class, null)).andReturn(true);
		expect(converter.getSupportedMediaTypes()).andReturn(Collections.singletonList(textPlain));
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.POST)).andReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		expect(request.getHeaders()).andReturn(requestHeaders).times(2);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		expect(response.getHeaders()).andReturn(responseHeaders).times(2);
		expect(converter.canRead(Integer.class, textPlain)).andReturn(true);
		expect(converter.read(Integer.class, response)).andReturn(null);
		response.close();

		replayMocks();
		Integer result = template.postForObject("http://example.com", null, Integer.class);
		assertNull("Invalid POST result", result);
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());

		verifyMocks();
	}
	
	@Test
	public void postForEntityNull() throws Exception {
		MediaType textPlain = new MediaType("text", "plain");
		expect(converter.canRead(Integer.class, null)).andReturn(true);
		expect(converter.getSupportedMediaTypes()).andReturn(Collections.singletonList(textPlain));
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.POST)).andReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		expect(request.getHeaders()).andReturn(requestHeaders).times(2);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(textPlain);
		responseHeaders.setContentLength(10);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		expect(response.getHeaders()).andReturn(responseHeaders).times(3);
		expect(converter.canRead(Integer.class, textPlain)).andReturn(true);
		expect(converter.read(Integer.class, response)).andReturn(null);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		response.close();

		replayMocks();
		ResponseEntity<Integer> result = template.postForEntity("http://example.com", null, Integer.class);
		assertFalse("Invalid POST result", result.hasBody());
		assertEquals("Invalid Content-Type", textPlain, result.getHeaders().getContentType());
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verifyMocks();
	}

	@Test
	public void put() throws Exception {
		expect(converter.canWrite(String.class, null)).andReturn(true);
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.PUT)).andReturn(request);
		String helloWorld = "Hello World";
		converter.write(helloWorld, null, request);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		response.close();

		replayMocks();

		template.put("http://example.com", helloWorld);

		verifyMocks();
	}

	@Test
	public void putNull() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.PUT)).andReturn(request);
		HttpHeaders requestHeaders = new HttpHeaders();
		expect(request.getHeaders()).andReturn(requestHeaders);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		response.close();

		replayMocks();
		template.put("http://example.com", null);
		assertEquals("Invalid content length", 0, requestHeaders.getContentLength());

		verifyMocks();
	}

	@Test
	public void delete() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.DELETE)).andReturn(request);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		response.close();

		replayMocks();

		template.delete("http://example.com");

		verifyMocks();
	}

	@Test
	public void optionsForAllow() throws Exception {
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.OPTIONS)).andReturn(request);
		expect(request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		EnumSet<HttpMethod> expected = EnumSet.of(HttpMethod.GET, HttpMethod.POST);
		responseHeaders.setAllow(expected);
		expect(response.getHeaders()).andReturn(responseHeaders);
		response.close();

		replayMocks();

		Set<HttpMethod> result = template.optionsForAllow("http://example.com");
		assertEquals("Invalid OPTIONS result", expected, result);

		verifyMocks();
	}

	@Test
	public void ioException() throws Exception {
		expect(converter.canRead(String.class, null)).andReturn(true);
		MediaType mediaType = new MediaType("foo", "bar");
		expect(converter.getSupportedMediaTypes()).andReturn(Collections.singletonList(mediaType));
		expect(requestFactory.createRequest(new URI("http://example.com/resource"), HttpMethod.GET)).andReturn(request);
		expect(request.getHeaders()).andReturn(new HttpHeaders());
		expect(request.execute()).andThrow(new IOException());

		replayMocks();

		try {
			template.getForObject("http://example.com/resource", String.class);
			fail("RestClientException expected");
		}
		catch (ResourceAccessException ex) {
			// expected
		}

		verifyMocks();
	}

	@Test
	public void exchange() throws Exception {
		expect(converter.canRead(Integer.class, null)).andReturn(true);
		expect(converter.getSupportedMediaTypes()).andReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.POST)).andReturn(this.request);
		HttpHeaders requestHeaders = new HttpHeaders();
		expect(this.request.getHeaders()).andReturn(requestHeaders).times(2);
		expect(converter.canWrite(String.class, null)).andReturn(true);
		String body = "Hello World";
		converter.write(body, null, this.request);
		expect(this.request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		responseHeaders.setContentLength(10);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		expect(response.getHeaders()).andReturn(responseHeaders).times(3);
		Integer expected = 42;
		expect(converter.canRead(Integer.class, MediaType.TEXT_PLAIN)).andReturn(true);
		expect(converter.read(Integer.class, response)).andReturn(expected);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		response.close();

		replayMocks();

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> requestEntity = new HttpEntity<String>(body, entityHeaders);
		ResponseEntity<Integer> result = template.exchange("http://example.com", HttpMethod.POST, requestEntity, Integer.class);
		assertEquals("Invalid POST result", expected, result.getBody());
		assertEquals("Invalid Content-Type", MediaType.TEXT_PLAIN, result.getHeaders().getContentType());
		assertEquals("Invalid Accept header", MediaType.TEXT_PLAIN_VALUE, requestHeaders.getFirst("Accept"));
		assertEquals("Invalid custom header", "MyValue", requestHeaders.getFirst("MyHeader"));
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verifyMocks();
	}

	@Test
	public void exchangeParameterizedType() throws Exception {
		GenericHttpMessageConverter converter = createMock(GenericHttpMessageConverter.class);
		template.setMessageConverters(Collections.<HttpMessageConverter<?>>singletonList(converter));

		ParameterizedTypeReference<List<Integer>> intList = new ParameterizedTypeReference<List<Integer>>() {};
		expect(converter.canRead(intList.getType(), null)).andReturn(true);
		expect(converter.getSupportedMediaTypes()).andReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		expect(requestFactory.createRequest(new URI("http://example.com"), HttpMethod.POST)).andReturn(this.request);
		HttpHeaders requestHeaders = new HttpHeaders();
		expect(this.request.getHeaders()).andReturn(requestHeaders).times(2);
		expect(converter.canWrite(String.class, null)).andReturn(true);
		String requestBody = "Hello World";
		converter.write(requestBody, null, this.request);
		expect(this.request.execute()).andReturn(response);
		expect(errorHandler.hasError(response)).andReturn(false);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		responseHeaders.setContentLength(10);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		expect(response.getHeaders()).andReturn(responseHeaders).times(3);
		List<Integer> expected = Collections.singletonList(42);
		expect(converter.canRead(intList.getType(), MediaType.TEXT_PLAIN)).andReturn(true);
		expect(converter.read(intList.getType(), response)).andReturn(expected);
		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		response.close();

		replay(requestFactory, request, response, errorHandler, converter);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> requestEntity = new HttpEntity<String>(requestBody, entityHeaders);
		ResponseEntity<List<Integer>> result = template.exchange("http://example.com", HttpMethod.POST, requestEntity, intList);
		assertEquals("Invalid POST result", expected, result.getBody());
		assertEquals("Invalid Content-Type", MediaType.TEXT_PLAIN, result.getHeaders().getContentType());
		assertEquals("Invalid Accept header", MediaType.TEXT_PLAIN_VALUE, requestHeaders.getFirst("Accept"));
		assertEquals("Invalid custom header", "MyValue", requestHeaders.getFirst("MyHeader"));
		assertEquals("Invalid status code", HttpStatus.OK, result.getStatusCode());

		verify(requestFactory, request, response, errorHandler, converter);
	}


	private void replayMocks() {
		replay(requestFactory, request, response, errorHandler, converter);
	}

	private void verifyMocks() {
		verify(requestFactory, request, response, errorHandler, converter);
	}


}
