/*
 * Copyright 2002-2022 the original author or authors.
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
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;
import reactor.netty5.http.client.HttpClient;
import reactor.netty5.http.client.WebsocketClientSpec;
import reactor.netty5.http.websocket.WebsocketInbound;

import org.springframework.core.io.buffer.Netty5DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.adapter.ReactorNetty2WebSocketSession;

/**
 * {@link WebSocketClient} implementation for use with Reactor Netty for Netty 5.
 *
 * <p>This class is based on {@link ReactorNettyWebSocketClient}.
 *
 * @author Violeta Georgieva
 * @since 6.0
 */
public class ReactorNetty2WebSocketClient implements WebSocketClient {

	private static final Log logger = LogFactory.getLog(ReactorNetty2WebSocketClient.class);


	private final HttpClient httpClient;

	private final Supplier<WebsocketClientSpec.Builder> specBuilderSupplier;

	@Nullable
	private Boolean handlePing;


	/**
	 * Default constructor.
	 */
	public ReactorNetty2WebSocketClient() {
		this(HttpClient.create());
	}

	/**
	 * Constructor that accepts an existing {@link HttpClient} builder
	 * with a default {@link WebsocketClientSpec.Builder}.
	 * @since 5.1
	 */
	public ReactorNetty2WebSocketClient(HttpClient httpClient) {
		this(httpClient, WebsocketClientSpec.builder());
	}

	/**
	 * Constructor that accepts an existing {@link HttpClient} builder
	 * and a pre-configured {@link WebsocketClientSpec.Builder}.
	 */
	public ReactorNetty2WebSocketClient(
			HttpClient httpClient, Supplier<WebsocketClientSpec.Builder> builderSupplier) {

		Assert.notNull(httpClient, "HttpClient is required");
		Assert.notNull(builderSupplier, "WebsocketClientSpec.Builder is required");
		this.httpClient = httpClient;
		this.specBuilderSupplier = builderSupplier;
	}


	/**
	 * Return the configured {@link HttpClient}.
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * Build an instance of {@code WebsocketClientSpec} that reflects the current
	 * configuration. This can be used to check the configured parameters except
	 * for sub-protocols which depend on the {@link WebSocketHandler} that is used
	 * for a given upgrade.
	 */
	public WebsocketClientSpec getWebsocketClientSpec() {
		return buildSpec(null);
	}

	private WebsocketClientSpec buildSpec(@Nullable String protocols) {
		WebsocketClientSpec.Builder builder = this.specBuilderSupplier.get();
		if (StringUtils.hasText(protocols)) {
			builder.protocols(protocols);
		}
		return builder.build();
	}


	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders requestHeaders, WebSocketHandler handler) {
		String protocols = StringUtils.collectionToCommaDelimitedString(handler.getSubProtocols());
		WebsocketClientSpec clientSpec = buildSpec(protocols);
		return getHttpClient()
				.headers(nettyHeaders -> setNettyHeaders(requestHeaders, nettyHeaders))
				.websocket(clientSpec)
				.uri(url.toString())
				.handle((inbound, outbound) -> {
					HttpHeaders responseHeaders = toHttpHeaders(inbound);
					String protocol = responseHeaders.getFirst("Sec-WebSocket-Protocol");
					HandshakeInfo info = new HandshakeInfo(url, responseHeaders, Mono.empty(), protocol);
					Netty5DataBufferFactory factory = new Netty5DataBufferFactory(outbound.alloc());
					WebSocketSession session = new ReactorNetty2WebSocketSession(
							inbound, outbound, info, factory, clientSpec.maxFramePayloadLength());
					if (logger.isDebugEnabled()) {
						logger.debug("Started session '" + session.getId() + "' for " + url);
					}
					return handler.handle(session).checkpoint(url + " [ReactorNetty2WebSocketClient]");
				})
				.doOnRequest(n -> {
					if (logger.isDebugEnabled()) {
						logger.debug("Connecting to " + url);
					}
				})
				.next();
	}

	private void setNettyHeaders(HttpHeaders httpHeaders, io.netty5.handler.codec.http.headers.HttpHeaders nettyHeaders) {
		httpHeaders.forEach(nettyHeaders::set);
	}

	private HttpHeaders toHttpHeaders(WebsocketInbound inbound) {
		HttpHeaders headers = new HttpHeaders();
		inbound.headers().iterator().forEachRemaining(entry ->
				headers.add(entry.getKey().toString(), entry.getValue().toString()));
		return headers;
	}

}
