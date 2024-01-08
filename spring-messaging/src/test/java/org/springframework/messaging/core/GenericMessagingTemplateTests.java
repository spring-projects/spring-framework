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

package org.springframework.messaging.core;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.StubMessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link GenericMessagingTemplate}.
 *
 * @author Rossen Stoyanchev
 * @author Gary Russell
 */
class GenericMessagingTemplateTests {

	private GenericMessagingTemplate template;

	private StubMessageChannel messageChannel;

	private ThreadPoolTaskExecutor executor;


	@BeforeEach
	void setup() {
		this.messageChannel = new StubMessageChannel();
		this.template = new GenericMessagingTemplate();
		this.template.setDefaultDestination(this.messageChannel);
		this.template.setDestinationResolver(new TestDestinationResolver());
		this.executor = new ThreadPoolTaskExecutor();
		this.executor.afterPropertiesSet();
	}

	@Test
	void sendWithTimeout() {
		SubscribableChannel channel = mock();
		final AtomicReference<Message<?>> sent = new AtomicReference<>();
		willAnswer(invocation -> {
			sent.set(invocation.getArgument(0));
			return true;
		}).given(channel).send(any(Message.class), eq(30_000L));
		Message<?> message = MessageBuilder.withPayload("request")
				.setHeader(GenericMessagingTemplate.DEFAULT_SEND_TIMEOUT_HEADER, 30_000L)
				.setHeader(GenericMessagingTemplate.DEFAULT_RECEIVE_TIMEOUT_HEADER, 1L)
				.build();
		this.template.send(channel, message);
		verify(channel).send(any(Message.class), eq(30_000L));
		assertThat(sent.get()).isNotNull();
		assertThat(sent.get().getHeaders().containsKey(GenericMessagingTemplate.DEFAULT_SEND_TIMEOUT_HEADER)).isFalse();
		assertThat(sent.get().getHeaders().containsKey(GenericMessagingTemplate.DEFAULT_RECEIVE_TIMEOUT_HEADER)).isFalse();
	}

	@Test
	void sendWithTimeoutMutable() {
		SubscribableChannel channel = mock();
		final AtomicReference<Message<?>> sent = new AtomicReference<>();
		willAnswer(invocation -> {
			sent.set(invocation.getArgument(0));
			return true;
		}).given(channel).send(any(Message.class), eq(30_000L));
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setLeaveMutable(true);
		Message<?> message = new GenericMessage<>("request", accessor.getMessageHeaders());
		accessor.setHeader(GenericMessagingTemplate.DEFAULT_SEND_TIMEOUT_HEADER, 30_000L);
		this.template.send(channel, message);
		verify(channel).send(any(Message.class), eq(30_000L));
		assertThat(sent.get()).isNotNull();
		assertThat(sent.get().getHeaders().containsKey(GenericMessagingTemplate.DEFAULT_SEND_TIMEOUT_HEADER)).isFalse();
		assertThat(sent.get().getHeaders().containsKey(GenericMessagingTemplate.DEFAULT_RECEIVE_TIMEOUT_HEADER)).isFalse();
	}

	@Test
	void sendAndReceive() {
		SubscribableChannel channel = new ExecutorSubscribableChannel(this.executor);
		channel.subscribe(message -> {
			MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
			replyChannel.send(new GenericMessage<>("response"));
		});

		String actual = this.template.convertSendAndReceive(channel, "request", String.class);
		assertThat(actual).isEqualTo("response");
	}

	@Test
	void sendAndReceiveTimeout() throws InterruptedException {
		final AtomicReference<Throwable> failure = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);

		this.template.setReceiveTimeout(1);
		this.template.setSendTimeout(30_000L);
		this.template.setThrowExceptionOnLateReply(true);

		SubscribableChannel channel = mock();
		MessageHandler handler = createLateReplier(latch, failure);
		willAnswer(invocation -> {
			this.executor.execute(() -> handler.handleMessage(invocation.getArgument(0)));
			return true;
		}).given(channel).send(any(Message.class), anyLong());

		assertThat(this.template.convertSendAndReceive(channel, "request", String.class)).isNull();
		assertThat(latch.await(10_000, TimeUnit.MILLISECONDS)).isTrue();

		Throwable ex = failure.get();
		if (ex != null) {
			throw new AssertionError(ex);
		}
		verify(channel).send(any(Message.class), eq(30_000L));
	}

	@Test
	void sendAndReceiveVariableTimeout() throws InterruptedException {
		final AtomicReference<Throwable> failure = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);

		this.template.setSendTimeout(20_000);
		this.template.setReceiveTimeout(10_000);
		this.template.setThrowExceptionOnLateReply(true);

		SubscribableChannel channel = mock();
		MessageHandler handler = createLateReplier(latch, failure);
		willAnswer(invocation -> {
			this.executor.execute(() -> handler.handleMessage(invocation.getArgument(0)));
			return true;
		}).given(channel).send(any(Message.class), anyLong());

		Message<?> message = MessageBuilder.withPayload("request")
				.setHeader(GenericMessagingTemplate.DEFAULT_SEND_TIMEOUT_HEADER, 30_000L)
				.setHeader(GenericMessagingTemplate.DEFAULT_RECEIVE_TIMEOUT_HEADER, 1L)
				.build();
		assertThat(this.template.sendAndReceive(channel, message)).isNull();
		assertThat(latch.await(10_000, TimeUnit.MILLISECONDS)).isTrue();

		Throwable ex = failure.get();
		if (ex != null) {
			throw new AssertionError(ex);
		}
		verify(channel).send(any(Message.class), eq(30_000L));
	}

	@Test
	void sendAndReceiveVariableTimeoutCustomHeaders() throws InterruptedException {
		final AtomicReference<Throwable> failure = new AtomicReference<>();
		final CountDownLatch latch = new CountDownLatch(1);

		this.template.setSendTimeout(20_000);
		this.template.setReceiveTimeout(10_000);
		this.template.setThrowExceptionOnLateReply(true);
		this.template.setSendTimeoutHeader("sto");
		this.template.setReceiveTimeoutHeader("rto");

		SubscribableChannel channel = mock();
		MessageHandler handler = createLateReplier(latch, failure);
		willAnswer(invocation -> {
			this.executor.execute(() -> handler.handleMessage(invocation.getArgument(0)));
			return true;
		}).given(channel).send(any(Message.class), anyLong());

		Message<?> message = MessageBuilder.withPayload("request")
				.setHeader("sto", 30_000L)
				.setHeader("rto", 1L)
				.build();
		assertThat(this.template.sendAndReceive(channel, message)).isNull();
		assertThat(latch.await(10_000, TimeUnit.MILLISECONDS)).isTrue();

		Throwable ex = failure.get();
		if (ex != null) {
			throw new AssertionError(ex);
		}
		verify(channel).send(any(Message.class), eq(30_000L));
	}

	private MessageHandler createLateReplier(final CountDownLatch latch, final AtomicReference<Throwable> failure) {
		return message -> {
			try {
				Thread.sleep(500);
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(new GenericMessage<>("response"));
				failure.set(new IllegalStateException("Expected exception"));
			}
			catch (InterruptedException e) {
				failure.set(e);
			}
			catch (MessageDeliveryException ex) {
				String expected = "Reply message received but the receiving thread has exited due to a timeout";
				String actual = ex.getMessage();
				if (!expected.equals(actual)) {
					failure.set(new IllegalStateException(
							"Unexpected error: '" + actual + "'"));
				}
			}
			finally {
				latch.countDown();
			}
		};
	}

	@Test
	void convertAndSendWithSimpMessageHeaders() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setHeader("key", "value");
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();

		this.template.convertAndSend("channel", "data", headers);
		List<Message<byte[]>> messages = this.messageChannel.getMessages();
		Message<byte[]> message = messages.get(0);

		assertThat(message.getHeaders()).isSameAs(headers);
		assertThat(accessor.isMutable()).isFalse();
	}


	private class TestDestinationResolver implements DestinationResolver<MessageChannel> {

		@Override
		public MessageChannel resolveDestination(String name) throws DestinationResolutionException {
			return messageChannel;
		}
	}

}
