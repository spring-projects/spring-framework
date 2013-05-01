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

package org.springframework.websocket.support;

import org.springframework.util.Assert;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketMessage;
import org.springframework.websocket.WebSocketSession;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class WebSocketHandlerDecorator implements WebSocketHandler {

	private final WebSocketHandler delegate;


	public WebSocketHandlerDecorator(WebSocketHandler delegate) {
		Assert.notNull(delegate, "delegate is required");
		this.delegate = delegate;
	}


	protected WebSocketHandler getDelegate() {
		return this.delegate;
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
	public boolean isStreaming() {
		return this.delegate.isStreaming();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + this.delegate + "]";
	}

}
