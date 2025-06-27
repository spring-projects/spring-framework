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

package org.springframework.aop.retry.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aop.retry.MethodRetryPredicate;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.core.annotation.AliasFor;

/**
 * A common annotation specifying retry characteristics for an individual method,
 * or for all proxy-invoked methods in a given class hierarchy if annotated at
 * the type level.
 *
 * <p>Aligned with {@link org.springframework.core.retry.RetryTemplate}
 * as well as Reactor's retry support, either re-invoking an imperative
 * target method or decorating a reactive result accordingly.
 *
 * <p>Inspired by the <a href="https://github.com/spring-projects/spring-retry">Spring Retry</a>
 * project but redesigned as a minimal core retry feature in the Spring Framework.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see RetryAnnotationBeanPostProcessor
 * @see RetryAnnotationInterceptor
 * @see org.springframework.core.retry.RetryTemplate
 * @see reactor.core.publisher.Mono#retryWhen
 * @see reactor.core.publisher.Flux#retryWhen
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective
public @interface Retryable {

	/**
	 * Convenient default attribute for {@link #includes()},
	 * typically used with a single exception type to retry for.
	 */
	@AliasFor("includes")
	Class<? extends Throwable>[] value() default {};

	/**
	 * Applicable exception types to attempt a retry for. This attribute
	 * allows for the convenient specification of assignable exception types.
	 * <p>The default is empty, leading to a retry attempt for any exception.
	 * @see #excludes()
	 * @see #predicate()
	 */
	@AliasFor("value")
	Class<? extends Throwable>[] includes() default {};

	/**
	 * Non-applicable exception types to avoid a retry for. This attribute
	 * allows for the convenient specification of assignable exception types.
	 * <p>The default is empty, leading to a retry attempt for any exception.
	 * @see #includes()
	 * @see #predicate()
	 */
	Class<? extends Throwable>[] excludes() default {};

	/**
	 * A predicate for filtering applicable exceptions for which
	 * an invocation can be retried.
	 * <p>The default is a retry attempt for any exception.
	 * @see #includes()
	 * @see #excludes()
	 */
	Class<? extends MethodRetryPredicate> predicate() default MethodRetryPredicate.class;

	/**
	 * The maximum number of retry attempts, in addition to the initial invocation.
	 * <p>The default is 3.
	 */
	int maxAttempts() default 3;

	/**
	 * The base delay after the initial invocation in milliseconds.
	 * If a multiplier is specified, this serves as the initial delay to multiply from.
	 * <p>The default is 1000.
	 * @see #jitter()
	 * @see #multiplier()
	 * @see #maxDelay()
	 */
	long delay() default 1000;

	/**
	 * A jitter value (in milliseconds) for the base retry attempt, randomly
	 * subtracted or added to the calculated delay, resulting in a value between
	 * {@code delay - jitter} and {@code delay + jitter} but never below the base
	 * {@link #delay()} or above {@link #maxDelay()}.
	 * <p>If a multiplier is specified, it is applied to the jitter value as well.
	 * <p>The default is 0 (no jitter).
	 * @see #delay()
	 * @see #multiplier()
	 * @see #maxDelay()
	 */
	long jitter() default 0;

	/**
	 * A multiplier for a delay for the next retry attempt, applied
	 * to the previous delay (starting with {@link #delay()}) as well
	 * as to the applicable {@link #jitter()} for each attempt.
	 * <p>The default is 1.0, effectively resulting in a fixed delay.
	 * @see #delay()
	 * @see #jitter()
	 * @see #maxDelay()
	 */
	double multiplier() default 1.0;

	/**
	 * The maximum delay for any retry attempt (in milliseconds), limiting
	 * how far {@link #jitter()} and {@link #multiplier()} can increase the
	 * {@linkplain #delay() delay}.
	 * <p>The default is unlimited.
	 * @see #delay()
	 * @see #jitter()
	 * @see #multiplier()
	 */
	long maxDelay() default Integer.MAX_VALUE;

}
