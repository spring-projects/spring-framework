/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
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
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;

/**
 * A SockJS session for use with the WebSocket transport.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class WebSocketServerSockJsSession extends AbstractSockJsSession implements NativeWebSocketSession {

	@Nullable
	private WebSocketSession webSocketSession;

	private volatile boolean openFrameSent;

	private final Queue<String> initSessionCache = new LinkedBlockingDeque<>();

	private final Object initSessionLock = new Object();

	private final Object disconnectLock = new Object();

	private volatile boolean disconnected;


	public WebSocketServerSockJsSession(String id, SockJsServiceConfig config,
			WebSocketHandler handler, @Nullable Map<String, Object> attributes) {

		super(id, config, handler, attributes);
	}


	@Override
	@Nullable
	public URI getUri() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		return this.webSocketSession.getUri();
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		return this.webSocketSession.getHandshakeHeaders();
	}

	@Override
	public Principal getPrincipal() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		return this.webSocketSession.getPrincipal();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		return this.webSocketSession.getLocalAddress();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		return this.webSocketSession.getRemoteAddress();
	}

	@Override
	public String getAcceptedProtocol() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		return this.webSocketSession.getAcceptedProtocol();
	}

	@Override
	public void setTextMessageSizeLimit(int messageSizeLimit) {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		this.webSocketSession.setTextMessageSizeLimit(messageSizeLimit);
	}

	@Override
	public int getTextMessageSizeLimit() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		return this.webSocketSession.getTextMessageSizeLimit();
	}

	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		this.webSocketSession.setBinaryMessageSizeLimit(messageSizeLimit);
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		return this.webSocketSession.getBinaryMessageSizeLimit();
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		return this.webSocketSession.getExtensions();
	}

	@Override
	public Object getNativeSession() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		return (this.webSocketSession instanceof NativeWebSocketSession nativeWsSession ?
				nativeWsSession.getNativeSession() : this.webSocketSession);
	}

	@Override
	@Nullable
	public <T> T getNativeSession(@Nullable Class<T> requiredType) {
		return (this.webSocketSession instanceof NativeWebSocketSession nativeWsSession ?
				nativeWsSession.getNativeSession(requiredType) : null);
	}


	public void initializeDelegateSession(WebSocketSession session) {
		synchronized (this.initSessionLock) {
			this.webSocketSession = session;
			try {
				// Let "our" handler know before sending the open frame to the remote handler
				delegateConnectionEstablished();
				this.webSocketSession.sendMessage(new TextMessage(SockJsFrame.openFrame().getContent()));

				// Flush any messages cached in the meantime
				while (!this.initSessionCache.isEmpty()) {
					writeFrame(SockJsFrame.messageFrame(getMessageCodec(), this.initSessionCache.poll()));
				}
				scheduleHeartbeat();
				this.openFrameSent = true;
			}
			catch (Exception ex) {
				tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
			}
		}
	}

	@Override
	public boolean isActive() {
		return (this.webSocketSession != null && this.webSocketSession.isOpen() && !this.disconnected);
	}

	public void handleMessage(TextMessage message, WebSocketSession wsSession) throws Exception {
		String payload = message.getPayload();
		if (!StringUtils.hasLength(payload)) {
			return;
		}
		String[] messages;
		try {
			messages = getSockJsServiceConfig().getMessageCodec().decode(payload);
		}
		catch (Exception ex) {
			logger.error("Broken data received. Terminating WebSocket connection abruptly", ex);
			tryCloseWithSockJsTransportError(ex, CloseStatus.BAD_DATA);
			return;
		}
		if (messages != null) {
			delegateMessages(messages);
		}
	}

	@Override
	public void sendMessageInternal(String message) throws SockJsTransportFailureException {
		// Open frame not sent yet?
		// If in the session initialization thread, then cache, otherwise wait.
		if (!this.openFrameSent) {
			synchronized (this.initSessionLock) {
				if (!this.openFrameSent) {
					this.initSessionCache.add(message);
					return;
				}
			}
		}

		cancelHeartbeat();
		writeFrame(SockJsFrame.messageFrame(getMessageCodec(), message));
		scheduleHeartbeat();
	}

	@Override
	protected void writeFrameInternal(SockJsFrame frame) throws IOException {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		if (logger.isTraceEnabled()) {
			logger.trace("Writing " + frame);
		}
		TextMessage message = new TextMessage(frame.getContent());
		this.webSocketSession.sendMessage(message);
	}

	@Override
	protected void disconnect(CloseStatus status) throws IOException {
		if (isActive()) {
			synchronized (this.disconnectLock) {
				if (isActive()) {
					this.disconnected = true;
					if (this.webSocketSession != null) {
						this.webSocketSession.close(status);
					}
				}
			}
		}
	}

}
