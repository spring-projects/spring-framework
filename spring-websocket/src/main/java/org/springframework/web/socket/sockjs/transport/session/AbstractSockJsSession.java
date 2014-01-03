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

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.sockjs.SockJsMessageDeliveryException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.SockJsSession;

/**
 * An abstract base class SockJS sessions implementing {@link SockJsSession}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractSockJsSession implements SockJsSession {

	protected final Log logger = LogFactory.getLog(getClass());

	private final String id;

	private final SockJsServiceConfig config;

	private final WebSocketHandler handler;

	private final Map<String, Object> handshakeAttributes;

	private State state = State.NEW;

	private final long timeCreated = System.currentTimeMillis();

	private long timeLastActive = this.timeCreated;

	private ScheduledFuture<?> heartbeatTask;


	/**
	 * @param id the session ID
	 * @param config SockJS service configuration options
	 * @param handler the recipient of SockJS messages
	 */
	public AbstractSockJsSession(String id, SockJsServiceConfig config,
			WebSocketHandler handler, Map<String, Object> handshakeAttributes) {

		Assert.notNull(id, "SessionId must not be null");
		Assert.notNull(config, "SockJsConfig must not be null");
		Assert.notNull(handler, "WebSocketHandler must not be null");

		this.id = id;
		this.config = config;
		this.handler = handler;
		this.handshakeAttributes = handshakeAttributes;
	}


	@Override
	public String getId() {
		return this.id;
	}

	public SockJsServiceConfig getSockJsServiceConfig() {
		return this.config;
	}

	@Override
	public Map<String, Object> getHandshakeAttributes() {
		return this.handshakeAttributes;
	}

	public boolean isNew() {
		return State.NEW.equals(this.state);
	}

	@Override
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

	@Override
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

	public void delegateConnectionEstablished() throws Exception {
		this.state = State.OPEN;
		this.handler.afterConnectionEstablished(this);
	}

	public void delegateMessages(String[] messages) throws SockJsMessageDeliveryException {
		List<String> undelivered = new ArrayList<String>(Arrays.asList(messages));
		for (String message : messages) {
			try {
				if (isClosed()) {
					throw new SockJsMessageDeliveryException(this.id, undelivered, null);
				}
				else {
					this.handler.handleMessage(this, new TextMessage(message));
					undelivered.remove(0);
				}
			}
			catch (Throwable ex) {
				throw new SockJsMessageDeliveryException(this.id, undelivered, ex);
			}
		}
	}

	/**
	 * Invoked in reaction to the underlying connection being closed by the remote side
	 * (or the WebSocket container) in order to perform cleanup and notify the
	 * {@link WebSocketHandler}. This is in contrast to {@link #close()} that pro-actively
	 * closes the connection.
	 */
	public final void delegateConnectionClosed(CloseStatus status) throws Exception {
		if (!isClosed()) {
			if (logger.isDebugEnabled()) {
				logger.debug(this + " was closed, " + status);
			}
			try {
				updateLastActiveTime();
				cancelHeartbeat();
			}
			finally {
				this.state = State.CLOSED;
				this.handler.afterConnectionClosed(this, status);
			}
		}
	}

	public void delegateError(Throwable ex) throws Exception {
		this.handler.handleTransportError(this, ex);
	}

	public final synchronized void sendMessage(WebSocketMessage<?> message) throws IOException {
		Assert.isTrue(!isClosed(), "Cannot send a message when session is closed");
		Assert.isInstanceOf(TextMessage.class, message, "Expected text message: " + message);
		sendMessageInternal(((TextMessage) message).getPayload());
	}

	protected abstract void sendMessageInternal(String message) throws IOException;

	/**
	 * {@inheritDoc}
	 *
	 * <p>Performs cleanup and notifies the {@link WebSocketHandler}.
	 */
	@Override
	public final void close() throws IOException {
		close(new CloseStatus(3000, "Go away!"));
	}

	/**
	 * {@inheritDoc}
	 * <p>Performs cleanup and notifies the {@link WebSocketHandler}.
	 */
	@Override
	public final void close(CloseStatus status) throws IOException {
		if (isOpen()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing " + this + ", " + status);
			}
			try {
				if (isActive()) {
					try {
						// bypass writeFrame
						writeFrameInternal(SockJsFrame.closeFrame(status.getCode(), status.getReason()));
					}
					catch (Throwable ex) {
						logger.warn("Failed to send SockJS close frame: " + ex.getMessage());
					}
				}
				updateLastActiveTime();
				cancelHeartbeat();
				disconnect(status);
			}
			finally {
				this.state = State.CLOSED;
				try {
					this.handler.afterConnectionClosed(this, status);
				}
				catch (Throwable ex) {
					logger.error("Unhandled error for " + this, ex);
				}
			}
		}
	}

	protected abstract void disconnect(CloseStatus status) throws IOException;

	/**
	 * Close due to error arising from SockJS transport handling.
	 */
	public void tryCloseWithSockJsTransportError(Throwable ex, CloseStatus closeStatus) {
		logger.error("Closing due to transport error for " + this);
		try {
			delegateError(ex);
		}
		catch (Throwable delegateEx) {
			// ignore
		}
		try {
			close(closeStatus);
		}
		catch (Throwable closeEx) {
			// ignore
		}
	}

	/**
	 * For internal use within a TransportHandler and the (TransportHandler-specific)
	 * session class.
	 */
	protected void writeFrame(SockJsFrame frame) throws SockJsTransportFailureException {
		if (logger.isTraceEnabled()) {
			logger.trace("Preparing to write " + frame);
		}
		try {
			writeFrameInternal(frame);
		}
		catch (Throwable ex) {
			logger.error("Terminating connection after failure to send message to client. " +
					"This may be because the client has gone away " +
					"(see https://java.net/jira/browse/SERVLET_SPEC-44)", ex);
			try {
				disconnect(CloseStatus.SERVER_ERROR);
				close(CloseStatus.SERVER_ERROR);
			}
			catch (Throwable ex2) {
				// ignore
			}
			throw new SockJsTransportFailureException("Failed to write " + frame, this.getId(), ex);
		}
	}

	protected abstract void writeFrameInternal(SockJsFrame frame) throws IOException;

	public synchronized void sendHeartbeat() throws SockJsTransportFailureException {
		if (isActive()) {
			writeFrame(SockJsFrame.heartbeatFrame());
			scheduleHeartbeat();
		}
	}

	protected void scheduleHeartbeat() {
		Assert.state(this.config.getTaskScheduler() != null, "No TaskScheduler configured for heartbeat");
		cancelHeartbeat();
		if (!isActive()) {
			return;
		}
		Date time = new Date(System.currentTimeMillis() + this.config.getHeartbeatTime());
		this.heartbeatTask = this.config.getTaskScheduler().schedule(new Runnable() {
			public void run() {
				try {
					sendHeartbeat();
				}
				catch (Throwable ex) {
					// ignore
				}
			}
		}, time);
		if (logger.isTraceEnabled()) {
			logger.trace("Scheduled heartbeat after " + this.config.getHeartbeatTime()/1000 + " seconds");
		}
	}

	protected void cancelHeartbeat() {
		if ((this.heartbeatTask != null) && !this.heartbeatTask.isDone()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Cancelling heartbeat");
			}
			this.heartbeatTask.cancel(false);
		}
		this.heartbeatTask = null;
	}


	@Override
	public String toString() {
		return "SockJS session id=" + this.id;
	}


	private enum State { NEW, OPEN, CLOSED }

}
