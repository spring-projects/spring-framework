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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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

	private final BlockingQueue<String> messageCache;

	private ServerHttpRequest request;

	private ServerHttpResponse response;

	private ServerHttpAsyncRequestControl asyncRequestControl;

	private SockJsFrameFormat frameFormat;

	private URI uri;

	private HttpHeaders handshakeHeaders;

	private Principal principal;

	private InetSocketAddress localAddress;

	private InetSocketAddress remoteAddress;

	private String acceptedProtocol;


	public AbstractHttpSockJsSession(String id, SockJsServiceConfig config,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {

		super(id, config, wsHandler, attributes);
		this.messageCache = new ArrayBlockingQueue<String>(config.getHttpMessageCacheSize());
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
	 * Unlike WebSocket where sub-protocol negotiation is part of the
	 * initial handshake, in HTTP transports the same negotiation must
	 * be emulated and the selected protocol set through this setter.
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
	public synchronized void handleInitialRequest(ServerHttpRequest request, ServerHttpResponse response,
			SockJsFrameFormat frameFormat) throws SockJsException {

		initRequest(request, response, frameFormat);

		this.uri = request.getURI();
		this.handshakeHeaders = request.getHeaders();
		this.principal = request.getPrincipal();
		this.localAddress = request.getLocalAddress();
		this.remoteAddress = request.getRemoteAddress();

		try {
			writePrelude();
			writeFrame(SockJsFrame.openFrame());
		}
		catch (Throwable ex) {
			tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
			throw new SockJsTransportFailureException("Failed to send \"open\" frame", getId(), ex);
		}

		try {
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

		this.request = request;
		this.response = response;
		this.asyncRequestControl = request.getAsyncRequestControl(response);
		this.frameFormat = frameFormat;
	}

	protected void writePrelude() throws IOException {
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
	public synchronized void handleSuccessiveRequest(ServerHttpRequest request,
			ServerHttpResponse response, SockJsFrameFormat frameFormat) throws SockJsException {

		initRequest(request, response, frameFormat);
		try {
			writePrelude();
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
			scheduleHeartbeat();
			tryFlushCache();
		}
		catch (Throwable ex) {
			tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
			throw new SockJsTransportFailureException("Failed to flush messages", getId(), ex);
		}
	}

	@Override
	public synchronized boolean isActive() {
		return (this.asyncRequestControl != null && !this.asyncRequestControl.isCompleted());
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
		if (this.messageCache.isEmpty()) {
			logger.trace("Nothing to flush");
			return;
		}
		if (logger.isTraceEnabled()) {
			logger.trace(this.messageCache.size() + " message(s) to flush");
		}
		if (isActive()) {
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

	protected synchronized void resetRequest() {
		updateLastActiveTime();
		if (isActive() && this.asyncRequestControl.isStarted()) {
			try {
				logger.debug("Completing asynchronous request");
				this.asyncRequestControl.complete();
			}
			catch (Throwable ex) {
				logger.error("Failed to complete request: " + ex.getMessage());
			}
		}
		this.request = null;
		this.response = null;
		this.asyncRequestControl = null;
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
