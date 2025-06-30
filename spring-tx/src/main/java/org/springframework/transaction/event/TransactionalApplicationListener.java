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

package org.springframework.transaction.event;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.core.Ordered;

/**
 * An {@link ApplicationListener} that is invoked according to a {@link TransactionPhase}.
 * This is a programmatic equivalent of the {@link TransactionalEventListener} annotation.
 *
 * <p>Adding {@link org.springframework.core.Ordered} to your listener implementation
 * allows you to prioritize that listener amongst other listeners running before or after
 * transaction completion.
 *
 * <p>As of 6.1, transactional event listeners can work with thread-bound transactions managed
 * by a {@link org.springframework.transaction.PlatformTransactionManager} as well as reactive
 * transactions managed by a {@link org.springframework.transaction.ReactiveTransactionManager}.
 * For the former, listeners are guaranteed to see the current thread-bound transaction.
 * Since the latter uses the Reactor context instead of thread-local variables, the transaction
 * context needs to be included in the published event instance as the event source:
 * see {@link org.springframework.transaction.reactive.TransactionalEventPublisher}.
 *
 * @author Juergen Hoeller
 * @author Oliver Drotbohm
 * @since 5.3
 * @param <E> the specific {@code ApplicationEvent} subclass to listen to
 * @see TransactionalEventListener
 * @see TransactionalApplicationListenerAdapter
 * @see #forPayload
 */
public interface TransactionalApplicationListener<E extends ApplicationEvent>
		extends ApplicationListener<E>, Ordered {

	/**
	 * Return the execution order within transaction synchronizations.
	 * <p>Default is {@link Ordered#LOWEST_PRECEDENCE}.
	 * @see org.springframework.transaction.support.TransactionSynchronization#getOrder()
	 */
	@Override
	default int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
	 * Transaction-synchronized listeners do not support asynchronous execution,
	 * only their target listener ({@link #processEvent}) potentially does.
	 * @since 6.1
	 */
	@Override
	default boolean supportsAsyncExecution() {
		return false;
	}

	/**
	 * Return an identifier for the listener to be able to refer to it individually.
	 * <p>It might be necessary for specific completion callback implementations
	 * to provide a specific id, whereas for other scenarios an empty String
	 * (as the common default value) is acceptable as well.
	 * @see org.springframework.context.event.SmartApplicationListener#getListenerId()
	 * @see TransactionalEventListener#id
	 * @see #addCallback
	 */
	default String getListenerId() {
		return "";
	}

	/**
	 * Return the {@link TransactionPhase} in which the listener will be invoked.
	 * <p>The default phase is {@link TransactionPhase#AFTER_COMMIT}.
	 */
	default TransactionPhase getTransactionPhase() {
		return TransactionPhase.AFTER_COMMIT;
	}

	/**
	 * Add a callback to be invoked on processing within transaction synchronization,
	 * i.e. when {@link #processEvent} is being triggered during actual transactions.
	 * @param callback the synchronization callback to apply
	 */
	void addCallback(SynchronizationCallback callback);

	/**
	 * Immediately process the given {@link ApplicationEvent}. In contrast to
	 * {@link #onApplicationEvent(ApplicationEvent)}, a call to this method will
	 * directly process the given event without deferring it to the associated
	 * {@link #getTransactionPhase() transaction phase}.
	 * @param event the event to process through the target listener implementation
	 */
	void processEvent(E event);


	/**
	 * Create a new {@code TransactionalApplicationListener} for the given payload consumer,
	 * to be applied in the default phase {@link TransactionPhase#AFTER_COMMIT}.
	 * @param consumer the event payload consumer
	 * @param <T> the type of the event payload
	 * @return a corresponding {@code TransactionalApplicationListener} instance
	 * @see PayloadApplicationEvent#getPayload()
	 * @see TransactionalApplicationListenerAdapter
	 */
	static <T> TransactionalApplicationListener<PayloadApplicationEvent<T>> forPayload(Consumer<T> consumer) {
		return forPayload(TransactionPhase.AFTER_COMMIT, consumer);
	}

	/**
	 * Create a new {@code TransactionalApplicationListener} for the given payload consumer.
	 * @param phase the transaction phase in which to invoke the listener
	 * @param consumer the event payload consumer
	 * @param <T> the type of the event payload
	 * @return a corresponding {@code TransactionalApplicationListener} instance
	 * @see PayloadApplicationEvent#getPayload()
	 * @see TransactionalApplicationListenerAdapter
	 */
	static <T> TransactionalApplicationListener<PayloadApplicationEvent<T>> forPayload(
			TransactionPhase phase, Consumer<T> consumer) {

		TransactionalApplicationListenerAdapter<PayloadApplicationEvent<T>> listener =
				new TransactionalApplicationListenerAdapter<>(event -> consumer.accept(event.getPayload()));
		listener.setTransactionPhase(phase);
		return listener;
	}


	/**
	 * Callback to be invoked on synchronization-driven event processing,
	 * wrapping the target listener invocation ({@link #processEvent}).
	 *
	 * @see #addCallback
	 * @see #processEvent
	 */
	interface SynchronizationCallback {

		/**
		 * Called before transactional event listener invocation.
		 * @param event the event that transaction synchronization is about to process
		 */
		default void preProcessEvent(ApplicationEvent event) {
		}

		/**
		 * Called after a transactional event listener invocation.
		 * @param event the event that transaction synchronization finished processing
		 * @param ex an exception that occurred during listener invocation, if any
		 */
		default void postProcessEvent(ApplicationEvent event, @Nullable Throwable ex) {
		}
	}

}
