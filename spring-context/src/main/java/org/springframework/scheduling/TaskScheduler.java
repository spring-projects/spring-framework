/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.scheduling;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.springframework.lang.Nullable;

/**
 * Task scheduler interface that abstracts the scheduling of
 * {@link Runnable Runnables} based on different kinds of triggers.
 *
 * <p>This interface is separate from {@link SchedulingTaskExecutor} since it
 * usually represents a different kind of backend, i.e. a thread pool with
 * different characteristics and capabilities. Implementations may implement
 * both interfaces if they can handle both kinds of execution characteristics.
 *
 * <p>The 'default' implementation is
 * {@link org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler},
 * wrapping a native {@link java.util.concurrent.ScheduledExecutorService}
 * and adding extended trigger capabilities.
 *
 * <p>This interface is roughly equivalent to a JSR-236
 * {@code ManagedScheduledExecutorService} as supported in Jakarta EE
 * environments but aligned with Spring's {@code TaskExecutor} model.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.core.task.TaskExecutor
 * @see java.util.concurrent.ScheduledExecutorService
 * @see org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
 */
public interface TaskScheduler {

	/**
	 * Return the clock to use for scheduling purposes.
	 * @since 5.3
	 * @see Clock#systemDefaultZone()
	 */
	default Clock getClock() {
		return Clock.systemDefaultZone();
	}

	/**
	 * Schedule the given {@link Runnable}, invoking it whenever the trigger
	 * indicates a next execution time.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param trigger an implementation of the {@link Trigger} interface,
	 * e.g. a {@link org.springframework.scheduling.support.CronTrigger} object
	 * wrapping a cron expression
	 * @return a {@link ScheduledFuture} representing pending execution of the task,
	 * or {@code null} if the given Trigger object never fires (i.e. returns
	 * {@code null} from {@link Trigger#nextExecution})
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @see org.springframework.scheduling.support.CronTrigger
	 */
	@Nullable
	ScheduledFuture<?> schedule(Runnable task, Trigger trigger);

	/**
	 * Schedule the given {@link Runnable}, invoking it at the specified execution time.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @return a {@link ScheduledFuture} representing pending execution of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 */
	ScheduledFuture<?> schedule(Runnable task, Instant startTime);

	/**
	 * Schedule the given {@link Runnable}, invoking it at the specified execution time.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @return a {@link ScheduledFuture} representing pending execution of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @deprecated as of 6.0, in favor of {@link #schedule(Runnable, Instant)}
	 */
	@Deprecated(since = "6.0")
	default ScheduledFuture<?> schedule(Runnable task, Date startTime) {
		return schedule(task, startTime.toInstant());
	}

	/**
	 * Schedule the given {@link Runnable}, invoking it at the specified execution time
	 * and subsequently with the given period.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired first execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @param period the interval between successive executions of the task
	 * @return a {@link ScheduledFuture} representing pending execution of the task
	 * @throws org.springframework.core.task.TaskRejectedException if  the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 */
	ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period);

	/**
	 * Schedule the given {@link Runnable}, invoking it at the specified execution time
	 * and subsequently with the given period.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired first execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @param period the interval between successive executions of the task (in milliseconds)
	 * @return a {@link ScheduledFuture} representing pending execution of the task
	 * @throws org.springframework.core.task.TaskRejectedException if  the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @deprecated as of 6.0, in favor of {@link #scheduleAtFixedRate(Runnable, Instant, Duration)}
	 */
	@Deprecated(since = "6.0")
	default ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		return scheduleAtFixedRate(task, startTime.toInstant(), Duration.ofMillis(period));
	}

	/**
	 * Schedule the given {@link Runnable}, starting as soon as possible and
	 * invoking it with the given period.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param period the interval between successive executions of the task
	 * @return a {@link ScheduledFuture} representing pending execution of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 */
	ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period);

	/**
	 * Schedule the given {@link Runnable}, starting as soon as possible and
	 * invoking it with the given period.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param period the interval between successive executions of the task (in milliseconds)
	 * @return a {@link ScheduledFuture} representing pending execution of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @deprecated as of 6.0, in favor of {@link #scheduleAtFixedRate(Runnable, Duration)}
	 */
	@Deprecated(since = "6.0")
	default ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
		return scheduleAtFixedRate(task, Duration.ofMillis(period));
	}

	/**
	 * Schedule the given {@link Runnable}, invoking it at the specified execution time
	 * and subsequently with the given delay between the completion of one execution
	 * and the start of the next.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired first execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @param delay the delay between the completion of one execution and the start of the next
	 * @return a {@link ScheduledFuture} representing pending execution of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 */
	ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay);

	/**
	 * Schedule the given {@link Runnable}, invoking it at the specified execution time
	 * and subsequently with the given delay between the completion of one execution
	 * and the start of the next.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param startTime the desired first execution time for the task
	 * (if this is in the past, the task will be executed immediately, i.e. as soon as possible)
	 * @param delay the delay between the completion of one execution and the start of the next
	 * (in milliseconds)
	 * @return a {@link ScheduledFuture} representing pending execution of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @deprecated as of 6.0, in favor of {@link #scheduleWithFixedDelay(Runnable, Instant, Duration)}
	 */
	@Deprecated(since = "6.0")
	default ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		return scheduleWithFixedDelay(task, startTime.toInstant(), Duration.ofMillis(delay));
	}

	/**
	 * Schedule the given {@link Runnable}, starting as soon as possible and invoking it with
	 * the given delay between the completion of one execution and the start of the next.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param delay the delay between the completion of one execution and the start of the next
	 * @return a {@link ScheduledFuture} representing pending execution of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @since 5.0
	 */
	ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay);

	/**
	 * Schedule the given {@link Runnable}, starting as soon as possible and invoking it with
	 * the given delay between the completion of one execution and the start of the next.
	 * <p>Execution will end once the scheduler shuts down or the returned
	 * {@link ScheduledFuture} gets cancelled.
	 * @param task the Runnable to execute whenever the trigger fires
	 * @param delay the delay between the completion of one execution and the start of the next
	 * (in milliseconds)
	 * @return a {@link ScheduledFuture} representing pending execution of the task
	 * @throws org.springframework.core.task.TaskRejectedException if the given task was not accepted
	 * for internal reasons (e.g. a pool overload handling policy or a pool shutdown in progress)
	 * @deprecated as of 6.0, in favor of {@link #scheduleWithFixedDelay(Runnable, Duration)}
	 */
	@Deprecated(since = "6.0")
	default ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
		return scheduleWithFixedDelay(task, Duration.ofMillis(delay));
	}

}
