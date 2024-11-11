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

package org.springframework.scheduling.support;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
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
	@Nullable
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
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
