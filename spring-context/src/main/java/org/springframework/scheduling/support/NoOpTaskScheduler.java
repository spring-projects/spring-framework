/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.support;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

/**
 * A basic, no operation {@link TaskScheduler} implementation suitable
 * for disabling scheduling, typically used for test setups.
 *
 * <p>Will accept any scheduling request but never actually execute it.
 *
 * @author Juergen Hoeller
 * @since 6.1.3
 */
public class NoOpTaskScheduler implements TaskScheduler {

	@Override
	public @Nullable ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		Instant nextExecution = trigger.nextExecution(new SimpleTriggerContext(getClock()));
		return (nextExecution != null ? new NoOpScheduledFuture<>() : null);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
		return new NoOpScheduledFuture<>();
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
		return new NoOpScheduledFuture<>();
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
		return new NoOpScheduledFuture<>();
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
		return new NoOpScheduledFuture<>();
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
		return new NoOpScheduledFuture<>();
	}


	private static class NoOpScheduledFuture<V> implements ScheduledFuture<V> {

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return true;
		}

		@Override
		public boolean isCancelled() {
			return true;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public V get() {
			throw new CancellationException("No-op");
		}

		@Override
		public V get(long timeout, TimeUnit unit) {
			throw new CancellationException("No-op");
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return 0;
		}

		@Override
		public int compareTo(Delayed other) {
			return 0;
		}
	}

}
