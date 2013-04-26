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
package org.springframework.websocket.client;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.websocket.HandlerProvider;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;

/**
 * Contract for starting a WebSocket handshake request.
 *
 * <p>To automatically start a WebSocket connection when the application starts, see
 * {@link WebSocketConnectionManager}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 * @see WebSocketConnectionManager
 */
public interface WebSocketClient {

	WebSocketSession doHandshake(WebSocketHandler handler,
			String uriTemplate, Object... uriVariables) throws WebSocketConnectFailureException;

	WebSocketSession doHandshake(HandlerProvider<WebSocketHandler<?>> handler,
			String uriTemplate, Object... uriVariables) throws WebSocketConnectFailureException;

	WebSocketSession doHandshake(HandlerProvider<WebSocketHandler<?>> handler, HttpHeaders headers, URI uri)
			throws WebSocketConnectFailureException;

}
