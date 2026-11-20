/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.socket.client;

import java.net.URI;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;
import reactor.netty.http.websocket.WebsocketInbound;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;

/**
 * {@link WebSocketClient} implementation for use with Reactor Netty.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyWebSocketClient implements WebSocketClient {

	private static final Log logger = LogFactory.getLog(ReactorNettyWebSocketClient.class);


	private final HttpClient httpClient;

	private final Supplier<WebsocketClientSpec.Builder> specBuilderSupplier;


	/**
	 * Default constructor.
	 */
	public ReactorNettyWebSocketClient() {
		this(HttpClient.create());
	}

	/**
	 * Constructor that accepts an existing {@link HttpClient} builder
	 * with a default {@link reactor.netty.http.client.WebsocketClientSpec.Builder}.
	 * @since 5.1
	 */
	public ReactorNettyWebSocketClient(HttpClient httpClient) {
		this(httpClient, WebsocketClientSpec.builder());
	}

	/**
	 * Constructor that accepts an existing {@link HttpClient} builder
	 * and a pre-configured {@link reactor.netty.http.client.WebsocketClientSpec.Builder}.
	 * @since 5.3
	 */
	public ReactorNettyWebSocketClient(
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
	 * @since 5.3
	 */
	public WebsocketClientSpec getWebsocketClientSpec() {
		return buildWebSocketClientSpec(null);
	}

	private WebsocketClientSpec buildWebSocketClientSpec(@Nullable String protocols) {
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
		WebsocketClientSpec clientSpec = buildWebSocketClientSpec(protocols);
		return getHttpClient()
				.headers(nettyHeaders -> setNettyHeaders(requestHeaders, nettyHeaders))
				.websocket(clientSpec)
				.uri(url.toString())
				.handle((inbound, outbound) -> {
					HttpHeaders responseHeaders = toHttpHeaders(inbound);
					String protocol = responseHeaders.getFirst("Sec-WebSocket-Protocol");
					HandshakeInfo info = new HandshakeInfo(url, responseHeaders, Mono.empty(), protocol);
					NettyDataBufferFactory factory = new NettyDataBufferFactory(outbound.alloc());
					WebSocketSession session = new ReactorNettyWebSocketSession(
							inbound, outbound, info, factory, clientSpec.maxFramePayloadLength());
					if (logger.isDebugEnabled()) {
						logger.debug("Started session '" + session.getId() + "' for " + url);
					}
					return handler.handle(session).checkpoint(url + " [ReactorNettyWebSocketClient]");
				})
				.doOnRequest(n -> {
					if (logger.isDebugEnabled()) {
						logger.debug("Connecting to " + url);
					}
				})
				.next();
	}

	private void setNettyHeaders(HttpHeaders httpHeaders, io.netty.handler.codec.http.HttpHeaders nettyHeaders) {
		httpHeaders.forEach(nettyHeaders::set);
	}

	private HttpHeaders toHttpHeaders(WebsocketInbound inbound) {
		HttpHeaders headers = new HttpHeaders();
		io.netty.handler.codec.http.HttpHeaders nettyHeaders = inbound.headers();
		nettyHeaders.forEach(entry -> {
			String name = entry.getKey();
			headers.put(name, nettyHeaders.getAll(name));
		});
		return headers;
	}

}
