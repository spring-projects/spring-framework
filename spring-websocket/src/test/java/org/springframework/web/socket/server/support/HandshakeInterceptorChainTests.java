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

package org.springframework.web.socket.server.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import static org.mockito.BDDMockito.*;

/**
 * Test fixture for {@link HandshakeInterceptorChain}.
 *
 * @author Rossen Stoyanchev
 */
public class HandshakeInterceptorChainTests extends AbstractHttpRequestTests {

	private HandshakeInterceptor i1;

	private HandshakeInterceptor i2;

	private HandshakeInterceptor i3;

	private List<HandshakeInterceptor> interceptors;

	private WebSocketHandler wsHandler;

	private Map<String, Object> attributes;


	@Before
	public void setup() {
		i1 = mock(HandshakeInterceptor.class);
		i2 = mock(HandshakeInterceptor.class);
		i3 = mock(HandshakeInterceptor.class);
		interceptors = Arrays.asList(i1, i2, i3);
		wsHandler = mock(WebSocketHandler.class);
		attributes = new HashMap<String, Object>();
	}


	@Test
	public void success() throws Exception {
		given(i1.beforeHandshake(request, response, wsHandler, attributes)).willReturn(true);
		given(i2.beforeHandshake(request, response, wsHandler, attributes)).willReturn(true);
		given(i3.beforeHandshake(request, response, wsHandler, attributes)).willReturn(true);

		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(interceptors, wsHandler);
		chain.applyBeforeHandshake(request, response, attributes);

		verify(i1).beforeHandshake(request, response, wsHandler, attributes);
		verify(i2).beforeHandshake(request, response, wsHandler, attributes);
		verify(i3).beforeHandshake(request, response, wsHandler, attributes);
		verifyNoMoreInteractions(i1, i2, i3);
	}

	@Test
	public void applyBeforeHandshakeWithFalseReturnValue() throws Exception {
		given(i1.beforeHandshake(request, response, wsHandler, attributes)).willReturn(true);
		given(i2.beforeHandshake(request, response, wsHandler, attributes)).willReturn(false);

		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(interceptors, wsHandler);
		chain.applyBeforeHandshake(request, response, attributes);

		verify(i1).beforeHandshake(request, response, wsHandler, attributes);
		verify(i1).afterHandshake(request, response, wsHandler, null);
		verify(i2).beforeHandshake(request, response, wsHandler, attributes);
		verifyNoMoreInteractions(i1, i2, i3);
	}

	@Test
	public void applyAfterHandshakeOnly() {
		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(interceptors, wsHandler);
		chain.applyAfterHandshake(request, response, null);

		verifyNoMoreInteractions(i1, i2, i3);
	}

}
