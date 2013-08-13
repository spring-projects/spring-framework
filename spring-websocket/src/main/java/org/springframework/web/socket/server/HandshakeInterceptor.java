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

package org.springframework.web.socket.server;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;


/**
 * Interceptor for WebSocket handshake requests. Can be used to inspect the handshake
 * request and response as well as to pass attributes to the target
 * {@link WebSocketHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 * @see org.springframework.web.socket.server.support.WebSocketHttpRequestHandler
 * @see org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService
 */
public interface HandshakeInterceptor {

	/**
	 * Invoked before the handshake is processed.
	 *
	 * @param request the current request
	 * @param response the current response
	 * @param wsHandler the target WebSocket handler
	 * @param attributes attributes to make available via
	 *        {@link WebSocketSession#getHandshakeAttributes()}
	 *
	 * @return whether to proceed with the handshake {@code true} or abort {@code false}
	 */
	boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception;

	/**
	 * Invoked after the handshake is done. The response status and headers indicate the
	 * results of the handshake, i.e. whether it was successful or not.
	 *
	 * @param request the current request
	 * @param response the current response
	 * @param wsHandler the target WebSocket handler
	 * @param exception an exception raised during the handshake, or {@code null}
	 */
	void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception);

}
