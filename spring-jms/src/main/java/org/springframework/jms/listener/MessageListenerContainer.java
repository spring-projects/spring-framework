/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.context.SmartLifecycle;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * Internal abstraction used by the framework representing a message
 * listener container. Not meant to be implemented externally with
 * support for both JMS and JCA style containers.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface MessageListenerContainer extends SmartLifecycle {

	/**
	 * Setup the message listener to use. Throws an {@link IllegalArgumentException}
	 * if that message listener type is not supported.
	 */
	void setupMessageListener(Object messageListener);

	/**
	 * Return the {@link MessageConverter} that can be used to
	 * convert {@link javax.jms.Message}, if any.
	 */
	MessageConverter getMessageConverter();

	/**
	 * Return the {@link DestinationResolver} to use to resolve
	 * destinations by names.
	 */
	DestinationResolver getDestinationResolver();

	/**
	 * Return whether the Publish/Subscribe domain ({@link javax.jms.Topic Topics}) is used.
	 * Otherwise, the Point-to-Point domain ({@link javax.jms.Queue Queues}) is used.
	 */
	boolean isPubSubDomain();

	/**
	 * Return whether the reply destination uses Publish/Subscribe domain
	 * ({@link javax.jms.Topic Topics}). Otherwise, the Point-to-Point domain
	 * ({@link javax.jms.Queue Queues}) is used.
	 * <p>By default, the value is identical to {@link #isPubSubDomain()}.
	 */
	boolean isReplyPubSubDomain();

}
