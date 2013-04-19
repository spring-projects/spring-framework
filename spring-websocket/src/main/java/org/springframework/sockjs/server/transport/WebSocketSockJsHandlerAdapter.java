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

package org.springframework.sockjs.server.transport;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.sockjs.SockJsHandler;
import org.springframework.sockjs.SockJsSessionSupport;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.util.Assert;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;


/**
 * A plain {@link WebSocketHandler} to {@link SockJsHandler} adapter that merely delegates
 * without any additional SockJS message framing. Used for raw WebSocket communication at
 * SockJS path "/websocket".
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketSockJsHandlerAdapter implements WebSocketHandler {

	private static final Log logger = LogFactory.getLog(WebSocketSockJsHandlerAdapter.class);

	private final SockJsConfiguration sockJsConfig;

	private final SockJsHandler sockJsHandler;

	private final Map<WebSocketSession, SockJsSessionSupport> sessions =
			new ConcurrentHashMap<WebSocketSession, SockJsSessionSupport>();


	public WebSocketSockJsHandlerAdapter(SockJsConfiguration sockJsConfig, SockJsHandler sockJsHandler) {
		Assert.notNull(sockJsConfig, "sockJsConfig is required");
		Assert.notNull(sockJsHandler, "sockJsHandler is required");
		this.sockJsConfig = sockJsConfig;
		this.sockJsHandler = sockJsHandler;
	}

	protected SockJsConfiguration getSockJsConfig() {
		return this.sockJsConfig;
	}

	protected SockJsHandler getSockJsHandler() {
		return this.sockJsHandler;
	}

	protected SockJsSessionSupport getSockJsSession(WebSocketSession wsSession) {
		return this.sessions.get(wsSession);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
		SockJsSessionSupport session = new SockJsWebSocketSessionAdapter(wsSession);
		this.sessions.put(wsSession, session);
	}

	@Override
	public void handleTextMessage(String message, WebSocketSession wsSession) throws Exception {
		SockJsSessionSupport session = getSockJsSession(wsSession);
		session.delegateMessages(new String[] { message });
	}

	@Override
	public void handleBinaryMessage(byte[] message, WebSocketSession session) throws Exception {
		logger.warn("Unexpected binary message for " + session);
		session.close(CloseStatus.NOT_ACCEPTABLE);
	}

	@Override
	public void afterConnectionClosed(CloseStatus status, WebSocketSession wsSession) throws Exception {
		SockJsSessionSupport session = this.sessions.remove(wsSession);
		session.delegateConnectionClosed(status);
	}

	@Override
	public void handleError(Throwable exception, WebSocketSession wsSession) {
		logger.error("Error for " + wsSession);
		SockJsSessionSupport session = getSockJsSession(wsSession);
		session.delegateError(exception);
	}


	private class SockJsWebSocketSessionAdapter extends SockJsSessionSupport {

		private final WebSocketSession wsSession;


		public SockJsWebSocketSessionAdapter(WebSocketSession wsSession) throws Exception {
			super(wsSession.getId(), getSockJsHandler());
			this.wsSession = wsSession;
			delegateConnectionEstablished();
		}

		@Override
		public boolean isActive() {
			return this.wsSession.isOpen();
		}

		@Override
		public void sendMessage(String message) throws IOException {
			this.wsSession.sendTextMessage(message);
		}

		@Override
		public void closeInternal(CloseStatus status) throws IOException {
			this.wsSession.close(status);
		}
	}

}
