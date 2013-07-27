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

package org.springframework.web.socket.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;


/**
 * A {@link WebSocketHandler} that delegates to other {@link WebSocketHandler} instances
 * based on the sub-protocol value accepted at the handshake. A default handler can also
 * be configured for use by default when a sub-protocol value if the WebSocket session
 * does not have a sub-protocol value associated with it.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MultiProtocolWebSocketHandler implements WebSocketHandler {

	private WebSocketHandler defaultHandler;

	private Map<String, WebSocketHandler> handlers = new HashMap<String, WebSocketHandler>();


	/**
	 * Configure {@link WebSocketHandler}'s to use by sub-protocol. The values for
	 * sub-protocols are case insensitive.
	 */
	public void setProtocolHandlers(Map<String, WebSocketHandler> protocolHandlers) {
		this.handlers.clear();
		for (String protocol : protocolHandlers.keySet()) {
			this.handlers.put(protocol.toLowerCase(), protocolHandlers.get(protocol));
		}
	}

	/**
	 * Return a read-only copy of the sub-protocol handler map.
	 */
	public Map<String, WebSocketHandler> getProtocolHandlers() {
		return Collections.unmodifiableMap(this.handlers);
	}

	/**
	 * Set the default {@link WebSocketHandler} to use if a sub-protocol was not
	 * requested.
	 */
	public void setDefaultProtocolHandler(WebSocketHandler defaultHandler) {
		this.defaultHandler = defaultHandler;
	}

	/**
	 * Return the default {@link WebSocketHandler} to be used.
	 */
	public WebSocketHandler getDefaultProtocolHandler() {
		return this.defaultHandler;
	}


	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		WebSocketHandler handler = getHandlerForSession(session);
		handler.afterConnectionEstablished(session);
	}

	private WebSocketHandler getHandlerForSession(WebSocketSession session) {
		WebSocketHandler handler = null;
		String protocol = session.getAcceptedProtocol();
		if (protocol != null) {
			handler = this.handlers.get(protocol.toLowerCase());
			Assert.state(handler != null,
					"No WebSocketHandler for sub-protocol '" + protocol + "', handlers=" + this.handlers);
		}
		else {
			handler = this.defaultHandler;
			Assert.state(handler != null,
					"No sub-protocol was requested and no default WebSocketHandler was configured");
		}
		return handler;
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		WebSocketHandler handler = getHandlerForSession(session);
		handler.handleMessage(session, message);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		WebSocketHandler handler = getHandlerForSession(session);
		handler.handleTransportError(session, exception);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		WebSocketHandler handler = getHandlerForSession(session);
		handler.afterConnectionClosed(session, closeStatus);
	}

	@Override
	public boolean supportsPartialMessages() {
		return false;
	}

}
