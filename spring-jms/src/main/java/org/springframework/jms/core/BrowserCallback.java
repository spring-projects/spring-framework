/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.core;

import jakarta.jms.JMSException;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import org.jspecify.annotations.Nullable;

/**
 * Callback for browsing the messages in a JMS queue.
 *
 * <p>To be used with {@link JmsTemplate}'s callback methods that take a
 * {@link BrowserCallback} argument, often implemented as an anonymous
 * inner class or as a lambda expression.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @param <T> the result type
 * @see JmsTemplate#browse(BrowserCallback)
 * @see JmsTemplate#browseSelected(String, BrowserCallback)
 */
@FunctionalInterface
public interface BrowserCallback<T> {

	/**
	 * Perform operations on the given {@link jakarta.jms.Session} and
	 * {@link jakarta.jms.QueueBrowser}.
	 * @param session the JMS {@code Session} object to use
	 * @param browser the JMS {@code QueueBrowser} object to use
	 * @return a result object from working with the {@code Session}, if any
	 * (or {@code null} if none)
	 * @throws jakarta.jms.JMSException if thrown by JMS API methods
	 */
	@Nullable T doInJms(Session session, QueueBrowser browser) throws JMSException;

}
