/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.util.Assert;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsFrameType;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;

/**
 * Base class for SockJS client implementations of {@link WebSocketSession}.
 * Provides processing of incoming SockJS message frames and delegates lifecycle
 * events and messages to the (application) {@link WebSocketHandler}.
 * Sub-classes implement actual send as well as disconnect logic.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public abstract class AbstractClientSockJsSession implements WebSocketSession {

	protected final Log logger = LogFactory.getLog(getClass());


	private final TransportRequest request;

	private final WebSocketHandler webSocketHandler;

	private final SettableListenableFuture<WebSocketSession> connectFuture;


	private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	private volatile State state = State.NEW;

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
		return new Runnable() {
			@Override
			public void run() {
				closeInternal(new CloseStatus(2007, "Transport timed out"));
			}
		};
	}

	@Override
	public boolean isOpen() {
		return State.OPEN.equals(this.state);
	}

	public boolean isDisconnected() {
		return (State.CLOSING.equals(this.state) || State.CLOSED.equals(this.state));
	}

	@Override
	public final void sendMessage(WebSocketMessage<?> message) throws IOException {
		Assert.state(State.OPEN.equals(this.state), this + " is not open, current state=" + this.state);
		Assert.isInstanceOf(TextMessage.class, message, this + " supports text messages only.");
		String payload = ((TextMessage) message).getPayload();
		payload = getMessageCodec().encode(new String[] { payload });
		payload = payload.substring(1); // the client-side doesn't need message framing (letter "a")
		message = new TextMessage(payload);
		if (logger.isTraceEnabled()) {
			logger.trace("Sending message " + message + " in " + this);
		}
		sendInternal((TextMessage) message);
	}

	protected abstract void sendInternal(TextMessage textMessage) throws IOException;

	@Override
	public final void close() throws IOException {
		close(CloseStatus.NORMAL);
	}

	@Override
	public final void close(CloseStatus status) {
		Assert.isTrue(status != null && isUserSetStatus(status), "Invalid close status: " + status);
		if (logger.isDebugEnabled()) {
			logger.debug("Closing session with " +  status + " in " + this);
		}
		closeInternal(status);
	}

	private boolean isUserSetStatus(CloseStatus status) {
		return (status.getCode() == 1000 || (status.getCode() >= 3000 && status.getCode() <= 4999));
	}

	protected void closeInternal(CloseStatus status) {
		if (this.state == null) {
			logger.warn("Ignoring close since connect() was never invoked");
			return;
		}
		if (State.CLOSING.equals(this.state) || State.CLOSED.equals(this.state)) {
			logger.debug("Ignoring close (already closing or closed), current state=" + this.state);
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
		if (SockJsFrameType.OPEN.equals(frame.getType())) {
			handleOpenFrame();
		}
		else if (SockJsFrameType.MESSAGE.equals(frame.getType())) {
			handleMessageFrame(frame);
		}
		else if (SockJsFrameType.CLOSE.equals(frame.getType())) {
			handleCloseFrame(frame);
		}
		else if (SockJsFrameType.HEARTBEAT.equals(frame.getType())) {
			if (logger.isTraceEnabled()) {
				logger.trace("Received heartbeat in " + this);
			}
		}
		else {
			// should never happen
			throw new IllegalStateException("Unknown SockJS frame type " + frame + " in " + this);
		}
	}

	private void handleOpenFrame() {
		if (logger.isDebugEnabled()) {
			logger.debug("Processing SockJS open frame in " + this);
		}
		if (State.NEW.equals(state)) {
			this.state = State.OPEN;
			try {
				this.webSocketHandler.afterConnectionEstablished(this);
				this.connectFuture.set(this);
			}
			catch (Throwable ex) {
				if (logger.isErrorEnabled()) {
					Class<?> type = this.webSocketHandler.getClass();
					logger.error(type + ".afterConnectionEstablished threw exception in " + this, ex);
				}
			}
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Open frame received in " + getId() + " but we're not" +
						"connecting (current state=" + this.state + "). The server might " +
						"have been restarted and lost track of the session.");
			}
			closeInternal(new CloseStatus(1006, "Server lost session"));
		}
	}

	private void handleMessageFrame(SockJsFrame frame) {
		if (!isOpen()) {
			if (logger.isErrorEnabled()) {
				logger.error("Ignoring received message due to state=" + this.state + " in " + this);
			}
			return;
		}
		String[] messages;
		try {
			messages = getMessageCodec().decode(frame.getFrameData());
		}
		catch (IOException ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Failed to decode data for SockJS \"message\" frame: " + frame + " in " + this, ex);
			}
			closeInternal(CloseStatus.BAD_DATA);
			return;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Processing SockJS message frame " + frame.getContent() + " in " + this);
		}
		for (String message : messages) {
			try {
				if (isOpen()) {
					this.webSocketHandler.handleMessage(this, new TextMessage(message));
				}
			}
			catch (Throwable ex) {
				Class<?> type = this.webSocketHandler.getClass();
				logger.error(type + ".handleMessage threw an exception on " + frame + " in " + this, ex);
			}
		}
	}

	private void handleCloseFrame(SockJsFrame frame) {
		CloseStatus closeStatus = CloseStatus.NO_STATUS_CODE;
		try {
			String[] data = getMessageCodec().decode(frame.getFrameData());
			if (data.length == 2) {
				closeStatus = new CloseStatus(Integer.valueOf(data[0]), data[1]);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Processing SockJS close frame with " + closeStatus + " in " + this);
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
		catch (Exception ex) {
			Class<?> type = this.webSocketHandler.getClass();
			if (logger.isErrorEnabled()) {
				logger.error(type + ".handleTransportError threw an exception", ex);
			}
		}
	}

	public void afterTransportClosed(CloseStatus closeStatus) {
		this.closeStatus = (this.closeStatus != null ? this.closeStatus : closeStatus);
		Assert.state(this.closeStatus != null, "CloseStatus not available");

		if (logger.isDebugEnabled()) {
			logger.debug("Transport closed with " + this.closeStatus + " in " + this);
		}

		this.state = State.CLOSED;
		try {
			this.webSocketHandler.afterConnectionClosed(this, this.closeStatus);
		}
		catch (Exception ex) {
			if (logger.isErrorEnabled()) {
				Class<?> type = this.webSocketHandler.getClass();
				logger.error(type + ".afterConnectionClosed threw an exception", ex);
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id='" + getId() + ", url=" + getUri() + "]";
	}


	private enum State { NEW, OPEN, CLOSING, CLOSED }

}
