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
 * A {@link MessageHandler} that handles messages by forwarding them to a STOMP broker and
 * reversely sends any returned messages from the broker to the provided
 * {@link MessageChannel}.
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
	 * Set the interval, in milliseconds, at which the "system" relay session will,
	 * in the absence of any other data being sent, send a heartbeat to the STOMP broker.
	 * A value of zero will prevent heartbeats from being sent to the broker.
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
		SystemStompRelaySession session = new SystemStompRelaySession();
		this.relaySessions.put(session.getId(), session);
		session.connect();
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
			headers.updateStompCommandAsClientMessage();
			message = MessageBuilder.withPayloadAndHeaders(message.getPayload(), headers).build();
		}

		if (sessionId == null) {
			logger.error("No sessionId, ignoring message: " + message);
			return;
		}

		if (command != null && command.requiresDestination() && !checkDestinationPrefix(destination)) {
			return;
		}

		if (SimpMessageType.CONNECT.equals(messageType)) {
			message = MessageBuilder.withPayloadAndHeaders(message.getPayload(), headers).build();
			StompRelaySession session = new StompRelaySession(sessionId);
			this.relaySessions.put(sessionId, session);
			session.connect(message);
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

		private final String sessionId;

		private volatile StompConnection stompConnection = new StompConnection();


		private StompRelaySession(String sessionId) {
			Assert.notNull(sessionId, "sessionId is required");
			this.sessionId = sessionId;
		}


		public String getId() {
			return this.sessionId;
		}

		public void connect(final Message<?> connectMessage) {
			Assert.notNull(connectMessage, "connectMessage is required");

			Composable<TcpConnection<Message<byte[]>, Message<byte[]>>> promise = initConnection();
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

		protected Composable<TcpConnection<Message<byte[]>, Message<byte[]>>> initConnection() {
			return tcpClient.open();
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
			forwardInternal(tcpConn, connectMessage);
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
				connected(headers, this.stompConnection);
			}

			headers.setSessionId(this.sessionId);
			message = MessageBuilder.withPayloadAndHeaders(message.getPayload(), headers).build();
			sendMessageToClient(message);
		}

		protected void connected(StompHeaderAccessor headers, StompConnection stompConnection) {
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
			Message<?> errorMessage = MessageBuilder.withPayloadAndHeaders(new byte[0], headers).build();
			sendMessageToClient(errorMessage);
		}

		protected void sendMessageToClient(Message<?> message) {
			messageChannel.send(message);
		}

		private void forward(Message<?> message) {
			TcpConnection<Message<byte[]>, Message<byte[]>> tcpConnection = this.stompConnection.getReadyConnection();
			if (tcpConnection == null) {
				logger.warn("Connection to STOMP broker is not active");
				handleForwardFailure(message);
			}
			else if (!forwardInternal(tcpConnection, message)) {
				handleForwardFailure(message);
			}
		}

		protected void handleForwardFailure(Message<?> message) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to forward message to the broker. message=" + message);
			}
		}

		private boolean forwardInternal(
				TcpConnection<Message<byte[]>, Message<byte[]>> tcpConnection, Message<?> message) {

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

		private static final long HEARTBEAT_RECEIVE_MULTIPLIER = 3;

		public static final String ID = "stompRelaySystemSessionId";

		private final byte[] heartbeatPayload = new byte[] {'\n'};


		public SystemStompRelaySession() {
			super(ID);
		}

		public void connect() {
			StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
			headers.setAcceptVersion("1.1,1.2");
			headers.setLogin(systemLogin);
			headers.setPasscode(systemPasscode);
			headers.setHeartbeat(systemHeartbeatSendInterval, systemHeartbeatReceiveInterval);
			Message<?> connectMessage = MessageBuilder.withPayloadAndHeaders(new byte[0], headers).build();
			super.connect(connectMessage);
		}

		@Override
		protected Composable<TcpConnection<Message<byte[]>, Message<byte[]>>> initConnection() {
			return tcpClient.open(new Reconnect() {
				@Override
				public Tuple2<InetSocketAddress, Long> reconnect(InetSocketAddress address, int attempt) {
					return Tuple.of(address, 5000L);
				}
			});
		}

		@Override
		protected void connectionClosed() {
			publishBrokerUnavailableEvent();
		}

		@Override
		protected void connected(StompHeaderAccessor headers, final StompConnection stompConnection) {

			long brokerReceiveInterval = headers.getHeartbeat()[1];
			if ((systemHeartbeatSendInterval > 0) && (brokerReceiveInterval > 0)) {
				long interval = Math.max(systemHeartbeatSendInterval,  brokerReceiveInterval);
				stompConnection.connection.on().writeIdle(interval, new Runnable() {

					@Override
					public void run() {
						TcpConnection<Message<byte[]>, Message<byte[]>> tcpConn = stompConnection.connection;
						if (tcpConn != null) {
							tcpConn.send(MessageBuilder.withPayload(heartbeatPayload).build(),
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

			long brokerSendInterval = headers.getHeartbeat()[0];
			if (systemHeartbeatReceiveInterval > 0 && brokerSendInterval > 0) {
				final long interval = Math.max(systemHeartbeatReceiveInterval, brokerSendInterval)
						* HEARTBEAT_RECEIVE_MULTIPLIER;
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

			super.connected(headers, stompConnection);
			publishBrokerAvailableEvent();
		}

		@Override
		protected void disconnected(String errorMessage) {
			super.disconnected(errorMessage);
			publishBrokerUnavailableEvent();
		}

		@Override
		protected void sendMessageToClient(Message<?> message) {
			StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
			if (StompCommand.ERROR.equals(headers.getCommand())) {
				if (logger.isErrorEnabled()) {
					logger.error("STOMP ERROR frame on system session: " + message);
				}
			}
			else {
				// Ignore
			}
		}

		@Override
		protected void handleForwardFailure(Message<?> message) {
			super.handleForwardFailure(message);
			throw new MessageDeliveryException(message);
		}
	}

}
