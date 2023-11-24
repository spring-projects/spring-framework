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

package org.springframework.context;

/**
 * Interface that encapsulates event publication functionality.
 *
 * <p>Serves as a super-interface for {@link ApplicationContext}.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 1.1.1
 * @see ApplicationContext
 * @see ApplicationEventPublisherAware
 * @see org.springframework.context.ApplicationEvent
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.event.EventPublicationInterceptor
 * @see org.springframework.transaction.event.TransactionalApplicationListener
 */
@FunctionalInterface
public interface ApplicationEventPublisher {

	/**
	 * Notify all <strong>matching</strong> listeners registered with this
	 * application of an application event. Events may be framework events
	 * (such as ContextRefreshedEvent) or application-specific events.
	 * <p>Such an event publication step is effectively a hand-off to the
	 * multicaster and does not imply synchronous/asynchronous execution
	 * or even immediate execution at all. Event listeners are encouraged
	 * to be as efficient as possible, individually using asynchronous
	 * execution for longer-running and potentially blocking operations.
	 * <p>For usage in a reactive call stack, include event publication
	 * as a simple hand-off:
	 * {@code Mono.fromRunnable(() -> eventPublisher.publishEvent(...))}.
	 * As with any asynchronous execution, thread-local data is not going
	 * to be available for reactive listener methods. All state which is
	 * necessary to process the event needs to be included in the event
	 * instance itself.
	 * <p>For the convenient inclusion of the current transaction context
	 * in a reactive hand-off, consider using
	 * {@link org.springframework.transaction.reactive.TransactionalEventPublisher#publishEvent(Function)}.
	 * For thread-bound transactions, this is not necessary since the
	 * state will be implicitly available through thread-local storage.
	 * @param event the event to publish
	 * @see #publishEvent(Object)
	 * @see ApplicationListener#supportsAsyncExecution()
	 * @see org.springframework.context.event.ContextRefreshedEvent
	 * @see org.springframework.context.event.ContextClosedEvent
	 */
	default void publishEvent(ApplicationEvent event) {
		publishEvent((Object) event);
	}

	/**
	 * Notify all <strong>matching</strong> listeners registered with this
	 * application of an event.
	 * <p>If the specified {@code event} is not an {@link ApplicationEvent},
	 * it is wrapped in a {@link PayloadApplicationEvent}.
	 * <p>Such an event publication step is effectively a hand-off to the
	 * multicaster and does not imply synchronous/asynchronous execution
	 * or even immediate execution at all. Event listeners are encouraged
	 * to be as efficient as possible, individually using asynchronous
	 * execution for longer-running and potentially blocking operations.
	 * <p>For the convenient inclusion of the current transaction context
	 * in a reactive hand-off, consider using
	 * {@link org.springframework.transaction.reactive.TransactionalEventPublisher#publishEvent(Object)}.
	 * For thread-bound transactions, this is not necessary since the
	 * state will be implicitly available through thread-local storage.
	 * @param event the event to publish
	 * @since 4.2
	 * @see #publishEvent(ApplicationEvent)
	 * @see PayloadApplicationEvent
	 */
	void publishEvent(Object event);

}
