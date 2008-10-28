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

package org.springframework.jms.listener.serversession;

import javax.jms.JMSException;
import javax.jms.Session;

/**
 * SPI interface for creating and executing JMS Sessions,
 * pre-populated with a specific MessageListener.
 * Implemented by ServerSessionMessageListenerContainer,
 * accessed by ServerSessionFactory implementations.
 *
 * <p>Effectively, an instance that implements this interface
 * represents a message listener container for a specific
 * listener and destination.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated as of Spring 2.5, in favor of DefaultMessageListenerContainer
 * and JmsMessageEndpointManager. To be removed in Spring 3.0.
 * @see ServerSessionFactory
 * @see ServerSessionMessageListenerContainer
 */
public interface ListenerSessionManager {

	/**
	 * Create a new JMS Session, pre-populated with this manager's
	 * MessageListener.
	 * @return the new JMS Session
	 * @throws JMSException if Session creation failed
	 * @see javax.jms.Session#setMessageListener(javax.jms.MessageListener)
	 */
	Session createListenerSession() throws JMSException;

	/**
	 * Execute the given JMS Session, triggering its MessageListener
	 * with pre-loaded messages.
	 * @param session the JMS Session to invoke
	 * @see javax.jms.Session#run()
	 */
	void executeListenerSession(Session session);

}
