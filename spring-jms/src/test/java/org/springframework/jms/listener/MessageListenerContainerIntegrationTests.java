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

package org.springframework.jms.listener;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.jms.core.JmsTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 5.3.5
 */
public class MessageListenerContainerIntegrationTests {

	@Test
	public void simpleMessageListenerContainer() throws InterruptedException {
		SimpleMessageListenerContainer mlc = new SimpleMessageListenerContainer();

		testMessageListenerContainer(mlc);
	}

	@Test
	public void defaultMessageListenerContainer() throws InterruptedException {
		DefaultMessageListenerContainer mlc = new DefaultMessageListenerContainer();

		testMessageListenerContainer(mlc);
	}

	@Test
	public void defaultMessageListenerContainerWithMaxMessagesPerTask() throws InterruptedException {
		DefaultMessageListenerContainer mlc = new DefaultMessageListenerContainer();
		mlc.setConcurrentConsumers(1);
		mlc.setMaxConcurrentConsumers(2);
		mlc.setMaxMessagesPerTask(1);

		testMessageListenerContainer(mlc);
	}

	@Test
	public void defaultMessageListenerContainerWithIdleReceivesPerTaskLimit() throws InterruptedException {
		DefaultMessageListenerContainer mlc = new DefaultMessageListenerContainer();
		mlc.setConcurrentConsumers(1);
		mlc.setMaxConcurrentConsumers(2);
		mlc.setIdleReceivesPerTaskLimit(1);

		testMessageListenerContainer(mlc);
	}

	private void testMessageListenerContainer(AbstractMessageListenerContainer mlc) throws InterruptedException {
		ActiveMQConnectionFactory aqcf = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
		TestMessageListener tml = new TestMessageListener();

		mlc.setConnectionFactory(aqcf);
		mlc.setMessageListener(tml);
		mlc.setDestinationName("test");
		mlc.afterPropertiesSet();
		mlc.start();

		JmsTemplate jt = new JmsTemplate(aqcf);
		jt.setDefaultDestinationName("test");

		Set<String> messages = new HashSet<>();
		messages.add("text1");
		messages.add("text2");
		for (String message : messages) {
			jt.convertAndSend(message);
		}
		assertThat(tml.result()).isEqualTo(messages);

		mlc.destroy();
	}


	private static class TestMessageListener implements SessionAwareMessageListener<TextMessage> {

		private final CountDownLatch latch = new CountDownLatch(2);

		private final Set<String> messages = new CopyOnWriteArraySet<>();

		@Override
		public void onMessage(TextMessage message, Session session) throws JMSException {
			this.messages.add(message.getText());
			this.latch.countDown();
		}

		public Set<String> result() throws InterruptedException {
			assertThat(this.latch.await(5, TimeUnit.SECONDS)).isTrue();
			return this.messages;
		}
	}

}
