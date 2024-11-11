/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
class ExtractingResponseErrorHandlerTests {

	private ExtractingResponseErrorHandler errorHandler;

	private final ClientHttpResponse response = mock();


	@BeforeEach
	void setup() {
		HttpMessageConverter<Object> converter = new MappingJackson2HttpMessageConverter();
		this.errorHandler = new ExtractingResponseErrorHandler(
				Collections.singletonList(converter));

		this.errorHandler.setStatusMapping(
				Collections.singletonMap(HttpStatus.I_AM_A_TEAPOT, MyRestClientException.class));
		this.errorHandler.setSeriesMapping(Collections
				.singletonMap(HttpStatus.Series.SERVER_ERROR, MyRestClientException.class));
	}


	@Test
	void hasError() throws Exception {
		given(this.response.getStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT);
		assertThat(this.errorHandler.hasError(this.response)).isTrue();

		given(this.response.getStatusCode()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(this.errorHandler.hasError(this.response)).isTrue();

		given(this.response.getStatusCode()).willReturn(HttpStatus.OK);
		assertThat(this.errorHandler.hasError(this.response)).isFalse();
	}

	@Test
	void hasErrorOverride() throws Exception {
		this.errorHandler.setSeriesMapping(Collections
				.singletonMap(HttpStatus.Series.CLIENT_ERROR, null));

		given(this.response.getStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT);
		assertThat(this.errorHandler.hasError(this.response)).isTrue();

		given(this.response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		assertThat(this.errorHandler.hasError(this.response)).isFalse();

		given(this.response.getStatusCode()).willReturn(HttpStatus.OK);
		assertThat(this.errorHandler.hasError(this.response)).isFalse();
	}

	@Test
	void handleErrorStatusMatch() throws Exception {
		given(this.response.getStatusCode()).willReturn(HttpStatus.I_AM_A_TEAPOT);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		given(this.response.getHeaders()).willReturn(responseHeaders);

		byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
		responseHeaders.setContentLength(body.length);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(body));

		assertThatExceptionOfType(MyRestClientException.class).isThrownBy(() ->
				this.errorHandler.handleError(this.response))
			.satisfies(ex -> assertThat(ex.getFoo()).isEqualTo("bar"));
	}

	@Test
	void handleErrorSeriesMatch() throws Exception {
		given(this.response.getStatusCode()).willReturn(HttpStatus.INTERNAL_SERVER_ERROR);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		given(this.response.getHeaders()).willReturn(responseHeaders);

		byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
		responseHeaders.setContentLength(body.length);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(body));

		assertThatExceptionOfType(MyRestClientException.class).isThrownBy(() ->
				this.errorHandler.handleError(this.response))
			.satisfies(ex -> assertThat(ex.getFoo()).isEqualTo("bar"));
	}

	@Test
	void handleNoMatch() throws Exception {
		given(this.response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		given(this.response.getHeaders()).willReturn(responseHeaders);

		byte[] body = "{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
		responseHeaders.setContentLength(body.length);
		given(this.response.getBody()).willReturn(new ByteArrayInputStream(body));

		assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() ->
				this.errorHandler.handleError(this.response))
			.satisfies(ex -> {
				assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(ex.getResponseBodyAsByteArray()).isEqualTo(body);
			});
	}

	@Test
	void handleNoMatchOverride() throws Exception {
		this.errorHandler.setSeriesMapping(Collections
				.singletonMap(HttpStatus.Series.CLIENT_ERROR, null));

		given(this.response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
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

	}

}
