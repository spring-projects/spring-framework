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
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Adapts a Jetty {@link org.eclipse.jetty.websocket.api.Session} to
 * {@link WebSocketSession}.
 *
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class JettyWebSocketSessionAdapter
		extends AbstractWebSocketSesssionAdapter<org.eclipse.jetty.websocket.api.Session> {

	private Session session;

	private Principal principal;

	private String protocol;


	@Override
	public void initSession(Session session) {
		Assert.notNull(session, "session must not be null");
		this.session = session;

		if (this.protocol == null) {
			UpgradeResponse response = session.getUpgradeResponse();
			if ((response != null) && response.getAcceptedSubProtocol() != null) {
				this.protocol = response.getAcceptedSubProtocol();
			}
		}
	}

	@Override
	public String getId() {
		return ObjectUtils.getIdentityHexString(this.session);
	}

	@Override
	public boolean isSecure() {
		return this.session.isSecure();
	}

	@Override
	public URI getUri() {
		return this.session.getUpgradeRequest().getRequestURI();
	}

	@Override
	public void setUri(URI uri) {
	}

	@Override
	public Principal getPrincipal() {
		return this.principal;
	}

	@Override
	public void setPrincipal(Principal principal) {
		this.principal = principal;
	}

	@Override
	public String getRemoteHostName() {
		return this.session.getRemoteAddress().getHostName();
	}

	@Override
	public void setRemoteHostName(String address) {
		// ignore
	}

	@Override
	public String getRemoteAddress() {
		InetSocketAddress address = this.session.getRemoteAddress();
		return address.isUnresolved() ? null : address.getAddress().getHostAddress();
	}

	@Override
	public void setRemoteAddress(String address) {
		// ignore
	}

	@Override
	public String getAcceptedProtocol() {
		return this.protocol;
	}

	@Override
	public void setAcceptedProtocol(String protocol) {
		this.protocol = protocol;
	}

	@Override
	public boolean isOpen() {
		return this.session.isOpen();
	}

	@Override
	protected void sendTextMessage(TextMessage message) throws IOException {
		this.session.getRemote().sendString(message.getPayload());
	}

	@Override
	protected void sendBinaryMessage(BinaryMessage message) throws IOException {
		this.session.getRemote().sendBytes(message.getPayload());
	}

	@Override
	protected void closeInternal(CloseStatus status) throws IOException {
		this.session.close(status.getCode(), status.getReason());
	}

}
