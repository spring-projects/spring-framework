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

package org.springframework.jms.core;

import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import org.jspecify.annotations.Nullable;

/**
 * Callback for sending a message to a JMS destination.
 *
 * <p>To be used with {@link JmsTemplate}'s callback methods that take a
 * {@link ProducerCallback} argument, often implemented as an anonymous
 * inner class or as a lambda expression.
 *
 * <p>The typical implementation will perform multiple operations on the
 * supplied JMS {@link Session} and {@link MessageProducer}.
 *
 * @author Mark Pollack
 * @since 1.1
 * @param <T> the result type
 * @see JmsTemplate#execute(ProducerCallback)
 * @see JmsTemplate#execute(jakarta.jms.Destination, ProducerCallback)
 * @see JmsTemplate#execute(String, ProducerCallback)
 */
@FunctionalInterface
public interface ProducerCallback<T> {

	/**
	 * Perform operations on the given {@link Session} and {@link MessageProducer}.
	 * <p>The message producer is not associated with any destination unless
	 * when specified in the JmsTemplate call.
	 * @param session the JMS {@code Session} object to use
	 * @param producer the JMS {@code MessageProducer} object to use
	 * @return a result object from working with the {@code Session}, if any
	 * (or {@code null} if none)
	 * @throws jakarta.jms.JMSException if thrown by JMS API methods
	 */
	@Nullable T doInJms(Session session, MessageProducer producer) throws JMSException;

}
