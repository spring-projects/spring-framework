/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WebSocketHttpRequestHandler}.
 * @author Rossen Stoyanchev
 * @since 5.1.9
 */
public class WebSocketHttpRequestHandlerTests {

	private HandshakeHandler handshakeHandler;

	private WebSocketHttpRequestHandler requestHandler;

	private MockHttpServletResponse response;


	@Before
	public void setUp() {
		this.handshakeHandler = mock(HandshakeHandler.class);
		this.requestHandler = new WebSocketHttpRequestHandler(mock(WebSocketHandler.class), this.handshakeHandler);
		this.response = new MockHttpServletResponse();
	}


	@Test
	public void success() throws ServletException, IOException {
		TestInterceptor interceptor = new TestInterceptor(true);
		this.requestHandler.setHandshakeInterceptors(Collections.singletonList(interceptor));
		this.requestHandler.handleRequest(new MockHttpServletRequest(), this.response);

		verify(this.handshakeHandler).doHandshake(any(), any(), any(), any());
		assertEquals("headerValue", this.response.getHeader("headerName"));
	}

	@Test
	public void failure() throws ServletException, IOException {
		TestInterceptor interceptor = new TestInterceptor(true);
		this.requestHandler.setHandshakeInterceptors(Collections.singletonList(interceptor));

		when(this.handshakeHandler.doHandshake(any(), any(), any(), any()))
				.thenThrow(new IllegalStateException("bad state"));

		try {
			this.requestHandler.handleRequest(new MockHttpServletRequest(), this.response);
			fail();
		}
		catch (HandshakeFailureException ex) {
			assertSame(ex, interceptor.getException());
			assertEquals("headerValue", this.response.getHeader("headerName"));
			assertEquals("exceptionHeaderValue", this.response.getHeader("exceptionHeaderName"));
		}
	}

	@Test // gh-23179
	public void handshakeNotAllowed() throws ServletException, IOException {
		TestInterceptor interceptor = new TestInterceptor(false);
		this.requestHandler.setHandshakeInterceptors(Collections.singletonList(interceptor));

		this.requestHandler.handleRequest(new MockHttpServletRequest(), this.response);

		verifyNoMoreInteractions(this.handshakeHandler);
		assertEquals("headerValue", this.response.getHeader("headerName"));
	}


	private static class TestInterceptor implements HandshakeInterceptor {

		private final boolean allowHandshake;

		private Exception exception;


		private TestInterceptor(boolean allowHandshake) {
			this.allowHandshake = allowHandshake;
		}


		public Exception getException() {
			return this.exception;
		}


		@Override
		public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
				WebSocketHandler wsHandler, Map<String, Object> attributes) {

			response.getHeaders().add("headerName", "headerValue");
			return this.allowHandshake;
		}

		@Override
		public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
				WebSocketHandler wsHandler, Exception exception) {

			response.getHeaders().add("exceptionHeaderName", "exceptionHeaderValue");
			this.exception = exception;
		}
	}

}
