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

package org.springframework.jms.listener;

/**
 * Interface to be implemented by message listener objects that suggest a specific
 * name for a durable subscription that they might be registered with. Otherwise,
 * the listener class name will be used as a default subscription name.
 *
 * <p>Applies to {@link jakarta.jms.MessageListener} objects as well as to
 * {@link SessionAwareMessageListener} objects and plain listener methods
 * (as supported by {@link org.springframework.jms.listener.adapter.MessageListenerAdapter}).
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 */
public interface SubscriptionNameProvider {

	/**
	 * Determine the subscription name for this message listener object.
	 */
	String getSubscriptionName();

}
