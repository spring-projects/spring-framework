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

package org.springframework.sockjs.server.transport;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.sockjs.AbstractSockJsSession;
import org.springframework.sockjs.server.TransportErrorException;
import org.springframework.sockjs.server.TransportHandler;
import org.springframework.websocket.WebSocketHandler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TODO
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHttpReceivingTransportHandler implements TransportHandler {

	protected final Log logger = LogFactory.getLog(this.getClass());

	// TODO: the JSON library used must be configurable
	private final ObjectMapper objectMapper = new ObjectMapper();


	public ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	@Override
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler webSocketHandler, AbstractSockJsSession session)
					throws TransportErrorException {

		if (session == null) {
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		handleRequestInternal(request, response, session);
	}

	protected void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			AbstractSockJsSession session) throws TransportErrorException {

		String[] messages = null;
		try {
			messages = readMessages(request);
		}
		catch (JsonMappingException ex) {
			sendInternalServerError(response, "Payload expected.", session.getId());
			return;
		}
		catch (IOException ex) {
			sendInternalServerError(response, "Broken JSON encoding.", session.getId());
			return;
		}
		catch (Throwable t) {
			sendInternalServerError(response, "Failed to process messages", session.getId());
			return;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Received messages: " + Arrays.asList(messages));
		}

		session.delegateMessages(messages);

		response.setStatusCode(getResponseStatus());
		response.getHeaders().setContentType(new MediaType("text", "plain", Charset.forName("UTF-8")));
	}

	protected void sendInternalServerError(ServerHttpResponse response, String error,
			String sessionId) throws TransportErrorException {

		try {
			response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			response.getBody().write(error.getBytes("UTF-8"));
		}
		catch (Throwable t) {
			throw new TransportErrorException("Failed to send error message to client", t, sessionId);
		}
	}

	protected abstract String[] readMessages(ServerHttpRequest request) throws IOException;

	protected abstract HttpStatus getResponseStatus();

}
