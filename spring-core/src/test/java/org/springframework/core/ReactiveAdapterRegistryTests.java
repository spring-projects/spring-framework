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

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import kotlinx.coroutines.Deferred;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ReactiveAdapterRegistry}.
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("unchecked")
class ReactiveAdapterRegistryTests {

	private final ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();


	@Test
	void defaultAdapterRegistrations() {

		// Reactor
		assertThat(getAdapter(Mono.class)).isNotNull();
		assertThat(getAdapter(Flux.class)).isNotNull();

		// Publisher
		assertThat(getAdapter(Publisher.class)).isNotNull();

		// Completable
		assertThat(getAdapter(CompletableFuture.class)).isNotNull();

		// RxJava 1
		assertThat(getAdapter(Observable.class)).isNotNull();
		assertThat(getAdapter(Single.class)).isNotNull();
		assertThat(getAdapter(Completable.class)).isNotNull();

		// RxJava 2
		assertThat(getAdapter(Flowable.class)).isNotNull();
		assertThat(getAdapter(io.reactivex.Observable.class)).isNotNull();
		assertThat(getAdapter(io.reactivex.Single.class)).isNotNull();
		assertThat(getAdapter(Maybe.class)).isNotNull();
		assertThat(getAdapter(io.reactivex.Completable.class)).isNotNull();

		// Coroutines
		assertThat(getAdapter(Deferred.class)).isNotNull();
	}

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

	@Test
	void publisherToFlux() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Publisher<Integer> source = Flowable.fromIterable(sequence);
		Object target = getAdapter(Flux.class).fromPublisher(source);
		boolean condition = target instanceof Flux;
		assertThat(condition).isTrue();
		assertThat(((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000))).isEqualTo(sequence);
	}

	// TODO: publisherToMono/CompletableFuture vs Single (ISE on multiple elements)?

	@Test
	void publisherToMono() {
		Publisher<Integer> source = Flowable.fromArray(1, 2, 3);
		Object target = getAdapter(Mono.class).fromPublisher(source);
		boolean condition = target instanceof Mono;
		assertThat(condition).isTrue();
		assertThat(((Mono<Integer>) target).block(Duration.ofMillis(1000))).isEqualTo(Integer.valueOf(1));
	}

	@Test
	void publisherToCompletableFuture() throws Exception {
		Publisher<Integer> source = Flowable.fromArray(1, 2, 3);
		Object target = getAdapter(CompletableFuture.class).fromPublisher(source);
		boolean condition = target instanceof CompletableFuture;
		assertThat(condition).isTrue();
		assertThat(((CompletableFuture<Integer>) target).get()).isEqualTo(Integer.valueOf(1));
	}

	@Test
	void publisherToRxObservable() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Publisher<Integer> source = Flowable.fromIterable(sequence);
		Object target = getAdapter(rx.Observable.class).fromPublisher(source);
		boolean condition = target instanceof Observable;
		assertThat(condition).isTrue();
		assertThat(((Observable<?>) target).toList().toBlocking().first()).isEqualTo(sequence);
	}

	@Test
	void publisherToRxSingle() {
		Publisher<Integer> source = Flowable.fromArray(1);
		Object target = getAdapter(rx.Single.class).fromPublisher(source);
		boolean condition = target instanceof Single;
		assertThat(condition).isTrue();
		assertThat(((Single<Integer>) target).toBlocking().value()).isEqualTo(Integer.valueOf(1));
	}

	@Test
	void publisherToRxCompletable() {
		Publisher<Integer> source = Flowable.fromArray(1, 2, 3);
		Object target = getAdapter(rx.Completable.class).fromPublisher(source);
		boolean condition = target instanceof Completable;
		assertThat(condition).isTrue();
		assertThat(((Completable) target).get()).isNull();
	}

	@Test
	void publisherToReactivexFlowable() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Publisher<Integer> source = Flux.fromIterable(sequence);
		Object target = getAdapter(io.reactivex.Flowable.class).fromPublisher(source);
		boolean condition = target instanceof Flowable;
		assertThat(condition).isTrue();
		assertThat(((Flowable<?>) target).toList().blockingGet()).isEqualTo(sequence);
	}

	@Test
	void publisherToReactivexObservable() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Publisher<Integer> source = Flowable.fromIterable(sequence);
		Object target = getAdapter(io.reactivex.Observable.class).fromPublisher(source);
		boolean condition = target instanceof io.reactivex.Observable;
		assertThat(condition).isTrue();
		assertThat(((io.reactivex.Observable<?>) target).toList().blockingGet()).isEqualTo(sequence);
	}

	@Test
	void publisherToReactivexSingle() {
		Publisher<Integer> source = Flowable.fromArray(1);
		Object target = getAdapter(io.reactivex.Single.class).fromPublisher(source);
		boolean condition = target instanceof io.reactivex.Single;
		assertThat(condition).isTrue();
		assertThat(((io.reactivex.Single<Integer>) target).blockingGet()).isEqualTo(Integer.valueOf(1));
	}

	@Test
	void publisherToReactivexCompletable() {
		Publisher<Integer> source = Flowable.fromArray(1, 2, 3);
		Object target = getAdapter(io.reactivex.Completable.class).fromPublisher(source);
		boolean condition = target instanceof io.reactivex.Completable;
		assertThat(condition).isTrue();
		((io.reactivex.Completable) target).blockingAwait();
	}

	@Test
	void rxObservableToPublisher() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Object source = rx.Observable.from(sequence);
		Object target = getAdapter(rx.Observable.class).toPublisher(source);
		boolean condition = target instanceof Flux;
		assertThat(condition).as("Expected Flux Publisher: " + target.getClass().getName()).isTrue();
		assertThat(((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000))).isEqualTo(sequence);
	}

	@Test
	void rxSingleToPublisher() {
		Object source = rx.Single.just(1);
		Object target = getAdapter(rx.Single.class).toPublisher(source);
		boolean condition = target instanceof Mono;
		assertThat(condition).as("Expected Mono Publisher: " + target.getClass().getName()).isTrue();
		assertThat(((Mono<Integer>) target).block(Duration.ofMillis(1000))).isEqualTo(Integer.valueOf(1));
	}

	@Test
	void rxCompletableToPublisher() {
		Object source = rx.Completable.complete();
		Object target = getAdapter(rx.Completable.class).toPublisher(source);
		boolean condition = target instanceof Mono;
		assertThat(condition).as("Expected Mono Publisher: " + target.getClass().getName()).isTrue();
		((Mono<Void>) target).block(Duration.ofMillis(1000));
	}

	@Test
	void reactivexFlowableToPublisher() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Object source = io.reactivex.Flowable.fromIterable(sequence);
		Object target = getAdapter(io.reactivex.Flowable.class).toPublisher(source);
		boolean condition = target instanceof Flux;
		assertThat(condition).as("Expected Flux Publisher: " + target.getClass().getName()).isTrue();
		assertThat(((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000))).isEqualTo(sequence);
	}

	@Test
	void reactivexObservableToPublisher() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Object source = io.reactivex.Observable.fromIterable(sequence);
		Object target = getAdapter(io.reactivex.Observable.class).toPublisher(source);
		boolean condition = target instanceof Flux;
		assertThat(condition).as("Expected Flux Publisher: " + target.getClass().getName()).isTrue();
		assertThat(((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000))).isEqualTo(sequence);
	}

	@Test
	void reactivexSingleToPublisher() {
		Object source = io.reactivex.Single.just(1);
		Object target = getAdapter(io.reactivex.Single.class).toPublisher(source);
		boolean condition = target instanceof Mono;
		assertThat(condition).as("Expected Mono Publisher: " + target.getClass().getName()).isTrue();
		assertThat(((Mono<Integer>) target).block(Duration.ofMillis(1000))).isEqualTo(Integer.valueOf(1));
	}

	@Test
	void reactivexCompletableToPublisher() {
		Object source = io.reactivex.Completable.complete();
		Object target = getAdapter(io.reactivex.Completable.class).toPublisher(source);
		boolean condition = target instanceof Mono;
		assertThat(condition).as("Expected Mono Publisher: " + target.getClass().getName()).isTrue();
		((Mono<Void>) target).block(Duration.ofMillis(1000));
	}

	@Test
	void completableFutureToPublisher() {
		CompletableFuture<Integer> future = new CompletableFuture<>();
		future.complete(1);
		Object target = getAdapter(CompletableFuture.class).toPublisher(future);
		boolean condition = target instanceof Mono;
		assertThat(condition).as("Expected Mono Publisher: " + target.getClass().getName()).isTrue();
		assertThat(((Mono<Integer>) target).block(Duration.ofMillis(1000))).isEqualTo(Integer.valueOf(1));
	}

	@Test
	void deferred() {
		assertThat(getAdapter(CompletableFuture.class).getDescriptor().isDeferred()).isEqualTo(false);

		assertThat(getAdapter(Mono.class).getDescriptor().isDeferred()).isEqualTo(true);
		assertThat(getAdapter(Flux.class).getDescriptor().isDeferred()).isEqualTo(true);

		assertThat(getAdapter(io.reactivex.Completable.class).getDescriptor().isDeferred()).isEqualTo(true);
		assertThat(getAdapter(io.reactivex.Single.class).getDescriptor().isDeferred()).isEqualTo(true);
		assertThat(getAdapter(io.reactivex.Flowable.class).getDescriptor().isDeferred()).isEqualTo(true);
		assertThat(getAdapter(io.reactivex.Observable.class).getDescriptor().isDeferred()).isEqualTo(true);

		assertThat(getAdapter(Deferred.class).getDescriptor().isDeferred()).isEqualTo(true);
		assertThat(getAdapter(kotlinx.coroutines.flow.Flow.class).getDescriptor().isDeferred()).isEqualTo(true);
	}

	private ReactiveAdapter getAdapter(Class<?> reactiveType) {
		ReactiveAdapter adapter = this.registry.getAdapter(reactiveType);
		assertThat(adapter).isNotNull();
		return adapter;
	}

}
