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

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * A {@link WebSocketSession} for use with the standard WebSocket for Java API.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketSession extends AbstractWebSocketSesssion<javax.websocket.Session> {

	private final HttpHeaders headers;

	private final InetSocketAddress localAddress;

	private final InetSocketAddress remoteAddress;


	/**
	 * Class constructor.
	 *
	 * @param handshakeHeaders the headers of the handshake request
	 */
	public StandardWebSocketSession(HttpHeaders handshakeHeaders, InetSocketAddress localAddress,
			InetSocketAddress remoteAddress) {

		handshakeHeaders = (handshakeHeaders != null) ? handshakeHeaders : new HttpHeaders();
		this.headers = HttpHeaders.readOnlyHttpHeaders(handshakeHeaders);
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
	}

	@Override
	public String getId() {
		checkDelegateSessionInitialized();
		return getDelegateSession().getId();
	}

	@Override
	public URI getUri() {
		checkDelegateSessionInitialized();
		return getDelegateSession().getRequestURI();
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		return this.headers;
	}

	@Override
	public Principal getPrincipal() {
		checkDelegateSessionInitialized();
		return getDelegateSession().getUserPrincipal();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return this.localAddress;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	@Override
	public String getAcceptedProtocol() {
		checkDelegateSessionInitialized();
		String protocol = getDelegateSession().getNegotiatedSubprotocol();
		return StringUtils.isEmpty(protocol)? null : protocol;
	}

	@Override
	public boolean isOpen() {
		return ((getDelegateSession() != null) && getDelegateSession().isOpen());
	}

	@Override
	protected void sendTextMessage(TextMessage message) throws IOException {
		getDelegateSession().getBasicRemote().sendText(message.getPayload(), message.isLast());
	}

	@Override
	protected void sendBinaryMessage(BinaryMessage message) throws IOException {
		getDelegateSession().getBasicRemote().sendBinary(message.getPayload(), message.isLast());
	}

	@Override
	protected void closeInternal(CloseStatus status) throws IOException {
		getDelegateSession().close(new CloseReason(CloseCodes.getCloseCode(status.getCode()), status.getReason()));
	}

}
