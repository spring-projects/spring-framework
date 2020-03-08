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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.frame.DefaultSockJsFrameFormat;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.frame.SockJsFrameFormat;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.session.HttpSockJsSessionTests.TestAbstractHttpSockJsSession;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link AbstractHttpSockJsSession}.
 *
 * @author Rossen Stoyanchev
 */
public class HttpSockJsSessionTests extends AbstractSockJsSessionTests<TestAbstractHttpSockJsSession> {

	protected ServerHttpRequest request;

	protected ServerHttpResponse response;

	protected MockHttpServletRequest servletRequest;

	protected MockHttpServletResponse servletResponse;

	private SockJsFrameFormat frameFormat;


	@Override
	protected TestAbstractHttpSockJsSession initSockJsSession() {
		return new TestAbstractHttpSockJsSession(this.sockJsConfig, this.webSocketHandler, null);
	}

	@BeforeEach
	public void setup() {

		super.setUp();

		this.frameFormat = new DefaultSockJsFrameFormat("%s");

		this.servletResponse = new MockHttpServletResponse();
		this.response = new ServletServerHttpResponse(this.servletResponse);

		this.servletRequest = new MockHttpServletRequest();
		this.servletRequest.setAsyncSupported(true);
		this.request = new ServletServerHttpRequest(this.servletRequest);
	}

	@Test
	public void handleInitialRequest() throws Exception {

		this.session.handleInitialRequest(this.request, this.response, this.frameFormat);

		assertThat(this.servletResponse.getContentAsString()).isEqualTo("hhh\no");
		assertThat(this.servletRequest.isAsyncStarted()).isTrue();

		verify(this.webSocketHandler).afterConnectionEstablished(this.session);
	}

	@Test
	public void handleSuccessiveRequest() throws Exception {

		this.session.getMessageCache().add("x");
		this.session.handleSuccessiveRequest(this.request, this.response, this.frameFormat);

		assertThat(this.servletRequest.isAsyncStarted()).isTrue();
		assertThat(this.session.wasHeartbeatScheduled()).isTrue();
		assertThat(this.session.wasCacheFlushed()).isTrue();
		assertThat(this.servletResponse.getContentAsString()).isEqualTo("hhh\n");

		verifyNoMoreInteractions(this.webSocketHandler);
	}


	static class TestAbstractHttpSockJsSession extends StreamingSockJsSession {

		private IOException exceptionOnWriteFrame;

		private boolean cacheFlushed;

		private boolean heartbeatScheduled;


		public TestAbstractHttpSockJsSession(SockJsServiceConfig config, WebSocketHandler handler,
				Map<String, Object> attributes) {

			super("1", config, handler, attributes);
		}

		@Override
		protected byte[] getPrelude(ServerHttpRequest request) {
			return "hhh\n".getBytes();
		}

		public boolean wasCacheFlushed() {
			return this.cacheFlushed;
		}

		public boolean wasHeartbeatScheduled() {
			return this.heartbeatScheduled;
		}

		public void setExceptionOnWriteFrame(IOException exceptionOnWriteFrame) {
			this.exceptionOnWriteFrame = exceptionOnWriteFrame;
		}

		@Override
		protected void flushCache() {
			this.cacheFlushed = true;
			scheduleHeartbeat();
		}

		@Override
		protected void scheduleHeartbeat() {
			this.heartbeatScheduled = true;
		}

		@Override
		protected synchronized void writeFrameInternal(SockJsFrame frame) throws IOException {
			if (this.exceptionOnWriteFrame != null) {
				throw this.exceptionOnWriteFrame;
			}
			else {
				super.writeFrameInternal(frame);
			}
		}
	}

}
