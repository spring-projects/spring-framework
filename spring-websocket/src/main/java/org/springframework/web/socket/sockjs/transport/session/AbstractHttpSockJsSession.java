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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.springframework.http.server.ServerHttpAsyncResponseControl;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpAsyncRequestControl;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame.FrameFormat;

/**
 * An abstract base class for use with HTTP transport based SockJS sessions.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHttpSockJsSession extends AbstractSockJsSession {

	private FrameFormat frameFormat;

	private final BlockingQueue<String> messageCache = new ArrayBlockingQueue<String>(100);

	private ServerHttpRequest request;

	private ServerHttpResponse response;

	private ServerHttpAsyncResponseControl asyncControl;

	private String protocol;


	public AbstractHttpSockJsSession(String sessionId, SockJsServiceConfig config, WebSocketHandler handler) {
		super(sessionId, config, handler);
	}


	/**
	 * Unlike WebSocket where sub-protocol negotiation is part of the
	 * initial handshake, in HTTP transports the same negotiation must
	 * be emulated and the selected protocol set through this setter.
	 *
	 * @param protocol the sub-protocol to set
	 */
	public void setAcceptedProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * Return the selected sub-protocol to use.
	 */
	public String getAcceptedProtocol() {
		return this.protocol;
	}

	public synchronized void setInitialRequest(ServerHttpRequest request, ServerHttpResponse response,
			FrameFormat frameFormat) throws SockJsException {

		udpateRequest(request, response, frameFormat);
		try {
			writePrelude();
			writeFrame(SockJsFrame.openFrame());
		}
		catch (Throwable t) {
			tryCloseWithSockJsTransportError(t, CloseStatus.SERVER_ERROR);
			throw new SockJsTransportFailureException("Failed to send \"open\" frame", getId(), t);
		}
		try {
			delegateConnectionEstablished();
		}
		catch (Throwable t) {
			throw new SockJsException("Unhandled exception from WebSocketHandler", getId(), t);
		}
	}

	protected void writePrelude() throws IOException {
	}

	public synchronized void setLongPollingRequest(ServerHttpRequest request, ServerHttpResponse response,
			FrameFormat frameFormat) throws SockJsException {

		udpateRequest(request, response, frameFormat);
		if (isClosed()) {
			logger.debug("Connection already closed (but not removed yet)");
			writeFrame(SockJsFrame.closeFrameGoAway());
			return;
		}
		try {
			this.asyncControl.start(-1);
			scheduleHeartbeat();
			tryFlushCache();
		}
		catch (Throwable t) {
			tryCloseWithSockJsTransportError(t, CloseStatus.SERVER_ERROR);
			throw new SockJsTransportFailureException("Failed to flush messages", getId(), t);
		}
	}

	private void udpateRequest(ServerHttpRequest request, ServerHttpResponse response, FrameFormat frameFormat) {
		Assert.notNull(request, "expected request");
		Assert.notNull(response, "expected response");
		Assert.notNull(frameFormat, "expected frameFormat");
		this.request = request;
		this.response = response;
		this.asyncControl = new ServletServerHttpAsyncRequestControl(this.request, this.response);
		this.frameFormat = frameFormat;
	}


	@Override
	public synchronized boolean isActive() {
		return ((this.asyncControl != null) && (!this.asyncControl.isCompleted()));
	}

	protected BlockingQueue<String> getMessageCache() {
		return this.messageCache;
	}

	protected ServerHttpRequest getRequest() {
		return this.request;
	}

	protected ServerHttpResponse getResponse() {
		return this.response;
	}

	@Override
	protected final synchronized void sendMessageInternal(String message) throws SockJsTransportFailureException {
		this.messageCache.add(message);
		tryFlushCache();
	}

	private void tryFlushCache() throws SockJsTransportFailureException {
		if (isActive() && !getMessageCache().isEmpty()) {
			logger.trace("Flushing messages");
			flushCache();
		}
	}

	/**
	 * Only called if the connection is currently active
	 */
	protected abstract void flushCache() throws SockJsTransportFailureException;

	@Override
	protected void disconnect(CloseStatus status) {
		resetRequest();
	}

	protected synchronized void resetRequest() {
		updateLastActiveTime();
		if (isActive() && this.asyncControl.hasStarted()) {
			try {
				logger.debug("Completing asynchronous request");
				this.asyncControl.complete();
			}
			catch (Throwable ex) {
				logger.error("Failed to complete request: " + ex.getMessage());
			}
		}
		this.request = null;
		this.response = null;
		this.asyncControl = null;
	}

	@Override
	protected synchronized void writeFrameInternal(SockJsFrame frame) throws IOException {
		if (isActive()) {
			frame = this.frameFormat.format(frame);
			if (logger.isTraceEnabled()) {
				logger.trace("Writing " + frame);
			}
			this.response.getBody().write(frame.getContentBytes());
		}
	}

}
