/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Juergen Hoeller
 * @since 7.0
 */
class SyncTaskExecutorTests {

	@Test
	void plainExecution() {
		SyncTaskExecutor executor = new SyncTaskExecutor();

		ConcurrentClass target = new ConcurrentClass();
		assertThatNoException().isThrownBy(() -> executor.execute(target::concurrentOperation));
		assertThat(executor.execute(target::concurrentOperationWithResult)).isEqualTo("result");
		assertThatIOException().isThrownBy(() -> executor.execute(target::concurrentOperationWithException));
	}

	@Test
	void withConcurrencyLimit() {
		SyncTaskExecutor executor = new SyncTaskExecutor();
		executor.setConcurrencyLimit(2);

		ConcurrentClass target = new ConcurrentClass();
		List<CompletableFuture<?>> futures = new ArrayList<>(10);
		for (int i = 0; i < 10; i++) {
			futures.add(CompletableFuture.runAsync(() -> executor.execute(target::concurrentOperation)));
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		assertThat(target.current).hasValue(0);
		assertThat(target.counter).hasValue(10);
	}

	@Test
	void withConcurrencyLimitAndResult() {
		SyncTaskExecutor executor = new SyncTaskExecutor();
		executor.setConcurrencyLimit(2);

		ConcurrentClass target = new ConcurrentClass();
		List<CompletableFuture<?>> futures = new ArrayList<>(10);
		for (int i = 0; i < 10; i++) {
			futures.add(CompletableFuture.runAsync(() ->
					assertThat(executor.execute(target::concurrentOperationWithResult)).isEqualTo("result")));
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		assertThat(target.current).hasValue(0);
		assertThat(target.counter).hasValue(10);
	}

	@Test
	void withConcurrencyLimitAndException() {
		SyncTaskExecutor executor = new SyncTaskExecutor();
		executor.setConcurrencyLimit(2);

		ConcurrentClass target = new ConcurrentClass();
		List<CompletableFuture<?>> futures = new ArrayList<>(10);
		for (int i = 0; i < 10; i++) {
			futures.add(CompletableFuture.runAsync(() ->
					assertThatIOException().isThrownBy(() -> executor.execute(target::concurrentOperationWithException))));
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		assertThat(target.current).hasValue(0);
		assertThat(target.counter).hasValue(10);
	}

	@Test
	void taskRejectedWhenConcurrencyLimitReached() throws Exception {
		if (ForkJoinPool.getCommonPoolParallelism() < 4) {
			return;  // not enough concurrency possible
		}

		SyncTaskExecutor executor = new SyncTaskExecutor();
		executor.setConcurrencyLimit(2);
		executor.setRejectTasksWhenLimitReached(true);

		ConcurrentClass target = new ConcurrentClass();
		List<CompletableFuture<?>> futures = new ArrayList<>(10);
		for (int i = 0; i < 2; i++) {
			futures.add(CompletableFuture.runAsync(() -> executor.execute(target::concurrentOperation)));
		}
		Thread.sleep(10);
		for (int i = 2; i < 4; i++) {
			futures.add(CompletableFuture.runAsync(() ->
					assertThatExceptionOfType(TaskRejectedException.class).isThrownBy(() -> executor.execute(target::concurrentOperation))));
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		assertThat(target.current).hasValue(0);
		assertThat(target.counter).hasValue(2);
	}


	static class ConcurrentClass {

		final AtomicInteger current = new AtomicInteger();

		final AtomicInteger counter = new AtomicInteger();

		public void concurrentOperation() {
			if (current.incrementAndGet() > 2) {
				throw new IllegalStateException();
			}
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
			current.decrementAndGet();
			counter.incrementAndGet();
		}

		public String concurrentOperationWithResult() {
			concurrentOperation();
			return "result";
		}

		public String concurrentOperationWithException() throws IOException {
			concurrentOperation();
			throw new IOException();
		}
	}

}
