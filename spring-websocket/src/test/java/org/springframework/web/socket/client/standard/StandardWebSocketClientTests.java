/*
 * Copyright 2002-2019 the original author or authors.
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

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.Endpoint;
import jakarta.websocket.WebSocketContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link StandardWebSocketClient}.
 *
 * @author Rossen Stoyanchev
 */
public class StandardWebSocketClientTests {

	private StandardWebSocketClient wsClient;

	private WebSocketContainer wsContainer;

	private WebSocketHandler wsHandler;

	private WebSocketHttpHeaders headers;


	@BeforeEach
	public void setup() {
		this.headers = new WebSocketHttpHeaders();
		this.wsHandler = new AbstractWebSocketHandler() {
		};
		this.wsContainer = mock(WebSocketContainer.class);
		this.wsClient = new StandardWebSocketClient(this.wsContainer);
	}


	@Test
	public void testGetLocalAddress() throws Exception {
		URI uri = new URI("ws://localhost/abc");
		WebSocketSession session = this.wsClient.doHandshake(this.wsHandler, this.headers, uri).get();

		assertThat(session.getLocalAddress()).isNotNull();
		assertThat(session.getLocalAddress().getPort()).isEqualTo(80);
	}

	@Test
	public void testGetLocalAddressWss() throws Exception {
		URI uri = new URI("wss://localhost/abc");
		WebSocketSession session = this.wsClient.doHandshake(this.wsHandler, this.headers, uri).get();

		assertThat(session.getLocalAddress()).isNotNull();
		assertThat(session.getLocalAddress().getPort()).isEqualTo(443);
	}

	@Test
	public void testGetLocalAddressNoScheme() throws Exception {
		URI uri = new URI("localhost/abc");
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.wsClient.doHandshake(this.wsHandler, this.headers, uri));
	}

	@Test
	public void testGetRemoteAddress() throws Exception {
		URI uri = new URI("wss://localhost/abc");
		WebSocketSession session = this.wsClient.doHandshake(this.wsHandler, this.headers, uri).get();

		assertThat(session.getRemoteAddress()).isNotNull();
		assertThat(session.getRemoteAddress().getHostName()).isEqualTo("localhost");
		assertThat(session.getLocalAddress().getPort()).isEqualTo(443);
	}

	@Test
	public void handshakeHeaders() throws Exception {

		URI uri = new URI("ws://localhost/abc");
		List<String> protocols = Collections.singletonList("abc");
		this.headers.setSecWebSocketProtocol(protocols);
		this.headers.add("foo", "bar");

		WebSocketSession session = this.wsClient.doHandshake(this.wsHandler, this.headers, uri).get();

		assertThat(session.getHandshakeHeaders().size()).isEqualTo(1);
		assertThat(session.getHandshakeHeaders().getFirst("foo")).isEqualTo("bar");
	}

	@Test
	public void clientEndpointConfig() throws Exception {

		URI uri = new URI("ws://localhost/abc");
		List<String> protocols = Collections.singletonList("abc");
		this.headers.setSecWebSocketProtocol(protocols);

		this.wsClient.doHandshake(this.wsHandler, this.headers, uri).get();

		ArgumentCaptor<ClientEndpointConfig> captor = ArgumentCaptor.forClass(ClientEndpointConfig.class);
		verify(this.wsContainer).connectToServer(any(Endpoint.class), captor.capture(), any(URI.class));
		ClientEndpointConfig endpointConfig = captor.getValue();

		assertThat(endpointConfig.getPreferredSubprotocols()).isEqualTo(protocols);
	}

	@Test
	public void clientEndpointConfigWithUserProperties() throws Exception {

		Map<String,Object> userProperties = Collections.singletonMap("foo", "bar");

		URI uri = new URI("ws://localhost/abc");
		this.wsClient.setUserProperties(userProperties);
		this.wsClient.doHandshake(this.wsHandler, this.headers, uri).get();

		ArgumentCaptor<ClientEndpointConfig> captor = ArgumentCaptor.forClass(ClientEndpointConfig.class);
		verify(this.wsContainer).connectToServer(any(Endpoint.class), captor.capture(), any(URI.class));
		ClientEndpointConfig endpointConfig = captor.getValue();

		assertThat(endpointConfig.getUserProperties()).isEqualTo(userProperties);
	}

	@Test
	public void standardWebSocketClientConfiguratorInsertsHandshakeHeaders() throws Exception {

		URI uri = new URI("ws://localhost/abc");
		this.headers.add("foo", "bar");

		this.wsClient.doHandshake(this.wsHandler, this.headers, uri).get();

		ArgumentCaptor<ClientEndpointConfig> captor = ArgumentCaptor.forClass(ClientEndpointConfig.class);
		verify(this.wsContainer).connectToServer(any(Endpoint.class), captor.capture(), any(URI.class));
		ClientEndpointConfig endpointConfig = captor.getValue();

		Map<String, List<String>> headers = new HashMap<>();
		endpointConfig.getConfigurator().beforeRequest(headers);
		assertThat(headers.size()).isEqualTo(1);
	}

	@Test
	public void taskExecutor() throws Exception {

		URI uri = new URI("ws://localhost/abc");
		this.wsClient.setTaskExecutor(new SimpleAsyncTaskExecutor());
		WebSocketSession session = this.wsClient.doHandshake(this.wsHandler, this.headers, uri).get();

		assertThat(session).isNotNull();
	}

}
