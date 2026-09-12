/*
 * Copyright 2002-present the original author or authors.
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.JdkWebSocketSession;

/**
 * {@link WebSocketClient} implementation for the JDK {@link HttpClient}.
 *
 * <p>The JDK API does not expose handshake response headers. Therefore,
 * {@link HandshakeInfo#getHeaders()} is empty, while the negotiated sub-protocol
 * is available through {@link HandshakeInfo#getSubProtocol()}.
 *
 * @author Goutam Adwant
 * @since 7.1
 */
public class JdkWebSocketClient implements WebSocketClient {

	private final HttpClient httpClient;

	private int maxMessageSize = 64 * 1024;


	/**
	 * Create a client with a default {@link HttpClient}.
	 */
	public JdkWebSocketClient() {
		this(HttpClient.newHttpClient());
	}

	/**
	 * Create a client with the given {@link HttpClient}.
	 * @param httpClient the client to use
	 */
	public JdkWebSocketClient(HttpClient httpClient) {
		Assert.notNull(httpClient, "HttpClient must not be null");
		this.httpClient = httpClient;
	}


	/**
	 * Return the configured {@link HttpClient}.
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * Set the maximum size in bytes of an incoming text or binary message,
	 * including all of its fragments. The default is 64 KiB.
	 */
	public void setMaxMessageSize(int maxMessageSize) {
		Assert.isTrue(maxMessageSize > 0, "Max message size must be positive");
		this.maxMessageSize = maxMessageSize;
	}

	/**
	 * Return the maximum incoming message size in bytes.
	 */
	public int getMaxMessageSize() {
		return this.maxMessageSize;
	}


	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {
		return Mono.defer(() -> {
			WebSocket.Builder builder = this.httpClient.newWebSocketBuilder();
			headers.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
			List<String> protocols = handler.getSubProtocols();
			if (!protocols.isEmpty()) {
				builder.subprotocols(protocols.get(0), protocols.subList(1, protocols.size()).toArray(String[]::new));
			}
			JdkWebSocketListener listener = new JdkWebSocketListener(url, this.maxMessageSize);
			return Mono.fromFuture(() -> {
						CompletableFuture<WebSocket> future = builder.buildAsync(url, listener);
						future.thenAccept(listener::setWebSocket);
						return future;
					})
					.then(listener.sessionReady.asMono())
					.flatMap(session -> Mono.defer(() -> handler.handle(session)).then(Mono.defer(session::close)))
					.takeUntilOther(listener.errors.asMono())
					.doFinally(signal -> listener.abort());
		});
	}


	private static final class JdkWebSocketListener implements WebSocket.Listener {

		private final URI url;

		private final int maxMessageSize;

		private final Sinks.Empty<Void> errors = Sinks.empty();

		private final Sinks.One<JdkWebSocketSession> sessionReady = Sinks.one();

		private volatile @Nullable WebSocket webSocket;

		private volatile @Nullable JdkWebSocketSession session;

		private volatile boolean aborted;

		JdkWebSocketListener(URI url, int maxMessageSize) {
			this.url = url;
			this.maxMessageSize = maxMessageSize;
		}

		JdkWebSocketSession getSession() {
			JdkWebSocketSession session = this.session;
			Assert.state(session != null, "WebSocket session must be initialized");
			return session;
		}

		void setWebSocket(WebSocket webSocket) {
			this.webSocket = webSocket;
			if (this.aborted) {
				webSocket.abort();
			}
		}

		void abort() {
			this.aborted = true;
			JdkWebSocketSession session = this.session;
			if (session != null) {
				session.abort();
			}
			else {
				WebSocket webSocket = this.webSocket;
				if (webSocket != null) {
					webSocket.abort();
				}
			}
		}

		@Override
		public void onOpen(WebSocket webSocket) {
			String protocol = webSocket.getSubprotocol();
			HandshakeInfo info = new HandshakeInfo(this.url, HttpHeaders.EMPTY, Mono.empty(),
					(!protocol.isEmpty() ? protocol : null));
			this.session = new JdkWebSocketSession(webSocket, info,
					DefaultDataBufferFactory.sharedInstance, this.maxMessageSize);
			if (this.aborted) {
				this.session.abort();
			}
			this.sessionReady.tryEmitValue(this.session);
		}

		@Override
		public @Nullable CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			return getSession().onText(webSocket, data, last);
		}

		@Override
		public @Nullable CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
			return getSession().onBinary(webSocket, data, last);
		}

		@Override
		public @Nullable CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
			return getSession().onPing(webSocket, message);
		}

		@Override
		public @Nullable CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
			return getSession().onPong(webSocket, message);
		}

		@Override
		public @Nullable CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
			try {
				CloseStatus.create(statusCode, reason);
				return getSession().onClose(webSocket, statusCode, reason);
			}
			catch (IllegalArgumentException ex) {
				onError(webSocket, ex);
				return null;
			}
		}

		@Override
		public void onError(WebSocket webSocket, Throwable error) {
			getSession().onError(webSocket, error);
			this.errors.tryEmitError(error);
		}
	}

}
