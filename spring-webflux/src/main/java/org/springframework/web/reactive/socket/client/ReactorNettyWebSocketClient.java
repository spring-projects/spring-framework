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

package org.springframework.web.reactive.socket.client;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.websocket.WebsocketInbound;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.adapter.NettyWebSocketSessionSupport;
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

	private int maxFramePayloadLength = NettyWebSocketSessionSupport.DEFAULT_FRAME_MAX_SIZE;

	private boolean handlePing;


	/**
	 * Default constructor.
	 */
	public ReactorNettyWebSocketClient() {
		this(HttpClient.create());
	}

	/**
	 * Constructor that accepts an existing {@link HttpClient} builder.
	 * @since 5.1
	 */
	public ReactorNettyWebSocketClient(HttpClient httpClient) {
		Assert.notNull(httpClient, "HttpClient is required");
		this.httpClient = httpClient;
	}


	/**
	 * Return the configured {@link HttpClient}.
	 */
	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	/**
	 * Configure the maximum allowable frame payload length. Setting this value
	 * to your application's requirement may reduce denial of service attacks
	 * using long data frames.
	 * <p>Corresponds to the argument with the same name in the constructor of
	 * {@link io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
	 * WebSocketServerHandshakerFactory} in Netty.
	 * <p>By default set to 65536 (64K).
	 * @param maxFramePayloadLength the max length for frames.
	 * @since 5.2
	 */
	public void setMaxFramePayloadLength(int maxFramePayloadLength) {
		this.maxFramePayloadLength = maxFramePayloadLength;
	}

	/**
	 * Return the configured {@link #setMaxFramePayloadLength(int) maxFramePayloadLength}.
	 * @since 5.2
	 */
	public int getMaxFramePayloadLength() {
		return this.maxFramePayloadLength;
	}

	/**
	 * Configure whether to let ping frames through to be handled by the
	 * {@link WebSocketHandler} given to the execute method. By default, Reactor
	 * Netty automatically replies with pong frames in response to pings. This is
	 * useful in a proxy for allowing ping and pong frames through.
	 * <p>By default this is set to {@code false} in which case ping frames are
	 * handled automatically by Reactor Netty. If set to {@code true}, ping
	 * frames will be passed through to the {@link WebSocketHandler}.
	 * @param handlePing whether to let Ping frames through for handling
	 * @since 5.2.4
	 */
	public void setHandlePing(boolean handlePing) {
		this.handlePing = handlePing;
	}

	/**
	 * Return the configured {@link #setHandlePing(boolean)}.
	 * @since 5.2.4
	 */
	public boolean getHandlePing() {
		return this.handlePing;
	}

	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders requestHeaders, WebSocketHandler handler) {
		String protocols = StringUtils.collectionToCommaDelimitedString(handler.getSubProtocols());
		return getHttpClient()
				.headers(nettyHeaders -> setNettyHeaders(requestHeaders, nettyHeaders))
				.websocket(protocols, getMaxFramePayloadLength(), this.handlePing)
				.uri(url.toString())
				.handle((inbound, outbound) -> {
					HttpHeaders responseHeaders = toHttpHeaders(inbound);
					String protocol = responseHeaders.getFirst("Sec-WebSocket-Protocol");
					HandshakeInfo info = new HandshakeInfo(url, responseHeaders, Mono.empty(), protocol);
					NettyDataBufferFactory factory = new NettyDataBufferFactory(outbound.alloc());
					WebSocketSession session = new ReactorNettyWebSocketSession(
							inbound, outbound, info, factory, getMaxFramePayloadLength());
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
