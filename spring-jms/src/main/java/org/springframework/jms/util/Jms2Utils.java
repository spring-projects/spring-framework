/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jms.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;


/**
 * The is a utility class to help invoke JMS 2.0 api methods using reflection.  
 * This class requires that the JMS 2.0 jar be on the classpath to work properly.
 * 
 * @author Juergen Hoeller
 * @author Chris Shannon
 * @since 4.1
 */
public class Jms2Utils {

	private static final Method createSharedConsumerMethod = ClassUtils.getMethodIfAvailable(
			Session.class, "createSharedConsumer", Topic.class, String.class, String.class);

	private static final Method createSharedDurableConsumerMethod = ClassUtils.getMethodIfAvailable(
			Session.class, "createSharedDurableConsumer", Topic.class, String.class, String.class);
	
	/**
	 * Utility method to create a shared consumer using the JMS 2.0 api
	 * 
	 * @param session the JMS Session to create a MessageConsumer for
	 * @param topic the JMS Topic to create a MessageConsumer for
	 * @param sharedSubscriptionName the shared subscription name
	 * @param messageSelector the JMS message selector expression (or {@code null} if none)
	 * @param durable whether the subscription is durable or not
	 * @return a new shared MessageConsumer
	 * @throws JMSException
	 */
	public static MessageConsumer createSharedConsumer(Session session, Topic topic, 
			String sharedSubscriptionName, String messageSelector, boolean durable) throws JMSException {
		
		Method method = (durable ? createSharedDurableConsumerMethod : createSharedConsumerMethod);
		MessageConsumer consumer = null;
		try {
			consumer = (MessageConsumer) method.invoke(session, topic, 
					sharedSubscriptionName, messageSelector);
		}
		catch (InvocationTargetException ex) {
			if (ex.getTargetException() instanceof JMSException) {
				throw (JMSException) ex.getTargetException();
			}
			ReflectionUtils.handleInvocationTargetException(ex);
		}
		catch (IllegalAccessException ex) {
			throw new IllegalStateException("Could not access JMS 2.0 API method: " + ex.getMessage());
		}
		return consumer;
	}
}
