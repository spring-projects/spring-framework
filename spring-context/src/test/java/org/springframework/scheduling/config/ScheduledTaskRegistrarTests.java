/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ScheduledTaskRegistrar}.
 *
 * @author Tobias Montagna-Hay
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.2
 */
class ScheduledTaskRegistrarTests {

	private static final Runnable no_op = () -> {};

	private final ScheduledTaskRegistrar taskRegistrar = new ScheduledTaskRegistrar();


	@BeforeEach
	void preconditions() {
		assertThat(this.taskRegistrar.getTriggerTaskList()).isEmpty();
		assertThat(this.taskRegistrar.getCronTaskList()).isEmpty();
		assertThat(this.taskRegistrar.getFixedRateTaskList()).isEmpty();
		assertThat(this.taskRegistrar.getFixedDelayTaskList()).isEmpty();
	}

	@Test
	void getTriggerTasks() {
		TriggerTask mockTriggerTask = mock();
		this.taskRegistrar.setTriggerTasksList(Collections.singletonList(mockTriggerTask));
		assertThat(this.taskRegistrar.getTriggerTaskList()).containsExactly(mockTriggerTask);
	}

	@Test
	void getCronTasks() {
		CronTask mockCronTask = mock();
		this.taskRegistrar.setCronTasksList(Collections.singletonList(mockCronTask));
		assertThat(this.taskRegistrar.getCronTaskList()).containsExactly(mockCronTask);
	}

	@Test
	void getFixedRateTasks() {
		IntervalTask mockFixedRateTask = mock();
		this.taskRegistrar.setFixedRateTasksList(Collections.singletonList(mockFixedRateTask));
		assertThat(this.taskRegistrar.getFixedRateTaskList()).containsExactly(mockFixedRateTask);
	}

	@Test
	void getFixedDelayTasks() {
		IntervalTask mockFixedDelayTask = mock();
		this.taskRegistrar.setFixedDelayTasksList(Collections.singletonList(mockFixedDelayTask));
		assertThat(this.taskRegistrar.getFixedDelayTaskList()).containsExactly(mockFixedDelayTask);
	}

	@Test
	void addCronTaskWithValidExpression() {
		this.taskRegistrar.addCronTask(no_op, "* * * * * ?");
		assertThat(this.taskRegistrar.getCronTaskList()).hasSize(1);
	}

	@Test
	void addCronTaskWithInvalidExpression() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.taskRegistrar.addCronTask(no_op, "* * *"))
			.withMessage("Cron expression must consist of 6 fields (found 3 in \"* * *\")");
	}

	@Test
	void addCronTaskWithDisabledExpression() {
		this.taskRegistrar.addCronTask(no_op, ScheduledTaskRegistrar.CRON_DISABLED);
		assertThat(this.taskRegistrar.getCronTaskList()).isEmpty();
	}

}
