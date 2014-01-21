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

package org.springframework.web.socket.messaging;

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
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompConversionException;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.messaging.simp.user.UserSessionRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * A {@link SubProtocolHandler} for STOMP that supports versions 1.0, 1.1, and 1.2
 * of the STOMP specification.
 *
 * @author Rossen Stoyanchev
 * @author Andy Wilkinson
 * @since 4.0
 */
public class StompSubProtocolHandler implements SubProtocolHandler {

	/**
	 * The name of the header set on the CONNECTED frame indicating the name of the user
	 * authenticated on the WebSocket session.
	 */
	public static final String CONNECTED_USER_HEADER = "user-name";

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private static final Log logger = LogFactory.getLog(StompSubProtocolHandler.class);


	private final StompDecoder stompDecoder = new StompDecoder();

	private final StompEncoder stompEncoder = new StompEncoder();

	private UserSessionRegistry userSessionRegistry;


	/**
	 * Provide a registry with which to register active user session ids.
	 * @see org.springframework.messaging.simp.user.UserDestinationMessageHandler
	 */
	public void setUserSessionRegistry(UserSessionRegistry registry) {
		this.userSessionRegistry = registry;
	}

	/**
	 * @return the configured UserSessionRegistry.
	 */
	public UserSessionRegistry getUserSessionRegistry() {
		return this.userSessionRegistry;
	}

	@Override
	public List<String> getSupportedProtocols() {
		return Arrays.asList("v10.stomp", "v11.stomp", "v12.stomp");
	}

	/**
	 * Handle incoming WebSocket messages from clients.
	 */
	public void handleMessageFromClient(WebSocketSession session,
			WebSocketMessage<?> webSocketMessage, MessageChannel outputChannel) {

		Message<?> message = null;
		Throwable decodeFailure = null;
		try {
			Assert.isInstanceOf(TextMessage.class,  webSocketMessage);
			String payload = ((TextMessage) webSocketMessage).getPayload();
			ByteBuffer byteBuffer = ByteBuffer.wrap(payload.getBytes(UTF8_CHARSET));

			message = this.stompDecoder.decode(byteBuffer);
			if (message == null) {
				decodeFailure = new IllegalStateException("Not a valid STOMP frame: " + payload);
			}
		}
		catch (Throwable ex) {
			decodeFailure = ex;
		}

		if (decodeFailure != null) {
			logger.error("Failed to parse WebSocket message as STOMP frame", decodeFailure);
			sendErrorMessage(session, decodeFailure);
			return;
		}

		try {
			StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
			if (logger.isTraceEnabled()) {
				if (SimpMessageType.HEARTBEAT.equals(headers.getMessageType())) {
					logger.trace("Received heartbeat from client session=" + session.getId());
				}
				else {
					logger.trace("Received message from client session=" + session.getId());
				}
			}

			headers.setSessionId(session.getId());
			headers.setUser(session.getPrincipal());

			message = MessageBuilder.withPayload(message.getPayload()).setHeaders(headers).build();
			outputChannel.send(message);
		}
		catch (Throwable ex) {
			logger.error("Terminating STOMP session due to failure to send message", ex);
			sendErrorMessage(session, ex);
		}
	}

	protected void sendErrorMessage(WebSocketSession session, Throwable error) {

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.ERROR);
		headers.setMessage(error.getMessage());
		Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
		String payload = new String(this.stompEncoder.encode(message), UTF8_CHARSET);
		try {
			session.sendMessage(new TextMessage(payload));
		}
		catch (Throwable ex) {
			// ignore
		}
	}

	/**
	 * Handle STOMP messages going back out to WebSocket clients.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void handleMessageToClient(WebSocketSession session, Message<?> message) {

		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);

		if (headers.getMessageType() == SimpMessageType.CONNECT_ACK) {
			StompHeaderAccessor connectedHeaders = StompHeaderAccessor.create(StompCommand.CONNECTED);
			connectedHeaders.setVersion(getVersion(headers));
			connectedHeaders.setHeartbeat(0, 0); // no heart-beat support with simple broker
			headers = connectedHeaders;
		}
		else if (SimpMessageType.MESSAGE.equals(headers.getMessageType())) {
			headers.updateStompCommandAsServerMessage();
		}

		if (headers.getCommand() == StompCommand.CONNECTED) {
			afterStompSessionConnected(headers, session);
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
			message = MessageBuilder.withPayload(message.getPayload()).setHeaders(headers).build();
			byte[] bytes = this.stompEncoder.encode((Message<byte[]>) message);

			synchronized(session) {
				session.sendMessage(new TextMessage(new String(bytes, UTF8_CHARSET)));
			}

		}
		catch (Throwable ex) {
			sendErrorMessage(session, ex);
		}
		finally {
			if (StompCommand.ERROR.equals(headers.getCommand())) {
				try {
					session.close(CloseStatus.PROTOCOL_ERROR);
				}
				catch (IOException ex) {
					// Ignore
				}
			}
		}
	}

	private String getVersion(StompHeaderAccessor connectAckHeaders) {

		String name = StompHeaderAccessor.CONNECT_MESSAGE_HEADER;
		Message<?> connectMessage = (Message<?>) connectAckHeaders.getHeader(name);
		StompHeaderAccessor connectHeaders = StompHeaderAccessor.wrap(connectMessage);
		Assert.notNull(connectMessage, "CONNECT_ACK does not contain original CONNECT " + connectAckHeaders);

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

	private void afterStompSessionConnected(StompHeaderAccessor headers, WebSocketSession session) {
		Principal principal = session.getPrincipal();
		if (principal != null) {
			headers.setNativeHeader(CONNECTED_USER_HEADER, principal.getName());
			if (this.userSessionRegistry != null) {
				String userName = resolveNameForUserSessionRegistry(principal);
				this.userSessionRegistry.registerSessionId(userName, session.getId());
			}
		}
	}

	private String resolveNameForUserSessionRegistry(Principal principal) {
		String userName = principal.getName();
		if (principal instanceof DestinationUserNameProvider) {
			userName = ((DestinationUserNameProvider) principal).getDestinationUserName();
		}
		return userName;
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

		Principal principal = session.getPrincipal();
		if ((this.userSessionRegistry != null) && (principal != null)) {
			String userName = resolveNameForUserSessionRegistry(principal);
			this.userSessionRegistry.unregisterSessionId(userName, session.getId());
		}

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		headers.setSessionId(session.getId());
		Message<?> message = MessageBuilder.withPayload(new byte[0]).setHeaders(headers).build();
		outputChannel.send(message);
	}

}
