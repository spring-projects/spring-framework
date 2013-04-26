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

package org.springframework.websocket.adapter;

import org.springframework.websocket.BinaryMessage;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;


/**
 * A {@link WebSocketHandler} for binary messages with empty methods.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 4.0
 */
public class BinaryWebSocketHandlerAdapter implements WebSocketHandler<BinaryMessage> {

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
	}

	@Override
	public void handleMessage(WebSocketSession session, BinaryMessage message) {
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
	}

}
