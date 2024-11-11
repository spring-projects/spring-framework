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

package org.springframework.scheduling.concurrent;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import org.springframework.scheduling.Trigger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultManagedTaskScheduler}.
 *
 * @author Stephane Nicoll
 */
class DefaultManagedTaskSchedulerTests {

	private final Runnable NO_OP = () -> {};

	@Test
	void scheduleWithTriggerAndNoScheduledExecutorProvidesDedicatedException() {
		DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
		assertNoExecutorException(() -> scheduler.schedule(NO_OP, mock(Trigger.class)));
	}

	@Test
	void scheduleWithInstantAndNoScheduledExecutorProvidesDedicatedException() {
		DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
		assertNoExecutorException(() -> scheduler.schedule(NO_OP, Instant.now()));
	}

	@Test
	void scheduleAtFixedRateWithStartTimeAndDurationAndNoScheduledExecutorProvidesDedicatedException() {
		DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
		assertNoExecutorException(() -> scheduler.scheduleAtFixedRate(
				NO_OP, Instant.now(), Duration.of(1, ChronoUnit.MINUTES)));
	}

	@Test
	void scheduleAtFixedRateWithDurationAndNoScheduledExecutorProvidesDedicatedException() {
		DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
		assertNoExecutorException(() -> scheduler.scheduleAtFixedRate(
				NO_OP, Duration.of(1, ChronoUnit.MINUTES)));
	}

	@Test
	void scheduleWithFixedDelayWithStartTimeAndDurationAndNoScheduledExecutorProvidesDedicatedException() {
		DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
		assertNoExecutorException(() -> scheduler.scheduleWithFixedDelay(
				NO_OP, Instant.now(), Duration.of(1, ChronoUnit.MINUTES)));
	}

	@Test
	void scheduleWithFixedDelayWithDurationAndNoScheduledExecutorProvidesDedicatedException() {
		DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
		assertNoExecutorException(() -> scheduler.scheduleWithFixedDelay(
				NO_OP, Duration.of(1, ChronoUnit.MINUTES)));
	}

	private void assertNoExecutorException(ThrowingCallable callable) {
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("No ScheduledExecutor is configured");
	}

}
