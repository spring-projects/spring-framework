/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

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
		response = mock(ClientHttpResponse.class);
	}

	@Test
	public void noContent() throws IOException {
		HttpMessageConverter<?> converter = mock(HttpMessageConverter.class);
		extractor = new HttpMessageConverterExtractor<String>(String.class, createConverterList(converter));
		given(response.getStatusCode()).willReturn(HttpStatus.NO_CONTENT);

		Object result = extractor.extractData(response);

		assertNull(result);
	}

	@Test
	public void notModified() throws IOException {
		HttpMessageConverter<?> converter = mock(HttpMessageConverter.class);
		extractor = new HttpMessageConverterExtractor<String>(String.class, createConverterList(converter));
		given(response.getStatusCode()).willReturn(HttpStatus.NOT_MODIFIED);

		Object result = extractor.extractData(response);

		assertNull(result);
	}

	@Test
	public void zeroContentLength() throws IOException {
		HttpMessageConverter<?> converter = mock(HttpMessageConverter.class);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentLength(0);
		extractor = new HttpMessageConverterExtractor<String>(String.class, createConverterList(converter));
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);

		Object result = extractor.extractData(response);

		assertNull(result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void normal() throws IOException {
		HttpMessageConverter<String> converter = mock(HttpMessageConverter.class);
		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(converter);
		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType contentType = MediaType.TEXT_PLAIN;
		responseHeaders.setContentType(contentType);
		String expected = "Foo";
		extractor = new HttpMessageConverterExtractor<String>(String.class, converters);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(converter.canRead(String.class, contentType)).willReturn(true);
		given(converter.read(String.class, response)).willReturn(expected);

		Object result = extractor.extractData(response);

		assertEquals(expected, result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void cannotRead() throws IOException {
		HttpMessageConverter<String> converter = mock(HttpMessageConverter.class);
		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.add(converter);
		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType contentType = MediaType.TEXT_PLAIN;
		responseHeaders.setContentType(contentType);
		extractor = new HttpMessageConverterExtractor<String>(String.class, converters);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(converter.canRead(String.class, contentType)).willReturn(false);

		try {
			extractor.extractData(response);
			fail("RestClientException expected");
		}
		catch (RestClientException expected) {
			// expected
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void generics() throws IOException {
		GenericHttpMessageConverter<String> converter = mock(GenericHttpMessageConverter.class);
		List<HttpMessageConverter<?>> converters = createConverterList(converter);
		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType contentType = MediaType.TEXT_PLAIN;
		responseHeaders.setContentType(contentType);
		String expected = "Foo";
		ParameterizedTypeReference<List<String>> reference = new ParameterizedTypeReference<List<String>>() {};
		Type type = reference.getType();
		extractor = new HttpMessageConverterExtractor<List<String>>(type, converters);
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(converter.canRead(type, null, contentType)).willReturn(true);
		given(converter.read(type, null, response)).willReturn(expected);

		Object result = extractor.extractData(response);

		assertEquals(expected, result);
	}

	private List<HttpMessageConverter<?>> createConverterList(HttpMessageConverter converter) {
		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>(1);
		converters.add(converter);
		return converters;
	}


}
