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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.observation.ObservationRegistry;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupport.createSubscriptionRunnable;
import static org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupport.getPublisherFor;
import static org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupport.isReactive;

/**
 * @author Simon Baslé
 * @since 6.1
 */
class ScheduledAnnotationReactiveSupportTests {

	@Test
	void ensureReactor() {
		assertThat(ScheduledAnnotationReactiveSupport.reactorPresent).isTrue();
	}

	@ParameterizedTest
	// Note: monoWithParams can't be found by this test.
	@ValueSource(strings = { "mono", "flux", "monoString", "fluxString", "publisherMono",
			"publisherString", "monoThrows", "flowable", "completable" })
	void checkIsReactive(String method) {
		Method m = ReflectionUtils.findMethod(ReactiveMethods.class, method);
		assertThat(isReactive(m)).as(m.getName()).isTrue();
	}

	@Test
	void checkNotReactive() {
		Method string = ReflectionUtils.findMethod(ReactiveMethods.class, "oops");

		assertThat(isReactive(string)).as("String-returning").isFalse();
	}

	@Test
	void rejectReactiveAdaptableButNotDeferred() {
		Method future = ReflectionUtils.findMethod(ReactiveMethods.class, "future");

		assertThatIllegalArgumentException().isThrownBy(() -> isReactive(future))
				.withMessage("Reactive methods may only be annotated with @Scheduled if the return type supports deferred execution");
	}

	@Test
	void isReactiveRejectsWithParams() {
		Method m = ReflectionUtils.findMethod(ReactiveMethods.class, "monoWithParam", String.class);

		// isReactive rejects with context
		assertThatIllegalArgumentException().isThrownBy(() -> isReactive(m))
				.withMessage("Reactive methods may only be annotated with @Scheduled if declared without arguments")
				.withNoCause();
	}

	@Test
	void rejectCantProducePublisher() {
		ReactiveMethods target = new ReactiveMethods();
		Method m = ReflectionUtils.findMethod(ReactiveMethods.class, "monoThrows");

		// static helper method
		assertThatIllegalArgumentException().isThrownBy(() -> getPublisherFor(m, target))
				.withMessage("Cannot obtain a Publisher-convertible value from the @Scheduled reactive method")
				.withCause(new IllegalStateException("expected"));
	}

	@Test
	void rejectCantAccessMethod() {
		ReactiveMethods target = new ReactiveMethods();
		Method m = ReflectionUtils.findMethod(ReactiveMethods.class, "monoThrowsIllegalAccess");

		// static helper method
		assertThatIllegalArgumentException().isThrownBy(() -> getPublisherFor(m, target))
				.withMessage("Cannot obtain a Publisher-convertible value from the @Scheduled reactive method")
				.withCause(new IllegalAccessException("expected"));
	}

	@Test
	void fixedDelayIsBlocking() {
		ReactiveMethods target = new ReactiveMethods();
		Method m = ReflectionUtils.findMethod(ReactiveMethods.class, "mono");
		Scheduled fixedDelayString = AnnotationUtils.synthesizeAnnotation(Map.of("fixedDelayString", "123"), Scheduled.class, null);
		Scheduled fixedDelayLong = AnnotationUtils.synthesizeAnnotation(Map.of("fixedDelay", 123L), Scheduled.class, null);
		List<Runnable> tracker = new ArrayList<>();

		assertThat(createSubscriptionRunnable(m, target, fixedDelayString, () -> ObservationRegistry.NOOP, tracker))
				.isInstanceOfSatisfying(ScheduledAnnotationReactiveSupport.SubscribingRunnable.class, sr ->
						assertThat(sr.shouldBlock).as("fixedDelayString.shouldBlock").isTrue()
				);

		assertThat(createSubscriptionRunnable(m, target, fixedDelayLong, () -> ObservationRegistry.NOOP, tracker))
				.isInstanceOfSatisfying(ScheduledAnnotationReactiveSupport.SubscribingRunnable.class, sr ->
						assertThat(sr.shouldBlock).as("fixedDelayLong.shouldBlock").isTrue()
				);
	}

	@Test
	void fixedRateIsNotBlocking() {
		ReactiveMethods target = new ReactiveMethods();
		Method m = ReflectionUtils.findMethod(ReactiveMethods.class, "mono");
		Scheduled fixedRateString = AnnotationUtils.synthesizeAnnotation(Map.of("fixedRateString", "123"), Scheduled.class, null);
		Scheduled fixedRateLong = AnnotationUtils.synthesizeAnnotation(Map.of("fixedRate", 123L), Scheduled.class, null);
		List<Runnable> tracker = new ArrayList<>();

		assertThat(createSubscriptionRunnable(m, target, fixedRateString, () -> ObservationRegistry.NOOP, tracker))
				.isInstanceOfSatisfying(ScheduledAnnotationReactiveSupport.SubscribingRunnable.class, sr ->
						assertThat(sr.shouldBlock).as("fixedRateString.shouldBlock").isFalse()
				);

		assertThat(createSubscriptionRunnable(m, target, fixedRateLong, () -> ObservationRegistry.NOOP, tracker))
				.isInstanceOfSatisfying(ScheduledAnnotationReactiveSupport.SubscribingRunnable.class, sr ->
						assertThat(sr.shouldBlock).as("fixedRateLong.shouldBlock").isFalse()
				);
	}

	@Test
	void cronIsNotBlocking() {
		ReactiveMethods target = new ReactiveMethods();
		Method m = ReflectionUtils.findMethod(ReactiveMethods.class, "mono");
		Scheduled cron = AnnotationUtils.synthesizeAnnotation(Map.of("cron", "-"), Scheduled.class, null);
		List<Runnable> tracker = new ArrayList<>();

		assertThat(createSubscriptionRunnable(m, target, cron, () -> ObservationRegistry.NOOP, tracker))
				.isInstanceOfSatisfying(ScheduledAnnotationReactiveSupport.SubscribingRunnable.class, sr ->
						assertThat(sr.shouldBlock).as("cron.shouldBlock").isFalse()
				);
	}

	@Test
	void hasCheckpointToString() {
		ReactiveMethods target = new ReactiveMethods();
		Method m = ReflectionUtils.findMethod(ReactiveMethods.class, "mono");
		Publisher<?> p = getPublisherFor(m, target);

		assertThat(p.getClass().getName())
				.as("checkpoint class")
				.isEqualTo("reactor.core.publisher.FluxOnAssembly");

		assertThat(p).hasToString("checkpoint(\"@Scheduled 'mono()' in 'org.springframework.scheduling.annotation.ScheduledAnnotationReactiveSupportTests$ReactiveMethods'\")");
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
			// simulate a reflection issue
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
			return Mono.<Void>empty().doOnSubscribe(s -> subscription.incrementAndGet());
		}

		public Mono<Void> monoError() {
			return Mono.error(new IllegalStateException("expected"));
		}
	}

}
