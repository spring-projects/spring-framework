/*
 * Copyright 2002-2016 the original author or authors.
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

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;

import org.springframework.lang.Nullable;

/**
 * An extended {@link MessageConverter} SPI with conversion hint support.
 *
 * <p>In case of a conversion hint being provided, the framework will call
 * the extended method if a converter implements this interface, instead
 * of calling the regular {@code toMessage} variant.
 *
 * @author Stephane Nicoll
 * @since 4.3
 */
public interface SmartMessageConverter extends MessageConverter {

	/**
	 * A variant of {@link #toMessage(Object, Session)} which takes an extra conversion
	 * context as an argument, allowing to take e.g. annotations on a payload parameter
	 * into account.
	 * @param object the object to convert
	 * @param session the Session to use for creating a JMS Message
	 * @param conversionHint an extra object passed to the {@link MessageConverter},
	 * e.g. the associated {@code MethodParameter} (may be {@code null}}
	 * @return the JMS Message
	 * @throws jakarta.jms.JMSException if thrown by JMS API methods
	 * @throws MessageConversionException in case of conversion failure
	 * @see #toMessage(Object, Session)
	 */
	Message toMessage(Object object, Session session, @Nullable Object conversionHint)
			throws JMSException, MessageConversionException;

}
