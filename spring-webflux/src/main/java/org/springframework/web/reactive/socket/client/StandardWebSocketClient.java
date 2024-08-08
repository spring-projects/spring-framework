/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.List;
import java.util.Map;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketSession;

/**
 * {@link WebSocketClient} implementation for use with the Jakarta WebSocket API.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see <a href="https://www.jcp.org/en/jsr/detail?id=356">https://www.jcp.org/en/jsr/detail?id=356</a>
 */
public class StandardWebSocketClient implements WebSocketClient {

	private static final Log logger = LogFactory.getLog(StandardWebSocketClient.class);


	private final WebSocketContainer webSocketContainer;


	/**
	 * Default constructor that calls
	 * {@code ContainerProvider.getWebSocketContainer()} to obtain a (new)
	 * {@link WebSocketContainer} instance.
	 */
	public StandardWebSocketClient() {
		this(ContainerProvider.getWebSocketContainer());
	}

	/**
	 * Constructor accepting an existing {@link WebSocketContainer} instance.
	 * @param webSocketContainer a web socket container
	 */
	public StandardWebSocketClient(WebSocketContainer webSocketContainer) {
		this.webSocketContainer = webSocketContainer;
	}


	/**
	 * Return the configured {@link WebSocketContainer} to use.
	 */
	public WebSocketContainer getWebSocketContainer() {
		return this.webSocketContainer;
	}


	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {
		return executeInternal(url, headers, handler);
	}

	private Mono<Void> executeInternal(URI url, HttpHeaders requestHeaders, WebSocketHandler handler) {
		Sinks.Empty<Void> completion = Sinks.empty();
		return Mono.deferContextual(
				contextView -> {
					if (logger.isDebugEnabled()) {
						logger.debug("Connecting to " + url);
					}
					List<String> protocols = handler.getSubProtocols();
					DefaultConfigurator configurator = new DefaultConfigurator(requestHeaders);
					Endpoint endpoint = createEndpoint(
							url, ContextWebSocketHandler.decorate(handler, contextView), completion, configurator);
					ClientEndpointConfig config = createEndpointConfig(configurator, protocols);
					try {
						this.webSocketContainer.connectToServer(endpoint, config, url);
						return completion.asMono();
					}
					catch (Exception ex) {
						return Mono.error(ex);
					}
				})
				.subscribeOn(Schedulers.boundedElastic());  // connectToServer is blocking
	}

	private StandardWebSocketHandlerAdapter createEndpoint(URI url, WebSocketHandler handler,
			Sinks.Empty<Void> completionSink, DefaultConfigurator configurator) {

		return new StandardWebSocketHandlerAdapter(handler, session ->
				createWebSocketSession(session, createHandshakeInfo(url, configurator), completionSink));
	}

	private HandshakeInfo createHandshakeInfo(URI url, DefaultConfigurator configurator) {
		HttpHeaders responseHeaders = configurator.getResponseHeaders();
		String protocol = responseHeaders.getFirst("Sec-WebSocket-Protocol");
		return new HandshakeInfo(url, responseHeaders, Mono.empty(), protocol);
	}

	/**
	 * Create the {@link StandardWebSocketSession} for the given Jakarta WebSocket Session.
	 * @see #bufferFactory()
	 */
	protected StandardWebSocketSession createWebSocketSession(
			Session session, HandshakeInfo info, Sinks.Empty<Void> completionSink) {

		return new StandardWebSocketSession(session, info, bufferFactory(), completionSink);
	}

	/**
	 * Return the {@link DataBufferFactory} to use.
	 * @see #createWebSocketSession
	 */
	protected DataBufferFactory bufferFactory() {
		return DefaultDataBufferFactory.sharedInstance;
	}

	/**
	 * Create the {@link ClientEndpointConfig} for the given configurator.
	 * Can be overridden to add extensions or an SSL context.
	 * @param configurator the configurator to apply
	 * @param subProtocols the preferred sub-protocols
	 * @since 6.1.3
	 */
	protected ClientEndpointConfig createEndpointConfig(Configurator configurator, List<String> subProtocols) {
		return ClientEndpointConfig.Builder.create()
				.configurator(configurator)
				.preferredSubprotocols(subProtocols)
				.build();
	}


	private static final class DefaultConfigurator extends Configurator {

		private final HttpHeaders requestHeaders;

		private final HttpHeaders responseHeaders = new HttpHeaders();

		public DefaultConfigurator(HttpHeaders requestHeaders) {
			this.requestHeaders = requestHeaders;
		}

		public HttpHeaders getResponseHeaders() {
			return this.responseHeaders;
		}

		@Override
		public void beforeRequest(Map<String, List<String>> requestHeaders) {
			requestHeaders.putAll(this.requestHeaders);
		}

		@Override
		public void afterResponse(HandshakeResponse response) {
			this.responseHeaders.putAll(response.getHeaders());
		}
	}

}
