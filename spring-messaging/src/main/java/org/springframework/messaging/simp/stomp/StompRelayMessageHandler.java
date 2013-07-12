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
import java.util.concurrent.TimeUnit;

import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.handler.AbstractSimpMessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
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
public class StompRelayMessageHandler extends AbstractSimpMessageHandler implements SmartLifecycle {

	private static final String STOMP_RELAY_SYSTEM_SESSION_ID = "stompRelaySystemSessionId";


	private MessageChannel outboundChannel;

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
	 */
	public StompRelayMessageHandler(MessageChannel outboundChannel) {
		Assert.notNull(outboundChannel, "outboundChannel is required");
		this.outboundChannel = outboundChannel;
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
	protected Collection<SimpMessageType> getSupportedMessageTypes() {
		return null;
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

			this.environment = new Environment();
			this.tcpClient = new TcpClientSpec<String, String>(NettyTcpClient.class)
					.env(this.environment)
					.codec(new DelimitedCodec<String, String>((byte) 0, true, StandardCodecs.STRING_CODEC))
					.connect(this.relayHost, this.relayPort)
					.get();

			StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
			headers.setAcceptVersion("1.1,1.2");
			headers.setLogin(this.systemLogin);
			headers.setPasscode(this.systemPasscode);
			headers.setHeartbeat(0,0); // TODO
			Message<?> message = MessageBuilder.withPayload(
					new byte[0]).copyHeaders(headers.toNativeHeaderMap()).build();

			RelaySession session = new RelaySession(message, headers) {
				@Override
				protected void sendMessageToClient(Message<?> message) {
					// TODO: check for ERROR frame (reconnect?)
				}
			};
			this.relaySessions.put(STOMP_RELAY_SYSTEM_SESSION_ID, session);

			this.running = true;
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			try {
				this.tcpClient.close().await(5000, TimeUnit.MILLISECONDS);
				this.environment.shutdown();
			}
			catch (InterruptedException e) {
				// ignore
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
	public void handleConnect(Message<?> message) {
		StompHeaderAccessor stompHeaders = StompHeaderAccessor.wrap(message);
		String sessionId = stompHeaders.getSessionId();
		if (sessionId == null) {
			logger.error("No sessionId in message " + message);
			return;
		}
		RelaySession relaySession = new RelaySession(message, stompHeaders);
		this.relaySessions.put(sessionId, relaySession);
	}

	@Override
	public void handlePublish(Message<?> message) {
		forwardMessage(message, StompCommand.SEND);
	}

	@Override
	public void handleSubscribe(Message<?> message) {
		forwardMessage(message, StompCommand.SUBSCRIBE);
	}

	@Override
	public void handleUnsubscribe(Message<?> message) {
		forwardMessage(message, StompCommand.UNSUBSCRIBE);
	}

	@Override
	public void handleDisconnect(Message<?> message) {
		StompHeaderAccessor stompHeaders = StompHeaderAccessor.wrap(message);
		if (stompHeaders.getStompCommand() != null) {
			forwardMessage(message, StompCommand.DISCONNECT);
		}
		String sessionId = stompHeaders.getSessionId();
		if (sessionId == null) {
			logger.error("No sessionId in message " + message);
			return;
		}
	}

	@Override
	public void handleOther(Message<?> message) {
		StompCommand command = (StompCommand) message.getHeaders().get(SimpMessageHeaderAccessor.PROTOCOL_MESSAGE_TYPE);
		Assert.notNull(command, "Expected STOMP command: " + message.getHeaders());
		forwardMessage(message, command);
	}

	private void forwardMessage(Message<?> message, StompCommand command) {

		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
		headers.setStompCommandIfNotSet(command);

		String sessionId = headers.getSessionId();
		if (sessionId == null) {
			if (StompCommand.SEND.equals(command)) {
				sessionId = STOMP_RELAY_SYSTEM_SESSION_ID;
			}
			else {
				logger.error("No sessionId in message " + message);
				return;
			}
		}

		RelaySession session = this.relaySessions.get(sessionId);
		if (session == null) {
			logger.warn("Session id=" + sessionId + " not found. Message cannot be forwarded: " + message);
			return;
		}

		session.forward(message, headers);
	}


	private class RelaySession {

		private final String sessionId;

		private final Promise<TcpConnection<String, String>> promise;

		private final BlockingQueue<Message<?>> messageQueue = new LinkedBlockingQueue<Message<?>>(50);

		private final Object monitor = new Object();

		private volatile boolean isConnected = false;


		public RelaySession(final Message<?> message, final StompHeaderAccessor stompHeaders) {

			Assert.notNull(message, "message is required");
			Assert.notNull(stompHeaders, "stompHeaders is required");

			this.sessionId = stompHeaders.getSessionId();
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
					stompHeaders.setHeartbeat(0,0); // TODO
					forwardInternal(message, stompHeaders, connection);
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

			// TODO: ATM no way to detect closed socket
		}

		private void readStompFrame(String stompFrame) {

			if (StringUtils.isEmpty(stompFrame)) {
				// heartbeat?
				return;
			}

			Message<?> message = stompMessageConverter.toMessage(stompFrame, this.sessionId);
			if (logger.isTraceEnabled()) {
				logger.trace("Reading message " + message);
			}

			StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
			if (StompCommand.CONNECTED == headers.getStompCommand()) {
				synchronized(this.monitor) {
					this.isConnected = true;
					flushMessages(promise.get());
				}
				return;
			}
			if (StompCommand.ERROR == headers.getStompCommand()) {
				if (logger.isDebugEnabled()) {
					logger.warn("STOMP ERROR: " + headers.getMessage() + ". Removing session: " + this.sessionId);
				}
				relaySessions.remove(this.sessionId);
			}
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

		public void forward(Message<?> message, StompHeaderAccessor headers) {

			if (!this.isConnected) {
				synchronized(this.monitor) {
					if (!this.isConnected) {
						this.messageQueue.add(message);
						if (logger.isTraceEnabled()) {
							logger.trace("Queued message " + message + ", queue size=" + this.messageQueue.size());
						}
						return;
					}
				}
			}

			TcpConnection<String, String> connection = this.promise.get();

			if (this.messageQueue.isEmpty()) {
				forwardInternal(message, headers, connection);
			}
			else {
				this.messageQueue.add(message);
				flushMessages(connection);
			}
		}

		private void flushMessages(TcpConnection<String, String> connection) {
			List<Message<?>> messages = new ArrayList<Message<?>>();
			this.messageQueue.drainTo(messages);
			for (Message<?> message : messages) {
				StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
				if (!forwardInternal(message, headers, connection)) {
					return;
				}
			}
		}

		private boolean forwardInternal(Message<?> message, StompHeaderAccessor headers, TcpConnection<String, String> connection) {
			try {
				headers.setStompCommandIfNotSet(StompCommand.SEND);

				if (logger.isTraceEnabled()) {
					logger.trace("Forwarding message " + message);
				}

				byte[] bytes = stompMessageConverter.fromMessage(message);
				connection.send(new String(bytes, Charset.forName("UTF-8")));
			}
			catch (Throwable ex) {
				logger.error("Failed to forward message " + message, ex);
				connection.close();
				sendError(this.sessionId, "Failed to forward message " + message + ": " + ex.getMessage());
				return false;
			}
			return true;
		}
	}

}
