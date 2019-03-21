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

package org.springframework.core;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ReactiveAdapterRegistry}.
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("unchecked")
public class ReactiveAdapterRegistryTests {

	private final ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();


	@Test
	public void defaultAdapterRegistrations() {

		// Reactor
		assertNotNull(getAdapter(Mono.class));
		assertNotNull(getAdapter(Flux.class));

		// Publisher
		assertNotNull(getAdapter(Publisher.class));

		// Completable
		assertNotNull(getAdapter(CompletableFuture.class));

		// RxJava 1
		assertNotNull(getAdapter(Observable.class));
		assertNotNull(getAdapter(Single.class));
		assertNotNull(getAdapter(Completable.class));

		// RxJava 2
		assertNotNull(getAdapter(Flowable.class));
		assertNotNull(getAdapter(io.reactivex.Observable.class));
		assertNotNull(getAdapter(io.reactivex.Single.class));
		assertNotNull(getAdapter(Maybe.class));
		assertNotNull(getAdapter(io.reactivex.Completable.class));
	}

	@Test
	public void getAdapterForReactiveSubType() {

		ReactiveAdapter adapter1 = getAdapter(Flux.class);
		ReactiveAdapter adapter2 = getAdapter(FluxProcessor.class);

		assertSame(adapter1, adapter2);

		this.registry.registerReactiveType(
				ReactiveTypeDescriptor.multiValue(FluxProcessor.class, FluxProcessor::empty),
				o -> (FluxProcessor<?, ?>) o,
				FluxProcessor::from);

		ReactiveAdapter adapter3 = getAdapter(FluxProcessor.class);

		assertNotNull(adapter3);
		assertNotSame(adapter1, adapter3);
	}

	@Test
	public void publisherToFlux() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Publisher<Integer> source = Flowable.fromIterable(sequence);
		Object target = getAdapter(Flux.class).fromPublisher(source);
		assertTrue(target instanceof Flux);
		assertEquals(sequence, ((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000)));
	}

	// TODO: publisherToMono/CompletableFuture vs Single (ISE on multiple elements)?

	@Test
	public void publisherToMono() {
		Publisher<Integer> source = Flowable.fromArray(1, 2, 3);
		Object target = getAdapter(Mono.class).fromPublisher(source);
		assertTrue(target instanceof Mono);
		assertEquals(Integer.valueOf(1), ((Mono<Integer>) target).block(Duration.ofMillis(1000)));
	}

	@Test
	public void publisherToCompletableFuture() throws Exception {
		Publisher<Integer> source = Flowable.fromArray(1, 2, 3);
		Object target = getAdapter(CompletableFuture.class).fromPublisher(source);
		assertTrue(target instanceof CompletableFuture);
		assertEquals(Integer.valueOf(1), ((CompletableFuture<Integer>) target).get());
	}

	@Test
	public void publisherToRxObservable() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Publisher<Integer> source = Flowable.fromIterable(sequence);
		Object target = getAdapter(rx.Observable.class).fromPublisher(source);
		assertTrue(target instanceof rx.Observable);
		assertEquals(sequence, ((rx.Observable<?>) target).toList().toBlocking().first());
	}

	@Test
	public void publisherToRxSingle() {
		Publisher<Integer> source = Flowable.fromArray(1);
		Object target = getAdapter(rx.Single.class).fromPublisher(source);
		assertTrue(target instanceof rx.Single);
		assertEquals(Integer.valueOf(1), ((rx.Single<Integer>) target).toBlocking().value());
	}

	@Test
	public void publisherToRxCompletable() {
		Publisher<Integer> source = Flowable.fromArray(1, 2, 3);
		Object target = getAdapter(rx.Completable.class).fromPublisher(source);
		assertTrue(target instanceof rx.Completable);
		assertNull(((rx.Completable) target).get());
	}

	@Test
	public void publisherToReactivexFlowable() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Publisher<Integer> source = Flux.fromIterable(sequence);
		Object target = getAdapter(io.reactivex.Flowable.class).fromPublisher(source);
		assertTrue(target instanceof io.reactivex.Flowable);
		assertEquals(sequence, ((io.reactivex.Flowable<?>) target).toList().blockingGet());
	}

	@Test
	public void publisherToReactivexObservable() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Publisher<Integer> source = Flowable.fromIterable(sequence);
		Object target = getAdapter(io.reactivex.Observable.class).fromPublisher(source);
		assertTrue(target instanceof io.reactivex.Observable);
		assertEquals(sequence, ((io.reactivex.Observable<?>) target).toList().blockingGet());
	}

	@Test
	public void publisherToReactivexSingle() {
		Publisher<Integer> source = Flowable.fromArray(1);
		Object target = getAdapter(io.reactivex.Single.class).fromPublisher(source);
		assertTrue(target instanceof io.reactivex.Single);
		assertEquals(Integer.valueOf(1), ((io.reactivex.Single<Integer>) target).blockingGet());
	}

	@Test
	public void publisherToReactivexCompletable() {
		Publisher<Integer> source = Flowable.fromArray(1, 2, 3);
		Object target = getAdapter(io.reactivex.Completable.class).fromPublisher(source);
		assertTrue(target instanceof io.reactivex.Completable);
		assertNull(((io.reactivex.Completable) target).blockingGet());
	}

	@Test
	public void rxObservableToPublisher() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Object source = rx.Observable.from(sequence);
		Object target = getAdapter(rx.Observable.class).toPublisher(source);
		assertTrue("Expected Flux Publisher: " + target.getClass().getName(), target instanceof Flux);
		assertEquals(sequence, ((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000)));
	}

	@Test
	public void rxSingleToPublisher() {
		Object source = rx.Single.just(1);
		Object target = getAdapter(rx.Single.class).toPublisher(source);
		assertTrue("Expected Mono Publisher: " + target.getClass().getName(), target instanceof Mono);
		assertEquals(Integer.valueOf(1), ((Mono<Integer>) target).block(Duration.ofMillis(1000)));
	}

	@Test
	public void rxCompletableToPublisher() {
		Object source = rx.Completable.complete();
		Object target = getAdapter(rx.Completable.class).toPublisher(source);
		assertTrue("Expected Mono Publisher: " + target.getClass().getName(), target instanceof Mono);
		((Mono<Void>) target).block(Duration.ofMillis(1000));
	}

	@Test
	public void reactivexFlowableToPublisher() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Object source = io.reactivex.Flowable.fromIterable(sequence);
		Object target = getAdapter(io.reactivex.Flowable.class).toPublisher(source);
		assertTrue("Expected Flux Publisher: " + target.getClass().getName(), target instanceof Flux);
		assertEquals(sequence, ((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000)));
	}

	@Test
	public void reactivexObservableToPublisher() {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Object source = io.reactivex.Observable.fromIterable(sequence);
		Object target = getAdapter(io.reactivex.Observable.class).toPublisher(source);
		assertTrue("Expected Flux Publisher: " + target.getClass().getName(), target instanceof Flux);
		assertEquals(sequence, ((Flux<Integer>) target).collectList().block(Duration.ofMillis(1000)));
	}

	@Test
	public void reactivexSingleToPublisher() {
		Object source = io.reactivex.Single.just(1);
		Object target = getAdapter(io.reactivex.Single.class).toPublisher(source);
		assertTrue("Expected Mono Publisher: " + target.getClass().getName(), target instanceof Mono);
		assertEquals(Integer.valueOf(1), ((Mono<Integer>) target).block(Duration.ofMillis(1000)));
	}

	@Test
	public void reactivexCompletableToPublisher() {
		Object source = io.reactivex.Completable.complete();
		Object target = getAdapter(io.reactivex.Completable.class).toPublisher(source);
		assertTrue("Expected Mono Publisher: " + target.getClass().getName(), target instanceof Mono);
		((Mono<Void>) target).block(Duration.ofMillis(1000));
	}

	@Test
	public void CompletableFutureToPublisher() {
		CompletableFuture<Integer> future = new CompletableFuture<>();
		future.complete(1);
		Object target = getAdapter(CompletableFuture.class).toPublisher(future);
		assertTrue("Expected Mono Publisher: " + target.getClass().getName(), target instanceof Mono);
		assertEquals(Integer.valueOf(1), ((Mono<Integer>) target).block(Duration.ofMillis(1000)));
	}


	private ReactiveAdapter getAdapter(Class<?> reactiveType) {
		return this.registry.getAdapter(reactiveType);
	}

}
