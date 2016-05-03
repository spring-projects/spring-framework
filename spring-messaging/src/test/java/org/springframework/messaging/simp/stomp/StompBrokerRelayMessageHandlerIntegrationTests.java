/*
 * Copyright 2002-2015 the original author or authors.
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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.StubMessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.SocketUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for {@link StompBrokerRelayMessageHandler} running against ActiveMQ.
 *
 * @author Rossen Stoyanchev
 */
public class StompBrokerRelayMessageHandlerIntegrationTests {

	@Rule
	public final TestName testName = new TestName();

	private static final Log logger = LogFactory.getLog(StompBrokerRelayMessageHandlerIntegrationTests.class);

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private StompBrokerRelayMessageHandler relay;

	private BrokerService activeMQBroker;

	private ExecutorSubscribableChannel responseChannel;

	private TestMessageHandler responseHandler;

	private TestEventPublisher eventPublisher;

	private int port;


	@Before
	public void setUp() throws Exception {
		logger.debug("Setting up before '" + this.testName.getMethodName() + "'");
		this.port = SocketUtils.findAvailableTcpPort(61613);
		this.responseChannel = new ExecutorSubscribableChannel();
		this.responseHandler = new TestMessageHandler();
		this.responseChannel.subscribe(this.responseHandler);
		this.eventPublisher = new TestEventPublisher();
		startActiveMqBroker();
		createAndStartRelay();
	}

	private void startActiveMqBroker() throws Exception {
		this.activeMQBroker = new BrokerService();
		this.activeMQBroker.addConnector("stomp://localhost:" + this.port);
		this.activeMQBroker.setStartAsync(false);
		this.activeMQBroker.setPersistent(false);
		this.activeMQBroker.setUseJmx(false);
		this.activeMQBroker.getSystemUsage().getMemoryUsage().setLimit(1024 * 1024 * 5);
		this.activeMQBroker.getSystemUsage().getTempUsage().setLimit(1024 * 1024 * 5);
		this.activeMQBroker.start();
	}

	private void createAndStartRelay() throws InterruptedException {
		this.relay = new StompBrokerRelayMessageHandler(new StubMessageChannel(),
				this.responseChannel, new StubMessageChannel(), Arrays.asList("/queue/", "/topic/"));
		this.relay.setRelayPort(this.port);
		this.relay.setApplicationEventPublisher(this.eventPublisher);
		this.relay.setSystemHeartbeatReceiveInterval(0);
		this.relay.setSystemHeartbeatSendInterval(0);

		this.relay.start();
		this.eventPublisher.expectBrokerAvailabilityEvent(true);
	}

	@After
	public void tearDown() throws Exception {
		try {
			logger.debug("STOMP broker relay stats: " + this.relay.getStatsInfo());
			this.relay.stop();
		}
		finally {
			stopActiveMqBrokerAndAwait();
		}
	}

	private void stopActiveMqBrokerAndAwait() throws Exception {
		logger.debug("Stopping ActiveMQ broker and will await shutdown");
		if (!this.activeMQBroker.isStarted()) {
			logger.debug("Broker not running");
			return;
		}
		final CountDownLatch latch = new CountDownLatch(1);
		this.activeMQBroker.addShutdownHook(new Runnable() {
			public void run() {
				latch.countDown();
			}
		});
		this.activeMQBroker.stop();
		assertTrue("Broker did not stop", latch.await(5, TimeUnit.SECONDS));
		logger.debug("Broker stopped");
	}

	@Test
	public void publishSubscribe() throws Exception {

		logger.debug("Starting test publishSubscribe()");

		String sess1 = "sess1";
		String sess2 = "sess2";
		String subs1 = "subs1";
		String destination = "/topic/test";

		MessageExchange conn1 = MessageExchangeBuilder.connect(sess1).build();
		MessageExchange conn2 = MessageExchangeBuilder.connect(sess2).build();
		this.relay.handleMessage(conn1.message);
		this.relay.handleMessage(conn2.message);
		this.responseHandler.expectMessages(conn1, conn2);

		MessageExchange subscribe = MessageExchangeBuilder.subscribeWithReceipt(sess1, subs1, destination, "r1").build();
		this.relay.handleMessage(subscribe.message);
		this.responseHandler.expectMessages(subscribe);

		MessageExchange send = MessageExchangeBuilder.send(destination, "foo").andExpectMessage(sess1, subs1).build();
		this.relay.handleMessage(send.message);
		this.responseHandler.expectMessages(send);
	}

	@Test(expected=MessageDeliveryException.class)
	public void messageDeliveryExceptionIfSystemSessionForwardFails() throws Exception {

		logger.debug("Starting test messageDeliveryExceptionIfSystemSessionForwardFails()");

		stopActiveMqBrokerAndAwait();
		this.eventPublisher.expectBrokerAvailabilityEvent(false);

		StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
		this.relay.handleMessage(MessageBuilder.createMessage("test".getBytes(), headers.getMessageHeaders()));
	}

	@Test
	public void brokerBecomingUnvailableTriggersErrorFrame() throws Exception {

		logger.debug("Starting test brokerBecomingUnvailableTriggersErrorFrame()");

		String sess1 = "sess1";
		MessageExchange connect = MessageExchangeBuilder.connect(sess1).build();
		this.relay.handleMessage(connect.message);
		this.responseHandler.expectMessages(connect);

		MessageExchange error = MessageExchangeBuilder.error(sess1).build();
		stopActiveMqBrokerAndAwait();
		this.eventPublisher.expectBrokerAvailabilityEvent(false);
		this.responseHandler.expectMessages(error);
	}

	@Test
	public void brokerAvailabilityEventWhenStopped() throws Exception {

		logger.debug("Starting test brokerAvailabilityEventWhenStopped()");

		stopActiveMqBrokerAndAwait();
		this.eventPublisher.expectBrokerAvailabilityEvent(false);
	}

	@Test
	public void relayReconnectsIfBrokerComesBackUp() throws Exception {

		logger.debug("Starting test relayReconnectsIfBrokerComesBackUp()");

		String sess1 = "sess1";
		MessageExchange conn1 = MessageExchangeBuilder.connect(sess1).build();
		this.relay.handleMessage(conn1.message);
		this.responseHandler.expectMessages(conn1);

		String subs1 = "subs1";
		String destination = "/topic/test";
		MessageExchange subscribe = MessageExchangeBuilder.subscribeWithReceipt(sess1, subs1, destination, "r1").build();
		this.relay.handleMessage(subscribe.message);
		this.responseHandler.expectMessages(subscribe);

		MessageExchange error = MessageExchangeBuilder.error(sess1).build();
		stopActiveMqBrokerAndAwait();
		this.responseHandler.expectMessages(error);

		this.eventPublisher.expectBrokerAvailabilityEvent(false);

		startActiveMqBroker();
		this.eventPublisher.expectBrokerAvailabilityEvent(true);
	}

	@Test
	public void disconnectWithReceipt() throws Exception {

		logger.debug("Starting test disconnectWithReceipt()");

		MessageExchange connect = MessageExchangeBuilder.connect("sess1").build();
		this.relay.handleMessage(connect.message);
		this.responseHandler.expectMessages(connect);

		MessageExchange disconnect = MessageExchangeBuilder.disconnectWithReceipt("sess1", "r123").build();
		this.relay.handleMessage(disconnect.message);

		this.responseHandler.expectMessages(disconnect);
	}


	private static class TestEventPublisher implements ApplicationEventPublisher {

		private final BlockingQueue<BrokerAvailabilityEvent> eventQueue = new LinkedBlockingQueue<>();

		@Override
		public void publishEvent(ApplicationEvent event) {
			publishEvent((Object) event);
		}

		@Override
		public void publishEvent(Object event) {
			logger.debug("Processing ApplicationEvent " + event);
			if (event instanceof BrokerAvailabilityEvent) {
				this.eventQueue.add((BrokerAvailabilityEvent) event);
			}
		}

		public void expectBrokerAvailabilityEvent(boolean isBrokerAvailable) throws InterruptedException {
			BrokerAvailabilityEvent event = this.eventQueue.poll(20000, TimeUnit.MILLISECONDS);
			assertNotNull("Times out waiting for BrokerAvailabilityEvent[" + isBrokerAvailable + "]", event);
			assertEquals(isBrokerAvailable, event.isBrokerAvailable());
		}
	}

	private static class TestMessageHandler implements MessageHandler {

		private final BlockingQueue<Message<?>> queue = new LinkedBlockingQueue<>();

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			if (SimpMessageType.HEARTBEAT == SimpMessageHeaderAccessor.getMessageType(message.getHeaders())) {
				return;
			}
			this.queue.add(message);
		}

		public void expectMessages(MessageExchange... messageExchanges) throws InterruptedException {

			List<MessageExchange> expectedMessages =
					new ArrayList<MessageExchange>(Arrays.<MessageExchange>asList(messageExchanges));

			while (expectedMessages.size() > 0) {
				Message<?> message = this.queue.poll(10000, TimeUnit.MILLISECONDS);
				assertNotNull("Timed out waiting for messages, expected [" + expectedMessages + "]", message);

				MessageExchange match = findMatch(expectedMessages, message);
				assertNotNull("Unexpected message=" + message + ", expected [" + expectedMessages + "]", match);

				expectedMessages.remove(match);
			}
		}

		private MessageExchange findMatch(List<MessageExchange> expectedMessages, Message<?> message) {
			for (MessageExchange exchange : expectedMessages) {
				if (exchange.matchMessage(message)) {
					return exchange;
				}
			}
			return null;
		}
	}

	/**
	 * Holds a message as well as expected and actual messages matched against expectations.
	 */
	private static class MessageExchange {

		private final Message<?> message;

		private final MessageMatcher[] expected;

		private final Message<?>[] actual;

		public MessageExchange(Message<?> message, MessageMatcher... expected) {
			this.message = message;
			this.expected = expected;
			this.actual = new Message<?>[expected.length];
		}

		public boolean matchMessage(Message<?> message) {
			for (int i = 0 ; i < this.expected.length; i++) {
				if (this.expected[i].match(message)) {
					this.actual[i] = message;
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return "Forwarded message:\n" + this.message + "\n" +
					"Should receive back:\n" + Arrays.toString(this.expected) + "\n" +
					"Actually received:\n" + Arrays.toString(this.actual) + "\n";
		}
	}

	private static class MessageExchangeBuilder {

		private final Message<?> message;

		private final StompHeaderAccessor headers;

		private final List<MessageMatcher> expected = new ArrayList<>();


		private MessageExchangeBuilder(Message<?> message) {
			this.message = message;
			this.headers = StompHeaderAccessor.wrap(message);
		}

		public static MessageExchangeBuilder error(String sessionId) {
			return new MessageExchangeBuilder(null).andExpectError(sessionId);
		}

		public static MessageExchangeBuilder connect(String sessionId) {
			StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
			headers.setSessionId(sessionId);
			headers.setAcceptVersion("1.1,1.2");
			headers.setHeartbeat(0, 0);
			Message<?> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

			MessageExchangeBuilder builder = new MessageExchangeBuilder(message);
			builder.expected.add(new StompConnectedFrameMessageMatcher(sessionId));
			return builder;
		}

		// TODO Determine why connectWithError() is unused.
		@SuppressWarnings("unused")
		public static MessageExchangeBuilder connectWithError(String sessionId) {
			StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.CONNECT);
			headers.setSessionId(sessionId);
			headers.setAcceptVersion("1.1,1.2");
			Message<?> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
			MessageExchangeBuilder builder = new MessageExchangeBuilder(message);
			return builder.andExpectError();
		}

		public static MessageExchangeBuilder subscribeWithReceipt(String sessionId, String subscriptionId,
				String destination, String receiptId) {

			StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
			headers.setSessionId(sessionId);
			headers.setSubscriptionId(subscriptionId);
			headers.setDestination(destination);
			headers.setReceipt(receiptId);
			Message<?> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

			MessageExchangeBuilder builder = new MessageExchangeBuilder(message);
			builder.expected.add(new StompReceiptFrameMessageMatcher(sessionId, receiptId));
			return builder;
		}

		public static MessageExchangeBuilder send(String destination, String payload) {
			SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
			headers.setDestination(destination);
			Message<?> message = MessageBuilder.createMessage(payload.getBytes(UTF_8), headers.getMessageHeaders());
			return new MessageExchangeBuilder(message);
		}

		public static MessageExchangeBuilder disconnectWithReceipt(String sessionId, String receiptId) {

			StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.DISCONNECT);
			headers.setSessionId(sessionId);
			headers.setReceipt(receiptId);
			Message<?> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());

			MessageExchangeBuilder builder = new MessageExchangeBuilder(message);
			builder.expected.add(new StompReceiptFrameMessageMatcher(sessionId, receiptId));
			return builder;
		}

		public MessageExchangeBuilder andExpectMessage(String sessionId, String subscriptionId) {
			Assert.isTrue(SimpMessageType.MESSAGE.equals(headers.getMessageType()));
			String destination = this.headers.getDestination();
			Object payload = this.message.getPayload();
			this.expected.add(new StompMessageFrameMessageMatcher(sessionId, subscriptionId, destination, payload));
			return this;
		}

		public MessageExchangeBuilder andExpectError() {
			String sessionId = this.headers.getSessionId();
			Assert.notNull(sessionId, "No sessionId to match the ERROR frame to");
			return andExpectError(sessionId);
		}

		public MessageExchangeBuilder andExpectError(String sessionId) {
			this.expected.add(new StompFrameMessageMatcher(StompCommand.ERROR, sessionId));
			return this;
		}

		public MessageExchange build() {
			return new MessageExchange(this.message, this.expected.toArray(new MessageMatcher[this.expected.size()]));
		}
	}

	private static interface MessageMatcher {

		boolean match(Message<?> message);

	}

	private static class StompFrameMessageMatcher implements MessageMatcher {

		private final StompCommand command;

		private final String sessionId;


		public StompFrameMessageMatcher(StompCommand command, String sessionId) {
			this.command = command;
			this.sessionId = sessionId;
		}


		@Override
		public final boolean match(Message<?> message) {
			StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
			if (!this.command.equals(headers.getCommand()) || (this.sessionId != headers.getSessionId())) {
				return false;
			}
			return matchInternal(headers, message.getPayload());
		}

		protected boolean matchInternal(StompHeaderAccessor headers, Object payload) {
			return true;
		}

		@Override
		public String toString() {
			return "command=" + this.command  + ", session=\"" + this.sessionId + "\"";
		}
	}

	private static class StompReceiptFrameMessageMatcher extends StompFrameMessageMatcher {

		private final String receiptId;

		public StompReceiptFrameMessageMatcher(String sessionId, String receipt) {
			super(StompCommand.RECEIPT, sessionId);
			this.receiptId = receipt;
		}

		@Override
		protected boolean matchInternal(StompHeaderAccessor headers, Object payload) {
			return (this.receiptId.equals(headers.getReceiptId()));
		}

		@Override
		public String toString() {
			return super.toString() + ", receiptId=\"" + this.receiptId + "\"";
		}
	}

	private static class StompMessageFrameMessageMatcher extends StompFrameMessageMatcher {

		private final String subscriptionId;

		private final String destination;

		private final Object payload;


		public StompMessageFrameMessageMatcher(String sessionId, String subscriptionId, String destination, Object payload) {
			super(StompCommand.MESSAGE, sessionId);
			this.subscriptionId = subscriptionId;
			this.destination = destination;
			this.payload = payload;
		}

		@Override
		protected boolean matchInternal(StompHeaderAccessor headers, Object payload) {
			if (!this.subscriptionId.equals(headers.getSubscriptionId()) ||  !this.destination.equals(headers.getDestination())) {
				return false;
			}
			if (payload instanceof byte[] && this.payload instanceof byte[]) {
				return Arrays.equals((byte[]) payload, (byte[]) this.payload);
			}
			else {
				return this.payload.equals(payload);
			}
		}

		@Override
		public String toString() {
			return super.toString() + ", subscriptionId=\"" + this.subscriptionId
					+ "\", destination=\"" + this.destination + "\", payload=\"" + getPayloadAsText() + "\"";
		}

		protected String getPayloadAsText() {
			return (this.payload instanceof byte[])
					? new String((byte[]) this.payload, UTF_8) : payload.toString();
		}
	}

	private static class StompConnectedFrameMessageMatcher extends StompFrameMessageMatcher {


		public StompConnectedFrameMessageMatcher(String sessionId) {
			super(StompCommand.CONNECTED, sessionId);
		}

	}

}
