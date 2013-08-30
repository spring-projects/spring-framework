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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
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

	private Environment environment;

	private TcpClient<Message<byte[]>, Message<byte[]>> tcpClient;

	private final Map<String, RelaySession> relaySessions = new ConcurrentHashMap<String, RelaySession>();


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
	 * Set the login for a "system" TCP connection used to send messages to the STOMP
	 * broker without having a client session (e.g. REST/HTTP request handling method).
	 */
	public void setSystemLogin(String systemLogin) {
		Assert.hasText(systemLogin, "systemLogin must not be empty");
		this.systemLogin = systemLogin;
	}

	/**
	 * @return the login for a shared, "system" connection to the STOMP message broker.
	 */
	public String getSystemLogin() {
		return this.systemLogin;
	}

	/**
	 * Set the passcode for a "system" TCP connection used to send messages to the STOMP
	 * broker without having a client session (e.g. REST/HTTP request handling method).
	 */
	public void setSystemPasscode(String systemPasscode) {
		this.systemPasscode = systemPasscode;
	}

	/**
	 * @return the passcode for a shared, "system" connection to the STOMP message broker.
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
		SystemRelaySession session = new SystemRelaySession();
		this.relaySessions.put(session.getId(), session);
		session.connect();
	}

	@Override
	protected void stopInternal() {
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
			sessionId = (sessionId == null) ? SystemRelaySession.ID : sessionId;
			headers.setSessionId(sessionId);
			command = (command == null) ? StompCommand.SEND : command;
			headers.setCommandIfNotSet(command);
			message = MessageBuilder.withPayloadAndHeaders(message.getPayload(), headers).build();
		}

		if (headers.getCommand() == null) {
			logger.error("No STOMP command, ignoring message: " + message);
			return;
		}
		if (sessionId == null) {
			logger.error("No sessionId, ignoring message: " + message);
			return;
		}
		if (command.requiresDestination() && !checkDestinationPrefix(destination)) {
			return;
		}

		try {
			if (SimpMessageType.CONNECT.equals(messageType)) {
				headers.setHeartbeat(0, 0);
				message = MessageBuilder.withPayloadAndHeaders(message.getPayload(), headers).build();
				RelaySession session = new RelaySession(sessionId);
				this.relaySessions.put(sessionId, session);
				session.connect(message);
			}
			else if (SimpMessageType.DISCONNECT.equals(messageType)) {
				RelaySession session = this.relaySessions.remove(sessionId);
				if (session == null) {
					if (logger.isTraceEnabled()) {
						logger.trace("Session already removed, sessionId=" + sessionId);
					}
					return;
				}
				session.forward(message);
			}
			else {
				RelaySession session = this.relaySessions.get(sessionId);
				if (session == null) {
					logger.warn("Session id=" + sessionId + " not found. Ignoring message: " + message);
					return;
				}
				session.forward(message);
			}
		}
		catch (Throwable t) {
			logger.error("Failed to handle message " + message, t);
		}
	}


	private class RelaySession {

		private final String sessionId;

		private final BlockingQueue<Message<?>> messageQueue = new LinkedBlockingQueue<Message<?>>(50);

		private volatile StompConnection stompConnection = new StompConnection();

		private final Object monitor = new Object();


		private RelaySession(String sessionId) {
			Assert.notNull(sessionId, "sessionId is required");
			this.sessionId = sessionId;
		}


		public String getId() {
			return this.sessionId;
		}

		public void connect(final Message<?> connectMessage) {
			Assert.notNull(connectMessage, "connectMessage is required");

			Composable<TcpConnection<Message<byte[]>, Message<byte[]>>> promise = openTcpConnection();
			promise.consume(new Consumer<TcpConnection<Message<byte[]>, Message<byte[]>>>() {
				@Override
				public void accept(TcpConnection<Message<byte[]>, Message<byte[]>> connection) {
					handleTcpConnection(connection, connectMessage);
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

		protected Composable<TcpConnection<Message<byte[]>, Message<byte[]>>> openTcpConnection() {
			return tcpClient.open();
		}

		protected void handleTcpConnection(TcpConnection<Message<byte[]>, Message<byte[]>> tcpConn, final Message<?> connectMessage) {
			this.stompConnection.setTcpConnection(tcpConn);
			tcpConn.in().consume(new Consumer<Message<byte[]>>() {
				@Override
				public void accept(Message<byte[]> message) {
					readStompFrame(message);
				}
			});
			forwardInternal(tcpConn, connectMessage);
		}

		private void readStompFrame(Message<byte[]> message) {
			if (logger.isTraceEnabled()) {
				logger.trace("Reading message " + message);
			}

			StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
			if (StompCommand.CONNECTED == headers.getCommand()) {
				synchronized(this.monitor) {
					this.stompConnection.setReady();
					publishBrokerAvailableEvent();
					flushMessages();
				}
				return;
			}

			headers.setSessionId(this.sessionId);
			message = MessageBuilder.withPayloadAndHeaders(message.getPayload(), headers).build();
			sendMessageToClient(message);
		}

		private void handleTcpClientFailure(String message, Throwable ex) {
			if (logger.isErrorEnabled()) {
				logger.error(message + ", sessionId=" + this.sessionId, ex);
			}
			this.stompConnection.setDisconnected();
			sendError(message);
			publishBrokerUnavailableEvent();
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

		public void forward(Message<?> message) {

			if (!this.stompConnection.isReady()) {
				synchronized(this.monitor) {
					if (!this.stompConnection.isReady()) {
						this.messageQueue.add(message);
						if (logger.isTraceEnabled()) {
							logger.trace("Not connected, message queued. Queue size=" + this.messageQueue.size());
						}
						return;
					}
				}
			}

			if (this.messageQueue.isEmpty()) {
				forwardInternal(message);
			}
			else {
				this.messageQueue.add(message);
				flushMessages();
			}
		}

		private boolean forwardInternal(final Message<?> message) {
			TcpConnection<Message<byte[]>, Message<byte[]>> tcpConnection = this.stompConnection.getReadyConnection();
			if (tcpConnection == null) {
				return false;
			}
			return forwardInternal(tcpConnection, message);
		}

		@SuppressWarnings("unchecked")
		private boolean forwardInternal(TcpConnection<Message<byte[]>, Message<byte[]>> tcpConnection, final Message<?> message) {

			Assert.isInstanceOf(byte[].class, message.getPayload(), "Message's payload must be a byte[]");

			if (logger.isTraceEnabled()) {
				logger.trace("Forwarding to STOMP broker, message: " + message);
			}

			final Deferred<Boolean, Promise<Boolean>> deferred = new DeferredPromiseSpec<Boolean>().get();
			tcpConnection.send((Message<byte[]>)message, new Consumer<Boolean>() {
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
					if (StompHeaderAccessor.wrap(message).getCommand() != StompCommand.DISCONNECT) {
						handleTcpClientFailure("Failed to forward message to the broker", null);
					}
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				handleTcpClientFailure("Interrupted while forwarding message to the broker", ex);
			}
			return (success != null) ? success : false;
		}

		private void flushMessages() {
			List<Message<?>> messages = new ArrayList<Message<?>>();
			this.messageQueue.drainTo(messages);
			for (Message<?> message : messages) {
				if (!forwardInternal(message)) {
					return;
				}
			}
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
			this.connection = null;
		}

		@Override
		public String toString() {
			return "StompConnection [ready=" + isReady() + "]";
		}
	}

	private class SystemRelaySession extends RelaySession {

		public static final String ID = "stompRelaySystemSessionId";


		public SystemRelaySession() {
			super(ID);
		}

		public void connect() {
			StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
			headers.setAcceptVersion("1.1,1.2");
			headers.setLogin(systemLogin);
			headers.setPasscode(systemPasscode);
			headers.setHeartbeat(0,0);
			Message<?> connectMessage = MessageBuilder.withPayloadAndHeaders(new byte[0], headers).build();
			super.connect(connectMessage);
		}

		@Override
		protected Composable<TcpConnection<Message<byte[]>, Message<byte[]>>> openTcpConnection() {
			return tcpClient.open(new Reconnect() {
				@Override
				public Tuple2<InetSocketAddress, Long> reconnect(InetSocketAddress address, int attempt) {
					return Tuple.of(address, 5000L);
				}
			});
		}

		@Override
		protected void sendMessageToClient(Message<?> message) {
			StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
			if (StompCommand.ERROR.equals(headers.getCommand())) {
				if (logger.isErrorEnabled()) {
					logger.error("System session received ERROR frame from broker: " + message);
				}
			}
			else {
				// Ignore
			}
		}
	}

}
