/*
 * Copyright 2002-present the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.support.DelegatingErrorHandlingRunnable;
import org.springframework.scheduling.support.TaskUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @since 5.0.5
 */
class DecoratedThreadPoolTaskExecutorTests extends AbstractSchedulingTaskExecutorTests {

	private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();


	@Override
	protected AsyncTaskExecutor buildExecutor() {
		this.executor.setTaskDecorator(runnable ->
				new DelegatingErrorHandlingRunnable(runnable, TaskUtils.LOG_AND_PROPAGATE_ERROR_HANDLER));
		this.executor.setThreadNamePrefix(this.threadNamePrefix);
		this.executor.setMaxPoolSize(1);
		this.executor.afterPropertiesSet();
		return this.executor;
	}


	@Test
	void executeDecoratedTaskOnlyOnce() throws InterruptedException {
		AtomicInteger taskInvocations = new AtomicInteger();
		AtomicInteger decoratorInvocations = new AtomicInteger();
		this.executor.setTaskDecorator(task -> () -> {
			decoratorInvocations.incrementAndGet();
			task.run();
		});
		this.executor.setWaitForTasksToCompleteOnShutdown(true);

		this.executor.execute(taskInvocations::incrementAndGet);
		this.executor.shutdown();

		assertThat(this.executor.getThreadPoolExecutor().awaitTermination(5, TimeUnit.SECONDS)).isTrue();
		assertThat(taskInvocations.get()).isEqualTo(1);
		assertThat(decoratorInvocations.get()).isEqualTo(1);
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void shutdownCancelsQueuedDecoratedFutures(boolean decorateWithFuture) throws InterruptedException {
		blockWorker();
		List<FutureTask<?>> decoratedFutures = new ArrayList<>();
		this.executor.setTaskDecorator(task -> {
			if (decorateWithFuture) {
				FutureTask<?> decorated = new FutureTask<>(task, null);
				decoratedFutures.add(decorated);
				return decorated;
			}
			return () -> task.run();
		});
		AtomicInteger taskInvocations = new AtomicInteger();
		Runnable task = taskInvocations::incrementAndGet;
		Future<?> runnable = this.executor.submit(task);
		Future<Integer> callable = this.executor.submit(() -> taskInvocations.incrementAndGet());
		FutureTask<?> futureTask = new FutureTask<>(taskInvocations::incrementAndGet, null);
		this.executor.execute(futureTask);

		assertThat(this.executor.getQueueSize()).isEqualTo(3);
		this.executor.shutdown();

		for (Future<?> future : List.of(runnable, callable, futureTask)) {
			assertThat(future.isCancelled()).isTrue();
			assertThatExceptionOfType(CancellationException.class)
					.isThrownBy(() -> future.get(1, TimeUnit.SECONDS));
		}
		assertThat(decoratedFutures).hasSize((decorateWithFuture ? 3 : 0));
		assertThat(decoratedFutures).allMatch(Future::isCancelled);
		assertThat(taskInvocations.get()).isZero();
		assertThat(this.executor.getThreadPoolExecutor().awaitTermination(5, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void shutdownCancelsQueuedFutureWithIdentityDecorator() throws InterruptedException {
		blockWorker();
		this.executor.setTaskDecorator(task -> task);
		Future<?> future = this.executor.submit(() -> {});

		this.executor.shutdown();

		assertThat(future.isCancelled()).isTrue();
		assertThat(this.executor.getThreadPoolExecutor().awaitTermination(5, TimeUnit.SECONDS)).isTrue();
	}

	private void blockWorker() throws InterruptedException {
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		this.executor.execute(() -> {
			started.countDown();
			try {
				release.await();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		});
		assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
	}

}
