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

package org.springframework.web.socket.client.endpoint;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.WebSocketContainer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.WebSocketHandlerAdapter;
import org.springframework.web.socket.support.WebSocketHttpHeaders;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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


	@Before
	public void setup() {
		this.headers = new WebSocketHttpHeaders();
		this.wsHandler = new WebSocketHandlerAdapter();
		this.wsContainer = mock(WebSocketContainer.class);
		this.wsClient = new StandardWebSocketClient(this.wsContainer);
	}


	@Test
	public void localAddress() throws Exception {
		URI uri = new URI("ws://example.com/abc");
		WebSocketSession session = this.wsClient.doHandshake(this.wsHandler, this.headers, uri).get();

		assertNotNull(session.getLocalAddress());
		assertEquals(80, session.getLocalAddress().getPort());
	}

	@Test
	public void localAddressWss() throws Exception {
		URI uri = new URI("wss://example.com/abc");
		WebSocketSession session = this.wsClient.doHandshake(this.wsHandler, this.headers, uri).get();

		assertNotNull(session.getLocalAddress());
		assertEquals(443, session.getLocalAddress().getPort());
	}

	@Test(expected=IllegalArgumentException.class)
	public void localAddressNoScheme() throws Exception {
		URI uri = new URI("example.com/abc");
		this.wsClient.doHandshake(this.wsHandler, this.headers, uri);
	}

	@Test
	public void remoteAddress() throws Exception {
		URI uri = new URI("wss://example.com/abc");
		WebSocketSession session = this.wsClient.doHandshake(this.wsHandler, this.headers, uri).get();

		assertNotNull(session.getRemoteAddress());
		assertEquals("example.com", session.getRemoteAddress().getHostName());
		assertEquals(443, session.getLocalAddress().getPort());
	}

	@Test
	public void headersWebSocketSession() throws Exception {

		URI uri = new URI("ws://example.com/abc");
		List<String> protocols = Arrays.asList("abc");
		this.headers.setSecWebSocketProtocol(protocols);
		this.headers.add("foo", "bar");

		WebSocketSession session = this.wsClient.doHandshake(this.wsHandler, this.headers, uri).get();

		assertEquals(Collections.singletonMap("foo", Arrays.asList("bar")), session.getHandshakeHeaders());
	}

	@Test
	public void headersClientEndpointConfigurator() throws Exception {

		URI uri = new URI("ws://example.com/abc");
		List<String> protocols = Arrays.asList("abc");
		this.headers.setSecWebSocketProtocol(protocols);
		this.headers.add("foo", "bar");

		this.wsClient.doHandshake(this.wsHandler, this.headers, uri).get();

		ArgumentCaptor<Endpoint> arg1 = ArgumentCaptor.forClass(Endpoint.class);
		ArgumentCaptor<ClientEndpointConfig> arg2 = ArgumentCaptor.forClass(ClientEndpointConfig.class);
		ArgumentCaptor<URI> arg3 = ArgumentCaptor.forClass(URI.class);
		verify(this.wsContainer).connectToServer(arg1.capture(), arg2.capture(), arg3.capture());

		ClientEndpointConfig endpointConfig = arg2.getValue();
		assertEquals(protocols, endpointConfig.getPreferredSubprotocols());

		Map<String, List<String>> map = new HashMap<>();
		endpointConfig.getConfigurator().beforeRequest(map);
		assertEquals(Collections.singletonMap("foo", Arrays.asList("bar")), map);
	}

}
