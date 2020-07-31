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

import java.net.HttpCookie;
import java.net.URI;
import java.util.Collection;
import java.util.function.Function;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.eclipse.jetty.util.Callback;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpRequest} implementation for the Jetty ReactiveStreams HTTP client.
 *
 * @author Sebastien Deleuze
 * @since 5.1
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">Jetty ReactiveStreams HttpClient</a>
 */
class JettyClientHttpRequest extends AbstractClientHttpRequest {

	private final Request jettyRequest;

	private final DataBufferFactory bufferFactory;

	@Nullable
	private ReactiveRequest reactiveRequest;


	public JettyClientHttpRequest(Request jettyRequest, DataBufferFactory bufferFactory) {
		this.jettyRequest = jettyRequest;
		this.bufferFactory = bufferFactory;
	}


	@Override
	public HttpMethod getMethod() {
		HttpMethod method = HttpMethod.resolve(this.jettyRequest.getMethod());
		Assert.state(method != null, "Method must not be null");
		return method;
	}

	@Override
	public URI getURI() {
		return this.jettyRequest.getURI();
	}

	@Override
	public Mono<Void> setComplete() {
		return doCommit(this::completes);
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return this.bufferFactory;
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return Mono.<Void>create(sink -> {
			Flux<ContentChunk> chunks = Flux.from(body).map(buffer -> toContentChunk(buffer, sink));
			ReactiveRequest.Content content = ReactiveRequest.Content.fromPublisher(chunks, getContentType());
			this.reactiveRequest = ReactiveRequest.newBuilder(this.jettyRequest).content(content).build();
			sink.success();
		})
		.then(doCommit(this::completes));
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return writeWith(Flux.from(body).flatMap(Function.identity()));
	}

	private String getContentType() {
		MediaType contentType = getHeaders().getContentType();
		return contentType != null ? contentType.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
	}

	private Mono<Void> completes() {
		return Mono.empty();
	}

	private ContentChunk toContentChunk(DataBuffer buffer, MonoSink<Void> sink) {
		return new ContentChunk(buffer.asByteBuffer(), new Callback() {
			@Override
			public void succeeded() {
				DataBufferUtils.release(buffer);
			}

			@Override
			public void failed(Throwable x) {
				DataBufferUtils.release(buffer);
				sink.error(x);
			}
		});
	}


	@Override
	protected void applyCookies() {
		getCookies().values().stream().flatMap(Collection::stream)
				.map(cookie -> new HttpCookie(cookie.getName(), cookie.getValue()))
				.forEach(this.jettyRequest::cookie);
	}

	@Override
	protected void applyHeaders() {
		HttpHeaders headers = getHeaders();
		headers.forEach((key, value) -> value.forEach(v -> this.jettyRequest.header(key, v)));
		if (!headers.containsKey(HttpHeaders.ACCEPT)) {
			this.jettyRequest.header(HttpHeaders.ACCEPT, "*/*");
		}
	}

	ReactiveRequest getReactiveRequest() {
		if (this.reactiveRequest == null) {
			this.reactiveRequest = ReactiveRequest.newBuilder(this.jettyRequest).build();
		}
		return this.reactiveRequest;
	}

}
