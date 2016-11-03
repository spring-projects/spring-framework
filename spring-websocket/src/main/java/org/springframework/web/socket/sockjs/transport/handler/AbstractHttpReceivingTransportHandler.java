/*
 * Copyright 2002-2016 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.transport.SockJsSession;
import org.springframework.web.socket.sockjs.transport.session.AbstractHttpSockJsSession;

/**
 * Base class for HTTP transport handlers that receive messages via HTTP POST.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHttpReceivingTransportHandler extends AbstractTransportHandler {


	@Override
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, SockJsSession wsSession) throws SockJsException {

		Assert.notNull(wsSession, "No session");
		AbstractHttpSockJsSession sockJsSession = (AbstractHttpSockJsSession) wsSession;

		handleRequestInternal(request, response, wsHandler, sockJsSession);
	}

	protected void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, AbstractHttpSockJsSession sockJsSession) throws SockJsException {

		String[] messages;
		try {
			messages = readMessages(request);
		}
		catch (IOException ex) {
			logger.error("Failed to read message", ex);
			if (ex.getClass().getName().contains("Mapping")) {
				// e.g. Jackson's JsonMappingException, indicating an incomplete payload
				handleReadError(response, "Payload expected.", sockJsSession.getId());
			}
			else {
				handleReadError(response, "Broken JSON encoding.", sockJsSession.getId());
			}
			return;
		}
		catch (Throwable ex) {
			logger.error("Failed to read message", ex);
			handleReadError(response, "Failed to read message(s)", sockJsSession.getId());
			return;
		}
		if (messages == null) {
			handleReadError(response, "Payload expected.", sockJsSession.getId());
			return;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Received message(s): " + Arrays.asList(messages));
		}
		response.setStatusCode(getResponseStatus());
		response.getHeaders().setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));

		sockJsSession.delegateMessages(messages);
	}

	private void handleReadError(ServerHttpResponse response, String error, String sessionId) {
		try {
			response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			response.getBody().write(error.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException ex) {
			throw new SockJsException("Failed to send error: " + error, sessionId, ex);
		}
	}

	protected abstract String[] readMessages(ServerHttpRequest request) throws IOException;

	protected abstract HttpStatus getResponseStatus();

	@Override
	public boolean checkSessionType(SockJsSession session) {
		return session instanceof AbstractHttpSockJsSession;
	}

}
