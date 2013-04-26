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
import javax.websocket.RemoteEndpoint.Basic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.websocket.BinaryMessage;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.TextMessage;
import org.springframework.websocket.WebSocketMessage;
import org.springframework.websocket.WebSocketSession;

/**
 * A standard Java implementation of {@link WebSocketSession} that delegates to
 * {@link javax.websocket.Session}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketSessionAdapter implements WebSocketSession {

	private static Log logger = LogFactory.getLog(StandardWebSocketSessionAdapter.class);

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
	public void sendMessage(WebSocketMessage message) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Sending: " + message + ", " + this);
		}
		Assert.isTrue(isOpen(), "Cannot send message after connection closed.");
		Basic remote = this.session.getBasicRemote();
		if (message instanceof TextMessage) {
			remote.sendText(((TextMessage) message).getPayload());
		}
		else if (message instanceof BinaryMessage) {
			remote.sendBinary(((BinaryMessage) message).getPayload());
		}
		else {
			throw new IllegalStateException("Unexpected WebSocketMessage type: " + message);
		}
	}

	@Override
	public void close() throws IOException {
		close(CloseStatus.NORMAL);
	}

	@Override
	public void close(CloseStatus status) throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Closing " + this);
		}
		this.session.close(new CloseReason(CloseCodes.getCloseCode(status.getCode()), status.getReason()));
	}

	@Override
	public String toString() {
		return "WebSocket session id=" + getId();
	}

}
