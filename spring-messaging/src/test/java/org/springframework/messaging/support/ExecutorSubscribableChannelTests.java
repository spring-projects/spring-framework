/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.messaging.support;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link ExecutorSubscribableChannel}.
 *
 * @author Phillip Webb
 */
public class ExecutorSubscribableChannelTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel();

	@Mock
	private MessageHandler handler;

	private final Object payload = new Object();

	private final Message<Object> message = MessageBuilder.withPayload(this.payload).build();

	@Captor
	private ArgumentCaptor<Runnable> runnableCaptor;


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}


	@Test
	public void messageMustNotBeNull() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Message must not be null");
		this.channel.send(null);
	}

	@Test
	public void sendWithoutExecutor() {
		BeforeHandleInterceptor interceptor = new BeforeHandleInterceptor();
		this.channel.addInterceptor(interceptor);
		this.channel.subscribe(this.handler);
		this.channel.send(this.message);
		verify(this.handler).handleMessage(this.message);
		assertEquals(1, interceptor.getCounter().get());
		assertTrue(interceptor.wasAfterHandledInvoked());
	}

	@Test
	public void sendWithExecutor() {
		BeforeHandleInterceptor interceptor = new BeforeHandleInterceptor();
		TaskExecutor executor = mock(TaskExecutor.class);
		ExecutorSubscribableChannel testChannel = new ExecutorSubscribableChannel(executor);
		testChannel.addInterceptor(interceptor);
		testChannel.subscribe(this.handler);
		testChannel.send(this.message);
		verify(executor).execute(this.runnableCaptor.capture());
		verify(this.handler, never()).handleMessage(this.message);
		this.runnableCaptor.getValue().run();
		verify(this.handler).handleMessage(this.message);
		assertEquals(1, interceptor.getCounter().get());
		assertTrue(interceptor.wasAfterHandledInvoked());
	}

	@Test
	public void subscribeTwice()  {
		assertThat(this.channel.subscribe(this.handler), equalTo(true));
		assertThat(this.channel.subscribe(this.handler), equalTo(false));
		this.channel.send(this.message);
		verify(this.handler, times(1)).handleMessage(this.message);
	}

	@Test
	public void unsubscribeTwice()  {
		this.channel.subscribe(this.handler);
		assertThat(this.channel.unsubscribe(this.handler), equalTo(true));
		assertThat(this.channel.unsubscribe(this.handler), equalTo(false));
		this.channel.send(this.message);
		verify(this.handler, never()).handleMessage(this.message);
	}

	@Test
	public void failurePropagates()  {
		RuntimeException ex = new RuntimeException();
		willThrow(ex).given(this.handler).handleMessage(this.message);
		MessageHandler secondHandler = mock(MessageHandler.class);
		this.channel.subscribe(this.handler);
		this.channel.subscribe(secondHandler);
		try {
			this.channel.send(message);
		}
		catch (MessageDeliveryException actualException) {
			assertThat(actualException.getCause(), equalTo(ex));
		}
		verifyZeroInteractions(secondHandler);
	}

	@Test
	public void concurrentModification()  {
		this.channel.subscribe(message1 -> channel.unsubscribe(handler));
		this.channel.subscribe(this.handler);
		this.channel.send(this.message);
		verify(this.handler).handleMessage(this.message);
	}

	@Test
	public void interceptorWithModifiedMessage() {
		Message<?> expected = mock(Message.class);
		BeforeHandleInterceptor interceptor = new BeforeHandleInterceptor();
		interceptor.setMessageToReturn(expected);
		this.channel.addInterceptor(interceptor);
		this.channel.subscribe(this.handler);
		this.channel.send(this.message);
		verify(this.handler).handleMessage(expected);
		assertEquals(1, interceptor.getCounter().get());
		assertTrue(interceptor.wasAfterHandledInvoked());
	}

	@Test
	public void interceptorWithNull() {
		BeforeHandleInterceptor interceptor1 = new BeforeHandleInterceptor();
		NullReturningBeforeHandleInterceptor interceptor2 = new NullReturningBeforeHandleInterceptor();
		this.channel.addInterceptor(interceptor1);
		this.channel.addInterceptor(interceptor2);
		this.channel.subscribe(this.handler);
		this.channel.send(this.message);
		verifyNoMoreInteractions(this.handler);
		assertEquals(1, interceptor1.getCounter().get());
		assertEquals(1, interceptor2.getCounter().get());
		assertTrue(interceptor1.wasAfterHandledInvoked());
	}

	@Test
	public void interceptorWithException() {
		IllegalStateException expected = new IllegalStateException("Fake exception");
		willThrow(expected).given(this.handler).handleMessage(this.message);
		BeforeHandleInterceptor interceptor = new BeforeHandleInterceptor();
		this.channel.addInterceptor(interceptor);
		this.channel.subscribe(this.handler);
		try {
			this.channel.send(this.message);
		}
		catch (MessageDeliveryException actual) {
			assertSame(expected, actual.getCause());
		}
		verify(this.handler).handleMessage(this.message);
		assertEquals(1, interceptor.getCounter().get());
		assertTrue(interceptor.wasAfterHandledInvoked());
	}


	private abstract static class AbstractTestInterceptor implements ChannelInterceptor, ExecutorChannelInterceptor {

		private AtomicInteger counter = new AtomicInteger();

		private volatile boolean afterHandledInvoked;

		public AtomicInteger getCounter() {
			return this.counter;
		}

		public boolean wasAfterHandledInvoked() {
			return this.afterHandledInvoked;
		}

		@Override
		public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
			assertNotNull(message);
			counter.incrementAndGet();
			return message;
		}

		@Override
		public void afterMessageHandled(
				Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {

			this.afterHandledInvoked = true;
		}
	}


	private static class BeforeHandleInterceptor extends AbstractTestInterceptor {

		private Message<?> messageToReturn;

		private RuntimeException exceptionToRaise;

		public void setMessageToReturn(Message<?> messageToReturn) {
			this.messageToReturn = messageToReturn;
		}

		// TODO Determine why setExceptionToRaise() is unused.
		@SuppressWarnings("unused")
		public void setExceptionToRaise(RuntimeException exception) {
			this.exceptionToRaise = exception;
		}

		@Override
		public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
			super.beforeHandle(message, channel, handler);
			if (this.exceptionToRaise != null) {
				throw this.exceptionToRaise;
			}
			return (this.messageToReturn != null ? this.messageToReturn : message);
		}
	}


	private static class NullReturningBeforeHandleInterceptor extends AbstractTestInterceptor {

		@Override
		public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
			super.beforeHandle(message, channel, handler);
			return null;
		}
	}

}
