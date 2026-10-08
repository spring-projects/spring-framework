/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SimpleClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		return new SimpleClientHttpRequestFactory();
	}

	@Override
	@Test
	void httpMethods() throws Exception {
		super.httpMethods();
		assertThatExceptionOfType(ProtocolException.class).isThrownBy(() ->
				assertHttpMethod("patch", HttpMethod.PATCH));
	}

	@Test
	void prepareConnectionWithRequestBody() throws Exception {
		URI uri = new URI("https://example.com");
		testRequestBodyAllowed(uri, "GET", false);
		testRequestBodyAllowed(uri, "HEAD", false);
		testRequestBodyAllowed(uri, "OPTIONS", false);
		testRequestBodyAllowed(uri, "TRACE", false);
		testRequestBodyAllowed(uri, "PUT", true);
		testRequestBodyAllowed(uri, "POST", true);
		testRequestBodyAllowed(uri, "DELETE", true);
	}

	private void testRequestBodyAllowed(URI uri, String httpMethod, boolean allowed) throws IOException {
		HttpURLConnection connection = new TestHttpURLConnection(uri.toURL());
		((SimpleClientHttpRequestFactory) this.factory).prepareConnection(connection, httpMethod);
		assertThat(connection.getDoOutput()).isEqualTo(allowed);
	}

	@Test
	void deleteWithoutBodyDoesNotRaiseException() throws Exception {
		HttpURLConnection connection = new TestHttpURLConnection(URI.create("https://example.com").toURL());
		((SimpleClientHttpRequestFactory) this.factory).prepareConnection(connection, "DELETE");
		SimpleClientHttpRequest request = new SimpleClientHttpRequest(connection, 4096);
		request.execute();
	}

	@Test  // SPR-8809
	void interceptor() throws Exception {
		final String headerName = "MyHeader";
		final String headerValue = "MyValue";
		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
			request.getHeaders().add(headerName, headerValue);
			return execution.execute(request, body);
		};
		InterceptingClientHttpRequestFactory factory = new InterceptingClientHttpRequestFactory(
				createRequestFactory(), Collections.singletonList(interceptor));

		ClientHttpResponse response = null;
		try {
			ClientHttpRequest request = factory.createRequest(URI.create(baseUrl + "/echo"), HttpMethod.GET);
			response = request.execute();
			assertThat(response.getStatusCode()).as("Invalid response status").isEqualTo(HttpStatus.OK);
			HttpHeaders responseHeaders = response.getHeaders();
			assertThat(responseHeaders.getFirst(headerName)).as("Custom header invalid").isEqualTo(headerValue);
		}
		finally {
			if (response != null) {
				response.close();
			}
		}
	}

	@Test  // SPR-13225
	void headerWithNullValue() {
		HttpURLConnection urlConnection = mock();
		given(urlConnection.getRequestMethod()).willReturn("GET");

		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", null);
		SimpleClientHttpRequest.addHeaders(urlConnection, headers);

		verify(urlConnection, times(1)).addRequestProperty("foo", "");
	}

	@Test
	void durationTimeouts() throws Exception {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(5));
		factory.setReadTimeout(Duration.ofSeconds(10));

		HttpURLConnection connection = prepareConnection(factory);
		assertThat(connection.getConnectTimeout()).isEqualTo(5_000);
		assertThat(connection.getReadTimeout()).isEqualTo(10_000);
	}

	@ParameterizedTest  // gh-37427
	@ValueSource(strings = {"P25D", "P50D", "PT9223372036854775807S"})
	void durationTimeoutsExceedingIntegerMaxValueAreLimited(Duration timeout) throws Exception {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(timeout);
		factory.setReadTimeout(timeout);

		HttpURLConnection connection = prepareConnection(factory);
		assertThat(connection.getConnectTimeout()).isEqualTo(Integer.MAX_VALUE);
		assertThat(connection.getReadTimeout()).isEqualTo(Integer.MAX_VALUE);
	}

	@ParameterizedTest  // gh-37427
	@ValueSource(strings = {"PT-0.001S", "P-25D", "P-50D"})
	void negativeDurationTimeoutsAreRejected(Duration timeout) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> factory.setConnectTimeout(timeout))
				.withMessage("Timeout must be a non-negative value");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> factory.setReadTimeout(timeout))
				.withMessage("Timeout must be a non-negative value");
	}

	private static HttpURLConnection prepareConnection(SimpleClientHttpRequestFactory factory) throws IOException {
		HttpURLConnection connection = new TestHttpURLConnection(URI.create("https://example.com").toURL());
		factory.prepareConnection(connection, "GET");
		return connection;
	}


	private static class TestHttpURLConnection extends HttpURLConnection {

		public TestHttpURLConnection(URL uri) {
			super(uri);
		}

		@Override
		public void connect() {
		}

		@Override
		public void disconnect() {
		}

		@Override
		public boolean usingProxy() {
			return false;
		}

		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream(new byte[0]);
		}
	}

}
