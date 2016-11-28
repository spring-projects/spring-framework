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

package org.springframework.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.RxReactiveStreams;

import org.springframework.util.ClassUtils;

/**
 * A registry of adapters to adapt a Reactive Streams {@link Publisher} to/from
 * various async/reactive types such as {@code CompletableFuture}, RxJava
 * {@code Observable}, and others.
 *
 * <p>By default, depending on classpath availability, adapters are registered
 * for Reactor, RxJava 1, RxJava 2 types, and {@link CompletableFuture}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class ReactiveAdapterRegistry {

	private static final boolean reactorPresent =
			ClassUtils.isPresent("reactor.core.publisher.Flux", ReactiveAdapterRegistry.class.getClassLoader());

	private static final boolean rxJava1Present =
			ClassUtils.isPresent("rx.Observable", ReactiveAdapterRegistry.class.getClassLoader());

	private static final boolean rxJava1Adapter =
			ClassUtils.isPresent("rx.RxReactiveStreams", ReactiveAdapterRegistry.class.getClassLoader());

	private static final boolean rxJava2Present =
			ClassUtils.isPresent("io.reactivex.Flowable", ReactiveAdapterRegistry.class.getClassLoader());


	private final List<ReactiveAdapter> adapters = new ArrayList<>(32);


	/**
	 * Create a registry and auto-register default adapters.
	 */
	public ReactiveAdapterRegistry() {

		if (reactorPresent) {
			new ReactorRegistrar().registerAdapters(this);
		}

		if (rxJava1Present && rxJava1Adapter) {
			new RxJava1Registrar().registerAdapters(this);
		}

		if (rxJava2Present) {
			new RxJava2Registrar().registerAdapters(this);
		}
	}


	/**
	 * Register a reactive type along with functions to adapt to and from a
	 * Reactive Streams {@link Publisher}. The functions can assume their
	 * input is never be {@code null} nor {@link Optional}.
	 */
	public void registerReactiveType(ReactiveTypeDescriptor descriptor,
			Function<Object, Publisher<?>> toAdapter, Function<Publisher<?>, Object> fromAdapter) {

		if (reactorPresent) {
			this.adapters.add(new ReactorAdapter(descriptor, toAdapter, fromAdapter));
		}
		else {
			this.adapters.add(new ReactiveAdapter(descriptor, toAdapter, fromAdapter));
		}
	}

	/**
	 * Get the adapter to use to adapt from the given reactive type.
	 */
	public ReactiveAdapter getAdapterFrom(Class<?> reactiveType) {
		return getAdapterFrom(reactiveType, null);
	}

	/**
	 * Get the adapter to use to adapt from the given reactive type. Or if the
	 * "source" object is not {@code null} its actual type is used instead.
	 */
	public ReactiveAdapter getAdapterFrom(Class<?> reactiveType, Object source) {
		source = (source instanceof Optional ? ((Optional<?>) source).orElse(null) : source);
		Class<?> clazz = (source != null ? source.getClass() : reactiveType);
		return getAdapter(type -> type.isAssignableFrom(clazz));
	}

	/**
	 * Get the adapter for the given reactive type to adapt to.
	 */
	public ReactiveAdapter getAdapterTo(Class<?> reactiveType) {
		return getAdapter(reactiveType::equals);
	}

	private ReactiveAdapter getAdapter(Predicate<Class<?>> predicate) {
		return this.adapters.stream()
				.filter(adapter -> predicate.test(adapter.getDescriptor().getReactiveType()))
				.findFirst()
				.orElse(null);
	}


	private static class ReactorRegistrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {

			// Flux and Mono ahead of Publisher...

			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleOptionalValue(Mono.class, Mono::empty),
					source -> (Mono<?>) source,
					Mono::from
			);

			registry.registerReactiveType(ReactiveTypeDescriptor.multiValue(Flux.class, Flux::empty),
					source -> (Flux<?>) source,
					Flux::from);

			registry.registerReactiveType(ReactiveTypeDescriptor.multiValue(Publisher.class, Flux::empty),
					source -> (Publisher<?>) source,
					source -> source);

			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleOptionalValue(CompletableFuture.class, () -> {
						CompletableFuture<?> empty = new CompletableFuture<>();
						empty.complete(null);
						return empty;
					}),
					source -> Mono.fromFuture((CompletableFuture<?>) source),
					source -> Mono.from(source).toFuture()
			);
		}
	}

	private static class RxJava1Registrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(rx.Observable.class, rx.Observable::empty),
					source -> RxReactiveStreams.toPublisher((rx.Observable<?>) source),
					RxReactiveStreams::toObservable
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleRequiredValue(rx.Single.class),
					source -> RxReactiveStreams.toPublisher((rx.Single<?>) source),
					RxReactiveStreams::toSingle
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.noValue(rx.Completable.class, Completable::complete),
					source -> RxReactiveStreams.toPublisher((rx.Completable) source),
					RxReactiveStreams::toCompletable
			);
		}
	}

	private static class RxJava2Registrar {

		void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(Flowable.class, Flowable::empty),
					source -> (Flowable<?>) source,
					source-> Flowable.fromPublisher(source)
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(Observable.class, Observable::empty),
					source -> ((Observable<?>) source).toFlowable(BackpressureStrategy.BUFFER),
					source -> Flowable.fromPublisher(source).toObservable()
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleRequiredValue(io.reactivex.Single.class),
					source -> ((io.reactivex.Single<?>) source).toFlowable(),
					source -> Flowable.fromPublisher(source).toObservable().singleElement().toSingle()
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleOptionalValue(Maybe.class, Maybe::empty),
					source -> ((Maybe<?>) source).toFlowable(),
					source -> Flowable.fromPublisher(source).toObservable().singleElement()
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.noValue(Completable.class, Completable::complete),
					source -> ((Completable) source).toFlowable(),
					source -> Flowable.fromPublisher(source).toObservable().ignoreElements()
			);
		}
	}

	/**
	 * Extension of ReactiveAdapter that wraps adapted (raw) Publisher's as
	 * {@link Flux} or {@link Mono} depending on the underlying reactive type's
	 * stream semantics.
	 */
	private static class ReactorAdapter extends ReactiveAdapter {

		ReactorAdapter(ReactiveTypeDescriptor descriptor,
				Function<Object, Publisher<?>> toPublisherFunction,
				Function<Publisher<?>, Object> fromPublisherFunction) {

			super(descriptor, toPublisherFunction, fromPublisherFunction);
		}

		@Override
		public <T> Publisher<T> toPublisher(Object source) {
			Publisher<T> publisher = super.toPublisher(source);
			return (getDescriptor().isMultiValue() ? Flux.from(publisher) : Mono.from(publisher));
		}
	}

}
