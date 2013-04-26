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

package org.springframework.websocket.client.endpoint;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.ClientEndpointConfig.Configurator;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.websocket.HandlerProvider;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketSession;
import org.springframework.websocket.adapter.StandardWebSocketSessionAdapter;
import org.springframework.websocket.adapter.StandardEndpointAdapter;
import org.springframework.websocket.client.WebSocketClient;
import org.springframework.websocket.client.WebSocketConnectFailureException;
import org.springframework.websocket.support.SimpleHandlerProvider;

/**
 * A standard Java {@link WebSocketClient}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StandardWebSocketClient implements WebSocketClient {

	private static final Set<String> EXCLUDED_HEADERS = new HashSet<String>(
			Arrays.asList("Sec-WebSocket-Accept", "Sec-WebSocket-Extensions", "Sec-WebSocket-Key",
					"Sec-WebSocket-Protocol", "Sec-WebSocket-Version"));

	private WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();


	public void setWebSocketContainer(WebSocketContainer container) {
		this.webSocketContainer = container;
	}

	@Override
	public WebSocketSession doHandshake(WebSocketHandler handler, String uriTemplate, Object... uriVariables)
			throws WebSocketConnectFailureException {

		return doHandshake(new SimpleHandlerProvider<WebSocketHandler<?>>(handler), uriTemplate, uriVariables);
	}

	public WebSocketSession doHandshake(HandlerProvider<WebSocketHandler<?>> handler,
			String uriTemplate, Object... uriVariables) throws WebSocketConnectFailureException {

		URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVariables).encode().toUri();
		return doHandshake(handler, null, uri);
	}

	@Override
	public WebSocketSession doHandshake(HandlerProvider<WebSocketHandler<?>> handler,
			final HttpHeaders httpHeaders, URI uri) throws WebSocketConnectFailureException {

		Endpoint endpoint = new StandardEndpointAdapter(handler);

		ClientEndpointConfig.Builder configBuidler = ClientEndpointConfig.Builder.create();
		if (httpHeaders != null) {
			List<String> protocols = httpHeaders.getSecWebSocketProtocol();
			if (!protocols.isEmpty()) {
				configBuidler.preferredSubprotocols(protocols);
			}
			configBuidler.configurator(new Configurator() {
				@Override
				public void beforeRequest(Map<String, List<String>> headers) {
					for (String headerName : httpHeaders.keySet()) {
						if (!EXCLUDED_HEADERS.contains(headerName)) {
							headers.put(headerName, httpHeaders.get(headerName));
						}
					}
				}
			});
		}

		try {
			Session session = this.webSocketContainer.connectToServer(endpoint, configBuidler.build(), uri);
			return new StandardWebSocketSessionAdapter(session);
		}
		catch (Exception e) {
			throw new WebSocketConnectFailureException("Failed to connect to " + uri, e);
		}
	}

}
