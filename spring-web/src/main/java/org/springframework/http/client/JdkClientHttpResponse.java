/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;

/**
 * {@link ClientHttpResponse} implementation based on
 * JDK HTTP client.
 *
 * <p>Created via the {@link JdkClientHttpRequest}.
 */
final class JdkClientHttpResponse implements ClientHttpResponse {

	private final HttpResponse<InputStream> httpResponse;

	@Nullable
	private HttpHeaders headers;


	JdkClientHttpResponse(HttpResponse<InputStream> httpResponse) {
		this.httpResponse = httpResponse;
	}


	@Override
	public HttpStatusCode getStatusCode() throws IOException {
		return HttpStatusCode.valueOf(this.httpResponse.statusCode());
	}

	@Override
	@Deprecated
	public int getRawStatusCode() throws IOException {
		return this.httpResponse.statusCode();
	}

	@Override
	public String getStatusText() throws IOException {
		return "";
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			this.httpResponse.headers().map().forEach((key, values) -> this.headers.addAll(key, values));
		}
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.httpResponse.body();
	}

	@Override
	public void close() {
		// Release underlying connection back to the connection manager
		try {
			try {
				// Attempt to keep connection alive by consuming its remaining content
				this.httpResponse.body().readAllBytes();
			}
			finally {
				this.httpResponse.body().close();
			}
		}
		catch (IOException ex) {
			// Ignore exception on close...
		}
	}

}
