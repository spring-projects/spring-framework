/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jms.config;

import org.springframework.jms.listener.MessageListenerContainer;

/**
 * Model for a JMS listener endpoint. Can be used against a
 * {@link org.springframework.jms.annotation.JmsListenerConfigurer
 * JmsListenerConfigurer} to register endpoints programmatically.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface JmsListenerEndpoint {

	/**
	 * Return the id of this endpoint.
	 */
	String getId();

	/**
	 * Setup the specified message listener container with the model
	 * defined by this endpoint.
	 * <p>This endpoint must provide the requested missing option(s) of
	 * the specified container to make it usable. Usually, this is about
	 * setting the {@code destination} and the {@code messageListener} to
	 * use but an implementation may override any default setting that
	 * was already set.
	 * @param listenerContainer the listener container to configure
	 */
	void setupListenerContainer(MessageListenerContainer listenerContainer);

}
