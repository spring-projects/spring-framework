/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.socket.server.standard;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.HttpUpgradeListener;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.websockets.ServletWebSocketHttpExchange;
import io.undertow.util.PathTemplate;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSocketVersion;
import io.undertow.websockets.core.protocol.Handshake;
import io.undertow.websockets.jsr.ConfiguredServerEndpoint;
import io.undertow.websockets.jsr.EncodingFactory;
import io.undertow.websockets.jsr.EndpointSessionHandler;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.jsr.annotated.AnnotatedEndpointFactory;
import io.undertow.websockets.jsr.handshake.HandshakeUtil;
import io.undertow.websockets.jsr.handshake.JsrHybi07Handshake;
import io.undertow.websockets.jsr.handshake.JsrHybi08Handshake;
import io.undertow.websockets.jsr.handshake.JsrHybi13Handshake;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.xnio.StreamConnection;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.socket.server.HandshakeFailureException;

/**
 * A WebSocket {@code RequestUpgradeStrategy} for WildFly and its underlying
 * Undertow web server. Also compatible with embedded Undertow usage.
 *
 * <p>Designed for Undertow 1.3.5+ as of Spring Framework 4.3, with a fallback
 * strategy for Undertow 1.0 to 1.3 - as included in WildFly 8.x, 9 and 10.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.1
 */
public class UndertowRequestUpgradeStrategy extends AbstractStandardUpgradeStrategy {

	private static final boolean HAS_DO_UPGRADE = ClassUtils.hasMethod(ServerWebSocketContainer.class, "doUpgrade",
			HttpServletRequest.class, HttpServletResponse.class, ServerEndpointConfig.class, Map.class);

	private static final FallbackStrategy FALLBACK_STRATEGY = (HAS_DO_UPGRADE ? null : new FallbackStrategy());

	private static final String[] VERSIONS = new String[] {
			WebSocketVersion.V13.toHttpHeaderValue(),
			WebSocketVersion.V08.toHttpHeaderValue(),
			WebSocketVersion.V07.toHttpHeaderValue()
	};


	@Override
	public String[] getSupportedVersions() {
		return VERSIONS;
	}

	@Override
	protected void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
			String selectedProtocol, List<Extension> selectedExtensions, Endpoint endpoint)
			throws HandshakeFailureException {

		if (HAS_DO_UPGRADE) {
			HttpServletRequest servletRequest = getHttpServletRequest(request);
			HttpServletResponse servletResponse = getHttpServletResponse(response);

			StringBuffer requestUrl = servletRequest.getRequestURL();
			String path = servletRequest.getRequestURI();  // shouldn't matter
			Map<String, String> pathParams = Collections.<String, String>emptyMap();

			ServerEndpointRegistration endpointConfig = new ServerEndpointRegistration(path, endpoint);
			endpointConfig.setSubprotocols(Collections.singletonList(selectedProtocol));
			endpointConfig.setExtensions(selectedExtensions);

			try {
				getContainer(servletRequest).doUpgrade(servletRequest, servletResponse, endpointConfig, pathParams);
			}
			catch (ServletException ex) {
				throw new HandshakeFailureException(
						"Servlet request failed to upgrade to WebSocket: " + requestUrl, ex);
			}
			catch (IOException ex) {
				throw new HandshakeFailureException(
						"Response update failed during upgrade to WebSocket: " + requestUrl, ex);
			}
		}
		else {
			FALLBACK_STRATEGY.upgradeInternal(request, response, selectedProtocol, selectedExtensions, endpoint);
		}
	}

	@Override
	public ServerWebSocketContainer getContainer(HttpServletRequest request) {
		return (ServerWebSocketContainer) super.getContainer(request);
	}


	/**
	 * Strategy for use with Undertow 1.0 to 1.3 before there was a public API
	 * to perform a WebSocket upgrade.
	 */
	private static class FallbackStrategy extends AbstractStandardUpgradeStrategy {

		private static final Constructor<ServletWebSocketHttpExchange> exchangeConstructor;

		private static final boolean exchangeConstructorWithPeerConnections;

		private static final Constructor<ConfiguredServerEndpoint> endpointConstructor;

		private static final boolean endpointConstructorWithEndpointFactory;

		private static final Method getBufferPoolMethod;

		private static final Method createChannelMethod;

		static {
			try {
				Class<ServletWebSocketHttpExchange> exchangeType = ServletWebSocketHttpExchange.class;
				Class<?>[] exchangeParamTypes =
						new Class<?>[] {HttpServletRequest.class, HttpServletResponse.class, Set.class};
				Constructor<ServletWebSocketHttpExchange> exchangeCtor =
						ClassUtils.getConstructorIfAvailable(exchangeType, exchangeParamTypes);
				if (exchangeCtor != null) {
					// Undertow 1.1+
					exchangeConstructor = exchangeCtor;
					exchangeConstructorWithPeerConnections = true;
				}
				else {
					// Undertow 1.0
					exchangeParamTypes = new Class<?>[] {HttpServletRequest.class, HttpServletResponse.class};
					exchangeConstructor = exchangeType.getConstructor(exchangeParamTypes);
					exchangeConstructorWithPeerConnections = false;
				}

				Class<ConfiguredServerEndpoint> endpointType = ConfiguredServerEndpoint.class;
				Class<?>[] endpointParamTypes = new Class<?>[] {ServerEndpointConfig.class, InstanceFactory.class,
						PathTemplate.class, EncodingFactory.class, AnnotatedEndpointFactory.class};
				Constructor<ConfiguredServerEndpoint> endpointCtor =
						ClassUtils.getConstructorIfAvailable(endpointType, endpointParamTypes);
				if (endpointCtor != null) {
					// Undertow 1.1+
					endpointConstructor = endpointCtor;
					endpointConstructorWithEndpointFactory = true;
				}
				else {
					// Undertow 1.0
					endpointParamTypes = new Class<?>[] {ServerEndpointConfig.class, InstanceFactory.class,
							PathTemplate.class, EncodingFactory.class};
					endpointConstructor = endpointType.getConstructor(endpointParamTypes);
					endpointConstructorWithEndpointFactory = false;
				}

				// Adapting between different Pool API types in Undertow 1.0-1.2 vs 1.3
				getBufferPoolMethod = WebSocketHttpExchange.class.getMethod("getBufferPool");
				createChannelMethod = ReflectionUtils.findMethod(Handshake.class, "createChannel", (Class<?>[]) null);
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Incompatible Undertow API version", ex);
			}
		}

		private final Set<WebSocketChannel> peerConnections;

		public FallbackStrategy() {
			if (exchangeConstructorWithPeerConnections) {
				this.peerConnections = Collections.newSetFromMap(new ConcurrentHashMap<WebSocketChannel, Boolean>());
			}
			else {
				this.peerConnections = null;
			}
		}

		@Override
		public String[] getSupportedVersions() {
			return VERSIONS;
		}

		@Override
		protected void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response,
				String selectedProtocol, List<Extension> selectedExtensions, final Endpoint endpoint)
				throws HandshakeFailureException {

			HttpServletRequest servletRequest = getHttpServletRequest(request);
			HttpServletResponse servletResponse = getHttpServletResponse(response);

			final ServletWebSocketHttpExchange exchange = createHttpExchange(servletRequest, servletResponse);
			exchange.putAttachment(HandshakeUtil.PATH_PARAMS, Collections.<String, String>emptyMap());

			ServerWebSocketContainer wsContainer = (ServerWebSocketContainer) getContainer(servletRequest);
			final EndpointSessionHandler endpointSessionHandler = new EndpointSessionHandler(wsContainer);

			final ConfiguredServerEndpoint configuredServerEndpoint = createConfiguredServerEndpoint(
					selectedProtocol, selectedExtensions, endpoint, servletRequest);

			final Handshake handshake = getHandshakeToUse(exchange, configuredServerEndpoint);

			exchange.upgradeChannel(new HttpUpgradeListener() {
				@Override
				public void handleUpgrade(StreamConnection connection, HttpServerExchange serverExchange) {
					Object bufferPool = ReflectionUtils.invokeMethod(getBufferPoolMethod, exchange);
					WebSocketChannel channel = (WebSocketChannel) ReflectionUtils.invokeMethod(
							createChannelMethod, handshake, exchange, connection, bufferPool);
					if (peerConnections != null) {
						peerConnections.add(channel);
					}
					endpointSessionHandler.onConnect(exchange, channel);
				}
			});

			handshake.handshake(exchange);
		}

		private ServletWebSocketHttpExchange createHttpExchange(HttpServletRequest request, HttpServletResponse response) {
			try {
				return (this.peerConnections != null ?
						exchangeConstructor.newInstance(request, response, this.peerConnections) :
						exchangeConstructor.newInstance(request, response));
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to instantiate ServletWebSocketHttpExchange", ex);
			}
		}

		private Handshake getHandshakeToUse(ServletWebSocketHttpExchange exchange, ConfiguredServerEndpoint endpoint) {
			Handshake handshake = new JsrHybi13Handshake(endpoint);
			if (handshake.matches(exchange)) {
				return handshake;
			}
			handshake = new JsrHybi08Handshake(endpoint);
			if (handshake.matches(exchange)) {
				return handshake;
			}
			handshake = new JsrHybi07Handshake(endpoint);
			if (handshake.matches(exchange)) {
				return handshake;
			}
			// Should never occur
			throw new HandshakeFailureException("No matching Undertow Handshake found: " + exchange.getRequestHeaders());
		}

		private ConfiguredServerEndpoint createConfiguredServerEndpoint(String selectedProtocol,
				List<Extension> selectedExtensions, Endpoint endpoint, HttpServletRequest servletRequest) {

			String path = servletRequest.getRequestURI();  // shouldn't matter
			ServerEndpointRegistration endpointRegistration = new ServerEndpointRegistration(path, endpoint);
			endpointRegistration.setSubprotocols(Collections.singletonList(selectedProtocol));
			endpointRegistration.setExtensions(selectedExtensions);

			EncodingFactory encodingFactory = new EncodingFactory(
					Collections.<Class<?>, List<InstanceFactory<? extends Encoder>>>emptyMap(),
					Collections.<Class<?>, List<InstanceFactory<? extends Decoder>>>emptyMap(),
					Collections.<Class<?>, List<InstanceFactory<? extends Encoder>>>emptyMap(),
					Collections.<Class<?>, List<InstanceFactory<? extends Decoder>>>emptyMap());
			try {
				return (endpointConstructorWithEndpointFactory ?
						endpointConstructor.newInstance(endpointRegistration,
								new EndpointInstanceFactory(endpoint), null, encodingFactory, null) :
						endpointConstructor.newInstance(endpointRegistration,
								new EndpointInstanceFactory(endpoint), null, encodingFactory));
			}
			catch (Exception ex) {
				throw new HandshakeFailureException("Failed to instantiate ConfiguredServerEndpoint", ex);
			}
		}


		private static class EndpointInstanceFactory implements InstanceFactory<Endpoint> {

			private final Endpoint endpoint;

			public EndpointInstanceFactory(Endpoint endpoint) {
				this.endpoint = endpoint;
			}

			@Override
			public InstanceHandle<Endpoint> createInstance() throws InstantiationException {
				return new InstanceHandle<Endpoint>() {
					@Override
					public Endpoint getInstance() {
						return endpoint;
					}
					@Override
					public void release() {
					}
				};
			}
		}
	}

}
