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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.NestedExceptionUtils;
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
	 * Log category to use on network IO exceptions after a client has gone away.
	 * <p>The Servlet API does not provide notifications when a client disconnects;
	 * see <a href="https://java.net/jira/browse/SERVLET_SPEC-44">SERVLET_SPEC-44</a>.
	 * Therefore network IO failures may occur simply because a client has gone away,
	 * and that can fill the logs with unnecessary stack traces.
	 * <p>We make a best effort to identify such network failures, on a per-server
	 * basis, and log them under a separate log category. A simple one-line message
	 * is logged at DEBUG level, while a full stack trace is shown at TRACE level.
	 * @see #disconnectedClientLogger
	 */
	public static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
			"org.springframework.web.socket.sockjs.DisconnectedClient";

	/**
	 * Tomcat: ClientAbortException or EOFException
	 * Jetty: EofException
	 * WildFly, GlassFish: java.io.IOException "Broken pipe" (already covered)
	 * @see #indicatesDisconnectedClient(Throwable)
	 */
	private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS =
			new HashSet<String>(Arrays.asList("ClientAbortException", "EOFException", "EofException"));


	/**
	 * Separate logger to use on network IO failure after a client has gone away.
	 * @see #DISCONNECTED_CLIENT_LOG_CATEGORY
	 */
	protected static final Log disconnectedClientLogger = LogFactory.getLog(DISCONNECTED_CLIENT_LOG_CATEGORY);

	protected final Log logger = LogFactory.getLog(getClass());

	protected final Object responseLock = new Object();

	private final String id;

	private final SockJsServiceConfig config;

	private final WebSocketHandler handler;

	private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	private volatile State state = State.NEW;

	private final long timeCreated = System.currentTimeMillis();

	private volatile long timeLastActive = this.timeCreated;

	private ScheduledFuture<?> heartbeatFuture;

	private HeartbeatTask heartbeatTask;

	private volatile boolean heartbeatDisabled;


	/**
	 * Create a new instance.
	 * @param id the session ID
	 * @param config SockJS service configuration options
	 * @param handler the recipient of SockJS messages
	 * @param attributes attributes from the HTTP handshake to associate with the WebSocket
	 * session; the provided attributes are copied, the original map is not used.
	 */
	public AbstractSockJsSession(String id, SockJsServiceConfig config, WebSocketHandler handler,
			Map<String, Object> attributes) {

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

	public void sendHeartbeat() throws SockJsTransportFailureException {
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
			Date time = new Date(System.currentTimeMillis() + this.config.getHeartbeatTime());
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
		catch (Throwable ex) {
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
		if (indicatesDisconnectedClient(ex)) {
			if (disconnectedClientLogger.isTraceEnabled()) {
				disconnectedClientLogger.trace("Looks like the client has gone away", ex);
			}
			else if (disconnectedClientLogger.isDebugEnabled()) {
				disconnectedClientLogger.debug("Looks like the client has gone away: " + ex +
						" (For a full stack trace, set the log category '" + DISCONNECTED_CLIENT_LOG_CATEGORY +
						"' to TRACE level.)");
			}
		}
		else {
			logger.debug("Terminating connection after failure to send message to client", ex);
		}
	}

	private boolean indicatesDisconnectedClient(Throwable ex)  {
		String message = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
		message = (message != null ? message.toLowerCase() : "");
		String className = ex.getClass().getSimpleName();
		return (message.contains("broken pipe") || DISCONNECTED_CLIENT_EXCEPTIONS.contains(className));
	}


	// Delegation methods

	public void delegateConnectionEstablished() throws Exception {
		this.state = State.OPEN;
		this.handler.afterConnectionEstablished(this);
	}

	public void delegateMessages(String... messages) throws SockJsMessageDeliveryException {
		List<String> undelivered = new ArrayList<String>(Arrays.asList(messages));
		for (String message : messages) {
			try {
				if (isClosed()) {
					throw new SockJsMessageDeliveryException(this.id, undelivered, "Session closed");
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
