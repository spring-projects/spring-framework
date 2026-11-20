/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import org.springframework.core.task.SimpleAsyncTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Arjen Poutsma
 */
class FutureUtilsTests {

	@Test
	void callAsyncNormal() throws ExecutionException, InterruptedException {
		String foo = "Foo";
		CompletableFuture<String> future = FutureUtils.callAsync(() -> foo);

		assertThat(future.get()).isEqualTo(foo);
		assertThat(future.isCancelled()).isFalse();
		assertThat(future.isDone()).isTrue();

		CountDownLatch latch = new CountDownLatch(1);
		future.whenComplete((s, throwable) -> {
			assertThat(s).isEqualTo(foo);
			assertThat(throwable).isNull();
			latch.countDown();
		});
		latch.await();
	}

	@Test
	void callAsyncException() throws InterruptedException {
		RuntimeException ex = new RuntimeException("Foo");
		CompletableFuture<String> future = FutureUtils.callAsync(() -> {
			throw ex;
		});
		assertThatExceptionOfType(ExecutionException.class)
				.isThrownBy(future::get)
				.withCause(ex);
		assertThat(future.isCancelled()).isFalse();
		assertThat(future.isDone()).isTrue();

		CountDownLatch latch = new CountDownLatch(1);
		future.whenComplete((s, throwable) -> {
			assertThat(s).isNull();
			assertThat(throwable).isInstanceOf(CompletionException.class)
					.hasCause(ex);
			latch.countDown();
		});
		latch.await();
	}

	@Test
	void callAsyncNormalExecutor() throws ExecutionException, InterruptedException {
		String foo = "Foo";
		CompletableFuture<String> future = FutureUtils.callAsync(() -> foo, new SimpleAsyncTaskExecutor());

		assertThat(future.get()).isEqualTo(foo);
		assertThat(future.isCancelled()).isFalse();
		assertThat(future.isDone()).isTrue();

		CountDownLatch latch = new CountDownLatch(1);
		future.whenComplete((s, throwable) -> {
			assertThat(s).isEqualTo(foo);
			assertThat(throwable).isNull();
			latch.countDown();
		});
		latch.await();
	}

	@Test
	void callAsyncExceptionExecutor() throws InterruptedException {
		RuntimeException ex = new RuntimeException("Foo");
		CompletableFuture<String> future = FutureUtils.callAsync(() -> {
			throw ex;
		}, new SimpleAsyncTaskExecutor());
		assertThatExceptionOfType(ExecutionException.class)
				.isThrownBy(future::get)
				.withCause(ex);
		assertThat(future.isCancelled()).isFalse();
		assertThat(future.isDone()).isTrue();

		CountDownLatch latch = new CountDownLatch(1);
		future.whenComplete((s, throwable) -> {
			assertThat(s).isNull();
			assertThat(throwable).isInstanceOf(CompletionException.class)
					.hasCause(ex);
			latch.countDown();
		});
		latch.await();
	}

}
