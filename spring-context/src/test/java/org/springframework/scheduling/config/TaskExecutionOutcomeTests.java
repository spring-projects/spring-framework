/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;


import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link TaskExecutionOutcome}.
 * @author Brian Clozel
 */
class TaskExecutionOutcomeTests {

	@Test
	void shouldCreateWithNoneStatus() {
		TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
		assertThat(outcome.status()).isEqualTo(TaskExecutionOutcome.Status.NONE);
		assertThat(outcome.executionTime()).isNull();
		assertThat(outcome.throwable()).isNull();
	}

	@Test
	void startedTaskShouldBeOngoing() {
		TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
		Instant now = Instant.now();
		outcome = outcome.start(now);
		assertThat(outcome.status()).isEqualTo(TaskExecutionOutcome.Status.STARTED);
		assertThat(outcome.executionTime()).isEqualTo(now);
		assertThat(outcome.throwable()).isNull();
	}

	@Test
	void shouldRejectSuccessWhenNotStarted() {
		TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
		assertThatIllegalStateException().isThrownBy(outcome::success);
	}

	@Test
	void shouldRejectErrorWhenNotStarted() {
		TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
		assertThatIllegalStateException().isThrownBy(() -> outcome.failure(new IllegalArgumentException("test error")));
	}

	@Test
	void finishedTaskShouldBeSuccessful() {
		TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
		Instant now = Instant.now();
		outcome = outcome.start(now);
		outcome = outcome.success();
		assertThat(outcome.status()).isEqualTo(TaskExecutionOutcome.Status.SUCCESS);
		assertThat(outcome.executionTime()).isEqualTo(now);
		assertThat(outcome.throwable()).isNull();
	}

	@Test
	void errorTaskShouldBeFailure() {
		TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
		Instant now = Instant.now();
		outcome = outcome.start(now);
		outcome = outcome.failure(new IllegalArgumentException(("test error")));
		assertThat(outcome.status()).isEqualTo(TaskExecutionOutcome.Status.ERROR);
		assertThat(outcome.executionTime()).isEqualTo(now);
		assertThat(outcome.throwable()).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void newTaskExecutionShouldNotFail() {
		TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
		Instant now = Instant.now();
		outcome = outcome.start(now);
		outcome = outcome.failure(new IllegalArgumentException(("test error")));

		outcome = outcome.start(now.plusSeconds(2));
		assertThat(outcome.status()).isEqualTo(TaskExecutionOutcome.Status.STARTED);
		assertThat(outcome.executionTime()).isAfter(now);
		assertThat(outcome.throwable()).isNull();
	}

}
