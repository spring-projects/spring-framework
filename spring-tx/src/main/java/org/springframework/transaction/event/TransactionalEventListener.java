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

package org.springframework.transaction.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;

/**
 * An {@link EventListener} that is invoked according to a {@link TransactionPhase}.
 * This is an annotation-based equivalent of {@link TransactionalApplicationListener}.
 *
 * <p>If the event is not published within an active transaction, the event is discarded
 * unless the {@link #fallbackExecution} flag is explicitly set. If a transaction is
 * running, the event is handled according to its {@code TransactionPhase}.
 *
 * <p>Adding {@link org.springframework.core.annotation.Order @Order} to your annotated
 * method allows you to prioritize that listener amongst other listeners running before
 * or after transaction completion.
 *
 * <p>As of 6.1, transactional event listeners can work with thread-bound transactions managed
 * by a {@link org.springframework.transaction.PlatformTransactionManager} as well as reactive
 * transactions managed by a {@link org.springframework.transaction.ReactiveTransactionManager}.
 * For the former, listeners are guaranteed to see the current thread-bound transaction.
 * Since the latter uses the Reactor context instead of thread-local variables, the transaction
 * context needs to be included in the published event instance as the event source:
 * see {@link org.springframework.transaction.reactive.TransactionalEventPublisher}.
 *
 * <p><strong>WARNING:</strong> if the {@code TransactionPhase} is set to
 * {@link TransactionPhase#AFTER_COMMIT AFTER_COMMIT} (the default),
 * {@link TransactionPhase#AFTER_ROLLBACK AFTER_ROLLBACK}, or
 * {@link TransactionPhase#AFTER_COMPLETION AFTER_COMPLETION}, the transaction will
 * have been committed or rolled back already, but the transactional resources might
 * still be active and accessible. As a consequence, any data access code triggered
 * at this point will still "participate" in the original transaction, but changes
 * will not be committed to the transactional resource. See
 * {@link org.springframework.transaction.support.TransactionSynchronization#afterCompletion(int)
 * TransactionSynchronization.afterCompletion(int)} for details.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @since 4.2
 * @see TransactionalApplicationListener
 * @see TransactionalApplicationListenerMethodAdapter
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EventListener
public @interface TransactionalEventListener {

	/**
	 * Phase to bind the handling of an event to.
	 * <p>The default phase is {@link TransactionPhase#AFTER_COMMIT}.
	 * <p>If no transaction is in progress, the event is not processed at
	 * all unless {@link #fallbackExecution} has been enabled explicitly.
	 */
	TransactionPhase phase() default TransactionPhase.AFTER_COMMIT;

	/**
	 * Whether the event should be handled if no transaction is running.
	 */
	boolean fallbackExecution() default false;

	/**
	 * Alias for {@link #classes}.
	 */
	@AliasFor(annotation = EventListener.class, attribute = "classes")
	Class<?>[] value() default {};

	/**
	 * The event classes that this listener handles.
	 * <p>If this attribute is specified with a single value, the annotated
	 * method may optionally accept a single parameter. However, if this
	 * attribute is specified with multiple values, the annotated method
	 * must <em>not</em> declare any parameters.
	 */
	@AliasFor(annotation = EventListener.class, attribute = "classes")
	Class<?>[] classes() default {};

	/**
	 * Spring Expression Language (SpEL) attribute used for making the event
	 * handling conditional.
	 * <p>The default is {@code ""}, meaning the event is always handled.
	 * @see EventListener#condition
	 */
	@AliasFor(annotation = EventListener.class, attribute = "condition")
	String condition() default "";

	/**
	 * An optional identifier for the listener, defaulting to the fully-qualified
	 * signature of the declaring method (e.g. "mypackage.MyClass.myMethod()").
	 * @since 5.3
	 * @see EventListener#id
	 * @see TransactionalApplicationListener#getListenerId()
	 */
	@AliasFor(annotation = EventListener.class, attribute = "id")
	String id() default "";

}
