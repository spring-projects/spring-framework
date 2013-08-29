/*
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

package org.springframework.web.socket.adapter;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * An abstract base class for implementations of {@link WebSocketSession}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractWebSocketSesssion<T> implements WebSocketSession, NativeWebSocketSession {

	protected final Log logger = LogFactory.getLog(getClass());

	private T nativeSession;

	private final Map<String, Object> handshakeAttributes;


	/**
	 * Class constructor
	 *
	 * @param handshakeAttributes attributes from the HTTP handshake to make available
	 *        through the WebSocket session
	 */
	public AbstractWebSocketSesssion(Map<String, Object> handshakeAttributes) {
		this.handshakeAttributes = handshakeAttributes;
	}


	@Override
	public Map<String, Object> getHandshakeAttributes() {
		return this.handshakeAttributes;
	}

	@Override
	public T getNativeSession() {
		return this.nativeSession;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getNativeSession(Class<R> requiredType) {
		if (requiredType != null) {
			if (requiredType.isInstance(this.nativeSession)) {
				return (R) this.nativeSession;
			}
		}
		return null;
	}

	public void initializeNativeSession(T session) {
		Assert.notNull(session, "session must not be null");
		this.nativeSession = session;
	}

	protected final void checkNativeSessionInitialized() {
		Assert.state(this.nativeSession != null, "WebSocket session is not yet initialized");
	}

	@Override
	public final void sendMessage(WebSocketMessage message) throws IOException {

		checkNativeSessionInitialized();
		Assert.isTrue(isOpen(), "Cannot send message after connection closed.");

		if (logger.isTraceEnabled()) {
			logger.trace("Sending " + message + ", " + this);
		}

		if (message instanceof TextMessage) {
			sendTextMessage((TextMessage) message);
		}
		else if (message instanceof BinaryMessage) {
			sendBinaryMessage((BinaryMessage) message);
		}
		else if (message instanceof PingMessage) {
			sendPingMessage((PingMessage) message);
		}
		else if (message instanceof PongMessage) {
			sendPongMessage((PongMessage) message);
		}
		else {
			throw new IllegalStateException("Unexpected WebSocketMessage type: " + message);
		}
	}

	protected abstract void sendTextMessage(TextMessage message) throws IOException;

	protected abstract void sendBinaryMessage(BinaryMessage message) throws IOException;

	protected abstract void sendPingMessage(PingMessage message) throws IOException;

	protected abstract void sendPongMessage(PongMessage message) throws IOException;

	@Override
	public final void close() throws IOException {
		close(CloseStatus.NORMAL);
	}

	@Override
	public final void close(CloseStatus status) throws IOException {
		checkNativeSessionInitialized();
		if (logger.isDebugEnabled()) {
			logger.debug("Closing " + this);
		}
		closeInternal(status);
	}

	protected abstract void closeInternal(CloseStatus status) throws IOException;


	@Override
	public String toString() {
		return "WebSocket session id=" + getId();
	}

}
