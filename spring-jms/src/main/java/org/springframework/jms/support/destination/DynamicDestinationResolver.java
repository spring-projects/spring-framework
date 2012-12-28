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

import org.springframework.util.Assert;

/**
 * Simple {@link DestinationResolver} implementation resolving destination names
 * as dynamic destinations.
 *
 * <p>This implementation will work on both JMS 1.1 and JMS 1.0.2,
 * because it uses the {@link javax.jms.QueueSession} or {@link javax.jms.TopicSession}
 * methods if possible, falling back to JMS 1.1's generic {@link javax.jms.Session}
 * methods.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see javax.jms.QueueSession#createQueue
 * @see javax.jms.TopicSession#createTopic
 * @see javax.jms.Session#createQueue
 * @see javax.jms.Session#createTopic
 */
public class DynamicDestinationResolver implements DestinationResolver {

	/**
	 * Resolve the specified destination name as a dynamic destination.
	 * @param session the current JMS Session
	 * @param destinationName the name of the destination
	 * @param pubSubDomain {@code true} if the domain is pub-sub, {@code false} if P2P
	 * @return the JMS destination (either a topic or a queue)
	 * @throws javax.jms.JMSException if resolution failed
	 * @see #resolveTopic(javax.jms.Session, String)
	 * @see #resolveQueue(javax.jms.Session, String)
	 */
	@Override
	public Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain)
			throws JMSException {

		Assert.notNull(session, "Session must not be null");
		Assert.notNull(destinationName, "Destination name must not be null");
		if (pubSubDomain) {
			return resolveTopic(session, destinationName);
		}
		else {
			return resolveQueue(session, destinationName);
		}
	}


	/**
	 * Resolve the given destination name to a {@link Topic}.
	 * @param session the current JMS Session
	 * @param topicName the name of the desired {@link Topic}
	 * @return the JMS {@link Topic}
	 * @throws javax.jms.JMSException if resolution failed
	 * @see Session#createTopic(String)
	 */
	protected Topic resolveTopic(Session session, String topicName) throws JMSException {
		if (session instanceof TopicSession) {
			// Cast to TopicSession: will work on both JMS 1.1 and 1.0.2
			return ((TopicSession) session).createTopic(topicName);
		}
		else {
			// Fall back to generic JMS Session: will only work on JMS 1.1
			return session.createTopic(topicName);
		}
	}

	/**
	 * Resolve the given destination name to a {@link Queue}.
	 * @param session the current JMS Session
	 * @param queueName the name of the desired {@link Queue}
	 * @return the JMS {@link Queue}
	 * @throws javax.jms.JMSException if resolution failed
	 * @see Session#createQueue(String)
	 */
	protected Queue resolveQueue(Session session, String queueName) throws JMSException {
		if (session instanceof QueueSession) {
			// Cast to QueueSession: will work on both JMS 1.1 and 1.0.2
			return ((QueueSession) session).createQueue(queueName);
		}
		else {
			// Fall back to generic JMS Session: will only work on JMS 1.1
			return session.createQueue(queueName);
		}
	}

}
