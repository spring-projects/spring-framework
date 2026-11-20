/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.retry.support;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Juergen Hoeller
 * @since 7.0.4
 */
class RetryTaskTests {

	RetryPolicy retryPolicy = RetryPolicy.builder().maxRetries(2).delay(Duration.ZERO).build();

	RetryTemplate customTemplate = new RetryTemplate(RetryPolicy.builder().maxRetries(3).delay(Duration.ZERO).build());

	SyncTaskExecutor syncExecutor = new SyncTaskExecutor();

	AsyncTaskExecutor asyncExecutor = new SimpleAsyncTaskExecutor();

	AtomicInteger invocationCount = new AtomicInteger();


	@Test
	void syncTaskWithImmediateSuccess() {
		assertThat(
				syncExecutor.execute(new RetryTask<String, RuntimeException>(() -> {
					invocationCount.incrementAndGet();
					return "always succeeds";
				}, retryPolicy)))
				.isEqualTo("always succeeds");
		assertThat(invocationCount).hasValue(1);
	}

	@Test
	void syncTaskWithSuccessAfterInitialFailures() {
		assertThat(
				syncExecutor.execute(new RetryTask<String, RuntimeException>(() -> {
					if (invocationCount.incrementAndGet() < 2) {
						throw new IllegalStateException("Boom " + invocationCount.get());
					}
					return "finally succeeded";
				}, retryPolicy)))
				.isEqualTo("finally succeeded");
		assertThat(invocationCount).hasValue(2);
	}

	@Test
	void syncTaskWithExhaustedPolicy() {
		assertThatIllegalStateException().isThrownBy(() ->
				syncExecutor.execute(new RetryTask<>(() -> {
					invocationCount.incrementAndGet();
					throw new IllegalStateException("Boom " + invocationCount.get());
				}, retryPolicy)));
		assertThat(invocationCount).hasValue(3);
	}

	@Test
	void syncTaskWithCustomTemplate() {
		assertThatIllegalStateException().isThrownBy(() ->
				syncExecutor.execute(new RetryTask<>(() -> {
					invocationCount.incrementAndGet();
					throw new IllegalStateException("Boom " + invocationCount.get());
				}, customTemplate)));
		assertThat(invocationCount).hasValue(4);
	}

	@Test
	void asyncTaskWithImmediateSuccess() throws Exception {
		assertThat(
				asyncExecutor.submit(new RetryTask<>(() -> {
					invocationCount.incrementAndGet();
					return "always succeeds";
				}, retryPolicy)).get())
				.isEqualTo("always succeeds");
		assertThat(invocationCount).hasValue(1);
	}

	@Test
	void asyncTaskWithSuccessAfterInitialFailures() throws Exception {
		assertThat(
				asyncExecutor.submit(new RetryTask<>(() -> {
					if (invocationCount.incrementAndGet() < 2) {
						throw new IllegalStateException("Boom " + invocationCount.get());
					}
					return "finally succeeded";
				}, retryPolicy)).get())
				.isEqualTo("finally succeeded");
		assertThat(invocationCount).hasValue(2);
	}

	@Test
	void asyncTaskWithExhaustedPolicy() {
		assertThatExceptionOfType(ExecutionException.class).isThrownBy(() ->
				asyncExecutor.submit(new RetryTask<>(() -> {
					invocationCount.incrementAndGet();
					throw new IllegalStateException("Boom " + invocationCount.get());
				}, retryPolicy)).get())
				.withCauseExactlyInstanceOf(IllegalStateException.class);
		assertThat(invocationCount).hasValue(3);
	}

	@Test
	void asyncTaskWithCustomTemplate() {
		assertThatExceptionOfType(ExecutionException.class).isThrownBy(() ->
				asyncExecutor.submit(new RetryTask<>(() -> {
					invocationCount.incrementAndGet();
					throw new IllegalStateException("Boom " + invocationCount.get());
				}, customTemplate)).get())
				.withCauseExactlyInstanceOf(IllegalStateException.class);
		assertThat(invocationCount).hasValue(4);
	}

	@Test
	void callableWithImmediateSuccess() throws Exception {
		assertThat(
				asyncExecutor.submit(RetryTask.wrap(() -> {
					invocationCount.incrementAndGet();
					return "always succeeds";
				}, retryPolicy)).get())
				.isEqualTo("always succeeds");
		assertThat(invocationCount).hasValue(1);
	}

	@Test
	void callableWithSuccessAfterInitialFailures() throws Exception {
		assertThat(
				asyncExecutor.submit(RetryTask.wrap(() -> {
					if (invocationCount.incrementAndGet() < 2) {
						throw new IllegalStateException("Boom " + invocationCount.get());
					}
					return "finally succeeded";
				}, retryPolicy)).get())
				.isEqualTo("finally succeeded");
		assertThat(invocationCount).hasValue(2);
	}

	@Test
	void callableWithExhaustedPolicy() {
		assertThatExceptionOfType(ExecutionException.class).isThrownBy(() ->
				asyncExecutor.submit(RetryTask.wrap(() -> {
					invocationCount.incrementAndGet();
					throw new IllegalStateException("Boom " + invocationCount.get());
				}, retryPolicy)).get())
				.withCauseExactlyInstanceOf(IllegalStateException.class);
		assertThat(invocationCount).hasValue(3);
	}

	@Test
	void callableWithCustomTemplate() {
		assertThatExceptionOfType(ExecutionException.class).isThrownBy(() ->
				asyncExecutor.submit(RetryTask.wrap(() -> {
					invocationCount.incrementAndGet();
					throw new IllegalStateException("Boom " + invocationCount.get());
				}, customTemplate)).get())
				.withCauseExactlyInstanceOf(IllegalStateException.class);
		assertThat(invocationCount).hasValue(4);
	}

	@Test
	void runnableWithImmediateSuccess() throws Exception {
		assertThat(
				asyncExecutor.submit(RetryTask.wrap(() -> {
					if (true) {  // forcing Runnable over Callable on overloaded wrap method
						invocationCount.incrementAndGet();
					}
				}, retryPolicy)).get())
				.isNull();
		assertThat(invocationCount).hasValue(1);
	}

	@Test
	void runnableWithSuccessAfterInitialFailures() throws Exception {
		assertThat(
				asyncExecutor.submit(RetryTask.wrap(() -> {
					if (invocationCount.incrementAndGet() < 2) {
						throw new IllegalStateException("Boom " + invocationCount.get());
					}
				}, retryPolicy)).get())
				.isNull();
		assertThat(invocationCount).hasValue(2);
	}

	@Test
	void runnableWithExhaustedPolicy() {
		assertThatExceptionOfType(ExecutionException.class).isThrownBy(() ->
				asyncExecutor.submit(RetryTask.wrap(() -> {
					invocationCount.incrementAndGet();
					if (true) {  // forcing Runnable over Callable on overloaded wrap method
						throw new IllegalStateException("Boom " + invocationCount.get());
					}
				}, retryPolicy)).get())
				.withCauseExactlyInstanceOf(IllegalStateException.class);
		assertThat(invocationCount).hasValue(3);
	}

	@Test
	void runnableWithCustomTemplate() {
		assertThatExceptionOfType(ExecutionException.class).isThrownBy(() ->
				asyncExecutor.submit(RetryTask.wrap(() -> {
					invocationCount.incrementAndGet();
					if (true) {  // forcing Runnable over Callable on overloaded wrap method
						throw new IllegalStateException("Boom " + invocationCount.get());
					}
				}, customTemplate)).get())
				.withCauseExactlyInstanceOf(IllegalStateException.class);
		assertThat(invocationCount).hasValue(4);
	}

}
