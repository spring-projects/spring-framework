/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.socket.messaging;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledFuture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.ConnectionHandlingStompSession;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WebSocketStompClient}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketStompClientTests {

	@Mock
	private TaskScheduler taskScheduler;

	@Mock
	private ConnectionHandlingStompSession stompSession;

	@Mock
	private WebSocketSession webSocketSession;


	private TestWebSocketStompClient stompClient;

	private ArgumentCaptor<WebSocketHandler> webSocketHandlerCaptor;

	private SettableListenableFuture<WebSocketSession> handshakeFuture;


	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		WebSocketClient webSocketClient = mock(WebSocketClient.class);
		this.stompClient = new TestWebSocketStompClient(webSocketClient);
		this.stompClient.setTaskScheduler(this.taskScheduler);
		this.stompClient.setStompSession(this.stompSession);

		this.webSocketHandlerCaptor = ArgumentCaptor.forClass(WebSocketHandler.class);
		this.handshakeFuture = new SettableListenableFuture<>();
		when(webSocketClient.doHandshake(this.webSocketHandlerCaptor.capture(), any(), any(URI.class)))
				.thenReturn(this.handshakeFuture);
	}


	@Test
	public void webSocketHandshakeFailure() throws Exception {
		connect();

		IllegalStateException handshakeFailure = new IllegalStateException("simulated exception");
		this.handshakeFuture.setException(handshakeFailure);

		verify(this.stompSession).afterConnectFailure(same(handshakeFailure));
	}

	@Test
	public void webSocketConnectionEstablished() throws Exception {
		connect().afterConnectionEstablished(this.webSocketSession);
		verify(this.stompSession).afterConnected(notNull());
	}

	@Test
	public void webSocketTransportError() throws Exception {
		IllegalStateException exception = new IllegalStateException("simulated exception");
		connect().handleTransportError(this.webSocketSession, exception);

		verify(this.stompSession).handleFailure(same(exception));
	}

	@Test
	public void webSocketConnectionClosed() throws Exception {
		connect().afterConnectionClosed(this.webSocketSession, CloseStatus.NORMAL);
		verify(this.stompSession).afterConnectionClosed();
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void handleWebSocketMessage() throws Exception {
		String text = "SEND\na:alpha\n\nMessage payload\0";
		connect().handleMessage(this.webSocketSession, new TextMessage(text));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(this.stompSession).handleMessage(captor.capture());
		Message<byte[]> message = captor.getValue();
		assertNotNull(message);

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		StompHeaders headers = StompHeaders.readOnlyStompHeaders(accessor.toNativeHeaderMap());
		assertEquals(StompCommand.SEND, accessor.getCommand());
		assertEquals("alpha", headers.getFirst("a"));
		assertEquals("Message payload", new String(message.getPayload(), StandardCharsets.UTF_8));
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void handleWebSocketMessageSplitAcrossTwoMessage() throws Exception {
		WebSocketHandler webSocketHandler = connect();

		String part1 = "SEND\na:alpha\n\nMessage";
		webSocketHandler.handleMessage(this.webSocketSession, new TextMessage(part1));

		verifyNoMoreInteractions(this.stompSession);

		String part2 = " payload\0";
		webSocketHandler.handleMessage(this.webSocketSession, new TextMessage(part2));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(this.stompSession).handleMessage(captor.capture());
		Message<byte[]> message = captor.getValue();
		assertNotNull(message);

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		StompHeaders headers = StompHeaders.readOnlyStompHeaders(accessor.toNativeHeaderMap());
		assertEquals(StompCommand.SEND, accessor.getCommand());
		assertEquals("alpha", headers.getFirst("a"));
		assertEquals("Message payload", new String(message.getPayload(), StandardCharsets.UTF_8));
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void handleWebSocketMessageBinary() throws Exception {
		String text = "SEND\na:alpha\n\nMessage payload\0";
		connect().handleMessage(this.webSocketSession, new BinaryMessage(text.getBytes(StandardCharsets.UTF_8)));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(this.stompSession).handleMessage(captor.capture());
		Message<byte[]> message = captor.getValue();
		assertNotNull(message);

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		StompHeaders headers = StompHeaders.readOnlyStompHeaders(accessor.toNativeHeaderMap());
		assertEquals(StompCommand.SEND, accessor.getCommand());
		assertEquals("alpha", headers.getFirst("a"));
		assertEquals("Message payload", new String(message.getPayload(), StandardCharsets.UTF_8));
	}

	@Test
	public void handleWebSocketMessagePong() throws Exception {
		connect().handleMessage(this.webSocketSession, new PongMessage());
		verifyNoMoreInteractions(this.stompSession);
	}

	@Test
	public void sendWebSocketMessage() throws Exception {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		accessor.setDestination("/topic/foo");
		byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);

		getTcpConnection().send(MessageBuilder.createMessage(payload, accessor.getMessageHeaders()));

		ArgumentCaptor<TextMessage> textMessageCaptor = ArgumentCaptor.forClass(TextMessage.class);
		verify(this.webSocketSession).sendMessage(textMessageCaptor.capture());
		TextMessage textMessage = textMessageCaptor.getValue();
		assertNotNull(textMessage);
		assertEquals("SEND\ndestination:/topic/foo\ncontent-length:7\n\npayload\0", textMessage.getPayload());
	}

	@Test
	public void sendWebSocketBinary() throws Exception {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
		accessor.setDestination("/b");
		accessor.setContentType(MimeTypeUtils.APPLICATION_OCTET_STREAM);
		byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);

		getTcpConnection().send(MessageBuilder.createMessage(payload, accessor.getMessageHeaders()));

		ArgumentCaptor<BinaryMessage> binaryMessageCaptor = ArgumentCaptor.forClass(BinaryMessage.class);
		verify(this.webSocketSession).sendMessage(binaryMessageCaptor.capture());
		BinaryMessage binaryMessage = binaryMessageCaptor.getValue();
		assertNotNull(binaryMessage);
		assertEquals("SEND\ndestination:/b\ncontent-type:application/octet-stream\ncontent-length:7\n\npayload\0",
				new String(binaryMessage.getPayload().array(), StandardCharsets.UTF_8));
	}

	@Test
	public void heartbeatDefaultValue() throws Exception {
		WebSocketStompClient stompClient = new WebSocketStompClient(mock(WebSocketClient.class));
		assertArrayEquals(new long[] {0, 0}, stompClient.getDefaultHeartbeat());

		StompHeaders connectHeaders = stompClient.processConnectHeaders(null);
		assertArrayEquals(new long[] {0, 0}, connectHeaders.getHeartbeat());
	}

	@Test
	public void heartbeatDefaultValueWithScheduler() throws Exception {
		WebSocketStompClient stompClient = new WebSocketStompClient(mock(WebSocketClient.class));
		stompClient.setTaskScheduler(mock(TaskScheduler.class));
		assertArrayEquals(new long[] {10000, 10000}, stompClient.getDefaultHeartbeat());

		StompHeaders connectHeaders = stompClient.processConnectHeaders(null);
		assertArrayEquals(new long[] {10000, 10000}, connectHeaders.getHeartbeat());
	}

	@Test
	public void heartbeatDefaultValueSetWithoutScheduler() throws Exception {
		WebSocketStompClient stompClient = new WebSocketStompClient(mock(WebSocketClient.class));
		stompClient.setDefaultHeartbeat(new long[] {5, 5});
		try {
			stompClient.processConnectHeaders(null);
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// ignore
		}
	}

	@Test
	public void readInactivityAfterDelayHasElapsed() throws Exception {
		TcpConnection<byte[]> tcpConnection = getTcpConnection();
		Runnable runnable = mock(Runnable.class);
		long delay = 2;
		tcpConnection.onReadInactivity(runnable, delay);
		testInactivityTaskScheduling(runnable, delay, 10);
	}

	@Test
	public void readInactivityBeforeDelayHasElapsed() throws Exception {
		TcpConnection<byte[]> tcpConnection = getTcpConnection();
		Runnable runnable = mock(Runnable.class);
		long delay = 10000;
		tcpConnection.onReadInactivity(runnable, delay);
		testInactivityTaskScheduling(runnable, delay, 0);
	}

	@Test
	public void writeInactivityAfterDelayHasElapsed() throws Exception {
		TcpConnection<byte[]> tcpConnection = getTcpConnection();
		Runnable runnable = mock(Runnable.class);
		long delay = 2;
		tcpConnection.onWriteInactivity(runnable, delay);
		testInactivityTaskScheduling(runnable, delay, 10);
	}

	@Test
	public void writeInactivityBeforeDelayHasElapsed() throws Exception {
		TcpConnection<byte[]> tcpConnection = getTcpConnection();
		Runnable runnable = mock(Runnable.class);
		long delay = 1000;
		tcpConnection.onWriteInactivity(runnable, delay);
		testInactivityTaskScheduling(runnable, delay, 0);
	}

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void cancelInactivityTasks() throws Exception {
		TcpConnection<byte[]> tcpConnection = getTcpConnection();

		ScheduledFuture future = mock(ScheduledFuture.class);
		when(this.taskScheduler.scheduleWithFixedDelay(any(), eq(1L))).thenReturn(future);

		tcpConnection.onReadInactivity(mock(Runnable.class), 2L);
		tcpConnection.onWriteInactivity(mock(Runnable.class), 2L);

		this.webSocketHandlerCaptor.getValue().afterConnectionClosed(this.webSocketSession, CloseStatus.NORMAL);

		verify(future, times(2)).cancel(true);
		verifyNoMoreInteractions(future);
	}


	private WebSocketHandler connect() {
		this.stompClient.connect("/foo", mock(StompSessionHandler.class));

		verify(this.stompSession).getSessionFuture();
		verifyNoMoreInteractions(this.stompSession);

		WebSocketHandler webSocketHandler = this.webSocketHandlerCaptor.getValue();
		assertNotNull(webSocketHandler);
		return webSocketHandler;
	}

	@SuppressWarnings("unchecked")
	private TcpConnection<byte[]> getTcpConnection() throws Exception {
		WebSocketHandler webSocketHandler = connect();
		webSocketHandler.afterConnectionEstablished(this.webSocketSession);
		return (TcpConnection<byte[]>) webSocketHandler;
	}

	private void testInactivityTaskScheduling(Runnable runnable, long delay, long sleepTime)
			throws InterruptedException {

		ArgumentCaptor<Runnable> inactivityTaskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.taskScheduler).scheduleWithFixedDelay(inactivityTaskCaptor.capture(), eq(delay/2));
		verifyNoMoreInteractions(this.taskScheduler);

		if (sleepTime > 0) {
			Thread.sleep(sleepTime);
		}

		Runnable inactivityTask = inactivityTaskCaptor.getValue();
		assertNotNull(inactivityTask);
		inactivityTask.run();

		if (sleepTime > 0) {
			verify(runnable).run();
		}
		else {
			verifyNoMoreInteractions(runnable);
		}
	}


	private static class TestWebSocketStompClient extends WebSocketStompClient {

		private ConnectionHandlingStompSession stompSession;

		public TestWebSocketStompClient(WebSocketClient webSocketClient) {
			super(webSocketClient);
		}

		public void setStompSession(ConnectionHandlingStompSession stompSession) {
			this.stompSession = stompSession;
		}

		@Override
		protected ConnectionHandlingStompSession createSession(StompHeaders headers, StompSessionHandler handler) {
			return this.stompSession;
		}
	}

}
