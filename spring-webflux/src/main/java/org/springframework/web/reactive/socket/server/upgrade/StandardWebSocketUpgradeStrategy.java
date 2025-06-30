/*
 * Copyright 2002-present the original author or authors.
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
import java.util.Map;
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.Assert;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.TomcatWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for the Jakarta WebSocket API 2.1+.
 *
 * <p>This strategy serves as a fallback if no specific server has been detected.
 * It can also be used with Jakarta EE 10 level servers such as Tomcat 10.1 and
 * Undertow 2.3 directly, relying on their built-in Jakarta WebSocket 2.1 support.
 *
 * @author Juergen Hoeller
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 6.0
 * @see jakarta.websocket.server.ServerContainer#upgradeHttpToWebSocket
 */
public class StandardWebSocketUpgradeStrategy implements RequestUpgradeStrategy {

	private static final String SERVER_CONTAINER_ATTR = "jakarta.websocket.server.ServerContainer";


	private @Nullable Long asyncSendTimeout;

	private @Nullable Long maxSessionIdleTimeout;

	private @Nullable Integer maxTextMessageBufferSize;

	private @Nullable Integer maxBinaryMessageBufferSize;

	private @Nullable ServerContainer serverContainer;


	/**
	 * Exposes the underlying config option on
	 * {@link ServerContainer#setAsyncSendTimeout(long)}.
	 */
	public void setAsyncSendTimeout(Long timeoutInMillis) {
		this.asyncSendTimeout = timeoutInMillis;
	}

	public @Nullable Long getAsyncSendTimeout() {
		return this.asyncSendTimeout;
	}

	/**
	 * Exposes the underlying config option on
	 * {@link ServerContainer#setDefaultMaxSessionIdleTimeout(long)}.
	 */
	public void setMaxSessionIdleTimeout(Long timeoutInMillis) {
		this.maxSessionIdleTimeout = timeoutInMillis;
	}

	public @Nullable Long getMaxSessionIdleTimeout() {
		return this.maxSessionIdleTimeout;
	}

	/**
	 * Exposes the underlying config option on
	 * {@link ServerContainer#setDefaultMaxTextMessageBufferSize(int)}.
	 */
	public void setMaxTextMessageBufferSize(Integer bufferSize) {
		this.maxTextMessageBufferSize = bufferSize;
	}

	public @Nullable Integer getMaxTextMessageBufferSize() {
		return this.maxTextMessageBufferSize;
	}

	/**
	 * Exposes the underlying config option on
	 * {@link ServerContainer#setDefaultMaxBinaryMessageBufferSize(int)}.
	 */
	public void setMaxBinaryMessageBufferSize(Integer bufferSize) {
		this.maxBinaryMessageBufferSize = bufferSize;
	}

	public @Nullable Integer getMaxBinaryMessageBufferSize() {
		return this.maxBinaryMessageBufferSize;
	}

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

					try {
						upgradeHttpToWebSocket(servletRequest, servletResponse, config, Collections.emptyMap());
					}
					catch (Exception ex) {
						return Mono.error(ex);
					}
					return Mono.empty();
				}));
	}


	protected void upgradeHttpToWebSocket(HttpServletRequest request, HttpServletResponse response,
			ServerEndpointConfig endpointConfig, Map<String,String> pathParams) throws Exception {

		getContainer(request).upgradeHttpToWebSocket(request, response, endpointConfig, pathParams);
	}

	protected ServerContainer getContainer(HttpServletRequest request) {
		if (this.serverContainer == null) {
			Object container = request.getServletContext().getAttribute(SERVER_CONTAINER_ATTR);
			Assert.state(container instanceof ServerContainer,
					"ServletContext attribute 'jakarta.websocket.server.ServerContainer' not found.");
			this.serverContainer = (ServerContainer) container;
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
