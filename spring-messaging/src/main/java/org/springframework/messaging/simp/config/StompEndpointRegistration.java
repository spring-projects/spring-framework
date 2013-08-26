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

import org.springframework.messaging.handler.websocket.SubProtocolWebSocketHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

	private StompSockJsServiceRegistration sockJsServiceRegistration;

	private TaskScheduler defaultTaskScheduler;


	public StompEndpointRegistration(Collection<String> paths, SubProtocolWebSocketHandler webSocketHandler) {
		this.paths = new ArrayList<String>(paths);
		this.wsHandler = webSocketHandler;
	}


	protected List<String> getPaths() {
		return this.paths;
	}

	protected SubProtocolWebSocketHandler getSubProtocolWebSocketHandler() {
		return this.wsHandler;
	}

	protected StompSockJsServiceRegistration getSockJsServiceRegistration() {
		return this.sockJsServiceRegistration;
	}

	public SockJsServiceRegistration withSockJS() {
		this.sockJsServiceRegistration = new StompSockJsServiceRegistration(this.defaultTaskScheduler);
		return this.sockJsServiceRegistration;
	}

	protected void setDefaultTaskScheduler(TaskScheduler defaultTaskScheduler) {
		this.defaultTaskScheduler = defaultTaskScheduler;
	}

	protected TaskScheduler getDefaultTaskScheduler() {
		return this.defaultTaskScheduler;
	}

	protected MultiValueMap<HttpRequestHandler, String> getMappings() {
		MultiValueMap<HttpRequestHandler, String> mappings = new LinkedMultiValueMap<HttpRequestHandler, String>();
		if (getSockJsServiceRegistration() == null) {
			HandshakeHandler handshakeHandler = createHandshakeHandler();
			for (String path : getPaths()) {
				WebSocketHttpRequestHandler handler = new WebSocketHttpRequestHandler(this.wsHandler, handshakeHandler);
				mappings.add(handler, path);
			}
		}
		else {
			SockJsService sockJsService = getSockJsServiceRegistration().getSockJsService();
			for (String path : this.paths) {
				SockJsHttpRequestHandler httpHandler = new SockJsHttpRequestHandler(sockJsService, this.wsHandler);
				mappings.add(httpHandler, path.endsWith("/") ? path + "**" : path + "/**");
			}
		}
		return mappings;
	}

	protected DefaultHandshakeHandler createHandshakeHandler() {
		return new DefaultHandshakeHandler();
	}


	private class StompSockJsServiceRegistration extends SockJsServiceRegistration {


		public StompSockJsServiceRegistration(TaskScheduler defaultTaskScheduler) {
			super(defaultTaskScheduler);
		}

		protected SockJsService getSockJsService() {
			return super.getSockJsService(getPaths().toArray(new String[getPaths().size()]));
		}
	}

}
