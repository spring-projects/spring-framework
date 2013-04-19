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
import org.springframework.sockjs.server.AbstractServerSockJsSession;
import org.springframework.sockjs.server.SockJsConfiguration;
import org.springframework.sockjs.server.SockJsFrame;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * A SockJS implementation of {@link WebSocketHandler}. Delegates messages to and from a
 * {@link SockJsHandler} and adds SockJS message framing.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsWebSocketHandler implements WebSocketHandler {

	private static final Log logger = LogFactory.getLog(SockJsWebSocketHandler.class);

	private final SockJsConfiguration sockJsConfig;

	private final SockJsHandler sockJsHandler;

	private final Map<WebSocketSession, SockJsSessionSupport> sessions =
			new ConcurrentHashMap<WebSocketSession, SockJsSessionSupport>();

	// TODO: JSON library used must be configurable
	private final ObjectMapper objectMapper = new ObjectMapper();


	public SockJsWebSocketHandler(SockJsConfiguration sockJsConfig, SockJsHandler sockJsHandler) {
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
		SockJsSessionSupport session = new WebSocketServerSockJsSession(wsSession, getSockJsConfig());
		this.sessions.put(wsSession, session);
	}

	@Override
	public void handleTextMessage(String message, WebSocketSession wsSession) throws Exception {
		if (StringUtils.isEmpty(message)) {
			logger.trace("Ignoring empty message");
			return;
		}
		try {
			String[] messages = this.objectMapper.readValue(message, String[].class);
			SockJsSessionSupport session = getSockJsSession(wsSession);
			session.delegateMessages(messages);
		}
		catch (IOException e) {
			logger.error("Broken data received. Terminating WebSocket connection abruptly", e);
			wsSession.close();
		}
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
	public void handleError(Throwable exception, WebSocketSession webSocketSession) {
		SockJsSessionSupport session = getSockJsSession(webSocketSession);
		session.delegateError(exception);
	}

	private static String getSockJsSessionId(WebSocketSession wsSession) {
		Assert.notNull(wsSession, "wsSession is required");
		String path = wsSession.getURI().getPath();
		String[] segments = StringUtils.tokenizeToStringArray(path, "/");
		Assert.isTrue(segments.length > 3, "SockJS request should have at least 3 patgh segments: " + path);
		return segments[segments.length-2];
	}


	private class WebSocketServerSockJsSession extends AbstractServerSockJsSession {

		private final WebSocketSession wsSession;


		public WebSocketServerSockJsSession(WebSocketSession wsSession, SockJsConfiguration sockJsConfig)
				throws Exception {

			super(getSockJsSessionId(wsSession), sockJsConfig, getSockJsHandler());
			this.wsSession = wsSession;
			this.wsSession.sendTextMessage(SockJsFrame.openFrame().getContent());
			scheduleHeartbeat();
			delegateConnectionEstablished();
		}

		@Override
		public boolean isActive() {
			return this.wsSession.isOpen();
		}

		@Override
		public void sendMessageInternal(String message) throws IOException {
			cancelHeartbeat();
			writeFrame(SockJsFrame.messageFrame(message));
			scheduleHeartbeat();
		}

		@Override
		protected void writeFrameInternal(SockJsFrame frame) throws IOException {
			if (logger.isTraceEnabled()) {
				logger.trace("Write " + frame);
			}
			this.wsSession.sendTextMessage(frame.getContent());
		}

		@Override
		protected void disconnect(CloseStatus status) throws IOException {
			this.wsSession.close(status);
		}
	}

}
