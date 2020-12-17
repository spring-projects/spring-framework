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

package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.core.io.buffer.DataBuffer;
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

	private final ClientHttpConnector delegate;

	private final Map<String, ClientExchangeInfo> exchanges = new ConcurrentHashMap<>();


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
					this.exchanges.put(requestId, new ClientExchangeInfo(wrappedRequest, wrappedResponse));
					return wrappedResponse;
				});
	}

	/**
	 * Create the {@link ExchangeResult} for the given "request-id" header value.
	 */
	ExchangeResult getExchangeResult(String requestId, @Nullable String uriTemplate, Duration timeout) {
		ClientExchangeInfo clientInfo = this.exchanges.remove(requestId);
		Assert.state(clientInfo != null, () -> {
			String header = WebTestClient.WEBTESTCLIENT_REQUEST_ID;
			return "No match for " + header + "=" + requestId;
		});
		return new ExchangeResult(clientInfo.getRequest(), clientInfo.getResponse(),
				clientInfo.getRequest().getRecorder().getContent(),
				clientInfo.getResponse().getRecorder().getContent(),
				timeout, uriTemplate,
				clientInfo.getResponse().getMockServerResult());
	}


	/**
	 * Holder for {@link WiretapClientHttpRequest} and {@link WiretapClientHttpResponse}.
	 */
	private static class ClientExchangeInfo {

		private final WiretapClientHttpRequest request;

		private final WiretapClientHttpResponse response;

		public ClientExchangeInfo(WiretapClientHttpRequest request, WiretapClientHttpResponse response) {
			this.request = request;
			this.response = response;
		}

		public WiretapClientHttpRequest getRequest() {
			return this.request;
		}

		public WiretapClientHttpResponse getResponse() {
			return this.response;
		}
	}


	/**
	 * Tap into a Publisher of data buffers to save the content.
	 */
	final static class WiretapRecorder {

		@Nullable
		private final Flux<? extends DataBuffer> publisher;

		@Nullable
		private final Flux<? extends Publisher<? extends DataBuffer>> publisherNested;

		private final DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.allocateBuffer();

		// unsafe(): we're intercepting, already serialized Publisher signals
		private final Sinks.One<byte[]> content = Sinks.unsafe().one();

		private boolean hasContentConsumer;


		public WiretapRecorder(@Nullable Publisher<? extends DataBuffer> publisher,
				@Nullable Publisher<? extends Publisher<? extends DataBuffer>> publisherNested) {

			if (publisher != null && publisherNested != null) {
				throw new IllegalArgumentException("At most one publisher expected");
			}

			this.publisher = publisher != null ?
					Flux.from(publisher)
							.doOnSubscribe(s -> this.hasContentConsumer = true)
							.doOnNext(this.buffer::write)
							.doOnError(this::handleOnError)
							.doOnCancel(this::handleOnComplete)
							.doOnComplete(this::handleOnComplete) : null;

			this.publisherNested = publisherNested != null ?
					Flux.from(publisherNested)
							.doOnSubscribe(s -> this.hasContentConsumer = true)
							.map(p -> Flux.from(p).doOnNext(this.buffer::write).doOnError(this::handleOnError))
							.doOnError(this::handleOnError)
							.doOnCancel(this::handleOnComplete)
							.doOnComplete(this::handleOnComplete) : null;

			if (publisher == null && publisherNested == null) {
				this.content.tryEmitEmpty();
			}
		}


		public Publisher<? extends DataBuffer> getPublisherToUse() {
			Assert.notNull(this.publisher, "Publisher not in use.");
			return this.publisher;
		}

		public Publisher<? extends Publisher<? extends DataBuffer>> getNestedPublisherToUse() {
			Assert.notNull(this.publisherNested, "Nested publisher not in use.");
			return this.publisherNested;
		}

		public Mono<byte[]> getContent() {
			return Mono.defer(() -> {
				if (this.content.scan(Scannable.Attr.TERMINATED) == Boolean.TRUE) {
					return this.content.asMono();
				}
				if (!this.hasContentConsumer) {
					// Couple of possible cases:
					//  1. Mock server never consumed request body (e.g. error before read)
					//  2. FluxExchangeResult: getResponseBodyContent called before getResponseBody
					//noinspection ConstantConditions
					(this.publisher != null ? this.publisher : this.publisherNested)
							.onErrorMap(ex -> new IllegalStateException(
									"Content has not been consumed, and " +
											"an error was raised while attempting to produce it.", ex))
							.subscribe();
				}
				return this.content.asMono();
			});
		}


		private void handleOnError(Throwable ex) {
			// Ignore result: signals cannot compete
			this.content.tryEmitError(ex);
		}

		private void handleOnComplete() {
			byte[] bytes = new byte[this.buffer.readableByteCount()];
			this.buffer.read(bytes);
			// Ignore result: signals cannot compete
			this.content.tryEmitValue(bytes);
		}
	}


	/**
	 * ClientHttpRequestDecorator that intercepts and saves the request body.
	 */
	private static class WiretapClientHttpRequest extends ClientHttpRequestDecorator {

		@Nullable
		private WiretapRecorder recorder;


		public WiretapClientHttpRequest(ClientHttpRequest delegate) {
			super(delegate);
		}

		public WiretapRecorder getRecorder() {
			Assert.notNull(this.recorder, "No WiretapRecorder: was the client request written?");
			return this.recorder;
		}

		@Override
		public Mono<Void> writeWith(Publisher<? extends DataBuffer> publisher) {
			this.recorder = new WiretapRecorder(publisher, null);
			return super.writeWith(this.recorder.getPublisherToUse());
		}

		@Override
		public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> publisher) {
			this.recorder = new WiretapRecorder(null, publisher);
			return super.writeAndFlushWith(this.recorder.getNestedPublisherToUse());
		}

		@Override
		public Mono<Void> setComplete() {
			this.recorder = new WiretapRecorder(null, null);
			return super.setComplete();
		}
	}


	/**
	 * ClientHttpResponseDecorator that intercepts and saves the response body.
	 */
	private static class WiretapClientHttpResponse extends ClientHttpResponseDecorator {

		private final WiretapRecorder recorder;


		public WiretapClientHttpResponse(ClientHttpResponse delegate) {
			super(delegate);
			this.recorder = new WiretapRecorder(super.getBody(), null);
		}


		public WiretapRecorder getRecorder() {
			return this.recorder;
		}

		@Override
		@SuppressWarnings("ConstantConditions")
		public Flux<DataBuffer> getBody() {
			return Flux.from(this.recorder.getPublisherToUse());
		}

		@Nullable
		public Object getMockServerResult() {
			return (getDelegate() instanceof MockServerClientHttpResponse ?
					((MockServerClientHttpResponse) getDelegate()).getServerResult() : null);
		}
	}

}
