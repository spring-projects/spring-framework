/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
