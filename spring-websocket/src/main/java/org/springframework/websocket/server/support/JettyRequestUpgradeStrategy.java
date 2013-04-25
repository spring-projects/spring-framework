/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.websocket.server.support;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.server.HandshakeRFC6455;
import org.eclipse.jetty.websocket.server.ServletWebSocketRequest;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.websocket.BinaryMessage;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.HandlerProvider;
import org.springframework.websocket.TextMessage;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketMessage;
import org.springframework.websocket.WebSocketSession;
import org.springframework.websocket.server.RequestUpgradeStrategy;

/**
 * {@link RequestUpgradeStrategy} for use with Jetty. Based on Jetty's internal
 * {@code org.eclipse.jetty.websocket.server.WebSocketHandler} class.
 *
 * @author Phillip Webb
 */
public class JettyRequestUpgradeStrategy implements RequestUpgradeStrategy {

	private static Log logger = LogFactory.getLog(JettyRequestUpgradeStrategy.class);

	// FIXME jetty has options, timeouts etc. Do we need a common abstraction

	// FIXME need a way for someone to plug their own RequestUpgradeStrategy or override
	// Jetty settings

	// FIXME when to call factory.cleanup();

	private static final String HANDLER_PROVIDER = JettyRequestUpgradeStrategy.class.getName()
			+ ".HANDLER_PROVIDER";

	private WebSocketServerFactory factory;


	public JettyRequestUpgradeStrategy() {
		this.factory = new WebSocketServerFactory();
		this.factory.setCreator(new WebSocketCreator() {
			@Override
			@SuppressWarnings("unchecked")
			public Object createWebSocket(UpgradeRequest req, UpgradeResponse resp) {
				Assert.isInstanceOf(ServletWebSocketRequest.class, req);
				ServletWebSocketRequest servletRequest = (ServletWebSocketRequest) req;
				HandlerProvider<WebSocketHandler> handlerProvider = (HandlerProvider<WebSocketHandler>) servletRequest.getServletAttributes().get(
						HANDLER_PROVIDER);
				return new WebSocketHandlerAdapter(handlerProvider);
			}
		});
		try {
			this.factory.init();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}


	@Override
	public String[] getSupportedVersions() {
		return new String[] { String.valueOf(HandshakeRFC6455.VERSION) };
	}

	@Override
	public void upgrade(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, HandlerProvider<WebSocketHandler> handlerProvider)
			throws IOException {
		Assert.isInstanceOf(ServletServerHttpRequest.class, request);
		Assert.isInstanceOf(ServletServerHttpResponse.class, response);
		upgrade(((ServletServerHttpRequest) request).getServletRequest(),
				((ServletServerHttpResponse) response).getServletResponse(),
				selectedProtocol, handlerProvider);
	}

	private void upgrade(HttpServletRequest request, HttpServletResponse response,
			String selectedProtocol, final HandlerProvider<WebSocketHandler> handlerProvider)
			throws IOException {
		request.setAttribute(HANDLER_PROVIDER, handlerProvider);
		Assert.state(factory.isUpgradeRequest(request, response), "Not a suitable WebSocket upgrade request");
		Assert.state(factory.acceptWebSocket(request, response), "Unable to accept WebSocket");
	}


	/**
	 * Adapts Spring's {@link WebSocketHandler} to Jetty's {@link WebSocketListener}.
	 */
	private static class WebSocketHandlerAdapter implements WebSocketListener {

		private final HandlerProvider<WebSocketHandler> provider;

		private WebSocketHandler handler;

		private WebSocketSession session;

		private final AtomicInteger sessionCount = new AtomicInteger(0);


		public WebSocketHandlerAdapter(HandlerProvider<WebSocketHandler> provider) {
			Assert.notNull(provider, "Provider must not be null");
			Assert.isAssignable(WebSocketHandler.class, provider.getHandlerType());
			this.provider = provider;
		}


		@Override
		public void onWebSocketConnect(Session session) {

			Assert.isTrue(this.sessionCount.compareAndSet(0, 1), "Unexpected connection");

			this.session = new WebSocketSessionAdapter(session);
			if (logger.isDebugEnabled()) {
				logger.debug("Connection established, WebSocket session id="
						+ this.session.getId() + ", uri=" + this.session.getURI());
			}
			this.handler = this.provider.getHandler();

			try {
				this.handler.afterConnectionEstablished(this.session);
			}
			catch (Throwable ex) {
				tryCloseWithError(ex);
			}
		}

		private void tryCloseWithError(Throwable ex) {
			logger.error("Unhandled error for " + this.session, ex);
			if (this.session.isOpen()) {
				try {
					this.session.close(CloseStatus.SERVER_ERROR);
				}
				catch (Throwable t) {
					destroyHandler();
				}
			}
		}

		private void destroyHandler() {
			try {
				if (this.handler != null) {
					this.provider.destroy(this.handler);
				}
			}
			catch (Throwable t) {
				logger.warn("Error while destroying handler", t);
			}
			finally {
				this.session = null;
				this.handler = null;
			}
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
			try {
				CloseStatus closeStatus = new CloseStatus(statusCode, reason);
				if (logger.isDebugEnabled()) {
					logger.debug("Connection closed, WebSocket session id="
							+ this.session.getId() + ", " + closeStatus);
				}
				this.handler.afterConnectionClosed(closeStatus, this.session);
			}
			catch (Throwable ex) {
				logger.error("Unhandled error for " + this.session, ex);
			}
			finally {
				destroyHandler();
			}
		}

		@Override
		public void onWebSocketText(String payload) {
			try {
				TextMessage message = new TextMessage(payload);
				if (logger.isTraceEnabled()) {
					logger.trace("Received message for WebSocket session id="
							+ this.session.getId() + ": " + message);
				}
				this.handler.handleTextMessage(message, this.session);
			}
			catch(Throwable ex) {
				tryCloseWithError(ex);
			}
		}

		@Override
		public void onWebSocketBinary(byte[] payload, int offset, int len) {
			try {
				BinaryMessage message = new BinaryMessage(payload, offset, len);
				if (logger.isTraceEnabled()) {
					logger.trace("Received binary data for WebSocket session id="
							+ this.session.getId() + ": " + message);
				}
				this.handler.handleBinaryMessage(message, this.session);
			}
			catch(Throwable ex) {
				tryCloseWithError(ex);
			}
		}

		@Override
		public void onWebSocketError(Throwable cause) {
			try {
				this.handler.handleTransportError(cause, this.session);
			}
			catch (Throwable ex) {
				tryCloseWithError(ex);
			}
		}
	}


	/**
	 * Adapts Jetty's {@link Session} to Spring's {@link WebSocketSession}.
	 */
	private static class WebSocketSessionAdapter implements WebSocketSession {

		private Session session;


		public WebSocketSessionAdapter(Session session) {
			this.session = session;
		}


		@Override
		public String getId() {
			return ObjectUtils.getIdentityHexString(this.session);
		}

		@Override
		public boolean isOpen() {
			return this.session.isOpen();
		}

		@Override
		public boolean isSecure() {
			return this.session.isSecure();
		}

		@Override
		public URI getURI() {
			return this.session.getUpgradeRequest().getRequestURI();
		}

		@Override
		public void sendMessage(WebSocketMessage message) throws IOException {
			if (message instanceof BinaryMessage) {
				sendMessage((BinaryMessage) message);
			}
			else if (message instanceof TextMessage) {
				sendMessage((TextMessage) message);
			}
			else {
				throw new IllegalArgumentException("Unsupported message type");
			}
		}

		private void sendMessage(BinaryMessage message) throws IOException {
			this.session.getRemote().sendBytes(message.getPayload());
		}

		private void sendMessage(TextMessage message) throws IOException {
			this.session.getRemote().sendString(message.getPayload());
		}

		@Override
		public void close() throws IOException {
			this.session.close();
		}

		@Override
		public void close(CloseStatus status) throws IOException {
			this.session.close(status.getCode(), status.getReason());
		}
	}

}
