/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.transport.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
	public boolean checkSessionType(SockJsSession session) {
		return (session instanceof AbstractHttpSockJsSession);
	}

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
				// for example, Jackson's JsonMappingException, indicating an incomplete payload
				handleReadError(response, "Payload expected.", sockJsSession.getId());
			}
			else {
				handleReadError(response, "Broken JSON encoding.", sockJsSession.getId());
			}
			return;
		}
		catch (Exception ex) {
			logger.error("Failed to read message", ex);
			handleReadError(response, "Failed to read message(s)", sockJsSession.getId());
			return;
		}
		if (messages == null) {
			handleReadError(response, "Payload expected.", sockJsSession.getId());
			return;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Received message(s): " + Arrays.toString(messages));
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


	protected abstract String @Nullable [] readMessages(ServerHttpRequest request) throws IOException;

	protected abstract HttpStatusCode getResponseStatus();

}
