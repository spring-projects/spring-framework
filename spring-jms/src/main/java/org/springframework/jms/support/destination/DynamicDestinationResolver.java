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
import jakarta.jms.Session;
import jakarta.jms.Topic;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Simple {@link DestinationResolver} implementation resolving destination names
 * as dynamic destinations.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see jakarta.jms.Session#createQueue
 * @see jakarta.jms.Session#createTopic
 */
public class DynamicDestinationResolver implements DestinationResolver {

	/**
	 * Resolve the specified destination name as a dynamic destination.
	 * @param session the current JMS Session
	 * @param destinationName the name of the destination
	 * @param pubSubDomain {@code true} if the domain is pub-sub, {@code false} if P2P
	 * @return the JMS destination (either a topic or a queue)
	 * @throws jakarta.jms.JMSException if resolution failed
	 * @see #resolveTopic(jakarta.jms.Session, String)
	 * @see #resolveQueue(jakarta.jms.Session, String)
	 */
	@Override
	public Destination resolveDestinationName(@Nullable Session session, String destinationName, boolean pubSubDomain)
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
	 * @throws jakarta.jms.JMSException if resolution failed
	 * @see Session#createTopic(String)
	 */
	protected Topic resolveTopic(Session session, String topicName) throws JMSException {
		return session.createTopic(topicName);
	}

	/**
	 * Resolve the given destination name to a {@link Queue}.
	 * @param session the current JMS Session
	 * @param queueName the name of the desired {@link Queue}
	 * @return the JMS {@link Queue}
	 * @throws jakarta.jms.JMSException if resolution failed
	 * @see Session#createQueue(String)
	 */
	protected Queue resolveQueue(Session session, String queueName) throws JMSException {
		return session.createQueue(queueName);
	}

}
