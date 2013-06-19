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

package org.springframework.web.messaging.stomp.support;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.PubSubChannelRegistry;
import org.springframework.web.messaging.converter.CompositeMessageConverter;
import org.springframework.web.messaging.converter.MessageConverter;
import org.springframework.web.messaging.service.AbstractPubSubMessageHandler;
import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.support.PubSubHeaderAccesssor;

import reactor.core.Environment;
import reactor.core.Promise;
import reactor.fn.Consumer;
import reactor.tcp.TcpClient;
import reactor.tcp.TcpConnection;
import reactor.tcp.encoding.DelimitedCodec;
import reactor.tcp.encoding.StandardCodecs;
import reactor.tcp.netty.NettyTcpClient;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("rawtypes")
public class StompRelayPubSubMessageHandler<M extends Message> extends AbstractPubSubMessageHandler<M> {

	private MessageChannel<M> clientChannel;

	private final StompMessageConverter<M> stompMessageConverter = new StompMessageConverter<M>();

	private MessageConverter payloadConverter;

	private final TcpClient<String, String> tcpClient;

	private final Map<String, RelaySession> relaySessions = new ConcurrentHashMap<String, RelaySession>();


	/**
	 * @param clientChannel a channel for sending messages from the remote message broker
	 *        back to clients
	 */
	public StompRelayPubSubMessageHandler(PubSubChannelRegistry<M, ?> registry) {

		Assert.notNull(registry, "registry is required");
		this.clientChannel = registry.getClientOutputChannel();

		this.tcpClient = new TcpClient.Spec<String, String>(NettyTcpClient.class)
				.using(new Environment())
				.codec(new DelimitedCodec<String, String>((byte) 0, true, StandardCodecs.STRING_CODEC))
				.connect("127.0.0.1", 61613)
				.get();

		this.payloadConverter = new CompositeMessageConverter(null);
	}

	public void setMessageConverters(List<MessageConverter> converters) {
		this.payloadConverter = new CompositeMessageConverter(converters);
	}

	@Override
	protected Collection<MessageType> getSupportedMessageTypes() {
		return null;
	}

	@Override
	public void handleConnect(M message) {
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
	public void handlePublish(M message) {
		forwardMessage(message, StompCommand.SEND);
	}

	@Override
	public void handleSubscribe(M message) {
		forwardMessage(message, StompCommand.SUBSCRIBE);
	}

	@Override
	public void handleUnsubscribe(M message) {
		forwardMessage(message, StompCommand.UNSUBSCRIBE);
	}

	@Override
	public void handleDisconnect(M message) {
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
	public void handleOther(M message) {
		StompCommand command = (StompCommand) message.getHeaders().get(PubSubHeaderAccesssor.PROTOCOL_MESSAGE_TYPE);
		Assert.notNull(command, "Expected STOMP command: " + message.getHeaders());
		forwardMessage(message, command);
	}

	private void forwardMessage(M message, StompCommand command) {

		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
		headers.setStompCommandIfNotSet(command);

		String sessionId = headers.getSessionId();
		if (sessionId == null) {
			logger.error("No sessionId in message " + message);
			return;
		}

		RelaySession session = this.relaySessions.get(sessionId);
		if (session == null) {
			// TODO: default (non-user) session for sending messages?
			logger.warn("No relay session for " + sessionId + ". Message '" + message + "' cannot be forwarded");
			return;
		}

		session.forward(message, headers);
	}


	private final class RelaySession {

		private final String sessionId;

		private final Promise<TcpConnection<String, String>> promise;

		private final BlockingQueue<M> messageQueue = new LinkedBlockingQueue<M>(50);

		private final Object monitor = new Object();

		private volatile boolean isConnected = false;


		public RelaySession(final M message, final StompHeaderAccessor stompHeaders) {

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
					stompHeaders.setHeartbeat(0, 0); // TODO
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

			M message = stompMessageConverter.toMessage(stompFrame, this.sessionId);
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
			clientChannel.send(message);
		}

		private void sendError(String sessionId, String errorText) {
			StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.ERROR);
			headers.setSessionId(sessionId);
			headers.setMessage(errorText);
			@SuppressWarnings("unchecked")
			M errorMessage = (M) MessageBuilder.withPayload(new byte[0]).copyHeaders(headers.toHeaders()).build();
			clientChannel.send(errorMessage);
		}

		public void forward(M message, StompHeaderAccessor headers) {

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
			List<M> messages = new ArrayList<M>();
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

				MediaType contentType = headers.getContentType();
				byte[] payload = payloadConverter.convertToPayload(message.getPayload(), contentType);
				@SuppressWarnings("unchecked")
				M byteMessage = (M) MessageBuilder.withPayload(payload).copyHeaders(headers.toHeaders()).build();

				if (logger.isTraceEnabled()) {
					logger.trace("Forwarding message " + byteMessage);
				}

				byte[] bytesToWrite = stompMessageConverter.fromMessage(byteMessage);
				connection.send(new String(bytesToWrite, Charset.forName("UTF-8")));
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
