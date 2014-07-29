/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.jms.InvalidDestinationException;
import org.springframework.jms.JmsException;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.destination.DestinationResolutionException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.MessagingExceptionTranslator;

/**
 * {@link MessagingExceptionTranslator} capable of translating {@link JmsException}
 * instances to Spring's {@link MessagingException} hierarchy.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class JmsMessagingExceptionTranslator implements MessagingExceptionTranslator {

	@Override
	public MessagingException translateExceptionIfPossible(RuntimeException ex) {
		if (ex instanceof JmsException) {
			return convertJmsException((JmsException) ex);
		}
		return null;
	}

	private MessagingException convertJmsException(JmsException ex) {
		if (ex instanceof DestinationResolutionException ||
				ex instanceof InvalidDestinationException) {
			return new org.springframework.messaging.core.DestinationResolutionException(ex.getMessage(), ex);
		}
		if (ex instanceof MessageConversionException) {
			return new org.springframework.messaging.converter.MessageConversionException(ex.getMessage(), ex);
		}


		// Fallback
		return new MessagingException(ex.getMessage(), ex);
	}
}
