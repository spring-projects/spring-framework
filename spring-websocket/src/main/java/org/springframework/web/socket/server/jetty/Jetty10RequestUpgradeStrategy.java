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

package org.springframework.web.socket.server.jetty;

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.adapter.jetty.Jetty10WebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketSession;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.RequestUpgradeStrategy;

/**
 * A {@link RequestUpgradeStrategy} for Jetty 10.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.4
 */
public class Jetty10RequestUpgradeStrategy implements RequestUpgradeStrategy {

	private static final String[] SUPPORTED_VERSIONS = new String[] { String.valueOf(13) };

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
	public String[] getSupportedVersions() {
		return SUPPORTED_VERSIONS;
	}

	@Override
	public List<WebSocketExtension> getSupportedExtensions(ServerHttpRequest request) {
		return Collections.emptyList();
	}


	@Override
	public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			@Nullable String selectedProtocol, List<WebSocketExtension> selectedExtensions,
			@Nullable Principal user, WebSocketHandler handler, Map<String, Object> attributes)
			throws HandshakeFailureException {

		Assert.isInstanceOf(ServletServerHttpRequest.class, request, "ServletServerHttpRequest required");
		HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
		ServletContext servletContext = servletRequest.getServletContext();

		Assert.isInstanceOf(ServletServerHttpResponse.class, response, "ServletServerHttpResponse required");
		HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();

		JettyWebSocketSession session = new JettyWebSocketSession(attributes, user);
		Jetty10WebSocketHandlerAdapter handlerAdapter = new Jetty10WebSocketHandlerAdapter(handler, session);

		try {
			Object creator = createJettyWebSocketCreator(handlerAdapter, selectedProtocol);
			Object container = ReflectionUtils.invokeMethod(getContainerMethod, null, servletContext);
			ReflectionUtils.invokeMethod(upgradeMethod, container, creator, servletRequest, servletResponse);
		}
		catch (UndeclaredThrowableException ex) {
			throw new HandshakeFailureException("Failed to upgrade", ex.getUndeclaredThrowable());
		}
		catch (Exception ex) {
			throw new HandshakeFailureException("Failed to upgrade", ex);
		}
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
