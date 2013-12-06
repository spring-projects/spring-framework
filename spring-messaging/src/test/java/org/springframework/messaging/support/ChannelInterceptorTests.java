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

package org.springframework.messaging.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

import static org.junit.Assert.*;

/**
 * Test fixture for the use of {@link ChannelInterceptor}s.
 * @author Rossen Stoyanchev
 */
public class ChannelInterceptorTests {

	private ExecutorSubscribableChannel channel;

	private TestMessageHandler messageHandler;


	@Before
	public void setup() {
		this.channel = new ExecutorSubscribableChannel();
		this.messageHandler = new TestMessageHandler();
		this.channel.subscribe(this.messageHandler);
	}


	@Test
	public void preSendInterceptorReturningModifiedMessage() {

		this.channel.addInterceptor(new PreSendReturnsMessageInterceptor());
		this.channel.send(MessageBuilder.withPayload("test").build());

		assertEquals(1, this.messageHandler.messages.size());
		Message<?> result = this.messageHandler.messages.get(0);

		assertNotNull(result);
		assertEquals("test", result.getPayload());
		assertEquals(1, result.getHeaders().get(PreSendReturnsMessageInterceptor.class.getSimpleName()));
	}

	@Test
	public void preSendInterceptorReturningNull() {

		PreSendReturnsNullInterceptor interceptor = new PreSendReturnsNullInterceptor();
		this.channel.addInterceptor(interceptor);
		Message<?> message = MessageBuilder.withPayload("test").build();
		this.channel.send(message);

		assertEquals(1, interceptor.counter.get());
		assertEquals(0, this.messageHandler.messages.size());
	}

	@Test
	public void postSendInterceptorMessageWasSent() {
		final AtomicBoolean invoked = new AtomicBoolean(false);
		this.channel.addInterceptor(new ChannelInterceptorAdapter() {
			@Override
			public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
				assertNotNull(message);
				assertNotNull(channel);
				assertSame(ChannelInterceptorTests.this.channel, channel);
				assertTrue(sent);
				invoked.set(true);
			}
		});
		this.channel.send(MessageBuilder.withPayload("test").build());
		assertTrue(invoked.get());
	}

	@Test
	public void postSendInterceptorMessageWasNotSent() {
		final AbstractMessageChannel testChannel = new AbstractMessageChannel() {
			@Override
			protected boolean sendInternal(Message<?> message, long timeout) {
				return false;
			}
		};
		final AtomicBoolean invoked = new AtomicBoolean(false);
		testChannel.addInterceptor(new ChannelInterceptorAdapter() {
			@Override
			public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
				assertNotNull(message);
				assertNotNull(channel);
				assertSame(testChannel, channel);
				assertFalse(sent);
				invoked.set(true);
			}
		});
		testChannel.send(MessageBuilder.withPayload("test").build());
		assertTrue(invoked.get());
	}


	private static class TestMessageHandler implements MessageHandler {

		private List<Message<?>> messages = new ArrayList<Message<?>>();

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			this.messages.add(message);
		}
	}

	private static class PreSendReturnsMessageInterceptor extends ChannelInterceptorAdapter {

		private AtomicInteger counter = new AtomicInteger();

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			assertNotNull(message);
			return MessageBuilder.fromMessage(message).setHeader(
					this.getClass().getSimpleName(), counter.incrementAndGet()).build();
		}
	}

	private static class PreSendReturnsNullInterceptor extends ChannelInterceptorAdapter {

		private AtomicInteger counter = new AtomicInteger();

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			assertNotNull(message);
			counter.incrementAndGet();
			return null;
		}
	}

}
