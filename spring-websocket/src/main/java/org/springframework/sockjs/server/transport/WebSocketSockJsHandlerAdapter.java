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
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.sockjs.SockJsHandler;
import org.springframework.sockjs.SockJsSessionSupport;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.util.Assert;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;


/**
 * A {@link WebSocketHandler} that merely delegates to a {@link SockJsHandler} without any
 * SockJS message framing. For use with raw WebSocket communication at SockJS path
 * "/websocket".
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
	public void newSession(WebSocketSession wsSession) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("New session: " + wsSession);
		}
		SockJsSessionSupport session = new WebSocketSessionAdapter(wsSession);
		this.sessions.put(wsSession, session);
	}

	@Override
	public void handleTextMessage(WebSocketSession wsSession, String message) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Received payload " + message);
		}
		SockJsSessionSupport session = getSockJsSession(wsSession);
		session.delegateMessages(message);
	}

	@Override
	public void handleBinaryMessage(WebSocketSession session, InputStream message) throws Exception {
		// should not happen
		throw new UnsupportedOperationException();
	}

	@Override
	public void handleException(WebSocketSession webSocketSession, Throwable exception) {
		SockJsSessionSupport session = getSockJsSession(webSocketSession);
		session.delegateException(exception);
	}

	@Override
	public void sessionClosed(WebSocketSession webSocketSession, int statusCode, String reason) throws Exception {
		logger.debug("WebSocket session closed " + webSocketSession);
		SockJsSessionSupport session = this.sessions.remove(webSocketSession);
		session.connectionClosed();
	}


	private class WebSocketSessionAdapter extends SockJsSessionSupport {

		private WebSocketSession wsSession;


		public WebSocketSessionAdapter(WebSocketSession wsSession) throws Exception {
			super(String.valueOf(wsSession.hashCode()), getSockJsHandler());
			this.wsSession = wsSession;
			connectionInitialized();
		}

		@Override
		public boolean isActive() {
			return (!isClosed() && this.wsSession.isOpen());
		}

		@Override
		public void sendMessage(String message) throws IOException {
			this.wsSession.sendText(message);
		}

		@Override
		public void connectionClosed() {
			logger.debug("Session closed");
			super.connectionClosed();
			this.wsSession = null;
		}

		@Override
		public void close() {
			if (!isClosed()) {
				logger.debug("Closing session");
				super.close();
				this.wsSession.close();
				this.wsSession = null;
			}
		}
	}

}
