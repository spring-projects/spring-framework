/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.socket.adapter.standard;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.Extension;
import jakarta.websocket.Session;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
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
 * A {@link WebSocketSession} for use with the standard WebSocket for Java API.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketSession extends AbstractWebSocketSession<Session> {

	private final String id;

	private @Nullable URI uri;

	private final HttpHeaders handshakeHeaders;

	private @Nullable String acceptedProtocol;

	private @Nullable List<WebSocketExtension> extensions;

	private @Nullable Principal user;

	private final @Nullable InetSocketAddress localAddress;

	private final @Nullable InetSocketAddress remoteAddress;


	/**
	 * Constructor for a standard WebSocket session.
	 * @param headers the headers of the handshake request
	 * @param attributes the attributes from the HTTP handshake to associate with the WebSocket
	 * session; the provided attributes are copied, the original map is not used.
	 * @param localAddress the address on which the request was received
	 * @param remoteAddress the address of the remote client
	 */
	public StandardWebSocketSession(@Nullable HttpHeaders headers, @Nullable Map<String, Object> attributes,
			@Nullable InetSocketAddress localAddress, @Nullable InetSocketAddress remoteAddress) {

		this(headers, attributes, localAddress, remoteAddress, null);
	}

	/**
	 * Constructor that associates a user with the WebSocket session.
	 * @param headers the headers of the handshake request
	 * @param attributes the attributes from the HTTP handshake to associate with the WebSocket session
	 * @param localAddress the address on which the request was received
	 * @param remoteAddress the address of the remote client
	 * @param user the user associated with the session; if {@code null} we'll
	 * fall back on the user available in the underlying WebSocket session
	 */
	public StandardWebSocketSession(@Nullable HttpHeaders headers, @Nullable Map<String, Object> attributes,
			@Nullable InetSocketAddress localAddress, @Nullable InetSocketAddress remoteAddress,
			@Nullable Principal user) {

		super(attributes);
		this.id = idGenerator.generateId().toString();
		headers = (headers != null ? headers : new HttpHeaders());
		this.handshakeHeaders = HttpHeaders.readOnlyHttpHeaders(headers);
		this.user = user;
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
	}


	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public @Nullable URI getUri() {
		checkNativeSessionInitialized();
		return this.uri;
	}

	@Override
	public HttpHeaders getHandshakeHeaders() {
		return this.handshakeHeaders;
	}

	@Override
	public @Nullable String getAcceptedProtocol() {
		checkNativeSessionInitialized();
		return this.acceptedProtocol;
	}

	@Override
	public List<WebSocketExtension> getExtensions() {
		Assert.state(this.extensions != null, "WebSocket session is not yet initialized");
		return this.extensions;
	}

	@Override
	public @Nullable Principal getPrincipal() {
		return this.user;
	}

	@Override
	public @Nullable InetSocketAddress getLocalAddress() {
		return this.localAddress;
	}

	@Override
	public @Nullable InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	@Override
	public void setTextMessageSizeLimit(int messageSizeLimit) {
		checkNativeSessionInitialized();
		getNativeSession().setMaxTextMessageBufferSize(messageSizeLimit);
	}

	@Override
	public int getTextMessageSizeLimit() {
		checkNativeSessionInitialized();
		return getNativeSession().getMaxTextMessageBufferSize();
	}

	@Override
	public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		checkNativeSessionInitialized();
		getNativeSession().setMaxBinaryMessageBufferSize(messageSizeLimit);
	}

	@Override
	public int getBinaryMessageSizeLimit() {
		checkNativeSessionInitialized();
		return getNativeSession().getMaxBinaryMessageBufferSize();
	}

	@Override
	public boolean isOpen() {
		return getNativeSession().isOpen();
	}

	@Override
	public void initializeNativeSession(Session session) {
		super.initializeNativeSession(session);

		this.uri = session.getRequestURI();
		this.acceptedProtocol = session.getNegotiatedSubprotocol();

		List<Extension> standardExtensions = getNativeSession().getNegotiatedExtensions();
		if (!CollectionUtils.isEmpty(standardExtensions)) {
			this.extensions = new ArrayList<>(standardExtensions.size());
			for (Extension standardExtension : standardExtensions) {
				this.extensions.add(new StandardToWebSocketExtensionAdapter(standardExtension));
			}
			this.extensions = Collections.unmodifiableList(this.extensions);
		}
		else {
			this.extensions = Collections.emptyList();
		}

		if (this.user == null) {
			this.user = session.getUserPrincipal();
		}
	}

	@Override
	protected void sendTextMessage(TextMessage message) throws IOException {
		getNativeSession().getBasicRemote().sendText(message.getPayload(), message.isLast());
	}

	@Override
	protected void sendBinaryMessage(BinaryMessage message) throws IOException {
		getNativeSession().getBasicRemote().sendBinary(message.getPayload(), message.isLast());
	}

	@Override
	protected void sendPingMessage(PingMessage message) throws IOException {
		getNativeSession().getBasicRemote().sendPing(message.getPayload());
	}

	@Override
	protected void sendPongMessage(PongMessage message) throws IOException {
		getNativeSession().getBasicRemote().sendPong(message.getPayload());
	}

	@Override
	protected void closeInternal(CloseStatus status) throws IOException {
		getNativeSession().close(new CloseReason(CloseCodes.getCloseCode(status.getCode()), status.getReason()));
	}

}
