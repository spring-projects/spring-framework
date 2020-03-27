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
import java.nio.ByteBuffer;
import java.util.function.Function;

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.reactive.ReactiveEntityProducer;
import org.apache.hc.core5.reactive.ReactiveResponseConsumer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

/**
 * {@link ClientHttpConnector} implementation for the Apache HttpComponents HttpClient 5.x.
 *
 * @author Martin Tarjányi
 * @since 5.3
 * @see <a href="https://hc.apache.org/index.html">Apache HttpComponents</a>
 */
public class HttpComponentsClientHttpConnector implements ClientHttpConnector {

	private final CloseableHttpAsyncClient client;

	private final DataBufferFactory dataBufferFactory;


	/**
	 * Default constructor that creates and starts a new instance of {@link CloseableHttpAsyncClient}.
	 */
	public HttpComponentsClientHttpConnector() {
		this(HttpAsyncClients.createDefault());
	}

	/**
	 * Constructor with an initialized {@link CloseableHttpAsyncClient}.
	 */
	public HttpComponentsClientHttpConnector(CloseableHttpAsyncClient client) {
		this.dataBufferFactory = new DefaultDataBufferFactory();
		this.client = client;
		this.client.start();
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		HttpComponentsClientHttpRequest request = new HttpComponentsClientHttpRequest(method, uri, this.dataBufferFactory);

		return requestCallback.apply(request).then(Mono.defer(() -> execute(request)));
	}

	private Mono<ClientHttpResponse> execute(HttpComponentsClientHttpRequest request) {
		Flux<ByteBuffer> byteBufferFlux = request.getByteBufferFlux();

		ReactiveEntityProducer reactiveEntityProducer = createReactiveEntityProducer(request, byteBufferFlux);

		BasicRequestProducer basicRequestProducer = new BasicRequestProducer(request.getHttpRequest(),
				reactiveEntityProducer);

		HttpClientContext context = HttpClientContext.create();
		context.setCookieStore(new BasicCookieStore());

		return Mono.<Message<HttpResponse, Publisher<ByteBuffer>>>create(sink -> {
			ReactiveResponseConsumer reactiveResponseConsumer =
					new ReactiveResponseConsumer(new MonoFutureCallbackAdapter<>(sink));

			this.client.execute(basicRequestProducer, reactiveResponseConsumer, context, null);
		}).map(message -> new HttpComponentsClientHttpResponse(this.dataBufferFactory, message, context));
	}

	@Nullable
	private ReactiveEntityProducer createReactiveEntityProducer(HttpComponentsClientHttpRequest request,
			@Nullable Flux<ByteBuffer> byteBufferFlux) {

		if (byteBufferFlux == null) {
			return null;
		}

		return new ReactiveEntityProducer(byteBufferFlux, request.getContentLength(), null, null);
	}


	private static class MonoFutureCallbackAdapter<T> implements FutureCallback<T> {

		private final MonoSink<T> sink;

		public MonoFutureCallbackAdapter(MonoSink<T> sink) {
			this.sink = sink;
		}

		@Override
		public void completed(T result) {
			this.sink.success(result);
		}

		@Override
		public void failed(Exception ex) {
			this.sink.error(ex);
		}

		@Override
		public void cancelled() {
		}
	}

}
