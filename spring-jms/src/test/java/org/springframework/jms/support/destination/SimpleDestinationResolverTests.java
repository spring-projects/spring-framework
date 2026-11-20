/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.support.destination;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import jakarta.jms.TopicSession;
import org.junit.jupiter.api.Test;

import org.springframework.jms.StubQueue;
import org.springframework.jms.StubTopic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Juergen Hoeller
 * @since 7.0
 */
class SimpleDestinationResolverTests {

	private static final String DESTINATION_NAME = "foo";

	private final SimpleDestinationResolver resolver = new SimpleDestinationResolver();


	@Test
	void resolveWithPubSubTopicSession() throws Exception {
		Topic expectedDestination1 = new StubTopic();
		Topic expectedDestination2 = new StubTopic();
		TopicSession session = mock();

		given(session.createTopic(DESTINATION_NAME)).willReturn(expectedDestination1);
		testResolveDestination(session, expectedDestination1, true);
		given(session.createTopic(DESTINATION_NAME)).willReturn(expectedDestination2);
		testResolveDestination(session, expectedDestination1, true);
	}

	@Test
	void resolveWithPubSubVanillaSession() throws Exception {
		Topic expectedDestination1 = new StubTopic();
		Topic expectedDestination2 = new StubTopic();
		Session session = mock();

		given(session.createTopic(DESTINATION_NAME)).willReturn(expectedDestination1);
		testResolveDestination(session, expectedDestination1, true);
		given(session.createTopic(DESTINATION_NAME)).willReturn(expectedDestination2);
		testResolveDestination(session, expectedDestination1, true);
	}

	@Test
	void resolveWithPointToPointQueueSession() throws Exception {
		Queue expectedDestination1 = new StubQueue();
		Queue expectedDestination2 = new StubQueue();
		QueueSession session = mock();

		given(session.createQueue(DESTINATION_NAME)).willReturn(expectedDestination1);
		testResolveDestination(session, expectedDestination1, false);
		given(session.createQueue(DESTINATION_NAME)).willReturn(expectedDestination2);
		testResolveDestination(session, expectedDestination1, false);
	}

	@Test
	void resolveWithPointToPointVanillaSession() throws Exception {
		Queue expectedDestination1 = new StubQueue();
		Queue expectedDestination2 = new StubQueue();
		Session session = mock();

		given(session.createQueue(DESTINATION_NAME)).willReturn(expectedDestination1);
		testResolveDestination(session, expectedDestination1, false);
		given(session.createQueue(DESTINATION_NAME)).willReturn(expectedDestination2);
		testResolveDestination(session, expectedDestination1, false);
	}

	private void testResolveDestination(Session session, Destination expected, boolean isPubSub) throws JMSException {
		Destination destination = resolver.resolveDestinationName(session, DESTINATION_NAME, isPubSub);
		assertThat(destination).isNotNull();
		assertThat(destination).isSameAs(expected);
	}

}
