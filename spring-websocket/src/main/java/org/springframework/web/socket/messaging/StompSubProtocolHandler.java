/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpAttributes;
import org.springframework.messaging.simp.SimpAttributesContextHolder;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.BufferingStompDecoder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.AbstractMessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ImmutableMessageChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.SessionLimitExceededException;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;
import org.springframework.web.socket.sockjs.transport.SockJsSession;

/**
 * A {@link SubProtocolHandler} for STOMP that supports versions 1.0, 1.1, and 1.2
 * of the STOMP specification.
 *
 * @author Rossen Stoyanchev
 * @author Andy Wilkinson
 * @since 4.0
 */
public class StompSubProtocolHandler implements SubProtocolHandler, ApplicationEventPublisherAware {

	/**
	 * This handler supports assembling large STOMP messages split into multiple
	 * WebSocket messages and STOMP clients (like stomp.js) indeed split large STOMP
	 * messages at 16K boundaries. Therefore the WebSocket server input message
	 * buffer size must allow 16K at least plus a little extra for SockJS framing.
	 */
	public static final int MINIMUM_WEBSOCKET_MESSAGE_SIZE = 16 * 1024 + 256;

	/**
	 * The name of the header set on the CONNECTED frame indicating the name
	 * of the user authenticated on the WebSocket session.
	 */
	public static final String CONNECTED_USER_HEADER = "user-name";

	private static final String[] SUPPORTED_VERSIONS = {"1.2", "1.1", "1.0"};

	private static final Log logger = LogFactory.getLog(StompSubProtocolHandler.class);

	private static final byte[] EMPTY_PAYLOAD = new byte[0];


	@Nullable
	private StompSubProtocolErrorHandler errorHandler;

	private int messageSizeLimit = 64 * 1024;

	private StompEncoder stompEncoder = new StompEncoder();

	private StompDecoder stompDecoder = new StompDecoder();

	private final Map<String, BufferingStompDecoder> decoders = new ConcurrentHashMap<>();

	@Nullable
	private MessageHeaderInitializer headerInitializer;

	private final Map<String, Principal> stompAuthentications = new ConcurrentHashMap<>();

	@Nullable
	private Boolean immutableMessageInterceptorPresent;

	@Nullable
	private ApplicationEventPublisher eventPublisher;

	private final Stats stats = new Stats();


	/**
	 * Configure a handler for error messages sent to clients which allows
	 * customizing the error messages or preventing them from being sent.
	 * <p>By default this isn't configured in which case an ERROR frame is sent
	 * with a message header reflecting the error.
	 * @param errorHandler the error handler
	 */
	public void setErrorHandler(StompSubProtocolErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Return the configured error handler.
	 */
	@Nullable
	public StompSubProtocolErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	/**
	 * Configure the maximum size allowed for an incoming STOMP message.
	 * Since a STOMP message can be received in multiple WebSocket messages,
	 * buffering may be required and therefore it is necessary to know the maximum
	 * allowed message size.
	 * <p>By default this property is set to 64K.
	 * @since 4.0.3
	 */
	public void setMessageSizeLimit(int messageSizeLimit) {
		this.messageSizeLimit = messageSizeLimit;
	}

	/**
	 * Get the configured message buffer size limit in bytes.
	 * @since 4.0.3
	 */
	public int getMessageSizeLimit() {
		return this.messageSizeLimit;
	}

	/**
	 * Configure a {@link StompEncoder} for encoding STOMP frames.
	 * @since 4.3.5
	 */
	public void setEncoder(StompEncoder encoder) {
		this.stompEncoder = encoder;
	}

	/**
	 * Configure a {@link StompDecoder} for decoding STOMP frames.
	 * @since 4.3.5
	 */
	public void setDecoder(StompDecoder decoder) {
		this.stompDecoder = decoder;
	}

	/**
	 * Configure a {@link MessageHeaderInitializer} to apply to the headers of all
	 * messages created from decoded STOMP frames and other messages sent to the
	 * client inbound channel.
	 * <p>By default this property is not set.
	 */
	public void setHeaderInitializer(@Nullable MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
		this.stompDecoder.setHeaderInitializer(headerInitializer);
	}

	/**
	 * Return the configured header initializer.
	 */
	@Nullable
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}

	@Override
	public List<String> getSupportedProtocols() {
		return Arrays.asList("v10.stomp", "v11.stomp", "v12.stomp");
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.eventPublisher = applicationEventPublisher;
	}

	/**
	 * Return a String describing internal state and counters.
	 */
	public String getStatsInfo() {
		return this.stats.toString();
	}


	/**
	 * Handle incoming WebSocket messages from clients.
	 */
	public void handleMessageFromClient(WebSocketSession session,
			WebSocketMessage<?> webSocketMessage, MessageChannel outputChannel) {

		List<Message<byte[]>> messages;
		try {
			ByteBuffer byteBuffer;
			if (webSocketMessage instanceof TextMessage) {
				byteBuffer = ByteBuffer.wrap(((TextMessage) webSocketMessage).asBytes());
			}
			else if (webSocketMessage instanceof BinaryMessage) {
				byteBuffer = ((BinaryMessage) webSocketMessage).getPayload();
			}
			else {
				return;
			}

			BufferingStompDecoder decoder = this.decoders.get(session.getId());
			if (decoder == null) {
				throw new IllegalStateException("No decoder for session id '" + session.getId() + "'");
			}

			messages = decoder.decode(byteBuffer);
			if (messages.isEmpty()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Incomplete STOMP frame content received in session " +
							session + ", bufferSize=" + decoder.getBufferSize() +
							", bufferSizeLimit=" + decoder.getBufferSizeLimit() + ".");
				}
				return;
			}
		}
		catch (Throwable ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Failed to parse " + webSocketMessage +
						" in session " + session.getId() + ". Sending STOMP ERROR to client.", ex);
			}
			handleError(session, ex, null);
			return;
		}

		for (Message<byte[]> message : messages) {
			try {
				StompHeaderAccessor headerAccessor =
						MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
				Assert.state(headerAccessor != null, "No StompHeaderAccessor");

				headerAccessor.setSessionId(session.getId());
				headerAccessor.setSessionAttributes(session.getAttributes());
				headerAccessor.setUser(getUser(session));
				headerAccessor.setHeader(SimpMessageHeaderAccessor.HEART_BEAT_HEADER, headerAccessor.getHeartbeat());
				if (!detectImmutableMessageInterceptor(outputChannel)) {
					headerAccessor.setImmutable();
				}

				if (logger.isTraceEnabled()) {
					logger.trace("From client: " + headerAccessor.getShortLogMessage(message.getPayload()));
				}

				StompCommand command = headerAccessor.getCommand();
				boolean isConnect = StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command);
				if (isConnect) {
					this.stats.incrementConnectCount();
				}
				else if (StompCommand.DISCONNECT.equals(command)) {
					this.stats.incrementDisconnectCount();
				}

				try {
					SimpAttributesContextHolder.setAttributesFromMessage(message);
					boolean sent = outputChannel.send(message);

					if (sent) {
						if (isConnect) {
							Principal user = headerAccessor.getUser();
							if (user != null && user != session.getPrincipal()) {
								this.stompAuthentications.put(session.getId(), user);
							}
						}
						if (this.eventPublisher != null) {
							Principal user = getUser(session);
							if (isConnect) {
								publishEvent(this.eventPublisher, new SessionConnectEvent(this, message, user));
							}
							else if (StompCommand.SUBSCRIBE.equals(command)) {
								publishEvent(this.eventPublisher, new SessionSubscribeEvent(this, message, user));
							}
							else if (StompCommand.UNSUBSCRIBE.equals(command)) {
								publishEvent(this.eventPublisher, new SessionUnsubscribeEvent(this, message, user));
							}
						}
					}
				}
				finally {
					SimpAttributesContextHolder.resetAttributes();
				}
			}
			catch (Throwable ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to send client message to application via MessageChannel" +
							" in session " + session.getId() + ". Sending STOMP ERROR to client.", ex);
				}
				handleError(session, ex, message);
			}
		}
	}

	@Nullable
	private Principal getUser(WebSocketSession session) {
		Principal user = this.stompAuthentications.get(session.getId());
		return (user != null ? user : session.getPrincipal());
	}

	private void handleError(WebSocketSession session, Throwable ex, @Nullable Message<byte[]> clientMessage) {
		if (getErrorHandler() == null) {
			sendErrorMessage(session, ex);
			return;
		}
		Message<byte[]> message = getErrorHandler().handleClientMessageProcessingError(clientMessage, ex);
		if (message == null) {
			return;
		}
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		Assert.state(accessor != null, "No StompHeaderAccessor");
		sendToClient(session, accessor, message.getPayload());
	}

	/**
	 * Invoked when no
	 * {@link #setErrorHandler(StompSubProtocolErrorHandler) errorHandler}
	 * is configured to send an ERROR frame to the client.
	 */
	private void sendErrorMessage(WebSocketSession session, Throwable error) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
		headerAccessor.setMessage(error.getMessage());

		byte[] bytes = this.stompEncoder.encode(headerAccessor.getMessageHeaders(), EMPTY_PAYLOAD);
		try {
			session.sendMessage(new TextMessage(bytes));
		}
		catch (Throwable ex) {
			// Could be part of normal workflow (e.g. browser tab closed)
			logger.debug("Failed to send STOMP ERROR to client", ex);
		}
		finally {
			try {
				session.close(CloseStatus.PROTOCOL_ERROR);
			}
			catch (IOException ex) {
				// Ignore
			}
		}
	}

	private boolean detectImmutableMessageInterceptor(MessageChannel channel) {
		if (this.immutableMessageInterceptorPresent != null) {
			return this.immutableMessageInterceptorPresent;
		}

		if (channel instanceof AbstractMessageChannel) {
			for (ChannelInterceptor interceptor : ((AbstractMessageChannel) channel).getInterceptors()) {
				if (interceptor instanceof ImmutableMessageChannelInterceptor) {
					this.immutableMessageInterceptorPresent = true;
					return true;
				}
			}
		}
		this.immutableMessageInterceptorPresent = false;
		return false;
	}

	private void publishEvent(ApplicationEventPublisher publisher, ApplicationEvent event) {
		try {
			publisher.publishEvent(event);
		}
		catch (Throwable ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Error publishing " + event, ex);
			}
		}
	}

	/**
	 * Handle STOMP messages going back out to WebSocket clients.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void handleMessageToClient(WebSocketSession session, Message<?> message) {
		if (!(message.getPayload() instanceof byte[])) {
			if (logger.isErrorEnabled()) {
				logger.error("Expected byte[] payload. Ignoring " + message + ".");
			}
			return;
		}

		StompHeaderAccessor accessor = getStompHeaderAccessor(message);
		StompCommand command = accessor.getCommand();

		if (StompCommand.MESSAGE.equals(command)) {
			if (accessor.getSubscriptionId() == null && logger.isWarnEnabled()) {
				logger.warn("No STOMP \"subscription\" header in " + message);
			}
			String origDestination = accessor.getFirstNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION);
			if (origDestination != null) {
				accessor = toMutableAccessor(accessor, message);
				accessor.removeNativeHeader(SimpMessageHeaderAccessor.ORIGINAL_DESTINATION);
				accessor.setDestination(origDestination);
			}
		}
		else if (StompCommand.CONNECTED.equals(command)) {
			this.stats.incrementConnectedCount();
			accessor = afterStompSessionConnected(message, accessor, session);
			if (this.eventPublisher != null) {
				try {
					SimpAttributes simpAttributes = new SimpAttributes(session.getId(), session.getAttributes());
					SimpAttributesContextHolder.setAttributes(simpAttributes);
					Principal user = getUser(session);
					publishEvent(this.eventPublisher, new SessionConnectedEvent(this, (Message<byte[]>) message, user));
				}
				finally {
					SimpAttributesContextHolder.resetAttributes();
				}
			}
		}

		byte[] payload = (byte[]) message.getPayload();
		if (StompCommand.ERROR.equals(command) && getErrorHandler() != null) {
			Message<byte[]> errorMessage = getErrorHandler().handleErrorMessageToClient((Message<byte[]>) message);
			if (errorMessage != null) {
				accessor = MessageHeaderAccessor.getAccessor(errorMessage, StompHeaderAccessor.class);
				Assert.state(accessor != null, "No StompHeaderAccessor");
				payload = errorMessage.getPayload();
			}
		}
		sendToClient(session, accessor, payload);
	}

	private void sendToClient(WebSocketSession session, StompHeaderAccessor stompAccessor, byte[] payload) {
		StompCommand command = stompAccessor.getCommand();
		try {
			byte[] bytes = this.stompEncoder.encode(stompAccessor.getMessageHeaders(), payload);
			boolean useBinary = (payload.length > 0 && !(session instanceof SockJsSession) &&
					MimeTypeUtils.APPLICATION_OCTET_STREAM.isCompatibleWith(stompAccessor.getContentType()));
			if (useBinary) {
				session.sendMessage(new BinaryMessage(bytes));
			}
			else {
				session.sendMessage(new TextMessage(bytes));
			}
		}
		catch (SessionLimitExceededException ex) {
			// Bad session, just get out
			throw ex;
		}
		catch (Throwable ex) {
			// Could be part of normal workflow (e.g. browser tab closed)
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to send WebSocket message to client in session " + session.getId(), ex);
			}
			command = StompCommand.ERROR;
		}
		finally {
			if (StompCommand.ERROR.equals(command)) {
				try {
					session.close(CloseStatus.PROTOCOL_ERROR);
				}
				catch (IOException ex) {
					// Ignore
				}
			}
		}
	}

	private StompHeaderAccessor getStompHeaderAccessor(Message<?> message) {
		MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class);
		if (accessor instanceof StompHeaderAccessor) {
			return (StompHeaderAccessor) accessor;
		}
		else {
			StompHeaderAccessor stompAccessor = StompHeaderAccessor.wrap(message);
			SimpMessageType messageType = SimpMessageHeaderAccessor.getMessageType(message.getHeaders());
			if (SimpMessageType.CONNECT_ACK.equals(messageType)) {
				stompAccessor = convertConnectAcktoStompConnected(stompAccessor);
			}
			else if (SimpMessageType.DISCONNECT_ACK.equals(messageType)) {
				String receipt = getDisconnectReceipt(stompAccessor);
				if (receipt != null) {
					stompAccessor = StompHeaderAccessor.create(StompCommand.RECEIPT);
					stompAccessor.setReceiptId(receipt);
				}
				else {
					stompAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
					stompAccessor.setMessage("Session closed.");
				}
			}
			else if (SimpMessageType.HEARTBEAT.equals(messageType)) {
				stompAccessor = StompHeaderAccessor.createForHeartbeat();
			}
			else if (stompAccessor.getCommand() == null || StompCommand.SEND.equals(stompAccessor.getCommand())) {
				stompAccessor.updateStompCommandAsServerMessage();
			}
			return stompAccessor;
		}
	}

	/**
	 * The simple broker produces {@code SimpMessageType.CONNECT_ACK} that's not STOMP
	 * specific and needs to be turned into a STOMP CONNECTED frame.
	 */
	private StompHeaderAccessor convertConnectAcktoStompConnected(StompHeaderAccessor connectAckHeaders) {
		String name = StompHeaderAccessor.CONNECT_MESSAGE_HEADER;
		Message<?> message = (Message<?>) connectAckHeaders.getHeader(name);
		if (message == null) {
			throw new IllegalStateException("Original STOMP CONNECT not found in " + connectAckHeaders);
		}

		StompHeaderAccessor connectHeaders = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		StompHeaderAccessor connectedHeaders = StompHeaderAccessor.create(StompCommand.CONNECTED);

		if (connectHeaders != null) {
			Set<String> acceptVersions = connectHeaders.getAcceptVersion();
			connectedHeaders.setVersion(
					Arrays.stream(SUPPORTED_VERSIONS)
							.filter(acceptVersions::contains)
							.findAny()
							.orElseThrow(() -> new IllegalArgumentException(
									"Unsupported STOMP version '" + acceptVersions + "'")));
		}

		long[] heartbeat = (long[]) connectAckHeaders.getHeader(SimpMessageHeaderAccessor.HEART_BEAT_HEADER);
		if (heartbeat != null) {
			connectedHeaders.setHeartbeat(heartbeat[0], heartbeat[1]);
		}
		else {
			connectedHeaders.setHeartbeat(0, 0);
		}

		return connectedHeaders;
	}

	@Nullable
	private String getDisconnectReceipt(SimpMessageHeaderAccessor simpHeaders) {
		String name = StompHeaderAccessor.DISCONNECT_MESSAGE_HEADER;
		Message<?> message = (Message<?>) simpHeaders.getHeader(name);
		if (message != null) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			if (accessor != null) {
				return accessor.getReceipt();
			}
		}
		return null;
	}

	protected StompHeaderAccessor toMutableAccessor(StompHeaderAccessor headerAccessor, Message<?> message) {
		return (headerAccessor.isMutable() ? headerAccessor : StompHeaderAccessor.wrap(message));
	}

	private StompHeaderAccessor afterStompSessionConnected(Message<?> message, StompHeaderAccessor accessor,
			WebSocketSession session) {

		Principal principal = getUser(session);
		if (principal != null) {
			accessor = toMutableAccessor(accessor, message);
			accessor.setNativeHeader(CONNECTED_USER_HEADER, principal.getName());
		}

		long[] heartbeat = accessor.getHeartbeat();
		if (heartbeat[1] > 0) {
			session = WebSocketSessionDecorator.unwrap(session);
			if (session instanceof SockJsSession) {
				((SockJsSession) session).disableHeartbeat();
			}
		}

		return accessor;
	}

	@Override
	@Nullable
	public String resolveSessionId(Message<?> message) {
		return SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
	}

	@Override
	public void afterSessionStarted(WebSocketSession session, MessageChannel outputChannel) {
		if (session.getTextMessageSizeLimit() < MINIMUM_WEBSOCKET_MESSAGE_SIZE) {
			session.setTextMessageSizeLimit(MINIMUM_WEBSOCKET_MESSAGE_SIZE);
		}
		this.decoders.put(session.getId(), new BufferingStompDecoder(this.stompDecoder, getMessageSizeLimit()));
	}

	@Override
	public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus, MessageChannel outputChannel) {
		this.decoders.remove(session.getId());

		Message<byte[]> message = createDisconnectMessage(session);
		SimpAttributes simpAttributes = SimpAttributes.fromMessage(message);
		try {
			SimpAttributesContextHolder.setAttributes(simpAttributes);
			if (this.eventPublisher != null) {
				Principal user = getUser(session);
				publishEvent(this.eventPublisher, new SessionDisconnectEvent(this, message, session.getId(), closeStatus, user));
			}
			outputChannel.send(message);
		}
		finally {
			this.stompAuthentications.remove(session.getId());
			SimpAttributesContextHolder.resetAttributes();
			simpAttributes.sessionCompleted();
		}
	}

	private Message<byte[]> createDisconnectMessage(WebSocketSession session) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
		if (getHeaderInitializer() != null) {
			getHeaderInitializer().initHeaders(headerAccessor);
		}

		headerAccessor.setSessionId(session.getId());
		headerAccessor.setSessionAttributes(session.getAttributes());

		Principal user = getUser(session);
		if (user != null) {
			headerAccessor.setUser(user);
		}

		return MessageBuilder.createMessage(EMPTY_PAYLOAD, headerAccessor.getMessageHeaders());
	}

	@Override
	public String toString() {
		return "StompSubProtocolHandler" + getSupportedProtocols();
	}


	private static class Stats {

		private final AtomicInteger connect = new AtomicInteger();

		private final AtomicInteger connected = new AtomicInteger();

		private final AtomicInteger disconnect = new AtomicInteger();

		public void incrementConnectCount() {
			this.connect.incrementAndGet();
		}

		public void incrementConnectedCount() {
			this.connected.incrementAndGet();
		}

		public void incrementDisconnectCount() {
			this.disconnect.incrementAndGet();
		}

		public String toString() {
			return "processed CONNECT(" + this.connect.get() + ")-CONNECTED(" +
					this.connected.get() + ")-DISCONNECT(" + this.disconnect.get() + ")";
		}
	}

}
