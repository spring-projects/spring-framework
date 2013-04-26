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

package org.springframework.sockjs;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.HandlerProvider;
import org.springframework.websocket.TextMessage;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;
import org.springframework.websocket.adapter.WebSocketHandlerInvoker;


/**
 * TODO
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractSockJsSession implements WebSocketSession {

	protected final Log logger = LogFactory.getLog(getClass());


	private final String sessionId;

	private WebSocketHandlerInvoker handler;

	private State state = State.NEW;

	private long timeCreated = System.currentTimeMillis();

	private long timeLastActive = System.currentTimeMillis();


	/**
	 *
	 * @param sessionId
	 * @param handlerProvider the recipient of SockJS messages
	 */
	public AbstractSockJsSession(String sessionId, HandlerProvider<WebSocketHandler> handlerProvider) {
		Assert.notNull(sessionId, "sessionId is required");
		Assert.notNull(handlerProvider, "handlerProvider is required");
		this.sessionId = sessionId;
		this.handler = new WebSocketHandlerInvoker(handlerProvider).setLogger(logger);
	}

	public String getId() {
		return this.sessionId;
	}

	@Override
	public boolean isSecure() {
		// TODO
		return false;
	}

	@Override
	public URI getURI() {
		// TODO
		return null;
	}

	public boolean isNew() {
		return State.NEW.equals(this.state);
	}

	public boolean isOpen() {
		return State.OPEN.equals(this.state);
	}

	public boolean isClosed() {
		return State.CLOSED.equals(this.state);
	}

	/**
	 * Polling and Streaming sessions periodically close the current HTTP request and
	 * wait for the next request to come through. During this "downtime" the session is
	 * still open but inactive and unable to send messages and therefore has to buffer
	 * them temporarily. A WebSocket session by contrast is stateful and remain active
	 * until closed.
	 */
	public abstract boolean isActive();

	/**
	 * Return the time since the session was last active, or otherwise if the
	 * session is new, the time since the session was created.
	 */
	public long getTimeSinceLastActive() {
		if (isNew()) {
			return (System.currentTimeMillis() - this.timeCreated);
		}
		else {
			return isActive() ? 0 : System.currentTimeMillis() - this.timeLastActive;
		}
	}

	/**
	 * Should be invoked whenever the session becomes inactive.
	 */
	protected void updateLastActiveTime() {
		this.timeLastActive = System.currentTimeMillis();
	}

	public void delegateConnectionEstablished() {
		this.state = State.OPEN;
		this.handler.afterConnectionEstablished(this);
	}

	/**
	 * Close due to error arising from SockJS transport handling.
	 */
	protected void tryCloseWithSockJsTransportError(Throwable ex, CloseStatus closeStatus) {
		delegateError(ex);
		this.handler.tryCloseWithError(this, ex, closeStatus);
	}

	public void delegateMessages(String[] messages) {
		for (String message : messages) {
			this.handler.handleTextMessage(new TextMessage(message), this);
		}
	}

	public void delegateError(Throwable ex) {
		this.handler.handleTransportError(ex, this);
	}

	/**
	 * Invoked in reaction to the underlying connection being closed by the remote side
	 * (or the WebSocket container) in order to perform cleanup and notify the
	 * {@link TextMessageHandler}. This is in contrast to {@link #close()} that pro-actively
	 * closes the connection.
	 */
	public final void delegateConnectionClosed(CloseStatus status) {
		if (!isClosed()) {
			if (logger.isDebugEnabled()) {
				logger.debug(this + " was closed, " + status);
			}
			try {
				connectionClosedInternal(status);
			}
			finally {
				this.state = State.CLOSED;
				this.handler.afterConnectionClosed(status, this);
			}
		}
	}

	protected void connectionClosedInternal(CloseStatus status) {
	}

	/**
	 * {@inheritDoc}
	 * <p>Performs cleanup and notifies the {@link SockJsHandler}.
	 */
	public final void close() throws IOException {
		close(CloseStatus.NORMAL);
	}

	/**
	 * {@inheritDoc}
	 * <p>Performs cleanup and notifies the {@link SockJsHandler}.
	 */
	public final void close(CloseStatus status) throws IOException {
		if (!isClosed()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing " + this + ", " + status);
			}
			try {
				closeInternal(status);
			}
			finally {
				this.state = State.CLOSED;
				this.handler.afterConnectionClosed(status, this);
			}
		}
	}

	protected abstract void closeInternal(CloseStatus status) throws IOException;


	@Override
	public String toString() {
		return "SockJS session id=" + this.sessionId;
	}


	private enum State { NEW, OPEN, CLOSED }

}
