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
import org.springframework.sockjs.SockJsSessionSupport;
import org.springframework.sockjs.server.TransportHandler;

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
	public boolean canCreateSession() {
		return false;
	}

	@Override
	public SockJsSessionSupport createSession(String sessionId) {
		throw new IllegalStateException("Transport handlers receiving messages do not create new sessions");
	}

	@Override
	public boolean handleNoSession(ServerHttpRequest request, ServerHttpResponse response) {
		response.setStatusCode(HttpStatus.NOT_FOUND);
		return false;
	}

	@Override
	public void handleRequest(ServerHttpRequest request, ServerHttpResponse response, SockJsSessionSupport session)
			throws Exception {

		String[] messages = null;
		try {
			messages = readMessages(request);
		}
		catch (JsonMappingException ex) {
			response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			response.getBody().write("Payload expected.".getBytes("UTF-8"));
			return;
		}
		catch (IOException ex) {
			response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			response.getBody().write("Broken JSON encoding.".getBytes("UTF-8"));
			return;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Received messages: " + Arrays.asList(messages));
		}

		session.delegateMessages(messages);

		response.setStatusCode(getResponseStatus());
		response.getHeaders().setContentType(new MediaType("text", "plain", Charset.forName("UTF-8")));
	}

	protected abstract String[] readMessages(ServerHttpRequest request) throws IOException;

	protected abstract HttpStatus getResponseStatus();

}
