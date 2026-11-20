/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.server.support;

import java.util.Map;

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
		mapping.setUrlMap(Map.of("/path", handler));
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

	@Test // gh-34503
	void defaultHandler() throws Exception {
		HttpRequestHandler handler = new WebSocketHttpRequestHandler(mock());

		WebSocketHandlerMapping mapping = new WebSocketHandlerMapping();
		mapping.setUrlMap(Map.of("/*", handler));
		mapping.setApplicationContext(new StaticWebApplicationContext());

		assertThat(mapping.getDefaultHandler()).isNull();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path");
		HandlerExecutionChain chain = mapping.getHandler(request);

		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isSameAs(handler);
	}

}
