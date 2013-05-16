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

package org.springframework.web.socket.adapter;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;


/**
 * A convenient base class for {@link WebSocketHandler} implementation with empty methods.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0
 */
public class WebSocketHandlerAdapter implements WebSocketHandler {

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		if (message instanceof TextMessage) {
			handleTextMessage(session, (TextMessage) message);
		}
		else if (message instanceof BinaryMessage) {
			handleBinaryMessage(session, (BinaryMessage) message);
		}
		else {
			throw new IllegalStateException("Unexpected WebSocket message type: " + message);
		}
	}

	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
	}

	protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
	}

	@Override
	public boolean supportsPartialMessages() {
		return false;
	}

}
