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

		registerReactiveType(
				ReactiveTypeDescriptor.singleOptionalValue(Mono.class),
				source -> (Mono<?>) source,
				source -> source
		);

		registerReactiveType(ReactiveTypeDescriptor.multiValue(Flux.class),
				source -> (Flux<?>) source,
				source -> source);

		registerReactiveType(ReactiveTypeDescriptor.multiValue(Publisher.class),
				source -> Flux.from((Publisher<?>) source),
				source -> source);

		registerReactiveType(
				ReactiveTypeDescriptor.singleOptionalValue(CompletableFuture.class),
				source -> Mono.fromFuture((CompletableFuture<?>) source),
				source -> Mono.from(source).toFuture()
		);

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

		ReactiveAdapter adapter = (descriptor.isMultiValue() ?
				new FluxReactiveAdapter(toAdapter, fromAdapter, descriptor) :
				new MonoReactiveAdapter(toAdapter, fromAdapter, descriptor));

		this.adapters.add(adapter);
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
		source = unwrapOptional(source);
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

	private static Object unwrapOptional(Object value) {
		return (value instanceof Optional ? ((Optional<?>) value).orElse(null) : value);
	}


	@SuppressWarnings("unchecked")
	private static class MonoReactiveAdapter implements ReactiveAdapter {

		private final Function<Object, Publisher<?>> toAdapter;

		private final Function<Publisher<?>, Object> fromAdapter;

		private final ReactiveTypeDescriptor descriptor;


		MonoReactiveAdapter(Function<Object, Publisher<?>> to, Function<Publisher<?>, Object> from,
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
			return (Mono<T>) Mono.from(this.toAdapter.apply(source));
		}

		@Override
		public <T> Flux<T> toFlux(Object source) {
			source = unwrapOptional(source);
			if (source == null) {
				return Flux.empty();
			}
			return (Flux<T>) toMono(source).flux();
		}

		@Override
		public <T> Publisher<T> toPublisher(Object source) {
			return toMono(source);
		}

		@Override
		public Object fromPublisher(Publisher<?> source) {
			return (source != null ? this.fromAdapter.apply(source) : null);
		}
	}

	@SuppressWarnings("unchecked")
	private static class FluxReactiveAdapter implements ReactiveAdapter {

		private final Function<Object, Publisher<?>> toAdapter;

		private final Function<Publisher<?>, Object> fromAdapter;

		private final ReactiveTypeDescriptor descriptor;


		FluxReactiveAdapter(Function<Object, Publisher<?>> to, Function<Publisher<?>, Object> from,
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
			return (Mono<T>) toFlux(source).next();
		}

		@Override
		public <T> Flux<T> toFlux(Object source) {
			source = unwrapOptional(source);
			if (source == null) {
				return Flux.empty();
			}
			return (Flux<T>) Flux.from(this.toAdapter.apply(source));
		}

		@Override
		public <T> Publisher<T> toPublisher(Object source) {
			return toFlux(source);
		}

		@Override
		public Object fromPublisher(Publisher<?> source) {
			return (source != null ? this.fromAdapter.apply(source) : null);
		}
	}


	private static class RxJava1Registrar {

		public void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(rx.Observable.class),
					source -> Flux.from(RxReactiveStreams.toPublisher((rx.Observable<?>) source)),
					RxReactiveStreams::toObservable
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleRequiredValue(rx.Single.class),
					source -> Mono.from(RxReactiveStreams.toPublisher((rx.Single<?>) source)),
					RxReactiveStreams::toSingle
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.noValue(rx.Completable.class),
					source -> Mono.from(RxReactiveStreams.toPublisher((rx.Completable) source)),
					RxReactiveStreams::toCompletable
			);
		}
	}

	private static class RxJava2Registrar {

		public void registerAdapters(ReactiveAdapterRegistry registry) {
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(Flowable.class),
					source -> Flux.from((Flowable<?>) source),
					source-> Flowable.fromPublisher(source)
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.multiValue(io.reactivex.Observable.class),
					source -> Flux.from(((io.reactivex.Observable<?>) source).toFlowable(BackpressureStrategy.BUFFER)),
					source -> Flowable.fromPublisher(source).toObservable()
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleRequiredValue(io.reactivex.Single.class),
					source -> Mono.from(((io.reactivex.Single<?>) source).toFlowable()),
					source -> Flowable.fromPublisher(source).toObservable().singleElement().toSingle()
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.singleOptionalValue(Maybe.class),
					source -> Mono.from(((Maybe<?>) source).toFlowable()),
					source -> Flowable.fromPublisher(source).toObservable().singleElement()
			);
			registry.registerReactiveType(
					ReactiveTypeDescriptor.noValue(io.reactivex.Completable.class),
					source -> Mono.from(((io.reactivex.Completable) source).toFlowable()),
					source -> Flowable.fromPublisher(source).toObservable().ignoreElements()
			);
		}
	}

}
