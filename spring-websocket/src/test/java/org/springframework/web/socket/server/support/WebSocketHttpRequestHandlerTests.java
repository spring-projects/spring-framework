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

import org.junit.jupiter.api.Test;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link WebSocketHttpRequestHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.1.9
 */
public class WebSocketHttpRequestHandlerTests {

	private final HandshakeHandler handshakeHandler = mock(HandshakeHandler.class);

	private final WebSocketHttpRequestHandler requestHandler = new WebSocketHttpRequestHandler(mock(WebSocketHandler.class), this.handshakeHandler);

	private final MockHttpServletResponse response = new MockHttpServletResponse();


	@Test
	public void success() throws ServletException, IOException {
		TestInterceptor interceptor = new TestInterceptor(true);
		this.requestHandler.setHandshakeInterceptors(Collections.singletonList(interceptor));
		this.requestHandler.handleRequest(new MockHttpServletRequest(), this.response);

		verify(this.handshakeHandler).doHandshake(any(), any(), any(), any());
		assertThat(this.response.getHeader("headerName")).isEqualTo("headerValue");
	}

	@Test
	public void failure() {
		TestInterceptor interceptor = new TestInterceptor(true);
		this.requestHandler.setHandshakeInterceptors(Collections.singletonList(interceptor));

		given(this.handshakeHandler.doHandshake(any(), any(), any(), any()))
				.willThrow(new IllegalStateException("bad state"));

		assertThatThrownBy(() -> this.requestHandler.handleRequest(new MockHttpServletRequest(), this.response))
				.isInstanceOf(HandshakeFailureException.class)
				.hasRootCauseInstanceOf(IllegalStateException.class)
				.hasMessageEndingWith("bad state");

		assertThat(this.response.getHeader("headerName")).isEqualTo("headerValue");
		assertThat(this.response.getHeader("exceptionHeaderName")).isEqualTo("exceptionHeaderValue");
	}

	@Test // gh-23179
	public void handshakeNotAllowed() throws ServletException, IOException {
		TestInterceptor interceptor = new TestInterceptor(false);
		this.requestHandler.setHandshakeInterceptors(Collections.singletonList(interceptor));

		this.requestHandler.handleRequest(new MockHttpServletRequest(), this.response);

		verifyNoMoreInteractions(this.handshakeHandler);
		assertThat(this.response.getHeader("headerName")).isEqualTo("headerValue");
	}


	private static class TestInterceptor implements HandshakeInterceptor {

		private final boolean allowHandshake;

		private TestInterceptor(boolean allowHandshake) {
			this.allowHandshake = allowHandshake;
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
		}
	}

}
