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

package org.springframework.messaging.simp.broker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class BrokerMessageHandlerTests {

	private final TestBrokerMessageHandler handler = new TestBrokerMessageHandler();


	@Test
	public void startShouldUpdateIsRunning() {
		assertThat(this.handler.isRunning()).isFalse();
		this.handler.start();
		assertThat(this.handler.isRunning()).isTrue();
	}

	@Test
	public void stopShouldUpdateIsRunning() {
		this.handler.start();
		assertThat(this.handler.isRunning()).isTrue();

		this.handler.stop();
		assertThat(this.handler.isRunning()).isFalse();
	}

	@Test
	public void startAndStopShouldNotPublishBrokerAvailabilityEvents() {
		this.handler.start();
		this.handler.stop();
		assertThat(this.handler.availabilityEvents).isEqualTo(Collections.emptyList());
	}

	@Test
	public void handleMessageWhenBrokerNotRunning() {
		this.handler.handleMessage(new GenericMessage<Object>("payload"));
		assertThat(this.handler.messages).isEqualTo(Collections.emptyList());
	}

	@Test
	public void publishBrokerAvailableEvent() {

		assertThat(this.handler.isBrokerAvailable()).isFalse();
		assertThat(this.handler.availabilityEvents).isEqualTo(Collections.emptyList());

		this.handler.publishBrokerAvailableEvent();

		assertThat(this.handler.isBrokerAvailable()).isTrue();
		assertThat(this.handler.availabilityEvents).isEqualTo(Arrays.asList(true));
	}

	@Test
	public void publishBrokerAvailableEventWhenAlreadyAvailable() {

		this.handler.publishBrokerAvailableEvent();
		this.handler.publishBrokerAvailableEvent();

		assertThat(this.handler.availabilityEvents).isEqualTo(Arrays.asList(true));
	}

	@Test
	public void publishBrokerUnavailableEvent() {

		this.handler.publishBrokerAvailableEvent();
		assertThat(this.handler.isBrokerAvailable()).isTrue();

		this.handler.publishBrokerUnavailableEvent();
		assertThat(this.handler.isBrokerAvailable()).isFalse();

		assertThat(this.handler.availabilityEvents).isEqualTo(Arrays.asList(true, false));
	}

	@Test
	public void publishBrokerUnavailableEventWhenAlreadyUnavailable() {

		this.handler.publishBrokerAvailableEvent();
		this.handler.publishBrokerUnavailableEvent();
		this.handler.publishBrokerUnavailableEvent();

		assertThat(this.handler.availabilityEvents).isEqualTo(Arrays.asList(true, false));
	}


	private static class TestBrokerMessageHandler extends AbstractBrokerMessageHandler
			implements ApplicationEventPublisher {

		private final List<Message<?>> messages = new ArrayList<>();

		private final List<Boolean> availabilityEvents = new ArrayList<>();


		private TestBrokerMessageHandler() {
			super(mock(SubscribableChannel.class), mock(MessageChannel.class), mock(SubscribableChannel.class));
			setApplicationEventPublisher(this);
		}

		@Override
		protected void handleMessageInternal(Message<?> message) {
			this.messages.add(message);
		}

		@Override
		public void publishEvent(ApplicationEvent event) {
			publishEvent((Object) event);
		}

		@Override
		public void publishEvent(Object event) {
			if (event instanceof BrokerAvailabilityEvent) {
				this.availabilityEvents.add(((BrokerAvailabilityEvent) event).isBrokerAvailable());
			}
		}
	}

}
