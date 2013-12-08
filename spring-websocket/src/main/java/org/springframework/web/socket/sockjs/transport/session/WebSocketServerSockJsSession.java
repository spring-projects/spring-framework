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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;

/**
 * A SockJS session for use with the WebSocket transport.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketServerSockJsSession extends AbstractSockJsSession implements NativeWebSocketSession {

	private WebSocketSession webSocketSession;


	public WebSocketServerSockJsSession(String id, SockJsServiceConfig config,
			WebSocketHandler handler, Map<String, Object> attributes) {

		super(id, config, handler, attributes);
	}


	@Override
	public URI getUri() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getUri();
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getHandshakeHeaders();
	}

	@Override
	public Principal getPrincipal() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getPrincipal();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getLocalAddress();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getRemoteAddress();
	}

	@Override
	public String getAcceptedProtocol() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getAcceptedProtocol();
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getExtensions();
	}

	private void checkDelegateSessionInitialized() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
	}

	@Override
	public Object getNativeSession() {
		if ((this.webSocketSession != null) && (this.webSocketSession instanceof NativeWebSocketSession)) {
			return ((NativeWebSocketSession) this.webSocketSession).getNativeSession();
		}
		return null;
	}

	@Override
	public <T> T getNativeSession(Class<T> requiredType) {
		if ((this.webSocketSession != null) && (this.webSocketSession instanceof NativeWebSocketSession)) {
			return ((NativeWebSocketSession) this.webSocketSession).getNativeSession(requiredType);
		}
		return null;
	}


	public void initializeDelegateSession(WebSocketSession session) {
		this.webSocketSession = session;
		try {
			TextMessage message = new TextMessage(SockJsFrame.openFrame().getContent());
			this.webSocketSession.sendMessage(message);
			scheduleHeartbeat();
			delegateConnectionEstablished();
		}
		catch (Exception ex) {
			tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
		}
	}

	@Override
	public boolean isActive() {
		return ((this.webSocketSession != null) && this.webSocketSession.isOpen());
	}

	public void handleMessage(TextMessage message, WebSocketSession wsSession) throws Exception {
		String payload = message.getPayload();
		if (StringUtils.isEmpty(payload)) {
			logger.trace("Ignoring empty message");
			return;
		}
		String[] messages;
		try {
			messages = getSockJsServiceConfig().getMessageCodec().decode(payload);
		}
		catch (Throwable ex) {
			logger.error("Broken data received. Terminating WebSocket connection abruptly", ex);
			tryCloseWithSockJsTransportError(ex, CloseStatus.BAD_DATA);
			return;
		}
		delegateMessages(messages);
	}

	@Override
	public void sendMessageInternal(String message) throws SockJsTransportFailureException {
		cancelHeartbeat();
		SockJsMessageCodec messageCodec = getSockJsServiceConfig().getMessageCodec();
		SockJsFrame frame = SockJsFrame.messageFrame(messageCodec, message);
		writeFrame(frame);
		scheduleHeartbeat();
	}

	@Override
	protected void writeFrameInternal(SockJsFrame frame) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Write " + frame);
		}
		TextMessage message = new TextMessage(frame.getContent());
		this.webSocketSession.sendMessage(message);
	}

	@Override
	protected void disconnect(CloseStatus status) throws IOException {
		if (this.webSocketSession != null) {
			this.webSocketSession.close(status);
		}
	}

}
