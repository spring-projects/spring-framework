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

package org.springframework.web.socket.sockjs.transport.handler;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.SockJsProcessingException;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.support.ExceptionWebSocketHandlerDecorator;

import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Base class for HTTP-based transports that read input messages from HTTP requests.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHttpReceivingTransportHandler
		extends TransportHandlerSupport implements TransportHandler {


	@Override
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler webSocketHandler, WebSocketSession webSocketSession) throws SockJsProcessingException {

		// TODO: check "Sec-WebSocket-Protocol" header
		// https://github.com/sockjs/sockjs-client/issues/130

		handleRequestInternal(request, response, webSocketHandler, webSocketSession);
	}

	protected void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, WebSocketSession wsSession) throws SockJsProcessingException {

		String[] messages = null;
		try {
			messages = readMessages(request);
		}
		catch (JsonMappingException ex) {
			logger.error("Failed to read message: " + ex.getMessage());
			sendInternalServerError(response, "Payload expected.", wsSession.getId());
			return;
		}
		catch (IOException ex) {
			logger.error("Failed to read message: " + ex.getMessage());
			sendInternalServerError(response, "Broken JSON encoding.", wsSession.getId());
			return;
		}
		catch (Throwable t) {
			logger.error("Failed to read message: " + t.getMessage());
			sendInternalServerError(response, "Failed to process messages", wsSession.getId());
			return;
		}

		if (messages == null) {
			sendInternalServerError(response, "Payload expected.", wsSession.getId());
			return;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Received message(s): " + Arrays.asList(messages));
		}

		response.setStatusCode(getResponseStatus());
		response.getHeaders().setContentType(new MediaType("text", "plain", Charset.forName("UTF-8")));

		try {
			for (String message : messages) {
				wsHandler.handleMessage(wsSession, new TextMessage(message));
			}
		}
		catch (Throwable t) {
			ExceptionWebSocketHandlerDecorator.tryCloseWithError(wsSession, t, logger);
			throw new SockJsProcessingException("Unhandled WebSocketHandler error in " + this, t, wsSession.getId());
		}
	}

	protected void sendInternalServerError(ServerHttpResponse response, String error,
			String sessionId) throws SockJsProcessingException {

		try {
			response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			response.getBody().write(error.getBytes("UTF-8"));
		}
		catch (Throwable t) {
			throw new SockJsProcessingException("Failed to send error message to client", t, sessionId);
		}
	}

	protected abstract String[] readMessages(ServerHttpRequest request) throws IOException;

	protected abstract HttpStatus getResponseStatus();

}
