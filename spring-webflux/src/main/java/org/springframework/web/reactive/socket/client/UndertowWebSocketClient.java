/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.socket.client;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.client.WebSocketClient.ConnectionBuilder;
import io.undertow.websockets.client.WebSocketClientNegotiation;
import io.undertow.websockets.core.WebSocketChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xnio.IoFuture;
import org.xnio.XnioWorker;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.UndertowWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.UndertowWebSocketSession;

/**
 * Undertow based implementation of {@link WebSocketClient}.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class UndertowWebSocketClient implements WebSocketClient {

	private static final Log logger = LogFactory.getLog(UndertowWebSocketClient.class);

	private static final int DEFAULT_POOL_BUFFER_SIZE = 8192;


	private final XnioWorker worker;

	private ByteBufferPool byteBufferPool;

	private final Consumer<ConnectionBuilder> builderConsumer;

	private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	/**
	 * Constructor with the {@link XnioWorker} to pass to
	 * {@link io.undertow.websockets.client.WebSocketClient#connectionBuilder}.
	 * @param worker the Xnio worker
	 */
	public UndertowWebSocketClient(XnioWorker worker) {
		this(worker, builder -> {});
	}

	/**
	 * Alternate constructor providing additional control over the
	 * {@link ConnectionBuilder} for each WebSocket connection.
	 * @param worker the Xnio worker to use to create {@code ConnectionBuilder}'s
	 * @param builderConsumer a consumer to configure {@code ConnectionBuilder}'s
	 */
	public UndertowWebSocketClient(XnioWorker worker, Consumer<ConnectionBuilder> builderConsumer) {
		this(worker, new DefaultByteBufferPool(false, DEFAULT_POOL_BUFFER_SIZE), builderConsumer);
	}

	/**
	 * Alternate constructor providing additional control over the
	 * {@link ConnectionBuilder} for each WebSocket connection.
	 * @param worker the Xnio worker to use to create {@code ConnectionBuilder}'s
	 * @param byteBufferPool the ByteBufferPool to use to create {@code ConnectionBuilder}'s
	 * @param builderConsumer a consumer to configure {@code ConnectionBuilder}'s
	 * @since 5.0.8
	 */
	public UndertowWebSocketClient(XnioWorker worker, ByteBufferPool byteBufferPool,
			Consumer<ConnectionBuilder> builderConsumer) {

		Assert.notNull(worker, "XnioWorker must not be null");
		Assert.notNull(byteBufferPool, "ByteBufferPool must not be null");
		this.worker = worker;
		this.byteBufferPool = byteBufferPool;
		this.builderConsumer = builderConsumer;
	}


	/**
	 * Return the configured {@link XnioWorker}.
	 */
	public XnioWorker getXnioWorker() {
		return this.worker;
	}

	/**
	 * Set the {@link io.undertow.connector.ByteBufferPool ByteBufferPool} to pass to
	 * {@link io.undertow.websockets.client.WebSocketClient#connectionBuilder}.
	 * <p>By default an indirect {@link io.undertow.server.DefaultByteBufferPool}
	 * with a buffer size of 8192 is used.
	 * @since 5.0.8
	 * @see #DEFAULT_POOL_BUFFER_SIZE
	 */
	public void setByteBufferPool(ByteBufferPool byteBufferPool) {
		Assert.notNull(byteBufferPool, "ByteBufferPool must not be null");
		this.byteBufferPool = byteBufferPool;
	}

	/**
	 * Return the {@link io.undertow.connector.ByteBufferPool} currently used
	 * for newly created WebSocket sessions by this client.
	 * @return the byte buffer pool
	 * @since 5.0.8
	 */
	public ByteBufferPool getByteBufferPool() {
		return this.byteBufferPool;
	}

	/**
	 * Return the configured <code>Consumer&lt;ConnectionBuilder&gt;</code>.
	 */
	public Consumer<ConnectionBuilder> getConnectionBuilderConsumer() {
		return this.builderConsumer;
	}


	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {
		return executeInternal(url, headers, handler);
	}

	private Mono<Void> executeInternal(URI url, HttpHeaders headers, WebSocketHandler handler) {
		MonoProcessor<Void> completion = MonoProcessor.create();
		return Mono.fromCallable(
				() -> {
					if (logger.isDebugEnabled()) {
						logger.debug("Connecting to " + url);
					}
					List<String> protocols = handler.getSubProtocols();
					ConnectionBuilder builder = createConnectionBuilder(url);
					DefaultNegotiation negotiation = new DefaultNegotiation(protocols, headers, builder);
					builder.setClientNegotiation(negotiation);
					return builder.connect().addNotifier(
							new IoFuture.HandlingNotifier<WebSocketChannel, Object>() {
								@Override
								public void handleDone(WebSocketChannel channel, Object attachment) {
									handleChannel(url, handler, completion, negotiation, channel);
								}
								@Override
								public void handleFailed(IOException ex, Object attachment) {
									completion.onError(new IllegalStateException("Failed to connect to " + url, ex));
								}
							}, null);
				})
				.then(completion);
	}

	/**
	 * Create a {@link ConnectionBuilder} for the given URI.
	 * <p>The default implementation creates a builder with the configured
	 * {@link #getXnioWorker() XnioWorker} and {@link #getByteBufferPool() ByteBufferPool} and
	 * then passes it to the {@link #getConnectionBuilderConsumer() consumer}
	 * provided at construction time.
	 */
	protected ConnectionBuilder createConnectionBuilder(URI url) {
		ConnectionBuilder builder = io.undertow.websockets.client.WebSocketClient
				.connectionBuilder(getXnioWorker(), getByteBufferPool(), url);
		this.builderConsumer.accept(builder);
		return builder;
	}

	private void handleChannel(URI url, WebSocketHandler handler, MonoProcessor<Void> completion,
			DefaultNegotiation negotiation, WebSocketChannel channel) {

		HandshakeInfo info = createHandshakeInfo(url, negotiation);
		UndertowWebSocketSession session = new UndertowWebSocketSession(channel, info, this.bufferFactory, completion);
		UndertowWebSocketHandlerAdapter adapter = new UndertowWebSocketHandlerAdapter(session);

		channel.getReceiveSetter().set(adapter);
		channel.resumeReceives();

		handler.handle(session)
				.checkpoint(url + " [UndertowWebSocketClient]")
				.subscribe(session);
	}

	private HandshakeInfo createHandshakeInfo(URI url, DefaultNegotiation negotiation) {
		HttpHeaders responseHeaders = negotiation.getResponseHeaders();
		String protocol = responseHeaders.getFirst("Sec-WebSocket-Protocol");
		return new HandshakeInfo(url, responseHeaders, Mono.empty(), protocol);
	}


	private static final class DefaultNegotiation extends WebSocketClientNegotiation {

		private final HttpHeaders requestHeaders;

		private final HttpHeaders responseHeaders = new HttpHeaders();

		@Nullable
		private final WebSocketClientNegotiation delegate;

		public DefaultNegotiation(List<String> protocols, HttpHeaders requestHeaders,
				ConnectionBuilder connectionBuilder) {

			super(protocols, Collections.emptyList());
			this.requestHeaders = requestHeaders;
			this.delegate = connectionBuilder.getClientNegotiation();
		}

		public HttpHeaders getResponseHeaders() {
			return this.responseHeaders;
		}

		@Override
		public void beforeRequest(Map<String, List<String>> headers) {
			this.requestHeaders.forEach(headers::put);
			if (this.delegate != null) {
				this.delegate.beforeRequest(headers);
			}
		}

		@Override
		public void afterRequest(Map<String, List<String>> headers) {
			headers.forEach(this.responseHeaders::put);
			if (this.delegate != null) {
				this.delegate.afterRequest(headers);
			}
		}
	}

}
