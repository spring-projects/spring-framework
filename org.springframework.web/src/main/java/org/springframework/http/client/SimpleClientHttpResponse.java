/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

/**
 * {@link ClientHttpResponse} implementation that uses standard J2SE facilities.
 * Obtained via the {@link SimpleClientHttpRequest#execute()}.
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
		return HttpStatus.valueOf(this.connection.getResponseCode());
	}

	public String getStatusText() throws IOException {
		return this.connection.getResponseMessage();
	}

	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			// Header field 0 is the status line for most HttpURLConnections, but not on GAE
			String name = this.connection.getHeaderFieldKey(0);
			if (StringUtils.hasLength(name)) {
				this.headers.add(name, this.connection.getHeaderField(0));
			}
			int i = 1;
			while (true) {
				name = this.connection.getHeaderFieldKey(i);
				if (!StringUtils.hasLength(name)) {
					break;
				}
				this.headers.add(name, this.connection.getHeaderField(i));
				i++;
			}
		}
		return this.headers;
	}

	public InputStream getBody() throws IOException {
		InputStream errorStream = this.connection.getErrorStream();
		return (errorStream != null ? errorStream : this.connection.getInputStream());
	}

	public void close() {
		this.connection.disconnect();
	}

}
