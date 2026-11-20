/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.concurrent;

import java.time.Duration;
import java.time.Instant;

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
				NO_OP, Instant.now(), Duration.ofMinutes(1)));
	}

	@Test
	void scheduleAtFixedRateWithDurationAndNoScheduledExecutorProvidesDedicatedException() {
		DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
		assertNoExecutorException(() -> scheduler.scheduleAtFixedRate(
				NO_OP, Duration.ofMinutes(1)));
	}

	@Test
	void scheduleWithFixedDelayWithStartTimeAndDurationAndNoScheduledExecutorProvidesDedicatedException() {
		DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
		assertNoExecutorException(() -> scheduler.scheduleWithFixedDelay(
				NO_OP, Instant.now(), Duration.ofMinutes(1)));
	}

	@Test
	void scheduleWithFixedDelayWithDurationAndNoScheduledExecutorProvidesDedicatedException() {
		DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
		assertNoExecutorException(() -> scheduler.scheduleWithFixedDelay(
				NO_OP, Duration.ofMinutes(1)));
	}

	private void assertNoExecutorException(ThrowingCallable callable) {
		assertThatThrownBy(callable)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("No ScheduledExecutor is configured");
	}

}
