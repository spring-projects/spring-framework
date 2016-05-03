/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.messaging.simp.stomp;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.SocketUtils;
import org.springframework.util.concurrent.ListenableFuture;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Integration tests for {@link Reactor2TcpStompClient}.
 *
 * @author Rossen Stoyanchev
 */
public class Reactor2TcpStompClientTests {

	private static final Log logger = LogFactory.getLog(Reactor2TcpStompClientTests.class);

	@Rule
	public final TestName testName = new TestName();

	private BrokerService activeMQBroker;

	private Reactor2TcpStompClient client;


	@Before
	public void setUp() throws Exception {

		logger.debug("Setting up before '" + this.testName.getMethodName() + "'");

		int port = SocketUtils.findAvailableTcpPort(61613);

		this.activeMQBroker = new BrokerService();
		this.activeMQBroker.addConnector("stomp://127.0.0.1:" + port);
		this.activeMQBroker.setStartAsync(false);
		this.activeMQBroker.setPersistent(false);
		this.activeMQBroker.setUseJmx(false);
		this.activeMQBroker.getSystemUsage().getMemoryUsage().setLimit(1024 * 1024 * 5);
		this.activeMQBroker.getSystemUsage().getTempUsage().setLimit(1024 * 1024 * 5);
		this.activeMQBroker.start();

		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();

		this.client = new Reactor2TcpStompClient("127.0.0.1", port);
		this.client.setMessageConverter(new StringMessageConverter());
		this.client.setTaskScheduler(taskScheduler);
	}

	@After
	public void tearDown() throws Exception {
		try {
			this.client.shutdown();
		}
		catch (Throwable ex) {
			logger.error("Failed to shut client", ex);
		}
		final CountDownLatch latch = new CountDownLatch(1);
		this.activeMQBroker.addShutdownHook(latch::countDown);
		logger.debug("Stopping ActiveMQ broker and will await shutdown");
		this.activeMQBroker.stop();
		if (!latch.await(5, TimeUnit.SECONDS)) {
			logger.debug("ActiveMQ broker did not shut in the expected time.");
		}
	}

	@Test
	public void publishSubscribe() throws Exception {
		String destination = "/topic/foo";
		ConsumingHandler consumingHandler1 = new ConsumingHandler(destination);
		ListenableFuture<StompSession> consumerFuture1 = this.client.connect(consumingHandler1);

		ConsumingHandler consumingHandler2 = new ConsumingHandler(destination);
		ListenableFuture<StompSession> consumerFuture2 = this.client.connect(consumingHandler2);

		assertTrue(consumingHandler1.awaitForSubscriptions(5000));
		assertTrue(consumingHandler2.awaitForSubscriptions(5000));

		ProducingHandler producingHandler = new ProducingHandler();
		producingHandler.addToSend(destination, "foo1");
		producingHandler.addToSend(destination, "foo2");
		ListenableFuture<StompSession> producerFuture = this.client.connect(producingHandler);

		assertTrue(consumingHandler1.awaitForMessageCount(2, 5000));
		assertThat(consumingHandler1.getReceived(), containsInAnyOrder("foo1", "foo2"));

		assertTrue(consumingHandler2.awaitForMessageCount(2, 5000));
		assertThat(consumingHandler2.getReceived(), containsInAnyOrder("foo1", "foo2"));

		consumerFuture1.get().disconnect();
		consumerFuture2.get().disconnect();
		producerFuture.get().disconnect();
	}


	private static class LoggingSessionHandler extends StompSessionHandlerAdapter {

		@Override
		public void handleException(StompSession session, StompCommand command,
				StompHeaders headers, byte[] payload, Throwable ex) {

			logger.error(command + " " + headers, ex);
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			logger.error("STOMP error frame " + headers + " payload=" + payload);
		}

		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			logger.error(exception);
		}
	}


	private static class ConsumingHandler extends LoggingSessionHandler {

		private final List<String> topics;

		private final CountDownLatch subscriptionLatch;

		private final List<String> received = new ArrayList<>();

		public ConsumingHandler(String... topics) {
			Assert.notEmpty(topics);
			this.topics = Arrays.asList(topics);
			this.subscriptionLatch = new CountDownLatch(this.topics.size());
		}

		public List<String> getReceived() {
			return this.received;
		}


		@Override
		public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
			for (String topic : this.topics) {
				session.setAutoReceipt(true);
				Subscription subscription = session.subscribe(topic, new StompFrameHandler() {
					@Override
					public Type getPayloadType(StompHeaders headers) {
						return String.class;
					}
					@Override
					public void handleFrame(StompHeaders headers, Object payload) {
						received.add((String) payload);
					}
				});
				subscription.addReceiptTask(subscriptionLatch::countDown);
			}
		}

		public boolean awaitForSubscriptions(long millisToWait) throws InterruptedException {
			if (logger.isDebugEnabled()) {
				logger.debug("Awaiting for subscription receipts");
			}
			return this.subscriptionLatch.await(millisToWait, TimeUnit.MILLISECONDS);
		}

		public boolean awaitForMessageCount(int expected, long millisToWait) throws InterruptedException {
			if (logger.isDebugEnabled()) {
				logger.debug("Awaiting for message count: " + expected);
			}
			long startTime = System.currentTimeMillis();
			while (this.received.size() < expected) {
				Thread.sleep(500);
				if ((System.currentTimeMillis() - startTime) > millisToWait) {
					return false;
				}
			}
			return true;
		}
	}


	private static class ProducingHandler extends LoggingSessionHandler {

		private final List<String> topics = new ArrayList<>();

		private final List<Object> payloads = new ArrayList<>();

		public ProducingHandler addToSend(String topic, Object payload) {
			this.topics.add(topic);
			this.payloads.add(payload);
			return this;
		}

		@Override
		public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
			for (int i = 0; i < this.topics.size(); i++) {
				session.send(this.topics.get(i), this.payloads.get(i));
			}
		}
	}

}
