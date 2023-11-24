/*
 * Copyright 2002-2023 the original author or authors.
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
import reactor.netty5.http.server.HttpServerResponse;
import reactor.netty5.http.server.WebsocketServerSpec;

import org.springframework.core.io.buffer.Netty5DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ReactorNetty2WebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for Reactor Netty for Netty 5.
 *
 * <p>This class is based on {@link ReactorNettyRequestUpgradeStrategy}.
 *
 * @author Violeta Georgieva
 * @since 6.0
 */
public class ReactorNetty2RequestUpgradeStrategy implements RequestUpgradeStrategy {

	private final Supplier<WebsocketServerSpec.Builder> specBuilderSupplier;


	/**
	 * Create an instances with a default {@link WebsocketServerSpec.Builder}.
	 * @since 5.2.6
	 */
	public ReactorNetty2RequestUpgradeStrategy() {
		this(WebsocketServerSpec::builder);
	}


	/**
	 * Create an instance with a pre-configured {@link WebsocketServerSpec.Builder}
	 * to use for WebSocket upgrades.
	 * @since 5.2.6
	 */
	public ReactorNetty2RequestUpgradeStrategy(Supplier<WebsocketServerSpec.Builder> builderSupplier) {
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
		return builder.build();
	}


	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler,
			@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		ServerHttpResponse response = exchange.getResponse();
		HttpServerResponse reactorResponse = ServerHttpResponseDecorator.getNativeResponse(response);
		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		Netty5DataBufferFactory bufferFactory = (Netty5DataBufferFactory) response.bufferFactory();
		URI uri = exchange.getRequest().getURI();

		// Trigger WebFlux preCommit actions and upgrade
		return response.setComplete()
				.then(Mono.defer(() -> {
					WebsocketServerSpec spec = buildSpec(subProtocol);
					return reactorResponse.sendWebsocket((in, out) -> {
						ReactorNetty2WebSocketSession session =
								new ReactorNetty2WebSocketSession(
										in, out, handshakeInfo, bufferFactory, spec.maxFramePayloadLength());
						return handler.handle(session).checkpoint(uri + " [ReactorNetty2RequestUpgradeStrategy]");
					}, spec);
				}));
	}

}
