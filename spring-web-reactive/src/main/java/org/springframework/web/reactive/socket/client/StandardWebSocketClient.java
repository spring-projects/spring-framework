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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.ClientEndpointConfig.Configurator;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.StandardEndpoint;

/**
 * A Java WebSocket API (JSR-356) based implementation of
 * {@link WebSocketClient}.
 * 
 * @author Violeta Georgieva
 * @since 5.0
 */
public class StandardWebSocketClient extends WebSocketClientSupport implements WebSocketClient {

	private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

	private final WebSocketContainer wsContainer;


	/**
	 * Default constructor that calls {@code ContainerProvider.getWebSocketContainer()}
	 * to obtain a (new) {@link WebSocketContainer} instance.
	 */
	public StandardWebSocketClient() {
		this(ContainerProvider.getWebSocketContainer());
	}

	/**
	 * Constructor accepting an existing {@link WebSocketContainer} instance.
	 * @param wsContainer a web socket container
	 */
	public StandardWebSocketClient(WebSocketContainer wsContainer) {
		this.wsContainer = wsContainer;
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
		return Mono.fromCallable(() -> {
					StandardWebSocketClientConfigurator configurator =
							new StandardWebSocketClientConfigurator(headers);

					ClientEndpointConfig endpointConfig = createClientEndpointConfig(
							configurator, beforeHandshake(url, headers, handler));

					HandshakeInfo info = new HandshakeInfo(url, Mono.empty());

					Endpoint endpoint = new StandardClientEndpoint(handler, info,
							this.bufferFactory, configurator, processor);

					Session session = this.wsContainer.connectToServer(endpoint, endpointConfig, url);
					return session;
				}).then(processor);
	}

	private ClientEndpointConfig createClientEndpointConfig(
			StandardWebSocketClientConfigurator configurator, String[] subProtocols) {

		return ClientEndpointConfig.Builder.create()
				.configurator(configurator)
				.preferredSubprotocols(Arrays.asList(subProtocols))
				.build();
	}


	private static final class StandardClientEndpoint extends StandardEndpoint {

		private final StandardWebSocketClientConfigurator configurator;

		public StandardClientEndpoint(WebSocketHandler handler, HandshakeInfo info,
				DataBufferFactory bufferFactory, StandardWebSocketClientConfigurator configurator,
				MonoProcessor<Void> processor) {
			super(handler, info, bufferFactory, processor);
			this.configurator = configurator;
		}

		@Override
		public void onOpen(Session nativeSession, EndpointConfig config) {
			getHandshakeInfo().setHeaders(this.configurator.getResponseHeaders());
			getHandshakeInfo().setSubProtocol(
					Optional.ofNullable(nativeSession.getNegotiatedSubprotocol()));

			super.onOpen(nativeSession, config);
		}
	}


	private static final class StandardWebSocketClientConfigurator extends Configurator {

		private final HttpHeaders requestHeaders;

		private HttpHeaders responseHeaders = new HttpHeaders();

		public StandardWebSocketClientConfigurator(HttpHeaders requestHeaders) {
			this.requestHeaders = requestHeaders;
		}

		@Override
		public void beforeRequest(Map<String, List<String>> requestHeaders) {
			requestHeaders.putAll(this.requestHeaders);
		}

		@Override
		public void afterResponse(HandshakeResponse response) {
			response.getHeaders().forEach((k, v) -> responseHeaders.put(k, v));
		}

		public HttpHeaders getResponseHeaders() {
			return this.responseHeaders;
		}
	}

}