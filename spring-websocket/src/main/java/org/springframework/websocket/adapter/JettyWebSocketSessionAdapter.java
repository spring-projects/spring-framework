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

import java.io.IOException;
import java.net.URI;

import org.eclipse.jetty.websocket.api.Session;
import org.springframework.util.ObjectUtils;
import org.springframework.websocket.BinaryMessage;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.TextMessage;
import org.springframework.websocket.WebSocketMessage;
import org.springframework.websocket.WebSocketSession;


/**
 * Adapts Jetty's {@link Session} to Spring's {@link WebSocketSession}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class JettyWebSocketSessionAdapter implements WebSocketSession {

	private Session session;


	public JettyWebSocketSessionAdapter(Session session) {
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