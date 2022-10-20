/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.websocket.api.ExtensionConfig;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.AbstractWebSocketSession;

/**
 * A {@link WebSocketSession} for use with the Jetty 9.4 WebSocket API.
 *
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 4.0
 */
public class JettyWebSocketSession extends AbstractWebSocketSession<Session> {

	private final String id;

	@Nullable
	private URI uri;

	@Nullable
	private HttpHeaders headers;

	@Nullable
	private String acceptedProtocol;

	@Nullable
	private List<WebSocketExtension> extensions;

	@Nullable
	private Principal user;


	/**
	 * Create a new {@link JettyWebSocketSession} instance.
	 * @param attributes the attributes from the HTTP handshake to associate with the WebSocket session
	 */
	public JettyWebSocketSession(Map<String, Object> attributes) {
		this(attributes, null);
	}

	/**
	 * Create a new {@link JettyWebSocketSession} instance associated with the given user.
	 * @param attributes the attributes from the HTTP handshake to associate with the WebSocket
	 * session; the provided attributes are copied, the original map is not used.
	 * @param user the user associated with the session; if {@code null} we'll fall back on the
	 * user available via {@link org.eclipse.jetty.websocket.api.Session#getUpgradeRequest()}
	 */
	public JettyWebSocketSession(Map<String, Object> attributes, @Nullable Principal user) {
		super(attributes);
		this.id = idGenerator.generateId().toString();
		this.user = user;
	}


	@Override
	public String getId() {
		return this.id;
	}

	@Override
	@Nullable
	public URI getUri() {
		checkNativeSessionInitialized();
		return this.uri;
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		Assert.state(this.headers != null, "WebSocket session is not yet initialized");
		return this.headers;
	}

	@Override
	public String getAcceptedProtocol() {
		checkNativeSessionInitialized();
		return this.acceptedProtocol;
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		Assert.state(this.extensions != null, "WebSocket session is not yet initialized");
		return this.extensions;
	}

	@Override
	public Principal getPrincipal() {
		return this.user;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		checkNativeSessionInitialized();
		return (InetSocketAddress) getNativeSession().getLocalAddress();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		checkNativeSessionInitialized();
		return (InetSocketAddress) getNativeSession().getRemoteAddress();
	}

	/**
	 * This method is a no-op for Jetty. As per {@link Session#getPolicy()}, the
	 * returned {@code WebSocketPolicy} is read-only and changing it has no effect.
	 */
	@Override
	public void setTextMessageSizeLimit(int messageSizeLimit) {
	}

	@Override
	public int getTextMessageSizeLimit() {
		checkNativeSessionInitialized();
		return (int) getNativeSession().getMaxTextMessageSize();
	}

	/**
	 * This method is a no-op for Jetty. As per {@link Session#getPolicy()}, the
	 * returned {@code WebSocketPolicy} is read-only and changing it has no effect.
	 */
	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		checkNativeSessionInitialized();
		return (int) getNativeSession().getMaxBinaryMessageSize();
	}

	@Override
	public boolean isOpen() {
		return getNativeSession().isOpen();
	}


	@Override
	public void initializeNativeSession(Session session) {
		super.initializeNativeSession(session);

		this.uri = session.getUpgradeRequest().getRequestURI();

		HttpHeaders headers = new HttpHeaders();
		Map<String, List<String>> nativeHeaders = session.getUpgradeRequest().getHeaders();
		if (!CollectionUtils.isEmpty(nativeHeaders)) {
			headers.putAll(nativeHeaders);
		}
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);

		this.acceptedProtocol = session.getUpgradeResponse().getAcceptedSubProtocol();
		this.extensions = getExtensions(session);

		if (this.user == null) {
			this.user = session.getUpgradeRequest().getUserPrincipal();
		}
	}

	private List<WebSocketExtension> getExtensions(Session session) {
		List<ExtensionConfig> configs = session.getUpgradeResponse().getExtensions();
		if (!CollectionUtils.isEmpty(configs)) {
			List<WebSocketExtension> result = new ArrayList<>(configs.size());
			for (ExtensionConfig config : configs) {
				result.add(new WebSocketExtension(config.getName(), config.getParameters()));
			}
			return Collections.unmodifiableList(result);
		}
		return Collections.emptyList();
	}


	@Override
	protected void sendTextMessage(TextMessage message) throws IOException {
		getRemoteEndpoint().sendString(message.getPayload());
	}

	@Override
	protected void sendBinaryMessage(BinaryMessage message) throws IOException {
		getRemoteEndpoint().sendBytes(message.getPayload());
	}

	@Override
	protected void sendPingMessage(PingMessage message) throws IOException {
		getRemoteEndpoint().sendPing(message.getPayload());
	}

	@Override
	protected void sendPongMessage(PongMessage message) throws IOException {
		getRemoteEndpoint().sendPong(message.getPayload());
	}

	private RemoteEndpoint getRemoteEndpoint() {
		return getNativeSession().getRemote();
	}

	@Override
	protected void closeInternal(CloseStatus status) throws IOException {
		getNativeSession().close(status.getCode(), status.getReason());
	}

}
