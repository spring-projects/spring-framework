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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpAsyncRequestControl;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsFrameFormat;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;

/**
 * An abstract base class for use with HTTP transport based SockJS sessions.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHttpSockJsSession extends AbstractSockJsSession {


	private volatile URI uri;

	private volatile HttpHeaders handshakeHeaders;

	private volatile Principal principal;

	private volatile InetSocketAddress localAddress;

	private volatile InetSocketAddress remoteAddress;

	private volatile String acceptedProtocol;


	private volatile ServerHttpResponse response;

	private volatile ServerHttpAsyncRequestControl asyncRequestControl;

	private volatile SockJsFrameFormat frameFormat;

	private volatile boolean requestInitialized;


	private final Queue<String> messageCache;


	public AbstractHttpSockJsSession(String id, SockJsServiceConfig config,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {

		super(id, config, wsHandler, attributes);
		this.messageCache = new LinkedBlockingQueue<String>(config.getHttpMessageCacheSize());
	}


	@Override
	public URI getUri() {
		return this.uri;
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		return this.handshakeHeaders;
	}

	@Override
	public Principal getPrincipal() {
		return this.principal;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return this.localAddress;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	/**
	 * Unlike WebSocket where sub-protocol negotiation is part of the initial
	 * handshake, in HTTP transports the same negotiation must be emulated and
	 * the selected protocol set through this setter.
	 * @param protocol the sub-protocol to set
	 */
	public void setAcceptedProtocol(String protocol) {
		this.acceptedProtocol = protocol;
	}

	/**
	 * Return the selected sub-protocol to use.
	 */
	public String getAcceptedProtocol() {
		return this.acceptedProtocol;
	}

	/**
	 * Return response for the current request, or {@code null} if between requests.
	 */
	protected ServerHttpResponse getResponse() {
		return this.response;
	}

	/**
	 * Return the SockJS buffer for messages stored transparently between polling
	 * requests. If the polling request takes longer than 5 seconds, the session
	 * will be closed.
	 *
	 * @see org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService
	 */
	protected Queue<String> getMessageCache() {
		return this.messageCache;
	}

	@Override
	public boolean isActive() {
		ServerHttpAsyncRequestControl control = this.asyncRequestControl;
		return (control != null && !control.isCompleted());
	}

	@Override
	public void setTextMessageSizeLimit(int messageSizeLimit) {
		// ignore
	}

	@Override
	public int getTextMessageSizeLimit() {
		return -1;
	}

	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		// ignore
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		return -1;
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		return Collections.emptyList();
	}


	/**
	 * Handle the first HTTP request, i.e. the one that starts a SockJS session.
	 * Write a prelude to the response (if needed), send the SockJS "open" frame
	 * to indicate to the client the session is opened, and invoke the
	 * delegate WebSocketHandler to provide it with the newly opened session.
	 * <p>
	 * The "xhr" and "jsonp" (polling-based) transports completes the initial request
	 * as soon as the open frame is sent. Following that the client should start a
	 * successive polling request within the same SockJS session.
	 * <p>
	 * The "xhr_streaming", "eventsource", and "htmlfile" transports are streaming
	 * based and will leave the initial request open in order to stream one or
	 * more messages. However, even streaming based transports eventually recycle
	 * the long running request, after a certain number of bytes have been streamed
	 * (128K by default), and allow the client to start a successive request within
	 * the same SockJS session.
	 *
	 * @param request the current request
	 * @param response the current response
	 * @param frameFormat the transport-specific SocksJS frame format to use
	 *
	 * @see #handleSuccessiveRequest(org.springframework.http.server.ServerHttpRequest, org.springframework.http.server.ServerHttpResponse, org.springframework.web.socket.sockjs.frame.SockJsFrameFormat)
	 */
	public void handleInitialRequest(ServerHttpRequest request, ServerHttpResponse response,
			SockJsFrameFormat frameFormat) throws SockJsException {

		initRequest(request, response, frameFormat);

		this.uri = request.getURI();
		this.handshakeHeaders = request.getHeaders();
		this.principal = request.getPrincipal();
		this.localAddress = request.getLocalAddress();
		this.remoteAddress = request.getRemoteAddress();

		try {
			writePrelude(request, response);
			writeFrame(SockJsFrame.openFrame());
		}
		catch (Throwable ex) {
			tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
			throw new SockJsTransportFailureException("Failed to send \"open\" frame", getId(), ex);
		}

		try {
			this.requestInitialized = true;
			delegateConnectionEstablished();
		}
		catch (Throwable ex) {
			throw new SockJsException("Unhandled exception from WebSocketHandler", getId(), ex);
		}
	}

	private void initRequest(ServerHttpRequest request, ServerHttpResponse response,
			SockJsFrameFormat frameFormat) {

		Assert.notNull(request, "Request must not be null");
		Assert.notNull(response, "Response must not be null");
		Assert.notNull(frameFormat, "SockJsFrameFormat must not be null");

		this.response = response;
		this.frameFormat = frameFormat;
		this.asyncRequestControl = request.getAsyncRequestControl(response);
	}

	protected void writePrelude(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
	}

	/**
	 * Handle all HTTP requests part of the same SockJS session except for the very
	 * first, initial request. Write a prelude (if needed) and keep the request
	 * open and ready to send a message from the server to the client.
	 * <p>
	 * The "xhr" and "jsonp" (polling-based) transports completes the request when
	 * the next message is sent, which could be an array of messages cached during
	 * the time between successive requests, or it could be a heartbeat message
	 * sent if no other messages were sent (by default within 25 seconds).
	 * <p>
	 * The "xhr_streaming", "eventsource", and "htmlfile" transports are streaming
	 * based and will leave the request open longer in order to stream messages over
	 * a period of time. However, even streaming based transports eventually recycle
	 * the long running request, after a certain number of bytes have been streamed
	 * (128K by default), and allow the client to start a successive request within
	 * the same SockJS session.
	 *
	 * @param request the current request
	 * @param response the current response
	 * @param frameFormat the transport-specific SocksJS frame format to use
	 *
	 * @see #handleInitialRequest(org.springframework.http.server.ServerHttpRequest, org.springframework.http.server.ServerHttpResponse, org.springframework.web.socket.sockjs.frame.SockJsFrameFormat)
	 */
	public void handleSuccessiveRequest(ServerHttpRequest request,
			ServerHttpResponse response, SockJsFrameFormat frameFormat) throws SockJsException {

		initRequest(request, response, frameFormat);
		try {
			writePrelude(request, response);
		}
		catch (Throwable ex) {
			tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
			throw new SockJsTransportFailureException("Failed to send \"open\" frame", getId(), ex);
		}
		startAsyncRequest();
	}

	protected void startAsyncRequest() throws SockJsException {
		try {
			this.asyncRequestControl.start(-1);
			this.requestInitialized = true;
			scheduleHeartbeat();
			tryFlushCache();
		}
		catch (Throwable ex) {
			tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
			throw new SockJsTransportFailureException("Failed to flush messages", getId(), ex);
		}
	}

	@Override
	protected final void sendMessageInternal(String message) throws SockJsTransportFailureException {
		this.messageCache.add(message);
		tryFlushCache();
	}

	private void tryFlushCache() throws SockJsTransportFailureException {
		if (this.messageCache.isEmpty()) {
			logger.trace("Nothing to flush");
			return;
		}
		if (logger.isTraceEnabled()) {
			logger.trace(this.messageCache.size() + " message(s) to flush");
		}
		if (isActive() && this.requestInitialized) {
			logger.trace("Flushing messages");
			flushCache();
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Not ready to flush");
			}
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

	protected void resetRequest() {

		this.requestInitialized = false;
		updateLastActiveTime();

		if (isActive()) {
			ServerHttpAsyncRequestControl control = this.asyncRequestControl;
			if (control.isStarted()) {
				try {
					logger.debug("Completing asynchronous request");
					control.complete();
				}
				catch (Throwable ex) {
					logger.error("Failed to complete request: " + ex.getMessage());
				}
			}
		}

		this.response = null;
		this.asyncRequestControl = null;
	}

	@Override
	protected void writeFrameInternal(SockJsFrame frame) throws IOException {
		if (isActive()) {
			frame = this.frameFormat.format(frame);
			if (logger.isTraceEnabled()) {
				logger.trace("Writing " + frame);
			}
			getResponse().getBody().write(frame.getContentBytes());
		}
	}

}
