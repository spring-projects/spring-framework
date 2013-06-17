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
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.PubSubChannelRegistry;
import org.springframework.web.messaging.PubSubHeaders;
import org.springframework.web.messaging.converter.CompositeMessageConverter;
import org.springframework.web.messaging.converter.MessageConverter;
import org.springframework.web.messaging.service.AbstractPubSubMessageHandler;
import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.stomp.StompHeaders;

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
public class StompRelayPubSubMessageHandler extends AbstractPubSubMessageHandler {

	private MessageChannel<Message<?>> clientChannel;

	private final StompMessageConverter stompMessageConverter = new StompMessageConverter();

	private MessageConverter payloadConverter;

	private final TcpClient<String, String> tcpClient;

	private final Map<String, RelaySession> relaySessions = new ConcurrentHashMap<String, RelaySession>();


	/**
	 * @param clientChannel a channel for sending messages from the remote message broker
	 *        back to clients
	 */
	public StompRelayPubSubMessageHandler(PubSubChannelRegistry registry) {

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
	public void handleConnect(Message<?> message) {
		StompHeaders stompHeaders = StompHeaders.fromMessageHeaders(message.getHeaders());
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
		StompHeaders stompHeaders = StompHeaders.fromMessageHeaders(message.getHeaders());
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
		StompCommand command = (StompCommand) message.getHeaders().get(PubSubHeaders.PROTOCOL_MESSAGE_TYPE);
		Assert.notNull(command, "Expected STOMP command: " + message.getHeaders());
		forwardMessage(message, command);
	}

	private void forwardMessage(Message<?> message, StompCommand command) {

		StompHeaders headers = StompHeaders.fromMessageHeaders(message.getHeaders());
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

		private final AtomicBoolean isConnected = new AtomicBoolean(false);

		private final BlockingQueue<Message<?>> messageQueue = new LinkedBlockingQueue<Message<?>>(50);


		public RelaySession(final Message<?> message, final StompHeaders stompHeaders) {

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

			Message<byte[]> message = stompMessageConverter.toMessage(stompFrame, this.sessionId);
			if (logger.isTraceEnabled()) {
				logger.trace("Reading message " + message);
			}

			StompHeaders headers = StompHeaders.fromMessageHeaders(message.getHeaders());
			if (StompCommand.CONNECTED == headers.getStompCommand()) {
				this.isConnected.set(true);
				flushMessages(promise.get());
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
			StompHeaders stompHeaders = StompHeaders.create(StompCommand.ERROR);
			stompHeaders.setSessionId(sessionId);
			stompHeaders.setMessage(errorText);
			Message<byte[]> errorMessage = MessageBuilder.fromPayloadAndHeaders(
					new byte[0], stompHeaders.toMessageHeaders()).build();
			clientChannel.send(errorMessage);
		}

		public void forward(Message<?> message, StompHeaders headers) {

			if (!this.isConnected.get()) {
				message = MessageBuilder.fromPayloadAndHeaders(message.getPayload(), headers.toMessageHeaders()).build();
				if (logger.isTraceEnabled()) {
					logger.trace("Adding to queue message " + message + ", queue size=" + this.messageQueue.size());
				}
				this.messageQueue.add(message);
				return;
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
				StompHeaders headers = StompHeaders.fromMessageHeaders(message.getHeaders());
				if (!forwardInternal(message, headers, connection)) {
					return;
				}
			}
		}

		private boolean forwardInternal(Message<?> message, StompHeaders headers, TcpConnection<String, String> connection) {
			try {
				headers.setStompCommandIfNotSet(StompCommand.SEND);

				MediaType contentType = headers.getContentType();
				byte[] payload = payloadConverter.convertToPayload(message.getPayload(), contentType);
				Message<byte[]> byteMessage = MessageBuilder.fromPayloadAndHeaders(payload, headers.toMessageHeaders()).build();

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
