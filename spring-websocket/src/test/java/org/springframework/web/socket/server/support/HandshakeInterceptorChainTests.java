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
