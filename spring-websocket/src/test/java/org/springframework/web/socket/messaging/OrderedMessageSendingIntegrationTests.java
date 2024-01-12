/*
 * Copyright 2002-2024 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.broker.OrderedMessageChannelDecorator;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BlockingWebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to publish messages to an Executor backed channel wrapped with
 * {@link OrderedMessageChannelDecorator} and handled by
 * {@link StompSubProtocolHandler} delegating to a
 * {@link ConcurrentWebSocketSessionDecorator} wrapped session.
 *
 * <p>The tests verify that:
 * <ul>
 * <li>messages are executed in the same order as they are published.
 * <li>send buffer size and send time limits at the
 * {@link ConcurrentWebSocketSessionDecorator} level are enforced.
 * </ul>
 *
 * <p>The key is for {@link OrderedMessageChannelDecorator} to release the next
 * message when after the current one is queued for sending, and not after it is
 * sent, which may block and cause messages to accumulate in the
 * {@link OrderedMessageChannelDecorator} instead of in
 * {@link ConcurrentWebSocketSessionDecorator} where send limits are enforced.
 *
 * @author Rossen Stoyanchev
 */
class OrderedMessageSendingIntegrationTests {

	private static final Log logger = LogFactory.getLog(OrderedMessageSendingIntegrationTests.class);

	private static final int MESSAGE_SIZE = new StompEncoder().encode(createMessage(0)).length;


	private BlockingWebSocketSession blockingSession;

	private ExecutorSubscribableChannel clientOutChannel;

	private OrderedMessageChannelDecorator orderedClientOutChannel;

	private ThreadPoolTaskExecutor executor;



	@BeforeEach
	void setup() {
		this.blockingSession = new BlockingWebSocketSession();
		this.blockingSession.setId("1");
		this.blockingSession.setOpen(true);

		this.executor = new ThreadPoolTaskExecutor();
		this.executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
		this.executor.setAllowCoreThreadTimeOut(true);
		this.executor.afterPropertiesSet();

		this.clientOutChannel = new ExecutorSubscribableChannel(this.executor);
		OrderedMessageChannelDecorator.configureInterceptor(this.clientOutChannel, true);

		this.orderedClientOutChannel = new OrderedMessageChannelDecorator(this.clientOutChannel, logger);
	}

	@AfterEach
	void tearDown() {
		this.executor.shutdown();
	}

	@Test
	void sendAfterBlockedSend() throws InterruptedException {

		int messageCount = 1000;

		ConcurrentWebSocketSessionDecorator concurrentSessionDecorator =
				new ConcurrentWebSocketSessionDecorator(
						this.blockingSession, 60 * 1000, messageCount * MESSAGE_SIZE);

		TestMessageHandler handler = new TestMessageHandler(concurrentSessionDecorator);
		this.clientOutChannel.subscribe(handler);

		List<Message<?>> expectedMessages = new ArrayList<>(messageCount);

		// Send one to block
		Message<byte[]> message = createMessage(0);
		expectedMessages.add(message);
		this.orderedClientOutChannel.send(message);

		CountDownLatch latch = new CountDownLatch(messageCount);
		handler.setMessageLatch(latch);

		for (int i = 1; i <= messageCount; i++) {
			message = createMessage(i);
			expectedMessages.add(message);
			this.orderedClientOutChannel.send(message);
		}

		assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

		assertThat(concurrentSessionDecorator.getTimeSinceSendStarted()).isGreaterThan(0);
		assertThat(concurrentSessionDecorator.getBufferSize()).isEqualTo((messageCount * MESSAGE_SIZE));
		assertThat(handler.getSavedMessages()).containsExactlyElementsOf(expectedMessages);
		assertThat(blockingSession.isOpen()).isTrue();
	}

	@Test
	void exceedTimeLimit() throws InterruptedException {

		ConcurrentWebSocketSessionDecorator concurrentSessionDecorator =
				new ConcurrentWebSocketSessionDecorator(this.blockingSession, 100, 1024);

		TestMessageHandler messageHandler = new TestMessageHandler(concurrentSessionDecorator);
		this.clientOutChannel.subscribe(messageHandler);

		// Send one to block
		this.orderedClientOutChannel.send(createMessage(0));

		// Exceed send time
		Thread.sleep(200);

		CountDownLatch messageLatch = new CountDownLatch(1);
		messageHandler.setMessageLatch(messageLatch);

		// Send one more
		this.orderedClientOutChannel.send(createMessage(1));

		assertThat(messageLatch.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(messageHandler.getSavedException()).hasMessageMatching(
				"Send time [\\d]+ \\(ms\\) for session '1' exceeded the allowed limit 100");
	}

	@Test
	void exceedBufferSizeLimit() throws InterruptedException {

		ConcurrentWebSocketSessionDecorator concurrentSessionDecorator =
				new ConcurrentWebSocketSessionDecorator(this.blockingSession, 60 * 1000, 2 * MESSAGE_SIZE);

		TestMessageHandler messageHandler = new TestMessageHandler(concurrentSessionDecorator);
		this.clientOutChannel.subscribe(messageHandler);

		// Send one to block
		this.orderedClientOutChannel.send(createMessage(0));

		int messageCount = 3;
		CountDownLatch messageLatch = new CountDownLatch(messageCount);
		messageHandler.setMessageLatch(messageLatch);

		for (int i = 1; i <= messageCount; i++) {
			this.orderedClientOutChannel.send(createMessage(i));
		}

		assertThat(messageLatch.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(messageHandler.getSavedException()).hasMessage(
				"Buffer size " + 3 * MESSAGE_SIZE + " bytes for session '1' " +
						"exceeds the allowed limit " + 2 * MESSAGE_SIZE);
	}

	private static Message<byte[]> createMessage(int index) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
		accessor.setHeader("index", index);
		accessor.setSubscriptionId("1");
		accessor.setLeaveMutable(true);
		byte[] bytes = "payload".getBytes(StandardCharsets.UTF_8);
		return MessageBuilder.createMessage(bytes, accessor.getMessageHeaders());

	}


	private static class TestMessageHandler implements MessageHandler {

		private final StompSubProtocolHandler subProtocolHandler = new StompSubProtocolHandler();

		private final WebSocketSession session;

		@Nullable
		private CountDownLatch messageLatch;

		private final Queue<Message<?>> messages = new LinkedBlockingQueue<>();

		private final AtomicReference<Exception> exception = new AtomicReference<>();


		public TestMessageHandler(WebSocketSession session) {
			this.session = session;
		}

		public void setMessageLatch(CountDownLatch latch) {
			this.messageLatch = latch;
		}

		public Collection<Message<?>> getSavedMessages() {
			return this.messages;
		}

		public Exception getSavedException() {
			return this.exception.get();
		}

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			this.messages.add(message);
			try {
				this.subProtocolHandler.handleMessageToClient(this.session, message);
			}
			catch (Exception ex) {
				this.exception.set(ex);
			}
			if (this.messageLatch != null) {
				this.messageLatch.countDown();
			}
		}
	}
}
