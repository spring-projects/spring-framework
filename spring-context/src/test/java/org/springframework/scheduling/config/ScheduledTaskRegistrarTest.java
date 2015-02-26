/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scheduling.config;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * @author Tobias Montagna-Hay
 * @since 4.2
 */
public class ScheduledTaskRegistrarTest {

	@Test
	public void testGetTriggerTasks() {		
		List<TriggerTask> triggerTaskList = new ArrayList<TriggerTask>();		
		TriggerTask mockTriggerTask = mock(TriggerTask.class);
		triggerTaskList.add(mockTriggerTask);
		ScheduledTaskRegistrar scheduledTaskRegistrar = new ScheduledTaskRegistrar();
		scheduledTaskRegistrar.setTriggerTasksList(triggerTaskList);
		List<TriggerTask> retrievedList = scheduledTaskRegistrar.getTriggerTaskList();
		assertNotSame(triggerTaskList, retrievedList);
		assertEquals(1, retrievedList.size());		
		assertEquals(mockTriggerTask, retrievedList.get(0));		
	}
	
	@Test
	public void testGetCronTasks() {
		List<CronTask> cronTaskList = new ArrayList<CronTask>();
		CronTask mockCronTask = mock(CronTask.class);
		cronTaskList.add(mockCronTask);
		ScheduledTaskRegistrar scheduledTaskRegistrar = new ScheduledTaskRegistrar();
		scheduledTaskRegistrar.setCronTasksList(cronTaskList);
		List<CronTask> retrievedList = scheduledTaskRegistrar.getCronTaskList();
		assertNotSame(cronTaskList, retrievedList);
		assertEquals(1, retrievedList.size());
		assertEquals(mockCronTask, retrievedList.get(0));
	}
	
	@Test
	public void testGetFixedRateTasks() {
		List<IntervalTask> fixedRateTaskList = new ArrayList<IntervalTask>();
		IntervalTask mockFixedRateTask = mock(IntervalTask.class);
		fixedRateTaskList.add(mockFixedRateTask);
		ScheduledTaskRegistrar scheduledTaskRegistrar = new ScheduledTaskRegistrar();
		scheduledTaskRegistrar.setFixedRateTasksList(fixedRateTaskList);
		List<IntervalTask> retrievedList = scheduledTaskRegistrar.getFixedRateTaskList();
		assertNotSame(fixedRateTaskList, retrievedList);
		assertEquals(1, retrievedList.size());
		assertEquals(mockFixedRateTask, retrievedList.get(0));
	}
	
	@Test
	public void testGetFixedDelayTasks() {
		List<IntervalTask> fixedDelayTaskList = new ArrayList<IntervalTask>();
		IntervalTask mockFixedDelayTask = mock(IntervalTask.class);
		fixedDelayTaskList.add(mockFixedDelayTask);
		ScheduledTaskRegistrar scheduledTaskRegistrar = new ScheduledTaskRegistrar();
		scheduledTaskRegistrar.setFixedDelayTasksList(fixedDelayTaskList);
		List<IntervalTask> retrievedList = scheduledTaskRegistrar.getFixedDelayTaskList();
		assertNotSame(fixedDelayTaskList, retrievedList);
		assertEquals(1, retrievedList.size());
		assertEquals(mockFixedDelayTask, retrievedList.get(0));
	}
	
	@Test
	public void testGetAllTasks() {
		ScheduledTaskRegistrar scheduledTaskRegistrar = new ScheduledTaskRegistrar();
		List<TriggerTask> triggerTaskList = new ArrayList<TriggerTask>();		
		TriggerTask mockTriggerTask = mock(TriggerTask.class);
		triggerTaskList.add(mockTriggerTask);
		scheduledTaskRegistrar.setTriggerTasksList(triggerTaskList);
		List<CronTask> cronTaskList = new ArrayList<CronTask>();
		CronTask mockCronTask = mock(CronTask.class);
		cronTaskList.add(mockCronTask);
		scheduledTaskRegistrar.setCronTasksList(cronTaskList);
		List<IntervalTask> fixedRateTaskList = new ArrayList<IntervalTask>();
		IntervalTask mockFixedRateTask = mock(IntervalTask.class);
		fixedRateTaskList.add(mockFixedRateTask);
		scheduledTaskRegistrar.setFixedRateTasksList(fixedRateTaskList);
		List<IntervalTask> fixedDelayTaskList = new ArrayList<IntervalTask>();
		IntervalTask mockFixedDelayTask = mock(IntervalTask.class);
		fixedDelayTaskList.add(mockFixedDelayTask);
		scheduledTaskRegistrar.setFixedDelayTasksList(fixedDelayTaskList);
		List<Task> retrievedList = scheduledTaskRegistrar.getAllTasks();
		assertEquals(4, retrievedList.size());
		assertTrue(retrievedList.contains(mockTriggerTask));
		assertTrue(retrievedList.contains(mockCronTask));
		assertTrue(retrievedList.contains(mockFixedRateTask));
		assertTrue(retrievedList.contains(mockFixedDelayTask));
	}
	
}
