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

package org.springframework.websocket.endpoint;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.WebSocketSession;


/**
 * A standard Java implementation of {@link WebSocketSession} that delegates to
 * {@link javax.websocket.Session}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketSession implements WebSocketSession {

	private static Log logger = LogFactory.getLog(StandardWebSocketSession.class);

	private final javax.websocket.Session session;


	public StandardWebSocketSession(javax.websocket.Session session) {
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
	public void sendTextMessage(String text) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Sending text message: " + text + ", " + this);
		}
		Assert.isTrue(isOpen(), "Cannot send message after connection closed.");
		this.session.getBasicRemote().sendText(text);
	}

	@Override
	public void sendBinaryMessage(ByteBuffer message) throws IOException {
		if (logger.isTraceEnabled()) {
			logger.trace("Sending binary message, " + this);
		}
		Assert.isTrue(isOpen(), "Cannot send message after connection closed.");
		this.session.getBasicRemote().sendBinary(message);
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
