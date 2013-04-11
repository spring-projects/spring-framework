/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.websocket.server.endpoint.handshake;

import javax.websocket.Endpoint;

import org.springframework.beans.BeanUtils;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ClassUtils;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.endpoint.WebSocketHandlerEndpoint;
import org.springframework.websocket.server.AbstractHandshakeHandler;
import org.springframework.websocket.server.HandshakeHandler;


/**
 * A {@link HandshakeHandler} for use with standard Java WebSocket runtimes. A
 * container-specific {@link RequestUpgradeStrategy} is required since standard
 * Java WebSocket currently does not provide any means of integrating a WebSocket
 * handshake into an HTTP request processing pipeline. Currently available are
 * implementations for Tomcat and Glassfish.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class EndpointHandshakeHandler extends AbstractHandshakeHandler {

	private final RequestUpgradeStrategy upgradeStrategy;


	public EndpointHandshakeHandler(Endpoint endpoint) {
		super(endpoint);
		this.upgradeStrategy = createRequestUpgradeStrategy();
	}

	public EndpointHandshakeHandler(WebSocketHandler webSocketHandler) {
		super(webSocketHandler);
		this.upgradeStrategy = createRequestUpgradeStrategy();
	}

	public EndpointHandshakeHandler(Class<?> handlerClass) {
		super(handlerClass);
		this.upgradeStrategy = createRequestUpgradeStrategy();
	}

	protected RequestUpgradeStrategy createRequestUpgradeStrategy() {
		return new RequestUpgradeStrategyFactory().create();
	}

	@Override
	protected String[] getSupportedVerions() {
		return this.upgradeStrategy.getSupportedVersions();
	}

	@Override
	public void doHandshakeInternal(ServerHttpRequest request, ServerHttpResponse response, String protocol)
			throws Exception {

		logger.debug("Upgrading HTTP request");

		Object webSocketHandler = getWebSocketHandler();

		Endpoint endpoint;
		if (webSocketHandler instanceof Endpoint) {
			endpoint = (Endpoint) webSocketHandler;
		}
		else if (webSocketHandler instanceof WebSocketHandler) {
			endpoint = new WebSocketHandlerEndpoint((WebSocketHandler) webSocketHandler);
		}
		else {
			String className = webSocketHandler.getClass().getName();
			throw new IllegalArgumentException("Unexpected WebSocket handler type: " + className);
		}

		this.upgradeStrategy.upgrade(request, response, protocol, endpoint);
	}


	private static class RequestUpgradeStrategyFactory {

		private static final String packageName = EndpointHandshakeHandler.class.getPackage().getName();

		private static final boolean tomcatWebSocketPresent = ClassUtils.isPresent(
				"org.apache.tomcat.websocket.server.WsHttpUpgradeHandler", EndpointHandshakeHandler.class.getClassLoader());

		private static final boolean glassfishWebSocketPresent = ClassUtils.isPresent(
				"org.glassfish.tyrus.servlet.TyrusHttpUpgradeHandler", EndpointHandshakeHandler.class.getClassLoader());


		private RequestUpgradeStrategy create() {
			String className;
			if (tomcatWebSocketPresent) {
				className = packageName + ".TomcatRequestUpgradeStrategy";
			}
			else if (glassfishWebSocketPresent) {
				className = packageName + ".GlassfishRequestUpgradeStrategy";
			}
			else {
				throw new IllegalStateException("No suitable " + RequestUpgradeStrategy.class.getSimpleName());
			}
			try {
				Class<?> clazz = ClassUtils.forName(className, EndpointHandshakeHandler.class.getClassLoader());
				return (RequestUpgradeStrategy) BeanUtils.instantiateClass(clazz.getConstructor());
			}
			catch (Throwable t) {
				throw new IllegalStateException("Failed to instantiate " + className, t);
			}
		}
	}

}