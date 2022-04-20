/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.http.HttpMethod;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * {@link ClientHttpRequest} implementation based on
 * Spring's web client in streaming mode.
 *
 * <p>Created via the {@link WebClientHttpRequestFactory}.
 */
final class WebClientStreamingHttpRequest extends AbstractClientHttpRequest
		implements StreamingHttpOutputMessage {

	private final WebClient webClient;

	private final HttpMethod method;

	private final URI uri;

	@Nullable
	private Body body;

	WebClientStreamingHttpRequest(WebClient client, HttpMethod method, URI uri) {
		this.webClient = client;
		this.method = method;
		this.uri = uri;
	}

	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	@Deprecated
	public String getMethodValue() {
		return this.method.name();
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public void setBody(Body body) {
		assertNotExecuted();
		this.body = body;
	}

	@Override
	protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
		throw new UnsupportedOperationException("getBody not supported");
	}

	@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
		WebClient.RequestHeadersSpec<?> request = this.webClient.method(this.method)
				.uri(this.uri)
				.bodyValue(this.body == null
						? BodyInserters.empty()
						: BodyInserters.fromOutputStream(outputStream -> this.body.writeTo(outputStream)));

		request.headers(value -> value.addAll(headers));

		@SuppressWarnings("deprecation")
		ClientResponse response = request.exchange().block();
		return new WebClientHttpResponse(response);
	}
}
