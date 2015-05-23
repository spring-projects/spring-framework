/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.server.standard;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import org.xnio.StreamConnection;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ClassUtils;
import org.springframework.web.socket.server.HandshakeFailureException;

/**
 * A {@link org.springframework.web.socket.server.RequestUpgradeStrategy} for use
 * with WildFly and its underlying Undertow web server.
 *
 * <p>Compatible with Undertow 1.0, 1.1, 1.2 - as included in WildFly 8.x and 9.0.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.1
 */
public class UndertowRequestUpgradeStrategy extends AbstractStandardUpgradeStrategy {

	private static final Constructor<ServletWebSocketHttpExchange> exchangeConstructor;

	private static final Constructor<ConfiguredServerEndpoint> endpointConstructor;

	private static final boolean undertow10Present;

	private static final boolean undertow11Present;

	static {
		Class<ServletWebSocketHttpExchange> exchangeType = ServletWebSocketHttpExchange.class;
		Class<?>[] exchangeParamTypes = new Class<?>[] {HttpServletRequest.class, HttpServletResponse.class, Set.class};
		if (ClassUtils.hasConstructor(exchangeType, exchangeParamTypes)) {
			exchangeConstructor = ClassUtils.getConstructorIfAvailable(exchangeType, exchangeParamTypes);
			undertow10Present = false;
		}
		else {
			exchangeParamTypes = new Class<?>[] {HttpServletRequest.class, HttpServletResponse.class};
			exchangeConstructor = ClassUtils.getConstructorIfAvailable(exchangeType, exchangeParamTypes);
			undertow10Present = true;
		}

		Class<ConfiguredServerEndpoint> endpointType = ConfiguredServerEndpoint.class;
		Class<?>[] endpointParamTypes = new Class<?>[] {ServerEndpointConfig.class, InstanceFactory.class,
				PathTemplate.class, EncodingFactory.class, AnnotatedEndpointFactory.class};
		if (ClassUtils.hasConstructor(endpointType, endpointParamTypes)) {
			endpointConstructor = ClassUtils.getConstructorIfAvailable(endpointType, endpointParamTypes);
			undertow11Present = true;
		}
		else {
			endpointParamTypes = new Class<?>[] {ServerEndpointConfig.class, InstanceFactory.class,
					PathTemplate.class, EncodingFactory.class};
			endpointConstructor = ClassUtils.getConstructorIfAvailable(endpointType, endpointParamTypes);
			undertow11Present = false;
		}
	}

	private static final String[] supportedVersions = new String[] {
			WebSocketVersion.V13.toHttpHeaderValue(),
			WebSocketVersion.V08.toHttpHeaderValue(),
			WebSocketVersion.V07.toHttpHeaderValue()
	};


	private final Set<WebSocketChannel> peerConnections;


	public UndertowRequestUpgradeStrategy() {
		if (undertow10Present) {
			this.peerConnections = null;
		}
		else {
			this.peerConnections = Collections.newSetFromMap(new ConcurrentHashMap<WebSocketChannel, Boolean>());
		}
	}


	@Override
	public String[] getSupportedVersions() {
		return supportedVersions;
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
				WebSocketChannel channel = handshake.createChannel(exchange, connection, exchange.getBufferPool());
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
		endpointRegistration.setSubprotocols(Arrays.asList(selectedProtocol));
		endpointRegistration.setExtensions(selectedExtensions);

		EncodingFactory encodingFactory = new EncodingFactory(
				Collections.<Class<?>, List<InstanceFactory<? extends Encoder>>>emptyMap(),
				Collections.<Class<?>, List<InstanceFactory<? extends Decoder>>>emptyMap(),
				Collections.<Class<?>, List<InstanceFactory<? extends Encoder>>>emptyMap(),
				Collections.<Class<?>, List<InstanceFactory<? extends Decoder>>>emptyMap());
		try {
			return (undertow11Present ?
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
