/*
 * Copyright 2002-present the original author or authors.
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
 * @author Rick Evans
 * @author Juergen Hoeller
 */
class DynamicDestinationResolverTests {

	private static final String DESTINATION_NAME = "foo";

	private final DynamicDestinationResolver resolver = new DynamicDestinationResolver();


	@Test
	void resolveWithPubSubTopicSession() throws Exception {
		Topic expectedDestination1 = new StubTopic();
		Topic expectedDestination2 = new StubTopic();
		TopicSession session = mock();

		given(session.createTopic(DESTINATION_NAME)).willReturn(expectedDestination1);
		testResolveDestination(session, expectedDestination1, true);
		given(session.createTopic(DESTINATION_NAME)).willReturn(expectedDestination2);
		testResolveDestination(session, expectedDestination2, true);
	}

	@Test
	void resolveWithPubSubVanillaSession() throws Exception {
		Topic expectedDestination1 = new StubTopic();
		Topic expectedDestination2 = new StubTopic();
		Session session = mock();

		given(session.createTopic(DESTINATION_NAME)).willReturn(expectedDestination1);
		testResolveDestination(session, expectedDestination1, true);
		given(session.createTopic(DESTINATION_NAME)).willReturn(expectedDestination2);
		testResolveDestination(session, expectedDestination2, true);
	}

	@Test
	void resolveWithPointToPointQueueSession() throws Exception {
		Queue expectedDestination1 = new StubQueue();
		Queue expectedDestination2 = new StubQueue();
		QueueSession session = mock();

		given(session.createQueue(DESTINATION_NAME)).willReturn(expectedDestination1);
		testResolveDestination(session, expectedDestination1, false);
		given(session.createQueue(DESTINATION_NAME)).willReturn(expectedDestination2);
		testResolveDestination(session, expectedDestination2, false);
	}

	@Test
	void resolveWithPointToPointVanillaSession() throws Exception {
		Queue expectedDestination1 = new StubQueue();
		Queue expectedDestination2 = new StubQueue();
		Session session = mock();

		given(session.createQueue(DESTINATION_NAME)).willReturn(expectedDestination1);
		testResolveDestination(session, expectedDestination1, false);
		given(session.createQueue(DESTINATION_NAME)).willReturn(expectedDestination2);
		testResolveDestination(session, expectedDestination2, false);
	}

	private void testResolveDestination(Session session, Destination expected, boolean isPubSub) throws JMSException {
		Destination destination = resolver.resolveDestinationName(session, DESTINATION_NAME, isPubSub);
		assertThat(destination).isNotNull();
		assertThat(destination).isSameAs(expected);
	}

}
