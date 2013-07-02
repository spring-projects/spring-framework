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

package org.springframework.messaging;

import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.support.MessageBuilder;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests for {@link PublishSubscribeChannel}.
 *
 * @author Phillip Webb
 */
public class PublishSubscibeChannelTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	private PublishSubscribeChannel channel = new PublishSubscribeChannel();

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
	public void messageMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Message must not be null");
		this.channel.send(null);
	}

	@Test
	public void payloadMustNotBeNull() throws Exception {
		Message<?> message = mock(Message.class);
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Message payload must not be null");
		this.channel.send(message);
	}

	@Test
	public void sendWithoutExecutor() {
		this.channel.subscribe(this.handler);
		this.channel.send(this.message);
		verify(this.handler).handleMessage(this.message);
	}

	@Test
	public void sendWithExecutor() throws Exception {
		Executor executor = mock(Executor.class);
		this.channel = new PublishSubscribeChannel(executor);
		this.channel.subscribe(this.handler);
		this.channel.send(this.message);
		verify(executor).execute(this.runnableCaptor.capture());
		verify(this.handler, never()).handleMessage(this.message);
		this.runnableCaptor.getValue().run();
		verify(this.handler).handleMessage(this.message);
	}

	@Test
	public void subscribeTwice() throws Exception {
		assertThat(this.channel.subscribe(this.handler), equalTo(true));
		assertThat(this.channel.subscribe(this.handler), equalTo(false));
		this.channel.send(this.message);
		verify(this.handler, times(1)).handleMessage(this.message);
	}

	@Test
	public void unsubscribeTwice() throws Exception {
		this.channel.subscribe(this.handler);
		assertThat(this.channel.unsubscribe(this.handler), equalTo(true));
		assertThat(this.channel.unsubscribe(this.handler), equalTo(false));
		this.channel.send(this.message);
		verify(this.handler, never()).handleMessage(this.message);
	}

	@Test
	public void failurePropagates() throws Exception {
		RuntimeException ex = new RuntimeException();
		willThrow(ex).given(this.handler).handleMessage(this.message);
		MessageHandler secondHandler = mock(MessageHandler.class);
		this.channel.subscribe(this.handler);
		this.channel.subscribe(secondHandler);
		try {
			this.channel.send(message);
		}
		catch(RuntimeException actualException) {
			assertThat(actualException, equalTo(ex));
		}
		verifyZeroInteractions(secondHandler);
	}

	@Test
	public void concurrentModification() throws Exception {
		this.channel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				channel.unsubscribe(handler);
			}
		});
		this.channel.subscribe(this.handler);
		this.channel.send(this.message);
		verify(this.handler).handleMessage(this.message);
	}

}
