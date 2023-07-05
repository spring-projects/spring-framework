/*
 * Copyright 2002-2023 the original author or authors.
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

import org.eclipse.jetty.client.api.Response;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.util.MultiValueMap;

/**
 * {@link ClientHttpResponse} implementation based on Jetty's
 * {@link org.eclipse.jetty.client.HttpClient}.
 *
 * @author Arjen Poutsma
 * @since 6.1
 */
class JettyClientHttpResponse implements ClientHttpResponse {

	private final Response response;

	private final InputStream body;

	private final HttpHeaders headers;


	public JettyClientHttpResponse(Response response, InputStream inputStream) {
		this.response = response;
		this.body = inputStream;

		MultiValueMap<String, String> headers = new JettyHeadersAdapter(response.getHeaders());
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
	}


	@Override
	public HttpStatusCode getStatusCode() throws IOException {
		return HttpStatusCode.valueOf(this.response.getStatus());
	}

	@Override
	public String getStatusText() throws IOException {
		return this.response.getReason();
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.body;
	}

	@Override
	public void close() {
		try {
			this.body.close();
		}
		catch (IOException ignored) {
		}
	}
}
