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

package org.springframework.web.socket.messaging;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link WebSocketStompClient}.
 *
 * @author Rossen Stoyanchev
 */
@MockitoSettings(strictness = Strictness.LENIENT)
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


	@BeforeEach
	public void setUp() throws Exception {
		WebSocketClient webSocketClient = mock(WebSocketClient.class);
		this.stompClient = new TestWebSocketStompClient(webSocketClient);
		this.stompClient.setTaskScheduler(this.taskScheduler);
		this.stompClient.setStompSession(this.stompSession);

		this.webSocketHandlerCaptor = ArgumentCaptor.forClass(WebSocketHandler.class);
		this.handshakeFuture = new SettableListenableFuture<>();
		given(webSocketClient.doHandshake(this.webSocketHandlerCaptor.capture(), any(), any(URI.class)))
				.willReturn(this.handshakeFuture);
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
		assertThat(message).isNotNull();

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		StompHeaders headers = StompHeaders.readOnlyStompHeaders(accessor.toNativeHeaderMap());
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.SEND);
		assertThat(headers.getFirst("a")).isEqualTo("alpha");
		assertThat(new String(message.getPayload(), StandardCharsets.UTF_8)).isEqualTo("Message payload");
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
		assertThat(message).isNotNull();

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		StompHeaders headers = StompHeaders.readOnlyStompHeaders(accessor.toNativeHeaderMap());
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.SEND);
		assertThat(headers.getFirst("a")).isEqualTo("alpha");
		assertThat(new String(message.getPayload(), StandardCharsets.UTF_8)).isEqualTo("Message payload");
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void handleWebSocketMessageBinary() throws Exception {
		String text = "SEND\na:alpha\n\nMessage payload\0";
		connect().handleMessage(this.webSocketSession, new BinaryMessage(text.getBytes(StandardCharsets.UTF_8)));

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(this.stompSession).handleMessage(captor.capture());
		Message<byte[]> message = captor.getValue();
		assertThat(message).isNotNull();

		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		StompHeaders headers = StompHeaders.readOnlyStompHeaders(accessor.toNativeHeaderMap());
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.SEND);
		assertThat(headers.getFirst("a")).isEqualTo("alpha");
		assertThat(new String(message.getPayload(), StandardCharsets.UTF_8)).isEqualTo("Message payload");
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
		assertThat(textMessage).isNotNull();
		assertThat(textMessage.getPayload()).isEqualTo("SEND\ndestination:/topic/foo\ncontent-length:7\n\npayload\0");
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
		assertThat(binaryMessage).isNotNull();
		assertThat(new String(binaryMessage.getPayload().array(), StandardCharsets.UTF_8)).isEqualTo("SEND\ndestination:/b\ncontent-type:application/octet-stream\ncontent-length:7\n\npayload\0");
	}

	@Test
	public void heartbeatDefaultValue() throws Exception {
		WebSocketStompClient stompClient = new WebSocketStompClient(mock(WebSocketClient.class));
		assertThat(stompClient.getDefaultHeartbeat()).isEqualTo(new long[] {0, 0});

		StompHeaders connectHeaders = stompClient.processConnectHeaders(null);
		assertThat(connectHeaders.getHeartbeat()).isEqualTo(new long[] {0, 0});
	}

	@Test
	public void heartbeatDefaultValueWithScheduler() throws Exception {
		WebSocketStompClient stompClient = new WebSocketStompClient(mock(WebSocketClient.class));
		stompClient.setTaskScheduler(mock(TaskScheduler.class));
		assertThat(stompClient.getDefaultHeartbeat()).isEqualTo(new long[] {10000, 10000});

		StompHeaders connectHeaders = stompClient.processConnectHeaders(null);
		assertThat(connectHeaders.getHeartbeat()).isEqualTo(new long[] {10000, 10000});
	}

	@Test
	public void heartbeatDefaultValueSetWithoutScheduler() throws Exception {
		WebSocketStompClient stompClient = new WebSocketStompClient(mock(WebSocketClient.class));
		stompClient.setDefaultHeartbeat(new long[] {5, 5});
		assertThatIllegalStateException().isThrownBy(() ->
				stompClient.processConnectHeaders(null));
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
		given(this.taskScheduler.scheduleWithFixedDelay(any(), eq(1L))).willReturn(future);

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
		assertThat(webSocketHandler).isNotNull();
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
		assertThat(inactivityTask).isNotNull();
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
