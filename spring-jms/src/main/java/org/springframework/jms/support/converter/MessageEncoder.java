/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.jms.support.converter;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;


/**
 * Strategy interface that specifies a converter from Java objects to JMS messages.
 * 
 * @author Philippe Marschall
 * @since 5.3
 */
public interface MessageEncoder<T> {
	
	/**
	 * Convert a Java object to a JMS Message using the supplied session
	 * to create the message object.
	 * @param object the object to convert
	 * @param session the Session to use for creating a JMS Message
	 * @return the JMS Message
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 * @throws MessageConversionException in case of conversion failure
	 */
	Message toMessage(T object, Session session) throws JMSException, MessageConversionException;

}
