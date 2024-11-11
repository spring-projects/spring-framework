/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.socket.server.support;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebSocketHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 */
class WebSocketHandlerMappingTests {

	@Test
	void webSocketHandshakeMatch() throws Exception {
		HttpRequestHandler handler = new WebSocketHttpRequestHandler(mock());

		WebSocketHandlerMapping mapping = new WebSocketHandlerMapping();
		mapping.setUrlMap(Collections.singletonMap("/path", handler));
		mapping.setApplicationContext(new StaticWebApplicationContext());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isSameAs(handler);

		mapping.setWebSocketUpgradeMatch(true);

		chain = mapping.getHandler(request);
		assertThat(chain).isNull();

		request.addHeader("Upgrade", "websocket");

		chain = mapping.getHandler(request);
		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isSameAs(handler);

		request.setMethod("POST");

		chain = mapping.getHandler(request);
		assertThat(chain).isNull();
	}

}
