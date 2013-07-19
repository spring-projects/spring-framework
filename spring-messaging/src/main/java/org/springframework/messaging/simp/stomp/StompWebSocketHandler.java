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
package org.springframework.messaging.simp.stomp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.handler.MutableUserQueueSuffixResolver;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.TextWebSocketHandlerAdapter;

import reactor.util.Assert;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompWebSocketHandler extends TextWebSocketHandlerAdapter implements MessageHandler {

	/**
	 * The name of the header set on the CONNECTED frame indicating the name of the user
	 * connected authenticated on the WebSocket session.
	 */
	public static final String CONNECTED_USER_HEADER = "user-name";

	/**
	 * A suffix unique to the current session that a client can use to append to
	 * a destination to make it unique.
	 *
	 * @see {@link org.springframework.messaging.simp.handler.UserDestinationMessageHandler}
	 */
	public static final String QUEUE_SUFFIX_HEADER = "queue-suffix";


	private static Log logger = LogFactory.getLog(StompWebSocketHandler.class);

	private MessageChannel dispatchChannel;

	private MutableUserQueueSuffixResolver queueSuffixResolver;

	private final StompMessageConverter stompMessageConverter = new StompMessageConverter();

	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<String, WebSocketSession>();


	/**
	 * @param dispatchChannel the channel to send client STOMP/WebSocket messages to
	 */
	public StompWebSocketHandler(MessageChannel dispatchChannel) {
		Assert.notNull(dispatchChannel, "dispatchChannel is required");
		this.dispatchChannel = dispatchChannel;
	}


	/**
	 * Configure a resolver to use to maintain queue suffixes for user
	 * @see {@link org.springframework.messaging.simp.handler.UserDestinationMessageHandler}
	 */
	public void setUserQueueSuffixResolver(MutableUserQueueSuffixResolver resolver) {
		this.queueSuffixResolver = resolver;
	}

	/**
	 * @return the resolver for queue suffixes for a user
	 */
	public MutableUserQueueSuffixResolver getUserQueueSuffixResolver() {
		return this.queueSuffixResolver;
	}

	public StompMessageConverter getStompMessageConverter() {
		return this.stompMessageConverter;
	}


	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		this.sessions.put(session.getId(), session);
	}

	/**
	 * Handle incoming WebSocket messages from clients.
	 */
	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
		try {
			String payload = textMessage.getPayload();
			Message<?> message = this.stompMessageConverter.toMessage(payload);

			// TODO: validate size limits
			// http://stomp.github.io/stomp-specification-1.2.html#Size_Limits

			if (logger.isTraceEnabled()) {
				logger.trace("Processing STOMP message: " + message);
			}

			try {
				StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
				headers.setSessionId(session.getId());
				headers.setUser(session.getPrincipal());
				message = MessageBuilder.withPayloadAndHeaders(message.getPayload(), headers).build();

				if (SimpMessageType.CONNECT.equals(headers.getMessageType())) {
					handleConnect(session, message);
				}

				this.dispatchChannel.send(message);

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

	protected void handleConnect(WebSocketSession session, Message<?> message) throws IOException {

		StompHeaderAccessor connectHeaders = StompHeaderAccessor.wrap(message);
		StompHeaderAccessor connectedHeaders = StompHeaderAccessor.create(StompCommand.CONNECTED);

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

		Principal principal = session.getPrincipal();
		if (principal != null) {
			connectedHeaders.setNativeHeader(CONNECTED_USER_HEADER, principal.getName());
			connectedHeaders.setNativeHeader(QUEUE_SUFFIX_HEADER, session.getId());

			if (this.queueSuffixResolver != null) {
				String suffix = session.getId();
				this.queueSuffixResolver.addQueueSuffix(principal.getName(), session.getId(), suffix);
			}
		}

		// TODO: security

		Message<?> connectedMessage = MessageBuilder.withPayloadAndHeaders(new byte[0], connectedHeaders).build();
		byte[] bytes = this.stompMessageConverter.fromMessage(connectedMessage);
		session.sendMessage(new TextMessage(new String(bytes, Charset.forName("UTF-8"))));
	}

	protected void sendErrorMessage(WebSocketSession session, Throwable error) {

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.ERROR);
		headers.setMessage(error.getMessage());
		Message<?> message = MessageBuilder.withPayloadAndHeaders(new byte[0], headers).build();
		byte[] bytes = this.stompMessageConverter.fromMessage(message);
		try {
			session.sendMessage(new TextMessage(new String(bytes, Charset.forName("UTF-8"))));
		}
		catch (Throwable t) {
			// ignore
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

		String sessionId = session.getId();
		this.sessions.remove(sessionId);

		if ((this.queueSuffixResolver != null) && (session.getPrincipal() != null)) {
			this.queueSuffixResolver.removeQueueSuffix(session.getPrincipal().getName(), sessionId);
		}

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		headers.setSessionId(sessionId);
		Message<?> message = MessageBuilder.withPayloadAndHeaders(new byte[0], headers).build();
		this.dispatchChannel.send(message);
	}

	/**
	 * Handle STOMP messages going back out to WebSocket clients.
	 */
	@Override
	public void handleMessage(Message<?> message) {

		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
		headers.setCommandIfNotSet(StompCommand.MESSAGE);

		if (StompCommand.CONNECTED.equals(headers.getCommand())) {
			// Ignore for now since we already sent it
			return;
		}

		String sessionId = headers.getSessionId();
		if (sessionId == null) {
			// TODO: failed message delivery mechanism
			logger.error("Ignoring message, no sessionId header: " + message);
			return;
		}

		WebSocketSession session = this.sessions.get(sessionId);
		if (session == null) {
			// TODO: failed message delivery mechanism
			logger.error("Ignoring message, sessionId not found: " + message);
			return;
		}

		if (StompCommand.MESSAGE.equals(headers.getCommand()) && (headers.getSubscriptionId() == null)) {
			// TODO: failed message delivery mechanism
			logger.error("Ignoring message, no subscriptionId header: " + message);
			return;
		}

		if (!(message.getPayload() instanceof byte[])) {
			// TODO: failed message delivery mechanism
			logger.error("Ignoring message, expected byte[] content: " + message);
			return;
		}

		try {
			message = MessageBuilder.withPayloadAndHeaders(message.getPayload(), headers).build();
			byte[] bytes = this.stompMessageConverter.fromMessage(message);
			session.sendMessage(new TextMessage(new String(bytes, Charset.forName("UTF-8"))));
		}
		catch (Throwable t) {
			sendErrorMessage(session, t);
		}
		finally {
			if (StompCommand.ERROR.equals(headers.getCommand())) {
				try {
					session.close(CloseStatus.PROTOCOL_ERROR);
				}
				catch (IOException e) {
				}
			}
		}
	}

}
