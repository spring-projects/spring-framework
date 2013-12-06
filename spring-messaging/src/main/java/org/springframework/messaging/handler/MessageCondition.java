/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging.handler;

import org.springframework.messaging.Message;

/**
 * Contract for mapping conditions to messages.
 *
 * <p>Message conditions can be combined (e.g. type + method-level conditions),
 * matched to a specific Message, as well as compared to each other in the
 * context of a Message to determine which one matches a request more closely.
 *
 * @param <T> The kind of condition that this condition can be combined with or compared to
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface MessageCondition<T> {

	/**
	 * Define the rules for combining this condition with another.
	 * For example combining type- and method-level conditions.
	 * @param other the condition to combine with
	 * @return the resulting message condition
	 */
	T combine(T other);

	/**
	 * Check if this condition matches the given Message and returns a
	 * potentially new condition with content tailored to the current message.
	 * For example a condition with destination patterns might return a new
	 * condition with sorted, matching patterns only.
	 * @return a condition instance in case of a match; or {@code null} if there is no match.
	 */
	T getMatchingCondition(Message<?> message);

	/**
	 * Compare this condition to another in the context of a specific message.
	 * It is assumed both instances have been obtained via
	 * {@link #getMatchingCondition(Message)} to ensure they have content
	 * relevant to current message only.
	 */
	int compareTo(T other, Message<?> message);

}
