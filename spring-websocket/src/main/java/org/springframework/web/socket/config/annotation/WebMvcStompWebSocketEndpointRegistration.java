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

package org.springframework.web.socket.config.annotation;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

/**
 * An abstract base class class for configuring STOMP over WebSocket/SockJS endpoints.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebMvcStompWebSocketEndpointRegistration implements StompWebSocketEndpointRegistration {

	private final String[] paths;

	private final WebSocketHandler webSocketHandler;

	private final TaskScheduler sockJsTaskScheduler;

	private HandshakeHandler handshakeHandler;

	private StompSockJsServiceRegistration registration;


	public WebMvcStompWebSocketEndpointRegistration(String[] paths, WebSocketHandler webSocketHandler,
			TaskScheduler sockJsTaskScheduler) {

		Assert.notEmpty(paths, "No paths specified");
		Assert.notNull(webSocketHandler, "WebSocketHandler must not be null");

		this.paths = paths;
		this.webSocketHandler = webSocketHandler;
		this.sockJsTaskScheduler = sockJsTaskScheduler;
	}

	/**
	 * Provide a custom or pre-configured {@link HandshakeHandler}.
	 */
	@Override
	public StompWebSocketEndpointRegistration setHandshakeHandler(HandshakeHandler handshakeHandler) {
		Assert.notNull(handshakeHandler, "'handshakeHandler' must not be null");
		this.handshakeHandler = handshakeHandler;
		return this;
	}

	/**
	 * Enable SockJS fallback options.
	 */
	@Override
	public SockJsServiceRegistration withSockJS() {
		this.registration = new StompSockJsServiceRegistration(this.sockJsTaskScheduler);
		if (this.handshakeHandler != null) {
			WebSocketTransportHandler transportHandler = new WebSocketTransportHandler(this.handshakeHandler);
			this.registration.setTransportHandlerOverrides(transportHandler);
		}
		return this.registration;
	}

	public final MultiValueMap<HttpRequestHandler, String> getMappings() {
		MultiValueMap<HttpRequestHandler, String> mappings = new LinkedMultiValueMap<HttpRequestHandler, String>();
		if (this.registration != null) {
			SockJsService sockJsService = this.registration.getSockJsService();
			for (String path : this.paths) {
				String pattern = path.endsWith("/") ? path + "**" : path + "/**";
				SockJsHttpRequestHandler handler = new SockJsHttpRequestHandler(sockJsService, this.webSocketHandler);
				mappings.add(handler, pattern);
			}
		}
		else {
			for (String path : this.paths) {
				WebSocketHttpRequestHandler handler = (this.handshakeHandler != null) ?
						new WebSocketHttpRequestHandler(this.webSocketHandler, this.handshakeHandler) :
						new WebSocketHttpRequestHandler(this.webSocketHandler);
				mappings.add(handler, path);
			}
		}
		return mappings;
	}


	private static class StompSockJsServiceRegistration extends SockJsServiceRegistration {

		public StompSockJsServiceRegistration(TaskScheduler defaultTaskScheduler) {
			super(defaultTaskScheduler);
		}

		protected SockJsService getSockJsService() {
			return super.getSockJsService();
		}
	}

}
