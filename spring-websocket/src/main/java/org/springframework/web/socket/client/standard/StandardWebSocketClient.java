/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.socket.client.standard;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLContext;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Endpoint;
import jakarta.websocket.Extension;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.WebSocketContainer;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.FutureUtils;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import org.springframework.web.socket.adapter.standard.WebSocketToStandardExtensionAdapter;
import org.springframework.web.socket.client.AbstractWebSocketClient;

/**
 * A WebSocketClient based on the standard Jakarta WebSocket API.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public class StandardWebSocketClient extends AbstractWebSocketClient {

	private final WebSocketContainer webSocketContainer;

	private final Map<String,Object> userProperties = new HashMap<>();

	@Nullable
	private SSLContext sslContext;

	@Nullable
	private AsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();


	/**
	 * Default constructor that calls {@code ContainerProvider.getWebSocketContainer()}
	 * to obtain a (new) {@link WebSocketContainer} instance. Also see constructor
	 * accepting existing {@code WebSocketContainer} instance.
	 */
	public StandardWebSocketClient() {
		this.webSocketContainer = ContainerProvider.getWebSocketContainer();
	}

	/**
	 * Constructor accepting an existing {@link WebSocketContainer} instance.
	 * <p>For XML configuration, see {@link WebSocketContainerFactoryBean}. For Java
	 * configuration, use {@code ContainerProvider.getWebSocketContainer()} to obtain
	 * the {@code WebSocketContainer} instance.
	 */
	public StandardWebSocketClient(WebSocketContainer webSocketContainer) {
		Assert.notNull(webSocketContainer, "WebSocketContainer must not be null");
		this.webSocketContainer = webSocketContainer;
	}


	/**
	 * The standard Jakarta WebSocket API allows passing "user properties" to the
	 * server via {@link ClientEndpointConfig#getUserProperties() userProperties}.
	 * Use this property to configure one or more properties to be passed on
	 * every handshake.
	 */
	public void setUserProperties(@Nullable Map<String, Object> userProperties) {
		if (userProperties != null) {
			this.userProperties.putAll(userProperties);
		}
	}

	/**
	 * Return the configured user properties.
	 */
	public Map<String, Object> getUserProperties() {
		return this.userProperties;
	}

	/**
	 * Set the {@link SSLContext} to use for {@link ClientEndpointConfig#getSSLContext()}.
	 * @since 6.1.3
	 */
	public void setSslContext(@Nullable SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	/**
	 * Return the {@link SSLContext} to use.
	 * @since 6.1.3
	 */
	@Nullable
	public SSLContext getSslContext() {
		return this.sslContext;
	}

	/**
	 * Set an {@link AsyncTaskExecutor} to use when opening connections.
	 * <p>If this property is set to {@code null}, calls to any of the
	 * {@code doHandshake} methods will block until the connection is established.
	 * <p>By default, an instance of {@code SimpleAsyncTaskExecutor} is used.
	 */
	public void setTaskExecutor(@Nullable AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Return the configured {@link AsyncTaskExecutor}.
	 */
	@Nullable
	public AsyncTaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}


	@Override
	protected CompletableFuture<WebSocketSession> executeInternal(WebSocketHandler webSocketHandler,
			HttpHeaders headers, final URI uri, List<String> protocols,
			List<WebSocketExtension> extensions, Map<String, Object> attributes) {

		int port = getPort(uri);
		InetSocketAddress localAddress = new InetSocketAddress(getLocalHost(), port);
		InetSocketAddress remoteAddress = new InetSocketAddress(uri.getHost(), port);

		StandardWebSocketSession session = new StandardWebSocketSession(headers,
				attributes, localAddress, remoteAddress);

		ClientEndpointConfig endpointConfig = ClientEndpointConfig.Builder.create()
				.configurator(new StandardWebSocketClientConfigurator(headers))
				.preferredSubprotocols(protocols)
				.extensions(adaptExtensions(extensions))
				.sslContext(getSslContext())
				.build();

		endpointConfig.getUserProperties().putAll(getUserProperties());

		Endpoint endpoint = new StandardWebSocketHandlerAdapter(webSocketHandler, session);

		Callable<WebSocketSession> connectTask = () -> {
			this.webSocketContainer.connectToServer(endpoint, endpointConfig, uri);
			return session;
		};

		if (this.taskExecutor != null) {
			return FutureUtils.callAsync(connectTask, this.taskExecutor);
		}
		else {
			return FutureUtils.callAsync(connectTask);
		}
	}

	private static List<Extension> adaptExtensions(List<WebSocketExtension> extensions) {
		List<Extension> result = new ArrayList<>();
		for (WebSocketExtension extension : extensions) {
			result.add(new WebSocketToStandardExtensionAdapter(extension));
		}
		return result;
	}

	private static InetAddress getLocalHost() {
		try {
			return InetAddress.getLocalHost();
		}
		catch (UnknownHostException ex) {
			return InetAddress.getLoopbackAddress();
		}
	}

	private static int getPort(URI uri) {
		if (uri.getPort() == -1) {
			String scheme = uri.getScheme().toLowerCase(Locale.ENGLISH);
			return ("wss".equals(scheme) ? 443 : 80);
		}
		return uri.getPort();
	}


	private class StandardWebSocketClientConfigurator extends Configurator {

		private final HttpHeaders headers;

		public StandardWebSocketClientConfigurator(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public void beforeRequest(Map<String, List<String>> requestHeaders) {
			requestHeaders.putAll(this.headers);
			if (logger.isTraceEnabled()) {
				logger.trace("Handshake request headers: " + requestHeaders);
			}
		}
		@Override
		public void afterResponse(HandshakeResponse response) {
			if (logger.isTraceEnabled()) {
				logger.trace("Handshake response headers: " + response.getHeaders());
			}
		}
	}

}
