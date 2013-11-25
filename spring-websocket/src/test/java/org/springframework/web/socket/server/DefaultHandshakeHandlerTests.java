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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.support.SubProtocolCapable;
import org.springframework.web.socket.support.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.adapter.TextWebSocketHandlerAdapter;
import org.springframework.web.socket.support.WebSocketHttpHeaders;

import static org.junit.Assert.assertEquals;
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
	public void supportedSubProtocols() throws Exception {

		this.handshakeHandler.setSupportedProtocols("stomp", "mqtt");

		when(this.upgradeStrategy.getSupportedVersions()).thenReturn(new String[] { "13" });

		this.servletRequest.setMethod("GET");

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders(this.request.getHeaders());
		headers.setUpgrade("WebSocket");
		headers.setConnection("Upgrade");
		headers.setSecWebSocketVersion("13");
		headers.setSecWebSocketKey("82/ZS2YHjEnUN97HLL8tbw==");
		headers.setSecWebSocketProtocol("STOMP");

		WebSocketHandler handler = new TextWebSocketHandlerAdapter();
		Map<String, Object> attributes = Collections.<String, Object>emptyMap();
		this.handshakeHandler.doHandshake(this.request, this.response, handler, attributes);

		verify(this.upgradeStrategy).upgrade(this.request, this.response,
				"STOMP", Collections.<WebSocketExtension>emptyList(), handler, attributes);
	}

	@Test
	public void subProtocolCapableHandler() throws Exception {

		when(this.upgradeStrategy.getSupportedVersions()).thenReturn(new String[]{"13"});

		this.servletRequest.setMethod("GET");

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders(this.request.getHeaders());
		headers.setUpgrade("WebSocket");
		headers.setConnection("Upgrade");
		headers.setSecWebSocketVersion("13");
		headers.setSecWebSocketKey("82/ZS2YHjEnUN97HLL8tbw==");
		headers.setSecWebSocketProtocol("v11.stomp");

		WebSocketHandler handler = new SubProtocolCapableHandler("v12.stomp", "v11.stomp");
		Map<String, Object> attributes = Collections.<String, Object>emptyMap();
		this.handshakeHandler.doHandshake(this.request, this.response, handler, attributes);

		verify(this.upgradeStrategy).upgrade(this.request, this.response,
				"v11.stomp", Collections.<WebSocketExtension>emptyList(), handler, attributes);
	}

	@Test
	public void subProtocolCapableHandlerNoMatch() throws Exception {

		when(this.upgradeStrategy.getSupportedVersions()).thenReturn(new String[]{"13"});

		this.servletRequest.setMethod("GET");

		WebSocketHttpHeaders headers = new WebSocketHttpHeaders(this.request.getHeaders());
		headers.setUpgrade("WebSocket");
		headers.setConnection("Upgrade");
		headers.setSecWebSocketVersion("13");
		headers.setSecWebSocketKey("82/ZS2YHjEnUN97HLL8tbw==");
		headers.setSecWebSocketProtocol("v10.stomp");

		WebSocketHandler handler = new SubProtocolCapableHandler("v12.stomp", "v11.stomp");
		Map<String, Object> attributes = Collections.<String, Object>emptyMap();
		this.handshakeHandler.doHandshake(this.request, this.response, handler, attributes);

		verify(this.upgradeStrategy).upgrade(this.request, this.response,
				null, Collections.<WebSocketExtension>emptyList(), handler, attributes);
	}


	private static class SubProtocolCapableHandler extends TextWebSocketHandlerAdapter implements SubProtocolCapable {

		private final List<String> subProtocols;


		private SubProtocolCapableHandler(String... subProtocols) {
			this.subProtocols = Arrays.asList(subProtocols);
		}

		@Override
		public List<String> getSubProtocols() {
			return this.subProtocols;
		}
	}

}
