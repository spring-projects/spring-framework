/*
 * Copyright 2002-2019 the original author or authors.
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
import java.net.URL;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

public class BufferedSimpleHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		return new SimpleClientHttpRequestFactory();
	}

	@Override
	@Test
	public void httpMethods() throws Exception {
		try {
			assertHttpMethod("patch", HttpMethod.PATCH);
		}
		catch (ProtocolException ex) {
			// Currently HttpURLConnection does not support HTTP PATCH
		}
	}

	@Test
	public void prepareConnectionWithRequestBody() throws Exception {
		URL uri = new URL("https://example.com");
		testRequestBodyAllowed(uri, "GET", false);
		testRequestBodyAllowed(uri, "HEAD", false);
		testRequestBodyAllowed(uri, "OPTIONS", false);
		testRequestBodyAllowed(uri, "TRACE", false);
		testRequestBodyAllowed(uri, "PUT", true);
		testRequestBodyAllowed(uri, "POST", true);
		testRequestBodyAllowed(uri, "DELETE", true);
	}

	@Test
	public void deleteWithoutBodyDoesNotRaiseException() throws Exception {
		HttpURLConnection connection = new TestHttpURLConnection(new URL("https://example.com"));
		((SimpleClientHttpRequestFactory) this.factory).prepareConnection(connection, "DELETE");
		SimpleBufferingClientHttpRequest request = new SimpleBufferingClientHttpRequest(connection, false);
		request.execute();
	}

	private void testRequestBodyAllowed(URL uri, String httpMethod, boolean allowed) throws IOException {
		HttpURLConnection connection = new TestHttpURLConnection(uri);
		((SimpleClientHttpRequestFactory) this.factory).prepareConnection(connection, httpMethod);
		assertThat(connection.getDoOutput()).isEqualTo(allowed);
	}


	private static class TestHttpURLConnection extends HttpURLConnection {

		public TestHttpURLConnection(URL uri) {
			super(uri);
		}

		@Override
		public void connect() throws IOException {
		}

		@Override
		public void disconnect() {
		}

		@Override
		public boolean usingProxy() {
			return false;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(new byte[0]);
		}
	}

}
