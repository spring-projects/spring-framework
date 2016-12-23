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
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.scheduler.Schedulers;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketSession;

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
		return executeInternal(url, headers, handler);
	}

	private Mono<Void> executeInternal(URI url, HttpHeaders requestHeaders, WebSocketHandler handler) {
		MonoProcessor<Void> completionMono = MonoProcessor.create();
		return Mono.fromCallable(
				() -> {
					String[] subProtocols = beforeHandshake(url, requestHeaders, handler);
					DefaultConfigurator configurator = new DefaultConfigurator(requestHeaders);
					ClientEndpointConfig config = createEndpointConfig(configurator, subProtocols);
					Endpoint endpoint = createEndpoint(url, handler, completionMono, configurator);
					return this.wsContainer.connectToServer(endpoint, config, url);
				})
				.subscribeOn(Schedulers.elastic()) // connectToServer is blocking
				.then(completionMono);
	}

	private ClientEndpointConfig createEndpointConfig(Configurator configurator, String[] subProtocols) {
		return ClientEndpointConfig.Builder.create()
				.configurator(configurator)
				.preferredSubprotocols(Arrays.asList(subProtocols))
				.build();
	}

	private StandardWebSocketHandlerAdapter createEndpoint(URI url, WebSocketHandler handler,
			MonoProcessor<Void> completion, DefaultConfigurator configurator) {

		return new StandardWebSocketHandlerAdapter(handler,
				session -> createSession(url, configurator.getResponseHeaders(), completion, session));
	}

	private StandardWebSocketSession createSession(URI url, HttpHeaders responseHeaders,
			MonoProcessor<Void> completion, Session session) {

		HandshakeInfo info = afterHandshake(url, responseHeaders);
		return new StandardWebSocketSession(session, info, this.bufferFactory, completion);
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
			response.getHeaders().forEach(this.responseHeaders::put);
		}
	}

}