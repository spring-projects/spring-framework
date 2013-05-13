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

package org.springframework.web.socket.sockjs.transport;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.springframework.http.server.AsyncServerHttpRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.AbstractServerSockJsSession;
import org.springframework.web.socket.sockjs.SockJsConfiguration;
import org.springframework.web.socket.sockjs.SockJsFrame;
import org.springframework.web.socket.sockjs.TransportErrorException;
import org.springframework.web.socket.sockjs.SockJsFrame.FrameFormat;
import org.springframework.web.socket.support.ExceptionWebSocketHandlerDecorator;

/**
 * An abstract base class for use with HTTP-based transports.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHttpServerSockJsSession extends AbstractServerSockJsSession {

	private FrameFormat frameFormat;

	private final BlockingQueue<String> messageCache = new ArrayBlockingQueue<String>(100);

	private AsyncServerHttpRequest asyncRequest;

	private ServerHttpResponse response;


	public AbstractHttpServerSockJsSession(String sessionId, SockJsConfiguration config, WebSocketHandler handler) {
		super(sessionId, config, handler);
	}

	public synchronized void setInitialRequest(ServerHttpRequest request, ServerHttpResponse response,
			FrameFormat frameFormat) throws TransportErrorException {

		try {
			udpateRequest(request, response, frameFormat);
			writePrelude();
			writeFrame(SockJsFrame.openFrame());
		}
		catch (Throwable t) {
			tryCloseWithSockJsTransportError(t, null);
			throw new TransportErrorException("Failed open SockJS session", t, getId());
		}
		try {
			delegateConnectionEstablished();
		}
		catch (Throwable t) {
			ExceptionWebSocketHandlerDecorator.tryCloseWithError(this, t, logger);
		}
	}

	protected void writePrelude() throws IOException {
	}

	public synchronized void setLongPollingRequest(ServerHttpRequest request, ServerHttpResponse response,
			FrameFormat frameFormat) throws TransportErrorException {

		try {
			udpateRequest(request, response, frameFormat);

			if (isClosed()) {
				logger.debug("connection already closed");
				try {
					writeFrame(SockJsFrame.closeFrameGoAway());
				}
				catch (IOException ex) {
					throw new TransportErrorException("Failed to send SockJS close frame", ex, getId());
				}
				return;
			}

			this.asyncRequest.setTimeout(-1);
			this.asyncRequest.startAsync();

			scheduleHeartbeat();
			tryFlushCache();
		}
		catch (Throwable t) {
			tryCloseWithSockJsTransportError(t, null);
			throw new TransportErrorException("Failed to start long running request and flush messages", t, getId());
		}
	}

	private void udpateRequest(ServerHttpRequest request, ServerHttpResponse response, FrameFormat frameFormat) {
		Assert.notNull(request, "expected request");
		Assert.notNull(response, "expected response");
		Assert.notNull(frameFormat, "expected frameFormat");
		Assert.isInstanceOf(AsyncServerHttpRequest.class, request, "Expected AsyncServerHttpRequest");
		this.asyncRequest = (AsyncServerHttpRequest) request;
		this.response = response;
		this.frameFormat = frameFormat;
	}


	@Override
	public synchronized boolean isActive() {
		return ((this.asyncRequest != null) && (!this.asyncRequest.isAsyncCompleted()));
	}

	protected BlockingQueue<String> getMessageCache() {
		return this.messageCache;
	}

	protected ServerHttpRequest getRequest() {
		return this.asyncRequest;
	}

	protected ServerHttpResponse getResponse() {
		return this.response;
	}

	@Override
	protected final synchronized void sendMessageInternal(String message) throws IOException {
		this.messageCache.add(message);
		tryFlushCache();
	}

	private void tryFlushCache() throws IOException {
		if (isActive() && !getMessageCache().isEmpty()) {
			logger.trace("Flushing messages");
			flushCache();
		}
	}

	/**
	 * Only called if the connection is currently active
	 */
	protected abstract void flushCache() throws IOException;

	@Override
	protected void disconnect(CloseStatus status) {
		resetRequest();
	}

	protected synchronized void resetRequest() {
		updateLastActiveTime();
		if (isActive() && this.asyncRequest.isAsyncStarted()) {
			try {
				logger.debug("Completing async request");
				this.asyncRequest.completeAsync();
			}
			catch (Throwable ex) {
				logger.error("Failed to complete async request: " + ex.getMessage());
			}
		}
		this.asyncRequest = null;
		this.response = null;
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
