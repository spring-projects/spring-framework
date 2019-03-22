/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
		assertTrue(this.taskRegistrar.getTriggerTaskList().isEmpty());
		assertTrue(this.taskRegistrar.getCronTaskList().isEmpty());
		assertTrue(this.taskRegistrar.getFixedRateTaskList().isEmpty());
		assertTrue(this.taskRegistrar.getFixedDelayTaskList().isEmpty());
	}

	@Test
	public void getTriggerTasks() {
		TriggerTask mockTriggerTask = mock(TriggerTask.class);
		List<TriggerTask> triggerTaskList = Collections.singletonList(mockTriggerTask);
		this.taskRegistrar.setTriggerTasksList(triggerTaskList);
		List<TriggerTask> retrievedList = this.taskRegistrar.getTriggerTaskList();
		assertEquals(1, retrievedList.size());
		assertEquals(mockTriggerTask, retrievedList.get(0));
	}

	@Test
	public void getCronTasks() {
		CronTask mockCronTask = mock(CronTask.class);
		List<CronTask> cronTaskList = Collections.singletonList(mockCronTask);
		this.taskRegistrar.setCronTasksList(cronTaskList);
		List<CronTask> retrievedList = this.taskRegistrar.getCronTaskList();
		assertEquals(1, retrievedList.size());
		assertEquals(mockCronTask, retrievedList.get(0));
	}

	@Test
	public void getFixedRateTasks() {
		IntervalTask mockFixedRateTask = mock(IntervalTask.class);
		List<IntervalTask> fixedRateTaskList = Collections.singletonList(mockFixedRateTask);
		this.taskRegistrar.setFixedRateTasksList(fixedRateTaskList);
		List<IntervalTask> retrievedList = this.taskRegistrar.getFixedRateTaskList();
		assertEquals(1, retrievedList.size());
		assertEquals(mockFixedRateTask, retrievedList.get(0));
	}

	@Test
	public void getFixedDelayTasks() {
		IntervalTask mockFixedDelayTask = mock(IntervalTask.class);
		List<IntervalTask> fixedDelayTaskList = Collections.singletonList(mockFixedDelayTask);
		this.taskRegistrar.setFixedDelayTasksList(fixedDelayTaskList);
		List<IntervalTask> retrievedList = this.taskRegistrar.getFixedDelayTaskList();
		assertEquals(1, retrievedList.size());
		assertEquals(mockFixedDelayTask, retrievedList.get(0));
	}

}
