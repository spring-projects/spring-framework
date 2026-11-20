/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.socket.server.upgrade;

import java.net.URI;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.WebsocketServerSpec;

import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
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

	private @Nullable Integer maxFramePayloadLength;

	private @Nullable Boolean handlePing;


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
