/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.sockjs.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;
import org.springframework.web.socket.sockjs.transport.TransportType;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for
 * {@link org.springframework.web.socket.sockjs.client.AbstractClientSockJsSession}.
 *
 * @author Rossen Stoyanchev
 */
public class ClientSockJsSessionTests {

	private static final Jackson2SockJsMessageCodec CODEC = new Jackson2SockJsMessageCodec();

	private TestClientSockJsSession session;

	private WebSocketHandler handler;

	private SettableListenableFuture<WebSocketSession> connectFuture;

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@Before
	public void setup() throws Exception {
		SockJsUrlInfo urlInfo = new SockJsUrlInfo(new URI("http://example.com"));
		Transport transport = mock(Transport.class);
		TransportRequest request = new DefaultTransportRequest(urlInfo, null, null, transport, TransportType.XHR, CODEC);
		this.handler = mock(WebSocketHandler.class);
		this.connectFuture = new SettableListenableFuture<>();
		this.session = new TestClientSockJsSession(request, this.handler, this.connectFuture);
	}


	@Test
	public void handleFrameOpen() throws Exception {
		assertThat(this.session.isOpen(), is(false));
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		assertThat(this.session.isOpen(), is(true));
		assertTrue(this.connectFuture.isDone());
		assertThat(this.connectFuture.get(), sameInstance(this.session));
		verify(this.handler).afterConnectionEstablished(this.session);
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void handleFrameOpenWhenStatusNotNew() throws Exception {
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		assertThat(this.session.isOpen(), is(true));
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		assertThat(this.session.disconnectStatus, equalTo(new CloseStatus(1006, "Server lost session")));
	}

	@Test
	public void handleFrameOpenWithWebSocketHandlerException() throws Exception {
		willThrow(new IllegalStateException("Fake error")).given(this.handler).afterConnectionEstablished(this.session);
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		assertThat(this.session.isOpen(), is(true));
	}

	@Test
	public void handleFrameMessage() throws Exception {
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		this.session.handleFrame(SockJsFrame.messageFrame(CODEC, "foo", "bar").getContent());
		verify(this.handler).afterConnectionEstablished(this.session);
		verify(this.handler).handleMessage(this.session, new TextMessage("foo"));
		verify(this.handler).handleMessage(this.session, new TextMessage("bar"));
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void handleFrameMessageWhenNotOpen() throws Exception {
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		this.session.close();
		reset(this.handler);
		this.session.handleFrame(SockJsFrame.messageFrame(CODEC, "foo", "bar").getContent());
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void handleFrameMessageWithBadData() throws Exception {
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		this.session.handleFrame("a['bad data");
		assertThat(this.session.isOpen(), equalTo(false));
		assertThat(this.session.disconnectStatus, equalTo(CloseStatus.BAD_DATA));
		verify(this.handler).afterConnectionEstablished(this.session);
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void handleFrameMessageWithWebSocketHandlerException() throws Exception {
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		willThrow(new IllegalStateException("Fake error")).given(this.handler)
				.handleMessage(this.session, new TextMessage("foo"));
		willThrow(new IllegalStateException("Fake error")).given(this.handler)
				.handleMessage(this.session, new TextMessage("bar"));
		this.session.handleFrame(SockJsFrame.messageFrame(CODEC, "foo", "bar").getContent());
		assertThat(this.session.isOpen(), equalTo(true));
		verify(this.handler).afterConnectionEstablished(this.session);
		verify(this.handler).handleMessage(this.session, new TextMessage("foo"));
		verify(this.handler).handleMessage(this.session, new TextMessage("bar"));
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void handleFrameClose() throws Exception {
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		this.session.handleFrame(SockJsFrame.closeFrame(1007, "").getContent());
		assertThat(this.session.isOpen(), equalTo(false));
		assertThat(this.session.disconnectStatus, equalTo(new CloseStatus(1007, "")));
		verify(this.handler).afterConnectionEstablished(this.session);
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void handleTransportError() throws Exception {
		final IllegalStateException ex = new IllegalStateException("Fake error");
		this.session.handleTransportError(ex);
		verify(this.handler).handleTransportError(this.session, ex);
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void afterTransportClosed() throws Exception {
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		this.session.afterTransportClosed(CloseStatus.SERVER_ERROR);
		assertThat(this.session.isOpen(), equalTo(false));
		verify(this.handler).afterConnectionEstablished(this.session);
		verify(this.handler).afterConnectionClosed(this.session, CloseStatus.SERVER_ERROR);
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void close() throws Exception {
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		this.session.close();
		assertThat(this.session.isOpen(), equalTo(false));
		assertThat(this.session.disconnectStatus, equalTo(CloseStatus.NORMAL));
		verify(this.handler).afterConnectionEstablished(this.session);
		verifyNoMoreInteractions(this.handler);
	}

	@Test
	public void closeWithStatus() throws Exception {
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		this.session.close(new CloseStatus(3000, "reason"));
		assertThat(this.session.disconnectStatus, equalTo(new CloseStatus(3000, "reason")));
	}

	@Test
	public void closeWithNullStatus() throws Exception {
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Invalid close status");
		this.session.close(null);
	}

	@Test
	public void closeWithStatusOutOfRange() throws Exception {
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Invalid close status");
		this.session.close(new CloseStatus(2999, "reason"));
	}

	@Test
	public void timeoutTask() {
		this.session.getTimeoutTask().run();
		assertThat(this.session.disconnectStatus, equalTo(new CloseStatus(2007, "Transport timed out")));
	}

	@Test
	public void send() throws Exception {
		this.session.handleFrame(SockJsFrame.openFrame().getContent());
		this.session.sendMessage(new TextMessage("foo"));
		assertThat(this.session.sentMessage, equalTo(new TextMessage("[\"foo\"]")));
	}


	private static class TestClientSockJsSession extends AbstractClientSockJsSession {

		private TextMessage sentMessage;

		private CloseStatus disconnectStatus;


		protected TestClientSockJsSession(TransportRequest request, WebSocketHandler handler,
				SettableListenableFuture<WebSocketSession> connectFuture) {
			super(request, handler, connectFuture);
		}

		@Override
		protected void sendInternal(TextMessage textMessage) throws IOException {
			this.sentMessage = textMessage;
		}

		@Override
		protected void disconnect(CloseStatus status) throws IOException {
			this.disconnectStatus = status;
		}

		@Override
		public InetSocketAddress getLocalAddress() {
			return null;
		}

		@Override
		public InetSocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public String getAcceptedProtocol() {
			return null;
		}

		@Override
		public void setTextMessageSizeLimit(int messageSizeLimit) {

		}

		@Override
		public int getTextMessageSizeLimit() {
			return 0;
		}

		@Override
		public void setBinaryMessageSizeLimit(int messageSizeLimit) {

		}

		@Override
		public int getBinaryMessageSizeLimit() {
			return 0;
		}

		@Override
		public List<WebSocketExtension> getExtensions() {
			return null;
		}
	}

}
