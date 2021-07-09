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
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

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

	private final ReactiveRequest.Builder builder;



	public JettyClientHttpRequest(Request jettyRequest, DataBufferFactory bufferFactory) {
		this.jettyRequest = jettyRequest;
		this.bufferFactory = bufferFactory;
		this.builder = ReactiveRequest.newBuilder(this.jettyRequest).abortOnCancel(true);
	}


	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.jettyRequest.getMethod());
	}

	@Override
	public URI getURI() {
		return this.jettyRequest.getURI();
	}

	@Override
	public Mono<Void> setComplete() {
		return doCommit();
	}

	@Override
	public DataBufferFactory bufferFactory() {
		return this.bufferFactory;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNativeRequest() {
		return (T) this.jettyRequest;
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return Mono.<Void>create(sink -> {
			ReactiveRequest.Content content = Flux.from(body)
					.map(buffer -> toContentChunk(buffer, sink))
					.as(chunks -> ReactiveRequest.Content.fromPublisher(chunks, getContentType()));
			this.builder.content(content);
			sink.success();
		})
				.then(doCommit());
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return writeWith(Flux.from(body)
				.flatMap(Function.identity())
				.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release));
	}

	private String getContentType() {
		MediaType contentType = getHeaders().getContentType();
		return contentType != null ? contentType.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
	}

	private ContentChunk toContentChunk(DataBuffer buffer, MonoSink<Void> sink) {
		return new ContentChunk(buffer.asByteBuffer(), new Callback() {
			@Override
			public void succeeded() {
				DataBufferUtils.release(buffer);
			}
			@Override
			public void failed(Throwable t) {
				DataBufferUtils.release(buffer);
				sink.error(t);
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

	public ReactiveRequest toReactiveRequest() {
		return this.builder.build();
	}


}
