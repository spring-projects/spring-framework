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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.sockjs.SockJsMessageDeliveryException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.SockJsSession;
import org.springframework.web.util.DisconnectedClientHelper;

/**
 * An abstract base class for SockJS sessions implementing {@link SockJsSession}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class AbstractSockJsSession implements SockJsSession {

	private enum State {NEW, OPEN, CLOSED}


	/**
	 * Log category to use for network failure after a client has gone away.
	 * @see DisconnectedClientHelper
	 */
	public static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
			"org.springframework.web.socket.sockjs.DisconnectedClient";

	private static final DisconnectedClientHelper disconnectedClientHelper =
			new DisconnectedClientHelper(DISCONNECTED_CLIENT_LOG_CATEGORY);


	protected final Log logger = LogFactory.getLog(getClass());

	protected final Object responseLock = new Object();

	private final String id;

	private final SockJsServiceConfig config;

	private final WebSocketHandler handler;

	private final Map<String, Object> attributes = new ConcurrentHashMap<>();

	private volatile State state = State.NEW;

	private final long timeCreated = System.currentTimeMillis();

	private volatile long timeLastActive = this.timeCreated;

	@Nullable
	private ScheduledFuture<?> heartbeatFuture;

	@Nullable
	private HeartbeatTask heartbeatTask;

	private volatile boolean heartbeatDisabled;


	/**
	 * Create a new instance.
	 * @param id the session ID
	 * @param config the SockJS service configuration options
	 * @param handler the recipient of SockJS messages
	 * @param attributes the attributes from the HTTP handshake to associate with the WebSocket
	 * session; the provided attributes are copied, the original map is not used.
	 */
	public AbstractSockJsSession(String id, SockJsServiceConfig config, WebSocketHandler handler,
			@Nullable Map<String, Object> attributes) {

		Assert.notNull(id, "Session id must not be null");
		Assert.notNull(config, "SockJsServiceConfig must not be null");
		Assert.notNull(handler, "WebSocketHandler must not be null");

		this.id = id;
		this.config = config;
		this.handler = handler;

		if (attributes != null) {
			this.attributes.putAll(attributes);
		}
	}


	@Override
	public String getId() {
		return this.id;
	}

	protected SockJsMessageCodec getMessageCodec() {
		return this.config.getMessageCodec();
	}

	public SockJsServiceConfig getSockJsServiceConfig() {
		return this.config;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}


	// Message sending

	@Override
	public final void sendMessage(WebSocketMessage<?> message) throws IOException {
		Assert.state(!isClosed(), "Cannot send a message when session is closed");
		Assert.isInstanceOf(TextMessage.class, message, "SockJS supports text messages only");
		sendMessageInternal(((TextMessage) message).getPayload());
	}

	protected abstract void sendMessageInternal(String message) throws IOException;


	// Lifecycle related methods

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
	 * Performs cleanup and notify the {@link WebSocketHandler}.
	 */
	@Override
	public final void close() throws IOException {
		close(new CloseStatus(3000, "Go away!"));
	}

	/**
	 * Performs cleanup and notify the {@link WebSocketHandler}.
	 */
	@Override
	public final void close(CloseStatus status) throws IOException {
		if (isOpen()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing SockJS session " + getId() + " with " + status);
			}
			this.state = State.CLOSED;
			try {
				if (isActive() && !CloseStatus.SESSION_NOT_RELIABLE.equals(status)) {
					try {
						writeFrameInternal(SockJsFrame.closeFrame(status.getCode(), status.getReason()));
					}
					catch (Throwable ex) {
						logger.debug("Failure while sending SockJS close frame", ex);
					}
				}
				updateLastActiveTime();
				cancelHeartbeat();
				disconnect(status);
			}
			finally {
				try {
					this.handler.afterConnectionClosed(this, status);
				}
				catch (Throwable ex) {
					logger.debug("Error from WebSocketHandler.afterConnectionClosed in " + this, ex);
				}
			}
		}
	}

	@Override
	public long getTimeSinceLastActive() {
		if (isNew()) {
			return (System.currentTimeMillis() - this.timeCreated);
		}
		else {
			return (isActive() ? 0 : System.currentTimeMillis() - this.timeLastActive);
		}
	}

	/**
	 * Should be invoked whenever the session becomes inactive.
	 */
	protected void updateLastActiveTime() {
		this.timeLastActive = System.currentTimeMillis();
	}

	@Override
	public void disableHeartbeat() {
		this.heartbeatDisabled = true;
		cancelHeartbeat();
	}

	protected void sendHeartbeat() throws SockJsTransportFailureException {
		synchronized (this.responseLock) {
			if (isActive() && !this.heartbeatDisabled) {
				writeFrame(SockJsFrame.heartbeatFrame());
				scheduleHeartbeat();
			}
		}
	}

	protected void scheduleHeartbeat() {
		if (this.heartbeatDisabled) {
			return;
		}
		synchronized (this.responseLock) {
			cancelHeartbeat();
			if (!isActive()) {
				return;
			}
			Instant time = Instant.now().plus(this.config.getHeartbeatTime(), ChronoUnit.MILLIS);
			this.heartbeatTask = new HeartbeatTask();
			this.heartbeatFuture = this.config.getTaskScheduler().schedule(this.heartbeatTask, time);
			if (logger.isTraceEnabled()) {
				logger.trace("Scheduled heartbeat in session " + getId());
			}
		}
	}

	protected void cancelHeartbeat() {
		synchronized (this.responseLock) {
			if (this.heartbeatFuture != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Cancelling heartbeat in session " + getId());
				}
				this.heartbeatFuture.cancel(false);
				this.heartbeatFuture = null;
			}
			if (this.heartbeatTask != null) {
				this.heartbeatTask.cancel();
				this.heartbeatTask = null;
			}
		}
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
	 * Actually close the underlying WebSocket session or in the case of HTTP
	 * transports complete the underlying request.
	 */
	protected abstract void disconnect(CloseStatus status) throws IOException;


	// Frame writing

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
		catch (Exception ex) {
			logWriteFrameFailure(ex);
			try {
				// Force disconnect (so we won't try to send close frame)
				disconnect(CloseStatus.SERVER_ERROR);
			}
			catch (Throwable disconnectFailure) {
				// Ignore
			}
			try {
				close(CloseStatus.SERVER_ERROR);
			}
			catch (Throwable closeFailure) {
				// Nothing of consequence, already forced disconnect
			}
			throw new SockJsTransportFailureException("Failed to write " + frame, getId(), ex);
		}
	}

	protected abstract void writeFrameInternal(SockJsFrame frame) throws IOException;

	private void logWriteFrameFailure(Throwable ex) {
		if (!disconnectedClientHelper.checkAndLogClientDisconnectedException(ex)) {
			logger.debug("Terminating connection after failure to send message to client", ex);
		}
	}


	// Delegation methods

	public void delegateConnectionEstablished() throws Exception {
		this.state = State.OPEN;
		this.handler.afterConnectionEstablished(this);
	}

	public void delegateMessages(String... messages) throws SockJsMessageDeliveryException {
		for (int i = 0; i < messages.length; i++) {
			try {
				if (isClosed()) {
					logUndeliveredMessages(i, messages);
					return;
				}
				this.handler.handleMessage(this, new TextMessage(messages[i]));
			}
			catch (Exception ex) {
				if (isClosed()) {
					if (logger.isTraceEnabled()) {
						logger.trace("Failed to handle message '" + messages[i] + "'", ex);
					}
					logUndeliveredMessages(i, messages);
					return;
				}
				throw new SockJsMessageDeliveryException(this.id, getUndelivered(messages, i), ex);
			}
		}
	}

	private void logUndeliveredMessages(int index, String[] messages) {
		List<String> undelivered = getUndelivered(messages, index);
		if (logger.isTraceEnabled() && !undelivered.isEmpty()) {
			logger.trace("Dropped inbound message(s) due to closed session: " + undelivered);
		}
	}

	private static List<String> getUndelivered(String[] messages, int i) {
		return switch (messages.length - i) {
			case 0 -> Collections.emptyList();
			case 1 -> (messages[i].trim().isEmpty() ?
					Collections.<String>emptyList() : Collections.singletonList(messages[i]));
			default -> Arrays.stream(Arrays.copyOfRange(messages, i, messages.length))
					.filter(message -> !message.trim().isEmpty())
					.toList();
		};
	}

	/**
	 * Invoked when the underlying connection is closed.
	 */
	public final void delegateConnectionClosed(CloseStatus status) throws Exception {
		if (!isClosed()) {
			try {
				updateLastActiveTime();
				// Avoid cancelHeartbeat() and responseLock within server "close" callback
				ScheduledFuture<?> future = this.heartbeatFuture;
				if (future != null) {
					this.heartbeatFuture = null;
					future.cancel(false);
				}
			}
			finally {
				this.state = State.CLOSED;
				this.handler.afterConnectionClosed(this, status);
			}
		}
	}

	/**
	 * Close due to error arising from SockJS transport handling.
	 */
	public void tryCloseWithSockJsTransportError(Throwable error, CloseStatus closeStatus) {
		if (logger.isDebugEnabled()) {
			logger.debug("Closing due to transport error for " + this);
		}
		try {
			delegateError(error);
		}
		catch (Throwable delegateException) {
			// Ignore
			logger.debug("Exception from error handling delegate", delegateException);
		}
		try {
			close(closeStatus);
		}
		catch (Throwable closeException) {
			logger.debug("Failure while closing " + this, closeException);
		}
	}

	public void delegateError(Throwable ex) throws Exception {
		this.handler.handleTransportError(this, ex);
	}


	// Self description

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id=" + getId() + "]";
	}


	private class HeartbeatTask implements Runnable {

		private boolean expired;

		@Override
		public void run() {
			synchronized (responseLock) {
				if (!this.expired && !isClosed()) {
					try {
						sendHeartbeat();
					}
					catch (Throwable ex) {
						// Ignore: already handled in writeFrame...
					}
					finally {
						this.expired = true;
					}
				}
			}
		}

		void cancel() {
			this.expired = true;
		}
	}

}
