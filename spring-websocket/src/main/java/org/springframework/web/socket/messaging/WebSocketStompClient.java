/*
 * Copyright 2002-2024 the original author or authors.
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
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.BufferingStompDecoder;
import org.springframework.messaging.simp.stomp.ConnectionHandlingStompSession;
import org.springframework.messaging.simp.stomp.SplittingStompEncoder;
import org.springframework.messaging.simp.stomp.StompClientSupport;
import org.springframework.messaging.simp.stomp.StompDecoder;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.messaging.tcp.TcpConnectionHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;
import org.springframework.web.socket.sockjs.transport.SockJsSession;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A STOMP over WebSocket client that connects using an implementation of
 * {@link org.springframework.web.socket.client.WebSocketClient WebSocketClient}
 * including {@link org.springframework.web.socket.sockjs.client.SockJsClient
 * SockJsClient}.
 *
 * @author Rossen Stoyanchev
 * @author Injae Kim
 * @since 4.2
 */
public class WebSocketStompClient extends StompClientSupport implements SmartLifecycle {

	private static final Log logger = LogFactory.getLog(WebSocketStompClient.class);


	private final WebSocketClient webSocketClient;

	private int inboundMessageSizeLimit = 64 * 1024;

	@Nullable
	private Integer outboundMessageSizeLimit;

	private boolean autoStartup = true;

	private int phase = DEFAULT_PHASE;

	private volatile boolean running;


	/**
	 * Class constructor. Sets {@link #setDefaultHeartbeat} to "0,0" but will
	 * reset it back to the preferred "10000,10000" when a
	 * {@link #setTaskScheduler} is configured.
	 * @param webSocketClient the WebSocket client to connect with
	 */
	public WebSocketStompClient(WebSocketClient webSocketClient) {
		Assert.notNull(webSocketClient, "WebSocketClient is required");
		this.webSocketClient = webSocketClient;
		setDefaultHeartbeat(new long[] {0, 0});
	}


	/**
	 * Return the configured WebSocketClient.
	 */
	public WebSocketClient getWebSocketClient() {
		return this.webSocketClient;
	}

	/**
	 * {@inheritDoc}
	 * <p>Also automatically sets the {@link #setDefaultHeartbeat defaultHeartbeat}
	 * property to "10000,10000" if it is currently set to "0,0".
	 */
	@Override
	public void setTaskScheduler(@Nullable TaskScheduler taskScheduler) {
		if (!isDefaultHeartbeatEnabled()) {
			setDefaultHeartbeat(new long[] {10000, 10000});
		}
		super.setTaskScheduler(taskScheduler);
	}

	/**
	 * Configure the maximum size allowed for inbound STOMP message.
	 * Since a STOMP message can be received in multiple WebSocket messages,
	 * buffering may be required and this property determines the maximum buffer
	 * size per message.
	 * <p>By default this is set to 64 * 1024 (64K).
	 */
	public void setInboundMessageSizeLimit(int inboundMessageSizeLimit) {
		this.inboundMessageSizeLimit = inboundMessageSizeLimit;
	}

	/**
	 * Get the configured inbound message buffer size in bytes.
	 */
	public int getInboundMessageSizeLimit() {
		return this.inboundMessageSizeLimit;
	}

	/**
	 * Configure the maximum size allowed for outbound STOMP message.
	 * If STOMP message's size exceeds {@link WebSocketStompClient#outboundMessageSizeLimit},
	 * STOMP message is split into multiple frames.
	 * <p>By default this is not set in which case each STOMP message are not split.
	 * @since 6.2
	 */
	public void setOutboundMessageSizeLimit(Integer outboundMessageSizeLimit) {
		this.outboundMessageSizeLimit = outboundMessageSizeLimit;
	}

	/**
	 * Get the configured outbound message buffer size in bytes.
	 * @since 6.2
	 */
	@Nullable
	public Integer getOutboundMessageSizeLimit() {
		return this.outboundMessageSizeLimit;
	}

	/**
	 * Set whether to auto-start the contained WebSocketClient when the Spring
	 * context has been refreshed.
	 * <p>Default is "true".
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * Return the value for the 'autoStartup' property. If "true", this client
	 * will automatically start and stop the contained WebSocketClient.
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * Specify the phase in which the WebSocket client should be started and
	 * subsequently closed. The startup order proceeds from lowest to highest,
	 * and the shutdown order is the reverse of that.
	 * <p>By default this is Integer.MAX_VALUE meaning that the WebSocket client
	 * is started as late as possible and stopped as soon as possible.
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * Return the configured phase.
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			if (getWebSocketClient() instanceof Lifecycle lifecycle) {
				lifecycle.start();
			}
		}

	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			if (getWebSocketClient() instanceof Lifecycle lifecycle) {
				lifecycle.stop();
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	/**
	 * Connect to the given WebSocket URL and notify the given
	 * {@link org.springframework.messaging.simp.stomp.StompSessionHandler}
	 * when connected on the STOMP level after the CONNECTED frame is received.
	 * @param url the url to connect to
	 * @param handler the session handler
	 * @param uriVars the URI variables to expand into the URL
	 * @return a {@code ListenableFuture} for access to the session when ready for use
	 * @deprecated as of 6.0, in favor of {@link #connectAsync(String, StompSessionHandler, Object...)}
	 */
	@Deprecated(since = "6.0")
	public org.springframework.util.concurrent.ListenableFuture<StompSession> connect(
			String url, StompSessionHandler handler, Object... uriVars) {

		return new org.springframework.util.concurrent.CompletableToListenableFutureAdapter<>(
				connectAsync(url, handler, uriVars));
	}

	/**
	 * Connect to the given WebSocket URL and notify the given
	 * {@link org.springframework.messaging.simp.stomp.StompSessionHandler}
	 * when connected on the STOMP level after the CONNECTED frame is received.
	 * @param url the url to connect to
	 * @param handler the session handler
	 * @param uriVars the URI variables to expand into the URL
	 * @return a CompletableFuture for access to the session when ready for use
	 * @since 6.0
	 */
	public CompletableFuture<StompSession> connectAsync(String url, StompSessionHandler handler, Object... uriVars) {
		return connectAsync(url, null, handler, uriVars);
	}

	/**
	 * An overloaded version of
	 * {@link #connect(String, StompSessionHandler, Object...)} that also
	 * accepts {@link WebSocketHttpHeaders} to use for the WebSocket handshake.
	 * @param url the url to connect to
	 * @param handshakeHeaders the headers for the WebSocket handshake
	 * @param handler the session handler
	 * @param uriVariables the URI variables to expand into the URL
	 * @return a {@code ListenableFuture} for access to the session when ready for use
	 * @deprecated as of 6.0, in favor of {@link #connectAsync(String, WebSocketHttpHeaders, StompSessionHandler, Object...)}
	 */
	@Deprecated(since = "6.0")
	public org.springframework.util.concurrent.ListenableFuture<StompSession> connect(
			String url, @Nullable WebSocketHttpHeaders handshakeHeaders,
			StompSessionHandler handler, Object... uriVariables) {

		return new org.springframework.util.concurrent.CompletableToListenableFutureAdapter<>(
				connectAsync(url, handshakeHeaders, null, handler, uriVariables));
	}

	/**
	 * An overloaded version of
	 * {@link #connect(String, StompSessionHandler, Object...)} that also
	 * accepts {@link WebSocketHttpHeaders} to use for the WebSocket handshake.
	 * @param url the url to connect to
	 * @param handshakeHeaders the headers for the WebSocket handshake
	 * @param handler the session handler
	 * @param uriVariables the URI variables to expand into the URL
	 * @return a {@code ListenableFuture} for access to the session when ready for use
	 * @since 6.0
	 */
	public CompletableFuture<StompSession> connectAsync(String url, @Nullable WebSocketHttpHeaders handshakeHeaders,
			StompSessionHandler handler, Object... uriVariables) {

		return connectAsync(url, handshakeHeaders, null, handler, uriVariables);
	}

	/**
	 * An overloaded version of
	 * {@link #connect(String, StompSessionHandler, Object...)} that also accepts
	 * {@link WebSocketHttpHeaders} to use for the WebSocket handshake and
	 * {@link StompHeaders} for the STOMP CONNECT frame.
	 * @param url the url to connect to
	 * @param handshakeHeaders headers for the WebSocket handshake
	 * @param connectHeaders headers for the STOMP CONNECT frame
	 * @param handler the session handler
	 * @param uriVariables the URI variables to expand into the URL
	 * @return a {@code ListenableFuture} for access to the session when ready for use
	 * @deprecated as of 6.0, in favor of {@link #connectAsync(String, WebSocketHttpHeaders, StompHeaders, StompSessionHandler, Object...)}
	 */
	@Deprecated(since = "6.0")
	public org.springframework.util.concurrent.ListenableFuture<StompSession> connect(
			String url, @Nullable WebSocketHttpHeaders handshakeHeaders,
			@Nullable StompHeaders connectHeaders, StompSessionHandler handler, Object... uriVariables) {

		return new org.springframework.util.concurrent.CompletableToListenableFutureAdapter<>(
				connectAsync(url, handshakeHeaders, connectHeaders, handler, uriVariables));
	}

	/**
	 * An overloaded version of
	 * {@link #connect(String, StompSessionHandler, Object...)} that also accepts
	 * {@link WebSocketHttpHeaders} to use for the WebSocket handshake and
	 * {@link StompHeaders} for the STOMP CONNECT frame.
	 * @param url the url to connect to
	 * @param handshakeHeaders headers for the WebSocket handshake
	 * @param connectHeaders headers for the STOMP CONNECT frame
	 * @param handler the session handler
	 * @param uriVariables the URI variables to expand into the URL
	 * @return a CompletableFuture for access to the session when ready for use
	 * @since 6.0
	 */
	public CompletableFuture<StompSession> connectAsync(String url, @Nullable WebSocketHttpHeaders handshakeHeaders,
			@Nullable StompHeaders connectHeaders, StompSessionHandler handler, Object... uriVariables) {

		Assert.notNull(url, "'url' must not be null");
		URI uri = UriComponentsBuilder.fromUriString(url).buildAndExpand(uriVariables).encode().toUri();
		return connectAsync(uri, handshakeHeaders, connectHeaders, handler);
	}

	/**
	 * An overloaded version of
	 * {@link #connect(String, WebSocketHttpHeaders, StompSessionHandler, Object...)}
	 * that accepts a fully prepared {@link java.net.URI}.
	 * @param url the url to connect to
	 * @param handshakeHeaders the headers for the WebSocket handshake
	 * @param connectHeaders headers for the STOMP CONNECT frame
	 * @param sessionHandler the STOMP session handler
	 * @return a {@code ListenableFuture} for access to the session when ready for use
	 * @deprecated as of 6.0, in favor of {@link #connectAsync(URI, WebSocketHttpHeaders, StompHeaders, StompSessionHandler)}
	 */
	@Deprecated(since = "6.0")
	public org.springframework.util.concurrent.ListenableFuture<StompSession> connect(
			URI url, @Nullable WebSocketHttpHeaders handshakeHeaders,
			@Nullable StompHeaders connectHeaders, StompSessionHandler sessionHandler) {

		return new org.springframework.util.concurrent.CompletableToListenableFutureAdapter<>(
				connectAsync(url, handshakeHeaders, connectHeaders, sessionHandler));
	}

	/**
	 * An overloaded version of
	 * {@link #connect(String, WebSocketHttpHeaders, StompSessionHandler, Object...)}
	 * that accepts a fully prepared {@link java.net.URI}.
	 * @param url the url to connect to
	 * @param handshakeHeaders the headers for the WebSocket handshake
	 * @param connectHeaders headers for the STOMP CONNECT frame
	 * @param sessionHandler the STOMP session handler
	 * @return a CompletableFuture for access to the session when ready for use
	 * @since 6.0
	 */
	public CompletableFuture<StompSession> connectAsync(URI url, @Nullable WebSocketHttpHeaders handshakeHeaders,
			@Nullable StompHeaders connectHeaders, StompSessionHandler sessionHandler) {

		Assert.notNull(url, "'url' must not be null");
		ConnectionHandlingStompSession session = createSession(connectHeaders, sessionHandler);
		WebSocketTcpConnectionHandlerAdapter adapter = new WebSocketTcpConnectionHandlerAdapter(session);
		getWebSocketClient()
				.execute(new LoggingWebSocketHandlerDecorator(adapter), handshakeHeaders, url)
				.whenComplete(adapter);
		return session.getSession();
	}

	@Override
	protected StompHeaders processConnectHeaders(@Nullable StompHeaders connectHeaders) {
		connectHeaders = super.processConnectHeaders(connectHeaders);
		if (connectHeaders.isHeartbeatEnabled()) {
			Assert.state(getTaskScheduler() != null, "TaskScheduler must be set if heartbeats are enabled");
		}
		return connectHeaders;
	}


	/**
	 * Adapt WebSocket to the TcpConnectionHandler and TcpConnection contracts.
	 */
	private class WebSocketTcpConnectionHandlerAdapter implements BiConsumer<WebSocketSession, Throwable>,
			WebSocketHandler, TcpConnection<byte[]> {

		private final TcpConnectionHandler<byte[]> stompSession;

		private final StompWebSocketMessageCodec codec =
				new StompWebSocketMessageCodec(getInboundMessageSizeLimit(),getOutboundMessageSizeLimit());

		@Nullable
		private volatile WebSocketSession session;

		private volatile long lastReadTime = -1;

		private volatile long lastWriteTime = -1;

		@Nullable
		private ScheduledFuture<?> readInactivityFuture;

		@Nullable
		private ScheduledFuture<?> writeInactivityFuture;

		public WebSocketTcpConnectionHandlerAdapter(TcpConnectionHandler<byte[]> stompSession) {
			Assert.notNull(stompSession, "TcpConnectionHandler must not be null");
			this.stompSession = stompSession;
		}

		// CompletableFuture callback implementation: handshake outcome

		@Override
		public void accept(@Nullable WebSocketSession webSocketSession, @Nullable Throwable throwable) {
			if (throwable != null) {
				this.stompSession.afterConnectFailure(throwable);
			}
		}

		// WebSocketHandler implementation

		@Override
		public void afterConnectionEstablished(WebSocketSession session) {
			this.session = session;
			this.stompSession.afterConnected(this);
		}

		@Override
		public void handleMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) {
			this.lastReadTime = (this.lastReadTime != -1 ? System.currentTimeMillis() : -1);
			List<Message<byte[]>> messages;
			try {
				messages = this.codec.decode(webSocketMessage);
			}
			catch (Throwable ex) {
				this.stompSession.handleFailure(ex);
				return;
			}
			for (Message<byte[]> message : messages) {
				this.stompSession.handleMessage(message);
			}
		}

		@Override
		public void handleTransportError(WebSocketSession session, Throwable ex) {
			this.stompSession.handleFailure(ex);
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
			this.stompSession.afterConnectionClosed();
		}

		@Override
		public boolean supportsPartialMessages() {
			return false;
		}

		// TcpConnection implementation

		@Override
		public CompletableFuture<Void> sendAsync(Message<byte[]> message) {
			updateLastWriteTime();
			CompletableFuture<Void> future = new CompletableFuture<>();
			try {
				WebSocketSession session = this.session;
				Assert.state(session != null, "No WebSocketSession available");
				if (this.codec.hasSplittingEncoder()) {
					for (WebSocketMessage<?> outMessage : this.codec.encodeAndSplit(message, session.getClass())) {
						session.sendMessage(outMessage);
					}
				}
				else {
					session.sendMessage(this.codec.encode(message, session.getClass()));
				}
				future.complete(null);
			}
			catch (Throwable ex) {
				future.completeExceptionally(ex);
			}
			finally {
				updateLastWriteTime();
			}
			return future;
		}

		private void updateLastWriteTime() {
			long lastWriteTime = this.lastWriteTime;
			if (lastWriteTime != -1) {
				this.lastWriteTime = System.currentTimeMillis();
			}
		}

		@Override
		public void onReadInactivity(final Runnable runnable, final long duration) {
			Assert.state(getTaskScheduler() != null, "No TaskScheduler configured");
			this.lastReadTime = System.currentTimeMillis();
			Duration delay = Duration.ofMillis(duration / 2);
			this.readInactivityFuture = getTaskScheduler().scheduleWithFixedDelay(() -> {
				if (System.currentTimeMillis() - this.lastReadTime > duration) {
					try {
						runnable.run();
					}
					catch (Throwable ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("ReadInactivityTask failure", ex);
						}
					}
				}
			}, delay);
		}

		@Override
		public void onWriteInactivity(final Runnable runnable, final long duration) {
			Assert.state(getTaskScheduler() != null, "No TaskScheduler configured");
			this.lastWriteTime = System.currentTimeMillis();
			Duration delay = Duration.ofMillis(duration / 2);
			this.writeInactivityFuture = getTaskScheduler().scheduleWithFixedDelay(() -> {
				if (System.currentTimeMillis() - this.lastWriteTime > duration) {
					try {
						runnable.run();
					}
					catch (Throwable ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("WriteInactivityTask failure", ex);
						}
					}
				}
			}, delay);
		}

		@Override
		public void close() {
			cancelInactivityTasks();
			WebSocketSession session = this.session;
			if (session != null) {
				try {
					session.close();
				}
				catch (IOException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to close session: " + session.getId(), ex);
					}
				}
			}
		}

		private void cancelInactivityTasks() {
			ScheduledFuture<?> readFuture = this.readInactivityFuture;
			this.readInactivityFuture = null;
			cancelFuture(readFuture);

			ScheduledFuture<?> writeFuture = this.writeInactivityFuture;
			this.writeInactivityFuture = null;
			cancelFuture(writeFuture);

			this.lastReadTime = -1;
			this.lastWriteTime = -1;
		}

		private static void cancelFuture(@Nullable ScheduledFuture<?> future) {
			if (future != null) {
				try {
					future.cancel(true);
				}
				catch (Throwable ex) {
					// Ignore
				}
			}
		}

	}


	/**
	 * Encode and decode STOMP WebSocket messages.
	 */
	private static class StompWebSocketMessageCodec {

		private static final StompEncoder ENCODER = new StompEncoder();

		private static final StompDecoder DECODER = new StompDecoder();

		private final BufferingStompDecoder bufferingDecoder;

		@Nullable
		private final SplittingStompEncoder splittingEncoder;

		public StompWebSocketMessageCodec(int inboundMessageSizeLimit, @Nullable Integer outboundMessageSizeLimit) {
			this.bufferingDecoder = new BufferingStompDecoder(DECODER, inboundMessageSizeLimit);
			this.splittingEncoder = (outboundMessageSizeLimit != null ?
					new SplittingStompEncoder(ENCODER, outboundMessageSizeLimit) : null);
		}

		public List<Message<byte[]>> decode(WebSocketMessage<?> webSocketMessage) {
			List<Message<byte[]>> result = Collections.emptyList();
			ByteBuffer byteBuffer;
			if (webSocketMessage instanceof TextMessage textMessage) {
				byteBuffer = ByteBuffer.wrap(textMessage.asBytes());
			}
			else if (webSocketMessage instanceof BinaryMessage binaryMessage) {
				byteBuffer = binaryMessage.getPayload();
			}
			else {
				return result;
			}
			result = this.bufferingDecoder.decode(byteBuffer);
			if (result.isEmpty()) {
				if (logger.isTraceEnabled()) {
					logger.trace("Incomplete STOMP frame content received, bufferSize=" +
							this.bufferingDecoder.getBufferSize() + ", bufferSizeLimit=" +
							this.bufferingDecoder.getBufferSizeLimit() + ".");
				}
			}
			return result;
		}

		public boolean hasSplittingEncoder() {
			return (this.splittingEncoder != null);
		}

		public WebSocketMessage<?> encode(Message<byte[]> message, Class<? extends WebSocketSession> sessionType) {
			StompHeaderAccessor accessor = getStompHeaderAccessor(message);
			byte[] payload = message.getPayload();
			byte[] frame = ENCODER.encode(accessor.getMessageHeaders(), payload);
			return (useBinary(accessor, payload, sessionType) ? new BinaryMessage(frame) : new TextMessage(frame));
		}

		public List<WebSocketMessage<?>> encodeAndSplit(Message<byte[]> message, Class<? extends WebSocketSession> sessionType) {
			Assert.state(this.splittingEncoder != null, "No SplittingEncoder");
			StompHeaderAccessor accessor = getStompHeaderAccessor(message);
			byte[] payload = message.getPayload();
			List<byte[]> frames = this.splittingEncoder.encode(accessor.getMessageHeaders(), payload);
			boolean useBinary = useBinary(accessor, payload, sessionType);

			List<WebSocketMessage<?>> messages = new ArrayList<>(frames.size());
			frames.forEach(frame -> messages.add(useBinary ? new BinaryMessage(frame) : new TextMessage(frame)));
			return messages;
		}

		private static StompHeaderAccessor getStompHeaderAccessor(Message<byte[]> message) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			Assert.notNull(accessor, "No StompHeaderAccessor available");
			return accessor;
		}

		private static boolean useBinary(
				StompHeaderAccessor accessor, byte[] payload, Class<? extends WebSocketSession> sessionType) {

			return (payload.length > 0 &&
					!(SockJsSession.class.isAssignableFrom(sessionType)) &&
					MimeTypeUtils.APPLICATION_OCTET_STREAM.isCompatibleWith(accessor.getContentType()));
		}
	}

}
