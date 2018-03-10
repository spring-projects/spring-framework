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

package org.springframework.web.socket.sockjs.transport.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.session.WebSocketServerSockJsSession;

/**
 * An implementation of {@link WebSocketHandler} that adds SockJS messages frames, sends
 * SockJS heartbeat messages, and delegates lifecycle events and messages to a target
 * {@link WebSocketHandler}.
 *
 * <p>Methods in this class allow exceptions from the wrapped {@link WebSocketHandler} to
 * propagate. However, any exceptions resulting from SockJS message handling (e.g. while
 * sending SockJS frames or heartbeat messages) are caught and treated as transport
 * errors, i.e. routed to the
 * {@link WebSocketHandler#handleTransportError(WebSocketSession, Throwable)
 * handleTransportError} method of the wrapped handler and the session closed.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsWebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable {

	private final SockJsServiceConfig sockJsServiceConfig;

	private final WebSocketServerSockJsSession sockJsSession;

	private final List<String> subProtocols;

	private final AtomicInteger sessionCount = new AtomicInteger(0);


	public SockJsWebSocketHandler(SockJsServiceConfig serviceConfig, WebSocketHandler webSocketHandler,
			WebSocketServerSockJsSession sockJsSession) {

		Assert.notNull(serviceConfig, "serviceConfig must not be null");
		Assert.notNull(webSocketHandler, "webSocketHandler must not be null");
		Assert.notNull(sockJsSession, "session must not be null");

		this.sockJsServiceConfig = serviceConfig;
		this.sockJsSession = sockJsSession;

		webSocketHandler = WebSocketHandlerDecorator.unwrap(webSocketHandler);
		this.subProtocols = ((webSocketHandler instanceof SubProtocolCapable) ?
				new ArrayList<>(((SubProtocolCapable) webSocketHandler).getSubProtocols()) : Collections.emptyList());
	}

	@Override
	public List<String> getSubProtocols() {
		return this.subProtocols;
	}

	protected SockJsServiceConfig getSockJsConfig() {
		return this.sockJsServiceConfig;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
		Assert.isTrue(this.sessionCount.compareAndSet(0, 1), "Unexpected connection");
		this.sockJsSession.initializeDelegateSession(wsSession);
	}

	@Override
	public void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
		this.sockJsSession.handleMessage(message, wsSession);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) throws Exception {
		this.sockJsSession.delegateConnectionClosed(status);
	}

	@Override
	public void handleTransportError(WebSocketSession webSocketSession, Throwable exception) throws Exception {
		this.sockJsSession.delegateError(exception);
	}

}
