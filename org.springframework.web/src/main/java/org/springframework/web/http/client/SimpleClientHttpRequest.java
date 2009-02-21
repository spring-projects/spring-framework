/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.http.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import org.springframework.web.http.HttpHeaders;
import org.springframework.web.http.HttpMethod;

/**
 * {@link ClientHttpRequest} implementation that uses standard J2SE facilities to execute requests. Created via the
 * {@link SimpleClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @see SimpleClientHttpRequestFactory#createRequest(java.net.URI, HttpMethod)
 * @since 3.0
 */
final class SimpleClientHttpRequest implements ClientHttpRequest {

	private final HttpURLConnection connection;

	private final HttpHeaders headers = new HttpHeaders();

	private boolean headersWritten = false;

	SimpleClientHttpRequest(HttpURLConnection connection) {
		this.connection = connection;
	}

	public HttpMethod getMethod() {
		return HttpMethod.valueOf(connection.getRequestMethod());
	}

	public HttpHeaders getHeaders() {
		return headers;
	}

	public OutputStream getBody() throws IOException {
		writeHeaders();
		return connection.getOutputStream();
	}

	public ClientHttpResponse execute() throws IOException {
		writeHeaders();
		connection.connect();
		return new SimpleClientHttpResponse(connection);
	}

	private void writeHeaders() {
		if (!headersWritten) {
			for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
				String headerName = entry.getKey();
				for (String headerValue : entry.getValue()) {
					connection.addRequestProperty(headerName, headerValue);
				}
			}
			headersWritten = true;
		}
	}
}
