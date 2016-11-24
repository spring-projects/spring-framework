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
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.RxReactiveStreams;

import org.springframework.util.ClassUtils;

/**
 * A registry of adapters to adapt to {@link Flux} and {@link Mono}.
 *
 * <p>By default, depending on classpath availability, adapters are registered
 * for RxJava 1, RxJava 2 types, and {@link CompletableFuture}. In addition the
 * registry contains adapters for Reactor's own Flux and Mono types (no-op)
 * along with adaption for any other Reactive Streams {@link Publisher}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class ReactiveAdapterRegistry {

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

		// Flux and Mono ahead of Publisher...

		registerMonoAdapter(Mono.class,
				source -> (Mono<?>) source, source -> source,
				ReactiveTypeDescriptor.singleOptionalValue(Mono.class));

		registerFluxAdapter(Flux.class,
				source -> (Flux<?>) source, source -> source);

		registerFluxAdapter(Publisher.class,
				source -> Flux.from((Publisher<?>) source), source -> source);

		registerMonoAdapter(CompletableFuture.class,
				source -> Mono.fromFuture((CompletableFuture<?>) source), Mono::toFuture,
				ReactiveTypeDescriptor.singleOptionalValue(CompletableFuture.class)
		);

		if (rxJava1Present && rxJava1Adapter) {
			new RxJava1AdapterRegistrar().register(this);
		}
		if (rxJava2Present) {
			new RxJava2AdapterRegistrar().register(this);
		}
	}


	/**
	 * Register an adapter for adapting to and from a {@link Mono}.
	 * <p>The provided functions can assume that input will never be {@code null}
	 * and also that any {@link Optional} wrapper is unwrapped.
	 */
	public void registerMonoAdapter(Class<?> reactiveType, Function<Object, Mono<?>> toAdapter,
			Function<Mono<?>, Object> fromAdapter, ReactiveTypeDescriptor descriptor) {

		this.adapters.add(new MonoReactiveAdapter(toAdapter, fromAdapter, descriptor));
	}

	/**
	 * Register an adapter for adapting to and from a {@link Flux}.
	 * <p>The provided functions can assume that input will never be {@code null}
	 * and also that any {@link Optional} wrapper is unwrapped.
	 */
	public void registerFluxAdapter(Class<?> reactiveType, Function<Object, Flux<?>> toAdapter,
			Function<Flux<?>, Object> fromAdapter) {

		this.adapters.add(new FluxReactiveAdapter(toAdapter, fromAdapter,
				ReactiveTypeDescriptor.multiValue(reactiveType)));
	}


	/**
	 * Get the adapter for the given reactive type to adapt from.
	 */
	public ReactiveAdapter getAdapterFrom(Class<?> reactiveType) {
		return getAdapterFrom(reactiveType, null);
	}

	/**
	 * Get the adapter for the given reactive type to adapt from.
	 * If the instance is not {@code null} its actual type is used to check.
	 */
	public ReactiveAdapter getAdapterFrom(Class<?> reactiveType, Object adaptee) {
		Class<?> actualType = getActualType(reactiveType, adaptee);
		return getAdapterInternal(supportedType -> supportedType.isAssignableFrom(actualType));
	}

	/**
	 * Get the adapter for the given reactive type to adapt to.
	 */
	public ReactiveAdapter getAdapterTo(Class<?> reactiveType) {
		return getAdapterTo(reactiveType, null);
	}

	/**
	 * Get the adapter for the given reactive type to adapt to.
	 * If the instance is not {@code null} its actual type is used to check.
	 */
	public ReactiveAdapter getAdapterTo(Class<?> reactiveType, Object adaptee) {
		Class<?> actualType = getActualType(reactiveType, adaptee);
		return getAdapterInternal(supportedType -> supportedType.equals(actualType));
	}

	private ReactiveAdapter getAdapterInternal(Predicate<Class<?>> predicate) {
		return this.adapters.stream()
				.filter(adapter -> predicate.test(adapter.getDescriptor().getReactiveType()))
				.findFirst()
				.orElse(null);
	}


	private static Class<?> getActualType(Class<?> reactiveType, Object adaptee) {
		adaptee = unwrapOptional(adaptee);
		return (adaptee != null ? adaptee.getClass() : reactiveType);
	}

	private static Object unwrapOptional(Object value) {
		return (value instanceof Optional ? ((Optional<?>) value).orElse(null) : value);
	}


	@SuppressWarnings("unchecked")
	private static class MonoReactiveAdapter implements ReactiveAdapter {

		private final Function<Object, Mono<?>> toAdapter;

		private final Function<Mono<?>, Object> fromAdapter;

		private final ReactiveTypeDescriptor descriptor;


		MonoReactiveAdapter(Function<Object, Mono<?>> to, Function<Mono<?>, Object> from,
				ReactiveTypeDescriptor descriptor) {

			this.toAdapter = to;
			this.fromAdapter = from;
			this.descriptor = descriptor;
		}

		@Override
		public ReactiveTypeDescriptor getDescriptor() {
			return this.descriptor;
		}

		@Override
		public <T> Mono<T> toMono(Object source) {
			source = unwrapOptional(source);
			if (source == null) {
				return Mono.empty();
			}
			return (Mono<T>) this.toAdapter.apply(source);
		}

		@Override
		public <T> Flux<T> toFlux(Object source) {
			source = unwrapOptional(source);
			if (source == null) {
				return Flux.empty();
			}
			return (Flux<T>) this.toMono(source).flux();
		}

		@Override
		public <T> Publisher<T> toPublisher(Object source) {
			return toMono(source);
		}

		@Override
		public Object fromPublisher(Publisher<?> source) {
			return (source != null ? this.fromAdapter.apply((Mono<?>) source) : null);
		}
	}

	@SuppressWarnings("unchecked")
	private static class FluxReactiveAdapter implements ReactiveAdapter {

		private final Function<Object, Flux<?>> toAdapter;

		private final Function<Flux<?>, Object> fromAdapter;

		private final ReactiveTypeDescriptor descriptor;


		FluxReactiveAdapter(Function<Object, Flux<?>> to, Function<Flux<?>, Object> from,
				ReactiveTypeDescriptor descriptor) {

			this.descriptor = descriptor;
			this.toAdapter = to;
			this.fromAdapter = from;
		}

		@Override
		public ReactiveTypeDescriptor getDescriptor() {
			return this.descriptor;
		}

		@Override
		public <T> Mono<T> toMono(Object source) {
			source = unwrapOptional(source);
			if (source == null) {
				return Mono.empty();
			}
			return (Mono<T>) this.toAdapter.apply(source).next();
		}

		@Override
		public <T> Flux<T> toFlux(Object source) {
			source = unwrapOptional(source);
			if (source == null) {
				return Flux.empty();
			}
			return (Flux<T>) this.toAdapter.apply(source);
		}

		@Override
		public <T> Publisher<T> toPublisher(Object source) {
			return toFlux(source);
		}

		@Override
		public Object fromPublisher(Publisher<?> source) {
			return (source != null ? this.fromAdapter.apply((Flux<?>) source) : null);
		}
	}


	private static class RxJava1AdapterRegistrar {

		public void register(ReactiveAdapterRegistry registry) {
			registry.registerFluxAdapter(rx.Observable.class,
					source -> Flux.from(RxReactiveStreams.toPublisher((rx.Observable<?>) source)),
					RxReactiveStreams::toObservable
			);
			registry.registerMonoAdapter(rx.Single.class,
					source -> Mono.from(RxReactiveStreams.toPublisher((rx.Single<?>) source)),
					RxReactiveStreams::toSingle,
					ReactiveTypeDescriptor.singleRequiredValue(rx.Single.class)
			);
			registry.registerMonoAdapter(rx.Completable.class,
					source -> Mono.from(RxReactiveStreams.toPublisher((rx.Completable) source)),
					RxReactiveStreams::toCompletable,
					ReactiveTypeDescriptor.noValue(rx.Completable.class)
			);
		}
	}

	private static class RxJava2AdapterRegistrar {

		public void register(ReactiveAdapterRegistry registry) {
			registry.registerFluxAdapter(Flowable.class,
					source -> Flux.from((Flowable<?>) source),
					source-> Flowable.fromPublisher(source)
			);
			registry.registerFluxAdapter(io.reactivex.Observable.class,
					source -> Flux.from(((io.reactivex.Observable<?>) source).toFlowable(BackpressureStrategy.BUFFER)),
					source -> Flowable.fromPublisher(source).toObservable()
			);
			registry.registerMonoAdapter(io.reactivex.Single.class,
					source -> Mono.from(((io.reactivex.Single<?>) source).toFlowable()),
					source -> Flowable.fromPublisher(source).toObservable().singleElement().toSingle(),
					ReactiveTypeDescriptor.singleRequiredValue(io.reactivex.Single.class)
			);
			registry.registerMonoAdapter(Maybe.class,
					source -> Mono.from(((Maybe<?>) source).toFlowable()),
					source -> Flowable.fromPublisher(source).toObservable().singleElement(),
					ReactiveTypeDescriptor.singleOptionalValue(Maybe.class)
			);
			registry.registerMonoAdapter(io.reactivex.Completable.class,
					source -> Mono.from(((io.reactivex.Completable) source).toFlowable()),
					source -> Flowable.fromPublisher(source).toObservable().ignoreElements(),
					ReactiveTypeDescriptor.noValue(io.reactivex.Completable.class)
			);
		}
	}

}
