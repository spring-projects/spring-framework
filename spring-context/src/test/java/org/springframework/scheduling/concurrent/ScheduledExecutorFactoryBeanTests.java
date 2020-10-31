/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.junit.jupiter.api.Test;

import org.springframework.core.task.NoOpRunnable;
import org.springframework.core.testfixture.EnabledForTestGroups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.core.testfixture.TestGroup.LONG_RUNNING;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
class ScheduledExecutorFactoryBeanTests {

	@Test
	void throwsExceptionIfPoolSizeIsLessThanZero() throws Exception {
		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setPoolSize(-1));
	}

	@Test
	@SuppressWarnings("serial")
	void shutdownNowIsPropagatedToTheExecutorOnDestroy() throws Exception {
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
			@Override
			protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
				return executor;
			}
		};
		factory.setScheduledExecutorTasks(new NoOpScheduledExecutorTask());
		factory.afterPropertiesSet();
		factory.destroy();

		verify(executor).shutdownNow();
	}

	@Test
	@SuppressWarnings("serial")
	void shutdownIsPropagatedToTheExecutorOnDestroy() throws Exception {
		final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
			@Override
			protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
				return executor;
			}
		};
		factory.setScheduledExecutorTasks(new NoOpScheduledExecutorTask());
		factory.setWaitForTasksToCompleteOnShutdown(true);
		factory.afterPropertiesSet();
		factory.destroy();

		verify(executor).shutdown();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void oneTimeExecutionIsSetUpAndFiresCorrectly() throws Exception {
		Runnable runnable = mock(Runnable.class);

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		factory.setScheduledExecutorTasks(new ScheduledExecutorTask(runnable));
		factory.afterPropertiesSet();
		pauseToLetTaskStart(1);
		factory.destroy();

		verify(runnable).run();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void fixedRepeatedExecutionIsSetUpAndFiresCorrectly() throws Exception {
		Runnable runnable = mock(Runnable.class);

		ScheduledExecutorTask task = new ScheduledExecutorTask(runnable);
		task.setPeriod(500);
		task.setFixedRate(true);

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		factory.setScheduledExecutorTasks(task);
		factory.afterPropertiesSet();
		pauseToLetTaskStart(2);
		factory.destroy();

		verify(runnable, atLeast(2)).run();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void fixedRepeatedExecutionIsSetUpAndFiresCorrectlyAfterException() throws Exception {
		Runnable runnable = mock(Runnable.class);
		willThrow(new IllegalStateException()).given(runnable).run();

		ScheduledExecutorTask task = new ScheduledExecutorTask(runnable);
		task.setPeriod(500);
		task.setFixedRate(true);

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		factory.setScheduledExecutorTasks(task);
		factory.setContinueScheduledExecutionAfterException(true);
		factory.afterPropertiesSet();
		pauseToLetTaskStart(2);
		factory.destroy();

		verify(runnable, atLeast(2)).run();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withInitialDelayRepeatedExecutionIsSetUpAndFiresCorrectly() throws Exception {
		Runnable runnable = mock(Runnable.class);

		ScheduledExecutorTask task = new ScheduledExecutorTask(runnable);
		task.setPeriod(500);
		task.setDelay(3000); // nice long wait...

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		factory.setScheduledExecutorTasks(task);
		factory.afterPropertiesSet();
		pauseToLetTaskStart(1);
		// invoke destroy before tasks have even been scheduled...
		factory.destroy();

		// Mock must never have been called
		verify(runnable, never()).run();
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void withInitialDelayRepeatedExecutionIsSetUpAndFiresCorrectlyAfterException() throws Exception {
		Runnable runnable = mock(Runnable.class);
		willThrow(new IllegalStateException()).given(runnable).run();

		ScheduledExecutorTask task = new ScheduledExecutorTask(runnable);
		task.setPeriod(500);
		task.setDelay(3000); // nice long wait...

		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		factory.setScheduledExecutorTasks(task);
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
	void settingThreadFactoryToNullForcesUseOfDefaultButIsOtherwiseCool() throws Exception {
		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
			@Override
			protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
				assertThat("Bah; the setThreadFactory(..) method must use a default ThreadFactory if a null arg is passed in.").isNotNull();
				return super.createExecutor(poolSize, threadFactory, rejectedExecutionHandler);
			}
		};
		factory.setScheduledExecutorTasks(new NoOpScheduledExecutorTask());
		factory.setThreadFactory(null); // the null must not propagate
		factory.afterPropertiesSet();
		factory.destroy();
	}

	@Test
	@SuppressWarnings("serial")
	void settingRejectedExecutionHandlerToNullForcesUseOfDefaultButIsOtherwiseCool() throws Exception {
		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
			@Override
			protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
				assertThat("Bah; the setRejectedExecutionHandler(..) method must use a default RejectedExecutionHandler if a null arg is passed in.").isNotNull();
				return super.createExecutor(poolSize, threadFactory, rejectedExecutionHandler);
			}
		};
		factory.setScheduledExecutorTasks(new NoOpScheduledExecutorTask());
		factory.setRejectedExecutionHandler(null); // the null must not propagate
		factory.afterPropertiesSet();
		factory.destroy();
	}

	@Test
	void objectTypeReportsCorrectType() throws Exception {
		ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
		assertThat(factory.getObjectType()).isEqualTo(ScheduledExecutorService.class);
	}


	private static void pauseToLetTaskStart(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		}
		catch (InterruptedException ignored) {
		}
	}


	private static class NoOpScheduledExecutorTask extends ScheduledExecutorTask {

		NoOpScheduledExecutorTask() {
			super(new NoOpRunnable());
		}
	}

}
