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

import java.net.URI;
import java.util.Optional;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.context.Lifecycle;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketHandlerAdapter;

/**
 * A Jetty based implementation of {@link WebSocketClient}.
 * 
 * @author Violeta Georgieva
 * @since 5.0
 */
public class JettyWebSocketClient extends WebSocketClientSupport implements WebSocketClient, Lifecycle {

	private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

	private final org.eclipse.jetty.websocket.client.WebSocketClient wsClient;

	private final Object lifecycleMonitor = new Object();


	/**
	 * Default constructor that creates an instance of
	 * {@link org.eclipse.jetty.websocket.client.WebSocketClient}.
	 */
	public JettyWebSocketClient() {
		this(new org.eclipse.jetty.websocket.client.WebSocketClient());
	}

	/**
	 * Constructor that accepts an existing
	 * {@link org.eclipse.jetty.websocket.client.WebSocketClient} instance.
	 * @param wsClient a web socket client
	 */
	public JettyWebSocketClient(org.eclipse.jetty.websocket.client.WebSocketClient wsClient) {
		this.wsClient = wsClient;
	}


	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {
		return connectInternal(url, headers, handler);
	}

	private Mono<Void> connectInternal(URI url, HttpHeaders headers, WebSocketHandler handler) {
		MonoProcessor<Void> processor = MonoProcessor.create();
		return Mono.fromCallable(
				() -> {
					HandshakeInfo info = new HandshakeInfo(url, Mono.empty());
					Object adapter = new JettyClientAdapter(handler, info, this.bufferFactory, processor);
					ClientUpgradeRequest request = createRequest(url, headers, handler);
					return this.wsClient.connect(adapter, url, request);
				})
				.then(processor);
	}

	private ClientUpgradeRequest createRequest(URI url, HttpHeaders headers, WebSocketHandler handler) {
		ClientUpgradeRequest request = new ClientUpgradeRequest();

		String[] protocols = beforeHandshake(url, headers, handler);
		if (!ObjectUtils.isEmpty(protocols)) {
			request.setSubProtocols(protocols);
		}

		headers.forEach((k, v) -> request.setHeader(k, v));

		return request;
	}


	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning()) {
				try {
					this.wsClient.start();
				}
				catch (Exception ex) {
					throw new IllegalStateException("Failed to start Jetty WebSocketClient", ex);
				}
			}
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (isRunning()) {
				try {
					this.wsClient.stop();
				}
				catch (Exception ex) {
					throw new IllegalStateException("Error stopping Jetty WebSocketClient", ex);
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.wsClient.isStarted();
		}
	}


	@WebSocket
	private static final class JettyClientAdapter extends JettyWebSocketHandlerAdapter {

		public JettyClientAdapter(WebSocketHandler delegate,
				HandshakeInfo info, DataBufferFactory bufferFactory, MonoProcessor<Void> processor) {
			super(delegate, info, bufferFactory, processor);
		}

		@Override
		public void onWebSocketConnect(Session session) {
			UpgradeResponse response = session.getUpgradeResponse();

			getHandshakeInfo().setHeaders(getResponseHeaders(response));
			getHandshakeInfo().setSubProtocol(
					Optional.ofNullable(response.getAcceptedSubProtocol()));

			super.onWebSocketConnect(session);
		}

		private HttpHeaders getResponseHeaders(UpgradeResponse response) {
			HttpHeaders responseHeaders = new HttpHeaders();
			response.getHeaders().forEach((k, v) -> responseHeaders.put(k, v));
			return responseHeaders;
		}

	}

}