/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.socket.server;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.adapter.TextWebSocketHandlerAdapter;

import static org.mockito.Mockito.*;


/**
 * Test fixture for {@link DefaultHandshakeHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultHandshakeHandlerTests extends AbstractHttpRequestTests {

	private DefaultHandshakeHandler handshakeHandler;

	@Mock
	private RequestUpgradeStrategy upgradeStrategy;


	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.handshakeHandler = new DefaultHandshakeHandler(this.upgradeStrategy);
	}


	@Test
	public void selectSubProtocol() throws Exception {

		this.handshakeHandler.setSupportedProtocols("stomp", "mqtt");

		when(this.upgradeStrategy.getSupportedVersions()).thenReturn(new String[] { "13" });

		this.servletRequest.setMethod("GET");
		this.request.getHeaders().setUpgrade("WebSocket");
		this.request.getHeaders().setConnection("Upgrade");
		this.request.getHeaders().setSecWebSocketVersion("13");
		this.request.getHeaders().setSecWebSocketKey("82/ZS2YHjEnUN97HLL8tbw==");
		this.request.getHeaders().setSecWebSocketProtocol("STOMP");

		WebSocketHandler handler = new TextWebSocketHandlerAdapter();

		this.handshakeHandler.doHandshake(this.request, this.response, handler);

		verify(this.upgradeStrategy).upgrade(request, response, "STOMP", handler);
	}

}
