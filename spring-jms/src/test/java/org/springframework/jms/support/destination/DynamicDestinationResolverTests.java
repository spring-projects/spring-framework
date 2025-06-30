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
 */
class DynamicDestinationResolverTests {

	private static final String DESTINATION_NAME = "foo";


	@Test
	void resolveWithPubSubTopicSession() throws Exception {
		Topic expectedDestination = new StubTopic();
		TopicSession session = mock();
		given(session.createTopic(DESTINATION_NAME)).willReturn(expectedDestination);
		testResolveDestination(session, expectedDestination, true);
	}

	@Test
	void resolveWithPubSubVanillaSession() throws Exception {
		Topic expectedDestination = new StubTopic();
		Session session = mock();
		given(session.createTopic(DESTINATION_NAME)).willReturn(expectedDestination);
		testResolveDestination(session, expectedDestination, true);
	}

	@Test
	void resolveWithPointToPointQueueSession() throws Exception {
		Queue expectedDestination = new StubQueue();
		QueueSession session = mock();
		given(session.createQueue(DESTINATION_NAME)).willReturn(expectedDestination);
		testResolveDestination(session, expectedDestination, false);
	}

	@Test
	void resolveWithPointToPointVanillaSession() throws Exception {
		Queue expectedDestination = new StubQueue();
		Session session = mock();
		given(session.createQueue(DESTINATION_NAME)).willReturn(expectedDestination);
		testResolveDestination(session, expectedDestination, false);
	}

	private static void testResolveDestination(Session session, Destination expectedDestination, boolean isPubSub) throws JMSException {
		DynamicDestinationResolver resolver = new DynamicDestinationResolver();
		Destination destination = resolver.resolveDestinationName(session, DESTINATION_NAME, isPubSub);
		assertThat(destination).isNotNull();
		assertThat(destination).isSameAs(expectedDestination);
	}

}
