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
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame.FrameFormat;
import org.springframework.web.socket.support.WebSocketExtension;

/**
 * An abstract base class for use with HTTP transport based SockJS sessions.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractHttpSockJsSession extends AbstractSockJsSession {

	private FrameFormat frameFormat;

	private final BlockingQueue<String> messageCache;

	private ServerHttpRequest request;

	private ServerHttpResponse response;

	private ServerHttpAsyncRequestControl asyncRequestControl;

	private URI uri;

	private HttpHeaders handshakeHeaders;

	private Principal principal;

	private InetSocketAddress localAddress;

	private InetSocketAddress remoteAddress;

	private String acceptedProtocol;


	public AbstractHttpSockJsSession(String id, SockJsServiceConfig config,
			WebSocketHandler wsHandler, Map<String, Object> handshakeAttributes) {

		super(id, config, wsHandler, handshakeAttributes);
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

	protected void setHandshakeHeaders(HttpHeaders handshakeHeaders) {
		this.handshakeHeaders = handshakeHeaders;
	}

	@Override
	public Principal getPrincipal() {
		return this.principal;
	}

	protected void setPrincipal(Principal principal) {
		this.principal = principal;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return this.localAddress;
	}

	protected void setLocalAddress(InetSocketAddress localAddress) {
		this.localAddress = localAddress;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	protected void setRemoteAddress(InetSocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		return Collections.emptyList();
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

	public synchronized void handleInitialRequest(ServerHttpRequest request, ServerHttpResponse response,
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

		this.uri = request.getURI();
		this.handshakeHeaders = request.getHeaders();
		this.principal = request.getPrincipal();
		this.localAddress = request.getLocalAddress();
		this.remoteAddress = request.getRemoteAddress();

		try {
			delegateConnectionEstablished();
		}
		catch (Throwable t) {
			throw new SockJsException("Unhandled exception from WebSocketHandler", getId(), t);
		}
	}

	protected void writePrelude() throws IOException {
	}

	public synchronized void startLongPollingRequest(ServerHttpRequest request,
			ServerHttpResponse response, FrameFormat frameFormat) throws SockJsException {

		udpateRequest(request, response, frameFormat);
		try {
			this.asyncRequestControl.start(-1);
			scheduleHeartbeat();
			tryFlushCache();
		}
		catch (Throwable t) {
			tryCloseWithSockJsTransportError(t, CloseStatus.SERVER_ERROR);
			throw new SockJsTransportFailureException("Failed to flush messages", getId(), t);
		}
	}

	private void udpateRequest(ServerHttpRequest request, ServerHttpResponse response,
			FrameFormat frameFormat) {
		Assert.notNull(request, "expected request");
		Assert.notNull(response, "expected response");
		Assert.notNull(frameFormat, "expected frameFormat");
		this.request = request;
		this.response = response;
		this.asyncRequestControl = request.getAsyncRequestControl(response);
		this.frameFormat = frameFormat;
		afterRequestUpdated();
	}

	protected void afterRequestUpdated() {
	}

	@Override
	public synchronized boolean isActive() {
		return ((this.asyncRequestControl != null) && (!this.asyncRequestControl.isCompleted()));
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
