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

package org.springframework.http.client.reactive;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpRequest} implementation for Java's {@link HttpClient}.
 *
 * @author Julien Eyraud
 * @since 6.0
 */
class JdkClientHttpRequest extends AbstractClientHttpRequest {

	private static final Set<String> DISALLOWED_HEADERS =
			Set.of("connection", "content-length", "date", "expect", "from", "host", "upgrade", "via", "warning");


	private final HttpClient httpClient;

	private final HttpMethod method;

	private final URI uri;

	private final HttpRequest.Builder builder;

	private final DataBufferFactory bufferFactory;

	@Nullable
	private Mono<ClientHttpResponse> response;


	public JdkClientHttpRequest(
			HttpClient httpClient, HttpMethod httpMethod, URI uri, DataBufferFactory bufferFactory) {

		Assert.notNull(httpClient, "HttpClient should not be null");
		Assert.notNull(httpMethod, "HttpMethod should not be null");
		Assert.notNull(uri, "URI should not be null");
		Assert.notNull(bufferFactory, "DataBufferFactory should not be null");

		this.httpClient = httpClient;
		this.method = httpMethod;
		this.uri = uri;
		this.builder = HttpRequest.newBuilder(uri);
		this.bufferFactory = bufferFactory;
	}


	@Override
	public HttpMethod getMethod() {
		return this.method;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return this.bufferFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNativeRequest() {
		return (T) this.builder.build();
	}

	Mono<ClientHttpResponse> getResponse() {
		Assert.notNull(this.response, "Response is not set");
		return this.response;
	}


	@Override
	protected void applyHeaders() {
		for (Map.Entry<String, List<String>> header : getHeaders().entrySet()) {
			if (DISALLOWED_HEADERS.contains(header.getKey().toLowerCase())) {
				continue;
			}
			for (String value : header.getValue()) {
				this.builder.header(header.getKey(), value);
			}
		}
		if (!getHeaders().containsKey(HttpHeaders.ACCEPT)) {
			this.builder.header(HttpHeaders.ACCEPT, "*/*");
		}
	}

	@Override
	protected void applyCookies() {
		this.builder.header(HttpHeaders.COOKIE,
				getCookies().values().stream()
						.flatMap(List::stream)
						.map(cookie -> cookie.getName() + "=" + cookie.getValue())
						.collect(Collectors.joining("; ")));
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return doCommit(() -> {
			Flow.Publisher<ByteBuffer> flow =
					JdkFlowAdapter.publisherToFlowPublisher(Flux.from(body).map(DataBuffer::asByteBuffer));

			HttpRequest.BodyPublisher bodyPublisher = (getHeaders().getContentLength() >= 0 ?
					HttpRequest.BodyPublishers.fromPublisher(flow, getHeaders().getContentLength()) :
					HttpRequest.BodyPublishers.fromPublisher(flow));

			this.response = Mono.fromCompletionStage(() -> {
						HttpRequest request = this.builder.method(this.method.name(), bodyPublisher).build();
						return this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofPublisher());
					})
					.map(response -> new JdkClientHttpResponse(response, this.bufferFactory));

			return Mono.empty();
		});
	}

	@Override
	public Mono<Void> writeAndFlushWith(final Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return writeWith(Flux.from(body).flatMap(Function.identity()));
	}

	@Override
	public Mono<Void> setComplete() {
		if (isCommitted()) {
			return Mono.empty();
		}

		return doCommit(() -> {
			this.response = Mono.fromCompletionStage(() -> {
						HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
						HttpRequest request = this.builder.method(this.method.name(), bodyPublisher).build();
						return this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofPublisher());
					})
					.map(response -> new JdkClientHttpResponse(response, this.bufferFactory));

			return Mono.empty();
		});
	}

}
