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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;

/**
 * Test fixture for {@link HttpMessageConverter}.
 *
 * @author Arjen Poutsma
 */
public class HttpMessageConverterExtractorTests {

	private HttpMessageConverterExtractor extractor;

	private ClientHttpResponse response;

	@Before
	public void createMocks() {
		response = createMock(ClientHttpResponse.class);
	}

	@Test
	public void noContent() throws IOException {
		HttpMessageConverter<?> converter = createMock(HttpMessageConverter.class);

		extractor = new HttpMessageConverterExtractor<String>(String.class, createConverterList(converter));

		expect(response.getStatusCode()).andReturn(HttpStatus.NO_CONTENT);

		replay(response, converter);
		Object result = extractor.extractData(response);

		assertNull(result);
		verify(response, converter);
	}

	@Test
	public void notModified() throws IOException {
		HttpMessageConverter<?> converter = createMock(HttpMessageConverter.class);

		extractor = new HttpMessageConverterExtractor<String>(String.class, createConverterList(converter));

		expect(response.getStatusCode()).andReturn(HttpStatus.NOT_MODIFIED);

		replay(response, converter);
		Object result = extractor.extractData(response);

		assertNull(result);
		verify(response, converter);
	}

	@Test
	public void zeroContentLength() throws IOException {
		HttpMessageConverter<?> converter = createMock(HttpMessageConverter.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentLength(0);

		extractor = new HttpMessageConverterExtractor<String>(String.class, createConverterList(converter));

		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		expect(response.getHeaders()).andReturn(responseHeaders);

		replay(response, converter);
		Object result = extractor.extractData(response);

		assertNull(result);
		verify(response, converter);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void normal() throws IOException {
		HttpMessageConverter<String> converter = createMock(HttpMessageConverter.class);
		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(converter);

		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType contentType = MediaType.TEXT_PLAIN;
		responseHeaders.setContentType(contentType);
		String expected = "Foo";

		extractor = new HttpMessageConverterExtractor<String>(String.class, converters);

		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		expect(response.getHeaders()).andReturn(responseHeaders).times(2);
		expect(converter.canRead(String.class, contentType)).andReturn(true);
		expect(converter.read(String.class, response)).andReturn(expected);

		replay(response, converter);
		Object result = extractor.extractData(response);

		assertEquals(expected, result);
		verify(response, converter);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void cannotRead() throws IOException {
		HttpMessageConverter<String> converter = createMock(HttpMessageConverter.class);
		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(converter);

		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType contentType = MediaType.TEXT_PLAIN;
		responseHeaders.setContentType(contentType);

		extractor = new HttpMessageConverterExtractor<String>(String.class, converters);

		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		expect(response.getHeaders()).andReturn(responseHeaders).times(2);
		expect(converter.canRead(String.class, contentType)).andReturn(false);

		replay(response, converter);
		try {
			extractor.extractData(response);
			fail("RestClientException expected");
		}
		catch (RestClientException expected) {
			// expected
		}

		verify(response, converter);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void generics() throws IOException {
		GenericHttpMessageConverter<String> converter = createMock(GenericHttpMessageConverter.class);
		List<HttpMessageConverter<?>> converters = createConverterList(converter);

		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType contentType = MediaType.TEXT_PLAIN;
		responseHeaders.setContentType(contentType);
		String expected = "Foo";

		ParameterizedTypeReference<List<String>> reference = new ParameterizedTypeReference<List<String>>() {};
		Type type = reference.getType();

		extractor = new HttpMessageConverterExtractor<List<String>>(type, converters);

		expect(response.getStatusCode()).andReturn(HttpStatus.OK);
		expect(response.getHeaders()).andReturn(responseHeaders).times(2);
		expect(converter.canRead(type, null, contentType)).andReturn(true);
		expect(converter.read(type, null, response)).andReturn(expected);

		replay(response, converter);
		Object result = extractor.extractData(response);

		assertEquals(expected, result);
		verify(response, converter);
	}

	private List<HttpMessageConverter<?>> createConverterList(HttpMessageConverter converter) {
		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>(1);
		converters.add(converter);
		return converters;
	}


}
