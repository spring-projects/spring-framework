/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jms.core;

import javax.jms.JMSException;
import javax.jms.QueueBrowser;
import javax.jms.Session;

import org.springframework.lang.Nullable;

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
	 * Perform operations on the given {@link javax.jms.Session} and
	 * {@link javax.jms.QueueBrowser}.
	 * @param session the JMS {@code Session} object to use
	 * @param browser the JMS {@code QueueBrowser} object to use
	 * @return a result object from working with the {@code Session}, if any
	 * (or {@code null} if none)
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	@Nullable
	T doInJms(Session session, QueueBrowser browser) throws JMSException;

}
