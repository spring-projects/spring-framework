/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.client;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Arjen Poutsma
 */
public class ExtractingResponseErrorHandlerTests {

	private ExtractingResponseErrorHandler errorHandler;

	private final ClientHttpResponse response = mock(ClientHttpResponse.class);


	@Before
	public void setup() throws Exception {
		HttpMessageConverter<Object> converter = new MappingJackson2HttpMessageConverter();
		this.errorHandler = new ExtractingResponseErrorHandler(
				Collections.singletonList(converter));

		this.errorHandler.setStatusMapping(
				Collections.singletonMap(HttpStatus.I_AM_A_TEAPOT, MyRestClientException.class));
		this.errorHandler.setSeriesMapping(Collections
				.singletonMap(HttpStatus.Series.SERVER_ERROR, MyRestClientException.class));
	}


	@Test
	public void hasError() throws Exception {
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT.value());
		assertTrue(this.errorHandler.hasError(this.response));

		given(this.response.getRawStatusCode()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
		assertTrue(this.errorHandler.hasError(this.response));

		given(this.response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		assertFalse(this.errorHandler.hasError(this.response));
	}

	@Test
	public void hasErrorOverride() throws Exception {
		this.errorHandler.setSeriesMapping(Collections
				.singletonMap(HttpStatus.Series.CLIENT_ERROR, null));

		given(this.response.getRawStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT.value());
		assertTrue(this.errorHandler.hasError(this.response));

		given(this.response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		assertFalse(this.errorHandler.hasError(this.response));

		given(this.response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		assertFalse(this.errorHandler.hasError(this.response));
	}

	@Test
	public void handleErrorStatusMatch() throws Exception {
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT.value());
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		given(this.response.getHeaders()).willReturn(responseHeaders);

		byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
		responseHeaders.setContentLength(body.length);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(body));

		try {
			this.errorHandler.handleError(this.response);
			fail("MyRestClientException expected");
		}
		catch (MyRestClientException ex) {
			assertEquals("bar", ex.getFoo());
		}
	}

	@Test
	public void handleErrorSeriesMatch() throws Exception {
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		given(this.response.getHeaders()).willReturn(responseHeaders);

		byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
		responseHeaders.setContentLength(body.length);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(body));

		try {
			this.errorHandler.handleError(this.response);
			fail("MyRestClientException expected");
		}
		catch (MyRestClientException ex) {
			assertEquals("bar", ex.getFoo());
		}
	}

	@Test
	public void handleNoMatch() throws Exception {
		given(this.response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		given(this.response.getHeaders()).willReturn(responseHeaders);

		byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
		responseHeaders.setContentLength(body.length);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(body));

		try {
			this.errorHandler.handleError(this.response);
			fail("HttpClientErrorException expected");
		}
		catch (HttpClientErrorException ex) {
			assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
			assertArrayEquals(body, ex.getResponseBodyAsByteArray());
		}
	}

	@Test
	public void handleNoMatchOverride() throws Exception {
		this.errorHandler.setSeriesMapping(Collections
				.singletonMap(HttpStatus.Series.CLIENT_ERROR, null));

		given(this.response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		given(this.response.getHeaders()).willReturn(responseHeaders);

		byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
		responseHeaders.setContentLength(body.length);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(body));

		this.errorHandler.handleError(this.response);
	}


	@SuppressWarnings("serial")
	private static class MyRestClientException extends RestClientException {

		private String foo;

		public MyRestClientException(String msg) {
			super(msg);
		}

		public MyRestClientException(String msg, Throwable ex) {
			super(msg, ex);
		}

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

}
