/*
 * Copyright 2002-2016 the original author or authors.
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

import static org.junit.Assert.*;

/**
 * Unit tests for
 * {@link org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator}.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("resource")
public class ConcurrentWebSocketSessionDecoratorTests {


	@Test
	public void send() throws IOException {

		TestWebSocketSession session = new TestWebSocketSession();
		session.setOpen(true);

		ConcurrentWebSocketSessionDecorator concurrentSession =
				new ConcurrentWebSocketSessionDecorator(session, 1000, 1024);

		TextMessage textMessage = new TextMessage("payload");
		concurrentSession.sendMessage(textMessage);

		assertEquals(1, session.getSentMessages().size());
		assertEquals(textMessage, session.getSentMessages().get(0));

		assertEquals(0, concurrentSession.getBufferSize());
		assertEquals(0, concurrentSession.getTimeSinceSendStarted());
		assertTrue(session.isOpen());
	}

	@Test
	public void sendAfterBlockedSend() throws IOException, InterruptedException {

		BlockingSession blockingSession = new BlockingSession();
		blockingSession.setOpen(true);
		CountDownLatch sentMessageLatch = blockingSession.getSentMessageLatch();

		final ConcurrentWebSocketSessionDecorator concurrentSession =
				new ConcurrentWebSocketSessionDecorator(blockingSession, 10 * 1000, 1024);

		Executors.newSingleThreadExecutor().submit((Runnable) () -> {
			TextMessage message = new TextMessage("slow message");
			try {
				concurrentSession.sendMessage(message);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		});

		assertTrue(sentMessageLatch.await(5, TimeUnit.SECONDS));

		// ensure some send time elapses
		Thread.sleep(100);
		assertTrue(concurrentSession.getTimeSinceSendStarted() > 0);

		TextMessage payload = new TextMessage("payload");
		for (int i=0; i < 5; i++) {
			concurrentSession.sendMessage(payload);
		}

		assertTrue(concurrentSession.getTimeSinceSendStarted() > 0);
		assertEquals(5 * payload.getPayloadLength(), concurrentSession.getBufferSize());
		assertTrue(blockingSession.isOpen());
	}

	@Test
	public void sendTimeLimitExceeded() throws IOException, InterruptedException {

		BlockingSession blockingSession = new BlockingSession();
		blockingSession.setId("123");
		blockingSession.setOpen(true);
		CountDownLatch sentMessageLatch = blockingSession.getSentMessageLatch();

		int sendTimeLimit = 100;
		int bufferSizeLimit = 1024;

		final ConcurrentWebSocketSessionDecorator concurrentSession =
				new ConcurrentWebSocketSessionDecorator(blockingSession, sendTimeLimit, bufferSizeLimit);

		Executors.newSingleThreadExecutor().submit((Runnable) () -> {
			TextMessage message = new TextMessage("slow message");
			try {
				concurrentSession.sendMessage(message);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		});

		assertTrue(sentMessageLatch.await(5, TimeUnit.SECONDS));

		// ensure some send time elapses
		Thread.sleep(sendTimeLimit + 100);

		try {
			TextMessage payload = new TextMessage("payload");
			concurrentSession.sendMessage(payload);
			fail("Expected exception");
		}
		catch (SessionLimitExceededException ex) {
			String actual = ex.getMessage();
			String regex = "Message send time [\\d]+ \\(ms\\) for session '123' exceeded the allowed limit 100";
			assertTrue("Unexpected message: " + actual, actual.matches(regex));
			assertEquals(CloseStatus.SESSION_NOT_RELIABLE, ex.getStatus());
		}
	}

	@Test
	public void sendBufferSizeExceeded() throws IOException, InterruptedException {

		BlockingSession blockingSession = new BlockingSession();
		blockingSession.setId("123");
		blockingSession.setOpen(true);
		CountDownLatch sentMessageLatch = blockingSession.getSentMessageLatch();

		int sendTimeLimit = 10 * 1000;
		int bufferSizeLimit = 1024;

		final ConcurrentWebSocketSessionDecorator concurrentSession =
				new ConcurrentWebSocketSessionDecorator(blockingSession, sendTimeLimit, bufferSizeLimit);

		Executors.newSingleThreadExecutor().submit((Runnable) () -> {
			TextMessage message = new TextMessage("slow message");
			try {
				concurrentSession.sendMessage(message);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		});

		assertTrue(sentMessageLatch.await(5, TimeUnit.SECONDS));

		StringBuilder sb = new StringBuilder();
		for (int i=0 ; i < 1023; i++) {
			sb.append("a");
		}

		TextMessage message = new TextMessage(sb.toString());
		concurrentSession.sendMessage(message);

		assertEquals(1023, concurrentSession.getBufferSize());
		assertTrue(blockingSession.isOpen());

		try {
			concurrentSession.sendMessage(message);
			fail("Expected exception");
		}
		catch (SessionLimitExceededException ex) {
			String actual = ex.getMessage();
			String regex = "The send buffer size [\\d]+ bytes for session '123' exceeded the allowed limit 1024";
			assertTrue("Unexpected message: " + actual, actual.matches(regex));
			assertEquals(CloseStatus.SESSION_NOT_RELIABLE, ex.getStatus());
		}
	}

	@Test
	public void closeStatusNormal() throws Exception {

		BlockingSession delegate = new BlockingSession();
		delegate.setOpen(true);
		WebSocketSession decorator = new ConcurrentWebSocketSessionDecorator(delegate, 10 * 1000, 1024);

		decorator.close(CloseStatus.PROTOCOL_ERROR);
		assertEquals(CloseStatus.PROTOCOL_ERROR, delegate.getCloseStatus());

		decorator.close(CloseStatus.SERVER_ERROR);
		assertEquals("Should have been ignored", CloseStatus.PROTOCOL_ERROR, delegate.getCloseStatus());
	}

	@Test
	public void closeStatusChangesToSessionNotReliable() throws Exception {

		BlockingSession blockingSession = new BlockingSession();
		blockingSession.setId("123");
		blockingSession.setOpen(true);
		CountDownLatch sentMessageLatch = blockingSession.getSentMessageLatch();

		int sendTimeLimit = 100;
		int bufferSizeLimit = 1024;

		final ConcurrentWebSocketSessionDecorator concurrentSession =
				new ConcurrentWebSocketSessionDecorator(blockingSession, sendTimeLimit, bufferSizeLimit);

		Executors.newSingleThreadExecutor().submit((Runnable) () -> {
			TextMessage message = new TextMessage("slow message");
			try {
				concurrentSession.sendMessage(message);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		});

		assertTrue(sentMessageLatch.await(5, TimeUnit.SECONDS));

		// ensure some send time elapses
		Thread.sleep(sendTimeLimit + 100);

		concurrentSession.close(CloseStatus.PROTOCOL_ERROR);

		assertEquals("CloseStatus should have changed to SESSION_NOT_RELIABLE",
				CloseStatus.SESSION_NOT_RELIABLE, blockingSession.getCloseStatus());
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
