/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.socket.messaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.SessionLimitExceededException;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;
import org.springframework.web.socket.sockjs.transport.session.PollingSockJsSession;
import org.springframework.web.socket.sockjs.transport.session.StreamingSockJsSession;

/**
 * An implementation of {@link WebSocketHandler} that delegates incoming WebSocket
 * messages to a {@link SubProtocolHandler} along with a {@link MessageChannel} to which
 * the sub-protocol handler can send messages from WebSocket clients to the application.
 *
 * <p>Also an implementation of {@link MessageHandler} that finds the WebSocket session
 * associated with the {@link Message} and passes it, along with the message, to the
 * sub-protocol handler to send messages from the application back to the client.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Andy Wilkinson
 * @author Artem Bilan
 * @since 4.0
 */
public class SubProtocolWebSocketHandler
		implements WebSocketHandler, SubProtocolCapable, MessageHandler, SmartLifecycle {

	/** The default value for {@link #setTimeToFirstMessage(int) timeToFirstMessage}. */
	private static final int DEFAULT_TIME_TO_FIRST_MESSAGE = 60 * 1000;


	private final Log logger = LogFactory.getLog(SubProtocolWebSocketHandler.class);


	private final MessageChannel clientInboundChannel;

	private final SubscribableChannel clientOutboundChannel;

	private final Map<String, SubProtocolHandler> protocolHandlerLookup =
			new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	private final Set<SubProtocolHandler> protocolHandlers = new LinkedHashSet<>();

	private @Nullable SubProtocolHandler defaultProtocolHandler;

	private final Map<String, WebSocketSessionHolder> sessions = new ConcurrentHashMap<>();

	private int sendTimeLimit = 10 * 1000;

	private int sendBufferSizeLimit = 512 * 1024;

	private int timeToFirstMessage = DEFAULT_TIME_TO_FIRST_MESSAGE;

	private volatile long lastSessionCheckTime = System.currentTimeMillis();

	private final Lock sessionCheckLock = new ReentrantLock();

	private final DefaultStats stats = new DefaultStats();

	private @Nullable Integer phase;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	/**
	 * Create a new {@code SubProtocolWebSocketHandler} for the given inbound and outbound channels.
	 * @param clientInboundChannel the inbound {@code MessageChannel}
	 * @param clientOutboundChannel the outbound {@code MessageChannel}
	 */
	public SubProtocolWebSocketHandler(MessageChannel clientInboundChannel, SubscribableChannel clientOutboundChannel) {
		Assert.notNull(clientInboundChannel, "Inbound MessageChannel must not be null");
		Assert.notNull(clientOutboundChannel, "Outbound MessageChannel must not be null");
		this.clientInboundChannel = clientInboundChannel;
		this.clientOutboundChannel = clientOutboundChannel;
	}


	/**
	 * Configure one or more handlers to use depending on the sub-protocol requested by
	 * the client in the WebSocket handshake request.
	 * @param protocolHandlers the sub-protocol handlers to use
	 */
	public void setProtocolHandlers(List<SubProtocolHandler> protocolHandlers) {
		this.protocolHandlerLookup.clear();
		this.protocolHandlers.clear();
		for (SubProtocolHandler handler : protocolHandlers) {
			addProtocolHandler(handler);
		}
	}

	public List<SubProtocolHandler> getProtocolHandlers() {
		return new ArrayList<>(this.protocolHandlers);
	}

	/**
	 * Register a sub-protocol handler.
	 */
	public void addProtocolHandler(SubProtocolHandler handler) {
		List<String> protocols = handler.getSupportedProtocols();
		if (CollectionUtils.isEmpty(protocols)) {
			if (logger.isErrorEnabled()) {
				logger.error("No sub-protocols for " + handler);
			}
			return;
		}
		for (String protocol : protocols) {
			SubProtocolHandler replaced = this.protocolHandlerLookup.put(protocol, handler);
			if (replaced != null && replaced != handler) {
				throw new IllegalStateException("Cannot map " + handler +
						" to protocol '" + protocol + "': already mapped to " + replaced + ".");
			}
		}
		this.protocolHandlers.add(handler);
	}

	/**
	 * Return the sub-protocols keyed by protocol name.
	 */
	public Map<String, SubProtocolHandler> getProtocolHandlerMap() {
		return this.protocolHandlerLookup;
	}

	/**
	 * Set the {@link SubProtocolHandler} to use when the client did not request a
	 * sub-protocol.
	 * @param defaultProtocolHandler the default handler
	 */
	public void setDefaultProtocolHandler(@Nullable SubProtocolHandler defaultProtocolHandler) {
		this.defaultProtocolHandler = defaultProtocolHandler;
		if (this.protocolHandlerLookup.isEmpty()) {
			setProtocolHandlers(Collections.singletonList(defaultProtocolHandler));
		}
	}

	/**
	 * Return the default sub-protocol handler to use.
	 */
	public @Nullable SubProtocolHandler getDefaultProtocolHandler() {
		return this.defaultProtocolHandler;
	}

	/**
	 * Return all supported protocols.
	 */
	@Override
	public List<String> getSubProtocols() {
		return new ArrayList<>(this.protocolHandlerLookup.keySet());
	}

	/**
	 * Specify the send-time limit (milliseconds).
	 * @see ConcurrentWebSocketSessionDecorator
	 */
	public void setSendTimeLimit(int sendTimeLimit) {
		this.sendTimeLimit = sendTimeLimit;
	}

	/**
	 * Return the send-time limit (milliseconds).
	 */
	public int getSendTimeLimit() {
		return this.sendTimeLimit;
	}

	/**
	 * Specify the buffer-size limit (number of bytes).
	 * @see ConcurrentWebSocketSessionDecorator
	 */
	public void setSendBufferSizeLimit(int sendBufferSizeLimit) {
		this.sendBufferSizeLimit = sendBufferSizeLimit;
	}

	/**
	 * Return the buffer-size limit (number of bytes).
	 */
	public int getSendBufferSizeLimit() {
		return this.sendBufferSizeLimit;
	}

	/**
	 * Set the maximum time allowed in milliseconds after the WebSocket connection
	 * is established and before the first sub-protocol message is received.
	 * <p>This handler is for WebSocket connections that use a sub-protocol.
	 * Therefore, we expect the client to send at least one sub-protocol message
	 * in the beginning, or else we assume the connection isn't doing well, for example,
	 * proxy issue, slow network, and can be closed.
	 * <p>By default this is set to {@code 60,000} (1 minute).
	 * @param timeToFirstMessage the maximum time allowed in milliseconds
	 * @since 5.1
	 * @see #checkSessions()
	 */
	public void setTimeToFirstMessage(int timeToFirstMessage) {
		this.timeToFirstMessage = timeToFirstMessage;
	}

	/**
	 * Return the maximum time allowed after the WebSocket connection is
	 * established and before the first sub-protocol message.
	 * @since 5.1
	 */
	public int getTimeToFirstMessage() {
		return this.timeToFirstMessage;
	}

	/**
	 * Set the phase that this handler should run in.
	 * <p>By default, this is {@link SmartLifecycle#DEFAULT_PHASE}, but with
	 * {@code @EnableWebSocketMessageBroker} configuration it is set to 0.
	 * @since 6.1.4
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public int getPhase() {
		return (this.phase != null ? this.phase : SmartLifecycle.super.getPhase());
	}

	/**
	 * Return a String describing internal state and counters.
	 * Effectively {@code toString()} on {@link #getStats() getStats()}.
	 */
	public String getStatsInfo() {
		return this.stats.toString();
	}

	/**
	 * Return a structured object with various session counters.
	 * @since 5.2
	 */
	public Stats getStats() {
		return this.stats;
	}



	@Override
	public final void start() {
		Assert.state(this.defaultProtocolHandler != null || !this.protocolHandlers.isEmpty(), "No handlers");

		synchronized (this.lifecycleMonitor) {
			this.clientOutboundChannel.subscribe(this);
			this.running = true;
		}
	}

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.clientOutboundChannel.unsubscribe(this);
		}

		// Proactively notify all active WebSocket sessions
		for (WebSocketSessionHolder holder : this.sessions.values()) {
			try {
				holder.getSession().close(CloseStatus.GOING_AWAY);
			}
			catch (Throwable ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Failed to close '" + holder.getSession() + "': " + ex);
				}
			}
		}
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	@Override
	public final boolean isRunning() {
		return this.running;
	}


	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		// WebSocketHandlerDecorator could close the session
		if (!session.isOpen()) {
			return;
		}

		checkSessions();

		this.stats.incrementSessionCount(session);
		session = decorateSession(session);
		this.sessions.put(session.getId(), new WebSocketSessionHolder(session));
		findProtocolHandler(session).afterSessionStarted(session, this.clientInboundChannel);
	}

	/**
	 * Handle an inbound message from a WebSocket client.
	 */
	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		WebSocketSessionHolder holder = this.sessions.get(session.getId());
		if (holder != null) {
			session = holder.getSession();
		}
		SubProtocolHandler protocolHandler = findProtocolHandler(session);
		protocolHandler.handleMessageFromClient(session, message, this.clientInboundChannel);
		if (holder != null) {
			holder.setHasHandledMessages();
		}
	}

	/**
	 * Handle an outbound Spring Message to a WebSocket client.
	 */
	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		String sessionId = resolveSessionId(message);
		if (sessionId == null) {
			if (logger.isErrorEnabled()) {
				logger.error("Could not find session id in " + message);
			}
			return;
		}

		WebSocketSessionHolder holder = this.sessions.get(sessionId);
		if (holder == null) {
			if (logger.isDebugEnabled()) {
				// The broker may not have removed the session yet
				logger.debug("No session for " + message);
			}
			return;
		}

		WebSocketSession session = holder.getSession();
		try {
			findProtocolHandler(session).handleMessageToClient(session, message);
		}
		catch (SessionLimitExceededException ex) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Terminating '" + session + "'", ex);
				}
				else if (logger.isWarnEnabled()) {
					logger.warn("Terminating '" + session + "': " + ex.getMessage());
				}
				this.stats.incrementLimitExceededCount();
				clearSession(session, ex.getStatus()); // clear first, session may be unresponsive
				session.close(ex.getStatus());
			}
			catch (Exception secondException) {
				logger.debug("Failure while closing session " + sessionId + ".", secondException);
			}
		}
		catch (Exception ex) {
			// Could be part of normal workflow (for example, browser tab closed)
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to send message to client in " + session + ": " + message, ex);
			}
		}
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		this.stats.incrementTransportError();
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		clearSession(session, closeStatus);
	}

	@Override
	public boolean supportsPartialMessages() {
		return false;
	}


	/**
	 * Decorate the given {@link WebSocketSession}, if desired.
	 * <p>The default implementation builds a {@link ConcurrentWebSocketSessionDecorator}
	 * with the configured {@link #getSendTimeLimit() send-time limit} and
	 * {@link #getSendBufferSizeLimit() buffer-size limit}.
	 * @param session the original {@code WebSocketSession}
	 * @return the decorated {@code WebSocketSession}, or potentially the given session as-is
	 * @since 4.3.13
	 */
	protected WebSocketSession decorateSession(WebSocketSession session) {
		return new ConcurrentWebSocketSessionDecorator(session, getSendTimeLimit(), getSendBufferSizeLimit());
	}

	/**
	 * Find a {@link SubProtocolHandler} for the given session.
	 * @param session the {@code WebSocketSession} to find a handler for
	 */
	protected final SubProtocolHandler findProtocolHandler(WebSocketSession session) {
		String protocol = null;
		try {
			protocol = session.getAcceptedProtocol();
		}
		catch (Exception ex) {
			// Shouldn't happen
			logger.error("Failed to obtain session.getAcceptedProtocol(): " +
					"will use the default protocol handler (if configured).", ex);
		}

		SubProtocolHandler handler;
		if (StringUtils.hasLength(protocol)) {
			handler = this.protocolHandlerLookup.get(protocol);
			if (handler == null) {
				throw new IllegalStateException(
						"No handler for '" + protocol + "' among " + this.protocolHandlerLookup);
			}
		}
		else {
			if (this.defaultProtocolHandler != null) {
				handler = this.defaultProtocolHandler;
			}
			else if (this.protocolHandlers.size() == 1) {
				handler = this.protocolHandlers.iterator().next();
			}
			else {
				throw new IllegalStateException("Multiple protocol handlers configured and " +
						"no protocol was negotiated. Consider configuring a default SubProtocolHandler.");
			}
		}
		return handler;
	}

	private @Nullable String resolveSessionId(Message<?> message) {
		for (SubProtocolHandler handler : this.protocolHandlerLookup.values()) {
			String sessionId = handler.resolveSessionId(message);
			if (sessionId != null) {
				return sessionId;
			}
		}
		if (this.defaultProtocolHandler != null) {
			String sessionId = this.defaultProtocolHandler.resolveSessionId(message);
			if (sessionId != null) {
				return sessionId;
			}
		}
		return null;
	}

	/**
	 * A higher-level protocol can use heartbeats to detect sessions that need to
	 * be cleaned up. However, if a WebSocket session is established, but messages
	 * can't flow (for example, due to a proxy issue), then the higher level protocol is
	 * never successfully negotiated, and without heartbeats, sessions can hang.
	 * The method  checks for sessions that have not received any messages 60
	 * seconds after the WebSocket session was established, and closes them.
	 */
	private void checkSessions() {
		long currentTime = System.currentTimeMillis();
		long timeSinceLastCheck = currentTime - this.lastSessionCheckTime;
		if (!isRunning() || timeSinceLastCheck < getTimeToFirstMessage() / 2) {
			return;
		}

		if (this.sessionCheckLock.tryLock()) {
			try {
				for (WebSocketSessionHolder holder : this.sessions.values()) {
					if (holder.hasHandledMessages()) {
						continue;
					}
					long timeSinceCreated = currentTime - holder.getCreateTime();
					if (timeSinceCreated < getTimeToFirstMessage()) {
						continue;
					}
					WebSocketSession session = holder.getSession();
					if (logger.isInfoEnabled()) {
						logger.info("No messages received after " + timeSinceCreated + " ms. " +
								"Closing " + holder.getSession() + ".");
					}
					try {
						this.stats.incrementNoMessagesReceivedCount();
						session.close(CloseStatus.SESSION_NOT_RELIABLE);
					}
					catch (Throwable ex) {
						if (logger.isWarnEnabled()) {
							logger.warn("Failed to close unreliable " + session, ex);
						}
					}
				}
			}
			finally {
				this.lastSessionCheckTime = currentTime;
				this.sessionCheckLock.unlock();
			}
		}
	}

	private void clearSession(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Clearing session " + session.getId());
		}
		if (this.sessions.remove(session.getId()) != null) {
			this.stats.decrementSessionCount(session);
		}
		findProtocolHandler(session).afterSessionEnded(session, closeStatus, this.clientInboundChannel);
	}


	@Override
	public String toString() {
		return "SubProtocolWebSocketHandler" + this.protocolHandlers;
	}


	private static class WebSocketSessionHolder {

		private final WebSocketSession session;

		private final long createTime;

		private volatile boolean hasHandledMessages;

		public WebSocketSessionHolder(WebSocketSession session) {
			this.session = session;
			this.createTime = System.currentTimeMillis();
		}

		public WebSocketSession getSession() {
			return this.session;
		}

		public long getCreateTime() {
			return this.createTime;
		}

		public void setHasHandledMessages() {
			this.hasHandledMessages = true;
		}

		public boolean hasHandledMessages() {
			return this.hasHandledMessages;
		}

		@Override
		public String toString() {
			return "WebSocketSessionHolder[session=" + this.session + ", createTime=" +
					this.createTime + ", hasHandledMessages=" + this.hasHandledMessages + "]";
		}
	}


	/**
	 * Contract for access to session counters.
	 * @since 5.2
	 */
	public interface Stats {

		int getTotalSessions();

		int getWebSocketSessions();

		int getHttpStreamingSessions();

		int getHttpPollingSessions();

		int getLimitExceededSessions();

		int getNoMessagesReceivedSessions();

		int getTransportErrorSessions();
	}


	private class DefaultStats implements Stats {

		private final AtomicInteger total = new AtomicInteger();

		private final AtomicInteger webSocket = new AtomicInteger();

		private final AtomicInteger httpStreaming = new AtomicInteger();

		private final AtomicInteger httpPolling = new AtomicInteger();

		private final AtomicInteger limitExceeded = new AtomicInteger();

		private final AtomicInteger noMessagesReceived = new AtomicInteger();

		private final AtomicInteger transportError = new AtomicInteger();

		@Override
		public int getTotalSessions() {
			return this.total.get();
		}

		@Override
		public int getWebSocketSessions() {
			return this.webSocket.get();
		}

		@Override
		public int getHttpStreamingSessions() {
			return this.httpStreaming.get();
		}

		@Override
		public int getHttpPollingSessions() {
			return this.httpPolling.get();
		}

		@Override
		public int getLimitExceededSessions() {
			return this.limitExceeded.get();
		}

		@Override
		public int getNoMessagesReceivedSessions() {
			return this.noMessagesReceived.get();
		}

		@Override
		public int getTransportErrorSessions() {
			return this.transportError.get();
		}

		void incrementSessionCount(WebSocketSession session) {
			getCountFor(session).incrementAndGet();
			this.total.incrementAndGet();
		}

		void decrementSessionCount(WebSocketSession session) {
			getCountFor(session).decrementAndGet();
		}

		void incrementLimitExceededCount() {
			this.limitExceeded.incrementAndGet();
		}

		void incrementNoMessagesReceivedCount() {
			this.noMessagesReceived.incrementAndGet();
		}

		void incrementTransportError() {
			this.transportError.incrementAndGet();
		}

		AtomicInteger getCountFor(WebSocketSession session) {
			session = WebSocketSessionDecorator.unwrap(session);
			if (session instanceof PollingSockJsSession) {
				return this.httpPolling;
			}
			else if (session instanceof StreamingSockJsSession) {
				return this.httpStreaming;
			}
			else {
				return this.webSocket;
			}
		}

		@Override
		public String toString() {
			return SubProtocolWebSocketHandler.this.sessions.size() +
					" current WS(" + this.webSocket.get() +
					")-HttpStream(" + this.httpStreaming.get() +
					")-HttpPoll(" + this.httpPolling.get() + "), " +
					this.total.get() + " total, " +
					(this.limitExceeded.get() + this.noMessagesReceived.get()) + " closed abnormally (" +
					this.noMessagesReceived.get() + " connect failure, " +
					this.limitExceeded.get() + " send limit, " +
					this.transportError.get() + " transport error)";
		}
	}

}
