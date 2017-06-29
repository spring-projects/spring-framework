/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.socket.sockjs.client;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;

/**
 * Base class for SockJS client implementations of {@link WebSocketSession}.
 * Provides processing of incoming SockJS message frames and delegates lifecycle
 * events and messages to the (application) {@link WebSocketHandler}.
 * Sub-classes implement actual send as well as disconnect logic.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.1
 */
public abstract class AbstractClientSockJsSession implements WebSocketSession {

	protected final Log logger = LogFactory.getLog(getClass());

	private final TransportRequest request;

	private final WebSocketHandler webSocketHandler;

	private final SettableListenableFuture<WebSocketSession> connectFuture;

	private final Map<String, Object> attributes = new ConcurrentHashMap<>();

	@Nullable
	private volatile State state = State.NEW;

	@Nullable
	private volatile CloseStatus closeStatus;


	protected AbstractClientSockJsSession(TransportRequest request, WebSocketHandler handler,
			SettableListenableFuture<WebSocketSession> connectFuture) {

		Assert.notNull(request, "'request' is required");
		Assert.notNull(handler, "'handler' is required");
		Assert.notNull(connectFuture, "'connectFuture' is required");
		this.request = request;
		this.webSocketHandler = handler;
		this.connectFuture = connectFuture;
	}


	@Override
	public String getId() {
		return this.request.getSockJsUrlInfo().getSessionId();
	}

	@Override
	public URI getUri() {
		return this.request.getSockJsUrlInfo().getSockJsUrl();
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		return this.request.getHandshakeHeaders();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}

	@Override
	public Principal getPrincipal() {
		return this.request.getUser();
	}

	public SockJsMessageCodec getMessageCodec() {
		return this.request.getMessageCodec();
	}

	public WebSocketHandler getWebSocketHandler() {
		return this.webSocketHandler;
	}

	/**
	 * Return a timeout cleanup task to invoke if the SockJS sessions is not
	 * fully established within the retransmission timeout period calculated in
	 * {@code SockJsRequest} based on the duration of the initial SockJS "Info"
	 * request.
	 */
	Runnable getTimeoutTask() {
		return () -> closeInternal(new CloseStatus(2007, "Transport timed out"));
	}

	@Override
	public boolean isOpen() {
		return (this.state == State.OPEN);
	}

	public boolean isDisconnected() {
		return (this.state == State.CLOSING || this.state == State.CLOSED);
	}

	@Override
	public final void sendMessage(WebSocketMessage<?> message) throws IOException {
		if (!(message instanceof TextMessage)) {
			throw new IllegalArgumentException(this + " supports text messages only.");
		}
		if (this.state != State.OPEN) {
			throw new IllegalStateException(this + " is not open: current state " + this.state);
		}

		String payload = ((TextMessage) message).getPayload();
		payload = getMessageCodec().encode(payload);
		payload = payload.substring(1);  // the client-side doesn't need message framing (letter "a")

		TextMessage messageToSend = new TextMessage(payload);
		if (logger.isTraceEnabled()) {
			logger.trace("Sending message " + messageToSend + " in " + this);
		}
		sendInternal(messageToSend);
	}

	protected abstract void sendInternal(TextMessage textMessage) throws IOException;

	@Override
	public final void close() throws IOException {
		close(CloseStatus.NORMAL);
	}

	@Override
	public final void close(CloseStatus status) {
		Assert.isTrue(isUserSetStatus(status), "Invalid close status: " + status);
		if (logger.isDebugEnabled()) {
			logger.debug("Closing session with " +  status + " in " + this);
		}
		closeInternal(status);
	}

	private boolean isUserSetStatus(@Nullable CloseStatus status) {
		return (status != null && (status.getCode() == 1000 ||
				(status.getCode() >= 3000 && status.getCode() <= 4999)));
	}

	protected void closeInternal(CloseStatus status) {
		if (this.state == null) {
			logger.warn("Ignoring close since connect() was never invoked");
			return;
		}
		if (isDisconnected()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Ignoring close (already closing or closed): current state " + this.state);
			}
			return;
		}

		this.state = State.CLOSING;
		this.closeStatus = status;
		try {
			disconnect(status);
		}
		catch (Throwable ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Failed to close " + this, ex);
			}
		}
	}

	protected abstract void disconnect(CloseStatus status) throws IOException;

	public void handleFrame(String payload) {
		SockJsFrame frame = new SockJsFrame(payload);
		switch (frame.getType()) {
			case OPEN:
				handleOpenFrame();
				break;
			case HEARTBEAT:
				if (logger.isTraceEnabled()) {
					logger.trace("Received heartbeat in " + this);
				}
				break;
			case MESSAGE:
				handleMessageFrame(frame);
				break;
			case CLOSE:
				handleCloseFrame(frame);
		}
	}

	private void handleOpenFrame() {
		if (logger.isDebugEnabled()) {
			logger.debug("Processing SockJS open frame in " + this);
		}
		if (this.state == State.NEW) {
			this.state = State.OPEN;
			try {
				this.webSocketHandler.afterConnectionEstablished(this);
				this.connectFuture.set(this);
			}
			catch (Throwable ex) {
				if (logger.isErrorEnabled()) {
					logger.error("WebSocketHandler.afterConnectionEstablished threw exception in " + this, ex);
				}
			}
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Open frame received in " + getId() + " but we're not connecting (current state " +
						this.state + "). The server might have been restarted and lost track of the session.");
			}
			closeInternal(new CloseStatus(1006, "Server lost session"));
		}
	}

	private void handleMessageFrame(SockJsFrame frame) {
		if (!isOpen()) {
			if (logger.isErrorEnabled()) {
				logger.error("Ignoring received message due to state " + this.state + " in " + this);
			}
			return;
		}

		String[] messages = null;
		String frameData = frame.getFrameData();
		if (frameData != null) {
			try {
				messages = getMessageCodec().decode(frameData);
			}
			catch (IOException ex) {
				if (logger.isErrorEnabled()) {
					logger.error("Failed to decode data for SockJS \"message\" frame: " + frame + " in " + this, ex);
				}
				closeInternal(CloseStatus.BAD_DATA);
				return;
			}
		}
		if (messages == null) {
			return;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Processing SockJS message frame " + frame.getContent() + " in " + this);
		}
		for (String message : messages) {
			if (isOpen()) {
				try {
					this.webSocketHandler.handleMessage(this, new TextMessage(message));
				}
				catch (Throwable ex) {
					logger.error("WebSocketHandler.handleMessage threw an exception on " + frame + " in " + this, ex);
				}
			}
		}
	}

	private void handleCloseFrame(SockJsFrame frame) {
		CloseStatus closeStatus = CloseStatus.NO_STATUS_CODE;
		try {
			String frameData = frame.getFrameData();
			if (frameData != null) {
				String[] data = getMessageCodec().decode(frameData);
				if (data != null && data.length == 2) {
					closeStatus = new CloseStatus(Integer.valueOf(data[0]), data[1]);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Processing SockJS close frame with " + closeStatus + " in " + this);
				}
			}
		}
		catch (IOException ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Failed to decode data for " + frame + " in " + this, ex);
			}
		}
		closeInternal(closeStatus);
	}

	public void handleTransportError(Throwable error) {
		try {
			if (logger.isErrorEnabled()) {
				logger.error("Transport error in " + this, error);
			}
			this.webSocketHandler.handleTransportError(this, error);
		}
		catch (Throwable ex) {
			logger.error("WebSocketHandler.handleTransportError threw an exception", ex);
		}
	}

	public void afterTransportClosed(@Nullable CloseStatus closeStatus) {
		CloseStatus cs = this.closeStatus;
		if (cs == null) {
			cs = closeStatus;
			this.closeStatus = closeStatus;
		}
		Assert.state(cs != null, "CloseStatus not available");
		if (logger.isDebugEnabled()) {
			logger.debug("Transport closed with " + cs + " in " + this);
		}

		this.state = State.CLOSED;
		try {
			this.webSocketHandler.afterConnectionClosed(this, cs);
		}
		catch (Throwable ex) {
			logger.error("WebSocketHandler.afterConnectionClosed threw an exception", ex);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id='" + getId() + ", url=" + getUri() + "]";
	}


	private enum State { NEW, OPEN, CLOSING, CLOSED }

}
