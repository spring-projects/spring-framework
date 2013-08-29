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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.messaging.handler.websocket.SubProtocolWebSocketHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.socket.server.DefaultHandshakeHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.config.SockJsServiceRegistration;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsService;


/**
 * A helper class for configuring STOMP protocol handling over WebSocket
 * with optional SockJS fallback options.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompEndpointRegistration {

	private final List<String> paths;

	private final SubProtocolWebSocketHandler wsHandler;

	private HandshakeHandler handshakeHandler;

	private StompSockJsServiceRegistration sockJsServiceRegistration;

	private final TaskScheduler defaultSockJsTaskScheduler;


	public StompEndpointRegistration(Collection<String> paths, SubProtocolWebSocketHandler webSocketHandler,
			TaskScheduler defaultSockJsTaskScheduler) {

		this.paths = new ArrayList<String>(paths);
		this.wsHandler = webSocketHandler;
		this.defaultSockJsTaskScheduler = defaultSockJsTaskScheduler;
	}


	public StompEndpointRegistration setHandshakeHandler(HandshakeHandler handshakeHandler) {
		this.handshakeHandler = handshakeHandler;
		return this;
	}

	public SockJsServiceRegistration withSockJS() {
		this.sockJsServiceRegistration = new StompSockJsServiceRegistration(this.defaultSockJsTaskScheduler);
		return this.sockJsServiceRegistration;
	}

	protected MultiValueMap<HttpRequestHandler, String> getMappings() {
		MultiValueMap<HttpRequestHandler, String> mappings = new LinkedMultiValueMap<HttpRequestHandler, String>();
		if (this.sockJsServiceRegistration == null) {
			HandshakeHandler handshakeHandler = getOrCreateHandshakeHandler();
			for (String path : this.paths) {
				WebSocketHttpRequestHandler handler = new WebSocketHttpRequestHandler(this.wsHandler, handshakeHandler);
				mappings.add(handler, path);
			}
		}
		else {
			SockJsService sockJsService = this.sockJsServiceRegistration.getSockJsService();
			for (String path : this.paths) {
				SockJsHttpRequestHandler httpHandler = new SockJsHttpRequestHandler(sockJsService, this.wsHandler);
				mappings.add(httpHandler, path.endsWith("/") ? path + "**" : path + "/**");
			}
		}
		return mappings;
	}

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


	private class StompSockJsServiceRegistration extends SockJsServiceRegistration {


		public StompSockJsServiceRegistration(TaskScheduler defaultTaskScheduler) {
			super(defaultTaskScheduler);
		}

		protected SockJsService getSockJsService() {
			return super.getSockJsService(paths.toArray(new String[paths.size()]));
		}
	}

}
