/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.server.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link HandshakeInterceptorChain}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class HandshakeInterceptorChainTests extends AbstractHttpRequestTests {

	private Map<String, Object> attributes = new HashMap<>();

	private HandshakeInterceptor i1 = mock();
	private HandshakeInterceptor i2 = mock();
	private HandshakeInterceptor i3 = mock();

	private WebSocketHandler wsHandler = mock();

	private HandshakeInterceptorChain chain = new HandshakeInterceptorChain(List.of(i1, i2, i3), wsHandler);


	@Test
	void success() throws Exception {
		given(i1.beforeHandshake(request, response, wsHandler, attributes)).willReturn(true);
		given(i2.beforeHandshake(request, response, wsHandler, attributes)).willReturn(true);
		given(i3.beforeHandshake(request, response, wsHandler, attributes)).willReturn(true);

		chain.applyBeforeHandshake(request, response, attributes);

		verify(i1).beforeHandshake(request, response, wsHandler, attributes);
		verify(i2).beforeHandshake(request, response, wsHandler, attributes);
		verify(i3).beforeHandshake(request, response, wsHandler, attributes);
		verifyNoMoreInteractions(i1, i2, i3);
	}

	@Test
	void applyBeforeHandshakeWithFalseReturnValue() throws Exception {
		given(i1.beforeHandshake(request, response, wsHandler, attributes)).willReturn(true);
		given(i2.beforeHandshake(request, response, wsHandler, attributes)).willReturn(false);

		chain.applyBeforeHandshake(request, response, attributes);

		verify(i1).beforeHandshake(request, response, wsHandler, attributes);
		verify(i1).afterHandshake(request, response, wsHandler, null);
		verify(i2).beforeHandshake(request, response, wsHandler, attributes);
		verifyNoMoreInteractions(i1, i2, i3);
	}

	@Test
	void applyAfterHandshakeOnly() {
		chain.applyAfterHandshake(request, response, null);

		verifyNoMoreInteractions(i1, i2, i3);
	}

}
