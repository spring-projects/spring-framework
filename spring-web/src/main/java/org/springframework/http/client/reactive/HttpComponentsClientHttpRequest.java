/*
 * Copyright 2002-2020 the original author or authors.
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
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.function.Function;

import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.reactive.ReactiveEntityProducer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import static org.springframework.http.MediaType.ALL_VALUE;

/**
 * {@link ClientHttpRequest} implementation for the Apache HttpComponents HttpClient 5.x.
 * @author Martin Tarjányi
 * @author Arjen Poutsma
 * @since 5.3
 * @see <a href="https://hc.apache.org/index.html">Apache HttpComponents</a>
 */
class HttpComponentsClientHttpRequest extends AbstractClientHttpRequest {

	private final HttpRequest httpRequest;

	private final DataBufferFactory dataBufferFactory;

	private final HttpClientContext context;

	@Nullable
	private Flux<ByteBuffer> byteBufferFlux;


	public HttpComponentsClientHttpRequest(HttpMethod method, URI uri, HttpClientContext context,
			DataBufferFactory dataBufferFactory) {

		this.context = context;
		this.httpRequest = new BasicHttpRequest(method.name(), uri);
		this.dataBufferFactory = dataBufferFactory;
	}


	@Override
	public HttpMethod getMethod() {
		HttpMethod method = HttpMethod.resolve(this.httpRequest.getMethod());
		Assert.state(method != null, "Method must not be null");
		return method;
	}

	@Override
	public URI getURI() {
		try {
			return this.httpRequest.getUri();
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException("Invalid URI syntax: " + ex.getMessage());
		}
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return this.dataBufferFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNativeRequest() {
		return (T) this.httpRequest;
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return doCommit(() -> {
			this.byteBufferFlux = Flux.from(body).map(DataBuffer::asByteBuffer);
			return Mono.empty();
		});
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return writeWith(Flux.from(body).flatMap(Function.identity()));
	}

	@Override
	public Mono<Void> setComplete() {
		return doCommit();
	}

	@Override
	protected void applyHeaders() {
		HttpHeaders headers = getHeaders();

		headers.entrySet()
				.stream()
				.filter(entry -> !HttpHeaders.CONTENT_LENGTH.equals(entry.getKey()))
				.forEach(entry -> entry.getValue().forEach(v -> this.httpRequest.addHeader(entry.getKey(), v)));

		if (!this.httpRequest.containsHeader(HttpHeaders.ACCEPT)) {
			this.httpRequest.addHeader(HttpHeaders.ACCEPT, ALL_VALUE);
		}
	}

	@Override
	protected void applyCookies() {
		if (getCookies().isEmpty()) {
			return;
		}

		CookieStore cookieStore = this.context.getCookieStore();

		getCookies().values()
				.stream()
				.flatMap(Collection::stream)
				.forEach(cookie -> {
					BasicClientCookie clientCookie = new BasicClientCookie(cookie.getName(), cookie.getValue());
					clientCookie.setDomain(getURI().getHost());
					clientCookie.setPath(getURI().getPath());
					cookieStore.addCookie(clientCookie);
				});
	}

	public AsyncRequestProducer toRequestProducer() {
		ReactiveEntityProducer reactiveEntityProducer = null;

		if (this.byteBufferFlux != null) {
			String contentEncoding = getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
			ContentType contentType = null;
			if (getHeaders().getContentType() != null) {
				contentType = ContentType.parse(getHeaders().getContentType().toString());
			}
			reactiveEntityProducer = new ReactiveEntityProducer(this.byteBufferFlux, getHeaders().getContentLength(),
					contentType, contentEncoding);
		}

		return new BasicRequestProducer(this.httpRequest, reactiveEntityProducer);
	}

}
