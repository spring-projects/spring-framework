/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSession;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.jms.StubQueue;
import org.springframework.jms.StubTopic;

/**
 * @author Rick Evans
 */
public class DynamicDestinationResolverTests extends TestCase {

	private static final String DESTINATION_NAME = "foo";


	public void testResolveWithPubSubTopicSession() throws Exception {

		Topic expectedDestination = new StubTopic();

		MockControl mockSession = MockControl.createControl(TopicSession.class);
		TopicSession session = (TopicSession) mockSession.getMock();
		session.createTopic(DESTINATION_NAME);
		mockSession.setReturnValue(expectedDestination);
		mockSession.replay();

		testResolveDestination(session, expectedDestination, true);

		mockSession.verify();
	}

	public void testResolveWithPubSubVanillaSession() throws Exception {

		Topic expectedDestination = new StubTopic();

		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		session.createTopic(DESTINATION_NAME);
		mockSession.setReturnValue(expectedDestination);
		mockSession.replay();

		testResolveDestination(session, expectedDestination, true);

		mockSession.verify();
	}

	public void testResolveWithPointToPointQueueSession() throws Exception {

		Queue expectedDestination = new StubQueue();

		MockControl mockSession = MockControl.createControl(QueueSession.class);
		Session session = (Session) mockSession.getMock();
		session.createQueue(DESTINATION_NAME);
		mockSession.setReturnValue(expectedDestination);
		mockSession.replay();

		testResolveDestination(session, expectedDestination, false);

		mockSession.verify();
	}

	public void testResolveWithPointToPointVanillaSession() throws Exception {

		Queue expectedDestination = new StubQueue();

		MockControl mockSession = MockControl.createControl(Session.class);
		Session session = (Session) mockSession.getMock();
		session.createQueue(DESTINATION_NAME);
		mockSession.setReturnValue(expectedDestination);
		mockSession.replay();

		testResolveDestination(session, expectedDestination, false);

		mockSession.verify();
	}

	private static void testResolveDestination(Session session, Destination expectedDestination, boolean isPubSub) throws JMSException {
		DynamicDestinationResolver resolver = new DynamicDestinationResolver();
		Destination destination = resolver.resolveDestinationName(session, DESTINATION_NAME, isPubSub);
		assertNotNull(destination);
		assertSame(expectedDestination, destination);
	}

}
