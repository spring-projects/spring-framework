/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.socket.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator.OverflowStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ConcurrentWebSocketSessionDecorator}.
 *
 * @author Rossen Stoyanchev
 */
class ConcurrentWebSocketSessionDecoratorTests {

	@Test
	void send() throws IOException {

		TestWebSocketSession session = new TestWebSocketSession();
		session.setOpen(true);

		ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, 1000, 1024);

		TextMessage textMessage = new TextMessage("payload");
		decorator.sendMessage(textMessage);

		assertThat(session.getSentMessages()).containsExactly(textMessage);

		assertThat(decorator.getBufferSize()).isEqualTo(0);
		assertThat(decorator.getTimeSinceSendStarted()).isEqualTo(0);
		assertThat(session.isOpen()).isTrue();
	}

	@Test
	void sendAfterBlockedSend() throws IOException, InterruptedException {

		BlockingWebSocketSession session = new BlockingWebSocketSession();
		session.setOpen(true);

		ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, 10 * 1000, 1024);

		sendBlockingMessage(decorator);

		Thread.sleep(50);
		assertThat(decorator.getTimeSinceSendStarted()).isGreaterThan(0);

		TextMessage payload = new TextMessage("payload");
		for (int i = 0; i < 5; i++) {
			decorator.sendMessage(payload);
		}

		assertThat(decorator.getTimeSinceSendStarted()).isGreaterThan(0);
		assertThat(decorator.getBufferSize()).isEqualTo((5 * payload.getPayloadLength()));
		assertThat(session.isOpen()).isTrue();
	}

	@Test
	void sendTimeLimitExceeded() throws InterruptedException {

		BlockingWebSocketSession session = new BlockingWebSocketSession();
		session.setId("123");
		session.setOpen(true);

		ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, 100, 1024);

		sendBlockingMessage(decorator);

		// Exceed send time
		Thread.sleep(200);

		TextMessage payload = new TextMessage("payload");
		assertThatExceptionOfType(SessionLimitExceededException.class).isThrownBy(() ->
				decorator.sendMessage(payload))
			.withMessageMatching("Send time [\\d]+ \\(ms\\) for session '123' exceeded the allowed limit 100")
			.satisfies(ex -> assertThat(ex.getStatus()).isEqualTo(CloseStatus.SESSION_NOT_RELIABLE));
	}

	@Test
	void sendBufferSizeExceeded() throws IOException, InterruptedException {

		BlockingWebSocketSession session = new BlockingWebSocketSession();
		session.setId("123");
		session.setOpen(true);

		ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, 10*1000, 1024);

		sendBlockingMessage(decorator);

		String msg = String.format("%1023s", "a");
		TextMessage message = new TextMessage(msg);
		decorator.sendMessage(message);

		assertThat(decorator.getBufferSize()).isEqualTo(1023);
		assertThat(session.isOpen()).isTrue();

		assertThatExceptionOfType(SessionLimitExceededException.class).isThrownBy(() ->
				decorator.sendMessage(message))
			.withMessageMatching("Buffer size [\\d]+ bytes for session '123' exceeds the allowed limit 1024")
			.satisfies(ex -> assertThat(ex.getStatus()).isEqualTo(CloseStatus.SESSION_NOT_RELIABLE));
	}

	@Test // SPR-17140
	public void overflowStrategyDrop() throws IOException, InterruptedException {

		BlockingWebSocketSession session = new BlockingWebSocketSession();
		session.setId("123");
		session.setOpen(true);

		ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, 10*1000, 1024, OverflowStrategy.DROP);

		sendBlockingMessage(decorator);

		String msg = String.format("%1023s", "a");

		for (int i = 0; i < 5; i++) {
			TextMessage message = new TextMessage(msg);
			decorator.sendMessage(message);
		}

		assertThat(decorator.getBufferSize()).isEqualTo(1023);
		assertThat(session.isOpen()).isTrue();
	}

	@Test
	void closeStatusNormal() throws Exception {

		BlockingWebSocketSession session = new BlockingWebSocketSession();
		session.setOpen(true);
		WebSocketSession decorator = new ConcurrentWebSocketSessionDecorator(session, 10 * 1000, 1024);

		decorator.close(CloseStatus.PROTOCOL_ERROR);
		assertThat(session.getCloseStatus()).isEqualTo(CloseStatus.PROTOCOL_ERROR);

		decorator.close(CloseStatus.SERVER_ERROR);
		assertThat(session.getCloseStatus()).as("Should have been ignored").isEqualTo(CloseStatus.PROTOCOL_ERROR);
	}

	@Test
	void closeStatusChangesToSessionNotReliable() throws Exception {

		BlockingWebSocketSession session = new BlockingWebSocketSession();
		session.setId("123");
		session.setOpen(true);
		CountDownLatch sentMessageLatch = session.initSendLatch();

		int sendTimeLimit = 100;
		int bufferSizeLimit = 1024;

		ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, sendTimeLimit, bufferSizeLimit);

		Executors.newSingleThreadExecutor().submit(() -> {
			TextMessage message = new TextMessage("slow message");
			try {
				decorator.sendMessage(message);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		});

		assertThat(sentMessageLatch.await(5, TimeUnit.SECONDS)).isTrue();

		// ensure some send time elapses
		Thread.sleep(sendTimeLimit + 100);

		decorator.close(CloseStatus.PROTOCOL_ERROR);

		assertThat(session.getCloseStatus())
				.as("CloseStatus should have changed to SESSION_NOT_RELIABLE")
				.isEqualTo(CloseStatus.SESSION_NOT_RELIABLE);
	}

	private void sendBlockingMessage(ConcurrentWebSocketSessionDecorator session) throws InterruptedException {
		CountDownLatch latch = ((BlockingWebSocketSession) session.getDelegate()).initSendLatch();
		Executors.newSingleThreadExecutor().submit(() -> {
			TextMessage message = new TextMessage("slow message");
			try {
				session.sendMessage(message);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		});
		assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void configuredProperties() {
		TestWebSocketSession session = new TestWebSocketSession();
		ConcurrentWebSocketSessionDecorator sessionDecorator =
				new ConcurrentWebSocketSessionDecorator(session, 42, 43, OverflowStrategy.DROP);

		assertThat(sessionDecorator.getSendTimeLimit()).isEqualTo(42);
		assertThat(sessionDecorator.getBufferSizeLimit()).isEqualTo(43);
		assertThat(sessionDecorator.getOverflowStrategy()).isEqualTo(OverflowStrategy.DROP);
	}

	@Test
	void concurrentBinaryMessageSharingAcrossSessions() throws Exception {
		byte[] originalData = new byte[100];
		for (int i = 0; i < originalData.length; i++) {
			originalData[i] = (byte) i;
		}
		ByteBuffer buffer = ByteBuffer.wrap(originalData);
		BinaryMessage sharedMessage = new BinaryMessage(buffer);

		int sessionCount = 5;
		int messagesPerSession = 3;
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch completeLatch = new CountDownLatch(sessionCount * messagesPerSession);
		AtomicInteger corruptedBuffers = new AtomicInteger(0);
		
		List<TestBinaryMessageCapturingSession> sessions = new ArrayList<>();
		List<ConcurrentWebSocketSessionDecorator> decorators = new ArrayList<>();
		
		for (int i = 0; i < sessionCount; i++) {
			TestBinaryMessageCapturingSession session = new TestBinaryMessageCapturingSession();
			session.setOpen(true);
			session.setId("session-" + i);
			sessions.add(session);
			
			ConcurrentWebSocketSessionDecorator decorator = 
					new ConcurrentWebSocketSessionDecorator(session, 10000, 10240);
			decorators.add(decorator);
		}

		ExecutorService executor = Executors.newFixedThreadPool(sessionCount * messagesPerSession);

		try {
			for (ConcurrentWebSocketSessionDecorator decorator : decorators) {
				for (int j = 0; j < messagesPerSession; j++) {
					executor.submit(() -> {
						try {
							startLatch.await();
							decorator.sendMessage(sharedMessage);
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							completeLatch.countDown();
						}
					});
				}
			}

			startLatch.countDown();
			assertThat(completeLatch.await(5, TimeUnit.SECONDS)).isTrue();

			for (TestBinaryMessageCapturingSession session : sessions) {
				List<ByteBuffer> capturedBuffers = session.getCapturedBuffers();
				
				for (ByteBuffer capturedBuffer : capturedBuffers) {
					byte[] capturedData = new byte[capturedBuffer.remaining()];
					capturedBuffer.get(capturedData);
					
					boolean isCorrupted = false;
					if (capturedData.length != originalData.length) {
						isCorrupted = true;
					} else {
						for (int j = 0; j < originalData.length; j++) {
							if (capturedData[j] != originalData[j]) {
								isCorrupted = true;
								break;
							}
						}
					}
					
					if (isCorrupted) {
						corruptedBuffers.incrementAndGet();
					}
				}
			}

			assertThat(corruptedBuffers.get())
					.as("No ByteBuffer corruption should occur with duplicate() fix")
					.isEqualTo(0);
		} finally {
			executor.shutdown();
		}
	}

	static class TestBinaryMessageCapturingSession extends TestWebSocketSession {
		private final List<ByteBuffer> capturedBuffers = new ArrayList<>();
		
		@Override
		public void sendMessage(WebSocketMessage<?> message) throws IOException {
			if (message instanceof BinaryMessage) {
				ByteBuffer payload = ((BinaryMessage) message).getPayload();
				ByteBuffer captured = ByteBuffer.allocate(payload.remaining());
				
				while (payload.hasRemaining()) {
					captured.put(payload.get());
				}
				captured.flip();
				
				synchronized (capturedBuffers) {
					capturedBuffers.add(captured);
				}
				
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			super.sendMessage(message);
		}
		
		public synchronized List<ByteBuffer> getCapturedBuffers() {
			return new ArrayList<>(capturedBuffers);
		}
	}

}
