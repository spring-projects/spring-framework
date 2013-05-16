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

import java.io.IOException;
import java.net.URI;
import java.security.Principal;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import org.springframework.util.Assert;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Adapts a standard {@link javax.websocket.Session} to {@link WebSocketSession}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketSessionAdapter extends AbstractWebSocketSesssionAdapter<javax.websocket.Session> {

	private javax.websocket.Session session;

	private URI uri;

	private String remoteHostName;

	private String remoteAddress;


	@Override
	public void initSession(javax.websocket.Session session) {
		Assert.notNull(session, "session is required");
		this.session = session;
	}

	@Override
	public String getId() {
		return this.session.getId();
	}

	@Override
	public URI getUri() {
		return this.uri;
	}

	@Override
	public void setUri(URI uri) {
		this.uri = uri;
	}


	@Override
	public boolean isSecure() {
		return this.session.isSecure();
	}

	@Override
	public Principal getPrincipal() {
		return this.session.getUserPrincipal();
	}

	@Override
	public void setPrincipal(Principal principal) {
		// ignore
	}

	@Override
	public String getRemoteHostName() {
		return this.remoteHostName;
	}

	@Override
	public void setRemoteHostName(String name) {
		this.remoteHostName = name;
	}

	@Override
	public String getRemoteAddress() {
		return this.remoteAddress;
	}

	@Override
	public void setRemoteAddress(String address) {
		this.remoteAddress = address;
	}

	@Override
	public boolean isOpen() {
		return this.session.isOpen();
	}

	@Override
	protected void sendTextMessage(TextMessage message) throws IOException {
		this.session.getBasicRemote().sendText(message.getPayload(), message.isLast());
	}

	@Override
	protected void sendBinaryMessage(BinaryMessage message) throws IOException {
		this.session.getBasicRemote().sendBinary(message.getPayload(), message.isLast());
	}

	@Override
	protected void closeInternal(CloseStatus status) throws IOException {
		this.session.close(new CloseReason(CloseCodes.getCloseCode(status.getCode()), status.getReason()));
	}

}
