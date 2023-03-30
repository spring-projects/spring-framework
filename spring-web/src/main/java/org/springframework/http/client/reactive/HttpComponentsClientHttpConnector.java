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

package org.springframework.http.client.reactive;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStreamResetException;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.reactive.ReactiveResponseConsumer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * {@link ClientHttpConnector} implementation for the Apache HttpComponents HttpClient 5.x.
 *
 * @author Martin Tarjányi
 * @author Arjen Poutsma
 * @since 5.3
 * @see <a href="https://hc.apache.org/index.html">Apache HttpComponents</a>
 */
public class HttpComponentsClientHttpConnector implements ClientHttpConnector, Closeable {

	private final CloseableHttpAsyncClient client;

	private final BiFunction<HttpMethod, URI, ? extends HttpClientContext> contextProvider;

	private DataBufferFactory dataBufferFactory = DefaultDataBufferFactory.sharedInstance;


	/**
	 * Default constructor that creates and starts a new instance of {@link CloseableHttpAsyncClient}.
	 */
	public HttpComponentsClientHttpConnector() {
		this(HttpAsyncClients.createDefault());
	}

	/**
	 * Constructor with a pre-configured {@link CloseableHttpAsyncClient} instance.
	 * @param client the client to use
	 */
	public HttpComponentsClientHttpConnector(CloseableHttpAsyncClient client) {
		this(client, (method, uri) -> HttpClientContext.create());
	}

	/**
	 * Constructor with a pre-configured {@link CloseableHttpAsyncClient} instance
	 * and a {@link HttpClientContext} supplier lambda which is called before each request
	 * and passed to the client.
	 * @param client the client to use
	 * @param contextProvider a {@link HttpClientContext} supplier
	 */
	public HttpComponentsClientHttpConnector(CloseableHttpAsyncClient client,
			BiFunction<HttpMethod, URI, ? extends HttpClientContext> contextProvider) {

		Assert.notNull(client, "Client must not be null");
		Assert.notNull(contextProvider, "ContextProvider must not be null");

		this.contextProvider = contextProvider;
		this.client = client;
		this.client.start();
	}


	/**
	 * Set the buffer factory to use.
	 */
	public void setBufferFactory(DataBufferFactory bufferFactory) {
		this.dataBufferFactory = bufferFactory;
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		HttpClientContext context = this.contextProvider.apply(method, uri);
		if (context.getCookieStore() == null) {
			context.setCookieStore(new BasicCookieStore());
		}

		HttpComponentsClientHttpRequest request =
				new HttpComponentsClientHttpRequest(method, uri, context, this.dataBufferFactory);
		return requestCallback.apply(request).then(Mono.defer(() -> execute(request, context)));
	}

	private Mono<ClientHttpResponse> execute(HttpComponentsClientHttpRequest request, HttpClientContext context) {
		AsyncRequestProducer requestProducer = request.toRequestProducer();

		return Mono.create(sink -> {
			ReactiveResponseConsumer reactiveResponseConsumer =
					new ReactiveResponseConsumer(new MonoFutureCallbackAdapter(sink, this.dataBufferFactory, context));
			this.client.execute(requestProducer, reactiveResponseConsumer, context, null);
		});
	}

	@Override
	public void close() throws IOException {
		this.client.close();
	}


	private static class MonoFutureCallbackAdapter
			implements FutureCallback<Message<HttpResponse, Publisher<ByteBuffer>>> {

		private final MonoSink<ClientHttpResponse> sink;

		private final DataBufferFactory dataBufferFactory;

		private final HttpClientContext context;

		public MonoFutureCallbackAdapter(MonoSink<ClientHttpResponse> sink,
				DataBufferFactory dataBufferFactory, HttpClientContext context) {

			this.sink = sink;
			this.dataBufferFactory = dataBufferFactory;
			this.context = context;
		}

		@Override
		public void completed(Message<HttpResponse, Publisher<ByteBuffer>> result) {
			this.sink.success(new HttpComponentsClientHttpResponse(this.dataBufferFactory, result, this.context));
		}

		@Override
		public void failed(Exception ex) {
			this.sink.error(ex instanceof HttpStreamResetException && ex.getCause() != null ? ex.getCause() : ex);
		}

		@Override
		public void cancelled() {
		}
	}

}
