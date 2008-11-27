/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jms.core;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * Callback for sending a message to a JMS destination.
 *
 * <p>To be used with JmsTemplate's callback methods that take a ProducerCallback
 * argument, often implemented as an anonymous inner class.
 *
 * <p>The typical implementation will perform multiple operations on the
 * supplied JMS {@link Session} and {@link MessageProducer}. When used with
 * a 1.0.2 provider, you need to downcast to the appropriate domain
 * implementation, either {@link javax.jms.QueueSender} or
 * {@link javax.jms.TopicPublisher}, to actually send a message.
 *
 * @author Mark Pollack
 * @since 1.1
 * @see JmsTemplate#execute(ProducerCallback)
 * @see JmsTemplate#execute(javax.jms.Destination, ProducerCallback)
 * @see JmsTemplate#execute(String, ProducerCallback)
 */
public interface ProducerCallback<T> {

	/**
	 * Perform operations on the given {@link Session} and {@link MessageProducer}.
	 * <p>The message producer is not associated with any destination unless
	 * when specified in the JmsTemplate call.
	 * @param session the JMS <code>Session</code> object to use
	 * @param producer the JMS <code>MessageProducer</code> object to use
	 * @return a result object from working with the <code>Session</code>, if any (can be <code>null</code>) 
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	T doInJms(Session session, MessageProducer producer) throws JMSException;

}
