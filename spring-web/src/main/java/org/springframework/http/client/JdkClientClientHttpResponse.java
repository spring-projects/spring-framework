/*
 * Copyright 2023-2023 the original author or authors.
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
import java.net.http.HttpResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

/**
 * {@link ClientHttpResponse} implementation based on the Java {@code HttpClient}.
 *
 * @author Marten Deinum
 * @since 6.1
 */
public class JdkClientClientHttpResponse implements ClientHttpResponse {

	private final HttpResponse<InputStream> response;
	@Nullable
	private volatile HttpHeaders headers;

	public JdkClientClientHttpResponse(HttpResponse<InputStream> response) {
		this.response = response;
	}

	@Override
	public HttpStatusCode getStatusCode() throws IOException {
		return HttpStatusCode.valueOf(this.response.statusCode());
	}

	@Override
	@Deprecated
	public int getRawStatusCode() {
		return this.response.statusCode();
	}

	@Override
	public String getStatusText() {
		HttpStatus status = HttpStatus.resolve(this.response.statusCode());
		return (status != null) ? status.getReasonPhrase() : "";
	}

	@Override
	public InputStream getBody() throws IOException {
		InputStream body = this.response.body();
		return (body != null ? body : InputStream.nullInputStream());
	}

	@Override
	public HttpHeaders getHeaders() {
		HttpHeaders headers = this.headers;
		if (headers == null) {
			headers = new HttpHeaders();
			for (String headerName : this.response.headers().map().keySet()) {
				for (String headerValue : this.response.headers().allValues(headerName)) {
					headers.add(headerName, headerValue);
				}
			}
			this.headers = headers;
		}
		return headers;
	}

	@Override
	public void close() {
		InputStream body = this.response.body();
		try {
			try {
				StreamUtils.drain(body);
			}
			finally {
				body.close();
			}
		}
		catch (IOException ex) {
			// Ignore exception on close...
		}
	}
}
