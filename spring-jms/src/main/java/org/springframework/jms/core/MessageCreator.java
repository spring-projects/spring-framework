/*
 * Copyright 2002-2016 the original author or authors.
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
import javax.jms.Message;
import javax.jms.Session;

/**
 * Creates a JMS message given a {@link Session}.
 *
 * <p>The {@code Session} typically is provided by an instance
 * of the {@link JmsTemplate} class.
 *
 * <p>Implementations <i>do not</i> need to concern themselves with
 * checked {@code JMSExceptions} (from the '{@code javax.jms}'
 * package) that may be thrown from operations they attempt. The
 * {@code JmsTemplate} will catch and handle these
 * {@code JMSExceptions} appropriately.
 *
 * @author Mark Pollack
 * @since 1.1
 */
@FunctionalInterface
public interface MessageCreator {

	/**
	 * Create a {@link Message} to be sent.
	 * @param session the JMS {@link Session} to be used to create the
	 * {@code Message} (never {@code null})
	 * @return the {@code Message} to be sent
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	Message createMessage(Session session) throws JMSException;

}
