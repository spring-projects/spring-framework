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

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.handler.AbstractBrokerMessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

import reactor.core.Environment;
import reactor.core.composable.Composable;
import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.spec.DeferredPromiseSpec;
import reactor.function.Consumer;
import reactor.tcp.Reconnect;
import reactor.tcp.TcpClient;
import reactor.tcp.TcpConnection;
import reactor.tcp.netty.NettyTcpClient;
import reactor.tcp.spec.TcpClientSpec;
import reactor.tuple.Tuple;
import reactor.tuple.Tuple2;


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
 * provided to configure the "system" session including the the
 * {@link #setSystemLogin(String) login} {@link #setSystemPasscode(String) passcode},
 * heartbeat {@link #setSystemHeartbeatSendInterval(long) send} and
 * {@link #setSystemHeartbeatReceiveInterval(long) receive} intervals.
 *
 * @author Rossen Stoyanchev
 * @author Andy Wilkinson
 * @since 4.0
 */
public class StompBrokerRelayMessageHandler extends AbstractBrokerMessageHandler {

	private final MessageChannel messageChannel;

	private String relayHost = "127.0.0.1";

	private int relayPort = 61613;

	private String systemLogin = "guest";

	private String systemPasscode = "guest";

	private long systemHeartbeatSendInterval = 10000;

	private long systemHeartbeatReceiveInterval = 10000;

	private String virtualHost;

	private Environment environment;

	private TcpClient<Message<byte[]>, Message<byte[]>> tcpClient;

	private final Map<String, StompRelaySession> relaySessions = new ConcurrentHashMap<String, StompRelaySession>();


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
	 * Set the interval, in milliseconds, at which the "system" relay session will, in the
	 * absence of any other data being sent, send a heartbeat to the STOMP broker. A value
	 * of zero will prevent heartbeats from being sent to the broker.
	 * <p>
	 * The default value is 10000.
	 * <p>
	 * See class-level documentation for more information on the "system" session.
	 */
	public void setSystemHeartbeatSendInterval(long systemHeartbeatSendInterval) {
		this.systemHeartbeatSendInterval = systemHeartbeatSendInterval;
	}

	/**
	 * @return The interval, in milliseconds, at which the "system" relay session will
	 * send heartbeats to the STOMP broker.
	 */
	public long getSystemHeartbeatSendInterval() {
		return this.systemHeartbeatSendInterval;
	}

	/**
	 * Set the maximum interval, in milliseconds, at which the "system" relay session
	 * expects, in the absence of any other data, to receive a heartbeat from the STOMP
	 * broker. A value of zero will configure the relay session to expect not to receive
	 * heartbeats from the broker.
	 * <p>
	 * The default value is 10000.
	 * <p>
	 * See class-level documentation for more information on the "system" session.
	 */
	public void setSystemHeartbeatReceiveInterval(long heartbeatReceiveInterval) {
		this.systemHeartbeatReceiveInterval = heartbeatReceiveInterval;
	}

	/**
	 * @return The interval, in milliseconds, at which the "system" relay session expects
	 * to receive heartbeats from the STOMP broker.
	 */
	public long getSystemHeartbeatReceiveInterval() {
		return this.systemHeartbeatReceiveInterval;
	}

	/**
	 * Set the login for the "system" relay session used to send messages to the STOMP
	 * broker without having a client session (e.g. REST/HTTP request handling method).
	 * <p>
	 * See class-level documentation for more information on the "system" session.
	 */
	public void setSystemLogin(String systemLogin) {
		Assert.hasText(systemLogin, "systemLogin must not be empty");
		this.systemLogin = systemLogin;
	}

	/**
	 * @return the login used by the "system" relay session to connect to the STOMP broker
	 */
	public String getSystemLogin() {
		return this.systemLogin;
	}

	/**
	 * Set the passcode for the "system" relay session used to send messages to the STOMP
	 * broker without having a client session (e.g. REST/HTTP request handling method).
	 * <p>
	 * See class-level documentation for more information on the "system" session.
	 */
	public void setSystemPasscode(String systemPasscode) {
		this.systemPasscode = systemPasscode;
	}

	/**
	 * @return the passcode used by the "system" relay session to connect to the STOMP broker
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
		this.environment = new Environment();
		this.tcpClient = new TcpClientSpec<Message<byte[]>, Message<byte[]>>(NettyTcpClient.class)
				.env(this.environment)
				.codec(new StompCodec())
				.connect(this.relayHost, this.relayPort)
				.get();

		if (logger.isDebugEnabled()) {
			logger.debug("Initializing \"system\" TCP connection");
		}

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setAcceptVersion("1.1,1.2");
		headers.setLogin(this.systemLogin);
		headers.setPasscode(this.systemPasscode);
		headers.setHeartbeat(this.systemHeartbeatSendInterval, this.systemHeartbeatReceiveInterval);
		headers.setHost(getVirtualHost());
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();

		SystemStompRelaySession session = new SystemStompRelaySession();
		session.connect(message);

		this.relaySessions.put(session.getId(), session);
	}

	@Override
	protected void stopInternal() {
		for (StompRelaySession session: this.relaySessions.values()) {
			session.disconnect();
		}
		try {
			this.tcpClient.close().await();
		}
		catch (Throwable t) {
			logger.error("Failed to close reactor TCP client", t);
		}
		try {
			this.environment.shutdown();
		}
		catch (Throwable t) {
			logger.error("Failed to shut down reactor Environment", t);
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
			sessionId = (sessionId == null) ? SystemStompRelaySession.ID : sessionId;
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
				message = MessageBuilder.withPayload(message.getPayload()).setHeaders(headers).build();
			}
			StompRelaySession session = new StompRelaySession(sessionId);
			session.connect(message);
			this.relaySessions.put(sessionId, session);
		}
		else if (SimpMessageType.DISCONNECT.equals(messageType)) {
			StompRelaySession session = this.relaySessions.remove(sessionId);
			if (session == null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Session already removed, sessionId=" + sessionId);
				}
				return;
			}
			session.forward(message);
		}
		else {
			StompRelaySession session = this.relaySessions.get(sessionId);
			if (session == null) {
				logger.warn("Session id=" + sessionId + " not found. Ignoring message: " + message);
				return;
			}
			session.forward(message);
		}
	}


	private class StompRelaySession {

		private static final long HEARTBEAT_MULTIPLIER = 3;

		private final String sessionId;

		private final boolean isRemoteClientSession;

		private final long reconnectInterval;

		private volatile StompConnection stompConnection = new StompConnection();

		private volatile StompHeaderAccessor connectHeaders;

		private volatile StompHeaderAccessor connectedHeaders;


		private StompRelaySession(String sessionId) {
			this(sessionId, true, 0);
		}

		private StompRelaySession(String sessionId, boolean isRemoteClientSession, long reconnectInterval) {
			Assert.notNull(sessionId, "sessionId is required");
			this.sessionId = sessionId;
			this.isRemoteClientSession = isRemoteClientSession;
			this.reconnectInterval = reconnectInterval;
		}


		public String getId() {
			return this.sessionId;
		}

		public void connect(final Message<?> connectMessage) {

			Assert.notNull(connectMessage, "connectMessage is required");
			this.connectHeaders = StompHeaderAccessor.wrap(connectMessage);

			Composable<TcpConnection<Message<byte[]>, Message<byte[]>>> promise;
			if (this.reconnectInterval > 0) {
				promise = tcpClient.open(new Reconnect() {
					@Override
					public Tuple2<InetSocketAddress, Long> reconnect(InetSocketAddress address, int attempt) {
						return Tuple.of(address, 5000L);
					}
				});
			}
			else {
				promise = tcpClient.open();
			}

			promise.consume(new Consumer<TcpConnection<Message<byte[]>, Message<byte[]>>>() {
				@Override
				public void accept(TcpConnection<Message<byte[]>, Message<byte[]>> connection) {
					handleConnectionReady(connection, connectMessage);
				}
			});
			promise.when(Throwable.class, new Consumer<Throwable>() {
				@Override
				public void accept(Throwable ex) {
					relaySessions.remove(sessionId);
					handleTcpClientFailure("Failed to connect to message broker", ex);
				}
			});
		}

		public void disconnect() {
			this.stompConnection.setDisconnected();
		}

		protected void handleConnectionReady(
				TcpConnection<Message<byte[]>, Message<byte[]>> tcpConn, final Message<?> connectMessage) {

			this.stompConnection.setTcpConnection(tcpConn);
			tcpConn.on().close(new Runnable() {
				@Override
				public void run() {
					connectionClosed();
				}
			});
			tcpConn.in().consume(new Consumer<Message<byte[]>>() {
				@Override
				public void accept(Message<byte[]> message) {
					readStompFrame(message);
				}
			});
			forwardInternal(connectMessage, tcpConn);
		}

		protected void connectionClosed() {
			relaySessions.remove(this.sessionId);
			if (this.stompConnection.isReady()) {
				sendError("Lost connection to the broker");
			}
		}

		private void readStompFrame(Message<byte[]> message) {
			if (logger.isTraceEnabled()) {
				logger.trace("Reading message for sessionId=" + sessionId + ", " + message);
			}

			StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
			if (StompCommand.CONNECTED == headers.getCommand()) {
				this.connectedHeaders = headers;
				connected();
			}

			headers.setSessionId(this.sessionId);
			message = MessageBuilder.withPayload(message.getPayload()).setHeaders(headers).build();
			sendMessageToClient(message);
		}

		private void initHeartbeats() {

			long clientSendInterval = this.connectHeaders.getHeartbeat()[0];
			long clientReceiveInterval = this.connectHeaders.getHeartbeat()[1];

			long serverSendInterval = this.connectedHeaders.getHeartbeat()[0];
			long serverReceiveInterval = this.connectedHeaders.getHeartbeat()[1];

			if ((clientSendInterval > 0) && (serverReceiveInterval > 0)) {
				long interval = Math.max(clientSendInterval,  serverReceiveInterval);
				stompConnection.connection.on().writeIdle(interval, new Runnable() {

					@Override
					public void run() {
						TcpConnection<Message<byte[]>, Message<byte[]>> tcpConn = stompConnection.connection;
						if (tcpConn != null) {
							tcpConn.send(MessageBuilder.withPayload(new byte[] {'\n'}).build(),
								new Consumer<Boolean>() {
									@Override
									public void accept(Boolean result) {
										if (!result) {
											handleTcpClientFailure("Failed to send heartbeat to the broker", null);
										}
									}
								});
						}
					}
				});
			}

			if (clientReceiveInterval > 0 && serverSendInterval > 0) {
				final long interval = Math.max(clientReceiveInterval, serverSendInterval) * HEARTBEAT_MULTIPLIER;
				stompConnection.connection.on().readIdle(interval,  new Runnable() {

					@Override
					public void run() {
						String message = "Broker hearbeat missed: connection idle for more than " + interval + "ms";
						if (logger.isWarnEnabled()) {
							logger.warn(message);
						}
						disconnected(message);
					}
				});
			}
		}

		protected void connected() {
			if (!this.isRemoteClientSession) {
				initHeartbeats();
			}
			this.stompConnection.setReady();
		}

		protected void handleTcpClientFailure(String message, Throwable ex) {
			if (logger.isErrorEnabled()) {
				logger.error(message + ", sessionId=" + this.sessionId, ex);
			}
			disconnected(message);
		}

		protected void disconnected(String errorMessage) {
			this.stompConnection.setDisconnected();
			sendError(errorMessage);
		}

		private void sendError(String errorText) {
			StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.ERROR);
			headers.setSessionId(this.sessionId);
			headers.setMessage(errorText);
			Message<?> errorMessage = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
			sendMessageToClient(errorMessage);
		}

		protected void sendMessageToClient(Message<?> message) {
			if (this.isRemoteClientSession) {
				messageChannel.send(message);
			}
			else {
				StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
				if (StompCommand.ERROR.equals(headers.getCommand())) {
					if (logger.isErrorEnabled()) {
						logger.error("STOMP ERROR on sessionId=" + this.sessionId + ": " + message);
					}
				}
				// ignore otherwise
			}
		}

		private void forward(Message<?> message) {
			TcpConnection<Message<byte[]>, Message<byte[]>> tcpConnection = this.stompConnection.getReadyConnection();
			if (tcpConnection == null) {
				logger.warn("Connection to STOMP broker is not active");
				handleForwardFailure(message);
			}
			else if (!forwardInternal(message, tcpConnection)) {
				handleForwardFailure(message);
			}
		}

		protected void handleForwardFailure(Message<?> message) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to forward message to the broker. message=" + message);
			}
		}

		private boolean forwardInternal(
				Message<?> message, TcpConnection<Message<byte[]>, Message<byte[]>> tcpConnection) {

			Assert.isInstanceOf(byte[].class, message.getPayload(), "Message's payload must be a byte[]");

			@SuppressWarnings("unchecked")
			Message<byte[]> byteMessage = (Message<byte[]>) message;
			if (logger.isTraceEnabled()) {
				logger.trace("Forwarding to STOMP broker, message: " + message);
			}

			StompCommand command = StompHeaderAccessor.wrap(message).getCommand();

			final Deferred<Boolean, Promise<Boolean>> deferred = new DeferredPromiseSpec<Boolean>().get();
			tcpConnection.send(byteMessage, new Consumer<Boolean>() {
				@Override
				public void accept(Boolean success) {
					deferred.accept(success);
				}
			});

			Boolean success = null;
			try {
				success = deferred.compose().await();
				if (success == null) {
					handleTcpClientFailure("Timed out waiting for message to be forwarded to the broker", null);
				}
				else if (!success) {
					handleTcpClientFailure("Failed to forward message to the broker", null);
				}
				else {
					if (command == StompCommand.DISCONNECT) {
						this.stompConnection.setDisconnected();
					}
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				handleTcpClientFailure("Interrupted while forwarding message to the broker", ex);
			}
			return (success != null) ? success : false;
		}
	}

	private static class StompConnection {

		private volatile TcpConnection<Message<byte[]>, Message<byte[]>> connection;

		private AtomicReference<TcpConnection<Message<byte[]>, Message<byte[]>>> readyConnection =
				new AtomicReference<TcpConnection<Message<byte[]>, Message<byte[]>>>();


		public void setTcpConnection(TcpConnection<Message<byte[]>, Message<byte[]>> connection) {
			Assert.notNull(connection, "connection must not be null");
			this.connection = connection;
		}

		/**
		 * Return the underlying {@link TcpConnection} but only after the CONNECTED STOMP
		 * frame is received.
		 */
		public TcpConnection<Message<byte[]>, Message<byte[]>> getReadyConnection() {
			return this.readyConnection.get();
		}

		public void setReady() {
			this.readyConnection.set(this.connection);
		}

		public boolean isReady() {
			return (this.readyConnection.get() != null);
		}

		public void setDisconnected() {
			this.readyConnection.set(null);

			TcpConnection<Message<byte[]>, Message<byte[]>> localConnection = this.connection;
			if (localConnection != null) {
				localConnection.close();
				this.connection = null;
			}
		}

		@Override
		public String toString() {
			return "StompConnection [ready=" + isReady() + "]";
		}
	}

	private class SystemStompRelaySession extends StompRelaySession {

		public static final String ID = "stompRelaySystemSessionId";


		public SystemStompRelaySession() {
			super(ID, false, 5000);
		}

		@Override
		protected void connected() {
			super.connected();
			publishBrokerAvailableEvent();
		}

		@Override
		protected void disconnected(String errorMessage) {
			super.disconnected(errorMessage);
			publishBrokerUnavailableEvent();
		}

		@Override
		protected void connectionClosed() {
			publishBrokerUnavailableEvent();
		}

		@Override
		protected void handleForwardFailure(Message<?> message) {
			super.handleForwardFailure(message);
			throw new MessageDeliveryException(message);
		}
	}

}
