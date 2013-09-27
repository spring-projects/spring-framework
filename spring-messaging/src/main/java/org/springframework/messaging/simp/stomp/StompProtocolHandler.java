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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.websocket.SubProtocolHandler;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.handler.MutableUserQueueSuffixResolver;
import org.springframework.messaging.simp.handler.SimpleUserQueueSuffixResolver;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * A {@link SubProtocolHandler} for STOMP that supports versions 1.0, 1.1, and 1.2 of the
 * STOMP specification.
 *
 * @author Rossen Stoyanchev
 * @author Andy Wilkinson
 */
public class StompProtocolHandler implements SubProtocolHandler {

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

	private final Log logger = LogFactory.getLog(StompProtocolHandler.class);

	private final StompDecoder stompDecoder = new StompDecoder();

	private final StompEncoder stompEncoder = new StompEncoder();

	private MutableUserQueueSuffixResolver queueSuffixResolver = new SimpleUserQueueSuffixResolver();

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

	@Override
	public List<String> getSupportedProtocols() {
		return Arrays.asList("v10.stomp", "v11.stomp", "v12.stomp");
	}

	/**
	 * Handle incoming WebSocket messages from clients.
	 */
	public void handleMessageFromClient(WebSocketSession session, WebSocketMessage webSocketMessage,
			MessageChannel outputChannel) {

		Message<?> message;
		try {
			Assert.isInstanceOf(TextMessage.class,  webSocketMessage);
			String payload = ((TextMessage)webSocketMessage).getPayload();
			ByteBuffer byteBuffer = ByteBuffer.wrap(payload.getBytes(Charset.forName("UTF-8")));
			message = this.stompDecoder.decode(byteBuffer);
		}
		catch (Throwable error) {
			logger.error("Failed to parse STOMP frame, WebSocket message payload: ", error);
			sendErrorMessage(session, error);
			return;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Message " + message);
		}

		try {
			StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
			headers.setSessionId(session.getId());
			headers.setUser(session.getPrincipal());

			message = MessageBuilder.withPayloadAndHeaders(message.getPayload(), headers).build();
			outputChannel.send(message);
		}
		catch (Throwable t) {
			logger.error("Terminating STOMP session due to failure to send message: ", t);
			sendErrorMessage(session, t);
		}
	}

	/**
	 * Handle STOMP messages going back out to WebSocket clients.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void handleMessageToClient(WebSocketSession session, Message<?> message) {

		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
		if (headers.getCommand() == null && SimpMessageType.MESSAGE == headers.getMessageType()) {
			headers.setCommandIfNotSet(StompCommand.MESSAGE);
		}

		if (headers.getMessageType() == SimpMessageType.CONNECT_ACK) {
			StompHeaderAccessor connectedHeaders = StompHeaderAccessor.create(StompCommand.CONNECTED);
			connectedHeaders.setVersion(getVersion(headers));
			connectedHeaders.setHeartbeat(0, 0);
			headers = connectedHeaders;
		}

		if (headers.getCommand() == StompCommand.CONNECTED) {
			augmentConnectedHeaders(headers, session);
		}

		if (StompCommand.MESSAGE.equals(headers.getCommand()) && (headers.getSubscriptionId() == null)) {
			logger.error("Ignoring message, no subscriptionId header: " + message);
			return;
		}

		if (!(message.getPayload() instanceof byte[])) {
			logger.error("Ignoring message, expected byte[] content: " + message);
			return;
		}

		try {
			message = MessageBuilder.withPayloadAndHeaders(message.getPayload(), headers).build();
			byte[] bytes = this.stompEncoder.encode((Message<byte[]>)message);
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

	private void augmentConnectedHeaders(StompHeaderAccessor headers, WebSocketSession session) {
		Principal principal = session.getPrincipal();
		if (principal != null) {
			headers.setNativeHeader(CONNECTED_USER_HEADER, principal.getName());
			headers.setNativeHeader(QUEUE_SUFFIX_HEADER, session.getId());

			if (this.queueSuffixResolver != null) {
				String suffix = session.getId();
				this.queueSuffixResolver.addQueueSuffix(principal.getName(), session.getId(), suffix);
			}
		}
	}

	protected void sendErrorMessage(WebSocketSession session, Throwable error) {

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.ERROR);
		headers.setMessage(error.getMessage());
		Message<byte[]> message = MessageBuilder.withPayloadAndHeaders(new byte[0], headers).build();
		String payload = new String(this.stompEncoder.encode(message), Charset.forName("UTF-8"));
		try {
			session.sendMessage(new TextMessage(payload));
		}
		catch (Throwable t) {
			// ignore
		}
	}

	@Override
	public String resolveSessionId(Message<?> message) {
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
		return headers.getSessionId();
	}

	@Override
	public void afterSessionStarted(WebSocketSession session, MessageChannel outputChannel) {
	}

	@Override
	public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus, MessageChannel outputChannel) {

		if ((this.queueSuffixResolver != null) && (session.getPrincipal() != null)) {
			this.queueSuffixResolver.removeQueueSuffix(session.getPrincipal().getName(), session.getId());
		}

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		headers.setSessionId(session.getId());
		Message<?> message = MessageBuilder.withPayloadAndHeaders(new byte[0], headers).build();
		outputChannel.send(message);
	}

	private String getVersion(StompHeaderAccessor connectAckHeaders) {
		Message<?> connectMessage =
				(Message<?>) connectAckHeaders.getHeader(StompHeaderAccessor.CONNECT_MESSAGE_HEADER);
		StompHeaderAccessor connectHeaders = StompHeaderAccessor.wrap(connectMessage);

		Set<String> acceptVersions = connectHeaders.getAcceptVersion();
		if (acceptVersions.contains("1.2")) {
			return "1.2";
		}
		else if (acceptVersions.contains("1.1")) {
			return "1.1";
		}
		else if (acceptVersions.isEmpty()) {
			return null;
		}
		else {
			throw new StompConversionException("Unsupported version '" + acceptVersions + "'");
		}
	}

}
