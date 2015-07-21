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

import javax.servlet.ServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpAsyncRequestControl;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsFrameFormat;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;

/**
 * An abstract base class for use with HTTP transport SockJS sessions.
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

	private volatile SockJsFrameFormat frameFormat;


	private volatile ServerHttpAsyncRequestControl asyncRequestControl;

	private final Object responseLock = new Object();

	private volatile boolean readyToSend;


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
	 * Return the SockJS buffer for messages stored transparently between polling
	 * requests. If the polling request takes longer than 5 seconds, the session
	 * is closed.
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
	 * @deprecated as of 4.2, since this method is no longer used.
	 */
	@Deprecated
	protected abstract boolean isStreaming();


	/**
	 * Handle the first request for receiving messages on a SockJS HTTP transport
	 * based session.
	 * <p>Long polling-based transports (e.g. "xhr", "jsonp") complete the request
	 * after writing the open frame. Streaming-based transports ("xhr_streaming",
	 * "eventsource", and "htmlfile") leave the response open longer for further
	 * streaming of message frames but will also close it eventually after some
	 * amount of data has been sent.
	 * @param request the current request
	 * @param response the current response
	 * @param frameFormat the transport-specific SocksJS frame format to use
	 */
	public void handleInitialRequest(ServerHttpRequest request, ServerHttpResponse response,
			SockJsFrameFormat frameFormat) throws SockJsException {

		this.uri = request.getURI();
		this.handshakeHeaders = request.getHeaders();
		this.principal = request.getPrincipal();
		this.localAddress = request.getLocalAddress();
		this.remoteAddress = request.getRemoteAddress();

		synchronized (this.responseLock) {
			try {
				this.response = response;
				this.frameFormat = frameFormat;
				this.asyncRequestControl = request.getAsyncRequestControl(response);
				this.asyncRequestControl.start(-1);

				disableShallowEtagHeaderFilter(request);

				// Let "our" handler know before sending the open frame to the remote handler
				delegateConnectionEstablished();

				handleRequestInternal(request, response, true);

				// Request might have been reset (e.g. polling sessions do after writing)
				this.readyToSend = isActive();
			}
			catch (Throwable ex) {
				tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
				throw new SockJsTransportFailureException("Failed to open session", getId(), ex);
			}
		}
	}

	/**
	 * Handle all requests, except the first one, to receive messages on a SockJS
	 * HTTP transport based session.
	 * <p>Long polling-based transports (e.g. "xhr", "jsonp") complete the request
	 * after writing any buffered message frames (or the next one). Streaming-based
	 * transports ("xhr_streaming", "eventsource", and "htmlfile") leave the
	 * response open longer for further streaming of message frames but will also
	 * close it eventually after some amount of data has been sent.
	 * @param request the current request
	 * @param response the current response
	 * @param frameFormat the transport-specific SocksJS frame format to use
	 */
	public void handleSuccessiveRequest(ServerHttpRequest request, ServerHttpResponse response,
			SockJsFrameFormat frameFormat) throws SockJsException {

		synchronized (this.responseLock) {
			try {
				if (isClosed()) {
					response.getBody().write(SockJsFrame.closeFrameGoAway().getContentBytes());
					return;
				}
				this.response = response;
				this.frameFormat = frameFormat;
				this.asyncRequestControl = request.getAsyncRequestControl(response);
				this.asyncRequestControl.start(-1);

				disableShallowEtagHeaderFilter(request);

				handleRequestInternal(request, response, false);
				this.readyToSend = isActive();
			}
			catch (Throwable ex) {
				tryCloseWithSockJsTransportError(ex, CloseStatus.SERVER_ERROR);
				throw new SockJsTransportFailureException("Failed to handle SockJS receive request", getId(), ex);
			}
		}
	}

	private void disableShallowEtagHeaderFilter(ServerHttpRequest request) {
		if (request instanceof ServletServerHttpRequest) {
			ServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
			ShallowEtagHeaderFilter.disableContentCaching(servletRequest);
		}
	}

	/**
	 * Invoked when a SockJS transport request is received.
	 * @param request the current request
	 * @param response the current response
	 * @param initialRequest whether it is the first request for the session
	 */
	protected abstract void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			boolean initialRequest) throws IOException;

	@Override
	protected final void sendMessageInternal(String message) throws SockJsTransportFailureException {
		synchronized (this.responseLock) {
			this.messageCache.add(message);
			if (logger.isTraceEnabled()) {
				logger.trace(this.messageCache.size() + " message(s) to flush in session " + this.getId());
			}
			if (isActive() && this.readyToSend) {
				if (logger.isTraceEnabled()) {
					logger.trace("Session is active, ready to flush.");
				}
				cancelHeartbeat();
				flushCache();
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Session is not active, not ready to flush.");
				}
			}
		}
	}

	/**
	 * Called when the connection is active and ready to write to the response.
	 * Subclasses should only call this method from a method where the
	 * "responseLock" is acquired.
	 */
	protected abstract void flushCache() throws SockJsTransportFailureException;


	/**
	 * @deprecated as of 4.2 this method is deprecated since the prelude is written
	 * in {@link #handleRequestInternal} of the StreamingSockJsSession subclass.
	 */
	@Deprecated
	protected void writePrelude(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
	}

	@Override
	protected void disconnect(CloseStatus status) {
		resetRequest();
	}

	protected void resetRequest() {
		synchronized (this.responseLock) {

			ServerHttpAsyncRequestControl control = this.asyncRequestControl;
			this.asyncRequestControl = null;
			this.readyToSend = false;
			this.response = null;

			updateLastActiveTime();

			if (control != null && !control.isCompleted()) {
				if (control.isStarted()) {
					try {
						control.complete();
					}
					catch (Throwable ex) {
						// Could be part of normal workflow (e.g. browser tab closed)
						logger.debug("Failed to complete request: " + ex.getMessage());
					}
				}
			}
		}
	}

	@Override
	protected void writeFrameInternal(SockJsFrame frame) throws IOException {
		if (isActive()) {
			String formattedFrame = this.frameFormat.format(frame);
			if (logger.isTraceEnabled()) {
				logger.trace("Writing to HTTP response: " + formattedFrame);
			}
			this.response.getBody().write(formattedFrame.getBytes(SockJsFrame.CHARSET));
			this.response.flush();
		}
	}

}
