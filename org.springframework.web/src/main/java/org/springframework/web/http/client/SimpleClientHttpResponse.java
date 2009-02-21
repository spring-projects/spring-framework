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
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.springframework.util.StringUtils;
import org.springframework.web.http.HttpHeaders;
import org.springframework.web.http.HttpStatus;

/**
 * {@link ClientHttpResponse} implementation that uses standard J2SE facilities. Obtained via the {@link
 * SimpleClientHttpRequest#execute()}.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
final class SimpleClientHttpResponse implements ClientHttpResponse {

	private final HttpURLConnection connection;

	private HttpHeaders headers;

	SimpleClientHttpResponse(HttpURLConnection connection) {
		this.connection = connection;
	}

	public HttpStatus getStatusCode() throws IOException {
		return HttpStatus.valueOf(connection.getResponseCode());
	}

	public String getStatusText() throws IOException {
		return connection.getResponseMessage();
	}

	public HttpHeaders getHeaders() {
		if (headers == null) {
			headers = new HttpHeaders();
			// Header field 0 is the status line, so we start at 1
			int i = 1;
			while (true) {
				String name = connection.getHeaderFieldKey(i);
				if (!StringUtils.hasLength(name)) {
					break;
				}
				headers.add(name, connection.getHeaderField(i));
				i++;
			}
		}
		return headers;
	}

	public InputStream getBody() throws IOException {
		if (connection.getErrorStream() == null) {
			return connection.getInputStream();
		}
		else {
			return connection.getErrorStream();
		}
	}

	public void close() {
		connection.disconnect();
	}
}
