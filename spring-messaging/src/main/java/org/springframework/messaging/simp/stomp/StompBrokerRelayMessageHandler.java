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

package org.springframework.messaging.simp.stomp;

import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.messaging.tcp.FixedIntervalReconnectStrategy;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.ReactorNettyCodec;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.scheduling.TaskScheduler;
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
 * sends messages back on the TCP connection, those messages are enriched with the
 * session id of the client and sent back downstream through the {@link MessageChannel}
 * provided to the constructor.
 *
 * <p>This class also automatically opens a default "system" TCP connection to the
 * message broker that is used for sending messages that originate from the server
 * application (as opposed to from a client). Such messages are not associated with
 * any client and therefore do not have a session id header. The "system" connection
 * is effectively shared and cannot be used to receive messages. Several properties
 * are provided to configure the "system" connection including:
 * <ul>
 * <li>{@link #setSystemLogin}</li>
 * <li>{@link #setSystemPasscode}</li>
 * <li>{@link #setSystemHeartbeatSendInterval}</li>
 * <li>{@link #setSystemHeartbeatReceiveInterval}</li>
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author Andy Wilkinson
 * @since 4.0
 */
public class StompBrokerRelayMessageHandler extends AbstractBrokerMessageHandler {

	/**
	 * The system session ID.
	 */
	public static final String SYSTEM_SESSION_ID = "_system_";

	/** STOMP recommended error of margin for receiving heartbeats. */
	private static final long HEARTBEAT_MULTIPLIER = 3;

	/**
	 * Heartbeat starts once CONNECTED frame with heartbeat settings is received.
	 * If CONNECTED doesn't arrive within a minute, we'll close the connection.
	 */
	private static final int MAX_TIME_TO_CONNECTED_FRAME = 60 * 1000;

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private static final ListenableFutureTask<Void> EMPTY_TASK = new ListenableFutureTask<>(new VoidCallable());

	private static final StompHeaderAccessor HEART_BEAT_ACCESSOR;

	private static final Message<byte[]> HEARTBEAT_MESSAGE;

	static {
		EMPTY_TASK.run();
		HEART_BEAT_ACCESSOR = StompHeaderAccessor.createForHeartbeat();
		HEARTBEAT_MESSAGE = MessageBuilder.createMessage(
				StompDecoder.HEARTBEAT_PAYLOAD, HEART_BEAT_ACCESSOR.getMessageHeaders());
	}


	private String relayHost = "127.0.0.1";

	private int relayPort = 61613;

	private String clientLogin = "guest";

	private String clientPasscode = "guest";

	private String systemLogin = "guest";

	private String systemPasscode = "guest";

	private long systemHeartbeatSendInterval = 10000;

	private long systemHeartbeatReceiveInterval = 10000;

	private final Map<String, MessageHandler> systemSubscriptions = new HashMap<>(4);

	@Nullable
	private String virtualHost;

	@Nullable
	private TcpOperations<byte[]> tcpClient;

	@Nullable
	private MessageHeaderInitializer headerInitializer;

	private final DefaultStats stats = new DefaultStats();

	private final Map<String, RelayConnectionHandler> connectionHandlers = new ConcurrentHashMap<>();

	@Nullable
	private TaskScheduler taskScheduler;


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
	 * Configure one more destinations to subscribe to on the shared "system"
	 * connection along with MessageHandler's to handle received messages.
	 * <p>This is for internal use in a multi-application server scenario where
	 * servers forward messages to each other (e.g. unresolved user destinations).
	 * @param subscriptions the destinations to subscribe to.
	 */
	public void setSystemSubscriptions(@Nullable Map<String, MessageHandler> subscriptions) {
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
	public void setVirtualHost(@Nullable String virtualHost) {
		this.virtualHost = virtualHost;
	}

	/**
	 * Return the configured virtual host value.
	 */
	@Nullable
	public String getVirtualHost() {
		return this.virtualHost;
	}

	/**
	 * Configure a TCP client for managing TCP connections to the STOMP broker.
	 * <p>By default {@link ReactorNettyTcpClient} is used.
	 * <p><strong>Note:</strong> when this property is used, any
	 * {@link #setRelayHost(String) host} or {@link #setRelayPort(int) port}
	 * specified are effectively ignored.
	 */
	public void setTcpClient(@Nullable TcpOperations<byte[]> tcpClient) {
		this.tcpClient = tcpClient;
	}

	/**
	 * Get the configured TCP client (never {@code null} unless not configured
	 * invoked and this method is invoked before the handler is started and
	 * hence a default implementation initialized).
	 */
	@Nullable
	public TcpOperations<byte[]> getTcpClient() {
		return this.tcpClient;
	}

	/**
	 * Configure a {@link MessageHeaderInitializer} to apply to the headers of all
	 * messages created through the {@code StompBrokerRelayMessageHandler} that
	 * are sent to the client outbound message channel.
	 * <p>By default this property is not set.
	 */
	public void setHeaderInitializer(@Nullable MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * Return the configured header initializer.
	 */
	@Nullable
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}

	/**
	 * Return a String describing internal state and counters.
	 * Effectively {@code toString()} on {@link #getStats() getStats()}.
	 */
	public String getStatsInfo() {
		return this.stats.toString();
	}

	/**
	 * Return a structured object with internal state and counters.
	 * @since 5.2
	 */
	public Stats getStats() {
		return this.stats;
	}


	/**
	 * Return the current count of TCP connection to the broker.
	 */
	public int getConnectionCount() {
		return this.connectionHandlers.size();
	}

	/**
	 * Configure the {@link TaskScheduler} to use to reset client-to-broker
	 * message count in the current heartbeat period. For more details, see
	 * {@link org.springframework.messaging.simp.config.StompBrokerRelayRegistration#setTaskScheduler(TaskScheduler)}.
	 * @param taskScheduler the scheduler to use
	 * @since 5.3
	 */
	public void setTaskScheduler(@Nullable TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	@Nullable
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}


	@Override
	protected void startInternal() {
		if (this.tcpClient == null) {
			this.tcpClient = initTcpClient();
		}

		if (logger.isInfoEnabled()) {
			logger.info("Starting \"system\" session, " + toString());
		}

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setAcceptVersion("1.1,1.2");
		accessor.setLogin(this.systemLogin);
		accessor.setPasscode(this.systemPasscode);
		accessor.setHeartbeat(this.systemHeartbeatSendInterval, this.systemHeartbeatReceiveInterval);
		String virtualHost = getVirtualHost();
		if (virtualHost != null) {
			accessor.setHost(virtualHost);
		}
		accessor.setSessionId(SYSTEM_SESSION_ID);
		if (logger.isDebugEnabled()) {
			logger.debug("Forwarding " + accessor.getShortLogMessage(EMPTY_PAYLOAD));
		}

		SystemSessionConnectionHandler handler = new SystemSessionConnectionHandler(accessor);
		this.connectionHandlers.put(handler.getSessionId(), handler);

		this.stats.incrementConnectCount();
		this.tcpClient.connect(handler, new FixedIntervalReconnectStrategy(5000));

		if (this.taskScheduler != null) {
			this.taskScheduler.scheduleWithFixedDelay(new ClientSendMessageCountTask(), 5000);
		}
	}

	private ReactorNettyTcpClient<byte[]> initTcpClient() {
		StompDecoder decoder = new StompDecoder();
		if (this.headerInitializer != null) {
			decoder.setHeaderInitializer(this.headerInitializer);
		}
		ReactorNettyCodec<byte[]> codec = new StompReactorNettyCodec(decoder);
		ReactorNettyTcpClient<byte[]> client = new ReactorNettyTcpClient<>(this.relayHost, this.relayPort, codec);
		client.setLogger(SimpLogging.forLog(client.getLogger()));
		return client;
	}

	@Override
	protected void stopInternal() {
		publishBrokerUnavailableEvent();
		if (this.tcpClient != null) {
			try {
				this.tcpClient.shutdown().get(5000, TimeUnit.MILLISECONDS);
			}
			catch (Throwable ex) {
				logger.error("Error in shutdown of TCP client", ex);
			}
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
			RelayConnectionHandler handler = this.connectionHandlers.get(sessionId);
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
				Principal user = SimpMessageHeaderAccessor.getUser(message.getHeaders());
				if (user != null) {
					accessor.setUser(user);
				}
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

		if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
			if (this.connectionHandlers.get(sessionId) != null) {
				if (logger.isWarnEnabled()) {
					logger.warn("Ignoring CONNECT in session " + sessionId + ". Already connected.");
				}
				return;
			}
			if (logger.isDebugEnabled()) {
				logger.debug(stompAccessor.getShortLogMessage(EMPTY_PAYLOAD));
			}
			stompAccessor = (stompAccessor.isMutable() ? stompAccessor : StompHeaderAccessor.wrap(message));
			stompAccessor.setLogin(this.clientLogin);
			stompAccessor.setPasscode(this.clientPasscode);
			if (getVirtualHost() != null) {
				stompAccessor.setHost(getVirtualHost());
			}
			RelayConnectionHandler handler = new RelayConnectionHandler(sessionId, stompAccessor);
			this.connectionHandlers.put(sessionId, handler);
			this.stats.incrementConnectCount();
			Assert.state(this.tcpClient != null, "No TCP client available");
			this.tcpClient.connect(handler);
		}
		else if (StompCommand.DISCONNECT.equals(command)) {
			RelayConnectionHandler handler = this.connectionHandlers.get(sessionId);
			if (handler == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring DISCONNECT in session " + sessionId + ". Connection already cleaned up.");
				}
				return;
			}
			this.stats.incrementDisconnectCount();
			handler.forward(message, stompAccessor);
		}
		else {
			RelayConnectionHandler handler = this.connectionHandlers.get(sessionId);
			if (handler == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("No TCP connection for session " + sessionId + " in " + message);
				}
				return;
			}

			String destination = stompAccessor.getDestination();
			if (command != null && command.requiresDestination() && !checkDestinationPrefix(destination)) {
				// Not a broker destination but send a heartbeat to keep the connection
				if (handler.shouldSendHeartbeatForIgnoredMessage()) {
					handler.forward(HEARTBEAT_MESSAGE, HEART_BEAT_ACCESSOR);
				}
				return;
			}

			handler.forward(message, stompAccessor);
		}
	}

	@Override
	public String toString() {
		return "StompBrokerRelay[" + getTcpClientInfo() + "]";
	}

	private String getTcpClientInfo() {
		return this.tcpClient != null ? this.tcpClient.toString() : this.relayHost + ":" + this.relayPort;
	}


	private class RelayConnectionHandler implements StompTcpConnectionHandler<byte[]> {

		private final String sessionId;

		private final StompHeaderAccessor connectHeaders;

		private final boolean isRemoteClientSession;

		private final MessageChannel outboundChannel;

		@Nullable
		private volatile TcpConnection<byte[]> tcpConnection;

		private volatile boolean isStompConnected;

		private long clientSendInterval;

		@Nullable
		private final AtomicInteger clientSendMessageCount;

		private long clientSendMessageTimestamp;


		protected RelayConnectionHandler(String sessionId, StompHeaderAccessor connectHeaders) {
			this(sessionId, connectHeaders, true);
		}

		private RelayConnectionHandler(String sessionId, StompHeaderAccessor connectHeaders, boolean isClientSession) {
			Assert.notNull(sessionId, "'sessionId' must not be null");
			Assert.notNull(connectHeaders, "'connectHeaders' must not be null");
			this.sessionId = sessionId;
			this.connectHeaders = connectHeaders;
			this.isRemoteClientSession = isClientSession;
			this.outboundChannel = getClientOutboundChannelForSession(sessionId);
			if (isClientSession && taskScheduler != null) {
				this.clientSendInterval = connectHeaders.getHeartbeat()[0];
			}
			if (this.clientSendInterval > 0) {
				this.clientSendMessageCount = new AtomicInteger();
				this.clientSendMessageTimestamp = System.currentTimeMillis();
			}
			else {
				this.clientSendMessageCount = null;
			}
		}


		public String getSessionId() {
			return this.sessionId;
		}

		@Override
		public StompHeaderAccessor getConnectHeaders() {
			return this.connectHeaders;
		}

		@Nullable
		protected TcpConnection<byte[]> getTcpConnection() {
			return this.tcpConnection;
		}

		@Override
		public void afterConnected(TcpConnection<byte[]> connection) {
			if (logger.isDebugEnabled()) {
				logger.debug("TCP connection opened in session=" + getSessionId());
			}
			this.tcpConnection = connection;
			connection.onReadInactivity(() -> {
				if (this.tcpConnection != null && !this.isStompConnected) {
					handleTcpConnectionFailure("No CONNECTED frame received in " +
							MAX_TIME_TO_CONNECTED_FRAME + " ms.", null);
				}
			}, MAX_TIME_TO_CONNECTED_FRAME);
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
		protected void handleTcpConnectionFailure(String error, @Nullable Throwable ex) {
			if (logger.isInfoEnabled()) {
				logger.info("TCP connection failure in session " + this.sessionId + ": " + error, ex);
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
				StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
				if (getHeaderInitializer() != null) {
					getHeaderInitializer().initHeaders(accessor);
				}
				accessor.setSessionId(this.sessionId);
				Principal user = this.connectHeaders.getUser();
				if (user != null) {
					accessor.setUser(user);
				}
				accessor.setMessage(errorText);
				accessor.setLeaveMutable(true);
				Message<?> errorMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());
				handleInboundMessage(errorMessage);
			}
		}

		protected void handleInboundMessage(Message<?> message) {
			if (this.isRemoteClientSession) {
				this.outboundChannel.send(message);
			}
		}

		@Override
		public void handleMessage(Message<byte[]> message) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			Assert.state(accessor != null, "No StompHeaderAccessor");
			accessor.setSessionId(this.sessionId);
			Principal user = this.connectHeaders.getUser();
			if (user != null) {
				accessor.setUser(user);
			}

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

		protected void initHeartbeats(StompHeaderAccessor connectedHeaders) {
			if (taskScheduler != null) {
				long interval = connectedHeaders.getHeartbeat()[1];
				this.clientSendInterval = Math.max(interval, this.clientSendInterval);
			}
		}

		/**
		 * Whether to forward a heartbeat message in lieu of a message with a non-broker
		 * destination. This is done if client-side heartbeats are expected and if there
		 * haven't been any other messages in the current heartbeat period.
		 * @since 5.3
		 */
		protected boolean shouldSendHeartbeatForIgnoredMessage() {
			return (this.clientSendMessageCount != null && this.clientSendMessageCount.get() == 0);
		}

		/**
		 * Reset the clientSendMessageCount if the current heartbeat period has expired.
		 * @since 5.3
		 */
		void updateClientSendMessageCount(long now) {
			if (this.clientSendMessageCount != null && this.clientSendInterval > (now - this.clientSendMessageTimestamp)) {
				this.clientSendMessageCount.set(0);
				this.clientSendMessageTimestamp = now;
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

			if (!this.isStompConnected || conn == null) {
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

			if (this.clientSendMessageCount != null) {
				this.clientSendMessageCount.incrementAndGet();
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
				public void onSuccess(@Nullable Void result) {
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
		 * close the connection proactively. However, if the DISCONNECT has a
		 * receipt header we leave the connection open and expect the server will
		 * respond with a RECEIPT and then close the connection.
		 * @see <a href="https://stomp.github.io/stomp-specification-1.2.html#DISCONNECT">
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


	private class SystemSessionConnectionHandler extends RelayConnectionHandler {

		public SystemSessionConnectionHandler(StompHeaderAccessor connectHeaders) {
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

		protected void initHeartbeats(StompHeaderAccessor connectedHeaders) {
			TcpConnection<byte[]> con = getTcpConnection();
			Assert.state(con != null, "No TcpConnection available");

			long clientSendInterval = getConnectHeaders().getHeartbeat()[0];
			long clientReceiveInterval = getConnectHeaders().getHeartbeat()[1];
			long serverSendInterval = connectedHeaders.getHeartbeat()[0];
			long serverReceiveInterval = connectedHeaders.getHeartbeat()[1];

			if (clientSendInterval > 0 && serverReceiveInterval > 0) {
				long interval = Math.max(clientSendInterval, serverReceiveInterval);
				con.onWriteInactivity(() ->
						con.send(HEARTBEAT_MESSAGE).addCallback(
								result -> {},
								ex -> handleTcpConnectionFailure(
										"Failed to forward heartbeat: " + ex.getMessage(), ex)), interval);
			}
			if (clientReceiveInterval > 0 && serverSendInterval > 0) {
				final long interval = Math.max(clientReceiveInterval, serverSendInterval) * HEARTBEAT_MULTIPLIER;
				con.onReadInactivity(
						() -> handleTcpConnectionFailure("No messages received in " + interval + " ms.", null), interval);
			}
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
							result -> {},
							ex -> {
								String error = "Failed to subscribe in \"system\" session.";
								handleTcpConnectionFailure(error, ex);
							});
				}
			}
		}

		@Override
		protected void handleInboundMessage(Message<?> message) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			if (accessor != null && StompCommand.MESSAGE.equals(accessor.getCommand())) {
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
		protected void handleTcpConnectionFailure(String errorMessage, @Nullable Throwable ex) {
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

		@Override
		protected boolean shouldSendHeartbeatForIgnoredMessage() {
			return false;
		}
	}


	private class ClientSendMessageCountTask implements Runnable {

		@Override
		public void run() {
			long now = System.currentTimeMillis();
			for (RelayConnectionHandler handler : connectionHandlers.values()) {
				handler.updateClientSendMessageCount(now);
			}
		}
	}



	private static class VoidCallable implements Callable<Void> {

		@Override
		public Void call() {
			return null;
		}
	}


	/**
	 * Contract for access to session counters.
	 * @since 5.2
	 */
	public interface Stats {

		/**
		 * The number of connection handlers.
		 */
		int getTotalHandlers();

		/**
		 * The number of CONNECT frames processed.
		 */
		int getTotalConnect();

		/**
		 * The number of CONNECTED frames processed.
		 */
		int getTotalConnected();

		/**
		 * The number of DISCONNECT frames processed.
		 */
		int getTotalDisconnect();
	}


	private class DefaultStats implements Stats {

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

		@Override
		public int getTotalHandlers() {
			return connectionHandlers.size();
		}

		@Override
		public int getTotalConnect() {
			return this.connect.get();
		}

		@Override
		public int getTotalConnected() {
			return this.connected.get();
		}

		@Override
		public int getTotalDisconnect() {
			return this.disconnect.get();
		}

		@Override
		public String toString() {
			return (connectionHandlers.size() + " sessions, " + getTcpClientInfo() +
					(isBrokerAvailable() ? " (available)" : " (not available)") +
					", processed CONNECT(" + this.connect.get() + ")-CONNECTED(" +
					this.connected.get() + ")-DISCONNECT(" + this.disconnect.get() + ")");
		}
	}

}
