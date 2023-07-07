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

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.NoOpRunnable;
import org.springframework.core.task.TaskDecorator;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
class ConcurrentTaskExecutorTests extends AbstractSchedulingTaskExecutorTests {

	private final ThreadPoolExecutor concurrentExecutor =
			new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());


	@Override
	protected AsyncListenableTaskExecutor buildExecutor() {
		concurrentExecutor.setThreadFactory(new CustomizableThreadFactory(this.threadNamePrefix));
		return new ConcurrentTaskExecutor(concurrentExecutor);
	}

	@Override
	@AfterEach
	void shutdownExecutor() {
		for (Runnable task : concurrentExecutor.shutdownNow()) {
			if (task instanceof RunnableFuture) {
				((RunnableFuture<?>) task).cancel(true);
			}
		}
	}


	@Test
	void zeroArgCtorResultsInDefaultTaskExecutorBeingUsed() {
		ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
		assertThatCode(() -> executor.execute(new NoOpRunnable())).doesNotThrowAnyException();
	}

	@Test
	void passingNullExecutorToCtorResultsInDefaultTaskExecutorBeingUsed() {
		ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor(null);
		assertThatCode(() -> executor.execute(new NoOpRunnable())).doesNotThrowAnyException();
	}

	@Test
	void earlySetConcurrentExecutorCallRespectsConfiguredTaskDecorator() {
		ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
		executor.setConcurrentExecutor(new DecoratedExecutor());
		executor.setTaskDecorator(new RunnableDecorator());
		assertThatCode(() -> executor.execute(new NoOpRunnable())).doesNotThrowAnyException();
	}

	@Test
	void lateSetConcurrentExecutorCallRespectsConfiguredTaskDecorator() {
		ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
		executor.setTaskDecorator(new RunnableDecorator());
		executor.setConcurrentExecutor(new DecoratedExecutor());
		assertThatCode(() -> executor.execute(new NoOpRunnable())).doesNotThrowAnyException();
	}


	private static class DecoratedRunnable implements Runnable {

		@Override
		public void run() {
		}
	}


	private static class RunnableDecorator implements TaskDecorator {

		@Override
		public Runnable decorate(Runnable runnable) {
			return new DecoratedRunnable();
		}
	}


	private static class DecoratedExecutor implements Executor {

		@Override
		public void execute(Runnable command) {
			Assert.state(command instanceof DecoratedRunnable, "TaskDecorator not applied");
		}
	}

}
