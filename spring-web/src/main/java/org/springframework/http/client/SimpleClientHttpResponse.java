/*
 * Copyright 2002-2025 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ClientHttpResponse} implementation that uses standard JDK facilities.
 * Obtained via {@link SimpleClientHttpRequest#execute()}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 3.0
 */
final class SimpleClientHttpResponse implements ClientHttpResponse {

	private final HttpURLConnection connection;

	private @Nullable HttpHeaders headers;

	private @Nullable InputStream responseStream;


	SimpleClientHttpResponse(HttpURLConnection connection) {
		this.connection = connection;
	}


	@Override
	public HttpStatusCode getStatusCode() throws IOException {
		return HttpStatusCode.valueOf(this.connection.getResponseCode());
	}

	@Override
	public String getStatusText() throws IOException {
		String result = this.connection.getResponseMessage();
		return (result != null) ? result : "";
	}

	@Override
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

	@Override
	public InputStream getBody() throws IOException {
		if (this.responseStream == null) {
			if (this.connection.getResponseCode() >= 400) {
				InputStream errorStream = this.connection.getErrorStream();
				this.responseStream = (errorStream != null) ? errorStream : InputStream.nullInputStream();
			}
			else {
				this.responseStream = this.connection.getInputStream();
			}
		}
		return this.responseStream;
	}

	@Override
	public void close() {
		try {
			if (this.responseStream == null) {
				getBody();
			}
			Objects.requireNonNull(this.responseStream);
			StreamUtils.drain(this.responseStream);
			this.responseStream.close();
		}
		catch (Exception ex) {
			// ignore
		}
	}

}
