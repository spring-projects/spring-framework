/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.transaction.reactive;

import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.PayloadApplicationEvent;

/**
 * A delegate for publishing transactional events in a reactive setup.
 * Includes the current Reactor-managed {@link TransactionContext} as
 * a source object for every {@link ApplicationEvent} to be published.
 *
 * <p>This delegate is just a convenience. The current {@link TransactionContext}
 * can be directly included as the event source as well, and then published
 * through an {@link ApplicationEventPublisher} such as the Spring
 * {@link org.springframework.context.ApplicationContext}:
 *
 * <pre class="code">
 * TransactionContextManager.currentContext()
 *     .map(source -> new PayloadApplicationEvent&lt;&gt;(source, "myPayload"))
 *     .doOnSuccess(this.eventPublisher::publishEvent)
 * </pre>
 *
 * @author Juergen Hoeller
 * @since 6.1
 * @see #publishEvent(Function)
 * @see #publishEvent(Object)
 * @see ApplicationEventPublisher
 */
public class TransactionalEventPublisher {

	private final ApplicationEventPublisher eventPublisher;


	/**
	 * Create a new delegate for publishing transactional events in a reactive setup.
	 * @param eventPublisher the actual event publisher to use,
	 * typically a Spring {@link org.springframework.context.ApplicationContext}
	 */
	public TransactionalEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}


	/**
	 * Publish an event created through the given function which maps the transaction
	 * source object (the {@link TransactionContext}) to the event instance.
	 * @param eventCreationFunction a function mapping the source object to the event instance,
	 * e.g. {@code source -> new PayloadApplicationEvent&lt;&gt;(source, "myPayload")}
	 * @return the Reactor {@link Mono} for the transactional event publication
	 */
	public Mono<Void> publishEvent(Function<TransactionContext, ApplicationEvent> eventCreationFunction) {
		return TransactionContextManager.currentContext().map(eventCreationFunction)
				.doOnSuccess(this.eventPublisher::publishEvent).then();
	}

	/**
	 * Publish an event created for the given payload.
	 * @param payload the payload to publish as an event
	 * @return the Reactor {@link Mono} for the transactional event publication
	 */
	public Mono<Void> publishEvent(Object payload) {
		if (payload instanceof ApplicationEvent) {
			return Mono.error(new IllegalArgumentException("Cannot publish ApplicationEvent with transactional " +
					"source - publish payload object or use publishEvent(Function<Object, ApplicationEvent>"));
		}
		return publishEvent(source -> new PayloadApplicationEvent<>(source, payload));
	}

}
