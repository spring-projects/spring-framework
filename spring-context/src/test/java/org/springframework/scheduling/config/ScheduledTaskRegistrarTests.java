/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.scheduling.config;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ScheduledTaskRegistrar}.
 *
 * @author Tobias Montagna-Hay
 * @author Juergen Hoeller
 * @since 4.2
 */
public class ScheduledTaskRegistrarTests {

	private final ScheduledTaskRegistrar taskRegistrar = new ScheduledTaskRegistrar();


	@Test
	public void emptyTaskLists() {
		assertThat(this.taskRegistrar.getTriggerTaskList().isEmpty()).isTrue();
		assertThat(this.taskRegistrar.getCronTaskList().isEmpty()).isTrue();
		assertThat(this.taskRegistrar.getFixedRateTaskList().isEmpty()).isTrue();
		assertThat(this.taskRegistrar.getFixedDelayTaskList().isEmpty()).isTrue();
	}

	@Test
	public void getTriggerTasks() {
		TriggerTask mockTriggerTask = mock(TriggerTask.class);
		List<TriggerTask> triggerTaskList = Collections.singletonList(mockTriggerTask);
		this.taskRegistrar.setTriggerTasksList(triggerTaskList);
		List<TriggerTask> retrievedList = this.taskRegistrar.getTriggerTaskList();
		assertThat(retrievedList.size()).isEqualTo(1);
		assertThat(retrievedList.get(0)).isEqualTo(mockTriggerTask);
	}

	@Test
	public void getCronTasks() {
		CronTask mockCronTask = mock(CronTask.class);
		List<CronTask> cronTaskList = Collections.singletonList(mockCronTask);
		this.taskRegistrar.setCronTasksList(cronTaskList);
		List<CronTask> retrievedList = this.taskRegistrar.getCronTaskList();
		assertThat(retrievedList.size()).isEqualTo(1);
		assertThat(retrievedList.get(0)).isEqualTo(mockCronTask);
	}

	@Test
	public void getFixedRateTasks() {
		IntervalTask mockFixedRateTask = mock(IntervalTask.class);
		List<IntervalTask> fixedRateTaskList = Collections.singletonList(mockFixedRateTask);
		this.taskRegistrar.setFixedRateTasksList(fixedRateTaskList);
		List<IntervalTask> retrievedList = this.taskRegistrar.getFixedRateTaskList();
		assertThat(retrievedList.size()).isEqualTo(1);
		assertThat(retrievedList.get(0)).isEqualTo(mockFixedRateTask);
	}

	@Test
	public void getFixedDelayTasks() {
		IntervalTask mockFixedDelayTask = mock(IntervalTask.class);
		List<IntervalTask> fixedDelayTaskList = Collections.singletonList(mockFixedDelayTask);
		this.taskRegistrar.setFixedDelayTasksList(fixedDelayTaskList);
		List<IntervalTask> retrievedList = this.taskRegistrar.getFixedDelayTaskList();
		assertThat(retrievedList.size()).isEqualTo(1);
		assertThat(retrievedList.get(0)).isEqualTo(mockFixedDelayTask);
	}

}
