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

package org.springframework.messaging.support;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Unit tests for {@link ExecutorSubscribableChannel}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
public class ExecutorSubscribableChannelTests {

	private ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel();

	@Mock
	private MessageHandler handler;

	@Captor
	private ArgumentCaptor<Runnable> runnableCaptor;

	private final Object payload = new Object();

	private final Message<Object> message = MessageBuilder.withPayload(this.payload).build();


	@Test
	public void messageMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.channel.send(null))
			.withMessageContaining("Message must not be null");
	}

	@Test
	public void sendWithoutExecutor() {
		BeforeHandleInterceptor interceptor = new BeforeHandleInterceptor();
		this.channel.addInterceptor(interceptor);
		this.channel.subscribe(this.handler);
		this.channel.send(this.message);
		verify(this.handler).handleMessage(this.message);
		assertThat(interceptor.getCounter().get()).isEqualTo(1);
		assertThat(interceptor.wasAfterHandledInvoked()).isTrue();
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
		assertThat(interceptor.getCounter().get()).isEqualTo(1);
		assertThat(interceptor.wasAfterHandledInvoked()).isTrue();
	}

	@Test
	public void subscribeTwice()  {
		assertThat(this.channel.subscribe(this.handler)).isEqualTo(true);
		assertThat(this.channel.subscribe(this.handler)).isEqualTo(false);
		this.channel.send(this.message);
		verify(this.handler, times(1)).handleMessage(this.message);
	}

	@Test
	public void unsubscribeTwice()  {
		this.channel.subscribe(this.handler);
		assertThat(this.channel.unsubscribe(this.handler)).isEqualTo(true);
		assertThat(this.channel.unsubscribe(this.handler)).isEqualTo(false);
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
			assertThat(actualException.getCause()).isEqualTo(ex);
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
		assertThat(interceptor.getCounter().get()).isEqualTo(1);
		assertThat(interceptor.wasAfterHandledInvoked()).isTrue();
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
		assertThat(interceptor1.getCounter().get()).isEqualTo(1);
		assertThat(interceptor2.getCounter().get()).isEqualTo(1);
		assertThat(interceptor1.wasAfterHandledInvoked()).isTrue();
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
			assertThat(actual.getCause()).isSameAs(expected);
		}
		verify(this.handler).handleMessage(this.message);
		assertThat(interceptor.getCounter().get()).isEqualTo(1);
		assertThat(interceptor.wasAfterHandledInvoked()).isTrue();
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
			assertThat(message).isNotNull();
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
