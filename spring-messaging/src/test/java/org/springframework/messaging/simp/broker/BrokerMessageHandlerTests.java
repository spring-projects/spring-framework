/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

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
		assertThat(this.handler.availabilityEvents).isEqualTo(Collections.singletonList(true));
	}

	@Test
	public void publishBrokerAvailableEventWhenAlreadyAvailable() {
		this.handler.publishBrokerAvailableEvent();
		this.handler.publishBrokerAvailableEvent();

		assertThat(this.handler.availabilityEvents).isEqualTo(Collections.singletonList(true));
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

	@Test
	public void checkDestination() {
		TestBrokerMessageHandler theHandler = new TestBrokerMessageHandler("/topic");
		theHandler.start();

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		accessor.setLeaveMutable(true);

		accessor.setDestination("/topic/foo");
		theHandler.handleMessage(MessageBuilder.createMessage("p", accessor.toMessageHeaders()));

		accessor.setDestination("/app/foo");
		theHandler.handleMessage(MessageBuilder.createMessage("p", accessor.toMessageHeaders()));

		accessor.setDestination(null);
		theHandler.handleMessage(MessageBuilder.createMessage("p", accessor.toMessageHeaders()));

		List<Message<?>> list = theHandler.messages;
		assertThat(list).hasSize(2);
		assertThat(list.get(0).getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER)).isEqualTo("/topic/foo");
		assertThat(list.get(1).getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER)).isNull();
	}

	@Test
	public void checkDestinationWithoutConfiguredPrefixes() {
		this.handler.setUserDestinationPredicate(destination -> destination.startsWith("/user/"));
		this.handler.start();

		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		accessor.setLeaveMutable(true);

		accessor.setDestination("/user/1/foo");
		this.handler.handleMessage(MessageBuilder.createMessage("p", accessor.toMessageHeaders()));

		accessor.setDestination("/foo");
		this.handler.handleMessage(MessageBuilder.createMessage("p", accessor.toMessageHeaders()));

		List<Message<?>> list = this.handler.messages;
		assertThat(list).hasSize(1);
		assertThat(list.get(0).getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER)).isEqualTo("/foo");
	}

	private static class TestBrokerMessageHandler extends AbstractBrokerMessageHandler
			implements ApplicationEventPublisher {

		private final List<Message<?>> messages = new ArrayList<>();

		private final List<Boolean> availabilityEvents = new ArrayList<>();


		TestBrokerMessageHandler(String... destinationPrefixes) {
			super(mock(SubscribableChannel.class), mock(MessageChannel.class),
					mock(SubscribableChannel.class), Arrays.asList(destinationPrefixes));

			setApplicationEventPublisher(this);
		}

		@Override
		protected void handleMessageInternal(Message<?> message) {
			String destination = (String) message.getHeaders().get(SimpMessageHeaderAccessor.DESTINATION_HEADER);
			if (checkDestinationPrefix(destination)) {
				this.messages.add(message);
			}
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
