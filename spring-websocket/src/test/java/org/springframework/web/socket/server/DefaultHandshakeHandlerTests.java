/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.socket.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link org.springframework.web.socket.server.support.DefaultHandshakeHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultHandshakeHandlerTests extends AbstractHttpRequestTests {

	private RequestUpgradeStrategy upgradeStrategy = mock();

	private DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler(this.upgradeStrategy);


	@Test
	public void supportedSubProtocols() {
		this.handshakeHandler.setSupportedProtocols("stomp", "mqtt");
		given(this.upgradeStrategy.getSupportedVersions()).willReturn(new String[] {"13"});

		this.servletRequest.setMethod("GET");
		initHeaders(this.request.getHeaders()).setSecWebSocketProtocol("STOMP");

		WebSocketHandler handler = new TextWebSocketHandler();
		Map<String, Object> attributes = Collections.emptyMap();
		this.handshakeHandler.doHandshake(this.request, this.response, handler, attributes);

		verify(this.upgradeStrategy).upgrade(this.request, this.response, "STOMP",
				Collections.emptyList(), null, handler, attributes);
	}

	@Test
	public void supportedExtensions() {
		WebSocketExtension extension1 = new WebSocketExtension("ext1");
		WebSocketExtension extension2 = new WebSocketExtension("ext2");

		given(this.upgradeStrategy.getSupportedVersions()).willReturn(new String[] {"13"});
		given(this.upgradeStrategy.getSupportedExtensions(this.request)).willReturn(Collections.singletonList(extension1));

		this.servletRequest.setMethod("GET");
		initHeaders(this.request.getHeaders()).setSecWebSocketExtensions(Arrays.asList(extension1, extension2));

		WebSocketHandler handler = new TextWebSocketHandler();
		Map<String, Object> attributes = Collections.emptyMap();
		this.handshakeHandler.doHandshake(this.request, this.response, handler, attributes);

		verify(this.upgradeStrategy).upgrade(this.request, this.response, null,
				Collections.singletonList(extension1), null, handler, attributes);
	}

	@Test
	public void subProtocolCapableHandler() {
		given(this.upgradeStrategy.getSupportedVersions()).willReturn(new String[] {"13"});

		this.servletRequest.setMethod("GET");
		initHeaders(this.request.getHeaders()).setSecWebSocketProtocol("v11.stomp");

		WebSocketHandler handler = new SubProtocolCapableHandler("v12.stomp", "v11.stomp");
		Map<String, Object> attributes = Collections.emptyMap();
		this.handshakeHandler.doHandshake(this.request, this.response, handler, attributes);

		verify(this.upgradeStrategy).upgrade(this.request, this.response, "v11.stomp",
				Collections.emptyList(), null, handler, attributes);
	}

	@Test
	public void subProtocolCapableHandlerNoMatch() {
		given(this.upgradeStrategy.getSupportedVersions()).willReturn(new String[] {"13"});

		this.servletRequest.setMethod("GET");
		initHeaders(this.request.getHeaders()).setSecWebSocketProtocol("v10.stomp");

		WebSocketHandler handler = new SubProtocolCapableHandler("v12.stomp", "v11.stomp");
		Map<String, Object> attributes = Collections.emptyMap();
		this.handshakeHandler.doHandshake(this.request, this.response, handler, attributes);

		verify(this.upgradeStrategy).upgrade(this.request, this.response, null,
				Collections.emptyList(), null, handler, attributes);
	}

	private WebSocketHttpHeaders initHeaders(HttpHeaders httpHeaders) {
		WebSocketHttpHeaders headers = new WebSocketHttpHeaders(httpHeaders);
		headers.setUpgrade("WebSocket");
		headers.setConnection("Upgrade");
		headers.setSecWebSocketVersion("13");
		headers.setSecWebSocketKey("82/ZS2YHjEnUN97HLL8tbw==");
		return headers;
	}


	private static class SubProtocolCapableHandler extends TextWebSocketHandler implements SubProtocolCapable {

		private final List<String> subProtocols;

		public SubProtocolCapableHandler(String... subProtocols) {
			this.subProtocols = Arrays.asList(subProtocols);
		}

		@Override
		public List<String> getSubProtocols() {
			return this.subProtocols;
		}
	}

}
