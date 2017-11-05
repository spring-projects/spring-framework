/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.client.reactive.ClientHttpResponseDecorator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Decorate another {@link ClientHttpConnector} with the purpose of
 * intercepting, capturing, and exposing actual request and response data
 * transmitted to and received from the server.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see HttpHandlerConnector
 */
class WiretapConnector implements ClientHttpConnector {

	private static final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	private final ClientHttpConnector delegate;

	private final Map<String, Info> exchanges = new ConcurrentHashMap<>();


	WiretapConnector(ClientHttpConnector delegate) {
		this.delegate = delegate;
	}


	@Override
	public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
			Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

		AtomicReference<WiretapClientHttpRequest> requestRef = new AtomicReference<>();

		return this.delegate
				.connect(method, uri, request -> {
					WiretapClientHttpRequest wrapped = new WiretapClientHttpRequest(request);
					requestRef.set(wrapped);
					return requestCallback.apply(wrapped);
				})
				.map(response ->  {
					WiretapClientHttpRequest wrappedRequest = requestRef.get();
					String header = WebTestClient.WEBTESTCLIENT_REQUEST_ID;
					String requestId = wrappedRequest.getHeaders().getFirst(header);
					Assert.state(requestId != null, () -> "No \"" + header + "\" header");
					WiretapClientHttpResponse wrappedResponse = new WiretapClientHttpResponse(response);
					this.exchanges.put(requestId, new Info(wrappedRequest, wrappedResponse));
					return wrappedResponse;
				});
	}

	/**
	 * Retrieve the {@link Info} for the given "request-id" header value.
	 */
	public Info claimRequest(String requestId) {
		Info info = this.exchanges.remove(requestId);
		Assert.state(info != null, () -> {
			String header = WebTestClient.WEBTESTCLIENT_REQUEST_ID;
			return "No match for " + header + "=" + requestId;
		});
		return info;
	}


	class Info {

		private final WiretapClientHttpRequest request;

		private final WiretapClientHttpResponse response;


		public Info(WiretapClientHttpRequest request, WiretapClientHttpResponse response) {
			this.request = request;
			this.response = response;
		}


		public ExchangeResult createExchangeResult(@Nullable  String uriTemplate) {
			return new ExchangeResult(this.request, this.response,
					this.request.getContent(), this.response.getContent(), uriTemplate);
		}
	}


	/**
	 * ClientHttpRequestDecorator that intercepts and saves the request body.
	 */
	private static class WiretapClientHttpRequest extends ClientHttpRequestDecorator {

		private final DataBuffer buffer;

		private final MonoProcessor<byte[]> body = MonoProcessor.create();


		public WiretapClientHttpRequest(ClientHttpRequest delegate) {
			super(delegate);
			this.buffer = bufferFactory.allocateBuffer();
		}


		/**
		 * Return a "promise" with the request body content written to the server.
		 */
		public MonoProcessor<byte[]> getContent() {
			return this.body;
		}


		@Override
		public Mono<Void> writeWith(Publisher<? extends DataBuffer> publisher) {
			return super.writeWith(
					Flux.from(publisher)
							.doOnNext(this::handleOnNext)
							.doOnError(this::handleError)
							.doOnCancel(this::handleOnComplete)
							.doOnComplete(this::handleOnComplete));
		}

		@Override
		public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> publisher) {
			return super.writeAndFlushWith(
					Flux.from(publisher)
							.map(p -> Flux.from(p).doOnNext(this::handleOnNext).doOnError(this::handleError))
							.doOnError(this::handleError)
							.doOnCancel(this::handleOnComplete)
							.doOnComplete(this::handleOnComplete));
		}

		@Override
		public Mono<Void> setComplete() {
			handleOnComplete();
			return super.setComplete();
		}

		private void handleOnNext(DataBuffer buffer) {
			this.buffer.write(buffer);
		}

		private void handleError(Throwable ex) {
			if (!this.body.isTerminated()) {
				this.body.onError(ex);
			}
		}

		private void handleOnComplete() {
			if (!this.body.isTerminated()) {
				byte[] bytes = new byte[this.buffer.readableByteCount()];
				this.buffer.read(bytes);
				this.body.onNext(bytes);
			}
		}
	}


	/**
	 * ClientHttpResponseDecorator that intercepts and saves the response body.
	 */
	private static class WiretapClientHttpResponse extends ClientHttpResponseDecorator {

		private final DataBuffer buffer;

		private final MonoProcessor<byte[]> body = MonoProcessor.create();


		public WiretapClientHttpResponse(ClientHttpResponse delegate) {
			super(delegate);
			this.buffer = bufferFactory.allocateBuffer();
		}


		/**
		 * Return a "promise" with the response body content read from the server.
		 */
		public MonoProcessor<byte[]> getContent() {
			return this.body;
		}

		@Override
		public Flux<DataBuffer> getBody() {
			return super.getBody()
					.doOnNext(buffer::write)
					.doOnError(body::onError)
					.doOnCancel(this::handleOnComplete)
					.doOnComplete(this::handleOnComplete);
		}

		private void handleOnComplete() {
			if (!this.body.isTerminated()) {
				byte[] bytes = new byte[this.buffer.readableByteCount()];
				this.buffer.read(bytes);
				this.body.onNext(bytes);
			}
		}
	}

}
