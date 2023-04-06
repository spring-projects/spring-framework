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

package org.springframework.web.reactive.socket.server.upgrade;

import java.net.URI;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.WebsocketServerSpec;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ReactorNettyWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for Reactor Netty.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactorNettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

	private final Supplier<WebsocketServerSpec.Builder> specBuilderSupplier;

	@Nullable
	private Integer maxFramePayloadLength;

	@Nullable
	private Boolean handlePing;


	/**
	 * Create an instances with a default {@link reactor.netty.http.server.WebsocketServerSpec.Builder}.
	 * @since 5.2.6
	 */
	public ReactorNettyRequestUpgradeStrategy() {
		this(WebsocketServerSpec::builder);
	}


	/**
	 * Create an instance with a pre-configured {@link reactor.netty.http.server.WebsocketServerSpec.Builder}
	 * to use for WebSocket upgrades.
	 * @since 5.2.6
	 */
	public ReactorNettyRequestUpgradeStrategy(Supplier<WebsocketServerSpec.Builder> builderSupplier) {
		Assert.notNull(builderSupplier, "WebsocketServerSpec.Builder is required");
		this.specBuilderSupplier = builderSupplier;
	}


	/**
	 * Build an instance of {@code WebsocketServerSpec} that reflects the current
	 * configuration. This can be used to check the configured parameters except
	 * for sub-protocols which depend on the {@link WebSocketHandler} that is used
	 * for a given upgrade.
	 * @since 5.2.6
	 */
	public WebsocketServerSpec getWebsocketServerSpec() {
		return buildSpec(null);
	}

	WebsocketServerSpec buildSpec(@Nullable String subProtocol) {
		WebsocketServerSpec.Builder builder = this.specBuilderSupplier.get();
		if (subProtocol != null) {
			builder.protocols(subProtocol);
		}
		if (this.maxFramePayloadLength != null) {
			builder.maxFramePayloadLength(this.maxFramePayloadLength);
		}
		if (this.handlePing != null) {
			builder.handlePing(this.handlePing);
		}
		return builder.build();
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
	 * @since 5.1
	 * @deprecated as of 5.2.6 in favor of providing a supplier of
	 * {@link reactor.netty.http.server.WebsocketServerSpec.Builder} with a
	 * constructor argument
	 */
	@Deprecated
	public void setMaxFramePayloadLength(Integer maxFramePayloadLength) {
		this.maxFramePayloadLength = maxFramePayloadLength;
	}

	/**
	 * Return the configured max length for frames.
	 * @since 5.1
	 * @deprecated as of 5.2.6 in favor of {@link #getWebsocketServerSpec()}
	 */
	@Deprecated
	public int getMaxFramePayloadLength() {
		return getWebsocketServerSpec().maxFramePayloadLength();
	}

	/**
	 * Configure whether to let ping frames through to be handled by the
	 * {@link WebSocketHandler} given to the upgrade method. By default, Reactor
	 * Netty automatically replies with pong frames in response to pings. This is
	 * useful in a proxy for allowing ping and pong frames through.
	 * <p>By default this is set to {@code false} in which case ping frames are
	 * handled automatically by Reactor Netty. If set to {@code true}, ping
	 * frames will be passed through to the {@link WebSocketHandler}.
	 * @param handlePing whether to let Ping frames through for handling
	 * @since 5.2.4
	 * @deprecated as of 5.2.6 in favor of providing a supplier of
	 * {@link reactor.netty.http.server.WebsocketServerSpec.Builder} with a
	 * constructor argument
	 */
	@Deprecated
	public void setHandlePing(boolean handlePing) {
		this.handlePing = handlePing;
	}

	/**
	 * Return the configured {@link #setHandlePing(boolean)}.
	 * @since 5.2.4
	 * @deprecated as of 5.2.6 in favor of {@link #getWebsocketServerSpec()}
	 */
	@Deprecated
	public boolean getHandlePing() {
		return getWebsocketServerSpec().handlePing();
	}


	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler,
			@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		ServerHttpResponse response = exchange.getResponse();
		HttpServerResponse reactorResponse = ServerHttpResponseDecorator.getNativeResponse(response);
		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		NettyDataBufferFactory bufferFactory = (NettyDataBufferFactory) response.bufferFactory();
		URI uri = exchange.getRequest().getURI();

		// Trigger WebFlux preCommit actions and upgrade
		return response.setComplete()
				.then(Mono.defer(() -> {
					WebsocketServerSpec spec = buildSpec(subProtocol);
					return reactorResponse.sendWebsocket((in, out) -> {
						ReactorNettyWebSocketSession session =
								new ReactorNettyWebSocketSession(
										in, out, handshakeInfo, bufferFactory, spec.maxFramePayloadLength());
						return handler.handle(session).checkpoint(uri + " [ReactorNettyRequestUpgradeStrategy]");
					}, spec);
				}));
	}

}
