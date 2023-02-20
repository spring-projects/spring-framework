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

package org.springframework.scheduling.annotation;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupport.getPublisherFor;
import static org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupport.isReactive;

class ScheduledAnnotationReactiveSupportTests {

	@Test
	void ensureReactor() {
		assertThat(ScheduledAnnotationReactiveSupport.reactorPresent).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "mono", "flux", "monoString", "fluxString", "publisherMono",
			"publisherString", "monoThrows", "flowable", "completable" }) //note: monoWithParams can't be found by this test
	void checkIsReactive(String method) {
		Method m = ReflectionUtils.findMethod(ReactiveMethods.class, method);
		assertThat(isReactive(m)).as(m.getName()).isTrue();
	}

	@Test
	void checkNotReactive() {
		Method string = ReflectionUtils.findMethod(ReactiveMethods.class, "oops");

		assertThat(isReactive(string))
				.as("String-returning").isFalse();
	}

	@Test
	void rejectReactiveAdaptableButNotDeferred() {
		Method future = ReflectionUtils.findMethod(ReactiveMethods.class, "future");

		assertThatIllegalArgumentException().isThrownBy(
				() -> isReactive(future)
		)
						.withMessage("Reactive methods may only be annotated with @Scheduled if the return type supports deferred execution");
	}

	static class ReactiveMethods {

		public String oops() {
			return "oops";
		}

		public Mono<Void> mono() {
			return Mono.empty();
		}

		public Flux<Void> flux() {
			return Flux.empty();
		}

		public Mono<String> monoString() {
			return Mono.just("example");
		}

		public Flux<String> fluxString() {
			return Flux.just("example");
		}

		public Publisher<Void> publisherMono() {
			return Mono.empty();
		}

		public Publisher<String> publisherString() {
			return fluxString();
		}

		public CompletableFuture<String> future() {
			return CompletableFuture.completedFuture("example");
		}

		public Mono<Void> monoWithParam(String param) {
			return Mono.just(param).then();
		}

		public Mono<Void> monoThrows() {
			throw new IllegalStateException("expected");
		}

		public Mono<Void> monoThrowsIllegalAccess() throws IllegalAccessException {
			//simulate a reflection issue
			throw new IllegalAccessException("expected");
		}

		public Flowable<Void> flowable() {
			return Flowable.empty();
		}

		public Completable completable() {
			return Completable.complete();
		}

		AtomicInteger subscription = new AtomicInteger();

		public Mono<Void> trackingMono() {
			return Mono.<Void>empty()
					.doOnSubscribe(s -> subscription.incrementAndGet());
		}

		public Mono<Void> monoError() {
			return Mono.error(new IllegalStateException("expected"));
		}

	}

	@Nested
	class ReactiveTaskTests {

		private ReactiveMethods target;

		@BeforeEach
		void init() {
			this.target = new ReactiveMethods();
		}

		@Test
		void isReactiveRejectsWithParams() {
			Method m = ReflectionUtils.findMethod(ReactiveMethods.class, "monoWithParam", String.class);

			//isReactive rejects with context
			assertThatIllegalArgumentException().isThrownBy(() -> isReactive(m))
					.withMessage("Reactive methods may only be annotated with @Scheduled if declared without arguments")
					.withNoCause();

			//constructor of task doesn't provide the context isReactive does
			assertThatIllegalArgumentException().isThrownBy(() -> new ScheduledAnnotationReactiveSupport.ReactiveTask(
							m, target, Duration.ZERO, Duration.ZERO, false))
					.withMessage("wrong number of arguments")
					.withNoCause();
		}

		@Test
		void rejectCantProducePublisher() {
			Method m = ReflectionUtils.findMethod(ReactiveMethods.class, "monoThrows");

			//static helper method
			assertThatIllegalArgumentException().isThrownBy(() -> getPublisherFor(m, target))
					.withMessage("Cannot obtain a Publisher-convertible value from the @Scheduled reactive method")
					.withCause(new IllegalStateException("expected"));

			//constructor of task
			assertThatIllegalArgumentException().isThrownBy(() -> new ScheduledAnnotationReactiveSupport.ReactiveTask(
							m, target, Duration.ZERO, Duration.ZERO, false))
					.withMessage("Cannot obtain a Publisher-convertible value from the @Scheduled reactive method")
					.withCause(new IllegalStateException("expected"));
		}

		@Test
		void rejectCantAccessMethod() {
			Method m = ReflectionUtils.findMethod(ReactiveMethods.class, "monoThrowsIllegalAccess");

			//static helper method
			assertThatIllegalArgumentException().isThrownBy(() -> getPublisherFor(m, target))
					.withMessage("Cannot obtain a Publisher-convertible value from the @Scheduled reactive method")
					.withCause(new IllegalAccessException("expected"));

			//constructor of task
			assertThatIllegalArgumentException().isThrownBy(() -> new ScheduledAnnotationReactiveSupport.ReactiveTask(
							m, target, Duration.ZERO, Duration.ZERO, false))
					.withMessage("Cannot obtain a Publisher-convertible value from the @Scheduled reactive method")
					.withCause(new IllegalAccessException("expected"));
		}

		@Test
		void hasCheckpointToString() {
			Method m = ReflectionUtils.findMethod(ReactiveMethods.class, "mono");
			final ScheduledAnnotationReactiveSupport.ReactiveTask reactiveTask = new ScheduledAnnotationReactiveSupport.ReactiveTask(
					m, target, Duration.ZERO, Duration.ZERO, false);

		assertThat(reactiveTask).hasToString("@Scheduled 'mono()' in bean 'org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupportTests$ReactiveMethods'");
		}

		@Test
		void cancelledEarlyPreventsSubscription() {
			Method m = ReflectionUtils.findMethod(ReactiveMethods.class, "trackingMono");
			final ScheduledAnnotationReactiveSupport.ReactiveTask reactiveTask = new ScheduledAnnotationReactiveSupport.ReactiveTask(
					m, target, Duration.ZERO, Duration.ofSeconds(10), false);
			reactiveTask.cancel();
			reactiveTask.subscribe();

			assertThat(target.subscription).hasValue(0);
		}

		@Test
		void noInitialDelayFixedDelay() throws InterruptedException {
			Method m = ReflectionUtils.findMethod(target.getClass(), "trackingMono");
			final ScheduledAnnotationReactiveSupport.ReactiveTask reactiveTask = new ScheduledAnnotationReactiveSupport.ReactiveTask(
					m, target, Duration.ZERO, Duration.ofSeconds(10), false);
			reactiveTask.subscribe();
			Thread.sleep(500);
			reactiveTask.cancel();

			assertThat(target.subscription).hasValue(1);
		}

		@Test
		void noInitialDelayFixedRate() throws InterruptedException {
			Method m = ReflectionUtils.findMethod(target.getClass(), "trackingMono");
			final ScheduledAnnotationReactiveSupport.ReactiveTask reactiveTask = new ScheduledAnnotationReactiveSupport.ReactiveTask(
					m, target, Duration.ZERO, Duration.ofSeconds(10), true);
			reactiveTask.subscribe();
			Thread.sleep(500);
			reactiveTask.cancel();

			assertThat(target.subscription).hasValue(1);
		}

		@Test
		void initialDelayFixedDelay() throws InterruptedException {
			Method m = ReflectionUtils.findMethod(target.getClass(), "trackingMono");
			final ScheduledAnnotationReactiveSupport.ReactiveTask reactiveTask = new ScheduledAnnotationReactiveSupport.ReactiveTask(
					m, target, Duration.ofSeconds(10), Duration.ofMillis(500), false);
			reactiveTask.subscribe();
			Thread.sleep(500);
			reactiveTask.cancel();

			assertThat(target.subscription).hasValue(0);
		}

		@Test
		void initialDelayFixedRate() throws InterruptedException {
			Method m = ReflectionUtils.findMethod(target.getClass(), "trackingMono");
			final ScheduledAnnotationReactiveSupport.ReactiveTask reactiveTask = new ScheduledAnnotationReactiveSupport.ReactiveTask(
					m, target, Duration.ofSeconds(10), Duration.ofMillis(500), true);
			reactiveTask.subscribe();
			Thread.sleep(500);
			reactiveTask.cancel();

			assertThat(target.subscription).hasValue(0);
		}

		@Test
		void monoErrorHasCheckpoint() throws InterruptedException {
			Method m = ReflectionUtils.findMethod(target.getClass(), "monoError");
			final ScheduledAnnotationReactiveSupport.ReactiveTask reactiveTask = new ScheduledAnnotationReactiveSupport.ReactiveTask(
					m, target, Duration.ZERO, Duration.ofSeconds(10), true);

			assertThat(reactiveTask.checkpoint).isEqualTo("@Scheduled 'monoError()' in bean "
					+ "'org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupportTests$ReactiveMethods'");
		}

	}
}
