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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import reactor.adapter.RxJava1Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Observable;
import rx.Single;

import org.springframework.util.ClassUtils;

/**
 * A registry of adapters to adapt to {@link Flux} and {@link Mono}.
 *
 * <p>By default there are adapters for {@link CompletableFuture}, RxJava 1, and
 * also for a any Reactive Streams {@link Publisher}. Additional adapters can be
 * registered via {@link #registerFluxAdapter} and {@link #registerMonoAdapter}.
 *  <p> {@link Flux} and {@link Mono} 适配器的注册器
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactiveAdapterRegistry {

	private static final boolean rxJava1Present =
			ClassUtils.isPresent("rx.Observable", ReactiveAdapterRegistry.class.getClassLoader());

	private final Map<Class<?>, ReactiveAdapter> adapterMap = new LinkedHashMap<>(4);


	/**
	 * Create a registry and auto-register default adapters.
	 * 
	 * 创建一个注册器，自动注册默认的适配器
	 */
	public ReactiveAdapterRegistry() {
		// Flux and Mono ahead of Publisher...
		registerMonoAdapter(Mono.class,
				source -> (Mono<?>) source, source -> source,
				new ReactiveAdapter.Descriptor(false, true, false));
		registerFluxAdapter(
				Flux.class, source -> (Flux<?>) source, source -> source);
		registerFluxAdapter(
				Publisher.class, source -> Flux.from((Publisher<?>) source), source -> source);

		registerMonoAdapter(CompletableFuture.class,
				source -> Mono.fromFuture((CompletableFuture<?>) source), Mono::toFuture,
				new ReactiveAdapter.Descriptor(false, true, false)
		);

		if (rxJava1Present) {
			new RxJava1AdapterRegistrar().register(this);
		}
	}


	/**
	 * Register an adapter for adapting to and from a {@link Mono}.
	 * <p>The provided functions can assume that input will never be {@code null}
	 * and also that any {@link Optional} wrapper is unwrapped.
	 */
	public void registerMonoAdapter(Class<?> adapteeType, Function<Object, Mono<?>> toAdapter,
			Function<Mono<?>, Object> fromAdapter, ReactiveAdapter.Descriptor descriptor) {

		this.adapterMap.put(adapteeType, new MonoReactiveAdapter(toAdapter, fromAdapter, descriptor));
	}

	/**
	 * Register an adapter for adapting to and from a {@link Flux}.
	 * <p>The provided functions can assume that input will never be {@code null}
	 * and also that any {@link Optional} wrapper is unwrapped.
	 */
	public void registerFluxAdapter(Class<?> adapteeType, Function<Object, Flux<?>> toAdapter,
			Function<Flux<?>, Object> fromAdapter) {

		this.adapterMap.put(adapteeType, new FluxReactiveAdapter(toAdapter, fromAdapter));
	}


	/**
	 * Get the adapter for the given adaptee type to adapt from.
	 */
	public ReactiveAdapter getAdapterFrom(Class<?> adapteeType) {
		return getAdapterFrom(adapteeType, null);
	}

	/**
	 * Get the adapter for the given adaptee type to adapt from.
	 * If the instance is not {@code null} its actual type is used to check.
	 */
	public ReactiveAdapter getAdapterFrom(Class<?> adapteeType, Object adaptee) {
		Class<?> actualType = getActualType(adapteeType, adaptee);
		return getAdapterInternal(supportedType -> supportedType.isAssignableFrom(actualType));
	}

	/**
	 * Get the adapter for the given adaptee type to adapt to.
	 */
	public ReactiveAdapter getAdapterTo(Class<?> adapteeType) {
		return getAdapterTo(adapteeType, null);
	}

	/**
	 * Get the adapter for the given adaptee type to adapt to.
	 * If the instance is not {@code null} its actual type is used to check.
	 */
	public ReactiveAdapter getAdapterTo(Class<?> adapteeType, Object adaptee) {
		Class<?> actualType = getActualType(adapteeType, adaptee);
		return getAdapterInternal(supportedType -> supportedType.equals(actualType));
	}

	private ReactiveAdapter getAdapterInternal(Predicate<Class<?>> adapteeTypePredicate) {
		return this.adapterMap.keySet().stream()
				.filter(adapteeTypePredicate)
				.map(this.adapterMap::get)
				.findFirst()
				.orElse(null);
	}


	private static Class<?> getActualType(Class<?> adapteeType, Object adaptee) {
		adaptee = unwrapOptional(adaptee);
		return (adaptee != null ? adaptee.getClass() : adapteeType);
	}

	private static Object unwrapOptional(Object value) {
		return (value instanceof Optional ? ((Optional<?>) value).orElse(null) : value);
	}


	@SuppressWarnings("unchecked")
	private static class MonoReactiveAdapter implements ReactiveAdapter {

		private final Function<Object, Mono<?>> toAdapter;

		private final Function<Mono<?>, Object> fromAdapter;

		private final Descriptor descriptor;


		MonoReactiveAdapter(Function<Object, Mono<?>> to, Function<Mono<?>, Object> from, Descriptor descriptor) {
			this.toAdapter = to;
			this.fromAdapter = from;
			this.descriptor = descriptor;
		}

		@Override
		public Descriptor getDescriptor() {
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

		private final Descriptor descriptor = new Descriptor(true, true, false);


		FluxReactiveAdapter(Function<Object, Flux<?>> to, Function<Flux<?>, Object> from) {
			this.toAdapter = to;
			this.fromAdapter = from;
		}

		@Override
		public Descriptor getDescriptor() {
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
			registry.registerFluxAdapter(Observable.class,
					source -> RxJava1Adapter.observableToFlux((Observable<?>) source),
					RxJava1Adapter::publisherToObservable
			);
			registry.registerMonoAdapter(Single.class,
					source -> RxJava1Adapter.singleToMono((Single<?>) source),
					RxJava1Adapter::publisherToSingle,
					new ReactiveAdapter.Descriptor(false, false, false)
			);
			registry.registerMonoAdapter(Completable.class,
					source -> RxJava1Adapter.completableToMono((Completable) source),
					RxJava1Adapter::publisherToCompletable,
					new ReactiveAdapter.Descriptor(false, true, true)
			);
		}
	}

}
