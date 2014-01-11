/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.socket.server.standard;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.HttpUpgradeListener;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.websockets.ServletWebSocketHttpExchange;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.protocol.Handshake;
import io.undertow.websockets.core.protocol.version07.Hybi07Handshake;
import io.undertow.websockets.core.protocol.version08.Hybi08Handshake;
import io.undertow.websockets.core.protocol.version13.Hybi13Handshake;
import io.undertow.websockets.jsr.ConfiguredServerEndpoint;
import io.undertow.websockets.jsr.EncodingFactory;
import io.undertow.websockets.jsr.EndpointSessionHandler;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.jsr.handshake.HandshakeUtil;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.xnio.StreamConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import java.util.*;


/**
 * A {@link org.springframework.web.socket.server.RequestUpgradeStrategy} for use
 * with WildFly and its underlying Undertow web server.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.1
 */
public class UndertowRequestUpgradeStrategy extends AbstractStandardUpgradeStrategy {

	private final Handshake[] handshakes;

	private final String[] supportedVersions;


	public UndertowRequestUpgradeStrategy() {
		this.handshakes = new Handshake[] { new Hybi13Handshake(), new Hybi08Handshake(), new Hybi07Handshake() };
		this.supportedVersions = initSupportedVersions(this.handshakes);
	}

	private String[] initSupportedVersions(Handshake[] handshakes) {
		String[] versions = new String[handshakes.length];
		for (int i=0; i < versions.length; i++) {
			versions[i] = handshakes[i].getVersion().toHttpHeaderValue();
		}
		return versions;
	}

	@Override
	public String[] getSupportedVersions() {
		return this.supportedVersions;
	}

	@Override
	protected void upgradeInternal(ServerHttpRequest request, ServerHttpResponse response, String selectedProtocol,
			List<Extension> selectedExtensions, final Endpoint endpoint) throws HandshakeFailureException {

		HttpServletRequest servletRequest = getHttpServletRequest(request);
		HttpServletResponse servletResponse = getHttpServletResponse(response);

		final ServletWebSocketHttpExchange exchange = new ServletWebSocketHttpExchange(servletRequest, servletResponse);
		exchange.putAttachment(HandshakeUtil.PATH_PARAMS, Collections.<String, String>emptyMap());

		ServerWebSocketContainer wsContainer = (ServerWebSocketContainer) getContainer(servletRequest);
		final EndpointSessionHandler endpointSessionHandler = new EndpointSessionHandler(wsContainer);

		final Handshake handshake = getHandshakeToUse(exchange);

		final ConfiguredServerEndpoint configuredServerEndpoint = createConfiguredServerEndpoint(
				selectedProtocol, selectedExtensions, endpoint, servletRequest);

		exchange.upgradeChannel(new HttpUpgradeListener() {
			@Override
			public void handleUpgrade(StreamConnection connection, HttpServerExchange serverExchange) {
				WebSocketChannel channel = handshake.createChannel(exchange, connection, exchange.getBufferPool());
				HandshakeUtil.setConfig(channel, configuredServerEndpoint);
				endpointSessionHandler.onConnect(exchange, channel);
			}
		});

		handshake.handshake(exchange);
	}

	private Handshake getHandshakeToUse(ServletWebSocketHttpExchange exchange) {
		for (Handshake handshake : this.handshakes) {
			if (handshake.matches(exchange)) {
				return handshake;
			}
		}
		// Should never occur
		throw new HandshakeFailureException("No matching Undertow Handshake found: " + exchange.getRequestHeaders());
	}

	private ConfiguredServerEndpoint createConfiguredServerEndpoint(String selectedProtocol,
			List<Extension> selectedExtensions, Endpoint endpoint, HttpServletRequest servletRequest) {

		String path = servletRequest.getRequestURI(); // shouldn't matter
		ServerEndpointRegistration endpointRegistration = new ServerEndpointRegistration(path, endpoint);
		endpointRegistration.setSubprotocols(Arrays.asList(selectedProtocol));
		endpointRegistration.setExtensions(selectedExtensions);

		return new ConfiguredServerEndpoint(endpointRegistration,
				new EndpointInstanceFactory(endpoint), null,
				new EncodingFactory(
						Collections.<Class<?>, List<InstanceFactory<? extends Encoder>>>emptyMap(),
						Collections.<Class<?>, List<InstanceFactory<? extends Decoder>>>emptyMap(),
						Collections.<Class<?>, List<InstanceFactory<? extends Encoder>>>emptyMap(),
						Collections.<Class<?>, List<InstanceFactory<? extends Decoder>>>emptyMap()));
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
