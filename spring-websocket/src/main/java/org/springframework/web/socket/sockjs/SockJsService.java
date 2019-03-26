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

package org.springframework.web.socket.sockjs;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;

/**
 * The main entry point for processing HTTP requests from SockJS clients.
 *
 * <p>In a Servlet 3+ container, {@link org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler}
 * can be used to invoke this service. The processing servlet, as well as all filters involved,
 * must have asynchronous support enabled through the ServletContext API or by adding an
 * {@code <async-support>true</async-support>} element to servlet and filter declarations
 * in web.xml.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler
 */
public interface SockJsService {

	/**
	 * Process a SockJS HTTP request.
	 * <p>See the "Base URL", "Static URLs", and "Session URLs" sections of the <a
	 * href="https://sockjs.github.io/sockjs-protocol/sockjs-protocol-0.3.3.html">SockJS
	 * protocol</a> for details on the types of URLs expected.
	 * @param request the current request
	 * @param response the current response
	 * @param sockJsPath the remainder of the path within the SockJS service prefix
	 * @param handler the handler that will exchange messages with the SockJS client
	 * @throws SockJsException raised when request processing fails; generally, failed
	 * attempts to send messages to clients automatically close the SockJS session
	 * and raise {@link SockJsTransportFailureException}; failed attempts to read
	 * messages from clients do not automatically close the session and may result
	 * in {@link SockJsMessageDeliveryException} or {@link SockJsException};
	 * exceptions from the WebSocketHandler can be handled internally or through
	 * {@link ExceptionWebSocketHandlerDecorator} or some alternative decorator.
	 * The former is automatically added when using
	 * {@link org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler}.
	 */
	void handleRequest(ServerHttpRequest request, ServerHttpResponse response, String sockJsPath,
			WebSocketHandler handler) throws SockJsException;

}
