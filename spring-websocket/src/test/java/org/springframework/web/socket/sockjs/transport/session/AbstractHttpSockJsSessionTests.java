/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame.DefaultFrameFormat;
import org.springframework.web.socket.sockjs.support.frame.SockJsFrame.FrameFormat;
import org.springframework.web.socket.sockjs.transport.session.AbstractHttpSockJsSessionTests.TestAbstractHttpSockJsSession;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test fixture for {@link AbstractHttpSockJsSession}.
 *
 * @author Rossen Stoyanchev
 */
public class AbstractHttpSockJsSessionTests extends BaseAbstractSockJsSessionTests<TestAbstractHttpSockJsSession> {

	protected ServerHttpRequest request;

	protected ServerHttpResponse response;

	protected MockHttpServletRequest servletRequest;

	protected MockHttpServletResponse servletResponse;

	private FrameFormat frameFormat;


	@Before
	public void setup() {

		super.setUp();

		this.frameFormat = new DefaultFrameFormat("%s");

		this.servletResponse = new MockHttpServletResponse();
		this.response = new ServletServerHttpResponse(this.servletResponse);

		this.servletRequest = new MockHttpServletRequest();
		this.servletRequest.setAsyncSupported(true);
		this.request = new ServletServerHttpRequest(this.servletRequest);
	}

	@Override
	protected TestAbstractHttpSockJsSession initSockJsSession() {
		return new TestAbstractHttpSockJsSession(this.sockJsConfig, this.webSocketHandler);
	}

	@Test
	public void setInitialRequest() throws Exception {

		this.session.setInitialRequest(this.request, this.response, this.frameFormat);

		assertTrue(this.session.hasRequest());
		assertTrue(this.session.hasResponse());

		assertEquals("o", this.servletResponse.getContentAsString());
		assertFalse(this.servletRequest.isAsyncStarted());

		verify(this.webSocketHandler).afterConnectionEstablished(this.session);
	}

	@Test
	public void setLongPollingRequest() throws Exception {

		this.session.getMessageCache().add("x");
		this.session.setLongPollingRequest(this.request, this.response, this.frameFormat);

		assertTrue(this.session.hasRequest());
		assertTrue(this.session.hasResponse());
		assertTrue(this.servletRequest.isAsyncStarted());

		assertTrue(this.session.wasHeartbeatScheduled());
		assertTrue(this.session.wasCacheFlushed());

		verifyNoMoreInteractions(this.webSocketHandler);
	}

	@Test
	public void setLongPollingRequestWhenClosed() throws Exception {

		this.session.delegateConnectionClosed(CloseStatus.NORMAL);
		assertClosed();

		this.session.setLongPollingRequest(this.request, this.response, this.frameFormat);

		assertEquals("c[3000,\"Go away!\"]", this.servletResponse.getContentAsString());
		assertFalse(this.servletRequest.isAsyncStarted());
	}


	static class TestAbstractHttpSockJsSession extends AbstractHttpSockJsSession {

		private IOException exceptionOnWriteFrame;

		private boolean cacheFlushed;

		private boolean heartbeatScheduled;


		public TestAbstractHttpSockJsSession(SockJsServiceConfig config, WebSocketHandler handler) {
			super("1", config, handler);
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
		protected void flushCache() throws IOException {
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
