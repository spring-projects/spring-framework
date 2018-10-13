/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.socket.client;

import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.tomcat.websocket.WsWebSocketContainer;
import reactor.core.publisher.MonoProcessor;

import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketSession;
import org.springframework.web.reactive.socket.adapter.TomcatWebSocketSession;

/**
 * {@link WebSocketClient} implementation for use with the Java WebSocket API.
 *
 * @author Violeta Georgieva
 * @since 5.0
 */
public class TomcatWebSocketClient extends StandardWebSocketClient {


	public TomcatWebSocketClient() {
		this(new WsWebSocketContainer());
	}

	public TomcatWebSocketClient(WebSocketContainer webSocketContainer) {
		super(webSocketContainer);
	}


	@Override
	protected StandardWebSocketSession createWebSocketSession(Session session,
			HandshakeInfo info, MonoProcessor<Void> completion) {

			return new TomcatWebSocketSession(session, info, bufferFactory(), completion);
	}

}
