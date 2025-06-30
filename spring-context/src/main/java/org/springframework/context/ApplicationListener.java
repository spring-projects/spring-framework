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

package org.springframework.context;

import java.util.EventListener;
import java.util.function.Consumer;

/**
 * Interface to be implemented by application event listeners.
 *
 * <p>Based on the standard {@link java.util.EventListener} interface for the
 * Observer design pattern.
 *
 * <p>An {@code ApplicationListener} can generically declare the event type that
 * it is interested in. When registered with a Spring {@code ApplicationContext},
 * events will be filtered accordingly, with the listener getting invoked for
 * matching event objects only.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @param <E> the specific {@code ApplicationEvent} subclass to listen to
 * @see org.springframework.context.ApplicationEvent
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.event.SmartApplicationListener
 * @see org.springframework.context.event.GenericApplicationListener
 * @see org.springframework.context.event.EventListener
 */
@FunctionalInterface
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

	/**
	 * Handle an application event.
	 * @param event the event to respond to
	 */
	void onApplicationEvent(E event);

	/**
	 * Return whether this listener supports asynchronous execution.
	 * @return {@code true} if this listener instance can be executed asynchronously
	 * depending on the multicaster configuration (the default), or {@code false} if it
	 * needs to immediately run within the original thread which published the event
	 * @since 6.1
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster#setTaskExecutor
	 */
	default boolean supportsAsyncExecution() {
		return true;
	}


	/**
	 * Create a new {@code ApplicationListener} for the given payload consumer.
	 * @param consumer the event payload consumer
	 * @param <T> the type of the event payload
	 * @return a corresponding {@code ApplicationListener} instance
	 * @since 5.3
	 * @see PayloadApplicationEvent
	 */
	static <T> ApplicationListener<PayloadApplicationEvent<T>> forPayload(Consumer<T> consumer) {
		return event -> consumer.accept(event.getPayload());
	}

}
