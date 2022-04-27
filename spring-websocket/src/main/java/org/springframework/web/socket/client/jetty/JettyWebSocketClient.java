/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.socket.client.jetty;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import org.springframework.context.Lifecycle;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.jetty.Jetty10WebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketHandlerAdapter;
import org.springframework.web.socket.adapter.jetty.JettyWebSocketSession;
import org.springframework.web.socket.adapter.jetty.WebSocketToJettyExtensionConfigAdapter;
import org.springframework.web.socket.client.AbstractWebSocketClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Initiates WebSocket requests to a WebSocket server programmatically
 * through the Jetty WebSocket API.
 *
 * <p>As of 4.1 this class implements {@link Lifecycle} rather than
 * {@link org.springframework.context.SmartLifecycle}. Use
 * {@link org.springframework.web.socket.client.WebSocketConnectionManager
 * WebSocketConnectionManager} instead to auto-start a WebSocket connection.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class JettyWebSocketClient extends AbstractWebSocketClient implements Lifecycle {

	private static ClassLoader loader = JettyWebSocketClient.class.getClassLoader();

	private static final boolean jetty10Present;

	private static final Method setHeadersMethod;

	static {
		jetty10Present = ClassUtils.isPresent(
				"org.eclipse.jetty.websocket.client.JettyUpgradeListener", loader);
		try {
			setHeadersMethod = ClientUpgradeRequest.class.getMethod("setHeaders", Map.class);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException("No compatible Jetty version found", ex);
		}
	}


	private final org.eclipse.jetty.websocket.client.WebSocketClient client;

	@Nullable
	private AsyncListenableTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	private final UpgradeHelper upgradeHelper =
			(jetty10Present ? new Jetty10UpgradeHelper() : new Jetty9UpgradeHelper());


	/**
	 * Default constructor that creates an instance of
	 * {@link org.eclipse.jetty.websocket.client.WebSocketClient}.
	 */
	public JettyWebSocketClient() {
		this.client = new org.eclipse.jetty.websocket.client.WebSocketClient();
	}

	/**
	 * Constructor that accepts an existing
	 * {@link org.eclipse.jetty.websocket.client.WebSocketClient} instance.
	 */
	public JettyWebSocketClient(WebSocketClient client) {
		this.client = client;
	}


	/**
	 * Set an {@link AsyncListenableTaskExecutor} to use when opening connections.
	 * If this property is set to {@code null}, calls to any of the
	 * {@code doHandshake} methods will block until the connection is established.
	 * <p>By default an instance of {@code SimpleAsyncTaskExecutor} is used.
	 */
	public void setTaskExecutor(@Nullable AsyncListenableTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Return the configured {@link TaskExecutor}.
	 */
	@Nullable
	public AsyncListenableTaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}


	@Override
	public void start() {
		try {
			this.client.start();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to start Jetty WebSocketClient", ex);
		}
	}

	@Override
	public void stop() {
		try {
			this.client.stop();
		}
		catch (Exception ex) {
			logger.error("Failed to stop Jetty WebSocketClient", ex);
		}
	}

	@Override
	public boolean isRunning() {
		return this.client.isStarted();
	}


	@Override
	public ListenableFuture<WebSocketSession> doHandshake(WebSocketHandler webSocketHandler,
			String uriTemplate, Object... uriVars) {

		UriComponents uriComponents = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode();
		return doHandshake(webSocketHandler, null, uriComponents.toUri());
	}

	@Override
	public ListenableFuture<WebSocketSession> doHandshakeInternal(WebSocketHandler wsHandler,
			HttpHeaders headers, final URI uri, List<String> protocols,
			List<WebSocketExtension> extensions,  Map<String, Object> attributes) {

		final ClientUpgradeRequest request = new ClientUpgradeRequest();
		request.setSubProtocols(protocols);

		for (WebSocketExtension e : extensions) {
			request.addExtensions(new WebSocketToJettyExtensionConfigAdapter(e));
		}

		// Jetty  9: setHeaders declared in UpgradeRequestAdapter base class
		// Jetty 10: setHeaders declared in ClientUpgradeRequest
		ReflectionUtils.invokeMethod(setHeadersMethod, request, headers);

		Principal user = getUser();
		JettyWebSocketSession wsSession = new JettyWebSocketSession(attributes, user);

		Callable<WebSocketSession> connectTask = () -> {
			Future<Session> future = this.upgradeHelper.connect(this.client, uri, request, wsHandler, wsSession);
			future.get(this.client.getConnectTimeout() + 2000, TimeUnit.MILLISECONDS);
			return wsSession;
		};

		if (this.taskExecutor != null) {
			return this.taskExecutor.submitListenable(connectTask);
		}
		else {
			ListenableFutureTask<WebSocketSession> task = new ListenableFutureTask<>(connectTask);
			task.run();
			return task;
		}
	}

	/**
	 * Return the user to make available through {@link WebSocketSession#getPrincipal()}.
	 * By default this method returns {@code null}
	 */
	@Nullable
	protected Principal getUser() {
		return null;
	}


	/**
	 * Encapsulate incompatible changes between Jetty 9.4 and 10.
	 */
	private interface UpgradeHelper {

		Future<Session> connect(WebSocketClient client, URI url, ClientUpgradeRequest request,
				WebSocketHandler handler, JettyWebSocketSession session) throws IOException;
	}


	private static class Jetty9UpgradeHelper implements UpgradeHelper {

		@Override
		public Future<Session> connect(WebSocketClient client, URI url, ClientUpgradeRequest request,
				WebSocketHandler handler, JettyWebSocketSession session) throws IOException {

			JettyWebSocketHandlerAdapter adapter = new JettyWebSocketHandlerAdapter(handler, session);
			return client.connect(adapter, url, request);
		}
	}


	private static class Jetty10UpgradeHelper implements UpgradeHelper {

		// On Jetty 9 returns Future, on Jetty 10 returns CompletableFuture
		private static final Method connectMethod;

		static {
			try {
				Class<?> type = loader.loadClass("org.eclipse.jetty.websocket.client.WebSocketClient");
				connectMethod = type.getMethod("connect", Object.class, URI.class, ClientUpgradeRequest.class);
			}
			catch (ClassNotFoundException | NoSuchMethodException ex) {
				throw new IllegalStateException("No compatible Jetty version found", ex);
			}
		}

		@Override
		@SuppressWarnings({"ConstantConditions", "unchecked"})
		public Future<Session> connect(WebSocketClient client, URI url, ClientUpgradeRequest request,
				WebSocketHandler handler, JettyWebSocketSession session) {

			Jetty10WebSocketHandlerAdapter adapter = new Jetty10WebSocketHandlerAdapter(handler, session);

			// TODO: pass JettyUpgradeListener argument to set headers from HttpHeaders (like we do for Jetty 9)
			//  which would require a JDK Proxy since it is new in Jetty 10
			return (Future<Session>) ReflectionUtils.invokeMethod(connectMethod, client, adapter, url, request);
		}
	}

}
