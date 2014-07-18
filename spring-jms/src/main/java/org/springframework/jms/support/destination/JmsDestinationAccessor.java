/*
 * Copyright 2002-2014 the original author or authors.
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
import javax.jms.Session;

import org.springframework.jms.support.JmsAccessor;
import org.springframework.util.Assert;

/**
 * Base class for {@link org.springframework.jms.core.JmsTemplate} and other
 * JMS-accessing gateway helpers, adding destination-related properties to
 * {@link JmsAccessor JmsAccessor's} common properties.
 *
 * <p>Not intended to be used directly.
 * See {@link org.springframework.jms.core.JmsTemplate}.
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 * @see org.springframework.jms.support.JmsAccessor
 * @see org.springframework.jms.core.JmsTemplate
 */
public abstract class JmsDestinationAccessor extends JmsAccessor {

	private DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private boolean pubSubDomain = false;


	/**
	 * Set the {@link DestinationResolver} that is to be used to resolve
	 * {@link javax.jms.Destination} references for this accessor.
	 * <p>The default resolver is a DynamicDestinationResolver. Specify a
	 * JndiDestinationResolver for resolving destination names as JNDI locations.
	 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
	 * @see org.springframework.jms.support.destination.JndiDestinationResolver
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		Assert.notNull(destinationResolver, "'destinationResolver' must not be null");
		this.destinationResolver = destinationResolver;
	}

	/**
	 * Return the DestinationResolver for this accessor (never {@code null}).
	 */
	public DestinationResolver getDestinationResolver() {
		return this.destinationResolver;
	}

	/**
	 * Configure the destination accessor with knowledge of the JMS domain used.
	 * Default is Point-to-Point (Queues).
	 * <p>This setting primarily indicates what type of destination to resolve
	 * if dynamic destinations are enabled.
	 * @param pubSubDomain "true" for the Publish/Subscribe domain ({@link javax.jms.Topic Topics}),
	 * "false" for the Point-to-Point domain ({@link javax.jms.Queue Queues})
	 * @see #setDestinationResolver
	 */
	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	/**
	 * Return whether the Publish/Subscribe domain ({@link javax.jms.Topic Topics}) is used.
	 * Otherwise, the Point-to-Point domain ({@link javax.jms.Queue Queues}) is used.
	 */
	public boolean isPubSubDomain() {
		return this.pubSubDomain;
	}


	/**
	 * Resolve the given destination name into a JMS {@link Destination},
	 * via this accessor's {@link DestinationResolver}.
	 * @param session the current JMS {@link Session}
	 * @param destinationName the name of the destination
	 * @return the located {@link Destination}
	 * @throws javax.jms.JMSException if resolution failed
	 * @see #setDestinationResolver
	 */
	protected Destination resolveDestinationName(Session session, String destinationName) throws JMSException {
		return getDestinationResolver().resolveDestinationName(session, destinationName, isPubSubDomain());
	}

}
