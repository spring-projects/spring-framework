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

package org.springframework.web.reactive.socket.client;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.api.UpgradeResponse;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.io.UpgradeListener;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.context.Lifecycle;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.Jetty10WebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.adapter.JettyWebSocketSession;

/**
 * A {@link WebSocketClient} implementation for use with Jetty
 * {@link org.eclipse.jetty.websocket.client.WebSocketClient}.
 *
 * <p><strong>Note: </strong> the Jetty {@code WebSocketClient} requires
 * lifecycle management and must be started and stopped. This is automatically
 * managed when this class is declared as a Spring bean and created with the
 * default constructor. See constructor notes for more details.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class JettyWebSocketClient implements WebSocketClient, Lifecycle {

	private static ClassLoader loader = JettyWebSocketClient.class.getClassLoader();

	private static final boolean jetty10Present;

	static {
		jetty10Present = ClassUtils.isPresent(
				"org.eclipse.jetty.websocket.client.JettyUpgradeListener", loader);
	}


	private static final Log logger = LogFactory.getLog(JettyWebSocketClient.class);


	private final org.eclipse.jetty.websocket.client.WebSocketClient jettyClient;

	private final boolean externallyManaged;

	private final UpgradeHelper upgradeHelper =
			(jetty10Present ? new Jetty10UpgradeHelper() : new Jetty9UpgradeHelper());


	/**
	 * Default constructor that creates and manages an instance of a Jetty
	 * {@link org.eclipse.jetty.websocket.client.WebSocketClient WebSocketClient}.
	 * The instance can be obtained with {@link #getJettyClient()} for further
	 * configuration.
	 *
	 * <p><strong>Note: </strong> When this constructor is used {@link Lifecycle}
	 * methods of this class are delegated to the Jetty {@code WebSocketClient}.
	 */
	public JettyWebSocketClient() {
		this.jettyClient = new org.eclipse.jetty.websocket.client.WebSocketClient();
		this.externallyManaged = false;
	}

	/**
	 * Constructor that accepts an existing instance of a Jetty
	 * {@link org.eclipse.jetty.websocket.client.WebSocketClient WebSocketClient}.
	 *
	 * <p><strong>Note: </strong> Use of this constructor implies the Jetty
	 * {@code WebSocketClient} is externally managed and hence {@link Lifecycle}
	 * methods of this class are not delegated to it.
	 */
	public JettyWebSocketClient(org.eclipse.jetty.websocket.client.WebSocketClient jettyClient) {
		this.jettyClient = jettyClient;
		this.externallyManaged = true;
	}


	/**
	 * Return the underlying Jetty {@code WebSocketClient}.
	 */
	public org.eclipse.jetty.websocket.client.WebSocketClient getJettyClient() {
		return this.jettyClient;
	}


	@Override
	public void start() {
		if (!this.externallyManaged) {
			try {
				this.jettyClient.start();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to start Jetty WebSocketClient", ex);
			}
		}
	}

	@Override
	public void stop() {
		if (!this.externallyManaged) {
			try {
				this.jettyClient.stop();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Error stopping Jetty WebSocketClient", ex);
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.jettyClient.isRunning();
	}


	@Override
	public Mono<Void> execute(URI url, WebSocketHandler handler) {
		return execute(url, new HttpHeaders(), handler);
	}

	@Override
	public Mono<Void> execute(URI url, HttpHeaders headers, WebSocketHandler handler) {
		return executeInternal(url, headers, handler);
	}

	private Mono<Void> executeInternal(URI url, HttpHeaders headers, WebSocketHandler handler) {
		Sinks.Empty<Void> completionSink = Sinks.empty();
		return Mono.deferContextual(contextView -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Connecting to " + url);
			}
			Object jettyHandler = createHandler(
					url, ContextWebSocketHandler.decorate(handler, contextView), completionSink);
			ClientUpgradeRequest request = new ClientUpgradeRequest();
			request.setSubProtocols(handler.getSubProtocols());
			return this.upgradeHelper.upgrade(
					this.jettyClient, jettyHandler, url, request, headers, completionSink);
		});
	}

	private Object createHandler(URI url, WebSocketHandler handler, Sinks.Empty<Void> completion) {
		Function<Session, JettyWebSocketSession> sessionFactory = session -> {
			HandshakeInfo info = createHandshakeInfo(url, session);
			return new JettyWebSocketSession(session, info, DefaultDataBufferFactory.sharedInstance, completion);
		};
		return (jetty10Present ?
				new Jetty10WebSocketHandlerAdapter(handler, sessionFactory) :
				new JettyWebSocketHandlerAdapter(handler, sessionFactory));
	}

	private HandshakeInfo createHandshakeInfo(URI url, Session jettySession) {
		HttpHeaders headers = new HttpHeaders();
		jettySession.getUpgradeResponse().getHeaders().forEach(headers::put);
		String protocol = headers.getFirst("Sec-WebSocket-Protocol");
		return new HandshakeInfo(url, headers, Mono.empty(), protocol);
	}


	/**
	 * Encapsulate incompatible changes between Jetty 9.4 and 10.
	 */
	private interface UpgradeHelper {

		Mono<Void> upgrade(org.eclipse.jetty.websocket.client.WebSocketClient jettyClient,
				Object jettyHandler, URI url, ClientUpgradeRequest request, HttpHeaders headers,
				Sinks.Empty<Void> completionSink);
	}


	private static class Jetty9UpgradeHelper implements UpgradeHelper {

		@Override
		public Mono<Void> upgrade(org.eclipse.jetty.websocket.client.WebSocketClient jettyClient,
				Object jettyHandler, URI url, ClientUpgradeRequest request, HttpHeaders headers,
				Sinks.Empty<Void> completionSink) {

			try {
				jettyClient.connect(jettyHandler, url, request, new DefaultUpgradeListener(headers));
				return completionSink.asMono();
			}
			catch (IOException ex) {
				return Mono.error(ex);
			}
		}
	}

	private static class DefaultUpgradeListener implements UpgradeListener {

		private final HttpHeaders headers;


		public DefaultUpgradeListener(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public void onHandshakeRequest(UpgradeRequest request) {
			this.headers.forEach(request::setHeader);
		}

		@Override
		public void onHandshakeResponse(UpgradeResponse response) {
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
		public Mono<Void> upgrade(org.eclipse.jetty.websocket.client.WebSocketClient jettyClient,
				Object jettyHandler, URI url, ClientUpgradeRequest request, HttpHeaders headers,
				Sinks.Empty<Void> completionSink) {

			// TODO: pass JettyUpgradeListener argument to set headers from HttpHeaders (like we do for Jetty 9)
			//  which would require a JDK Proxy since it is new in Jetty 10

			ReflectionUtils.invokeMethod(connectMethod, jettyClient, jettyHandler, url, request);
			return completionSink.asMono();
		}
	}

}
