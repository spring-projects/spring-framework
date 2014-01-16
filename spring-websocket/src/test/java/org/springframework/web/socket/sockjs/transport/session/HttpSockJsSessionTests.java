/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.frame.DefaultSockJsFrameFormat;
import org.springframework.web.socket.sockjs.frame.SockJsFrameFormat;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.transport.SockJsServiceConfig;
import org.springframework.web.socket.sockjs.transport.session.HttpSockJsSessionTests.TestAbstractHttpSockJsSession;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

	@Before
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

		assertTrue(this.session.hasRequest());
		assertTrue(this.session.hasResponse());

		assertEquals("hhh\no", this.servletResponse.getContentAsString());
		assertFalse(this.servletRequest.isAsyncStarted());

		verify(this.webSocketHandler).afterConnectionEstablished(this.session);
	}

	@Test
	public void handleSuccessiveRequest() throws Exception {

		this.session.getMessageCache().add("x");
		this.session.handleSuccessiveRequest(this.request, this.response, this.frameFormat);

		assertTrue(this.session.hasRequest());
		assertTrue(this.session.hasResponse());
		assertTrue(this.servletRequest.isAsyncStarted());

		assertTrue(this.session.wasHeartbeatScheduled());
		assertTrue(this.session.wasCacheFlushed());

		assertEquals("hhh\n", this.servletResponse.getContentAsString());

		verifyNoMoreInteractions(this.webSocketHandler);
	}


	static class TestAbstractHttpSockJsSession extends AbstractHttpSockJsSession {

		private IOException exceptionOnWriteFrame;

		private boolean cacheFlushed;

		private boolean heartbeatScheduled;


		public TestAbstractHttpSockJsSession(SockJsServiceConfig config, WebSocketHandler handler,
				Map<String, Object> attributes) {

			super("1", config, handler, attributes);
		}

		@Override
		protected void writePrelude() throws IOException {
			getResponse().getBody().write("hhh\n".getBytes());
		}

		public boolean wasCacheFlushed() {
			return this.cacheFlushed;
		}

		public boolean wasHeartbeatScheduled() {
			return this.heartbeatScheduled;
		}

		public boolean hasRequest() {
			return getRequest() != null;
		}

		public boolean hasResponse() {
			return getResponse() != null;
		}

		public void setExceptionOnWriteFrame(IOException exceptionOnWriteFrame) {
			this.exceptionOnWriteFrame = exceptionOnWriteFrame;
		}

		@Override
		protected void flushCache() {
			this.cacheFlushed = true;
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
