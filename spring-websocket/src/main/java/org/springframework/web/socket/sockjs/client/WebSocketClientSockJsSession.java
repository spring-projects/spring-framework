/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.socket.sockjs.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.NativeWebSocketSession;

/**
 * An extension of {@link AbstractClientSockJsSession} wrapping and delegating
 * to an actual WebSocket session.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class WebSocketClientSockJsSession extends AbstractClientSockJsSession implements NativeWebSocketSession {

	@Nullable
	private WebSocketSession webSocketSession;

	/**
	 * Create a new {@code WebSocketClientSockJsSession}.
	 * @deprecated as of 6.0, in favor of {@link #WebSocketClientSockJsSession(TransportRequest, WebSocketHandler, CompletableFuture)}
	 */
	@Deprecated(since = "6.0")
	public WebSocketClientSockJsSession(TransportRequest request, WebSocketHandler handler,
			org.springframework.util.concurrent.SettableListenableFuture<WebSocketSession> connectFuture) {

		super(request, handler, connectFuture);
	}

	public WebSocketClientSockJsSession(TransportRequest request, WebSocketHandler handler,
			CompletableFuture<WebSocketSession> connectFuture) {

		super(request, handler, connectFuture);
	}


	@Override
	public Object getNativeSession() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		return this.webSocketSession;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <T> T getNativeSession(@Nullable Class<T> requiredType) {
		return (requiredType == null || requiredType.isInstance(this.webSocketSession) ? (T) this.webSocketSession : null);
	}

	@Override
	@Nullable
	public InetSocketAddress getLocalAddress() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		return this.webSocketSession.getLocalAddress();
	}

	@Override
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		return this.webSocketSession.getRemoteAddress();
	}

	@Override
	@Nullable
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

	public void initializeDelegateSession(WebSocketSession session) {
		this.webSocketSession = session;
	}

	@Override
	protected void sendInternal(TextMessage textMessage) throws IOException {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
		this.webSocketSession.sendMessage(textMessage);
	}

	@Override
	protected void disconnect(CloseStatus status) throws IOException {
		if (this.webSocketSession != null) {
			this.webSocketSession.close(status);
		}
	}

}
