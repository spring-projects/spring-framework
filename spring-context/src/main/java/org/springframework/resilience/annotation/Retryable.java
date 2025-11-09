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

package org.springframework.resilience.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.core.annotation.AliasFor;
import org.springframework.resilience.retry.MethodRetryPredicate;

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
 * @author Sam Brannen
 * @since 7.0
 * @see EnableResilientMethods
 * @see RetryAnnotationBeanPostProcessor
 * @see org.springframework.core.retry.RetryPolicy
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
	 * <p>The supplied exception types will be matched against an exception
	 * thrown by a failed invocation as well as nested
	 * {@linkplain Throwable#getCause() causes}.
	 * <p>This can optionally be combined with {@link #excludes() excludes} or
	 * a custom {@link #predicate() predicate}.
	 * <p>The default is empty, leading to a retry attempt for any exception.
	 * @see #excludes()
	 * @see #predicate()
	 */
	@AliasFor("value")
	Class<? extends Throwable>[] includes() default {};

	/**
	 * Non-applicable exception types to avoid a retry for. This attribute
	 * allows for the convenient specification of assignable exception types.
	 * <p>The supplied exception types will be matched against an exception
	 * thrown by a failed invocation as well as nested
	 * {@linkplain Throwable#getCause() causes}.
	 * <p>This can optionally be combined with {@link #includes() includes} or
	 * a custom {@link #predicate() predicate}.
	 * <p>The default is empty, leading to a retry attempt for any exception.
	 * @see #includes()
	 * @see #predicate()
	 */
	Class<? extends Throwable>[] excludes() default {};

	/**
	 * A predicate for filtering applicable exceptions for which an invocation can
	 * be retried.
	 * <p>A specified {@link MethodRetryPredicate} implementation will be instantiated
	 * per method. It can use dependency injection at the constructor level or through
	 * autowiring annotations, in case it needs access to other beans or facilities.
	 * <p>This can optionally be combined with {@link #includes() includes} or
	 * {@link #excludes() excludes}.
	 * <p>The default is a retry attempt for any exception.
	 * @see #includes()
	 * @see #excludes()
	 */
	Class<? extends MethodRetryPredicate> predicate() default MethodRetryPredicate.class;

	/**
	 * The maximum number of retry attempts.
	 * <p>Note that {@code total attempts = 1 initial attempt + maxRetries attempts}.
	 * Thus, if {@code maxRetries} is set to 4, the annotated method will be invoked
	 * at least once and at most 5 times.
	 * <p>The default is 3.
	 */
	long maxRetries() default 3;

	/**
	 * The maximum number of retry attempts, as a configurable String.
	 * <p>A non-empty value specified here overrides the {@link #maxRetries()} attribute.
	 * <p>This supports Spring-style "${...}" placeholders as well as SpEL expressions.
	 * @see #maxRetries()
	 */
	String maxRetriesString() default "";

	/**
	 * The base delay after the initial invocation. If a multiplier is specified,
	 * this serves as the initial delay to multiply from.
	 * <p>The time unit is milliseconds by default but can be overridden via
	 * {@link #timeUnit}.
	 * <p>Must be greater than or equal to zero. The default is 1000.
	 * @see #jitter()
	 * @see #multiplier()
	 * @see #maxDelay()
	 */
	long delay() default 1000;

	/**
	 * The base delay after the initial invocation, as a duration String.
	 * <p>A non-empty value specified here overrides the {@link #delay()} attribute.
	 * <p>The duration String can be in several formats:
	 * <ul>
	 * <li>a plain integer &mdash; which is interpreted to represent a duration in
	 * milliseconds by default unless overridden via {@link #timeUnit()} (prefer
	 * using {@link #delay()} in that case)</li>
	 * <li>any of the known {@link org.springframework.format.annotation.DurationFormat.Style
	 * DurationFormat.Style}: the {@link org.springframework.format.annotation.DurationFormat.Style#ISO8601 ISO8601}
	 * style or the {@link org.springframework.format.annotation.DurationFormat.Style#SIMPLE SIMPLE} style
	 * &mdash; using the {@link #timeUnit()} as fallback if the string doesn't contain an explicit unit</li>
	 * <li>one of the above, with Spring-style "${...}" placeholders as well as SpEL expressions</li>
	 * </ul>
	 * @return the initial delay as a String value &mdash; for example a placeholder,
	 * or a {@link org.springframework.format.annotation.DurationFormat.Style#ISO8601 java.time.Duration} compliant value
	 * or a {@link org.springframework.format.annotation.DurationFormat.Style#SIMPLE simple format} compliant value
	 * @see #delay()
	 */
	String delayString() default "";

	/**
	 * A jitter value for the base retry attempt, randomly subtracted or added to
	 * the calculated delay, resulting in a value between {@code delay - jitter}
	 * and {@code delay + jitter} but never below the base {@link #delay()} or
	 * above {@link #maxDelay()}. If a multiplier is specified, it is applied
	 * to the jitter value as well.
	 * <p>The time unit is milliseconds by default but can be overridden via
	 * {@link #timeUnit}.
	 * <p>The default is 0 (no jitter).
	 * @see #delay()
	 * @see #multiplier()
	 * @see #maxDelay()
	 */
	long jitter() default 0;

	/**
	 * A jitter value for the base retry attempt, as a duration String.
	 * <p>A non-empty value specified here overrides the {@link #jitter()} attribute.
	 * <p>The duration String can be in several formats:
	 * <ul>
	 * <li>a plain integer &mdash; which is interpreted to represent a duration in
	 * milliseconds by default unless overridden via {@link #timeUnit()} (prefer
	 * using {@link #jitter()} in that case)</li>
	 * <li>any of the known {@link org.springframework.format.annotation.DurationFormat.Style
	 * DurationFormat.Style}: the {@link org.springframework.format.annotation.DurationFormat.Style#ISO8601 ISO8601}
	 * style or the {@link org.springframework.format.annotation.DurationFormat.Style#SIMPLE SIMPLE} style
	 * &mdash; using the {@link #timeUnit()} as fallback if the string doesn't contain an explicit unit</li>
	 * <li>one of the above, with Spring-style "${...}" placeholders as well as SpEL expressions</li>
	 * </ul>
	 * @return the initial delay as a String value &mdash; for example a placeholder,
	 * or a {@link org.springframework.format.annotation.DurationFormat.Style#ISO8601 java.time.Duration} compliant value
	 * or a {@link org.springframework.format.annotation.DurationFormat.Style#SIMPLE simple format} compliant value
	 * @see #jitter()
	 */
	String jitterString() default "";

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
	 * A multiplier for a delay for the next retry attempt, as a configurable String.
	 * <p>A non-empty value specified here overrides the {@link #multiplier()} attribute.
	 * <p>This supports Spring-style "${...}" placeholders as well as SpEL expressions.
	 * @see #multiplier()
	 */
	String multiplierString() default "";

	/**
	 * The maximum delay for any retry attempt, limiting how far {@link #jitter()}
	 * and {@link #multiplier()} can increase the {@linkplain #delay() delay}.
	 * <p>The time unit is milliseconds by default but can be overridden via
	 * {@link #timeUnit}.
	 * <p>The default is unlimited.
	 * @see #delay()
	 * @see #jitter()
	 * @see #multiplier()
	 */
	long maxDelay() default Long.MAX_VALUE;

	/**
	 * The maximum delay for any retry attempt, as a duration String.
	 * <p>A non-empty value specified here overrides the {@link #maxDelay()} attribute.
	 * <p>The duration String can be in several formats:
	 * <ul>
	 * <li>a plain integer &mdash; which is interpreted to represent a duration in
	 * milliseconds by default unless overridden via {@link #timeUnit()} (prefer
	 * using {@link #maxDelay()} in that case)</li>
	 * <li>any of the known {@link org.springframework.format.annotation.DurationFormat.Style
	 * DurationFormat.Style}: the {@link org.springframework.format.annotation.DurationFormat.Style#ISO8601 ISO8601}
	 * style or the {@link org.springframework.format.annotation.DurationFormat.Style#SIMPLE SIMPLE} style
	 * &mdash; using the {@link #timeUnit()} as fallback if the string doesn't contain an explicit unit</li>
	 * <li>one of the above, with Spring-style "${...}" placeholders as well as SpEL expressions</li>
	 * </ul>
	 * @return the initial delay as a String value &mdash; for example a placeholder,
	 * or a {@link org.springframework.format.annotation.DurationFormat.Style#ISO8601 java.time.Duration} compliant value
	 * or a {@link org.springframework.format.annotation.DurationFormat.Style#SIMPLE simple format} compliant value
	 * @see #maxDelay()
	 */
	String maxDelayString() default "";

	/**
	 * The {@link TimeUnit} to use for {@link #delay}, {@link #delayString},
	 * {@link #jitter}, {@link #jitterString}, {@link #maxDelay}, and
	 * {@link #maxDelayString}.
	 * <p>The default is {@link TimeUnit#MILLISECONDS}.
	 * <p>This attribute is ignored for {@link java.time.Duration} values supplied
	 * via {@link #delayString}, {@link #jitterString}, or {@link #maxDelayString}.
	 * @return the {@code TimeUnit} to use
	 */
	TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

}
