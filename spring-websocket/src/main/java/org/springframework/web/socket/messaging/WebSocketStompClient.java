/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.messaging;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.BufferingStompDecoder;
import org.springframework.messaging.simp.stomp.ConnectionHandlingStompSession;
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
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.sockjs.transport.SockJsSession;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A STOMP over WebSocket client that connects using an implementation of
 * {@link org.springframework.web.socket.client.WebSocketClient WebSocketClient}
 * including {@link org.springframework.web.socket.sockjs.client.SockJsClient
 * SockJsClient}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class WebSocketStompClient extends StompClientSupport implements SmartLifecycle {

	private static final Log logger = LogFactory.getLog(WebSocketStompClient.class);


	private final WebSocketClient webSocketClient;

	private int inboundMessageSizeLimit = 64 * 1024;

	private boolean autoStartup = true;

	private boolean running = false;

	private int phase = Integer.MAX_VALUE;


	/**
	 * Class constructor. Sets {@link #setDefaultHeartbeat} to "0,0" but will
	 * reset it back to the preferred "10000,10000" when a
	 * {@link #setTaskScheduler} is configured.
	 * @param webSocketClient the WebSocket client to connect with
	 */
	public WebSocketStompClient(WebSocketClient webSocketClient) {
		Assert.notNull(webSocketClient, "'webSocketClient' is required.");
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
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		if (taskScheduler != null && !isDefaultHeartbeatEnabled()) {
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

	@Override
	public boolean isRunning() {
		return this.running;
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
			if (getWebSocketClient() instanceof Lifecycle) {
				((Lifecycle) getWebSocketClient()).start();
			}
		}

	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			if (getWebSocketClient() instanceof Lifecycle) {
				((Lifecycle) getWebSocketClient()).stop();
			}
		}
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	/**
	 * Connect to the given WebSocket URL and notify the given
	 * {@link org.springframework.messaging.simp.stomp.StompSessionHandler}
	 * when connected on the STOMP level after the CONNECTED frame is received.
	 * @param url the url to connect to
	 * @param handler the session handler
	 * @param uriVars URI variables to expand into the URL
	 * @return ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(String url, StompSessionHandler handler, Object... uriVars) {
		return connect(url, null, handler, uriVars);
	}

	/**
	 * An overloaded version of
	 * {@link #connect(String, StompSessionHandler, Object...)} that also
	 * accepts {@link WebSocketHttpHeaders} to use for the WebSocket handshake.
	 * @param url the url to connect to
	 * @param handshakeHeaders the headers for the WebSocket handshake
	 * @param handler the session handler
	 * @param uriVariables URI variables to expand into the URL
	 * @return ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(String url, WebSocketHttpHeaders handshakeHeaders,
			StompSessionHandler handler, Object... uriVariables) {

		return connect(url, handshakeHeaders, null, handler, uriVariables);
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
	 * @param uriVariables URI variables to expand into the URL
	 * @return ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(String url, WebSocketHttpHeaders handshakeHeaders,
			StompHeaders connectHeaders, StompSessionHandler handler, Object... uriVariables) {

		Assert.notNull(url, "uriTemplate must not be null");
		URI uri = UriComponentsBuilder.fromUriString(url).buildAndExpand(uriVariables).encode().toUri();
		return connect(uri, handshakeHeaders, connectHeaders, handler);
	}

	/**
	 * An overloaded version of
	 * {@link #connect(String, WebSocketHttpHeaders, StompSessionHandler, Object...)}
	 * that accepts a fully prepared {@link java.net.URI}.
	 * @param url the url to connect to
	 * @param handshakeHeaders the headers for the WebSocket handshake
	 * @param connectHeaders headers for the STOMP CONNECT frame
	 * @param sessionHandler the STOMP session handler
	 * @return ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(URI url, WebSocketHttpHeaders handshakeHeaders,
			StompHeaders connectHeaders, StompSessionHandler sessionHandler) {

		Assert.notNull(url, "'uri' must not be null");
		ConnectionHandlingStompSession session = createSession(connectHeaders, sessionHandler);
		WebSocketTcpConnectionHandlerAdapter adapter = new WebSocketTcpConnectionHandlerAdapter(session);
		getWebSocketClient().doHandshake(adapter, handshakeHeaders, url).addCallback(adapter);
		return session.getSessionFuture();
	}

	@Override
	protected StompHeaders processConnectHeaders(StompHeaders connectHeaders) {
		connectHeaders = super.processConnectHeaders(connectHeaders);
		if (connectHeaders.isHeartbeatEnabled()) {
			Assert.notNull(getTaskScheduler(), "TaskScheduler cannot be null if heartbeats are enabled.");
		}
		return connectHeaders;
	}


	/**
	 * Adapt WebSocket to the TcpConnectionHandler and TcpConnection contracts.
	 */
	private class WebSocketTcpConnectionHandlerAdapter implements ListenableFutureCallback<WebSocketSession>,
			WebSocketHandler, TcpConnection<byte[]> {

		private final TcpConnectionHandler<byte[]> connectionHandler;

		private final StompWebSocketMessageCodec codec = new StompWebSocketMessageCodec(getInboundMessageSizeLimit());

		private volatile WebSocketSession session;

		private volatile long lastReadTime = -1;

		private volatile long lastWriteTime = -1;

		private final List<ScheduledFuture<?>> inactivityTasks = new ArrayList<ScheduledFuture<?>>(2);

		public WebSocketTcpConnectionHandlerAdapter(TcpConnectionHandler<byte[]> connectionHandler) {
			Assert.notNull(connectionHandler);
			this.connectionHandler = connectionHandler;
		}

		// ListenableFutureCallback implementation: handshake outcome

		@Override
		public void onSuccess(WebSocketSession webSocketSession) {
		}

		@Override
		public void onFailure(Throwable ex) {
			this.connectionHandler.afterConnectFailure(ex);
		}

		// WebSocketHandler implementation

		@Override
		public void afterConnectionEstablished(WebSocketSession session) {
			this.session = session;
			this.connectionHandler.afterConnected(this);
		}

		@Override
		public void handleMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) {
			this.lastReadTime = (this.lastReadTime != -1 ? System.currentTimeMillis() : -1);
			List<Message<byte[]>> messages;
			try {
				messages = this.codec.decode(webSocketMessage);
			}
			catch (Throwable ex) {
				this.connectionHandler.handleFailure(ex);
				return;
			}
			for (Message<byte[]> message : messages) {
				this.connectionHandler.handleMessage(message);
			}
		}

		@Override
		public void handleTransportError(WebSocketSession session, Throwable ex) throws Exception {
			this.connectionHandler.handleFailure(ex);
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
			cancelInactivityTasks();
			this.connectionHandler.afterConnectionClosed();
		}

		private void cancelInactivityTasks() {
			for (ScheduledFuture<?> task : this.inactivityTasks) {
				try {
					task.cancel(true);
				}
				catch (Throwable ex) {
					// Ignore
				}
			}
			this.lastReadTime = -1;
			this.lastWriteTime = -1;
			this.inactivityTasks.clear();
		}

		@Override
		public boolean supportsPartialMessages() {
			return false;
		}

		// TcpConnection implementation

		@Override
		public ListenableFuture<Void> send(Message<byte[]> message) {
			updateLastWriteTime();
			SettableListenableFuture<Void> future = new SettableListenableFuture<Void>();
			try {
				this.session.sendMessage(this.codec.encode(message, this.session.getClass()));
				future.set(null);
			}
			catch (Throwable ex) {
				future.setException(ex);
			}
			finally {
				updateLastWriteTime();
			}
			return future;
		}

		private void updateLastWriteTime() {
			this.lastWriteTime = (this.lastWriteTime != -1 ? System.currentTimeMillis() : -1);
		}

		@Override
		public void onReadInactivity(final Runnable runnable, final long duration) {
			Assert.notNull(getTaskScheduler(), "No scheduler configured.");
			this.lastReadTime = System.currentTimeMillis();
			this.inactivityTasks.add(getTaskScheduler().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					if (System.currentTimeMillis() - lastReadTime > duration) {
						try {
							runnable.run();
						}
						catch (Throwable ex) {
							if (logger.isDebugEnabled()) {
								logger.debug("ReadInactivityTask failure", ex);
							}
						}
					}
				}
			}, duration / 2));
		}

		@Override
		public void onWriteInactivity(final Runnable runnable, final long duration) {
			Assert.notNull(getTaskScheduler(), "No scheduler configured.");
			this.lastWriteTime = System.currentTimeMillis();
			this.inactivityTasks.add(getTaskScheduler().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					if (System.currentTimeMillis() - lastWriteTime > duration) {
						try {
							runnable.run();
						}
						catch (Throwable ex) {
							if (logger.isDebugEnabled()) {
								logger.debug("WriteInactivityTask failure", ex);
							}
						}
					}
				}
			}, duration / 2));
		}

		@Override
		public void close() {
			try {
				this.session.close();
			}
			catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to close session: " + this.session.getId(), ex);
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

		public StompWebSocketMessageCodec(int messageSizeLimit) {
			this.bufferingDecoder = new BufferingStompDecoder(DECODER, messageSizeLimit);
		}

		public List<Message<byte[]>> decode(WebSocketMessage<?> webSocketMessage) {
			List<Message<byte[]>> result = Collections.<Message<byte[]>>emptyList();
			ByteBuffer byteBuffer;
			if (webSocketMessage instanceof TextMessage) {
				byteBuffer = ByteBuffer.wrap(((TextMessage) webSocketMessage).asBytes());
			}
			else if (webSocketMessage instanceof BinaryMessage) {
				byteBuffer = ((BinaryMessage) webSocketMessage).getPayload();
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

		public WebSocketMessage<?> encode(Message<byte[]> message, Class<? extends WebSocketSession> sessionType) {
			StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
			Assert.notNull(accessor);
			byte[] payload = message.getPayload();
			byte[] bytes = ENCODER.encode(accessor.getMessageHeaders(), payload);

			boolean useBinary = (payload.length > 0  &&
					!(SockJsSession.class.isAssignableFrom(sessionType)) &&
					MimeTypeUtils.APPLICATION_OCTET_STREAM.isCompatibleWith(accessor.getContentType()));

			return (useBinary ? new BinaryMessage(bytes) : new TextMessage(bytes));
		}
	}

}
