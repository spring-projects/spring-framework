/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.HandshakeResponse;
import javax.websocket.WebSocketContainer;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.standard.StandardWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.standard.StandardWebSocketSession;
import org.springframework.web.socket.adapter.standard.WebSocketToStandardExtensionAdapter;
import org.springframework.web.socket.client.AbstractWebSocketClient;

/**
 * Initiates WebSocket requests to a WebSocket server programmatically
 * through the standard Java WebSocket API.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketClient extends AbstractWebSocketClient {

	private final WebSocketContainer webSocketContainer;

	private AsyncListenableTaskExecutor taskExecutor;


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
	 *
	 * <p>For XML configuration see {@link WebSocketContainerFactoryBean}. For Java
	 * configuration use {@code ContainerProvider.getWebSocketContainer()} to obtain
	 * the {@code WebSocketContainer} instance.
	 */
	public StandardWebSocketClient(WebSocketContainer webSocketContainer) {
		Assert.notNull(webSocketContainer, "WebSocketContainer must not be null");
		this.webSocketContainer = webSocketContainer;
	}


	/**
	 * Set an {@link AsyncListenableTaskExecutor} to use when opening connections.
	 *
	 * <p>If this property is not configured, calls to  any of the
	 * {@code doHandshake} methods will block until the connection is established.
	 */
	public void setTaskExecutor(AsyncListenableTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Return the configured {@link TaskExecutor}.
	 */
	public AsyncListenableTaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}


	@Override
	protected ListenableFuture<WebSocketSession> doHandshakeInternal(WebSocketHandler webSocketHandler,
			HttpHeaders headers, final URI uri, List<String> protocols,
			List<WebSocketExtension> extensions, Map<String, Object> handshakeAttributes) {

		int port = getPort(uri);
		InetSocketAddress localAddress = new InetSocketAddress(getLocalHost(), port);
		InetSocketAddress remoteAddress = new InetSocketAddress(uri.getHost(), port);

		final StandardWebSocketSession session = new StandardWebSocketSession(headers,
				handshakeAttributes, localAddress, remoteAddress);

		final ClientEndpointConfig.Builder configBuidler = ClientEndpointConfig.Builder.create();
		configBuidler.configurator(new StandardWebSocketClientConfigurator(headers));
		configBuidler.preferredSubprotocols(protocols);
		configBuidler.extensions(adaptExtensions(extensions));
		final Endpoint endpoint = new StandardWebSocketHandlerAdapter(webSocketHandler, session);

		Callable<WebSocketSession> connectTask = new Callable<WebSocketSession>() {
			@Override
			public WebSocketSession call() throws Exception {
				webSocketContainer.connectToServer(endpoint, configBuidler.build(), uri);
				return session;
			}
		};

		if (this.taskExecutor != null) {
			return this.taskExecutor.submitListenable(connectTask);
		}
		else {
			ListenableFutureTask<WebSocketSession> task = new ListenableFutureTask<WebSocketSession>(connectTask);
			task.run();
			return task;
		}
	}

	private static List<Extension> adaptExtensions(List<WebSocketExtension> extensions) {
		List<Extension> result = new ArrayList<Extension>();
		for (WebSocketExtension e : extensions) {
			result.add(new WebSocketToStandardExtensionAdapter(e));
		}
		return result;
	}

	private InetAddress getLocalHost() {
		try {
			return InetAddress.getLocalHost();
		}
		catch (UnknownHostException ex) {
			return InetAddress.getLoopbackAddress();
		}
	}

	private int getPort(URI uri) {
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
			if (logger.isDebugEnabled()) {
				logger.debug("Handshake request headers: " + requestHeaders);
			}
		}
		@Override
		public void afterResponse(HandshakeResponse response) {
			if (logger.isDebugEnabled()) {
				logger.debug("Handshake response headers: " + response.getHeaders());
			}
		}
	}

}
