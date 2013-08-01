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
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.channel.ExecutorSubscribableChannel;
import org.springframework.util.SocketUtils;

import static org.junit.Assert.*;


/**
 * Integration tests for {@link StompBrokerRelayMessageHandler}
 *
 * @author Andy Wilkinson
 */
public class StompBrokerRelayMessageHandlerIntegrationTests {

	private final SubscribableChannel messageChannel = new ExecutorSubscribableChannel();

	private final StompBrokerRelayMessageHandler relay =
			new StompBrokerRelayMessageHandler(messageChannel, Arrays.asList("/queue/", "/topic/"));


	@Test
	public void basicPublishAndSubscribe() throws IOException, InterruptedException {
		int port = SocketUtils.findAvailableTcpPort();

		TestStompBroker stompBroker = new TestStompBroker(port);
		stompBroker.start();

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

		relay.setRelayPort(port);
		relay.start();

		relay.handleMessage(createConnectMessage(client1SessionId));
		relay.handleMessage(createConnectMessage(client2SessionId));
		relay.handleMessage(createSubscribeMessage(client1SessionId, "/topic/test"));

		stompBroker.awaitMessages(4);

		relay.handleMessage(createSendMessage(client2SessionId, "/topic/test", "fromClient2"));

		assertTrue(messageLatch.await(30, TimeUnit.SECONDS));

		this.relay.stop();
		stompBroker.stop();
	}

	@Test
	public void whenConnectFailsDueToTheBrokerBeingUnavailableAnErrorFrameIsSentToTheClient()
			throws IOException, InterruptedException {
		int port = SocketUtils.findAvailableTcpPort();

		TestStompBroker stompBroker = new TestStompBroker(port);
		stompBroker.start();

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

		relay.setRelayPort(port);
		relay.start();

		stompBroker.awaitMessages(1);

		stompBroker.stop();

		relay.handleMessage(createConnectMessage(sessionId));

		errorLatch.await(30, TimeUnit.SECONDS);
	}

	@Test
	public void whenSendFailsDueToTheBrokerBeingUnavailableAnErrorFrameIsSentToTheClient()
			throws IOException, InterruptedException {
		int port = SocketUtils.findAvailableTcpPort();

		TestStompBroker stompBroker = new TestStompBroker(port);
		stompBroker.start();

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

		relay.setRelayPort(port);
		relay.start();

		relay.handleMessage(createConnectMessage(sessionId));

		stompBroker.awaitMessages(2);

		stompBroker.stop();

		relay.handleMessage(createSubscribeMessage(sessionId, "/topic/test/"));

		errorLatch.await(30, TimeUnit.SECONDS);
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
}
