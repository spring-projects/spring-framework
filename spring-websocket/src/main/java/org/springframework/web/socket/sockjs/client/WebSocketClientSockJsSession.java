/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.util.Assert;
import org.springframework.util.concurrent.SettableListenableFuture;
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

	private WebSocketSession webSocketSession;


	public WebSocketClientSockJsSession(TransportRequest request, WebSocketHandler handler,
			SettableListenableFuture<WebSocketSession> connectFuture) {

		super(request, handler, connectFuture);
	}


	@Override
	public Object getNativeSession() {
		return this.webSocketSession;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeSession(Class<T> requiredType) {
		return (requiredType == null || requiredType.isInstance(this.webSocketSession) ? (T) this.webSocketSession : null);
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
	public void setTextMessageSizeLimit(int messageSizeLimit) {
		checkDelegateSessionInitialized();
		this.webSocketSession.setTextMessageSizeLimit(messageSizeLimit);
	}

	@Override
	public int getTextMessageSizeLimit() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getTextMessageSizeLimit();
	}

	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		checkDelegateSessionInitialized();
		this.webSocketSession.setBinaryMessageSizeLimit(messageSizeLimit);
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getBinaryMessageSizeLimit();
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		checkDelegateSessionInitialized();
		return this.webSocketSession.getExtensions();
	}

	private void checkDelegateSessionInitialized() {
		Assert.state(this.webSocketSession != null, "WebSocketSession not yet initialized");
	}

	public void initializeDelegateSession(WebSocketSession session) {
		this.webSocketSession = session;
	}

	@Override
	protected void sendInternal(TextMessage textMessage) throws IOException {
		this.webSocketSession.sendMessage(textMessage);
	}

	@Override
	protected void disconnect(CloseStatus status) throws IOException {
		if (this.webSocketSession != null) {
			this.webSocketSession.close(status);
		}
	}

}
