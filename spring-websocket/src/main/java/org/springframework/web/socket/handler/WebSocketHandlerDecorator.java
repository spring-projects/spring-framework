/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.handler;

import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Wraps another {@link org.springframework.web.socket.WebSocketHandler}
 * instance and delegates to it.
 *
 * <p>Also provides a {@link #getDelegate()} method to return the decorated
 * handler as well as a {@link #getLastHandler()} method to go through all nested
 * delegates and return the "last" handler.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketHandlerDecorator implements WebSocketHandler {

	private final WebSocketHandler delegate;


	public WebSocketHandlerDecorator(WebSocketHandler delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}


	public WebSocketHandler getDelegate() {
		return this.delegate;
	}

	public WebSocketHandler getLastHandler() {
		WebSocketHandler result = this.delegate;
		while (result instanceof WebSocketHandlerDecorator webSocketHandlerDecorator) {
			result = webSocketHandlerDecorator.getDelegate();
		}
		return result;
	}

	public static WebSocketHandler unwrap(WebSocketHandler handler) {
		if (handler instanceof WebSocketHandlerDecorator webSocketHandlerDecorator) {
			return webSocketHandlerDecorator.getLastHandler();
		}
		else {
			return handler;
		}
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		this.delegate.afterConnectionEstablished(session);
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		this.delegate.handleMessage(session, message);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		this.delegate.handleTransportError(session, exception);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		this.delegate.afterConnectionClosed(session, closeStatus);
	}

	@Override
	public boolean supportsPartialMessages() {
		return this.delegate.supportsPartialMessages();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + this.delegate + "]";
	}

}
