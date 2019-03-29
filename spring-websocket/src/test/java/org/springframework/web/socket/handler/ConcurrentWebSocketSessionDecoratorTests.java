/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator.OverflowStrategy;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ConcurrentWebSocketSessionDecorator}.
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("resource")
public class ConcurrentWebSocketSessionDecoratorTests {

	@Test
	public void send() throws IOException {

		TestWebSocketSession session = new TestWebSocketSession();
		session.setOpen(true);

		ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, 1000, 1024);

		TextMessage textMessage = new TextMessage("payload");
		decorator.sendMessage(textMessage);

		assertEquals(1, session.getSentMessages().size());
		assertEquals(textMessage, session.getSentMessages().get(0));

		assertEquals(0, decorator.getBufferSize());
		assertEquals(0, decorator.getTimeSinceSendStarted());
		assertTrue(session.isOpen());
	}

	@Test
	public void sendAfterBlockedSend() throws IOException, InterruptedException {

		BlockingSession session = new BlockingSession();
		session.setOpen(true);

		final ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, 10 * 1000, 1024);

		sendBlockingMessage(decorator);

		Thread.sleep(50);
		assertTrue(decorator.getTimeSinceSendStarted() > 0);

		TextMessage payload = new TextMessage("payload");
		for (int i = 0; i < 5; i++) {
			decorator.sendMessage(payload);
		}

		assertTrue(decorator.getTimeSinceSendStarted() > 0);
		assertEquals(5 * payload.getPayloadLength(), decorator.getBufferSize());
		assertTrue(session.isOpen());
	}

	@Test
	public void sendTimeLimitExceeded() throws IOException, InterruptedException {

		BlockingSession session = new BlockingSession();
		session.setId("123");
		session.setOpen(true);

		final ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, 100, 1024);

		sendBlockingMessage(decorator);

		// Exceed send time..
		Thread.sleep(200);

		try {
			TextMessage payload = new TextMessage("payload");
			decorator.sendMessage(payload);
			fail("Expected exception");
		}
		catch (SessionLimitExceededException ex) {
			String actual = ex.getMessage();
			String regex = "Send time [\\d]+ \\(ms\\) for session '123' exceeded the allowed limit 100";
			assertTrue("Unexpected message: " + actual, actual.matches(regex));
			assertEquals(CloseStatus.SESSION_NOT_RELIABLE, ex.getStatus());
		}
	}

	@Test
	public void sendBufferSizeExceeded() throws IOException, InterruptedException {

		BlockingSession session = new BlockingSession();
		session.setId("123");
		session.setOpen(true);

		final ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, 10*1000, 1024);

		sendBlockingMessage(decorator);

		StringBuilder sb = new StringBuilder();
		for (int i = 0 ; i < 1023; i++) {
			sb.append("a");
		}

		TextMessage message = new TextMessage(sb.toString());
		decorator.sendMessage(message);

		assertEquals(1023, decorator.getBufferSize());
		assertTrue(session.isOpen());

		try {
			decorator.sendMessage(message);
			fail("Expected exception");
		}
		catch (SessionLimitExceededException ex) {
			String actual = ex.getMessage();
			String regex = "Buffer size [\\d]+ bytes for session '123' exceeds the allowed limit 1024";
			assertTrue("Unexpected message: " + actual, actual.matches(regex));
			assertEquals(CloseStatus.SESSION_NOT_RELIABLE, ex.getStatus());
		}
	}

	@Test // SPR-17140
	public void overflowStrategyDrop() throws IOException, InterruptedException {

		BlockingSession session = new BlockingSession();
		session.setId("123");
		session.setOpen(true);

		final ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, 10*1000, 1024, OverflowStrategy.DROP);

		sendBlockingMessage(decorator);

		StringBuilder sb = new StringBuilder();
		for (int i = 0 ; i < 1023; i++) {
			sb.append("a");
		}

		for (int i=0; i < 5; i++) {
			TextMessage message = new TextMessage(sb.toString());
			decorator.sendMessage(message);
		}

		assertEquals(1023, decorator.getBufferSize());
		assertTrue(session.isOpen());

	}

	@Test
	public void closeStatusNormal() throws Exception {

		BlockingSession session = new BlockingSession();
		session.setOpen(true);
		WebSocketSession decorator = new ConcurrentWebSocketSessionDecorator(session, 10 * 1000, 1024);

		decorator.close(CloseStatus.PROTOCOL_ERROR);
		assertEquals(CloseStatus.PROTOCOL_ERROR, session.getCloseStatus());

		decorator.close(CloseStatus.SERVER_ERROR);
		assertEquals("Should have been ignored", CloseStatus.PROTOCOL_ERROR, session.getCloseStatus());
	}

	@Test
	public void closeStatusChangesToSessionNotReliable() throws Exception {

		BlockingSession session = new BlockingSession();
		session.setId("123");
		session.setOpen(true);
		CountDownLatch sentMessageLatch = session.getSentMessageLatch();

		int sendTimeLimit = 100;
		int bufferSizeLimit = 1024;

		final ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, sendTimeLimit, bufferSizeLimit);

		Executors.newSingleThreadExecutor().submit((Runnable) () -> {
			TextMessage message = new TextMessage("slow message");
			try {
				decorator.sendMessage(message);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		});

		assertTrue(sentMessageLatch.await(5, TimeUnit.SECONDS));

		// ensure some send time elapses
		Thread.sleep(sendTimeLimit + 100);

		decorator.close(CloseStatus.PROTOCOL_ERROR);

		assertEquals("CloseStatus should have changed to SESSION_NOT_RELIABLE",
				CloseStatus.SESSION_NOT_RELIABLE, session.getCloseStatus());
	}

	private void sendBlockingMessage(ConcurrentWebSocketSessionDecorator session) throws InterruptedException {
		Executors.newSingleThreadExecutor().submit(() -> {
			TextMessage message = new TextMessage("slow message");
			try {
				session.sendMessage(message);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		});
		BlockingSession delegate = (BlockingSession) session.getDelegate();
		assertTrue(delegate.getSentMessageLatch().await(5, TimeUnit.SECONDS));
	}



	private static class BlockingSession extends TestWebSocketSession {

		private AtomicReference<CountDownLatch> nextMessageLatch = new AtomicReference<>();

		private AtomicReference<CountDownLatch> releaseLatch = new AtomicReference<>();


		public CountDownLatch getSentMessageLatch() {
			this.nextMessageLatch.set(new CountDownLatch(1));
			return this.nextMessageLatch.get();
		}

		@Override
		public void sendMessage(WebSocketMessage<?> message) throws IOException {
			super.sendMessage(message);
			if (this.nextMessageLatch != null) {
				this.nextMessageLatch.get().countDown();
			}
			block();
		}

		private void block() {
			try {
				this.releaseLatch.set(new CountDownLatch(1));
				this.releaseLatch.get().await();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
