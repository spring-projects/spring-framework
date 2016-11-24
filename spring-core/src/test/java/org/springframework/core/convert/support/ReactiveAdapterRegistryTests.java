/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.core.convert.support;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;

import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ReactiveAdapterRegistry}.
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("unchecked")
public class ReactiveAdapterRegistryTests {

	private ReactiveAdapterRegistry registry;


	@Before
	public void setUp() throws Exception {
		this.registry = new ReactiveAdapterRegistry();
	}


	@Test
	public void getDefaultAdapters() throws Exception {

		// Reactor
		assertNotNull(getAdapterTo(Mono.class));
		assertNotNull(getAdapterTo(Flux.class));

		assertNotNull(getAdapterTo(Publisher.class));
		assertNotNull(getAdapterTo(CompletableFuture.class));

		// RxJava 1
		assertNotNull(getAdapterTo(Observable.class));
		assertNotNull(getAdapterTo(Single.class));
		assertNotNull(getAdapterTo(Completable.class));

		// RxJava 2
		assertNotNull(getAdapterTo(Flowable.class));
		assertNotNull(getAdapterTo(io.reactivex.Observable.class));
		assertNotNull(getAdapterTo(io.reactivex.Single.class));
		assertNotNull(getAdapterTo(Maybe.class));
		assertNotNull(getAdapterTo(io.reactivex.Completable.class));
	}

	@Test
	public void publisherToFlux() throws Exception {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Publisher<Integer> source = Flowable.fromIterable(sequence);
		Object target = getAdapterTo(Flux.class).fromPublisher(source);
		assertTrue(target instanceof Flux);
		assertEquals(sequence, ((Flux<Integer>) target).collectList().blockMillis(1000));
	}

	// TODO: publisherToMono/CompletableFuture vs Single (ISE on multiple elements)?

	@Test
	public void publisherToMono() throws Exception {
		Publisher<Integer> source = Flowable.fromArray(1, 2, 3);
		Object target = getAdapterTo(Mono.class).fromPublisher(source);
		assertTrue(target instanceof Mono);
		assertEquals(new Integer(1), ((Mono<Integer>) target).blockMillis(1000));
	}

	@Test
	public void publisherToCompletableFuture() throws Exception {
		Publisher<Integer> source = Flowable.fromArray(1, 2, 3);
		Object target = getAdapterTo(CompletableFuture.class).fromPublisher(source);
		assertTrue(target instanceof CompletableFuture);
		assertEquals(new Integer(1), ((CompletableFuture<Integer>) target).get());
	}

	@Test
	public void publisherToRxObservable() throws Exception {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Publisher<Integer> source = Flowable.fromIterable(sequence);
		Object target = getAdapterTo(rx.Observable.class).fromPublisher(source);
		assertTrue(target instanceof rx.Observable);
		assertEquals(sequence, ((rx.Observable) target).toList().toBlocking().first());
	}

	@Test
	public void publisherToRxSingle() throws Exception {
		Publisher<Integer> source = Flowable.fromArray(1);
		Object target = getAdapterTo(rx.Single.class).fromPublisher(source);
		assertTrue(target instanceof rx.Single);
		assertEquals(new Integer(1), ((rx.Single<Integer>) target).toBlocking().value());
	}

	@Test
	public void publisherToRxCompletable() throws Exception {
		Publisher<Integer> source = Flowable.fromArray(1, 2, 3);
		Object target = getAdapterTo(rx.Completable.class).fromPublisher(source);
		assertTrue(target instanceof rx.Completable);
		assertNull(((rx.Completable) target).get());
	}

	@Test
	public void publisherToReactivexFlowable() throws Exception {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Publisher<Integer> source = Flux.fromIterable(sequence);
		Object target = getAdapterTo(io.reactivex.Flowable.class).fromPublisher(source);
		assertTrue(target instanceof io.reactivex.Flowable);
		assertEquals(sequence, ((io.reactivex.Flowable) target).toList().blockingGet());
	}

	@Test
	public void publisherToReactivexObservable() throws Exception {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Publisher<Integer> source = Flowable.fromIterable(sequence);
		Object target = getAdapterTo(io.reactivex.Observable.class).fromPublisher(source);
		assertTrue(target instanceof io.reactivex.Observable);
		assertEquals(sequence, ((io.reactivex.Observable) target).toList().blockingGet());
	}

	@Test
	public void publisherToReactivexSingle() throws Exception {
		Publisher<Integer> source = Flowable.fromArray(1);
		Object target = getAdapterTo(io.reactivex.Single.class).fromPublisher(source);
		assertTrue(target instanceof io.reactivex.Single);
		assertEquals(new Integer(1), ((io.reactivex.Single<Integer>) target).blockingGet());
	}

	@Test
	public void publisherToReactivexCompletable() throws Exception {
		Publisher<Integer> source = Flowable.fromArray(1, 2, 3);
		Object target = getAdapterTo(io.reactivex.Completable.class).fromPublisher(source);
		assertTrue(target instanceof io.reactivex.Completable);
		assertNull(((io.reactivex.Completable) target).blockingGet());
	}

	@Test
	public void rxObservableToPublisher() throws Exception {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Object source = rx.Observable.from(sequence);
		Object target = getAdapterFrom(rx.Observable.class).toPublisher(source);
		assertTrue("Expected Flux Publisher: " + target.getClass().getName(), target instanceof Flux);
		assertEquals(sequence, ((Flux<Integer>) target).collectList().blockMillis(1000));
	}

	@Test
	public void rxSingleToPublisher() throws Exception {
		Object source = rx.Single.just(1);
		Object target = getAdapterFrom(rx.Single.class).toPublisher(source);
		assertTrue("Expected Mono Publisher: " + target.getClass().getName(), target instanceof Mono);
		assertEquals(new Integer(1), ((Mono<Integer>) target).blockMillis(1000));
	}

	@Test
	public void rxCompletableToPublisher() throws Exception {
		Object source = rx.Completable.complete();
		Object target = getAdapterFrom(rx.Completable.class).toPublisher(source);
		assertTrue("Expected Mono Publisher: " + target.getClass().getName(), target instanceof Mono);
		((Mono<Void>) target).blockMillis(1000);
	}

	@Test
	public void reactivexFlowableToPublisher() throws Exception {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Object source = io.reactivex.Flowable.fromIterable(sequence);
		Object target = getAdapterFrom(io.reactivex.Flowable.class).toPublisher(source);
		assertTrue("Expected Flux Publisher: " + target.getClass().getName(), target instanceof Flux);
		assertEquals(sequence, ((Flux<Integer>) target).collectList().blockMillis(1000));
	}

	@Test
	public void reactivexObservableToPublisher() throws Exception {
		List<Integer> sequence = Arrays.asList(1, 2, 3);
		Object source = io.reactivex.Observable.fromIterable(sequence);
		Object target = getAdapterFrom(io.reactivex.Observable.class).toPublisher(source);
		assertTrue("Expected Flux Publisher: " + target.getClass().getName(), target instanceof Flux);
		assertEquals(sequence, ((Flux<Integer>) target).collectList().blockMillis(1000));
	}

	@Test
	public void reactivexSingleToPublisher() throws Exception {
		Object source = io.reactivex.Single.just(1);
		Object target = getAdapterFrom(io.reactivex.Single.class).toPublisher(source);
		assertTrue("Expected Mono Publisher: " + target.getClass().getName(), target instanceof Mono);
		assertEquals(new Integer(1), ((Mono<Integer>) target).blockMillis(1000));
	}

	@Test
	public void reactivexCompletableToPublisher() throws Exception {
		Object source = io.reactivex.Completable.complete();
		Object target = getAdapterFrom(io.reactivex.Completable.class).toPublisher(source);
		assertTrue("Expected Mono Publisher: " + target.getClass().getName(), target instanceof Mono);
		((Mono<Void>) target).blockMillis(1000);
	}

	@Test
	public void CompletableFutureToPublisher() throws Exception {
		CompletableFuture<Integer> future = new CompletableFuture();
		future.complete(1);
		Object target = getAdapterFrom(CompletableFuture.class).toPublisher(future);
		assertTrue("Expected Mono Publisher: " + target.getClass().getName(), target instanceof Mono);
		assertEquals(new Integer(1), ((Mono<Integer>) target).blockMillis(1000));
	}


	private ReactiveAdapter getAdapterTo(Class<?> reactiveType) {
		return this.registry.getAdapterTo(reactiveType);
	}

	private ReactiveAdapter getAdapterFrom(Class<?> reactiveType) {
		return this.registry.getAdapterFrom(reactiveType);
	}

}
