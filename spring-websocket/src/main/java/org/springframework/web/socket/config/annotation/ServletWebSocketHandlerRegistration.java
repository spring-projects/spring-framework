/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.socket.config.annotation;

import java.util.Arrays;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;

/**
 * A helper class for configuring {@link WebSocketHandler} request handling
 * including SockJS fallback options.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ServletWebSocketHandlerRegistration
		extends AbstractWebSocketHandlerRegistration<MultiValueMap<HttpRequestHandler, String>> {

	public ServletWebSocketHandlerRegistration(TaskScheduler sockJsTaskScheduler) {
		super(sockJsTaskScheduler);
	}


	@Override
	protected MultiValueMap<HttpRequestHandler, String> createMappings() {
		return new LinkedMultiValueMap<HttpRequestHandler, String>();
	}

	@Override
	protected void addSockJsServiceMapping(MultiValueMap<HttpRequestHandler, String> mappings,
			SockJsService sockJsService, WebSocketHandler handler, String pathPattern) {

		SockJsHttpRequestHandler httpHandler = new SockJsHttpRequestHandler(sockJsService, handler);
		mappings.add(httpHandler, pathPattern);
	}

	@Override
	protected void addWebSocketHandlerMapping(MultiValueMap<HttpRequestHandler, String> mappings,
			WebSocketHandler wsHandler, HandshakeHandler handshakeHandler,
			HandshakeInterceptor[] interceptors, String path) {

		WebSocketHttpRequestHandler httpHandler = new WebSocketHttpRequestHandler(wsHandler, handshakeHandler);
		if (!ObjectUtils.isEmpty(interceptors)) {
			httpHandler.setHandshakeInterceptors(Arrays.asList(interceptors));
		}
		mappings.add(httpHandler, path);
	}

}
