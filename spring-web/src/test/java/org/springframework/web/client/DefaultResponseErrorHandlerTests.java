/*
 * Copyright 2002-2020 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link DefaultResponseErrorHandler}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Denys Ivano
 */
public class DefaultResponseErrorHandlerTests {

	private final DefaultResponseErrorHandler handler = new DefaultResponseErrorHandler();

	private final ClientHttpResponse response = mock(ClientHttpResponse.class);


	@Test
	public void hasErrorTrue() throws Exception {
		given(response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		assertThat(handler.hasError(response)).isTrue();
	}

	@Test
	public void hasErrorFalse() throws Exception {
		given(response.getRawStatusCode()).willReturn(HttpStatus.OK.value());
		assertThat(handler.hasError(response)).isFalse();
	}

	@Test
	public void handleError() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		given(response.getStatusText()).willReturn("Not Found");
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willReturn(new ByteArrayInputStream("Hello World".getBytes(StandardCharsets.UTF_8)));

		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> handler.handleError(response))
				.satisfies(ex -> assertThat(ex.getResponseHeaders()).isSameAs(headers))
				.satisfies(ex -> assertThat(ex.getMessage()).isEqualTo("404 Not Found: [Hello World]"));
	}

	@Test
	public void handleErrorWithLongBody() throws Exception {

		Function<Integer, String> bodyGenerator =
				size -> Flux.just("a").repeat(size-1).reduce((s, s2) -> s + s2).block();

		given(response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		given(response.getStatusText()).willReturn("Not Found");
		given(response.getHeaders()).willReturn(new HttpHeaders());
		given(response.getBody()).willReturn(
				new ByteArrayInputStream(bodyGenerator.apply(500).getBytes(StandardCharsets.UTF_8)));

		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> handler.handleError(response))
				.satisfies(ex -> assertThat(ex.getMessage()).isEqualTo(
						"404 Not Found: [" + bodyGenerator.apply(200) + "... (500 bytes)]"));
	}

	@Test
	public void handleErrorIOException() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		given(response.getStatusText()).willReturn("Not Found");
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willThrow(new IOException());

		assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() -> handler.handleError(response));
	}

	@Test
	public void handleErrorNullResponse() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(HttpStatus.NOT_FOUND.value());
		given(response.getStatusText()).willReturn("Not Found");
		given(response.getHeaders()).willReturn(headers);

		assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() ->
				handler.handleError(response));
	}

	@Test  // SPR-16108
	public void hasErrorForUnknownStatusCode() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(999);
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);

		assertThat(handler.hasError(response)).isFalse();
	}

	@Test // SPR-9406
	public void handleErrorUnknownStatusCode() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(999);
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);

		assertThatExceptionOfType(UnknownHttpStatusCodeException.class).isThrownBy(() ->
				handler.handleError(response));
	}

	@Test  // SPR-17461
	public void hasErrorForCustomClientError() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(499);
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);

		assertThat(handler.hasError(response)).isTrue();
	}

	@Test
	public void handleErrorForCustomClientError() throws Exception {
		int statusCode = 499;
		String statusText = "Custom status code";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		String responseBody = "Hello World";
		TestByteArrayInputStream body = new TestByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));

		given(response.getRawStatusCode()).willReturn(statusCode);
		given(response.getStatusText()).willReturn(statusText);
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willReturn(body);

		Throwable throwable = catchThrowable(() -> handler.handleError(response));

		// validate exception
		assertThat(throwable).isInstanceOf(UnknownHttpStatusCodeException.class);
		UnknownHttpStatusCodeException actualUnknownHttpStatusCodeException = (UnknownHttpStatusCodeException) throwable;
		assertThat(actualUnknownHttpStatusCodeException.getRawStatusCode()).isEqualTo(statusCode);
		assertThat(actualUnknownHttpStatusCodeException.getStatusText()).isEqualTo(statusText);
		assertThat(actualUnknownHttpStatusCodeException.getResponseHeaders()).isEqualTo(headers);
		assertThat(actualUnknownHttpStatusCodeException.getMessage()).contains(responseBody);
		assertThat(actualUnknownHttpStatusCodeException.getResponseBodyAsString()).isEqualTo(responseBody);
	}

	@Test  // SPR-17461
	public void hasErrorForCustomServerError() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		given(response.getRawStatusCode()).willReturn(599);
		given(response.getStatusText()).willReturn("Custom status code");
		given(response.getHeaders()).willReturn(headers);

		assertThat(handler.hasError(response)).isTrue();
	}

	@Test
	public void handleErrorForCustomServerError() throws Exception {
		int statusCode = 599;
		String statusText = "Custom status code";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);

		String responseBody = "Hello World";
		TestByteArrayInputStream body = new TestByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));

		given(response.getRawStatusCode()).willReturn(statusCode);
		given(response.getStatusText()).willReturn(statusText);
		given(response.getHeaders()).willReturn(headers);
		given(response.getBody()).willReturn(body);

		Throwable throwable = catchThrowable(() -> handler.handleError(response));

		// validate exception
		assertThat(throwable).isInstanceOf(UnknownHttpStatusCodeException.class);
		UnknownHttpStatusCodeException actualUnknownHttpStatusCodeException = (UnknownHttpStatusCodeException) throwable;
		assertThat(actualUnknownHttpStatusCodeException.getRawStatusCode()).isEqualTo(statusCode);
		assertThat(actualUnknownHttpStatusCodeException.getStatusText()).isEqualTo(statusText);
		assertThat(actualUnknownHttpStatusCodeException.getResponseHeaders()).isEqualTo(headers);
		assertThat(actualUnknownHttpStatusCodeException.getMessage()).contains(responseBody);
		assertThat(actualUnknownHttpStatusCodeException.getResponseBodyAsString()).isEqualTo(responseBody);
	}

	@Test  // SPR-16604
	public void bodyAvailableAfterHasErrorForUnknownStatusCode() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_PLAIN);
		TestByteArrayInputStream body = new TestByteArrayInputStream("Hello World".getBytes(StandardCharsets.UTF_8));

		given(response.getRawStatusCode()).willReturn(999);
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
