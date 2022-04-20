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

package org.springframework.web.reactive.function;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.io.IOException;
import java.io.InputStream;
/**
 * {@link ClientHttpResponse} implementation based on
 * Spring's web client.
 *
 * <p>Created via the {@link WebClientHttpRequest}.
 */
final class WebClientHttpResponse implements ClientHttpResponse {

	private final ClientResponse response;

	@Nullable
	private HttpHeaders headers;


	WebClientHttpResponse(ClientResponse response) {
		this.response = response;
	}


	@Override
	public HttpStatusCode getStatusCode() throws IOException {
		return this.response.statusCode();
	}

	@Override
	@Deprecated
	public int getRawStatusCode() throws IOException {
		return this.response.statusCode().value();
	}

	@Override
	public String getStatusText() throws IOException {
		return "";
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = this.response.headers().asHttpHeaders();
		}
		return this.headers;
	}

	@Override
	public InputStream getBody() throws IOException {
		return this.response.body(BodyExtractors.toInputStream()).block();
	}

	@Override
	public void close() {
		try {
			this.response.releaseBody().block();
		}
		catch (Exception ex) {
			// Ignore exception on close...
		}
	}

}
