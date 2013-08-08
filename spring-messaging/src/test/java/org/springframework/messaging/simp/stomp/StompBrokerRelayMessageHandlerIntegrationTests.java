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

package org.springframework.messaging.simp.stomp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.channel.ExecutorSubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.SocketUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Integration tests for {@link StompBrokerRelayMessageHandler}
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {StompBrokerRelayMessageHandlerIntegrationTests.TestConfiguration.class})
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class StompBrokerRelayMessageHandlerIntegrationTests {

	@Autowired
	private SubscribableChannel messageChannel;

	@Autowired
	private StompBrokerRelayMessageHandler relay;

	@Autowired
	private TestStompBroker stompBroker;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private BrokerAvailabilityListener brokerAvailabilityListener;


	@Test
	public void basicPublishAndSubscribe() throws IOException, InterruptedException {

		String client1SessionId = "abc123";
		String client2SessionId = "def456";

		final CountDownLatch messageLatch = new CountDownLatch(1);

		messageChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
				if (headers.getCommand() == StompCommand.MESSAGE) {
					messageLatch.countDown();
				}
			}

		});

		relay.handleMessage(createConnectMessage(client1SessionId));
		relay.handleMessage(createConnectMessage(client2SessionId));
		relay.handleMessage(createSubscribeMessage(client1SessionId, "/topic/test"));

		stompBroker.awaitMessages(4);

		relay.handleMessage(createSendMessage(client2SessionId, "/topic/test", "fromClient2"));

		assertTrue(messageLatch.await(30, TimeUnit.SECONDS));

		assertEquals(1, brokerAvailabilityListener.availabilityEvents.size());
		assertTrue(brokerAvailabilityListener.availabilityEvents.get(0) instanceof BrokerBecameAvailableEvent);
	}

	@Test
	public void whenConnectFailsDueToTheBrokerBeingUnavailableAnErrorFrameIsSentToTheClient()
			throws IOException, InterruptedException {

		String sessionId = "abc123";

		final CountDownLatch errorLatch = new CountDownLatch(1);

		messageChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
				if (headers.getCommand() == StompCommand.ERROR) {
					errorLatch.countDown();
				}
			}

		});

		stompBroker.awaitMessages(1);

		assertEquals(1, brokerAvailabilityListener.availabilityEvents.size());
		assertTrue(brokerAvailabilityListener.availabilityEvents.get(0) instanceof BrokerBecameAvailableEvent);

		stompBroker.stop();

		relay.handleMessage(createConnectMessage(sessionId));

		errorLatch.await(30, TimeUnit.SECONDS);

		assertEquals(2, brokerAvailabilityListener.availabilityEvents.size());
		assertTrue(brokerAvailabilityListener.availabilityEvents.get(0) instanceof BrokerBecameAvailableEvent);
		assertTrue(brokerAvailabilityListener.availabilityEvents.get(1) instanceof BrokerBecameUnavailableEvent);
	}

	@Test
	public void whenSendFailsDueToTheBrokerBeingUnavailableAnErrorFrameIsSentToTheClient()
			throws IOException, InterruptedException {

		String sessionId = "abc123";

		final CountDownLatch errorLatch = new CountDownLatch(1);

		messageChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
				if (headers.getCommand() == StompCommand.ERROR) {
					errorLatch.countDown();
				}
			}

		});

		relay.handleMessage(createConnectMessage(sessionId));

		stompBroker.awaitMessages(2);

		assertEquals(1, brokerAvailabilityListener.availabilityEvents.size());
		assertTrue(brokerAvailabilityListener.availabilityEvents.get(0) instanceof BrokerBecameAvailableEvent);

		stompBroker.stop();

		relay.handleMessage(createSubscribeMessage(sessionId, "/topic/test/"));

		errorLatch.await(30, TimeUnit.SECONDS);

		assertEquals(2, brokerAvailabilityListener.availabilityEvents.size());
		assertTrue(brokerAvailabilityListener.availabilityEvents.get(0) instanceof BrokerBecameAvailableEvent);
		assertTrue(brokerAvailabilityListener.availabilityEvents.get(1) instanceof BrokerBecameUnavailableEvent);
	}

	private Message<?> createConnectMessage(String sessionId) {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
		headers.setSessionId(sessionId);
		return MessageBuilder.withPayloadAndHeaders(new byte[0], headers).build();
	}

	private Message<?> createSubscribeMessage(String sessionId, String destination) {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		headers.setSessionId(sessionId);
		headers.setDestination(destination);
		headers.setNativeHeader(StompHeaderAccessor.STOMP_ID_HEADER,  sessionId);

		return MessageBuilder.withPayloadAndHeaders(new byte[0], headers).build();
	}

	private Message<?> createSendMessage(String sessionId, String destination, String payload) {
		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		headers.setSessionId(sessionId);
		headers.setDestination(destination);

		return MessageBuilder.withPayloadAndHeaders(payload.getBytes(), headers).build();
	}


	@Configuration
	public static class TestConfiguration {

		@Bean
		public MessageChannel messageChannel() {
			return new ExecutorSubscribableChannel();
		}

		@Bean
		public StompBrokerRelayMessageHandler relay() {
			StompBrokerRelayMessageHandler relay =
					new StompBrokerRelayMessageHandler(messageChannel(), Arrays.asList("/queue/", "/topic/"));
			relay.setRelayPort(SocketUtils.findAvailableTcpPort());
			return relay;
		}

		@Bean
		public TestStompBroker broker() throws IOException {
			TestStompBroker broker = new TestStompBroker(relay().getRelayPort());
			return broker;
		}

		@Bean
		public BrokerAvailabilityListener availabilityListener() {
			return new BrokerAvailabilityListener();
		}
	}


	private static class BrokerAvailabilityListener implements ApplicationListener<BrokerAvailabilityEvent> {

		private final List<BrokerAvailabilityEvent> availabilityEvents = new ArrayList<BrokerAvailabilityEvent>();

		@Override
		public void onApplicationEvent(BrokerAvailabilityEvent event) {
			this.availabilityEvents.add(event);
		}
	}
}
