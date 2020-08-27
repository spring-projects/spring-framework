/*
 * Copyright 2002-2020 the original author or authors.
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

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OrderedMessageSender}.
 * @author Rossen Stoyanchev
 */
public class OrderedMessageSenderTests {

	private static final Log logger = LogFactory.getLog(OrderedMessageSenderTests.class);

	private static final Random random = new Random();


	private OrderedMessageSender sender;

	ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel(this.executor);

	private ThreadPoolTaskExecutor executor;


	@BeforeEach
	public void setup() {
		this.executor = new ThreadPoolTaskExecutor();
		this.executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
		this.executor.setAllowCoreThreadTimeOut(true);
		this.executor.afterPropertiesSet();

		this.channel = new ExecutorSubscribableChannel(this.executor);
		OrderedMessageSender.configureOutboundChannel(this.channel, true);

		this.sender = new OrderedMessageSender(this.channel, logger);

	}

	@AfterEach
	public void tearDown() {
		this.executor.shutdown();
	}


	@Test
	public void test() throws InterruptedException {

		int sessionCount = 25;
		int messagesPerSessionCount = 500;

		TestMessageHandler handler = new TestMessageHandler(sessionCount * messagesPerSessionCount);
		this.channel.subscribe(handler);

		Publisher<Flux<Message<String>>> messageFluxes =
				Flux.range(1, sessionCount).map(sessionId ->
						Flux.range(1, messagesPerSessionCount)
								.map(sequence -> createMessage(sessionId, sequence))
								.delayElements(Duration.ofMillis(Math.abs(random.nextLong()) % 5)));

		Flux.merge(messageFluxes)
				.doOnNext(message -> this.sender.send(message))
				.blockLast();

		handler.await(20, TimeUnit.SECONDS);

		assertThat(handler.getDescription()).isEqualTo("Total processed: " + sessionCount * messagesPerSessionCount);
		assertThat(handler.getSequenceBySession()).hasSize(sessionCount);
		handler.getSequenceBySession().forEach((key, value) ->
				assertThat(value.get()).as(key).isEqualTo(messagesPerSessionCount));
	}

	private static Message<String> createMessage(Integer sessionId, Integer sequence) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		accessor.setSessionId("session" + sessionId);
		accessor.setHeader("seq", sequence);
		accessor.setLeaveMutable(true);
		return MessageBuilder.createMessage("payload", accessor.getMessageHeaders());
	}


	private static class TestMessageHandler implements MessageHandler {

		private final int totalExpected;

		private final Map<String, AtomicInteger> sequenceBySession = new ConcurrentHashMap<>();

		private final AtomicReference<String> description = new AtomicReference<>();

		private final AtomicInteger totalReceived = new AtomicInteger();

		private final CountDownLatch latch = new CountDownLatch(1);

		TestMessageHandler(int totalExpected) {
			this.totalExpected = totalExpected;
		}

		public void await(long timeout, TimeUnit timeUnit) throws InterruptedException {
			latch.await(timeout, timeUnit);
		}

		public Map<String, AtomicInteger> getSequenceBySession() {
			return sequenceBySession;
		}

		public String getDescription() {
			return description.get();
		}

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			String id = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
			Integer seq = (Integer) message.getHeaders().getOrDefault("seq", -1);

			AtomicInteger prev = sequenceBySession.computeIfAbsent(id, i -> new AtomicInteger(0));
			if (!prev.compareAndSet(seq - 1, seq)) {
				description.set("Out of order, session=" + id + ", prev=" + prev + ", next=" + seq);
				latch.countDown();
				return;
			}

			if (seq == 100) {
				try {
					// Processing delay to cause other session messages to queue up
					Thread.sleep(50);
				}
				catch (InterruptedException ex) {
					description.set(ex.toString());
					latch.countDown();
					return;
				}
			}

			int total = totalReceived.incrementAndGet();
			description.set("Total processed: " + total);
			if (total == totalExpected) {
				latch.countDown();
			}
		}
	}
}

