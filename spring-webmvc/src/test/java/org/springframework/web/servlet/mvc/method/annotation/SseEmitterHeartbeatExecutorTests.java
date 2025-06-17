/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

/**
 * @author Réda Housni Alaoui
 */
class SseEmitterHeartbeatExecutorTests {

	private static final MediaType TEXT_PLAIN_UTF8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

	private TestTaskScheduler taskScheduler;

	@BeforeEach
	void beforeEach() {
		this.taskScheduler = new TestTaskScheduler();
	}

	@Test
	@DisplayName("It sends heartbeat at a fixed rate")
	void test1() {
		SseEmitterHeartbeatExecutor executor = new SseEmitterHeartbeatExecutor(taskScheduler, Duration.ofSeconds(5));

		TestEmitter emitter = createEmitter();
		executor.register(emitter.emitter());
		assertThat(taskScheduler.fixedRateTask).isNotNull();
		assertThat(taskScheduler.fixedRatePeriod).isEqualTo(Duration.ofSeconds(5));
		taskScheduler.fixedRateTask.run();

		emitter.handler.assertSentObjectCount(3);
		emitter.handler.assertObject(0, "event:ping\ndata:", TEXT_PLAIN_UTF8);
		emitter.handler.assertObject(1, "ping", MediaType.TEXT_PLAIN);
		emitter.handler.assertObject(2, "\n\n", TEXT_PLAIN_UTF8);
		emitter.handler.assertWriteCount(1);
	}

	@Test
	@DisplayName("Emitter is unregistered on completion")
	void test2() {
		SseEmitterHeartbeatExecutor executor = new SseEmitterHeartbeatExecutor(taskScheduler, Duration.ofSeconds(5));

		TestEmitter emitter = createEmitter();
		executor.register(emitter.emitter());

		assertThat(executor.isRegistered(emitter.emitter)).isTrue();
		emitter.emitter.complete();
		assertThat(executor.isRegistered(emitter.emitter)).isFalse();
	}

	@Test
	@DisplayName("Emitter is unregistered on error")
	void test3() {
		SseEmitterHeartbeatExecutor executor = new SseEmitterHeartbeatExecutor(taskScheduler, Duration.ofSeconds(5));

		TestEmitter emitter = createEmitter();
		executor.register(emitter.emitter());

		assertThat(executor.isRegistered(emitter.emitter)).isTrue();
		emitter.emitter.completeWithError(new RuntimeException());
		assertThat(executor.isRegistered(emitter.emitter)).isFalse();
	}

	@Test
	@DisplayName("Emitter is unregistered on timeout")
	void test4() {
		SseEmitterHeartbeatExecutor executor = new SseEmitterHeartbeatExecutor(taskScheduler, Duration.ofSeconds(5));

		TestEmitter emitter = createEmitter();
		executor.register(emitter.emitter());

		assertThat(executor.isRegistered(emitter.emitter)).isTrue();
		emitter.handler.completeWithTimeout();
		assertThat(executor.isRegistered(emitter.emitter)).isFalse();
	}

	@Test
	@DisplayName("Emitters are unregistered on executor shutdown")
	void test5() {
		SseEmitterHeartbeatExecutor executor = new SseEmitterHeartbeatExecutor(taskScheduler, Duration.ofSeconds(5));

		TestEmitter emitter = createEmitter();
		executor.register(emitter.emitter());

		assertThat(executor.isRegistered(emitter.emitter)).isTrue();
	}

	@Test
	@DisplayName("The task never throws")
	void test6() {
		SseEmitterHeartbeatExecutor executor = new SseEmitterHeartbeatExecutor(taskScheduler, Duration.ofSeconds(5));

		TestEmitter emitter = createEmitter();
		executor.register(emitter.emitter());
		assertThat(taskScheduler.fixedRateTask).isNotNull();
		emitter.handler.exceptionToThrowOnSend = new RuntimeException();

		assertThatCode(() -> taskScheduler.fixedRateTask.run()).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("The heartbeat rate can be customized")
	void test7() {
		SseEmitterHeartbeatExecutor executor = new SseEmitterHeartbeatExecutor(taskScheduler, Duration.ofSeconds(30));
		TestEmitter emitter = createEmitter();
		executor.register(emitter.emitter());
		assertThat(taskScheduler.fixedRateTask).isNotNull();
		assertThat(taskScheduler.fixedRatePeriod).isEqualTo(Duration.ofSeconds(30));
	}

	private TestEmitter createEmitter() {
		SseEmitter sseEmitter = new SseEmitter();
		TestEmitterHandler handler = new TestEmitterHandler();
		try {
			sseEmitter.initialize(handler);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new TestEmitter(sseEmitter, handler);
	}

	private record TestEmitter(SseEmitter emitter, TestEmitterHandler handler) {

	}

	private static class TestEmitterHandler implements ResponseBodyEmitter.Handler {

		private final List<Object> objects = new ArrayList<>();

		private final List<@Nullable MediaType> mediaTypes = new ArrayList<>();

		private final List<Runnable> timeoutCallbacks = new ArrayList<>();
		private final List<Runnable> completionCallbacks = new ArrayList<>();
		private final List<Consumer<Throwable>> errorCallbacks = new ArrayList<>();

		private int writeCount;
		@Nullable
		private RuntimeException exceptionToThrowOnSend;

		public void assertSentObjectCount(int size) {
			assertThat(this.objects).hasSize(size);
		}

		public void assertObject(int index, Object object, MediaType mediaType) {
			assertThat(index).isLessThanOrEqualTo(this.objects.size());
			assertThat(this.objects.get(index)).isEqualTo(object);
			assertThat(this.mediaTypes.get(index)).isEqualTo(mediaType);
		}

		public void assertWriteCount(int writeCount) {
			assertThat(this.writeCount).isEqualTo(writeCount);
		}

		@Override
		public void send(Object data, @Nullable MediaType mediaType) {
			failSendIfNeeded();
			this.objects.add(data);
			this.mediaTypes.add(mediaType);
			this.writeCount++;
		}

		@Override
		public void send(Set<ResponseBodyEmitter.DataWithMediaType> items) {
			failSendIfNeeded();
			for (ResponseBodyEmitter.DataWithMediaType item : items) {
				this.objects.add(item.getData());
				this.mediaTypes.add(item.getMediaType());
			}
			this.writeCount++;
		}

		private void failSendIfNeeded() {
			Optional.ofNullable(exceptionToThrowOnSend)
					.ifPresent(e -> {
						throw e;
					});
		}

		@Override
		public void onCompletion(Runnable callback) {
			completionCallbacks.add(callback);
		}

		@Override
		public void onTimeout(Runnable callback) {
			timeoutCallbacks.add(callback);
		}

		@Override
		public void onError(Consumer<Throwable> callback) {
			errorCallbacks.add(callback);
		}

		@Override
		public void complete() {
			completionCallbacks.forEach(Runnable::run);
		}

		@Override
		public void completeWithError(Throwable failure) {
			errorCallbacks.forEach(consumer -> consumer.accept(failure));
		}

		public void completeWithTimeout() {
			timeoutCallbacks.forEach(Runnable::run);
		}
	}

	private static class TestTaskScheduler implements TaskScheduler {

		@Nullable
		private Runnable fixedRateTask;
		@Nullable
		private Duration fixedRatePeriod;
		private final TestScheduledFuture<?> fixedRateFuture = new TestScheduledFuture<>();

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
			this.fixedRateTask = task;
			this.fixedRatePeriod = period;
			return fixedRateFuture;
		}

		@Override
		public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
			throw new UnsupportedOperationException();
		}
	}

	private static class TestScheduledFuture<T> implements ScheduledFuture<T> {

		private boolean canceled;
		private boolean interrupted;

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			canceled = true;
			interrupted = mayInterruptIfRunning;
			return true;
		}

		@Override
		public long getDelay(@NotNull TimeUnit timeUnit) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int compareTo(@NotNull Delayed delayed) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isCancelled() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isDone() {
			throw new UnsupportedOperationException();
		}

		@Override
		public T get() {
			throw new UnsupportedOperationException();
		}

		@Override
		public T get(long l, @NotNull TimeUnit timeUnit) {
			throw new UnsupportedOperationException();
		}
	}
}
