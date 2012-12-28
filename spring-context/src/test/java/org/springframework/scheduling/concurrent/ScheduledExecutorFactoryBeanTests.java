/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.scheduling.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.task.NoOpRunnable;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class ScheduledExecutorFactoryBeanTests {

	@Test
	public void testThrowsExceptionIfPoolSizeIsLessThanZero() throws Exception {
		try {
			ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
			factory.setPoolSize(-1);
			factory.setScheduledExecutorTasks(new ScheduledExecutorTask[]{
				new NoOpScheduledExecutorTask()
			});
			factory.afterPropertiesSet();
			fail("Pool size less than zero");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	@Test
	@SuppressWarnings("serial")
	public void testShutdownNowIsPropagatedToTheExecutorOnDestroy() throws Exception {
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
			@Override
			protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
				return executor;
			}
		};
		factory.setScheduledExecutorTasks(new ScheduledExecutorTask[]{
			new NoOpScheduledExecutorTask()
		});
		factory.afterPropertiesSet();
		factory.destroy();

		verify(executor).shutdownNow();
	}

	@Test
	@SuppressWarnings("serial")
	public void testShutdownIsPropagatedToTheExecutorOnDestroy() throws Exception {
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
			@Override
			protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
				return executor;
			}
		};
		factory.setScheduledExecutorTasks(new ScheduledExecutorTask[]{
			new NoOpScheduledExecutorTask()
		});
		factory.setWaitForTasksToCompleteOnShutdown(true);
		factory.afterPropertiesSet();
		factory.destroy();

		verify(executor).shutdown();
	}

	@Test
	public void testOneTimeExecutionIsSetUpAndFiresCorrectly() throws Exception {
		Runnable runnable = mock(Runnable.class);

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		factory.setScheduledExecutorTasks(new ScheduledExecutorTask[]{
			new ScheduledExecutorTask(runnable)
		});
		factory.afterPropertiesSet();
		pauseToLetTaskStart(1);
		factory.destroy();

		verify(runnable).run();
	}

	@Test
	public void testFixedRepeatedExecutionIsSetUpAndFiresCorrectly() throws Exception {
		Runnable runnable = mock(Runnable.class);

		ScheduledExecutorTask task = new ScheduledExecutorTask(runnable);
		task.setPeriod(500);
		task.setFixedRate(true);

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		factory.setScheduledExecutorTasks(new ScheduledExecutorTask[]{task});
		factory.afterPropertiesSet();
		pauseToLetTaskStart(2);
		factory.destroy();

		verify(runnable, atLeast(2)).run();
	}

	@Test
	public void testFixedRepeatedExecutionIsSetUpAndFiresCorrectlyAfterException() throws Exception {
		Runnable runnable = mock(Runnable.class);
		willThrow(new IllegalStateException()).given(runnable).run();

		ScheduledExecutorTask task = new ScheduledExecutorTask(runnable);
		task.setPeriod(500);
		task.setFixedRate(true);

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		factory.setScheduledExecutorTasks(new ScheduledExecutorTask[]{task});
		factory.setContinueScheduledExecutionAfterException(true);
		factory.afterPropertiesSet();
		pauseToLetTaskStart(2);
		factory.destroy();

		verify(runnable, atLeast(2)).run();
	}

	@Ignore
	@Test
	public void testWithInitialDelayRepeatedExecutionIsSetUpAndFiresCorrectly() throws Exception {
		Runnable runnable = mock(Runnable.class);

		ScheduledExecutorTask task = new ScheduledExecutorTask(runnable);
		task.setPeriod(500);
		task.setDelay(3000); // nice long wait...

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		factory.setScheduledExecutorTasks(new ScheduledExecutorTask[] {task});
		factory.afterPropertiesSet();
		pauseToLetTaskStart(1);
		// invoke destroy before tasks have even been scheduled...
		factory.destroy();

		// Mock must never have been called
		verify(runnable, never()).run();
	}

	@Ignore
	@Test
	public void testWithInitialDelayRepeatedExecutionIsSetUpAndFiresCorrectlyAfterException() throws Exception {
		Runnable runnable = mock(Runnable.class);
		willThrow(new IllegalStateException()).given(runnable).run();

		ScheduledExecutorTask task = new ScheduledExecutorTask(runnable);
		task.setPeriod(500);
		task.setDelay(3000); // nice long wait...

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		factory.setScheduledExecutorTasks(new ScheduledExecutorTask[] {task});
		factory.setContinueScheduledExecutionAfterException(true);
		factory.afterPropertiesSet();
		pauseToLetTaskStart(1);
		// invoke destroy before tasks have even been scheduled...
		factory.destroy();

		// Mock must never have been called
		verify(runnable, never()).run();
	}

	@Test
	@SuppressWarnings("serial")
	public void testSettingThreadFactoryToNullForcesUseOfDefaultButIsOtherwiseCool() throws Exception {
		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
			@Override
			protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
				assertNotNull("Bah; the setThreadFactory(..) method must use a default ThreadFactory if a null arg is passed in.");
				return super.createExecutor(poolSize, threadFactory, rejectedExecutionHandler);
			}
		};
		factory.setScheduledExecutorTasks(new ScheduledExecutorTask[]{
			new NoOpScheduledExecutorTask()
		});
		factory.setThreadFactory(null); // the null must not propagate
		factory.afterPropertiesSet();
		factory.destroy();
	}

	@Test
	@SuppressWarnings("serial")
	public void testSettingRejectedExecutionHandlerToNullForcesUseOfDefaultButIsOtherwiseCool() throws Exception {
		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
			@Override
			protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
				assertNotNull("Bah; the setRejectedExecutionHandler(..) method must use a default RejectedExecutionHandler if a null arg is passed in.");
				return super.createExecutor(poolSize, threadFactory, rejectedExecutionHandler);
			}
		};
		factory.setScheduledExecutorTasks(new ScheduledExecutorTask[]{
			new NoOpScheduledExecutorTask()
		});
		factory.setRejectedExecutionHandler(null); // the null must not propagate
		factory.afterPropertiesSet();
		factory.destroy();
	}

	@Test
	public void testObjectTypeReportsCorrectType() throws Exception {
		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		assertEquals(ScheduledExecutorService.class, factory.getObjectType());
	}


	private static void pauseToLetTaskStart(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		}
		catch (InterruptedException ignored) {
		}
	}


	private static class NoOpScheduledExecutorTask extends ScheduledExecutorTask {

		public NoOpScheduledExecutorTask() {
			super(new NoOpRunnable());
		}
	}

}
