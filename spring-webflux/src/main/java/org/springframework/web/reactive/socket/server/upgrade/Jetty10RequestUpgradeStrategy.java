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

import java.lang.reflect.Method;
import java.util.function.Supplier;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import reactor.core.publisher.Mono;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.Jetty10WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketSession;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link RequestUpgradeStrategy} for use with Jetty 10.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.4
 */
public class Jetty10RequestUpgradeStrategy implements RequestUpgradeStrategy {

	private static final Class<?> webSocketCreatorClass;

	private static final Method getContainerMethod;

	private static final Method upgradeMethod;

	private static final Method setAcceptedSubProtocol;

	static {
		ClassLoader loader = Jetty10RequestUpgradeStrategy.class.getClassLoader();
		try {
			webSocketCreatorClass = loader.loadClass("org.eclipse.jetty.websocket.server.JettyWebSocketCreator");

			Class<?> type = loader.loadClass("org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer");
			getContainerMethod = type.getMethod("getContainer", ServletContext.class);
			Method upgrade = ReflectionUtils.findMethod(type, "upgrade", (Class<?>[]) null);
			Assert.state(upgrade != null, "Upgrade method not found");
			upgradeMethod = upgrade;

			type = loader.loadClass("org.eclipse.jetty.websocket.server.JettyServerUpgradeResponse");
			setAcceptedSubProtocol = type.getMethod("setAcceptedSubProtocol", String.class);
		}
		catch (Exception ex) {
			throw new IllegalStateException("No compatible Jetty version found", ex);
		}
	}


	@Override
	public Mono<Void> upgrade(
			ServerWebExchange exchange, WebSocketHandler handler,
			@Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();

		HttpServletRequest servletRequest = ServerHttpRequestDecorator.getNativeRequest(request);
		HttpServletResponse servletResponse = ServerHttpResponseDecorator.getNativeResponse(response);
		ServletContext servletContext = servletRequest.getServletContext();

		HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
		DataBufferFactory factory = response.bufferFactory();

		// Trigger WebFlux preCommit actions and upgrade
		return exchange.getResponse().setComplete()
				.then(Mono.deferContextual(contextView -> {
					Jetty10WebSocketHandlerAdapter adapter = new Jetty10WebSocketHandlerAdapter(
							ContextWebSocketHandler.decorate(handler, contextView),
							session -> new JettyWebSocketSession(session, handshakeInfo, factory));

					try {
						Object creator = createJettyWebSocketCreator(adapter, subProtocol);
						Object container = ReflectionUtils.invokeMethod(getContainerMethod, null, servletContext);
						ReflectionUtils.invokeMethod(upgradeMethod, container, creator, servletRequest, servletResponse);
					}
					catch (Exception ex) {
						return Mono.error(ex);
					}
					return Mono.empty();
				}));
	}

	private static Object createJettyWebSocketCreator(
			Jetty10WebSocketHandlerAdapter adapter, @Nullable String protocol) {

		ProxyFactory factory = new ProxyFactory(EmptyTargetSource.INSTANCE);
		factory.addInterface(webSocketCreatorClass);
		factory.addAdvice(new WebSocketCreatorInterceptor(adapter, protocol));
		return factory.getProxy();
	}


	/**
	 * Proxy for a JettyWebSocketCreator to supply the WebSocket handler and set the sub-protocol.
	 */
	private static class WebSocketCreatorInterceptor implements MethodInterceptor {

		private final Jetty10WebSocketHandlerAdapter adapter;

		@Nullable
		private final String protocol;


		public WebSocketCreatorInterceptor(
				Jetty10WebSocketHandlerAdapter adapter, @Nullable String protocol) {

			this.adapter = adapter;
			this.protocol = protocol;
		}

		@Nullable
		@Override
		public Object invoke(@NonNull MethodInvocation invocation) {
			if (this.protocol != null) {
				ReflectionUtils.invokeMethod(
						setAcceptedSubProtocol, invocation.getArguments()[2], this.protocol);
			}
			return this.adapter;
		}
	}

}
