/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.Collections;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerContainer;

import org.apache.tomcat.websocket.server.WsServerContainer;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.TomcatWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link RequestUpgradeStrategy} for use with Tomcat.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class TomcatRequestUpgradeStrategy implements RequestUpgradeStrategy {

	private static final String SERVER_CONTAINER_ATTR = "javax.websocket.server.ServerContainer";


	@Nullable
	private Long asyncSendTimeout;

	@Nullable
	private Long maxSessionIdleTimeout;

	@Nullable
	private Integer maxTextMessageBufferSize;

	@Nullable
	private Integer maxBinaryMessageBufferSize;

	@Nullable
	private WsServerContainer serverContainer;


	/**
	 * Exposes the underlying config option on
	 * {@link javax.websocket.server.ServerContainer#setAsyncSendTimeout(long)}.
	 */
	public void setAsyncSendTimeout(Long timeoutInMillis) {
		this.asyncSendTimeout = timeoutInMillis;
	}

	@Nullable
	public Long getAsyncSendTimeout() {
		return this.asyncSendTimeout;
	}

	/**
	 * Exposes the underlying config option on
	 * {@link javax.websocket.server.ServerContainer#setDefaultMaxSessionIdleTimeout(long)}.
	 */
	public void setMaxSessionIdleTimeout(Long timeoutInMillis) {
		this.maxSessionIdleTimeout = timeoutInMillis;
	}

	@Nullable
	public Long getMaxSessionIdleTimeout() {
		return this.maxSessionIdleTimeout;
	}

	/**
	 * Exposes the underlying config option on
	 * {@link javax.websocket.server.ServerContainer#setDefaultMaxTextMessageBufferSize(int)}.
	 */
	public void setMaxTextMessageBufferSize(Integer bufferSize) {
		this.maxTextMessageBufferSize = bufferSize;
	}

	@Nullable
	public Integer getMaxTextMessageBufferSize() {
		return this.maxTextMessageBufferSize;
	}

	/**
	 * Exposes the underlying config option on
	 * {@link javax.websocket.server.ServerContainer#setDefaultMaxBinaryMessageBufferSize(int)}.
	 */
	public void setMaxBinaryMessageBufferSize(Integer bufferSize) {
		this.maxBinaryMessageBufferSize = bufferSize;
	}

	@Nullable
	public Integer getMaxBinaryMessageBufferSize() {
		return this.maxBinaryMessageBufferSize;
	}


	@SuppressWarnings("deprecation")  // for old doUpgrade variant in Tomcat 9.0.55
	@Override
	public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler,
			@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory){

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		HttpServletRequest servletRequest = ServerHttpRequestDecorator.getNativeRequest(request);
		HttpServletResponse servletResponse = ServerHttpResponseDecorator.getNativeResponse(response);

		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		DataBufferFactory bufferFactory = response.bufferFactory();

		// Trigger WebFlux preCommit actions and upgrade
		return exchange.getResponse().setComplete()
				.then(Mono.deferContextual(contextView -> {
					Endpoint endpoint = new StandardWebSocketHandlerAdapter(
							ContextWebSocketHandler.decorate(handler, contextView),
							session -> new TomcatWebSocketSession(session, handshakeInfo, bufferFactory));

					String requestURI = servletRequest.getRequestURI();
					DefaultServerEndpointConfig config = new DefaultServerEndpointConfig(requestURI, endpoint);
					config.setSubprotocols(subProtocol != null ?
							Collections.singletonList(subProtocol) : Collections.emptyList());

					WsServerContainer container = getContainer(servletRequest);
					try {
						container.doUpgrade(servletRequest, servletResponse, config, Collections.emptyMap());
					}
					catch (Exception ex) {
						return Mono.error(ex);
					}
					return Mono.empty();
				}));
	}

	private WsServerContainer getContainer(HttpServletRequest request) {
		if (this.serverContainer == null) {
			Object container = request.getServletContext().getAttribute(SERVER_CONTAINER_ATTR);
			Assert.state(container instanceof WsServerContainer,
					"ServletContext attribute 'javax.websocket.server.ServerContainer' not found.");
			this.serverContainer = (WsServerContainer) container;
			initServerContainer(this.serverContainer);
		}
		return this.serverContainer;
	}

	private void initServerContainer(ServerContainer serverContainer) {
		if (this.asyncSendTimeout != null) {
			serverContainer.setAsyncSendTimeout(this.asyncSendTimeout);
		}
		if (this.maxSessionIdleTimeout != null) {
			serverContainer.setDefaultMaxSessionIdleTimeout(this.maxSessionIdleTimeout);
		}
		if (this.maxTextMessageBufferSize != null) {
			serverContainer.setDefaultMaxTextMessageBufferSize(this.maxTextMessageBufferSize);
		}
		if (this.maxBinaryMessageBufferSize != null) {
			serverContainer.setDefaultMaxBinaryMessageBufferSize(this.maxBinaryMessageBufferSize);
		}
	}

}
