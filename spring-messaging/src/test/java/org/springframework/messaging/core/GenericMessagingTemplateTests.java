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

package org.springframework.messaging.core;

import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.*;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.channel.ExecutorSubscribableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link GenericMessagingTemplate}.
 *
 * @author Rossen Stoyanchev
 */
public class GenericMessagingTemplateTests {

	private GenericMessagingTemplate template;

	private ThreadPoolTaskExecutor executor;


	@Before
	public void setup() {

		this.template = new GenericMessagingTemplate();

		this.executor = new ThreadPoolTaskExecutor();
		this.executor.afterPropertiesSet();
	}


	@Test
	public void sendAndReceive() {

		SubscribableChannel channel = new ExecutorSubscribableChannel(this.executor);
		channel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(new GenericMessage<String>("response"));
			}
		});

		String actual = this.template.convertSendAndReceive(channel, "request", String.class);

		assertEquals("response", actual);
	}

	@Test
	public void sendAndReceiveTimeout() throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);

		this.template.setReceiveTimeout(1);
		this.template.setThrowExceptionOnLateReply(true);

		SubscribableChannel channel = new ExecutorSubscribableChannel(this.executor);
		channel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				try {
					Thread.sleep(500);
					MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
					replyChannel.send(new GenericMessage<String>("response"));
					fail("Expected exception");
				}
				catch (InterruptedException e) {
					fail("Unexpected exception " + e.getMessage());
				}
				catch (MessageDeliveryException ex) {
					assertEquals("Reply message received but the receiving thread has already received a reply",
							ex.getMessage());
				}
				finally {
					latch.countDown();
				}
			}
		});

		assertNull(this.template.convertSendAndReceive(channel, "request", String.class));

		assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
	}

}
