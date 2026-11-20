/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs;

import org.jspecify.annotations.Nullable;

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
 * <code>&lt;async-support&gt;true&lt;/async-support&gt;</code> element to servlet and filter declarations
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
	void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			@Nullable String sockJsPath, WebSocketHandler handler) throws SockJsException;

}
