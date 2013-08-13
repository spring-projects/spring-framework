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
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * A {@link WebSocketSession} for use with the Jetty 9 WebSocket API.
 *
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class JettyWebSocketSession extends AbstractWebSocketSesssion<org.eclipse.jetty.websocket.api.Session> {

	private HttpHeaders headers;

	private final Principal principal;


	/**
	 * Class constructor.
	 *
	 * @param principal the user associated with the session, or {@code null}
	 * @param handshakeAttributes attributes from the HTTP handshake to make available
	 *        through the WebSocket session
	 */
	public JettyWebSocketSession(Principal principal, Map<String, Object> handshakeAttributes) {
		super(handshakeAttributes);
		this.principal = principal;
	}


	@Override
	public String getId() {
		checkDelegateSessionInitialized();
		return ObjectUtils.getIdentityHexString(getDelegateSession());
	}

	@Override
	public URI getUri() {
		checkDelegateSessionInitialized();
		return getDelegateSession().getUpgradeRequest().getRequestURI();
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		checkDelegateSessionInitialized();
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			this.headers.putAll(getDelegateSession().getUpgradeRequest().getHeaders());
			this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
		}
		return this.headers;
	}

	@Override
	public Principal getPrincipal() {
		return this.principal;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		checkDelegateSessionInitialized();
		return getDelegateSession().getLocalAddress();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		checkDelegateSessionInitialized();
		return getDelegateSession().getRemoteAddress();
	}

	@Override
	public String getAcceptedProtocol() {
		checkDelegateSessionInitialized();
		return getDelegateSession().getUpgradeResponse().getAcceptedSubProtocol();
	}

	@Override
	public boolean isOpen() {
		return ((getDelegateSession() != null) && getDelegateSession().isOpen());
	}

	@Override
	protected void sendTextMessage(TextMessage message) throws IOException {
		getDelegateSession().getRemote().sendString(message.getPayload());
	}

	@Override
	protected void sendBinaryMessage(BinaryMessage message) throws IOException {
		getDelegateSession().getRemote().sendBytes(message.getPayload());
	}

	@Override
	protected void closeInternal(CloseStatus status) throws IOException {
		getDelegateSession().close(status.getCode(), status.getReason());
	}

}
