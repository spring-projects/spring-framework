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

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.StandardEndpointAdapter;
import org.springframework.web.socket.adapter.WebSocketHandlerAdapter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * Test fixture for {@link StandardWebSocketClient}.
 *
 * @author Rossen Stoyanchev
 */
public class StandardWebSocketClientTests {


	@Test
	public void doHandshake() throws Exception {

		URI uri = new URI("ws://example.com/abc");
		List<String> subprotocols = Arrays.asList("abc");

		HttpHeaders headers = new HttpHeaders();
		headers.setSecWebSocketProtocol(subprotocols);
		headers.add("foo", "bar");

		WebSocketHandler handler = new WebSocketHandlerAdapter();
		WebSocketContainer webSocketContainer = mock(WebSocketContainer.class);
		StandardWebSocketClient client = new StandardWebSocketClient(webSocketContainer);
		WebSocketSession session = client.doHandshake(handler, headers, uri);

		ArgumentCaptor<Endpoint> endpointArg = ArgumentCaptor.forClass(Endpoint.class);
		ArgumentCaptor<ClientEndpointConfig> configArg = ArgumentCaptor.forClass(ClientEndpointConfig.class);
		ArgumentCaptor<URI> uriArg = ArgumentCaptor.forClass(URI.class);

		verify(webSocketContainer).connectToServer(endpointArg.capture(), configArg.capture(), uriArg.capture());

		assertNotNull(endpointArg.getValue());
		assertEquals(StandardEndpointAdapter.class, endpointArg.getValue().getClass());

		ClientEndpointConfig config = configArg.getValue();
		assertEquals(subprotocols, config.getPreferredSubprotocols());

		Map<String, List<String>> map = new HashMap<>();
		config.getConfigurator().beforeRequest(map);
		assertEquals(Collections.singletonMap("foo", Arrays.asList("bar")), map);

		assertEquals(uri, uriArg.getValue());
		assertEquals(uri, session.getUri());
		assertEquals("example.com", session.getRemoteHostName());
	}

}
