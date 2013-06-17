/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.web.messaging.stomp.support;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.PubSubChannelRegistry;
import org.springframework.web.messaging.PubSubHeaders;
import org.springframework.web.messaging.converter.CompositeMessageConverter;
import org.springframework.web.messaging.converter.MessageConverter;
import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.stomp.StompConversionException;
import org.springframework.web.messaging.stomp.StompHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.TextWebSocketHandlerAdapter;

import reactor.util.Assert;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompWebSocketHandler extends TextWebSocketHandlerAdapter implements MessageHandler<Message<?>> {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private static Log logger = LogFactory.getLog(StompWebSocketHandler.class);

	private MessageChannel outputChannel;

	private final StompMessageConverter stompMessageConverter = new StompMessageConverter();

	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<String, WebSocketSession>();

	private MessageConverter payloadConverter = new CompositeMessageConverter(null);


	public StompWebSocketHandler(PubSubChannelRegistry registry) {
		Assert.notNull(registry, "registry is required");
		this.outputChannel = registry.getClientInputChannel();
	}

	public void setMessageConverters(List<MessageConverter> converters) {
		this.payloadConverter = new CompositeMessageConverter(converters);
	}

	public StompMessageConverter getStompMessageConverter() {
		return this.stompMessageConverter;
	}

	protected WebSocketSession getWebSocketSession(String sessionId) {
		return this.sessions.get(sessionId);
	}


	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		Assert.notNull(this.outputChannel, "No output channel for STOMP messages.");
		this.sessions.put(session.getId(), session);
	}

	/**
	 * Handle incoming WebSocket messages from clients.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
		try {
			String payload = textMessage.getPayload();
			Message<byte[]> message = this.stompMessageConverter.toMessage(payload, session.getId());

			// TODO: validate size limits
			// http://stomp.github.io/stomp-specification-1.2.html#Size_Limits

			if (logger.isTraceEnabled()) {
				logger.trace("Processing STOMP message: " + message);
			}

			try {
				StompHeaders stompHeaders = StompHeaders.fromMessageHeaders(message.getHeaders());
				MessageType messageType = stompHeaders.getMessageType();
				if (MessageType.CONNECT.equals(messageType)) {
					handleConnect(session, message);
				}
				else if (MessageType.MESSAGE.equals(messageType)) {
					handlePublish(message);
				}
				else if (MessageType.SUBSCRIBE.equals(messageType)) {
					handleSubscribe(message);
				}
				else if (MessageType.UNSUBSCRIBE.equals(messageType)) {
					handleUnsubscribe(message);
				}
				else if (MessageType.DISCONNECT.equals(messageType)) {
					handleDisconnect(message);
				}
				this.outputChannel.send(message);
			}
			catch (Throwable t) {
				logger.error("Terminating STOMP session due to failure to send message: ", t);
				sendErrorMessage(session, t);
			}

			// TODO: send RECEIPT message if incoming message has "receipt" header
			// http://stomp.github.io/stomp-specification-1.2.html#Header_receipt

		}
		catch (Throwable error) {
			sendErrorMessage(session, error);
		}
	}

	protected void handleConnect(final WebSocketSession session, Message<byte[]> message) throws IOException {

		StompHeaders connectHeaders = StompHeaders.fromMessageHeaders(message.getHeaders());
		StompHeaders connectedHeaders = StompHeaders.create(StompCommand.CONNECTED);

		Set<String> acceptVersions = connectHeaders.getAcceptVersion();
		if (acceptVersions.contains("1.2")) {
			connectedHeaders.setAcceptVersion("1.2");
		}
		else if (acceptVersions.contains("1.1")) {
			connectedHeaders.setAcceptVersion("1.1");
		}
		else if (acceptVersions.isEmpty()) {
			// 1.0
		}
		else {
			throw new StompConversionException("Unsupported version '" + acceptVersions + "'");
		}
		connectedHeaders.setHeartbeat(0,0); // TODO

		// TODO: security

		Message<byte[]> connectedMessage = MessageBuilder.fromPayloadAndHeaders(EMPTY_PAYLOAD,
				connectedHeaders.toMessageHeaders()).build();
		byte[] bytes = getStompMessageConverter().fromMessage(connectedMessage);
		session.sendMessage(new TextMessage(new String(bytes, Charset.forName("UTF-8"))));
	}

	protected void handlePublish(Message<byte[]> stompMessage) {
	}

	protected void handleSubscribe(Message<byte[]> message) {
		// TODO: need a way to communicate back if subscription was successfully created or
		// not in which case an ERROR should be sent back and close the connection
		// http://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE
	}

	protected void handleUnsubscribe(Message<byte[]> message) {
	}

	protected void handleDisconnect(Message<byte[]> stompMessage) {
	}

	protected void sendErrorMessage(WebSocketSession session, Throwable error) {

		StompHeaders headers = StompHeaders.create(StompCommand.ERROR);
		headers.setMessage(error.getMessage());

		Message<byte[]> message = MessageBuilder.fromPayloadAndHeaders(EMPTY_PAYLOAD,
				headers.toMessageHeaders()).build();
		byte[] bytes = this.stompMessageConverter.fromMessage(message);

		try {
			session.sendMessage(new TextMessage(new String(bytes, Charset.forName("UTF-8"))));
		}
		catch (Throwable t) {
			// ignore
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		this.sessions.remove(session.getId());
		PubSubHeaders headers = PubSubHeaders.create(MessageType.DISCONNECT);
		headers.setSessionId(session.getId());
		Message<?> message = MessageBuilder.fromPayloadAndHeaders(new byte[0], headers.toMessageHeaders()).build();
		this.outputChannel.send(message);
	}

	/**
	 * Handle STOMP messages going back out to WebSocket clients.
	 */
	@Override
	public void handleMessage(Message<?> message) {

		StompHeaders headers = StompHeaders.fromMessageHeaders(message.getHeaders());
		headers.setStompCommandIfNotSet(StompCommand.MESSAGE);

		if (StompCommand.CONNECTED.equals(headers.getStompCommand())) {
			// Ignore for now since we already sent it
			return;
		}

		String sessionId = headers.getSessionId();
		if (sessionId == null) {
			logger.error("No \"sessionId\" header in message: " + message);
		}
		WebSocketSession session = getWebSocketSession(sessionId);
		if (session == null) {
			logger.error("Session not found: " + message);
		}

		byte[] payload;
		try {
			MediaType contentType = headers.getContentType();
			payload = payloadConverter.convertToPayload(message.getPayload(), contentType);
		}
		catch (Throwable t) {
			logger.error("Failed to send " + message, t);
			return;
		}

		try {
			Message<byte[]> byteMessage = MessageBuilder.fromPayloadAndHeaders(payload,
					headers.toMessageHeaders()).build();
			byte[] bytes = getStompMessageConverter().fromMessage(byteMessage);
			session.sendMessage(new TextMessage(new String(bytes, Charset.forName("UTF-8"))));
		}
		catch (Throwable t) {
			sendErrorMessage(session, t);
		}
		finally {
			if (StompCommand.ERROR.equals(headers.getStompCommand())) {
				try {
					session.close(CloseStatus.PROTOCOL_ERROR);
				}
				catch (IOException e) {
				}
			}
		}
	}

}
