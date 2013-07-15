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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import reactor.core.Environment;
import reactor.core.composable.Promise;
import reactor.function.Consumer;
import reactor.tcp.TcpClient;
import reactor.tcp.TcpConnection;
import reactor.tcp.encoding.DelimitedCodec;
import reactor.tcp.encoding.StandardCodecs;
import reactor.tcp.netty.NettyTcpClient;
import reactor.tcp.spec.TcpClientSpec;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompBrokerRelayMessageHandler implements MessageHandler, SmartLifecycle {

	private static final Log logger = LogFactory.getLog(StompBrokerRelayMessageHandler.class);

	private static final String STOMP_RELAY_SYSTEM_SESSION_ID = "stompRelaySystemSessionId";

	private final MessageChannel outboundChannel;

	private final String[] destinationPrefixes;

	private String relayHost = "127.0.0.1";

	private int relayPort = 61613;

	private String systemLogin = "guest";

	private String systemPasscode = "guest";

	private final StompMessageConverter stompMessageConverter = new StompMessageConverter();

	private Environment environment;

	private TcpClient<String, String> tcpClient;

	private final Map<String, RelaySession> relaySessions = new ConcurrentHashMap<String, RelaySession>();

	private Object lifecycleMonitor = new Object();

	private boolean running = false;


	/**
	 * @param outboundChannel a channel for messages going out to clients
	 * @param destinationPrefixes the broker supported destination prefixes; destinations
	 *        that do not match the given prefix are ignored.
	 */
	public StompBrokerRelayMessageHandler(MessageChannel outboundChannel, Collection<String> destinationPrefixes) {
		Assert.notNull(outboundChannel, "outboundChannel is required");
		Assert.notNull(destinationPrefixes, "destinationPrefixes is required");
		this.outboundChannel = outboundChannel;
		this.destinationPrefixes = destinationPrefixes.toArray(new String[destinationPrefixes.size()]);
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

	/**
	 * @return the configured STOMP broker supported destination prefixes.
	 */
	public String[] getDestinationPrefixes() {
		return destinationPrefixes;
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
	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (logger.isDebugEnabled()) {
				logger.debug("Starting STOMP broker relay");
			}
			this.environment = new Environment();
			this.tcpClient = new TcpClientSpec<String, String>(NettyTcpClient.class)
					.env(this.environment)
					.codec(new DelimitedCodec<String, String>((byte) 0, true, StandardCodecs.STRING_CODEC))
					.connect(this.relayHost, this.relayPort)
					.get();
			openSystemSession();
			this.running = true;
		}
	}

	/**
	 * Open a "system" session for sending messages from parts of the application
	 * not assoicated with a client STOMP session.
	 */
	private void openSystemSession() {

		RelaySession session = new RelaySession(STOMP_RELAY_SYSTEM_SESSION_ID) {
			@Override
			protected void sendMessageToClient(Message<?> message) {
				// ignore, only used to send messages
				// TODO: ERROR frame/reconnect
			}
		};
		this.relaySessions.put(STOMP_RELAY_SYSTEM_SESSION_ID, session);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setAcceptVersion("1.1,1.2");
		headers.setLogin(this.systemLogin);
		headers.setPasscode(this.systemPasscode);
		headers.setHeartbeat(0,0); // TODO

		if (logger.isDebugEnabled()) {
			logger.debug("Sending STOMP CONNECT frame to initialize \"system\" TCP connection");
		}
		Message<?> message = MessageBuilder.withPayload(new byte[0]).copyHeaders(headers.toMap()).build();
		session.open(message);
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (logger.isDebugEnabled()) {
				logger.debug("Stopping STOMP broker relay");
			}
			this.running = false;
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
	}

	@Override
	public void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	@Override
	public void handleMessage(Message<?> message) {

		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
		String sessionId = headers.getSessionId();
		String destination = headers.getDestination();
		StompCommand command = headers.getStompCommand();
		SimpMessageType messageType = headers.getMessageType();

		if (!this.running) {
			if (logger.isTraceEnabled()) {
				logger.trace("STOMP broker relay not running. Ignoring message id=" + headers.getId());
			}
			return;
		}

		if (SimpMessageType.MESSAGE.equals(messageType)) {
			sessionId = (sessionId == null) ? STOMP_RELAY_SYSTEM_SESSION_ID : sessionId;
			headers.setSessionId(sessionId);
			command = (command == null) ? StompCommand.SEND : command;
			headers.setStompCommandIfNotSet(command);
			message = MessageBuilder.fromMessage(message).copyHeaders(headers.toMap()).build();
		}

		if (headers.getStompCommand() == null) {
			logger.error("Ignoring message, no STOMP command: " + message);
			return;
		}
		if (sessionId == null) {
			logger.error("Ignoring message, no sessionId: " + message);
			return;
		}
		if (command.requiresDestination() && (destination == null)) {
			logger.error("Ignoring " + command + " message, no destination: " + message);
			return;
		}

		try {
			if ((destination == null) || supportsDestination(destination)) {
				if (logger.isTraceEnabled()) {
					logger.trace("Processing message: " + message);
				}
				handleInternal(message, messageType, sessionId);
			}
		}
		catch (Throwable t) {
			logger.error("Failed to handle message " + message, t);
		}
	}

	protected boolean supportsDestination(String destination) {
		for (String prefix : this.destinationPrefixes) {
			if (destination.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	protected void handleInternal(Message<?> message, SimpMessageType messageType, String sessionId) {
		if (SimpMessageType.CONNECT.equals(messageType)) {
			RelaySession session = new RelaySession(sessionId);
			this.relaySessions.put(sessionId, session);
			session.open(message);
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


	private class RelaySession {

		private final String sessionId;

		private final BlockingQueue<Message<?>> messageQueue = new LinkedBlockingQueue<Message<?>>(50);

		private Promise<TcpConnection<String, String>> promise;

		private volatile boolean isConnected = false;

		private final Object monitor = new Object();


		public RelaySession(String sessionId) {
			Assert.notNull(sessionId, "sessionId is required");
			this.sessionId = sessionId;
		}

		public void open(final Message<?> message) {
			Assert.notNull(message, "message is required");

			this.promise = tcpClient.open();

			this.promise.consume(new Consumer<TcpConnection<String,String>>() {
				@Override
				public void accept(TcpConnection<String, String> connection) {
					connection.in().consume(new Consumer<String>() {
						@Override
						public void accept(String stompFrame) {
							readStompFrame(stompFrame);
						}
					});
					forwardInternal(message, connection);
				}
			});
			this.promise.onError(new Consumer<Throwable>() {
				@Override
				public void accept(Throwable ex) {
					relaySessions.remove(sessionId);
					logger.error("Failed to connect to broker", ex);
					sendError(sessionId, "Failed to connect to message broker " + ex.toString());
				}
			});
		}

		private void readStompFrame(String stompFrame) {

			// heartbeat
			if (StringUtils.isEmpty(stompFrame)) {
				return;
			}

			Message<?> message = stompMessageConverter.toMessage(stompFrame);
			if (logger.isTraceEnabled()) {
				logger.trace("Reading message " + message);
			}

			StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
			if (StompCommand.CONNECTED == headers.getStompCommand()) {
				synchronized(this.monitor) {
					this.isConnected = true;
					flushMessages(this.promise.get());
				}
				return;
			}

			headers.setSessionId(this.sessionId);
			message = MessageBuilder.fromMessage(message).copyHeaders(headers.toMap()).build();
			sendMessageToClient(message);
		}

		protected void sendMessageToClient(Message<?> message) {
			outboundChannel.send(message);
		}

		private void sendError(String sessionId, String errorText) {
			StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.ERROR);
			headers.setSessionId(sessionId);
			headers.setMessage(errorText);
			Message<?> errorMessage = MessageBuilder.withPayload(new byte[0]).copyHeaders(headers.toMap()).build();
			sendMessageToClient(errorMessage);
		}

		public void forward(Message<?> message) {

			if (!this.isConnected) {
				synchronized(this.monitor) {
					if (!this.isConnected) {
						this.messageQueue.add(message);
						if (logger.isTraceEnabled()) {
							logger.trace("Not connected yet, message queued, queue size=" + this.messageQueue.size());
						}
						return;
					}
				}
			}

			TcpConnection<String, String> connection = this.promise.get();

			if (this.messageQueue.isEmpty()) {
				forwardInternal(message, connection);
			}
			else {
				this.messageQueue.add(message);
				flushMessages(connection);
			}
		}

		private boolean forwardInternal(Message<?> message, TcpConnection<String, String> connection) {
			if (logger.isTraceEnabled()) {
				logger.trace("Forwarding message to STOMP broker, message id=" + message.getHeaders().getId());
			}
			byte[] bytes = stompMessageConverter.fromMessage(message);
			connection.send(new String(bytes, Charset.forName("UTF-8")));

			// TODO: detect if send fails and send ERROR downstream (except on DISCONNECT)
			return true;
		}

		private void flushMessages(TcpConnection<String, String> connection) {
			List<Message<?>> messages = new ArrayList<Message<?>>();
			this.messageQueue.drainTo(messages);
			for (Message<?> message : messages) {
				if (!forwardInternal(message, connection)) {
					return;
				}
			}
		}
	}
}
