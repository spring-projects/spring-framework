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
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultResponseErrorHandler}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Denys Ivano
 */
class DefaultResponseErrorHandlerTests {

	private final DefaultResponseErrorHandler handler = new DefaultResponseErrorHandler();

	private final ClientHttpResponse response = mock();


	@Test
	void hasErrorTrue() throws Exception {
		given(response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		assertThat(handler.hasError(response)).isTrue();
	}

	@Test
	void hasErrorFalse() throws Exception {
		given(response.getStatusCode()).willReturn(HttpStatus.OK);
		assertThat(handler.hasError(response)).isFalse();
	}

	@Test
	void handleError() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		given(response.getStatusText()).willReturn("Not Found");
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willReturn(new ByteArrayInputStream("Hello World".getBytes(StandardCharsets.UTF_8)));

		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> handler.handleError(response))
				.withMessage("404 Not Found: \"Hello World\"")
				.satisfies(ex -> assertThat(ex.getResponseHeaders()).isEqualTo(headers));
	}

	@Test
	void handleErrorWithUrlAndMethod() throws Exception {
		setupClientHttpResponse(HttpStatus.NOT_FOUND, "Hello World");
		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> handler.handleError(URI.create("https://example.com"), HttpMethod.GET, response))
				.withMessage("404 Not Found on GET request for \"https://example.com\": \"Hello World\"");
	}

	@Test
	void handleErrorWithUrlAndQueryParameters() throws Exception {
		setupClientHttpResponse(HttpStatus.NOT_FOUND, "Hello World");
		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> handler.handleError(URI.create("https://example.com/resource?access_token=123"), HttpMethod.GET, response))
				.withMessage("404 Not Found on GET request for \"https://example.com/resource\": \"Hello World\"");
	}

	@Test
	void handleErrorWithUrlAndNoBody() throws Exception {
		setupClientHttpResponse(HttpStatus.NOT_FOUND, null);
		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> handler.handleError(URI.create("https://example.com"), HttpMethod.GET, response))
				.withMessage("404 Not Found on GET request for \"https://example.com\": [no body]");
	}

	private void setupClientHttpResponse(HttpStatus status, @Nullable String textBody) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		given(response.getStatusCode()).willReturn(status);
		given(response.getStatusText()).willReturn(status.getReasonPhrase());
		if (textBody != null) {
			headers.setContentType(MediaType.TEXT_PLAIN);
			given(response.getBody()).willReturn(new ByteArrayInputStream(textBody.getBytes(StandardCharsets.UTF_8)));
		}
		given(response.getHeaders()).willReturn(headers);
	}

	@Test
	void handleErrorIOException() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		given(response.getStatusText()).willReturn("Not Found");
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willThrow(new IOException());

		assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() -> handler.handleError(response));
	}

	@Test
	void handleErrorNullResponse() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getStatusCode()).willReturn(HttpStatus.NOT_FOUND);
		given(response.getStatusText()).willReturn("Not Found");
		given(response.getHeaders()).willReturn(headers);

		assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() ->
				handler.handleError(response));
	}

	@Test  // SPR-16108
	public void hasErrorForUnknownStatusCode() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getStatusCode()).willReturn(HttpStatusCode.valueOf(999));
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);

		assertThat(handler.hasError(response)).isFalse();
	}

	@Test // SPR-9406
	public void handleErrorUnknownStatusCode() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getStatusCode()).willReturn(HttpStatusCode.valueOf(999));
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);

		assertThatExceptionOfType(UnknownHttpStatusCodeException.class).isThrownBy(() ->
				handler.handleError(response));
	}

	@Test  // SPR-17461
	public void hasErrorForCustomClientError() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getStatusCode()).willReturn(HttpStatusCode.valueOf(499));
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);

		assertThat(handler.hasError(response)).isTrue();
	}

	@Test
	void handleErrorForCustomClientError() throws Exception {
		HttpStatusCode statusCode = HttpStatusCode.valueOf(499);
		String statusText = "Custom status code";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		String responseBody = "Hello World";
		TestByteArrayInputStream body = new TestByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));

		given(response.getStatusCode()).willReturn(statusCode);
		given(response.getStatusText()).willReturn(statusText);
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willReturn(body);

		Throwable throwable = catchThrowable(() -> handler.handleError(response));

		// validate exception
		assertThat(throwable).isInstanceOf(HttpClientErrorException.class);
		HttpClientErrorException actualHttpClientErrorException = (HttpClientErrorException) throwable;
		assertThat(actualHttpClientErrorException.getStatusCode()).isEqualTo(statusCode);
		assertThat(actualHttpClientErrorException.getStatusText()).isEqualTo(statusText);
		assertThat(actualHttpClientErrorException.getResponseHeaders()).isEqualTo(headers);
		assertThat(actualHttpClientErrorException.getMessage()).contains(responseBody);
		assertThat(actualHttpClientErrorException.getResponseBodyAsString()).isEqualTo(responseBody);
	}

	@Test  // SPR-17461
	public void hasErrorForCustomServerError() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getStatusCode()).willReturn(HttpStatusCode.valueOf(599));
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);

		assertThat(handler.hasError(response)).isTrue();
	}

	@Test
	void handleErrorForCustomServerError() throws Exception {
		HttpStatusCode statusCode = HttpStatusCode.valueOf(599);
		String statusText = "Custom status code";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		String responseBody = "Hello World";
		TestByteArrayInputStream body = new TestByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));

		given(response.getStatusCode()).willReturn(statusCode);
		given(response.getStatusText()).willReturn(statusText);
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willReturn(body);

		Throwable throwable = catchThrowable(() -> handler.handleError(response));

		// validate exception
		assertThat(throwable).isInstanceOf(HttpServerErrorException.class);
		HttpServerErrorException actualHttpServerErrorException = (HttpServerErrorException) throwable;
		assertThat(actualHttpServerErrorException.getStatusCode()).isEqualTo(statusCode);
		assertThat(actualHttpServerErrorException.getStatusText()).isEqualTo(statusText);
		assertThat(actualHttpServerErrorException.getResponseHeaders()).isEqualTo(headers);
		assertThat(actualHttpServerErrorException.getMessage()).contains(responseBody);
		assertThat(actualHttpServerErrorException.getResponseBodyAsString()).isEqualTo(responseBody);
	}

	@Test  // SPR-16604
	public void bodyAvailableAfterHasErrorForUnknownStatusCode() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		TestByteArrayInputStream body = new TestByteArrayInputStream("Hello World".getBytes(StandardCharsets.UTF_8));

		given(response.getStatusCode()).willReturn(HttpStatusCode.valueOf(999));
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willReturn(body);

		assertThat(handler.hasError(response)).isFalse();
		assertThat(body.isClosed()).isFalse();
		assertThat(StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8)).isEqualTo("Hello World");
	}


	private static class TestByteArrayInputStream extends ByteArrayInputStream {

		private boolean closed;

		public TestByteArrayInputStream(byte[] buf) {
			super(buf);
			this.closed = false;
		}

		public boolean isClosed() {
			return closed;
		}

		@Override
		public boolean markSupported() {
			return false;
		}

		@Override
		public synchronized void mark(int readlimit) {
			throw new UnsupportedOperationException("mark/reset not supported");
		}

		@Override
		public synchronized void reset() {
			throw new UnsupportedOperationException("mark/reset not supported");
		}

		@Override
		public void close() throws IOException {
			super.close();
			this.closed = true;
		}
	}

}
