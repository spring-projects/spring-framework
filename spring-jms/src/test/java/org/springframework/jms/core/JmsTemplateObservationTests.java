/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.jms.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.micrometer.observation.tck.TestObservationRegistry;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.junit.EmbeddedActiveMQExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Observability related {@link JmsTemplate}.
 *
 * @author Brian Clozel
 */
class JmsTemplateObservationTests {

	@RegisterExtension
	EmbeddedActiveMQExtension server = new EmbeddedActiveMQExtension();

	TestObservationRegistry registry = TestObservationRegistry.create();

	private ActiveMQConnectionFactory connectionFactory;

	@BeforeEach
	void setupServer() {
		server.start();
		connectionFactory = new ActiveMQConnectionFactory(server.getVmURL());
	}

	@Test
	void shouldRecordJmsPublishObservations() {
		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		jmsTemplate.setObservationRegistry(registry);
		jmsTemplate.convertAndSend("spring.test.observation", "message content");
		assertThat(registry).hasObservationWithNameEqualTo("jms.message.publish")
			.that()
			.hasHighCardinalityKeyValue("messaging.destination.name", "spring.test.observation");
	}

	@Test
	void shouldRecordJmsProcessObservations() {
		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		jmsTemplate.setObservationRegistry(registry);
		jmsTemplate.convertAndSend("spring.test.observation", "message content");
		jmsTemplate.execute(session -> {
			try {
				CountDownLatch latch = new CountDownLatch(1);
				MessageConsumer mc = session.createConsumer(session.createQueue("spring.test.observation"));
				mc.setMessageListener(message -> latch.countDown());
				return latch.await(2, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}, true);
		assertThat(registry).hasObservationWithNameEqualTo("jms.message.process")
			.that()
			.hasHighCardinalityKeyValue("messaging.destination.name", "spring.test.observation");
	}

	@Test
	void shouldRecordJmsPublishAndProcessObservations() throws Exception {
		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		jmsTemplate.setObservationRegistry(registry);

		new Thread(() -> {
			jmsTemplate.execute(session -> {
				try {
					CountDownLatch latch = new CountDownLatch(1);
					MessageConsumer mc = session.createConsumer(session.createQueue("spring.test.observation"));
					mc.setMessageListener(message -> {
						try {
							Destination jmsReplyTo = message.getJMSReplyTo();
							jmsTemplate.send(jmsReplyTo, new MessageCreator() {
								@Override
								public Message createMessage(Session session) throws JMSException {
									latch.countDown();
									return session.createTextMessage("response content");
								}
							});
						}
						catch (JMSException e) {
							throw new RuntimeException(e);
						}
					});
					return latch.await(2, TimeUnit.SECONDS);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}, true);

		}).start();
		Message response = jmsTemplate.sendAndReceive("spring.test.observation", new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage("request content");
			}
		});

		String responseBody = response.getBody(String.class);
		assertThat(responseBody).isEqualTo("response content");

		assertThat(registry).hasNumberOfObservationsWithNameEqualTo("jms.message.publish", 2);
		assertThat(registry).hasObservationWithNameEqualTo("jms.message.process").that()
				.hasHighCardinalityKeyValue("messaging.destination.name", "spring.test.observation");
	}

	@AfterEach
	void shutdownServer() {
		connectionFactory.close();
		server.stop();
	}

}
