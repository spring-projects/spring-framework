/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs.transport;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsService;

/**
 * Handle a SockJS session URL, i.e. transport-specific request.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface TransportHandler {


	/**
	 * @return the transport type supported by this handler
	 */
	TransportType getTransportType();

	/**
	 * Handle the given request and delegate messages to the provided
	 * {@link WebSocketHandler}.
	 *
	 * @param request the current request
	 * @param response the current response
	 * @param handler the target WebSocketHandler, never {@code null}
	 * @param session the SockJS session, never {@code null}
	 *
	 * @throws SockJsException raised when request processing fails as explained in
	 *         {@link SockJsService}
	 */
	void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler, WebSocketSession session) throws SockJsException;

}
