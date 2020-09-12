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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OrderedMessageChannelDecorator}.
 * @author Rossen Stoyanchev
 * @see org.springframework.web.socket.messaging.OrderedMessageSendingIntegrationTests
 */
public class OrderedMessageChannelDecoratorTests {

	private static final Log logger = LogFactory.getLog(OrderedMessageChannelDecoratorTests.class);


	private OrderedMessageChannelDecorator sender;

	ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel(this.executor);

	private ThreadPoolTaskExecutor executor;


	@BeforeEach
	public void setup() {
		this.executor = new ThreadPoolTaskExecutor();
		this.executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
		this.executor.setAllowCoreThreadTimeOut(true);
		this.executor.afterPropertiesSet();

		this.channel = new ExecutorSubscribableChannel(this.executor);
		OrderedMessageChannelDecorator.configureInterceptor(this.channel, true);

		this.sender = new OrderedMessageChannelDecorator(this.channel, logger);

	}

	@AfterEach
	public void tearDown() {
		this.executor.shutdown();
	}


	@Test
	public void test() throws InterruptedException {

		int start = 1;
		int end = 1000;

		AtomicInteger index = new AtomicInteger(start);
		AtomicReference<Object> result = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);

		this.channel.subscribe(message -> {
			int expected = index.getAndIncrement();
			Integer actual = (Integer) message.getHeaders().getOrDefault("seq", -1);
			if (actual != expected) {
				result.set("Expected: " + expected + ", but was: " + actual);
				latch.countDown();
				return;
			}
			// Force messages to queue up periodically
			if (actual % 101 == 0) {
				try {
					Thread.sleep(50);
				}
				catch (InterruptedException ex) {
					result.set(ex.toString());
					latch.countDown();
				}
			}
			if (actual == end) {
				result.set("Done");
				latch.countDown();
			}
		});

		for (int i = start; i <= end; i++) {
			SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
			accessor.setHeader("seq", i);
			accessor.setLeaveMutable(true);
			this.sender.send(MessageBuilder.createMessage("payload", accessor.getMessageHeaders()));
		}

		latch.await(10, TimeUnit.SECONDS);
		assertThat(result.get()).isEqualTo("Done");
	}

}

