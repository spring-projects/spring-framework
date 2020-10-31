/*
 * Copyright 2002-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator.OverflowStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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

		assertThat(session.getSentMessages().size()).isEqualTo(1);
		assertThat(session.getSentMessages().get(0)).isEqualTo(textMessage);

		assertThat(decorator.getBufferSize()).isEqualTo(0);
		assertThat(decorator.getTimeSinceSendStarted()).isEqualTo(0);
		assertThat(session.isOpen()).isTrue();
	}

	@Test
	public void sendAfterBlockedSend() throws IOException, InterruptedException {

		BlockingWebSocketSession session = new BlockingWebSocketSession();
		session.setOpen(true);

		ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, 10 * 1000, 1024);

		sendBlockingMessage(decorator);

		Thread.sleep(50);
		assertThat(decorator.getTimeSinceSendStarted() > 0).isTrue();

		TextMessage payload = new TextMessage("payload");
		for (int i = 0; i < 5; i++) {
			decorator.sendMessage(payload);
		}

		assertThat(decorator.getTimeSinceSendStarted() > 0).isTrue();
		assertThat(decorator.getBufferSize()).isEqualTo((5 * payload.getPayloadLength()));
		assertThat(session.isOpen()).isTrue();
	}

	@Test
	public void sendTimeLimitExceeded() throws InterruptedException {

		BlockingWebSocketSession session = new BlockingWebSocketSession();
		session.setId("123");
		session.setOpen(true);

		ConcurrentWebSocketSessionDecorator decorator =
				new ConcurrentWebSocketSessionDecorator(session, 100, 1024);

		sendBlockingMessage(decorator);

		// Exceed send time..
		Thread.sleep(200);

		TextMessage payload = new TextMessage("payload");
		assertThatExceptionOfType(SessionLimitExceededException.class).isThrownBy(() ->
				decorator.sendMessage(payload))
			.withMessageMatching("Send time [\\d]+ \\(ms\\) for session '123' exceeded the allowed limit 100")
			.satisfies(ex -> assertThat(ex.getStatus()).isEqualTo(CloseStatus.SESSION_NOT_RELIABLE));
	}

	@Test
	public void sendBufferSizeExceeded() throws IOException, InterruptedException {

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
	public void closeStatusNormal() throws Exception {

		BlockingWebSocketSession session = new BlockingWebSocketSession();
		session.setOpen(true);
		WebSocketSession decorator = new ConcurrentWebSocketSessionDecorator(session, 10 * 1000, 1024);

		decorator.close(CloseStatus.PROTOCOL_ERROR);
		assertThat(session.getCloseStatus()).isEqualTo(CloseStatus.PROTOCOL_ERROR);

		decorator.close(CloseStatus.SERVER_ERROR);
		assertThat(session.getCloseStatus()).as("Should have been ignored").isEqualTo(CloseStatus.PROTOCOL_ERROR);
	}

	@Test
	public void closeStatusChangesToSessionNotReliable() throws Exception {

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

}
