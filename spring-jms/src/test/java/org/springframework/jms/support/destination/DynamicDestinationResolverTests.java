/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.jms.support.destination;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSession;

import junit.framework.TestCase;

import org.springframework.jms.StubQueue;
import org.springframework.jms.StubTopic;

/**
 * @author Rick Evans
 */
public class DynamicDestinationResolverTests extends TestCase {

	private static final String DESTINATION_NAME = "foo";


	public void testResolveWithPubSubTopicSession() throws Exception {
		Topic expectedDestination = new StubTopic();
		TopicSession session = mock(TopicSession.class);
		given(session.createTopic(DESTINATION_NAME)).willReturn(expectedDestination);
		testResolveDestination(session, expectedDestination, true);
	}

	public void testResolveWithPubSubVanillaSession() throws Exception {
		Topic expectedDestination = new StubTopic();
		Session session = mock(Session.class);
		given(session.createTopic(DESTINATION_NAME)).willReturn(expectedDestination);
		testResolveDestination(session, expectedDestination, true);
	}

	public void testResolveWithPointToPointQueueSession() throws Exception {
		Queue expectedDestination = new StubQueue();
		Session session = mock(QueueSession.class);
		given(session.createQueue(DESTINATION_NAME)).willReturn(expectedDestination);
		testResolveDestination(session, expectedDestination, false);
	}

	public void testResolveWithPointToPointVanillaSession() throws Exception {
		Queue expectedDestination = new StubQueue();
		Session session = mock(Session.class);
		given(session.createQueue(DESTINATION_NAME)).willReturn(expectedDestination);
		testResolveDestination(session, expectedDestination, false);
	}

	private static void testResolveDestination(Session session, Destination expectedDestination, boolean isPubSub) throws JMSException {
		DynamicDestinationResolver resolver = new DynamicDestinationResolver();
		Destination destination = resolver.resolveDestinationName(session, DESTINATION_NAME, isPubSub);
		assertNotNull(destination);
		assertSame(expectedDestination, destination);
	}

}
