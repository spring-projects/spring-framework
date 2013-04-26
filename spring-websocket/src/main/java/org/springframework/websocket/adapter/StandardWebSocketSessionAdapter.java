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

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import org.springframework.util.Assert;
import org.springframework.websocket.BinaryMessage;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.TextMessage;
import org.springframework.websocket.WebSocketSession;

/**
 * A standard Java implementation of {@link WebSocketSession} that delegates to
 * {@link javax.websocket.Session}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketSessionAdapter extends AbstractWebSocketSesssionAdapter {

	private final javax.websocket.Session session;


	public StandardWebSocketSessionAdapter(javax.websocket.Session session) {
		Assert.notNull(session, "session is required");
		this.session = session;
	}


	@Override
	public String getId() {
		return this.session.getId();
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
		return this.session.getRequestURI();
	}

	@Override
	protected void sendTextMessage(TextMessage message) throws IOException {
		this.session.getBasicRemote().sendText(message.getPayload());
	}

	@Override
	protected void sendBinaryMessage(BinaryMessage message) throws IOException {
		this.session.getBasicRemote().sendBinary(message.getPayload());
	}

	@Override
	protected void closeInternal(CloseStatus status) throws IOException {
		this.session.close(new CloseReason(CloseCodes.getCloseCode(status.getCode()), status.getReason()));
	}

}
