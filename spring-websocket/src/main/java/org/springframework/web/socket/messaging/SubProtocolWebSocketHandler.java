/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.messaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
 * @author Andy Wilkinson
 * @author Artem Bilan
 * @since 4.0
 */
public class SubProtocolWebSocketHandler
		implements WebSocketHandler, SubProtocolCapable, MessageHandler, SmartLifecycle {

	/**
	 * Sessions connected to this handler use a sub-protocol. Hence we expect to
	 * receive some client messages. If we don't receive any within a minute, the
	 * connection isn't doing well (proxy issue, slow network?) and can be closed.
	 * @see #checkSessions()
	 */
	private static final int TIME_TO_FIRST_MESSAGE = 60 * 1000;


	private final Log logger = LogFactory.getLog(SubProtocolWebSocketHandler.class);


	private final MessageChannel clientInboundChannel;

	private final SubscribableChannel clientOutboundChannel;

	private final Map<String, SubProtocolHandler> protocolHandlerLookup =
			new TreeMap<String, SubProtocolHandler>(String.CASE_INSENSITIVE_ORDER);

	private final Set<SubProtocolHandler> protocolHandlers = new LinkedHashSet<SubProtocolHandler>();

	private SubProtocolHandler defaultProtocolHandler;

	private final Map<String, WebSocketSessionHolder> sessions = new ConcurrentHashMap<String, WebSocketSessionHolder>();

	private int sendTimeLimit = 10 * 1000;

	private int sendBufferSizeLimit = 512 * 1024;

	private volatile long lastSessionCheckTime = System.currentTimeMillis();

	private final ReentrantLock sessionCheckLock = new ReentrantLock();

	private final Stats stats = new Stats();

	private final Object lifecycleMonitor = new Object();

	private volatile boolean running = false;


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
		return new ArrayList<SubProtocolHandler>(this.protocolHandlers);
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
				throw new IllegalStateException("Can't map " + handler +
						" to protocol '" + protocol + "'. Already mapped to " + replaced + ".");
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
	public void setDefaultProtocolHandler(SubProtocolHandler defaultProtocolHandler) {
		this.defaultProtocolHandler = defaultProtocolHandler;
		if (this.protocolHandlerLookup.isEmpty()) {
			setProtocolHandlers(Collections.singletonList(defaultProtocolHandler));
		}
	}

	/**
	 * Return the default sub-protocol handler to use.
	 */
	public SubProtocolHandler getDefaultProtocolHandler() {
		return this.defaultProtocolHandler;
	}

	/**
	 * Return all supported protocols.
	 */
	public List<String> getSubProtocols() {
		return new ArrayList<String>(this.protocolHandlerLookup.keySet());
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
	 * Return a String describing internal state and counters.
	 */
	public String getStatsInfo() {
		return this.stats.toString();
	}


	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	@Override
	public final boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	@Override
	public final void start() {
		Assert.isTrue(this.defaultProtocolHandler != null || !this.protocolHandlers.isEmpty(), "No handlers");
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
			for (WebSocketSessionHolder holder : this.sessions.values()) {
				try {
					holder.getSession().close(CloseStatus.GOING_AWAY);
				}
				catch (Throwable ex) {
					if (logger.isErrorEnabled()) {
						logger.error("Failed to close '" + holder.getSession() + "': " + ex);
					}
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
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		// WebSocketHandlerDecorator could close the session
		if (!session.isOpen()) {
			return;
		}
		this.stats.incrementSessionCount(session);
		session = new ConcurrentWebSocketSessionDecorator(session, getSendTimeLimit(), getSendBufferSizeLimit());
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
		checkSessions();
	}

	/**
	 * Handle an outbound Spring Message to a WebSocket client.
	 */
	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		String sessionId = resolveSessionId(message);
		if (sessionId == null) {
			if (logger.isErrorEnabled()) {
				logger.error("Couldn't find session id in " + message);
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
				this.stats.incrementLimitExceededCount();
				clearSession(session, ex.getStatus()); // clear first, session may be unresponsive
				session.close(ex.getStatus());
			}
			catch (Exception secondException) {
				logger.debug("Failure while closing session " + sessionId + ".", secondException);
			}
		}
		catch (Exception ex) {
			// Could be part of normal workflow (e.g. browser tab closed)
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


	protected final SubProtocolHandler findProtocolHandler(WebSocketSession session) {
		String protocol = null;
		try {
			protocol = session.getAcceptedProtocol();
		}
		catch (Exception ex) {
			// Shouldn't happen
			logger.error("Failed to obtain session.getAcceptedProtocol(). " +
					"Will use the default protocol handler (if configured).", ex);
		}

		SubProtocolHandler handler;
		if (!StringUtils.isEmpty(protocol)) {
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

	private String resolveSessionId(Message<?> message) {
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
	 * When a session is connected through a higher-level protocol it has a chance
	 * to use heartbeat management to shut down sessions that are too slow to send
	 * or receive messages. However, after a WebSocketSession is established and
	 * before the higher level protocol is fully connected there is a possibility
	 * for sessions to hang. This method checks and closes any sessions that have
	 * been connected for more than 60 seconds without having received a single
	 * message.
	 */
	private void checkSessions() throws IOException {
		long currentTime = System.currentTimeMillis();
		if (!isRunning() || (currentTime - this.lastSessionCheckTime < TIME_TO_FIRST_MESSAGE)) {
			return;
		}

		if (this.sessionCheckLock.tryLock()) {
			try {
				for (WebSocketSessionHolder holder : this.sessions.values()) {
					if (holder.hasHandledMessages()) {
						continue;
					}
					long timeSinceCreated = currentTime - holder.getCreateTime();
					if (timeSinceCreated < TIME_TO_FIRST_MESSAGE) {
						continue;
					}
					WebSocketSession session = holder.getSession();
					if (logger.isErrorEnabled()) {
						logger.error("No messages received after " + timeSinceCreated + " ms. " +
								"Closing " + holder.getSession() + ".");
					}
					try {
						this.stats.incrementNoMessagesReceivedCount();
						session.close(CloseStatus.SESSION_NOT_RELIABLE);
					}
					catch (Throwable ex) {
						if (logger.isErrorEnabled()) {
							logger.error("Failure while closing " + session, ex);
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

		private final long createTime = System.currentTimeMillis();

		private volatile boolean handledMessages;

		private WebSocketSessionHolder(WebSocketSession session) {
			this.session = session;
		}

		public WebSocketSession getSession() {
			return this.session;
		}

		public long getCreateTime() {
			return this.createTime;
		}

		public void setHasHandledMessages() {
			this.handledMessages = true;
		}

		public boolean hasHandledMessages() {
			return this.handledMessages;
		}

		@Override
		public String toString() {
			return "WebSocketSessionHolder[session=" + this.session + ", createTime=" +
					this.createTime + ", hasHandledMessages=" + this.handledMessages + "]";
		}
	}


	private class Stats {

		private final AtomicInteger total = new AtomicInteger();

		private final AtomicInteger webSocket = new AtomicInteger();

		private final AtomicInteger httpStreaming = new AtomicInteger();

		private final AtomicInteger httpPolling = new AtomicInteger();

		private final AtomicInteger limitExceeded = new AtomicInteger();

		private final AtomicInteger noMessagesReceived = new AtomicInteger();

		private final AtomicInteger transportError = new AtomicInteger();

		public void incrementSessionCount(WebSocketSession session) {
			getCountFor(session).incrementAndGet();
			this.total.incrementAndGet();
		}

		public void decrementSessionCount(WebSocketSession session) {
			getCountFor(session).decrementAndGet();
		}

		public void incrementLimitExceededCount() {
			this.limitExceeded.incrementAndGet();
		}

		public void incrementNoMessagesReceivedCount() {
			this.noMessagesReceived.incrementAndGet();
		}

		public void incrementTransportError() {
			this.transportError.incrementAndGet();
		}

		private AtomicInteger getCountFor(WebSocketSession session) {
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
