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

package org.springframework.web.socket.adapter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;
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
 * @param <T> the native session type
 */
public abstract class AbstractWebSocketSession<T> implements NativeWebSocketSession {

	protected static final IdGenerator idGenerator = new AlternativeJdkIdGenerator();

	protected static final Log logger = LogFactory.getLog(NativeWebSocketSession.class);


	private final Map<String, Object> attributes = new ConcurrentHashMap<>();

	@Nullable
	private T nativeSession;


	/**
	 * Create a new instance and associate the given attributes with it.
	 * @param attributes the attributes from the HTTP handshake to associate with the WebSocket
	 * session; the provided attributes are copied, the original map is not used.
	 */
	public AbstractWebSocketSession(@Nullable Map<String, Object> attributes) {
		if (attributes != null) {
			attributes.entrySet().stream()
					.filter(entry -> (entry.getKey() != null && entry.getValue() != null))
					.forEach(entry -> this.attributes.put(entry.getKey(), entry.getValue()));
		}
	}


	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	@Override
	public T getNativeSession() {
		Assert.state(this.nativeSession != null, "WebSocket session not yet initialized");
		return this.nativeSession;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Nullable
	public <R> R getNativeSession(@Nullable Class<R> requiredType) {
		return (requiredType == null || requiredType.isInstance(this.nativeSession) ? (R) this.nativeSession : null);
	}

	public void initializeNativeSession(T session) {
		Assert.notNull(session, "WebSocket session must not be null");
		this.nativeSession = session;
	}

	protected final void checkNativeSessionInitialized() {
		Assert.state(this.nativeSession != null, "WebSocket session is not yet initialized");
	}

	@Override
	public final void sendMessage(WebSocketMessage<?> message) throws IOException {
		checkNativeSessionInitialized();

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
		if (this.nativeSession != null) {
			return getClass().getSimpleName() + "[id=" + getId() + ", uri=" + getUri() + "]";
		}
		else {
			return getClass().getSimpleName() + "[nativeSession=null]";
		}
	}

}
