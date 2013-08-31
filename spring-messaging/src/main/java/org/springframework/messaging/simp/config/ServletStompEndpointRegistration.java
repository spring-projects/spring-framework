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

import org.springframework.messaging.handler.websocket.SubProtocolWebSocketHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.socket.server.HandshakeHandler;
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
public class ServletStompEndpointRegistration
		extends AbstractStompEndpointRegistration<MultiValueMap<HttpRequestHandler, String>> {


	public ServletStompEndpointRegistration(String[] paths, SubProtocolWebSocketHandler wsHandler,
			TaskScheduler sockJsTaskScheduler) {

		super(paths, wsHandler, sockJsTaskScheduler);
	}

	@Override
	protected MultiValueMap<HttpRequestHandler, String> createMappings() {
		return new LinkedMultiValueMap<HttpRequestHandler, String>();
	}

	@Override
	protected void addSockJsServiceMapping(MultiValueMap<HttpRequestHandler, String> mappings,
			SockJsService sockJsService, SubProtocolWebSocketHandler wsHandler, String pathPattern) {

		SockJsHttpRequestHandler httpHandler = new SockJsHttpRequestHandler(sockJsService, wsHandler);
		mappings.add(httpHandler, pathPattern);
	}

	@Override
	protected void addWebSocketHandlerMapping(MultiValueMap<HttpRequestHandler, String> mappings,
			SubProtocolWebSocketHandler wsHandler, HandshakeHandler handshakeHandler, String path) {

		WebSocketHttpRequestHandler handler = new WebSocketHttpRequestHandler(wsHandler, handshakeHandler);
		mappings.add(handler, path);
	}

}
