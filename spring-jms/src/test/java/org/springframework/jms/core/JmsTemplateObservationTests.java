/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.micrometer.observation.tck.TestObservationRegistry;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
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
							jmsTemplate.send(jmsReplyTo, (MessageCreator) replySession -> {
								latch.countDown();
								return replySession.createTextMessage("response content");
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

		Message response = jmsTemplate.sendAndReceive("spring.test.observation",
				(MessageCreator) session -> session.createTextMessage("request content"));

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
