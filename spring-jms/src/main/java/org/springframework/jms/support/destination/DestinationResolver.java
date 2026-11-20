/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.support.destination;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.jspecify.annotations.Nullable;

/**
 * Strategy interface for resolving JMS destinations.
 *
 * <p>Used by {@link org.springframework.jms.core.JmsTemplate} for resolving
 * destination names from simple {@link String Strings} to actual
 * {@link Destination} implementation instances.
 *
 * <p>The default {@link DestinationResolver} implementation used by
 * {@link org.springframework.jms.core.JmsTemplate} instances is the
 * {@link SimpleDestinationResolver} class. Consider using the
 * {@link JndiDestinationResolver} for more advanced scenarios.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.jms.core.JmsTemplate#setDestinationResolver
 * @see org.springframework.jms.support.destination.SimpleDestinationResolver
 * @see org.springframework.jms.support.destination.JndiDestinationResolver
 */
@FunctionalInterface
public interface DestinationResolver {

	/**
	 * Resolve the given destination name, either as located resource
	 * or as dynamic destination.
	 * @param session the current JMS Session
	 * (may be {@code null} if the resolver implementation is able to work without it)
	 * @param destinationName the name of the destination
	 * @param pubSubDomain {@code true} if the domain is pub-sub, {@code false} if P2P
	 * @return the JMS destination (either a topic or a queue)
	 * @throws jakarta.jms.JMSException if the JMS Session failed to resolve the destination
	 * @throws DestinationResolutionException in case of general destination resolution failure
	 */
	Destination resolveDestinationName(@Nullable Session session, String destinationName, boolean pubSubDomain)
			throws JMSException;

}
