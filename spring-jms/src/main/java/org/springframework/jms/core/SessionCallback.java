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
import jakarta.jms.Session;
import org.jspecify.annotations.Nullable;

/**
 * Callback for executing any number of operations on a provided {@link Session}.
 *
 * <p>To be used with the {@link JmsTemplate#execute(SessionCallback)} method,
 * often implemented as an anonymous inner class or as a lambda expression.
 *
 * @author Mark Pollack
 * @since 1.1
 * @param <T> the result type
 * @see JmsTemplate#execute(SessionCallback)
 */
@FunctionalInterface
public interface SessionCallback<T> {

	/**
	 * Execute any number of operations against the supplied JMS {@link Session},
	 * possibly returning a result.
	 * @param session the JMS {@code Session}
	 * @return a result object from working with the {@code Session}, if any
	 * (or {@code null} if none)
	 * @throws jakarta.jms.JMSException if thrown by JMS API methods
	 */
	@Nullable T doInJms(Session session) throws JMSException;

}
