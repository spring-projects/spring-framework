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

package org.springframework.web.socket.adapter.jetty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.AbstractWebSocketSession;

/**
 * A {@link WebSocketSession} for use with the Jetty 9 WebSocket API.
 *
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class JettyWebSocketSession extends AbstractWebSocketSession<Session> {

	private HttpHeaders headers;

	private List<WebSocketExtension> extensions;

	private final Principal principal;


	/**
	 * Create a new {@link JettyWebSocketSession} instance.
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
		checkNativeSessionInitialized();
		return ObjectUtils.getIdentityHexString(getNativeSession());
	}

	@Override
	public URI getUri() {
		checkNativeSessionInitialized();
		return getNativeSession().getUpgradeRequest().getRequestURI();
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		checkNativeSessionInitialized();
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			this.headers.putAll(getNativeSession().getUpgradeRequest().getHeaders());
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
		checkNativeSessionInitialized();
		return getNativeSession().getLocalAddress();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		checkNativeSessionInitialized();
		return getNativeSession().getRemoteAddress();
	}

	@Override
	public String getAcceptedProtocol() {
		checkNativeSessionInitialized();
		return getNativeSession().getUpgradeResponse().getAcceptedSubProtocol();
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		checkNativeSessionInitialized();
		if(this.extensions == null) {
			List<ExtensionConfig> source = getNativeSession().getUpgradeResponse().getExtensions();
			this.extensions = new ArrayList<WebSocketExtension>(source.size());
			for(ExtensionConfig e : source) {
				this.extensions.add(new WebSocketExtension(e.getName(), e.getParameters()));
			}
		}
		return this.extensions;
	}

	@Override
	public boolean isOpen() {
		return ((getNativeSession() != null) && getNativeSession().isOpen());
	}

	@Override
	protected void sendTextMessage(TextMessage message) throws IOException {
		getNativeSession().getRemote().sendString(message.getPayload());
	}

	@Override
	protected void sendBinaryMessage(BinaryMessage message) throws IOException {
		getNativeSession().getRemote().sendBytes(message.getPayload());
	}

	@Override
	protected void sendPingMessage(PingMessage message) throws IOException {
		getNativeSession().getRemote().sendPing(message.getPayload());
	}

	@Override
	protected void sendPongMessage(PongMessage message) throws IOException {
		getNativeSession().getRemote().sendPong(message.getPayload());
	}

	@Override
	protected void closeInternal(CloseStatus status) throws IOException {
		getNativeSession().close(status.getCode(), status.getReason());
	}

}
