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

package org.springframework.messaging.simp.stomp;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.handler.AbstractBrokerMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.tcp.FixedIntervalReconnectStrategy;
import org.springframework.messaging.support.tcp.ReactorNettyTcpClient;
import org.springframework.messaging.support.tcp.TcpConnection;
import org.springframework.messaging.support.tcp.TcpConnectionHandler;
import org.springframework.messaging.support.tcp.TcpOperations;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureTask;


/**
 * A {@link MessageHandler} that handles messages by forwarding them to a STOMP broker.
 * For each new {@link SimpMessageType#CONNECT CONNECT} message, an independent TCP
 * connection to the broker is opened and used exclusively for all messages from the
 * client that originated the CONNECT message. Messages from the same client are
 * identified through the session id message header. Reversely, when the STOMP broker
 * sends messages back on the TCP connection, those messages are enriched with the session
 * id of the client and sent back downstream through the {@link MessageChannel} provided
 * to the constructor.
 * <p>
 * This class also automatically opens a default "system" TCP connection to the message
 * broker that is used for sending messages that originate from the server application (as
 * opposed to from a client). Such messages are recognized because they are not associated
 * with any client and therefore do not have a session id header. The "system" connection
 * is effectively shared and cannot be used to receive messages. Several properties are
 * provided to configure the "system" connection including the the
 * {@link #setSystemLogin(String) login} {@link #setSystemPasscode(String) passcode},
 * heartbeat {@link #setSystemHeartbeatSendInterval(long) send} and
 * {@link #setSystemHeartbeatReceiveInterval(long) receive} intervals.
 *
 * @author Rossen Stoyanchev
 * @author Andy Wilkinson
 * @since 4.0
 */
public class StompBrokerRelayMessageHandler extends AbstractBrokerMessageHandler {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private static final Message<byte[]> HEARTBEAT_MESSAGE = MessageBuilder.withPayload(new byte[] {'\n'}).build();

	private static final long HEARTBEAT_MULTIPLIER = 3;


	private final MessageChannel messageChannel;

	private String relayHost = "127.0.0.1";

	private int relayPort = 61613;

	private String systemLogin = "guest";

	private String systemPasscode = "guest";

	private long systemHeartbeatSendInterval = 10000;

	private long systemHeartbeatReceiveInterval = 10000;

	private String virtualHost;

	private TcpOperations<byte[]> tcpClient;

	private final Map<String, StompConnectionHandler> connectionHandlers =
			new ConcurrentHashMap<String, StompConnectionHandler>();


	/**
	 * @param messageChannel the channel to send messages from the STOMP broker to
	 * @param destinationPrefixes the broker supported destination prefixes; destinations
	 *        that do not match the given prefix are ignored.
	 */
	public StompBrokerRelayMessageHandler(MessageChannel messageChannel, Collection<String> destinationPrefixes) {
		super(destinationPrefixes);
		Assert.notNull(messageChannel, "messageChannel is required");
		this.messageChannel = messageChannel;
	}


	/**
	 * Set the STOMP message broker host.
	 */
	public void setRelayHost(String relayHost) {
		Assert.hasText(relayHost, "relayHost must not be empty");
		this.relayHost = relayHost;
	}

	/**
	 * @return the STOMP message broker host.
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
	 * @return the STOMP message broker port.
	 */
	public int getRelayPort() {
		return this.relayPort;
	}

	/**
	 * Configure the TCP client to for managing STOMP over TCP connections to the message
	 * broker. This is an optional property that can be used to replace the default
	 * implementation used for example for testing purposes.
	 * <p>
	 * By default an instance of {@link ReactorNettyTcpClient} is used.
	 */
	public void setTcpClient(TcpOperations<byte[]> tcpClient) {
		this.tcpClient = tcpClient;
	}

	/**
	 * Set the interval, in milliseconds, at which the "system" connection will, in the
	 * absence of any other data being sent, send a heartbeat to the STOMP broker. A value
	 * of zero will prevent heartbeats from being sent to the broker.
	 * <p>
	 * The default value is 10000.
	 * <p>
	 * See class-level documentation for more information on the "system" connection.
	 */
	public void setSystemHeartbeatSendInterval(long systemHeartbeatSendInterval) {
		this.systemHeartbeatSendInterval = systemHeartbeatSendInterval;
	}

	/**
	 * @return The interval, in milliseconds, at which the "system" connection will
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
	 * <p>
	 * The default value is 10000.
	 * <p>
	 * See class-level documentation for more information on the "system" connection.
	 */
	public void setSystemHeartbeatReceiveInterval(long heartbeatReceiveInterval) {
		this.systemHeartbeatReceiveInterval = heartbeatReceiveInterval;
	}

	/**
	 * @return The interval, in milliseconds, at which the "system" connection expects
	 * to receive heartbeats from the STOMP broker.
	 */
	public long getSystemHeartbeatReceiveInterval() {
		return this.systemHeartbeatReceiveInterval;
	}

	/**
	 * Set the login for the "system" connection used to send messages to the STOMP
	 * broker without having a client session (e.g. REST/HTTP request handling method).
	 * <p>
	 * See class-level documentation for more information on the "system" connection.
	 */
	public void setSystemLogin(String systemLogin) {
		Assert.hasText(systemLogin, "systemLogin must not be empty");
		this.systemLogin = systemLogin;
	}

	/**
	 * @return the login used by the "system" connection to connect to the STOMP broker
	 */
	public String getSystemLogin() {
		return this.systemLogin;
	}

	/**
	 * Set the passcode for the "system" connection used to send messages to the STOMP
	 * broker without having a client session (e.g. REST/HTTP request handling method).
	 * <p>
	 * See class-level documentation for more information on the "system" connection.
	 */
	public void setSystemPasscode(String systemPasscode) {
		this.systemPasscode = systemPasscode;
	}

	/**
	 * @return the passcode used by the "system" connection to connect to the STOMP broker
	 */
	public String getSystemPasscode() {
		return this.systemPasscode;
	}

	/**
	 * Set the value of the "host" header to use in STOMP CONNECT frames. When this
	 * property is configured, a "host" header will be added to every STOMP frame sent to
	 * the STOMP broker. This may be useful for example in a cloud environment where the
	 * actual host to which the TCP connection is established is different from the host
	 * providing the cloud-based STOMP service.
	 * <p>
	 * By default this property is not set.
	 */
	public void setVirtualHost(String virtualHost) {
		this.virtualHost = virtualHost;
	}

	/**
	 * @return the configured virtual host value.
	 */
	public String getVirtualHost() {
		return this.virtualHost;
	}


	@Override
	protected void startInternal() {

		this.tcpClient = new ReactorNettyTcpClient<byte[]>(this.relayHost, this.relayPort, new StompCodec());

		if (logger.isDebugEnabled()) {
			logger.debug("Initializing \"system\" TCP connection");
		}

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setAcceptVersion("1.1,1.2");
		headers.setLogin(this.systemLogin);
		headers.setPasscode(this.systemPasscode);
		headers.setHeartbeat(this.systemHeartbeatSendInterval, this.systemHeartbeatReceiveInterval);
		headers.setHost(getVirtualHost());

		SystemStompConnectionHandler handler = new SystemStompConnectionHandler(headers);
		this.connectionHandlers.put(handler.getSessionId(), handler);

		this.tcpClient.connect(handler, new FixedIntervalReconnectStrategy(5000));
	}

	@Override
	protected void stopInternal() {
		for (StompConnectionHandler handler : this.connectionHandlers.values()) {
			try {
				handler.resetTcpConnection();
			}
			catch (Throwable t) {
				logger.error("Failed to close STOMP connection " + t.getMessage());
			}
		}
		try {
			this.tcpClient.shutdown();
		}
		catch (Throwable t) {
			logger.error("Error while shutting down TCP client", t);
		}
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {

		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
		String sessionId = headers.getSessionId();
		String destination = headers.getDestination();
		StompCommand command = headers.getCommand();
		SimpMessageType messageType = headers.getMessageType();

		if (SimpMessageType.MESSAGE.equals(messageType)) {
			sessionId = (sessionId == null) ? SystemStompConnectionHandler.SESSION_ID : sessionId;
			headers.setSessionId(sessionId);
			command = headers.updateStompCommandAsClientMessage();
			message = MessageBuilder.withPayload(message.getPayload()).setHeaders(headers).build();
		}

		if (sessionId == null) {
			logger.error("No sessionId, ignoring message: " + message);
			return;
		}

		if ((command != null) && command.requiresDestination() && !checkDestinationPrefix(destination)) {
			return;
		}

		if (SimpMessageType.CONNECT.equals(messageType)) {
			if (getVirtualHost() != null) {
				headers.setHost(getVirtualHost());
			}
			StompConnectionHandler handler = new StompConnectionHandler(sessionId, headers);
			this.connectionHandlers.put(sessionId, handler);
			this.tcpClient.connect(handler);
		}
		else if (SimpMessageType.DISCONNECT.equals(messageType)) {
			StompConnectionHandler handler = removeConnectionHandler(sessionId);
			if (handler == null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Connection already removed for sessionId=" + sessionId);
				}
				return;
			}
			handler.forward(message);
		}
		else {
			StompConnectionHandler handler = this.connectionHandlers.get(sessionId);
			if (handler == null) {
				logger.warn("Connection for sessionId=" + sessionId + " not found. Ignoring message: " + message);
				return;
			}
			handler.forward(message);
		}
	}

	private StompConnectionHandler removeConnectionHandler(String sessionId) {
		return SystemStompConnectionHandler.SESSION_ID.equals(sessionId)
				? null : this.connectionHandlers.remove(sessionId);
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

		private StompConnectionHandler(String sessionId, StompHeaderAccessor connectHeaders,
				boolean isRemoteClientSession) {

			Assert.notNull(sessionId, "sessionId is required");
			Assert.notNull(connectHeaders, "connectHeaders is required");

			this.sessionId = sessionId;
			this.connectHeaders = connectHeaders;
			this.isRemoteClientSession = isRemoteClientSession;
		}

		public String getSessionId() {
			return this.sessionId;
		}

		@Override
		public void afterConnected(TcpConnection<byte[]> connection) {
			this.tcpConnection = connection;
			connection.send(MessageBuilder.withPayload(EMPTY_PAYLOAD).setHeaders(this.connectHeaders).build());
		}

		@Override
		public void afterConnectFailure(Throwable ex) {
			handleTcpConnectionFailure("Failed to connect to message broker", ex);
		}

		/**
		 * Invoked when any TCP connectivity issue is detected, i.e. failure to establish
		 * the TCP connection, failure to send a message, missed heartbeat.
		 */
		protected void handleTcpConnectionFailure(String errorMessage, Throwable ex) {
			if (logger.isErrorEnabled()) {
				logger.error(errorMessage + ", sessionId=" + this.sessionId, ex);
			}
			resetTcpConnection();
			sendStompErrorToClient(errorMessage);
		}

		private void sendStompErrorToClient(String errorText) {
			if (this.isRemoteClientSession) {
				StompConnectionHandler removed = removeConnectionHandler(this.sessionId);
				if (removed != null) {
					StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.ERROR);
					headers.setSessionId(this.sessionId);
					headers.setMessage(errorText);
					Message<?> errorMessage = MessageBuilder.withPayload(EMPTY_PAYLOAD).setHeaders(headers).build();
					sendMessageToClient(errorMessage);
				}
			}
		}

		protected void sendMessageToClient(Message<?> message) {
			if (this.isRemoteClientSession) {
				StompBrokerRelayMessageHandler.this.messageChannel.send(message);
			}
		}

		@Override
		public void handleMessage(Message<byte[]> message) {
			if (logger.isTraceEnabled()) {
				logger.trace("Reading message for sessionId=" + this.sessionId + ", " + message);
			}

			StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
			if (StompCommand.CONNECTED == headers.getCommand()) {
				afterStompConnected(headers);
			}

			headers.setSessionId(this.sessionId);
			message = MessageBuilder.withPayload(message.getPayload()).setHeaders(headers).build();
			sendMessageToClient(message);
		}

		/**
		 * Invoked after the STOMP CONNECTED frame is received. At this point the
		 * connection is ready for sending STOMP messages to the broker.
		 */
		protected void afterStompConnected(StompHeaderAccessor connectedHeaders) {
			this.isStompConnected = true;
			initHeartbeats(connectedHeaders);
		}

		private void initHeartbeats(StompHeaderAccessor connectedHeaders) {

			// Remote clients do their own heartbeat management
			if (this.isRemoteClientSession) {
				return;
			}

			long clientSendInterval = this.connectHeaders.getHeartbeat()[0];
			long clientReceiveInterval = this.connectHeaders.getHeartbeat()[1];

			long serverSendInterval = connectedHeaders.getHeartbeat()[0];
			long serverReceiveInterval = connectedHeaders.getHeartbeat()[1];

			if ((clientSendInterval > 0) && (serverReceiveInterval > 0)) {
				long interval = Math.max(clientSendInterval,  serverReceiveInterval);
				this.tcpConnection.onWriteInactivity(new Runnable() {
					@Override
					public void run() {
						TcpConnection<byte[]> conn = tcpConnection;
						if (conn != null) {
							conn.send(HEARTBEAT_MESSAGE).addCallback(
									new ListenableFutureCallback<Boolean>() {
										public void onFailure(Throwable t) {
											handleTcpConnectionFailure("Failed to send heartbeat", null);
										}
										public void onSuccess(Boolean result) {}
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
						handleTcpConnectionFailure("No hearbeat from broker for more than " +
								interval + "ms, closing connection", null);
					}
				},  interval);
			}
		}

		@Override
		public void afterConnectionClosed() {
			sendStompErrorToClient("Connection to broker closed");
		}

		public ListenableFuture<Boolean> forward(final Message<?> message) {

			if (!this.isStompConnected) {
				if (logger.isWarnEnabled()) {
					logger.warn("Connection to broker inactive or not ready, ignoring message=" + message);
				}
				return new ListenableFutureTask<Boolean>(new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return Boolean.FALSE;
					}
				});
			}

			if (logger.isTraceEnabled()) {
				logger.trace("Forwarding message to broker: " + message);
			}

			@SuppressWarnings("unchecked")
			ListenableFuture<Boolean> future = this.tcpConnection.send((Message<byte[]>) message);

			future.addCallback(new ListenableFutureCallback<Boolean>() {
				@Override
				public void onSuccess(Boolean result) {
					StompCommand command = StompHeaderAccessor.wrap(message).getCommand();
					if (command == StompCommand.DISCONNECT) {
						resetTcpConnection();
					}
				}
				@Override
				public void onFailure(Throwable t) {
					handleTcpConnectionFailure("Failed to send message " + message, t);
				}
			});

			return future;
		}

		public void resetTcpConnection() {
			TcpConnection<byte[]> conn = this.tcpConnection;
			this.isStompConnected = false;
			this.tcpConnection = null;
			if (conn != null) {
				try {
					this.tcpConnection.close();
				}
				catch (Throwable t) {
					// ignore
				}
			}
		}

	}

	private class SystemStompConnectionHandler extends StompConnectionHandler {

		public static final String SESSION_ID = "stompRelaySystemSessionId";


		public SystemStompConnectionHandler(StompHeaderAccessor connectHeaders) {
			super(SESSION_ID, connectHeaders, false);
		}

		@Override
		protected void afterStompConnected(StompHeaderAccessor connectedHeaders) {
			super.afterStompConnected(connectedHeaders);
			publishBrokerAvailableEvent();
		}

		@Override
		protected void handleTcpConnectionFailure(String errorMessage, Throwable t) {
			super.handleTcpConnectionFailure(errorMessage, t);
			publishBrokerUnavailableEvent();
		}

		@Override
		public void afterConnectionClosed() {
			super.afterConnectionClosed();
			publishBrokerUnavailableEvent();
		}

		@Override
		public ListenableFuture<Boolean> forward(Message<?> message) {
			try {
				ListenableFuture<Boolean> future = super.forward(message);
				if (!future.get()) {
					throw new MessageDeliveryException(message);
				}
				return future;
			}
			catch (Throwable t) {
				throw new MessageDeliveryException(message, t);
			}
		}
	}

}
