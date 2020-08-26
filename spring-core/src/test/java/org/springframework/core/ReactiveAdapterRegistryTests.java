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

package org.springframework.core;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import kotlinx.coroutines.Deferred;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ReactiveAdapterRegistry}.
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("unchecked")
class ReactiveAdapterRegistryTests {

	private final ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();


	@Test
	void getAdapterForReactiveSubType() {

		ReactiveAdapter adapter1 = getAdapter(Flux.class);
		ReactiveAdapter adapter2 = getAdapter(FluxProcessor.class);

		assertThat(adapter2).isSameAs(adapter1);

		this.registry.registerReactiveType(
				ReactiveTypeDescriptor.multiValue(FluxProcessor.class, FluxProcessor::empty),
				o -> (FluxProcessor<?, ?>) o,
				FluxProcessor::from);

		ReactiveAdapter adapter3 = getAdapter(FluxProcessor.class);

		assertThat(adapter3).isNotNull();
		assertThat(adapter3).isNotSameAs(adapter1);
	}

	@Nested
	class Reactor {

		@Test
		void defaultAdapterRegistrations() {

			// Reactor
			assertThat(getAdapter(Mono.class)).isNotNull();
			assertThat(getAdapter(Flux.class)).isNotNull();

			// Publisher
			assertThat(getAdapter(Publisher.class)).isNotNull();

			// Completable
			assertThat(getAdapter(CompletableFuture.class)).isNotNull();
		}

		@Test
		void toFlux() {
			List<Integer> sequence = Arrays.asList(1, 2, 3);
			Publisher<Integer> source = io.reactivex.rxjava3.core.Flowable.fromIterable(sequence);
			Object target = getAdapter(Flux.class).fromPublisher(source);
			assertThat(target instanceof Flux).isTrue();
			assertThat(((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000))).isEqualTo(sequence);
		}

		@Test
		void toMono() {
			Publisher<Integer> source = io.reactivex.rxjava3.core.Flowable.fromArray(1, 2, 3);
			Object target = getAdapter(Mono.class).fromPublisher(source);
			assertThat(target instanceof Mono).isTrue();
			assertThat(((Mono<Integer>) target).block(Duration.ofMillis(1000))).isEqualTo(Integer.valueOf(1));
		}

		@Test
		void toCompletableFuture() throws Exception {
			Publisher<Integer> source = Flux.fromArray(new Integer[] {1, 2, 3});
			Object target = getAdapter(CompletableFuture.class).fromPublisher(source);
			assertThat(target instanceof CompletableFuture).isTrue();
			assertThat(((CompletableFuture<Integer>) target).get()).isEqualTo(Integer.valueOf(1));
		}

		@Test
		void fromCompletableFuture() {
			CompletableFuture<Integer> future = new CompletableFuture<>();
			future.complete(1);
			Object target = getAdapter(CompletableFuture.class).toPublisher(future);
			assertThat(target instanceof Mono).as("Expected Mono Publisher: " + target.getClass().getName()).isTrue();
			assertThat(((Mono<Integer>) target).block(Duration.ofMillis(1000))).isEqualTo(Integer.valueOf(1));
		}
	}

	@Nested
	class RxJava1 {

		@Test
		void defaultAdapterRegistrations() {
			assertThat(getAdapter(rx.Observable.class)).isNotNull();
			assertThat(getAdapter(rx.Single.class)).isNotNull();
			assertThat(getAdapter(rx.Completable.class)).isNotNull();
		}

		@Test
		void toObservable() {
			List<Integer> sequence = Arrays.asList(1, 2, 3);
			Publisher<Integer> source = Flux.fromIterable(sequence);
			Object target = getAdapter(rx.Observable.class).fromPublisher(source);
			assertThat(target instanceof rx.Observable).isTrue();
			assertThat(((rx.Observable<?>) target).toList().toBlocking().first()).isEqualTo(sequence);
		}

		@Test
		void toSingle() {
			Publisher<Integer> source = Flux.fromArray(new Integer[] {1});
			Object target = getAdapter(rx.Single.class).fromPublisher(source);
			assertThat(target instanceof rx.Single).isTrue();
			assertThat(((rx.Single<Integer>) target).toBlocking().value()).isEqualTo(Integer.valueOf(1));
		}

		@Test
		void toCompletable() {
			Publisher<Integer> source = Flux.fromArray(new Integer[] {1, 2, 3});
			Object target = getAdapter(rx.Completable.class).fromPublisher(source);
			assertThat(target instanceof rx.Completable).isTrue();
			assertThat(((rx.Completable) target).get()).isNull();
		}

		@Test
		void fromObservable() {
			List<Integer> sequence = Arrays.asList(1, 2, 3);
			Object source = rx.Observable.from(sequence);
			Object target = getAdapter(rx.Observable.class).toPublisher(source);
			assertThat(target instanceof Flux).as("Expected Flux Publisher: " + target.getClass().getName()).isTrue();
			assertThat(((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000))).isEqualTo(sequence);
		}

		@Test
		void fromSingle() {
			Object source = rx.Single.just(1);
			Object target = getAdapter(rx.Single.class).toPublisher(source);
			assertThat(target instanceof Mono).as("Expected Mono Publisher: " + target.getClass().getName()).isTrue();
			assertThat(((Mono<Integer>) target).block(Duration.ofMillis(1000))).isEqualTo(Integer.valueOf(1));
		}

		@Test
		void fromCompletable() {
			Object source = rx.Completable.complete();
			Object target = getAdapter(rx.Completable.class).toPublisher(source);
			assertThat(target instanceof Mono).as("Expected Mono Publisher: " + target.getClass().getName()).isTrue();
			((Mono<Void>) target).block(Duration.ofMillis(1000));
		}
	}

	@Nested
	class RxJava2 {

		@Test
		void defaultAdapterRegistrations() {

			// RxJava 2
			assertThat(getAdapter(io.reactivex.Flowable.class)).isNotNull();
			assertThat(getAdapter(io.reactivex.Observable.class)).isNotNull();
			assertThat(getAdapter(io.reactivex.Single.class)).isNotNull();
			assertThat(getAdapter(io.reactivex.Maybe.class)).isNotNull();
			assertThat(getAdapter(io.reactivex.Completable.class)).isNotNull();
		}

		@Test
		void toFlowable() {
			List<Integer> sequence = Arrays.asList(1, 2, 3);
			Publisher<Integer> source = Flux.fromIterable(sequence);
			Object target = getAdapter(io.reactivex.Flowable.class).fromPublisher(source);
			assertThat(target instanceof io.reactivex.Flowable).isTrue();
			assertThat(((io.reactivex.Flowable<?>) target).toList().blockingGet()).isEqualTo(sequence);
		}

		@Test
		void toObservable() {
			List<Integer> sequence = Arrays.asList(1, 2, 3);
			Publisher<Integer> source = Flux.fromIterable(sequence);
			Object target = getAdapter(io.reactivex.Observable.class).fromPublisher(source);
			assertThat(target instanceof io.reactivex.Observable).isTrue();
			assertThat(((io.reactivex.Observable<?>) target).toList().blockingGet()).isEqualTo(sequence);
		}

		@Test
		void toSingle() {
			Publisher<Integer> source = Flux.fromArray(new Integer[] {1});
			Object target = getAdapter(io.reactivex.Single.class).fromPublisher(source);
			assertThat(target instanceof io.reactivex.Single).isTrue();
			assertThat(((io.reactivex.Single<Integer>) target).blockingGet()).isEqualTo(Integer.valueOf(1));
		}

		@Test
		void toCompletable() {
			Publisher<Integer> source = Flux.fromArray(new Integer[] {1, 2, 3});
			Object target = getAdapter(io.reactivex.Completable.class).fromPublisher(source);
			assertThat(target instanceof io.reactivex.Completable).isTrue();
			((io.reactivex.Completable) target).blockingAwait();
		}

		@Test
		void fromFlowable() {
			List<Integer> sequence = Arrays.asList(1, 2, 3);
			Object source = io.reactivex.Flowable.fromIterable(sequence);
			Object target = getAdapter(io.reactivex.Flowable.class).toPublisher(source);
			assertThat(target instanceof Flux).as("Expected Flux Publisher: " + target.getClass().getName()).isTrue();
			assertThat(((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000))).isEqualTo(sequence);
		}

		@Test
		void fromObservable() {
			List<Integer> sequence = Arrays.asList(1, 2, 3);
			Object source = io.reactivex.Observable.fromIterable(sequence);
			Object target = getAdapter(io.reactivex.Observable.class).toPublisher(source);
			assertThat(target instanceof Flux).as("Expected Flux Publisher: " + target.getClass().getName()).isTrue();
			assertThat(((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000))).isEqualTo(sequence);
		}

		@Test
		void fromSingle() {
			Object source = io.reactivex.Single.just(1);
			Object target = getAdapter(io.reactivex.Single.class).toPublisher(source);
			assertThat(target instanceof Mono).as("Expected Mono Publisher: " + target.getClass().getName()).isTrue();
			assertThat(((Mono<Integer>) target).block(Duration.ofMillis(1000))).isEqualTo(Integer.valueOf(1));
		}

		@Test
		void fromCompletable() {
			Object source = io.reactivex.Completable.complete();
			Object target = getAdapter(io.reactivex.Completable.class).toPublisher(source);
			assertThat(target instanceof Mono).as("Expected Mono Publisher: " + target.getClass().getName()).isTrue();
			((Mono<Void>) target).block(Duration.ofMillis(1000));
		}
	}

	@Nested
	class RxJava3 {

		@Test
		void defaultAdapterRegistrations() {

			// RxJava 3
			assertThat(getAdapter(io.reactivex.rxjava3.core.Flowable.class)).isNotNull();
			assertThat(getAdapter(io.reactivex.rxjava3.core.Observable.class)).isNotNull();
			assertThat(getAdapter(io.reactivex.rxjava3.core.Single.class)).isNotNull();
			assertThat(getAdapter(io.reactivex.rxjava3.core.Maybe.class)).isNotNull();
			assertThat(getAdapter(io.reactivex.rxjava3.core.Completable.class)).isNotNull();
		}

		@Test
		void toFlowable() {
			List<Integer> sequence = Arrays.asList(1, 2, 3);
			Publisher<Integer> source = Flux.fromIterable(sequence);
			Object target = getAdapter(io.reactivex.rxjava3.core.Flowable.class).fromPublisher(source);
			assertThat(target instanceof io.reactivex.rxjava3.core.Flowable).isTrue();
			assertThat(((io.reactivex.rxjava3.core.Flowable<?>) target).toList().blockingGet()).isEqualTo(sequence);
		}

		@Test
		void toObservable() {
			List<Integer> sequence = Arrays.asList(1, 2, 3);
			Publisher<Integer> source = Flux.fromIterable(sequence);
			Object target = getAdapter(io.reactivex.rxjava3.core.Observable.class).fromPublisher(source);
			assertThat(target instanceof io.reactivex.rxjava3.core.Observable).isTrue();
			assertThat(((io.reactivex.rxjava3.core.Observable<?>) target).toList().blockingGet()).isEqualTo(sequence);
		}

		@Test
		void toSingle() {
			Publisher<Integer> source = Flux.fromArray(new Integer[] {1});
			Object target = getAdapter(io.reactivex.rxjava3.core.Single.class).fromPublisher(source);
			assertThat(target instanceof io.reactivex.rxjava3.core.Single).isTrue();
			assertThat(((io.reactivex.rxjava3.core.Single<Integer>) target).blockingGet()).isEqualTo(Integer.valueOf(1));
		}

		@Test
		void toCompletable() {
			Publisher<Integer> source = Flux.fromArray(new Integer[] {1, 2, 3});
			Object target = getAdapter(io.reactivex.rxjava3.core.Completable.class).fromPublisher(source);
			assertThat(target instanceof io.reactivex.rxjava3.core.Completable).isTrue();
			((io.reactivex.rxjava3.core.Completable) target).blockingAwait();
		}

		@Test
		void fromFlowable() {
			List<Integer> sequence = Arrays.asList(1, 2, 3);
			Object source = io.reactivex.rxjava3.core.Flowable.fromIterable(sequence);
			Object target = getAdapter(io.reactivex.rxjava3.core.Flowable.class).toPublisher(source);
			assertThat(target instanceof Flux).as("Expected Flux Publisher: " + target.getClass().getName()).isTrue();
			assertThat(((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000))).isEqualTo(sequence);
		}

		@Test
		void fromObservable() {
			List<Integer> sequence = Arrays.asList(1, 2, 3);
			Object source = io.reactivex.rxjava3.core.Observable.fromIterable(sequence);
			Object target = getAdapter(io.reactivex.rxjava3.core.Observable.class).toPublisher(source);
			assertThat(target instanceof Flux).as("Expected Flux Publisher: " + target.getClass().getName()).isTrue();
			assertThat(((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000))).isEqualTo(sequence);
		}

		@Test
		void fromSingle() {
			Object source = io.reactivex.rxjava3.core.Single.just(1);
			Object target = getAdapter(io.reactivex.rxjava3.core.Single.class).toPublisher(source);
			assertThat(target instanceof Mono).as("Expected Mono Publisher: " + target.getClass().getName()).isTrue();
			assertThat(((Mono<Integer>) target).block(Duration.ofMillis(1000))).isEqualTo(Integer.valueOf(1));
		}

		@Test
		void fromCompletable() {
			Object source = io.reactivex.rxjava3.core.Completable.complete();
			Object target = getAdapter(io.reactivex.rxjava3.core.Completable.class).toPublisher(source);
			assertThat(target instanceof Mono).as("Expected Mono Publisher: " + target.getClass().getName()).isTrue();
			((Mono<Void>) target).block(Duration.ofMillis(1000));
		}
	}

	@Nested
	class Kotlin {

		@Test
		void defaultAdapterRegistrations() {

			// Coroutines
			assertThat(getAdapter(Deferred.class)).isNotNull();
		}

		@Test
		void deferred() {
			assertThat(getAdapter(CompletableFuture.class).getDescriptor().isDeferred()).isEqualTo(false);
			assertThat(getAdapter(Deferred.class).getDescriptor().isDeferred()).isEqualTo(true);
			assertThat(getAdapter(kotlinx.coroutines.flow.Flow.class).getDescriptor().isDeferred()).isEqualTo(true);
		}
	}

	private ReactiveAdapter getAdapter(Class<?> reactiveType) {
		ReactiveAdapter adapter = this.registry.getAdapter(reactiveType);
		assertThat(adapter).isNotNull();
		return adapter;
	}

}
