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

package org.springframework.messaging;

/**
 * Interface implemented by Spring integrations with messaging technologies
 * that throw runtime exceptions, such as JMS, STOMP and AMQP.
 *
 * <p>This allows consistent usage of combined exception translation functionality,
 * without forcing a single translator to understand every single possible type
 * of exception.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface MessagingExceptionTranslator {

	/**
	 * Translate the given runtime exception thrown by a messaging implementation
	 * to a corresponding exception from Spring's generic {@link MessagingException}
	 * hierarchy, if possible.
	 * <p>Do not translate exceptions that are not understand by this translator:
	 * for example, if resulting from user code and unrelated to messaging.
	 * @param ex a RuntimeException thrown
	 * @return the corresponding MessagingException (or {@code null} if the
	 * exception could not be translated, as in this case it may result from
	 * user code rather than an actual messaging problem)
	 */
	MessagingException translateExceptionIfPossible(RuntimeException ex);
}
