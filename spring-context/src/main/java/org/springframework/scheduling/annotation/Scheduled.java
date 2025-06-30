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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Annotation that marks a method to be scheduled. For periodic tasks, exactly one
 * of the {@link #cron}, {@link #fixedDelay}, or {@link #fixedRate} attributes
 * must be specified, and additionally an optional {@link #initialDelay}.
 * For a one-time task, it is sufficient to just specify an {@link #initialDelay}.
 *
 * <p>The annotated method must not accept arguments. It will typically have
 * a {@code void} return type; if not, the returned value will be ignored
 * when called through the scheduler.
 *
 * <p>Methods that return a reactive {@code Publisher} or a type which can be adapted
 * to {@code Publisher} by the default {@code ReactiveAdapterRegistry} are supported.
 * The {@code Publisher} must support multiple subsequent subscriptions. The returned
 * {@code Publisher} is only produced once, and the scheduling infrastructure then
 * periodically subscribes to it according to configuration. Values emitted by
 * the publisher are ignored. Errors are logged at {@code WARN} level, which
 * doesn't prevent further iterations. If a fixed delay is configured, the
 * subscription is blocked in order to respect the fixed delay semantics.
 *
 * <p>Kotlin suspending functions are also supported, provided the coroutine-reactor
 * bridge ({@code kotlinx.coroutine.reactor}) is present at runtime. This bridge is
 * used to adapt the suspending function to a {@code Publisher} which is treated
 * the same way as in the reactive method case (see above).
 *
 * <p>Processing of {@code @Scheduled} annotations is performed by registering a
 * {@link ScheduledAnnotationBeanPostProcessor}. This can be done manually or,
 * more conveniently, through the {@code <task:annotation-driven/>} XML element
 * or {@link EnableScheduling @EnableScheduling} annotation.
 *
 * <p>This annotation can be used as a <em>{@linkplain Repeatable repeatable}</em>
 * annotation. If several scheduled declarations are found on the same method,
 * each of them will be processed independently, with a separate trigger firing
 * for each of them. As a consequence, such co-located schedules may overlap
 * and execute multiple times in parallel or in immediate succession.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em> with attribute overrides.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 * @author Victor Brown
 * @author Sam Brannen
 * @since 3.0
 * @see EnableScheduling
 * @see ScheduledAnnotationBeanPostProcessor
 * @see Schedules
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Schedules.class)
@Reflective
public @interface Scheduled {

	/**
	 * A special cron expression value that indicates a disabled trigger: {@value}.
	 * <p>This is primarily meant for use with <code>${...}</code> placeholders,
	 * allowing for external disabling of corresponding scheduled methods.
	 * @since 5.1
	 * @see ScheduledTaskRegistrar#CRON_DISABLED
	 */
	String CRON_DISABLED = ScheduledTaskRegistrar.CRON_DISABLED;


	/**
	 * A cron-like expression, extending the usual UN*X definition to include triggers
	 * on the second, minute, hour, day of month, month, and day of week.
	 * <p>For example, {@code "0 * * * * MON-FRI"} means once per minute on weekdays
	 * (at the top of the minute - the 0th second).
	 * <p>The fields read from left to right are interpreted as follows.
	 * <ul>
	 * <li>second</li>
	 * <li>minute</li>
	 * <li>hour</li>
	 * <li>day of month</li>
	 * <li>month</li>
	 * <li>day of week</li>
	 * </ul>
	 * <p>The special value {@link #CRON_DISABLED "-"} indicates a disabled cron
	 * trigger, primarily meant for externally specified values resolved by a
	 * <code>${...}</code> placeholder.
	 * @return an expression that can be parsed to a cron schedule
	 * @see org.springframework.scheduling.support.CronExpression#parse(String)
	 */
	String cron() default "";

	/**
	 * A time zone for which the cron expression will be resolved. By default, this
	 * attribute is the empty String (i.e. the scheduler's time zone will be used).
	 * @return a zone id accepted by {@link java.util.TimeZone#getTimeZone(String)},
	 * or an empty String to indicate the scheduler's default time zone
	 * @since 4.0
	 * @see org.springframework.scheduling.support.CronTrigger#CronTrigger(String, java.util.TimeZone)
	 * @see java.util.TimeZone
	 */
	String zone() default "";

	/**
	 * Execute the annotated method with a fixed period between invocations.
	 * <p>The time unit is milliseconds by default but can be overridden via
	 * {@link #timeUnit}.
	 * @return the period
	 */
	long fixedRate() default -1;

	/**
	 * Execute the annotated method with a fixed period between invocations.
	 * <p>The duration String can be in several formats:
	 * <ul>
	 * <li>a plain integer &mdash; which is interpreted to represent a duration in
	 * milliseconds by default unless overridden via {@link #timeUnit()} (prefer
	 * using {@link #fixedDelay()} in that case)</li>
	 * <li>any of the known {@link org.springframework.format.annotation.DurationFormat.Style
	 * DurationFormat.Style}: the {@link org.springframework.format.annotation.DurationFormat.Style#ISO8601 ISO8601}
	 * style or the {@link org.springframework.format.annotation.DurationFormat.Style#SIMPLE SIMPLE} style
	 * &mdash; using the {@link #timeUnit()} as fallback if the string doesn't contain an explicit unit</li>
	 * <li>one of the above, with Spring-style "${...}" placeholders as well as SpEL expressions</li>
	 * </ul>
	 * @return the period as a String value &mdash; for example a placeholder,
	 * or a {@link org.springframework.format.annotation.DurationFormat.Style#ISO8601 java.time.Duration} compliant value
	 * or a {@link org.springframework.format.annotation.DurationFormat.Style#SIMPLE simple format} compliant value
	 * @since 3.2.2
	 * @see #fixedRate()
	 */
	String fixedRateString() default "";

	/**
	 * Execute the annotated method with a fixed period between the end of the
	 * last invocation and the start of the next.
	 * <p>The time unit is milliseconds by default but can be overridden via
	 * {@link #timeUnit}.
	 * <p><b>NOTE: With virtual threads, fixed rates and cron triggers are recommended
	 * over fixed delays.</b> Fixed-delay tasks operate on a single scheduler thread
	 * with {@link org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler}.
	 * @return the delay
	 */
	long fixedDelay() default -1;

	/**
	 * Execute the annotated method with a fixed period between the end of the
	 * last invocation and the start of the next.
	 * <p>The duration String can be in several formats:
	 * <ul>
	 * <li>a plain integer &mdash; which is interpreted to represent a duration in
	 * milliseconds by default unless overridden via {@link #timeUnit()} (prefer
	 * using {@link #fixedDelay()} in that case)</li>
	 * <li>any of the known {@link org.springframework.format.annotation.DurationFormat.Style
	 * DurationFormat.Style}: the {@link org.springframework.format.annotation.DurationFormat.Style#ISO8601 ISO8601}
	 * style or the {@link org.springframework.format.annotation.DurationFormat.Style#SIMPLE SIMPLE} style
	 * &mdash; using the {@link #timeUnit()} as fallback if the string doesn't contain an explicit unit</li>
	 * </ul>
	 * <p><b>NOTE: With virtual threads, fixed rates and cron triggers are recommended
	 * over fixed delays.</b> Fixed-delay tasks operate on a single scheduler thread
	 * with {@link org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler}.
	 * @return the delay as a String value &mdash; for example a placeholder,
	 * or a {@link org.springframework.format.annotation.DurationFormat.Style#ISO8601 java.time.Duration} compliant value
	 * or a {@link org.springframework.format.annotation.DurationFormat.Style#SIMPLE simple format} compliant value
	 * @since 3.2.2
	 * @see #fixedDelay()
	 */
	String fixedDelayString() default "";

	/**
	 * Number of units of time to delay before the first execution of a
	 * {@link #fixedRate} or {@link #fixedDelay} task.
	 * <p>The time unit is milliseconds by default but can be overridden via
	 * {@link #timeUnit}.
	 * @return the initial
	 * @since 3.2
	 */
	long initialDelay() default -1;

	/**
	 * Number of units of time to delay before the first execution of a
	 * {@link #fixedRate} or {@link #fixedDelay} task.
	 * <p>The duration String can be in several formats:
	 * <ul>
	 *     <li>a plain integer &mdash; which is interpreted to represent a duration in
	 *     milliseconds by default unless overridden via {@link #timeUnit()} (prefer
	 *     using {@link #fixedDelay()} in that case)</li>
	 *     <li>any of the known {@link org.springframework.format.annotation.DurationFormat.Style
	 *     DurationFormat.Style}: the {@link org.springframework.format.annotation.DurationFormat.Style#ISO8601 ISO8601}
	 *     style or the {@link org.springframework.format.annotation.DurationFormat.Style#SIMPLE SIMPLE} style
	 *     &mdash; using the {@link #timeUnit()} as fallback if the string doesn't contain an explicit unit</li>
	 *     <li>one of the above, with Spring-style "${...}" placeholders as well as SpEL expressions</li>
	 * </ul>
	 * @return the initial delay as a String value &mdash; for example a placeholder,
	 * or a {@link org.springframework.format.annotation.DurationFormat.Style#ISO8601 java.time.Duration} compliant value
	 * or a {@link org.springframework.format.annotation.DurationFormat.Style#SIMPLE simple format} compliant value
	 * @since 3.2.2
	 * @see #initialDelay()
	 */
	String initialDelayString() default "";

	/**
	 * The {@link TimeUnit} to use for {@link #fixedDelay}, {@link #fixedDelayString},
	 * {@link #fixedRate}, {@link #fixedRateString}, {@link #initialDelay}, and
	 * {@link #initialDelayString}.
	 * <p>Defaults to {@link TimeUnit#MILLISECONDS}.
	 * <p>This attribute is ignored for {@linkplain #cron() cron expressions}
	 * and for {@link java.time.Duration} values supplied via {@link #fixedDelayString},
	 * {@link #fixedRateString}, or {@link #initialDelayString}.
	 * @return the {@code TimeUnit} to use
	 * @since 5.3.10
	 */
	TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

	/**
	 * A qualifier for determining a scheduler to run this scheduled method on.
	 * <p>Defaults to an empty String, suggesting the default scheduler.
	 * <p>May be used to determine the target scheduler to be used,
	 * matching the qualifier value (or the bean name) of a specific
	 * {@link org.springframework.scheduling.TaskScheduler} or
	 * {@link java.util.concurrent.ScheduledExecutorService} bean definition.
	 * @since 6.1
	 * @see org.springframework.scheduling.SchedulingAwareRunnable#getQualifier()
	 */
	String scheduler() default "";

}
