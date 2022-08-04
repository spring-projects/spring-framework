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
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
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

	private static final ClassLoader loader = JettyWebSocketSession.class.getClassLoader();

	private static final boolean jetty10Present = ClassUtils.isPresent(
			"org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer", loader);


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

	private final SessionHelper sessionHelper;


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
		this.sessionHelper = (jetty10Present ? new Jetty10SessionHelper() : new Jetty9SessionHelper());
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
		return this.sessionHelper.getLocalAddress(getNativeSession());
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		checkNativeSessionInitialized();
		return this.sessionHelper.getRemoteAddress(getNativeSession());
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
		return this.sessionHelper.getTextMessageSizeLimit(getNativeSession());
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
		return this.sessionHelper.getBinaryMessageSizeLimit(getNativeSession());
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
		this.extensions = this.sessionHelper.getExtensions(session);

		if (this.user == null) {
			this.user = session.getUpgradeRequest().getUserPrincipal();
		}
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


	/**
	 * Encapsulate incompatible changes between Jetty 9.4 and 10.
	 */
	private interface SessionHelper {

		List<WebSocketExtension> getExtensions(Session session);

		int getTextMessageSizeLimit(Session session);

		int getBinaryMessageSizeLimit(Session session);

		InetSocketAddress getRemoteAddress(Session session);

		InetSocketAddress getLocalAddress(Session session);

	}


	private static class Jetty9SessionHelper implements SessionHelper {

		@Override
		public List<WebSocketExtension> getExtensions(Session session) {
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
		public int getTextMessageSizeLimit(Session session) {
			return session.getPolicy().getMaxTextMessageSize();
		}

		@Override
		public int getBinaryMessageSizeLimit(Session session) {
			return session.getPolicy().getMaxBinaryMessageSize();
		}

		@Override
		public InetSocketAddress getRemoteAddress(Session session) {
			return session.getRemoteAddress();
		}

		@Override
		public InetSocketAddress getLocalAddress(Session session) {
			return session.getLocalAddress();
		}
	}


	private static class Jetty10SessionHelper implements SessionHelper {

		private static final Method getTextMessageSizeLimitMethod;

		private static final Method getBinaryMessageSizeLimitMethod;

		private static final Method getRemoteAddressMethod;

		private static final Method getLocalAddressMethod;

		static {
			try {
				Class<?> type = loader.loadClass("org.eclipse.jetty.websocket.api.Session");
				getTextMessageSizeLimitMethod = type.getMethod("getMaxTextMessageSize");
				getBinaryMessageSizeLimitMethod = type.getMethod("getMaxBinaryMessageSize");
				getRemoteAddressMethod = type.getMethod("getRemoteAddress");
				getLocalAddressMethod = type.getMethod("getLocalAddress");
			}
			catch (ClassNotFoundException | NoSuchMethodException ex) {
				throw new IllegalStateException("No compatible Jetty version found", ex);
			}
		}

		// TODO: Extension info can't be accessed without compiling against Jetty 10
		//   Jetty 10: org.eclipse.jetty.websocket.api.ExtensionConfig
		//   Jetty  9: org.eclipse.jetty.websocket.api.extensions.ExtensionConfig

		@Override
		public List<WebSocketExtension> getExtensions(Session session) {
			return Collections.emptyList();
		}

		// TODO: WebSocketPolicy can't be accessed without compiling against Jetty 10 (class -> interface)

		@Override
		@SuppressWarnings("ConstantConditions")
		public int getTextMessageSizeLimit(Session session) {
			long result = (long) ReflectionUtils.invokeMethod(getTextMessageSizeLimitMethod, session.getPolicy());
			Assert.state(result <= Integer.MAX_VALUE, "textMessageSizeLimit is larger than Integer.MAX_VALUE");
			return (int) result;
		}

		@Override
		@SuppressWarnings("ConstantConditions")
		public int getBinaryMessageSizeLimit(Session session) {
			long result = (long) ReflectionUtils.invokeMethod(getBinaryMessageSizeLimitMethod, session.getPolicy());
			Assert.state(result <= Integer.MAX_VALUE, "binaryMessageSizeLimit is larger than Integer.MAX_VALUE");
			return (int) result;
		}

		@Override
		@SuppressWarnings("ConstantConditions")
		public InetSocketAddress getRemoteAddress(Session session) {
			SocketAddress address = (SocketAddress) ReflectionUtils.invokeMethod(getRemoteAddressMethod, session);
			Assert.isInstanceOf(InetSocketAddress.class, address);
			return (InetSocketAddress) address;
		}

		@Override
		@SuppressWarnings("ConstantConditions")
		public InetSocketAddress getLocalAddress(Session session) {
			SocketAddress address = (SocketAddress) ReflectionUtils.invokeMethod(getLocalAddressMethod, session);
			Assert.isInstanceOf(InetSocketAddress.class, address);
			return (InetSocketAddress) address;
		}
	}

}
