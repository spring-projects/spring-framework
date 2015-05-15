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

package org.springframework.messaging.simp.stomp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.messaging.tcp.FixedIntervalReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.Reactor2TcpClient;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureTask;

/**
 * A {@link org.springframework.messaging.MessageHandler} that handles messages by
 * forwarding them to a STOMP broker.
 *
 * <p>For each new {@link SimpMessageType#CONNECT CONNECT} message, an independent TCP
 * connection to the broker is opened and used exclusively for all messages from the
 * client that originated the CONNECT message. Messages from the same client are
 * identified through the session id message header. Reversely, when the STOMP broker
 * sends messages back on the TCP connection, those messages are enriched with the session
 * id of the client and sent back downstream through the {@link MessageChannel} provided
 * to the constructor.
 *
 * <p>This class also automatically opens a default "system" TCP connection to the message
 * broker that is used for sending messages that originate from the server application (as
 * opposed to from a client). Such messages are are not associated with any client and
 * therefore do not have a session id header. The "system" connection is effectively
 * shared and cannot be used to receive messages. Several properties are provided to
 * configure the "system" connection including:
 * <ul>
 * <li>{@link #setSystemLogin(String)}</li>
 * <li>{@link #setSystemPasscode(String)}</li>
 * <li>{@link #setSystemHeartbeatSendInterval(long)}</li>
 * <li>{@link #setSystemHeartbeatReceiveInterval(long)}</li>
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author Andy Wilkinson
 * @since 4.0
 */
public class StompBrokerRelayMessageHandler extends AbstractBrokerMessageHandler {

	public static final String SYSTEM_SESSION_ID = "_system_";

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private static final ListenableFutureTask<Void> EMPTY_TASK = new ListenableFutureTask<Void>(new VoidCallable());

	// STOMP recommends error of margin for receiving heartbeats
	private static final long HEARTBEAT_MULTIPLIER = 3;

	private static final Message<byte[]> HEARTBEAT_MESSAGE;


	static {
		EMPTY_TASK.run();
		StompHeaderAccessor accessor = StompHeaderAccessor.createForHeartbeat();
		HEARTBEAT_MESSAGE = MessageBuilder.createMessage(StompDecoder.HEARTBEAT_PAYLOAD, accessor.getMessageHeaders());
	}


	private String relayHost = "127.0.0.1";

	private int relayPort = 61613;

	private String clientLogin = "guest";

	private String clientPasscode = "guest";

	private String systemLogin = "guest";

	private String systemPasscode = "guest";

	private long systemHeartbeatSendInterval = 10000;

	private long systemHeartbeatReceiveInterval = 10000;

	private String virtualHost;

	private final Map<String, MessageHandler> systemSubscriptions = new HashMap<String, MessageHandler>(4);

	private TcpOperations<byte[]> tcpClient;

	private MessageHeaderInitializer headerInitializer;

	private final Map<String, StompConnectionHandler> connectionHandlers =
			new ConcurrentHashMap<String, StompConnectionHandler>();

	private final Stats stats = new Stats();


	/**
	 * Create a StompBrokerRelayMessageHandler instance with the given message channels
	 * and destination prefixes.
	 * @param inboundChannel the channel for receiving messages from clients (e.g. WebSocket clients)
	 * @param outboundChannel the channel for sending messages to clients (e.g. WebSocket clients)
	 * @param brokerChannel the channel for the application to send messages to the broker
	 * @param destinationPrefixes the broker supported destination prefixes; destinations
	 * that do not match the given prefix are ignored.
	 */
	public StompBrokerRelayMessageHandler(SubscribableChannel inboundChannel, MessageChannel outboundChannel,
			SubscribableChannel brokerChannel, Collection<String> destinationPrefixes) {

		super(inboundChannel, outboundChannel, brokerChannel, destinationPrefixes);
	}


	/**
	 * Set the STOMP message broker host.
	 */
	public void setRelayHost(String relayHost) {
		Assert.hasText(relayHost, "relayHost must not be empty");
		this.relayHost = relayHost;
	}

	/**
	 * Return the STOMP message broker host.
	 */
	public String getRelayHost() {
		return this.relayHost;
	}

	/**
	 * Set the STOMP message broker port.
	 */
	public void setRelayPort(int relayPort) {
		this.relayPort = relayPort;
	}

	/**
	 * Return the STOMP message broker port.
	 */
	public int getRelayPort() {
		return this.relayPort;
	}

	/**
	 * Set the interval, in milliseconds, at which the "system" connection will, in the
	 * absence of any other data being sent, send a heartbeat to the STOMP broker. A value
	 * of zero will prevent heartbeats from being sent to the broker.
	 * <p>The default value is 10000.
	 * <p>See class-level documentation for more information on the "system" connection.
	 */
	public void setSystemHeartbeatSendInterval(long systemHeartbeatSendInterval) {
		this.systemHeartbeatSendInterval = systemHeartbeatSendInterval;
	}

	/**
	 * Return the interval, in milliseconds, at which the "system" connection will
	 * send heartbeats to the STOMP broker.
	 */
	public long getSystemHeartbeatSendInterval() {
		return this.systemHeartbeatSendInterval;
	}

	/**
	 * Set the maximum interval, in milliseconds, at which the "system" connection
	 * expects, in the absence of any other data, to receive a heartbeat from the STOMP
	 * broker. A value of zero will configure the connection to expect not to receive
	 * heartbeats from the broker.
	 * <p>The default value is 10000.
	 * <p>See class-level documentation for more information on the "system" connection.
	 */
	public void setSystemHeartbeatReceiveInterval(long heartbeatReceiveInterval) {
		this.systemHeartbeatReceiveInterval = heartbeatReceiveInterval;
	}

	/**
	 * Return the interval, in milliseconds, at which the "system" connection expects
	 * to receive heartbeats from the STOMP broker.
	 */
	public long getSystemHeartbeatReceiveInterval() {
		return this.systemHeartbeatReceiveInterval;
	}

	/**
	 * Set the login to use when creating connections to the STOMP broker on
	 * behalf of connected clients.
	 * <p>By default this is set to "guest".
	 * @see #setSystemLogin(String)
	 */
	public void setClientLogin(String clientLogin) {
		Assert.hasText(clientLogin, "clientLogin must not be empty");
		this.clientLogin = clientLogin;
	}

	/**
	 * Return the configured login to use for connections to the STOMP broker
	 * on behalf of connected clients.
	 * @see #getSystemLogin()
	 */
	public String getClientLogin() {
		return this.clientLogin;
	}

	/**
	 * Set the client passcode to use to create connections to the STOMP broker on
	 * behalf of connected clients.
	 * <p>By default this is set to "guest".
	 * @see #setSystemPasscode
	 */
	public void setClientPasscode(String clientPasscode) {
		Assert.hasText(clientPasscode, "clientPasscode must not be empty");
		this.clientPasscode = clientPasscode;
	}

	/**
	 * Return the configured passcode to use for connections to the STOMP broker on
	 * behalf of connected clients.
	 * @see #getSystemPasscode()
	 */
	public String getClientPasscode() {
		return this.clientPasscode;
	}

	/**
	 * Set the login for the shared "system" connection used to send messages to
	 * the STOMP broker from within the application, i.e. messages not associated
	 * with a specific client session (e.g. REST/HTTP request handling method).
	 * <p>By default this is set to "guest".
	 */
	public void setSystemLogin(String systemLogin) {
		Assert.hasText(systemLogin, "systemLogin must not be empty");
		this.systemLogin = systemLogin;
	}

	/**
	 * Return the login used for the shared "system" connection to the STOMP broker.
	 */
	public String getSystemLogin() {
		return this.systemLogin;
	}

	/**
	 * Set the passcode for the shared "system" connection used to send messages to
	 * the STOMP broker from within the application, i.e. messages not associated
	 * with a specific client session (e.g. REST/HTTP request handling method).
	 * <p>By default this is set to "guest".
	 */
	public void setSystemPasscode(String systemPasscode) {
		this.systemPasscode = systemPasscode;
	}

	/**
	 * Return the passcode used for the shared "system" connection to the STOMP broker.
	 */
	public String getSystemPasscode() {
		return this.systemPasscode;
	}

	/**
	 * Configure one more destinations to subscribe to on the shared "system"
	 * connection along with MessageHandler's to handle received messages.
	 * <p>This is for internal use in a multi-application server scenario where
	 * servers forward messages to each other (e.g. unresolved user destinations).
	 * @param subscriptions the destinations to subscribe to.
	 */
	public void setSystemSubscriptions(Map<String, MessageHandler> subscriptions) {
		this.systemSubscriptions.clear();
		if (subscriptions != null) {
			this.systemSubscriptions.putAll(subscriptions);
		}
	}

	/**
	 * Return the configured map with subscriptions on the "system" connection.
	 */
	public Map<String, MessageHandler> getSystemSubscriptions() {
		return this.systemSubscriptions;
	}

	/**
	 * Set the value of the "host" header to use in STOMP CONNECT frames. When this
	 * property is configured, a "host" header will be added to every STOMP frame sent to
	 * the STOMP broker. This may be useful for example in a cloud environment where the
	 * actual host to which the TCP connection is established is different from the host
	 * providing the cloud-based STOMP service.
	 * <p>By default this property is not set.
	 */
	public void setVirtualHost(String virtualHost) {
		this.virtualHost = virtualHost;
	}

	/**
	 * Return the configured virtual host value.
	 */
	public String getVirtualHost() {
		return this.virtualHost;
	}

	/**
	 * Configure a TCP client for managing TCP connections to the STOMP broker.
	 * By default {@link Reactor2TcpClient} is used.
	 */
	public void setTcpClient(TcpOperations<byte[]> tcpClient) {
		this.tcpClient = tcpClient;
	}

	/**
	 * Get the configured TCP client. Never {@code null} unless not configured
	 * invoked and this method is invoked before the handler is started and
	 * hence a default implementation initialized.
	 */
	public TcpOperations<byte[]> getTcpClient() {
		return this.tcpClient;
	}

	/**
	 * Return the current count of TCP connection to the broker.
	 */
	public int getConnectionCount() {
		return this.connectionHandlers.size();
	}

	/**
	 * Configure a {@link MessageHeaderInitializer} to apply to the headers of all
	 * messages created through the {@code StompBrokerRelayMessageHandler} that
	 * are sent to the client outbound message channel.
	 * <p>By default this property is not set.
	 */
	public void setHeaderInitializer(MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * Return the configured header initializer.
	 */
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}

	/**
	 * Return a String describing internal state and counters.
	 */
	public String getStatsInfo() {
		return this.stats.toString();
	}


	@Override
	protected void startInternal() {
		if (this.tcpClient == null) {
			StompDecoder decoder = new StompDecoder();
			decoder.setHeaderInitializer(getHeaderInitializer());
			Reactor2StompCodec codec = new Reactor2StompCodec(new StompEncoder(), decoder);
			this.tcpClient = new StompTcpClientFactory().create(this.relayHost, this.relayPort, codec);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Connecting \"system\" session to " + this.relayHost + ":" + this.relayPort);
		}

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setAcceptVersion("1.1,1.2");
		accessor.setLogin(this.systemLogin);
		accessor.setPasscode(this.systemPasscode);
		accessor.setHeartbeat(this.systemHeartbeatSendInterval, this.systemHeartbeatReceiveInterval);
		accessor.setHost(getVirtualHost());
		accessor.setSessionId(SYSTEM_SESSION_ID);
		if (logger.isDebugEnabled()) {
			logger.debug("Forwarding " + accessor.getShortLogMessage(EMPTY_PAYLOAD));
		}

		SystemStompConnectionHandler handler = new SystemStompConnectionHandler(accessor);
		this.connectionHandlers.put(handler.getSessionId(), handler);

		this.stats.incrementConnectCount();
		this.tcpClient.connect(handler, new FixedIntervalReconnectStrategy(5000));
	}

	@Override
	protected void stopInternal() {
		publishBrokerUnavailableEvent();
		try {
			this.tcpClient.shutdown().get(5000, TimeUnit.MILLISECONDS);
		}
		catch (Throwable ex) {
			logger.error("Error in shutdown of TCP client", ex);
		}
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());

		if (!isBrokerAvailable()) {
			if (sessionId == null || SYSTEM_SESSION_ID.equals(sessionId)) {
				throw new MessageDeliveryException("Message broker not active. Consider subscribing to " +
						"receive BrokerAvailabilityEvent's from an ApplicationListener Spring bean.");
			}
			StompConnectionHandler handler = this.connectionHandlers.get(sessionId);
			if (handler != null) {
				handler.sendStompErrorFrameToClient("Broker not available.");
				handler.clearConnection();
			}
			else {
				StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
				if (getHeaderInitializer() != null) {
					getHeaderInitializer().initHeaders(accessor);
				}
				accessor.setSessionId(sessionId);
				accessor.setUser(SimpMessageHeaderAccessor.getUser(message.getHeaders()));
				accessor.setMessage("Broker not available.");
				MessageHeaders headers = accessor.getMessageHeaders();
				getClientOutboundChannel().send(MessageBuilder.createMessage(EMPTY_PAYLOAD, headers));
			}
			return;
		}

		StompHeaderAccessor stompAccessor;
		StompCommand command;

		MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
		if (accessor == null) {
			throw new IllegalStateException(
					"No header accessor (not using the SimpMessagingTemplate?): " + message);
		}
		else if (accessor instanceof StompHeaderAccessor) {
			stompAccessor = (StompHeaderAccessor) accessor;
			command = stompAccessor.getCommand();
		}
		else if (accessor instanceof SimpMessageHeaderAccessor) {
			stompAccessor = StompHeaderAccessor.wrap(message);
			command = stompAccessor.getCommand();
			if (command == null) {
				command = stompAccessor.updateStompCommandAsClientMessage();
			}
		}
		else {
			throw new IllegalStateException(
					"Unexpected header accessor type " + accessor.getClass() + " in " + message);
		}

		if (sessionId == null) {
			if (!SimpMessageType.MESSAGE.equals(stompAccessor.getMessageType())) {
				if (logger.isErrorEnabled()) {
					logger.error("Only STOMP SEND supported from within the server side. Ignoring " + message);
				}
				return;
			}
			sessionId = SYSTEM_SESSION_ID;
			stompAccessor.setSessionId(sessionId);
		}

		String destination = stompAccessor.getDestination();
		if (command != null && command.requiresDestination() && !checkDestinationPrefix(destination)) {
			return;
		}

		if (StompCommand.CONNECT.equals(command)) {
			if (logger.isDebugEnabled()) {
				logger.debug(stompAccessor.getShortLogMessage(EMPTY_PAYLOAD));
			}
			stompAccessor = (stompAccessor.isMutable() ? stompAccessor : StompHeaderAccessor.wrap(message));
			stompAccessor.setLogin(this.clientLogin);
			stompAccessor.setPasscode(this.clientPasscode);
			if (getVirtualHost() != null) {
				stompAccessor.setHost(getVirtualHost());
			}
			StompConnectionHandler handler = new StompConnectionHandler(sessionId, stompAccessor);
			this.connectionHandlers.put(sessionId, handler);
			this.stats.incrementConnectCount();
			this.tcpClient.connect(handler);
		}
		else if (StompCommand.DISCONNECT.equals(command)) {
			StompConnectionHandler handler = this.connectionHandlers.get(sessionId);
			if (handler == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring DISCONNECT in session " + sessionId + ". Connection already cleaned up.");
				}
				return;
			}
			stats.incrementDisconnectCount();
			handler.forward(message, stompAccessor);
		}
		else {
			StompConnectionHandler handler = this.connectionHandlers.get(sessionId);
			if (handler == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("No TCP connection for session " + sessionId + " in " + message);
				}
				return;
			}
			handler.forward(message, stompAccessor);
		}
	}

	@Override
	public String toString() {
		return "StompBrokerRelay[" + this.relayHost + ":" + this.relayPort + "]";
	}


	private class StompConnectionHandler implements TcpConnectionHandler<byte[]> {

		private final String sessionId;

		private final boolean isRemoteClientSession;

		private final StompHeaderAccessor connectHeaders;

		private volatile TcpConnection<byte[]> tcpConnection;

		private volatile boolean isStompConnected;


		private StompConnectionHandler(String sessionId, StompHeaderAccessor connectHeaders) {
			this(sessionId, connectHeaders, true);
		}

		private StompConnectionHandler(String sessionId, StompHeaderAccessor connectHeaders, boolean isClientSession) {
			Assert.notNull(sessionId, "'sessionId' must not be null");
			Assert.notNull(connectHeaders, "'connectHeaders' must not be null");
			this.sessionId = sessionId;
			this.connectHeaders = connectHeaders;
			this.isRemoteClientSession = isClientSession;
		}

		public String getSessionId() {
			return this.sessionId;
		}

		protected TcpConnection<byte[]> getTcpConnection() {
			return this.tcpConnection;
		}

		@Override
		public void afterConnected(TcpConnection<byte[]> connection) {
			if (logger.isDebugEnabled()) {
				logger.debug("TCP connection opened in session=" + getSessionId());
			}
			this.tcpConnection = connection;
			connection.send(MessageBuilder.createMessage(EMPTY_PAYLOAD, this.connectHeaders.getMessageHeaders()));
		}

		@Override
		public void afterConnectFailure(Throwable ex) {
			handleTcpConnectionFailure("Failed to connect: " + ex.getMessage(), ex);
		}

		/**
		 * Invoked when any TCP connectivity issue is detected, i.e. failure to establish
		 * the TCP connection, failure to send a message, missed heartbeat, etc.
		 */
		protected void handleTcpConnectionFailure(String error, Throwable ex) {
			if (logger.isErrorEnabled()) {
				logger.error("TCP connection failure in session " + this.sessionId + ": " + error, ex);
			}
			try {
				sendStompErrorFrameToClient(error);
			}
			finally {
				try {
					clearConnection();
				}
				catch (Throwable ex2) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failure while clearing TCP connection state in session " + this.sessionId, ex2);
					}
				}
			}
		}

		private void sendStompErrorFrameToClient(String errorText) {
			if (this.isRemoteClientSession) {
				StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
				if (getHeaderInitializer() != null) {
					getHeaderInitializer().initHeaders(headerAccessor);
				}
				headerAccessor.setSessionId(this.sessionId);
				headerAccessor.setUser(this.connectHeaders.getUser());
				headerAccessor.setMessage(errorText);
				Message<?> errorMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, headerAccessor.getMessageHeaders());
				handleInboundMessage(errorMessage);
			}
		}

		protected void handleInboundMessage(Message<?> message) {
			if (this.isRemoteClientSession) {
				StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
				accessor.setImmutable();
				StompBrokerRelayMessageHandler.this.getClientOutboundChannel().send(message);
			}
		}

		@Override
		public void handleMessage(Message<byte[]> message) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			accessor.setSessionId(this.sessionId);
			accessor.setUser(this.connectHeaders.getUser());

			StompCommand command = accessor.getCommand();
			if (StompCommand.CONNECTED.equals(command)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Received " + accessor.getShortLogMessage(EMPTY_PAYLOAD));
				}
				afterStompConnected(accessor);
			}
			else if (logger.isErrorEnabled() && StompCommand.ERROR.equals(command)) {
				logger.error("Received " + accessor.getShortLogMessage(message.getPayload()));
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("Received " + accessor.getDetailedLogMessage(message.getPayload()));
			}

			handleInboundMessage(message);
		}

		/**
		 * Invoked after the STOMP CONNECTED frame is received. At this point the
		 * connection is ready for sending STOMP messages to the broker.
		 */
		protected void afterStompConnected(StompHeaderAccessor connectedHeaders) {
			this.isStompConnected = true;
			stats.incrementConnectedCount();
			initHeartbeats(connectedHeaders);
		}

		private void initHeartbeats(StompHeaderAccessor connectedHeaders) {
			if (this.isRemoteClientSession) {
				return;
			}

			long clientSendInterval = this.connectHeaders.getHeartbeat()[0];
			long clientReceiveInterval = this.connectHeaders.getHeartbeat()[1];
			long serverSendInterval = connectedHeaders.getHeartbeat()[0];
			long serverReceiveInterval = connectedHeaders.getHeartbeat()[1];

			if (clientSendInterval > 0 && serverReceiveInterval > 0) {
				long interval = Math.max(clientSendInterval,  serverReceiveInterval);
				this.tcpConnection.onWriteInactivity(new Runnable() {
					@Override
					public void run() {
						TcpConnection<byte[]> conn = tcpConnection;
						if (conn != null) {
							conn.send(HEARTBEAT_MESSAGE).addCallback(
									new ListenableFutureCallback<Void>() {
										public void onSuccess(Void result) {
										}
										public void onFailure(Throwable ex) {
											handleTcpConnectionFailure(
													"Failed to forward heartbeat: " + ex.getMessage(), ex);
										}
									});
						}
					}
				}, interval);
			}
			if (clientReceiveInterval > 0 && serverSendInterval > 0) {
				final long interval = Math.max(clientReceiveInterval, serverSendInterval) * HEARTBEAT_MULTIPLIER;
				this.tcpConnection.onReadInactivity(new Runnable() {
					@Override
					public void run() {
						handleTcpConnectionFailure("No messages received in " + interval + " ms.", null);
					}
				}, interval);
			}
		}

		@Override
		public void handleFailure(Throwable ex) {
			if (this.tcpConnection != null) {
				handleTcpConnectionFailure("Transport failure: " + ex.getMessage(), ex);
			}
			else if (logger.isErrorEnabled()) {
				logger.error("Transport failure: " + ex);
			}
		}

		@Override
		public void afterConnectionClosed() {
			if (this.tcpConnection == null) {
				return;
			}
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("TCP connection to broker closed in session " + this.sessionId);
				}
				sendStompErrorFrameToClient("Connection to broker closed.");
			}
			finally {
				try {
					// Prevent clearConnection() from trying to close
					this.tcpConnection = null;
					clearConnection();
				}
				catch (Throwable ex) {
					// Shouldn't happen with connection reset beforehand
				}
			}
		}

		/**
		 * Forward the given message to the STOMP broker.
		 * <p>The method checks whether we have an active TCP connection and have
		 * received the STOMP CONNECTED frame. For client messages this should be
		 * false only if we lose the TCP connection around the same time when a
		 * client message is being forwarded, so we simply log the ignored message
		 * at debug level. For messages from within the application being sent on
		 * the "system" connection an exception is raised so that components sending
		 * the message have a chance to handle it -- by default the broker message
		 * channel is synchronous.
		 * <p>Note that if messages arrive concurrently around the same time a TCP
		 * connection is lost, there is a brief period of time before the connection
		 * is reset when one or more messages may sneak through and an attempt made
		 * to forward them. Rather than synchronizing to guard against that, this
		 * method simply lets them try and fail. For client sessions that may
		 * result in an additional STOMP ERROR frame(s) being sent downstream but
		 * code handling that downstream should be idempotent in such cases.
		 * @param message the message to send (never {@code null})
		 * @return a future to wait for the result
		 */
		@SuppressWarnings("unchecked")
		public ListenableFuture<Void> forward(final Message<?> message, final StompHeaderAccessor accessor) {
			TcpConnection<byte[]> conn = this.tcpConnection;

			if (!this.isStompConnected) {
				if (this.isRemoteClientSession) {
					if (logger.isDebugEnabled()) {
						logger.debug("TCP connection closed already, ignoring " +
								accessor.getShortLogMessage(message.getPayload()));
					}
					return EMPTY_TASK;
				}
				else {
					throw new IllegalStateException("Cannot forward messages " +
							(conn != null ? "before STOMP CONNECTED. " : "while inactive. ") +
							"Consider subscribing to receive BrokerAvailabilityEvent's from " +
							"an ApplicationListener Spring bean. Dropped " +
							accessor.getShortLogMessage(message.getPayload()));
				}
			}

			final Message<?> messageToSend = (accessor.isMutable() && accessor.isModified()) ?
					MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders()) : message;

			StompCommand command = accessor.getCommand();
			if (logger.isDebugEnabled() && (StompCommand.SEND.equals(command) || StompCommand.SUBSCRIBE.equals(command) ||
					StompCommand.UNSUBSCRIBE.equals(command) || StompCommand.DISCONNECT.equals(command))) {
				logger.debug("Forwarding " + accessor.getShortLogMessage(message.getPayload()));
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("Forwarding " + accessor.getDetailedLogMessage(message.getPayload()));
			}

			ListenableFuture<Void> future = conn.send((Message<byte[]>) messageToSend);
			future.addCallback(new ListenableFutureCallback<Void>() {
				@Override
				public void onSuccess(Void result) {
					if (accessor.getCommand() == StompCommand.DISCONNECT) {
						afterDisconnectSent(accessor);
					}
				}
				@Override
				public void onFailure(Throwable ex) {
					if (tcpConnection != null) {
						handleTcpConnectionFailure("failed to forward " +
								accessor.getShortLogMessage(message.getPayload()), ex);
					}
					else if (logger.isErrorEnabled()) {
						logger.error("Failed to forward " + accessor.getShortLogMessage(message.getPayload()));
					}
				}
			});
			return future;
		}

		/**
		 * After a DISCONNECT there should be no more client frames so we can
		 * close the connection pro-actively. However, if the DISCONNECT has a
		 * receipt header we leave the connection open and expect the server will
		 * respond with a RECEIPT and then close the connection.
		 * @see <a href="http://stomp.github.io/stomp-specification-1.2.html#DISCONNECT">
		 *     STOMP Specification 1.2 DISCONNECT</a>
		 */
		private void afterDisconnectSent(StompHeaderAccessor accessor) {
			if (accessor.getReceipt() == null) {
				try {
					clearConnection();
				}
				catch (Throwable ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failure while clearing TCP connection state in session " + this.sessionId, ex);
					}
				}
			}
		}

		/**
		 * Clean up state associated with the connection and close it.
		 * Any exception arising from closing the connection are propagated.
		 */
		public void clearConnection() {
			if (logger.isDebugEnabled()) {
				logger.debug("Cleaning up connection state for session " + this.sessionId);
			}

			if (this.isRemoteClientSession) {
				StompBrokerRelayMessageHandler.this.connectionHandlers.remove(this.sessionId);
			}

			this.isStompConnected = false;

			TcpConnection<byte[]> conn = this.tcpConnection;
			this.tcpConnection = null;
			if (conn != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Closing TCP connection in session " + this.sessionId);
				}
				conn.close();
			}
		}

		@Override
		public String toString() {
			return "StompConnectionHandler[sessionId=" + this.sessionId + "]";
		}
	}

	private class SystemStompConnectionHandler extends StompConnectionHandler {

		public SystemStompConnectionHandler(StompHeaderAccessor connectHeaders) {
			super(SYSTEM_SESSION_ID, connectHeaders, false);
		}

		@Override
		protected void afterStompConnected(StompHeaderAccessor connectedHeaders) {
			if (logger.isInfoEnabled()) {
				logger.info("\"System\" session connected.");
			}
			super.afterStompConnected(connectedHeaders);
			publishBrokerAvailableEvent();
			sendSystemSubscriptions();
		}

		private void sendSystemSubscriptions() {
			int i = 0;
			for (String destination : getSystemSubscriptions().keySet()) {
				StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
				accessor.setSubscriptionId(String.valueOf(i++));
				accessor.setDestination(destination);
				if (logger.isDebugEnabled()) {
					logger.debug("Subscribing to " + destination + " on \"system\" connection.");
				}
				TcpConnection<byte[]> conn = getTcpConnection();
				if (conn != null) {
					MessageHeaders headers = accessor.getMessageHeaders();
					conn.send(MessageBuilder.createMessage(EMPTY_PAYLOAD, headers)).addCallback(
							new ListenableFutureCallback<Void>() {
								public void onSuccess(Void result) {
								}
								public void onFailure(Throwable ex) {
									String error = "Failed to subscribe in \"system\" session.";
									handleTcpConnectionFailure(error, ex);
								}
							});
				}
			}
		}

		@Override
		protected void handleInboundMessage(Message<?> message) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			if (StompCommand.MESSAGE.equals(accessor.getCommand())) {
				String destination = accessor.getDestination();
				if (destination == null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Got message on \"system\" connection, with no destination: " +
								accessor.getDetailedLogMessage(message.getPayload()));
					}
					return;
				}
				if (!getSystemSubscriptions().containsKey(destination)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Got message on \"system\" connection with no handler: " +
								accessor.getDetailedLogMessage(message.getPayload()));
					}
					return;
				}
				try {
					MessageHandler handler = getSystemSubscriptions().get(destination);
					handler.handleMessage(message);
				}
				catch (Throwable ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Error while handling message on \"system\" connection.", ex);
					}
				}
			}
		}

		@Override
		protected void handleTcpConnectionFailure(String errorMessage, Throwable ex) {
			super.handleTcpConnectionFailure(errorMessage, ex);
			publishBrokerUnavailableEvent();
		}

		@Override
		public void afterConnectionClosed() {
			super.afterConnectionClosed();
			publishBrokerUnavailableEvent();
		}

		@Override
		public ListenableFuture<Void> forward(Message<?> message, StompHeaderAccessor accessor) {
			try {
				ListenableFuture<Void> future = super.forward(message, accessor);
				if (message.getHeaders().get(SimpMessageHeaderAccessor.IGNORE_ERROR) == null) {
					future.get();
				}
				return future;
			}
			catch (Throwable ex) {
				throw new MessageDeliveryException(message, ex);
			}
		}
	}


	private static class StompTcpClientFactory {

		public TcpOperations<byte[]> create(String relayHost, int relayPort, Reactor2StompCodec codec) {
			return new Reactor2TcpClient<byte[]>(relayHost, relayPort, codec);
		}
	}


	private static class VoidCallable implements Callable<Void> {

		@Override
		public Void call() throws Exception {
			return null;
		}
	}


	private class Stats {

		private final AtomicInteger connect = new AtomicInteger();

		private final AtomicInteger connected = new AtomicInteger();

		private final AtomicInteger disconnect = new AtomicInteger();

		public void incrementConnectCount() {
			this.connect.incrementAndGet();
		}

		public void incrementConnectedCount() {
			this.connected.incrementAndGet();
		}

		public void incrementDisconnectCount() {
			this.disconnect.incrementAndGet();
		}

		public String toString() {
			return connectionHandlers.size() + " sessions, " + relayHost + ":" + relayPort +
					(isBrokerAvailable() ? " (available)" : " (not available)") +
					", processed CONNECT(" + this.connect.get() + ")-CONNECTED(" +
					this.connected.get() + ")-DISCONNECT(" + this.disconnect.get() + ")";
		}
	}

}
