/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.socket.client;

import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.net.ssl.SSLContext;

import io.undertow.connector.ByteBufferPool;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.client.WebSocketClient.ConnectionBuilder;
import io.undertow.websockets.client.WebSocketClientNegotiation;
import io.undertow.websockets.core.WebSocketChannel;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.ssl.XnioSsl;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
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
public class UndertowWebSocketClient extends WebSocketClientSupport implements WebSocketClient {

	private static final int DEFAULT_BUFFER_SIZE = 8192;

	private static XnioWorker worker;

	static {
		try {
			worker = Xnio.getInstance().createWorker(OptionMap.builder()
					.set(Options.WORKER_IO_THREADS, 2)
					.set(Options.CONNECTION_HIGH_WATER, 1000000)
					.set(Options.CONNECTION_LOW_WATER, 1000000)
					.set(Options.WORKER_TASK_CORE_THREADS, 30)
					.set(Options.WORKER_TASK_MAX_THREADS, 30)
					.set(Options.TCP_NODELAY, true)
					.set(Options.CORK, true)
					.getMap());
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}


	private final Function<URI, ConnectionBuilder> builder;

	private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	/**
	 * Default constructor that uses
	 * {@link io.undertow.websockets.client.WebSocketClient#connectionBuilder(XnioWorker, ByteBufferPool, URI)}
	 * to create WebSocket connections.
	 */
	public UndertowWebSocketClient() {
		this(UndertowWebSocketClient::createDefaultConnectionBuilder);
	}

	/**
	 * Constructor that accepts a {@link Function} to prepare a
	 * {@link ConnectionBuilder} for WebSocket connections.
	 * @param builder a connection builder that can be used to create a web socket connection.
	 */
	public UndertowWebSocketClient(Function<URI, ConnectionBuilder> builder) {
		this.builder = builder;
	}

	private static ConnectionBuilder createDefaultConnectionBuilder(URI url) {

		ConnectionBuilder builder = io.undertow.websockets.client.WebSocketClient.connectionBuilder(
				worker, new DefaultByteBufferPool(false, DEFAULT_BUFFER_SIZE), url);

		boolean secure = "wss".equals(url.getScheme());
		if (secure) {
			try {
				XnioSsl ssl = new UndertowXnioSsl(Xnio.getInstance(), OptionMap.EMPTY, SSLContext.getDefault());
				builder.setSsl(ssl);
			}
			catch (NoSuchAlgorithmException ex) {
				throw new RuntimeException("Failed to create Undertow ConnectionBuilder for " + url, ex);
			}
		}

		return builder;
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
					String[] protocols = beforeHandshake(url, headers, handler);
					DefaultNegotiation negotiation = new DefaultNegotiation(protocols, headers);

					return this.builder.apply(url)
							.setClientNegotiation(negotiation)
							.connect()
							.addNotifier(new IoFuture.HandlingNotifier<WebSocketChannel, Object>() {

								@Override
								public void handleDone(WebSocketChannel channel, Object attachment) {
									handleChannel(url, handler, completion, negotiation, channel);
								}

								@Override
								public void handleFailed(IOException ex, Object attachment) {
									completion.onError(new IllegalStateException("Failed to connect", ex));
								}
							}, null);
				})
				.then(completion);
	}

	private void handleChannel(URI url, WebSocketHandler handler, MonoProcessor<Void> completion,
			DefaultNegotiation negotiation, WebSocketChannel channel) {

		HandshakeInfo info = afterHandshake(url, negotiation.getResponseHeaders());
		UndertowWebSocketSession session = new UndertowWebSocketSession(channel, info, bufferFactory, completion);
		UndertowWebSocketHandlerAdapter adapter = new UndertowWebSocketHandlerAdapter(session);

		channel.getReceiveSetter().set(adapter);
		channel.resumeReceives();

		handler.handle(session).subscribe(session);
	}


	private static final class DefaultNegotiation extends WebSocketClientNegotiation {

		private final HttpHeaders requestHeaders;

		private HttpHeaders responseHeaders = new HttpHeaders();


		public DefaultNegotiation(String[] subProtocols, HttpHeaders requestHeaders) {
			super(Arrays.asList(subProtocols), Collections.emptyList());
			this.requestHeaders = requestHeaders;
		}


		public HttpHeaders getResponseHeaders() {
			return this.responseHeaders;
		}

		@Override
		public void beforeRequest(Map<String, List<String>> headers) {
			this.requestHeaders.forEach(headers::put);
		}

		@Override
		public void afterRequest(Map<String, List<String>> headers) {
			headers.forEach((k, v) -> this.responseHeaders.put(k, v));
		}
	}

}
