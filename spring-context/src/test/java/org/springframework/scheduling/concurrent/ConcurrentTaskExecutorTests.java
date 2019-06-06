/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.core.task.NoOpRunnable;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class ConcurrentTaskExecutorTests extends AbstractSchedulingTaskExecutorTests {

	private final ThreadPoolExecutor concurrentExecutor =
			new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());


	@Override
	protected AsyncListenableTaskExecutor buildExecutor() {
		concurrentExecutor.setThreadFactory(new CustomizableThreadFactory(THREAD_NAME_PREFIX));
		return new ConcurrentTaskExecutor(concurrentExecutor);
	}

	@Override
	public void shutdownExecutor() {
		List<Runnable> remainingTasks = concurrentExecutor.shutdownNow();
		for (Runnable task : remainingTasks) {
			if (task instanceof RunnableFuture) {
				((RunnableFuture<?>) task).cancel(true);
			}
		}
	}


	@Test
	public void zeroArgCtorResultsInDefaultTaskExecutorBeingUsed() {
		ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
		// must not throw a NullPointerException
		executor.execute(new NoOpRunnable());
	}

	@Test
	public void passingNullExecutorToCtorResultsInDefaultTaskExecutorBeingUsed() {
		ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor(null);
		// must not throw a NullPointerException
		executor.execute(new NoOpRunnable());
	}

	@Test
	public void passingNullExecutorToSetterResultsInDefaultTaskExecutorBeingUsed() {
		ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
		executor.setConcurrentExecutor(null);
		// must not throw a NullPointerException
		executor.execute(new NoOpRunnable());
	}

}
