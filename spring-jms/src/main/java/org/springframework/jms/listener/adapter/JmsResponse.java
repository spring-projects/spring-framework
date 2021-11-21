/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.jms.listener.adapter;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Return type of any JMS listener method used to indicate the actual response
 * destination alongside the response itself. Typically used when said destination
 * needs to be computed at runtime.
 *
 * <p>The example below sends a response with the content of the {@code result}
 * argument to the {@code queueOut Queue}:
 *
 * <pre class="code">
 * package com.acme.foo;
 *
 * public class MyService {
 *     &#064;JmsListener
 *     public JmsResponse process(String msg) {
 *         // process incoming message
 *         return JmsResponse.forQueue(result, "queueOut");
 *     }
 * }</pre>
 *
 * If the destination does not need to be computed at runtime,
 * {@link org.springframework.messaging.handler.annotation.SendTo @SendTo}
 * is the recommended declarative approach.
 *
 * @author Stephane Nicoll
 * @since 4.2
 * @param <T> the type of the response
 * @see org.springframework.jms.annotation.JmsListener
 * @see org.springframework.messaging.handler.annotation.SendTo
 */
public class JmsResponse<T> {

	private final T response;

	private final Object destination;


	/**
	 * Create a new {@link JmsResponse} instance.
	 * @param response the content of the result
	 * @param destination the destination
	 */
	protected JmsResponse(T response, Object destination) {
		Assert.notNull(response, "Result must not be null");
		this.response = response;
		this.destination = destination;
	}


	/**
	 * Return the content of the response.
	 */
	public T getResponse() {
		return this.response;
	}

	/**
	 * Resolve the {@link Destination} to use for this instance. The {@link DestinationResolver}
	 * and {@link Session} can be used to resolve a destination at runtime.
	 * @param destinationResolver the destination resolver to use if necessary
	 * @param session the session to use, if necessary
	 * @return the {@link Destination} to use
	 * @throws JMSException if the DestinationResolver failed to resolve the destination
	 */
	@Nullable
	public Destination resolveDestination(DestinationResolver destinationResolver, Session session)
			throws JMSException {

		if (this.destination instanceof Destination) {
			return (Destination) this.destination;
		}
		if (this.destination instanceof DestinationNameHolder nameHolder) {
			return destinationResolver.resolveDestinationName(session,
					nameHolder.destinationName, nameHolder.pubSubDomain);
		}
		return null;
	}

	@Override
	public String toString() {
		return "JmsResponse [" + "response=" + this.response + ", destination=" + this.destination + ']';
	}


	/**
	 * Create a {@link JmsResponse} targeting the queue with the specified name.
	 */
	public static <T> JmsResponse<T> forQueue(T result, String queueName) {
		Assert.notNull(queueName, "Queue name must not be null");
		return new JmsResponse<>(result, new DestinationNameHolder(queueName, false));
	}

	/**
	 * Create a {@link JmsResponse} targeting the topic with the specified name.
	 */
	public static <T> JmsResponse<T> forTopic(T result, String topicName) {
		Assert.notNull(topicName, "Topic name must not be null");
		return new JmsResponse<>(result, new DestinationNameHolder(topicName, true));
	}

	/**
	 * Create a {@link JmsResponse} targeting the specified {@link Destination}.
	 */
	public static <T> JmsResponse<T> forDestination(T result, Destination destination) {
		Assert.notNull(destination, "Destination must not be null");
		return new JmsResponse<>(result, destination);
	}


	/**
	 * Internal class combining a destination name
	 * and its target destination type (queue or topic).
	 */
	private static class DestinationNameHolder {

		private final String destinationName;

		private final boolean pubSubDomain;

		public DestinationNameHolder(String destinationName, boolean pubSubDomain) {
			this.destinationName = destinationName;
			this.pubSubDomain = pubSubDomain;
		}

		@Override
		public String toString() {
			return this.destinationName + "{" + "pubSubDomain=" + this.pubSubDomain + '}';
		}
	}

}
