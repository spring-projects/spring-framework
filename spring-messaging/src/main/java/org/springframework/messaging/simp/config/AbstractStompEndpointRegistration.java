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

package org.springframework.messaging.simp.config;

import java.util.Set;

import org.springframework.messaging.handler.websocket.SubProtocolWebSocketHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.server.DefaultHandshakeHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.config.SockJsServiceRegistration;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;


/**
 * A helper class for configuring STOMP protocol handling over WebSocket
 * with optional SockJS fallback options.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractStompEndpointRegistration<M> implements StompEndpointRegistration {

	private final String[] paths;

	private final SubProtocolWebSocketHandler wsHandler;

	private HandshakeHandler handshakeHandler;

	private StompSockJsServiceRegistration sockJsServiceRegistration;

	private final TaskScheduler defaultSockJsTaskScheduler;


	public AbstractStompEndpointRegistration(String[] paths, SubProtocolWebSocketHandler webSocketHandler,
			TaskScheduler defaultSockJsTaskScheduler) {

		Assert.notEmpty(paths, "No paths specified");
		this.paths = paths;
		this.wsHandler = webSocketHandler;
		this.defaultSockJsTaskScheduler = defaultSockJsTaskScheduler;
	}


	protected SubProtocolWebSocketHandler getWsHandler() {
		return this.wsHandler;
	}

	@Override
	public StompEndpointRegistration setHandshakeHandler(HandshakeHandler handshakeHandler) {
		this.handshakeHandler = handshakeHandler;
		return this;
	}

	@Override
	public SockJsServiceRegistration withSockJS() {

		this.sockJsServiceRegistration = new StompSockJsServiceRegistration(this.defaultSockJsTaskScheduler);

		if (this.handshakeHandler != null) {
			WebSocketTransportHandler transportHandler = new WebSocketTransportHandler(this.handshakeHandler);
			this.sockJsServiceRegistration.setTransportHandlerOverrides(transportHandler);
		}

		return this.sockJsServiceRegistration;
	}

	protected M getMappings() {

		M mappings = createMappings();

		if (this.sockJsServiceRegistration != null) {
			SockJsService sockJsService = this.sockJsServiceRegistration.getSockJsService();
			for (String path : this.paths) {
				String pathPattern = path.endsWith("/") ? path + "**" : path + "/**";
				addSockJsServiceMapping(mappings, sockJsService, this.wsHandler, pathPattern);
			}
		}
		else {
			HandshakeHandler handshakeHandler = getOrCreateHandshakeHandler();
			for (String path : this.paths) {
				addWebSocketHandlerMapping(mappings, this.wsHandler, handshakeHandler, path);
			}
		}

		return mappings;
	}

	protected abstract M createMappings();

	private HandshakeHandler getOrCreateHandshakeHandler() {

		HandshakeHandler handler = (this.handshakeHandler != null)
				? this.handshakeHandler : new DefaultHandshakeHandler();

		if (handler instanceof DefaultHandshakeHandler) {
			DefaultHandshakeHandler defaultHandshakeHandler = (DefaultHandshakeHandler) handler;
			if (ObjectUtils.isEmpty(defaultHandshakeHandler.getSupportedProtocols())) {
				Set<String> protocols = this.wsHandler.getSupportedProtocols();
				defaultHandshakeHandler.setSupportedProtocols(protocols.toArray(new String[protocols.size()]));
			}
		}

		return handler;
	}

	protected abstract void addSockJsServiceMapping(M mappings, SockJsService sockJsService,
			SubProtocolWebSocketHandler wsHandler, String pathPattern);

	protected abstract void addWebSocketHandlerMapping(M mappings,
			SubProtocolWebSocketHandler wsHandler, HandshakeHandler handshakeHandler, String path);



	private class StompSockJsServiceRegistration extends SockJsServiceRegistration {

		public StompSockJsServiceRegistration(TaskScheduler defaultTaskScheduler) {
			super(defaultTaskScheduler);
		}

		protected SockJsService getSockJsService() {
			return super.getSockJsService(paths);
		}
	}

}
